package com.morefireworks.item;

import com.morefireworks.core.StarSpec;
import com.morefireworks.naming.NameComponents;
import com.morefireworks.registry.MFDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A single firework star carrying a {@link StarSpec}: shape, ordered RGB colours, fade colours and
 * the two effect flags. Stars are the building blocks that a rocket combines; the component makes
 * every one of them distinct without registering millions of items.
 */
public class SpecFireworkStarItem extends Item {

    public SpecFireworkStarItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        StarSpec star = stack.get(MFDataComponents.STAR_SPEC.get());
        if (star == null) {
            return super.getName(stack);
        }
        return NameComponents.starName(star).withStyle(ChatFormatting.RESET);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        StarSpec star = stack.get(MFDataComponents.STAR_SPEC.get());
        if (star == null) {
            return;
        }
        tooltip.addAll(NameComponents.starLines(star));
    }
}
