package com.morefireworks.registry;

import com.morefireworks.MoreFireworks;
import com.morefireworks.core.RgbColors;
import com.morefireworks.core.RocketRandomizer;
import com.morefireworks.core.RocketSpec;
import com.morefireworks.core.SpecCodec;
import com.morefireworks.core.StarShape;
import com.morefireworks.core.StarSpec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * A creative tab containing a small, curated set of examples plus every generated variant the
 * player is likely to want as a starting point.
 *
 * <p>Only a handful of stacks are listed. The tab never enumerates the variant space; adding the
 * whole space is mathematically impossible, which is exactly the point of the mod.</p>
 */
public final class MFCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MoreFireworks.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.morefireworks"))
                    .icon(() -> rocket(RocketRandomizer.complex(1L)))
                    .displayItems((parameters, output) -> {
                        // Plain rockets, one per flight tier.
                        for (int flight = RocketSpec.MIN_FLIGHT; flight <= RocketSpec.MAX_FLIGHT; flight++) {
                            output.accept(rocket(RocketSpec.plain(flight)));
                        }
                        // The five shapes, each in a single dye colour.
                        StarShape[] shapes = StarShape.values();
                        int[] dyes = {0xB02E26, 0x3C44AA, 0xFED83D, 0x80C71F, 0xC74EBD};
                        for (int i = 0; i < shapes.length; i++) {
                            output.accept(star(new StarSpec(shapes[i], List.of(dyes[i]), List.of(), false, false)));
                        }
                        // One fully loaded example of each random tier.
                        output.accept(rocket(RocketRandomizer.random(20260101L)));
                        output.accept(rocket(RocketRandomizer.complex(20260101L)));
                        output.accept(rocket(RocketRandomizer.extreme(20260101L)));
                        // A hand written showcase: three stages, gradients, both effects.
                        output.accept(rocket(RocketSpec.of(2, List.of(
                                new StarSpec(StarShape.STAR, List.of(0xB02E26, 0x3C44AA), List.of(0xFED83D), true, false),
                                new StarSpec(StarShape.LARGE_BALL, List.of(0x3AB3DA), List.of(0xF9FFFE), false, true),
                                new StarSpec(StarShape.CREEPER, List.of(RgbColors.of(0x12, 0xAB, 0x34)), List.of(), true, true)))));
                    })
                    .build());

    private MFCreativeTabs() {
    }

    public static ItemStack rocket(RocketSpec spec) {
        ItemStack stack = new ItemStack(MFItems.SPEC_ROCKET.get());
        stack.set(MFDataComponents.ROCKET_SPEC.get(), spec);
        stack.set(net.minecraft.core.component.DataComponents.FIREWORKS,
                com.morefireworks.logic.VanillaBridge.toVanilla(spec));
        return stack;
    }

    public static ItemStack star(StarSpec spec) {
        ItemStack stack = new ItemStack(MFItems.SPEC_STAR.get());
        stack.set(MFDataComponents.STAR_SPEC.get(), spec);
        // Vanilla's item model tints the star icon from this component, so every generated star
        // shows its own palette in the inventory without any custom rendering code.
        stack.set(net.minecraft.core.component.DataComponents.FIREWORK_EXPLOSION,
                com.morefireworks.logic.VanillaBridge.toExplosion(spec));
        return stack;
    }

    /** Useful for debugging: the canonical text of a spec, as stored on the stack. */
    public static String debugText(RocketSpec spec) {
        return SpecCodec.encode(spec);
    }
}
