package com.morefireworks.registry;

import com.morefireworks.MoreFireworks;
import com.morefireworks.item.SpecFireworkRocketItem;
import com.morefireworks.item.SpecFireworkStarItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The mod's two items.
 *
 * <p>Exactly two item types exist, regardless of how many variants the mathematics allows: the
 * variant itself lives in a data component. This is the whole point of the project — a fixed,
 * tiny registry footprint combined with an astronomical parameter space.</p>
 */
public final class MFItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MoreFireworks.MOD_ID);

    public static final DeferredItem<SpecFireworkRocketItem> SPEC_ROCKET =
            ITEMS.registerItem("spec_firework_rocket", SpecFireworkRocketItem::new,
                    new Item.Properties().stacksTo(16));

    public static final DeferredItem<SpecFireworkStarItem> SPEC_STAR =
            ITEMS.registerItem("spec_firework_star", SpecFireworkStarItem::new);

    private MFItems() {
    }
}
