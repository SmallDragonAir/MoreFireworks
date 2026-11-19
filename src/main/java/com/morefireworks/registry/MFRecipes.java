package com.morefireworks.registry;

import com.morefireworks.MoreFireworks;
import com.morefireworks.recipe.StarAssemblyRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Recipe serializers. Only one so far: the ordered star-assembly recipe. */
public final class MFRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, MoreFireworks.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<StarAssemblyRecipe>> STAR_ASSEMBLY =
            RECIPE_SERIALIZERS.register("star_assembly", () -> StarAssemblyRecipe.SERIALIZER);

    private MFRecipes() {
    }
}
