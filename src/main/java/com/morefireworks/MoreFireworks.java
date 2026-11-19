package com.morefireworks;

import com.morefireworks.command.MFCommands;
import com.morefireworks.logic.StagedDetonation;
import com.morefireworks.registry.MFCreativeTabs;
import com.morefireworks.registry.MFDataComponents;
import com.morefireworks.registry.MFItems;
import com.morefireworks.registry.MFRecipes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Entry point of the More Fireworks mod.
 *
 * <p>The registration surface is deliberately tiny — two items, two data components, one creative
 * tab — while the parameter space they describe is astronomical. Nothing is ever registered or
 * loaded per variant, so startup cost and memory use are constant.</p>
 */
@Mod(MoreFireworks.MOD_ID)
public final class MoreFireworks {

    public static final String MOD_ID = "morefireworks";

    public MoreFireworks(IEventBus modBus, net.neoforged.fml.ModContainer modContainer) {
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, MFConfig.SPEC);

        MFDataComponents.DATA_COMPONENTS.register(modBus);
        MFItems.ITEMS.register(modBus);
        MFCreativeTabs.TABS.register(modBus);
        MFRecipes.RECIPE_SERIALIZERS.register(modBus);

        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(MoreFireworks::onServerTick);
        NeoForge.EVENT_BUS.addListener(MoreFireworks::onServerStopped);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        MFCommands.register(event.getDispatcher());
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        StagedDetonation.tick();
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        StagedDetonation.clear();
    }
}
