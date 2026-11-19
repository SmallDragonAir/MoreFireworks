package com.morefireworks.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.morefireworks.core.FireworkLimits;
import com.morefireworks.core.RocketSpec;
import com.morefireworks.core.StarSpec;
import com.morefireworks.item.SpecFireworkRocketItem;
import com.morefireworks.logic.VanillaBridge;
import com.morefireworks.registry.MFCreativeTabs;
import com.morefireworks.registry.MFDataComponents;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Shapeless "assembly" recipe: one rocket shell plus any number of firework stars, in any slot
 * arrangement, becomes a single spec rocket whose explosions are appended <em>in slot order</em>.
 *
 * <p>This is the survival-friendly way to author ordered multi-stage fireworks. Vanilla's own
 * {@code crafting_special_firework_rocket} also collects stars, but it stores them as an unordered
 * set and detonates them all at once, so the sequence — the whole point of this mod — would be
 * lost. Accepting vanilla stars as well as spec stars means a player can build the individual
 * stars with the ordinary vanilla recipes and only needs this mod for the ordering.</p>
 */
public class StarAssemblyRecipe extends CustomRecipe {

    public static final MapCodec<StarAssemblyRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.fieldOf("shell").forGetter(recipe -> recipe.shell),
            Ingredient.CODEC.fieldOf("star").forGetter(recipe -> recipe.star)
    ).apply(instance, StarAssemblyRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, StarAssemblyRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.shell,
            Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.star,
            StarAssemblyRecipe::new
    );

    /**
     * In 1.21.1 {@link RecipeSerializer} is an interface rather than an instantiable class, so the
     * pair of codecs is wrapped in a tiny record. Record accessors {@code codec()} and
     * {@code streamCodec()} satisfy the interface directly.
     */
    public record Serializer(MapCodec<StarAssemblyRecipe> codec,
                             StreamCodec<RegistryFriendlyByteBuf, StarAssemblyRecipe> streamCodec)
            implements RecipeSerializer<StarAssemblyRecipe> {
    }

    public static final RecipeSerializer<StarAssemblyRecipe> SERIALIZER =
            new Serializer(MAP_CODEC, STREAM_CODEC);

    private final Ingredient shell;
    private final Ingredient star;

    public StarAssemblyRecipe(Ingredient shell, Ingredient star) {
        super(CraftingBookCategory.MISC);
        this.shell = shell;
        this.star = star;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int shells = 0;
        int stars = 0;
        int flight = RocketSpec.MIN_FLIGHT;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (this.shell.test(stack)) {
                if (++shells > 1) {
                    return false;
                }
                flight = specOfShell(stack).flight();
            } else if (this.star.test(stack)) {
                if (++stars > FireworkLimits.MAX_STARS) {
                    return false;
                }
            } else {
                return false;
            }
        }
        if (shells != 1 || stars < 1) {
            return false;
        }
        // Refuse to match when the tier cannot hold every star: reporting no result keeps the
        // ingredients in the grid, whereas matching and clamping would silently eat the surplus.
        int existing = shellStages(input);
        return existing + stars <= RocketSpec.maxStars(flight);
    }

    /** Number of stages the shell already carries, so an over-full grid is rejected up front. */
    private int shellStages(CraftingInput input) {
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (!stack.isEmpty() && this.shell.test(stack)) {
                return specOfShell(stack).starCount();
            }
        }
        return 0;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        RocketSpec base = null;
        List<StarSpec> added = new ArrayList<>();
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (this.shell.test(stack)) {
                base = specOfShell(stack);
            } else if (this.star.test(stack)) {
                StarSpec spec = specOfStar(stack);
                if (spec != null) {
                    added.add(spec);
                }
            }
        }
        if (base == null) {
            return ItemStack.EMPTY;
        }
        RocketSpec merged = merge(base, added);
        return merged.isEmpty() ? ItemStack.EMPTY : MFCreativeTabs.rocket(merged);
    }

    /**
     * A 3×3 table is not required: the recipe works in the 2×2 inventory grid as well, which is
     * where most players will try it.
     */
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    /**
     * Appends the collected stars to the shell's existing stages, preserving the order in which they
     * were found and stopping at the flight tier's legal maximum.
     */
    public static RocketSpec merge(RocketSpec shell, List<StarSpec> added) {
        return com.morefireworks.core.RocketAssembly.merge(shell, added);
    }

    /** Reads a shell stack, whether it is one of ours or a plain vanilla rocket. */
    private static RocketSpec specOfShell(ItemStack stack) {
        RocketSpec spec = stack.get(MFDataComponents.ROCKET_SPEC.get());
        if (spec != null) {
            return spec;
        }
        Fireworks vanilla = stack.get(DataComponents.FIREWORKS);
        if (vanilla != null) {
            return VanillaBridge.toSpec(vanilla);
        }
        return SpecFireworkRocketItem.plain(RocketSpec.MIN_FLIGHT);
    }

    /** Reads a star stack, whether it is one of ours or a plain vanilla firework star. */
    private static StarSpec specOfStar(ItemStack stack) {
        StarSpec spec = stack.get(MFDataComponents.STAR_SPEC.get());
        if (spec != null) {
            return spec;
        }
        FireworkExplosion explosion = stack.get(DataComponents.FIREWORK_EXPLOSION);
        return explosion == null ? null : VanillaBridge.toStar(explosion);
    }

    @Override
    public RecipeSerializer<StarAssemblyRecipe> getSerializer() {
        return SERIALIZER;
    }
}
