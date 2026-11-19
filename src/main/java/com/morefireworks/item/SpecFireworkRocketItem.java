package com.morefireworks.item;

import com.morefireworks.core.RocketSpec;
import com.morefireworks.core.StarSpec;
import com.morefireworks.logic.StagedDetonation;
import com.morefireworks.naming.NameComponents;
import com.morefireworks.registry.MFDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Consumer;

/**
 * A firework rocket whose parameters live in the {@code morefireworks:rocket_spec} component.
 *
 * <p>The vanilla {@code minecraft:fireworks} component is kept on the stack as well, so the stack
 * still works with vanilla mechanics (tooltips, dispenser behaviour, the vanilla rocket entity) while
 * our component carries the full ordered specification that vanilla cannot express.</p>
 */
public class SpecFireworkRocketItem extends Item {

    public SpecFireworkRocketItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        RocketSpec spec = stack.get(MFDataComponents.ROCKET_SPEC.get());
        if (spec == null) {
            return super.getName(stack);
        }
        return NameComponents.shortName(spec).withStyle(ChatFormatting.RESET);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        RocketSpec spec = stack.get(MFDataComponents.ROCKET_SPEC.get());
        if (spec == null) {
            return;
        }
        for (Component line : NameComponents.fullNameLines(spec)) {
            tooltip.accept(line);
        }
    }

    /** Convenience for command code: the spec, or {@code null} when the stack is a plain rocket. */
    public static RocketSpec specOf(ItemStack stack) {
        return stack.get(MFDataComponents.ROCKET_SPEC.get());
    }

    /**
     * Right-clicking a block launches the rocket from the clicked face, mirroring vanilla placement.
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.isFallFlying()) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        if (level instanceof ServerLevel serverLevel) {
            ItemStack stack = context.getItemInHand();
            Vec3 click = context.getClickLocation();
            Direction face = context.getClickedFace();
            Vec3 origin = click.add(face.getStepX() * 0.15, face.getStepY() * 0.15, face.getStepZ() * 0.15);
            StagedDetonation.launchAt(serverLevel, stack, origin, Vec3.ZERO);
            if (player != null) {
                stack.consume(1, player);
                player.awardStat(Stats.ITEM_USED.get(this));
            } else {
                stack.shrink(1);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Right-clicking the air launches the rocket straight ahead. Vanilla requires a block face, but
     * these rockets are aimed by their parameters rather than by block placement, so aiming with the
     * crosshair is both simpler and less surprising.
     */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            StagedDetonation.launch(serverLevel, player, stack);
            stack.consume(1, player);
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        return InteractionResult.SUCCESS;
    }

    /** Builds a plain (no explosion) rocket spec for the given flight tier. */
    public static RocketSpec plain(int flight) {
        return RocketSpec.plain(flight);
    }

    /** Utility used by the star item to build a one-star rocket. */
    public static RocketSpec single(StarSpec star, int flight) {
        return RocketSpec.of(flight, List.of(star));
    }
}
