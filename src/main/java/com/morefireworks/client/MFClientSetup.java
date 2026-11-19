package com.morefireworks.client;

import com.morefireworks.MoreFireworks;
import com.morefireworks.registry.MFItems;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.FireworkExplosion;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/**
 * Client-only glue.
 *
 * <p>In 1.21.1 item tints are still supplied by {@code ItemColor} providers rather than by item
 * model definitions, so the firework star icon needs a small client-side handler to show its
 * colours. The class is annotated with {@code value = Dist.CLIENT}, which means it is never loaded
 * on a dedicated server — requirement: nothing client-only may reach the server class loader.</p>
 */
@EventBusSubscriber(modid = MoreFireworks.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MFClientSetup {

    /** Same fallback vanilla uses for a star without explosion data. */
    private static final int DEFAULT_STAR_COLOR = 0xFF8A8A8A;

    private MFClientSetup() {
    }

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(MFClientSetup::starColor, MFItems.SPEC_STAR.get());
    }

    /**
     * Averages the star's initial colours, mirroring how 26.2 tints the same icon. Colour order is
     * not visible in a 16×16 icon, so an average is the honest summary.
     */
    private static int starColor(ItemStack stack, int tintIndex) {
        if (tintIndex != 0) {
            return 0xFFFFFFFF;
        }
        FireworkExplosion explosion = stack.get(DataComponents.FIREWORK_EXPLOSION);
        if (explosion == null) {
            return DEFAULT_STAR_COLOR;
        }
        IntList colors = explosion.colors();
        int count = colors.size();
        if (count == 0) {
            return DEFAULT_STAR_COLOR;
        }
        if (count == 1) {
            return 0xFF000000 | (colors.getInt(0) & 0xFFFFFF);
        }
        int red = 0;
        int green = 0;
        int blue = 0;
        for (int i = 0; i < count; i++) {
            int color = colors.getInt(i);
            red += (color >> 16) & 0xFF;
            green += (color >> 8) & 0xFF;
            blue += color & 0xFF;
        }
        return 0xFF000000 | ((red / count) << 16) | ((green / count) << 8) | (blue / count);
    }
}
