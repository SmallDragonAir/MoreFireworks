package com.morefireworks.logic;

import com.mojang.logging.LogUtils;
import com.morefireworks.MFConfig;
import com.morefireworks.core.FireworkLimits;
import com.morefireworks.core.RocketSpec;
import com.morefireworks.core.StarSpec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;

/**
 * Turns a {@link RocketSpec} into an actual, visibly <em>sequential</em> firework.
 *
 * <p>Vanilla detonates every explosion of a rocket on the same tick, so the order of the explosion
 * list is never observable. That is the one place where vanilla behaviour is genuinely
 * insufficient, so the mod implements its own staging:</p>
 *
 * <ol>
 *   <li>the rocket entity that flies up carries only stage 0 in its vanilla component, so the
 *       flight, the trail and the first burst all look exactly like vanilla;</li>
 *   <li>the remaining stages are kept server-side and fired {@link MFConfig#STAGE_INTERVAL_TICKS}
 *       ticks apart, in list order, at the position where the rocket detonated;</li>
 *   <li>each stage is emitted through {@code ServerLevel#createFireworks}, which broadcasts the
 *       particle packet without dealing extra damage or destroying blocks.</li>
 * </ol>
 *
 * <p>Every timeline is bounded: at most {@link MFConfig#MAX_ACTIVE_TIMELINES} may exist at
 * once, each holds at most {@link FireworkLimits#MAX_STARS} stages, and each expires after
 * {@link MFConfig#STAGE_TIMEOUT_TICKS} ticks even if the rocket entity is never removed.</p>
 */
public final class StagedDetonation {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<UUID, Timeline> ACTIVE = new HashMap<>();

    private StagedDetonation() {
    }

    private static final class Timeline {
        final ServerLevel level;
        final Entity rocket;
        final Deque<FireworkExplosion> pending;
        final Vec3 lastPosition;
        int delay;
        int age;
        int fired;
        final int total;

        Timeline(ServerLevel level, Entity rocket, Deque<FireworkExplosion> pending, Vec3 position) {
            this.level = level;
            this.rocket = rocket;
            this.pending = pending;
            this.lastPosition = position;
            this.delay = MFConfig.STAGE_INTERVAL_TICKS.get();
            this.total = pending.size();
        }
    }

    /** Registers the follow-up stages of a rocket that has just been launched. */
    public static void track(ServerLevel level, Entity rocket, List<StarSpec> stages, Vec3 position) {
        if (stages.isEmpty() || ACTIVE.size() >= MFConfig.MAX_ACTIVE_TIMELINES.get()) {
            return;
        }
        Deque<FireworkExplosion> pending = new ArrayDeque<>();
        for (StarSpec star : stages) {
            pending.add(VanillaBridge.toExplosion(star));
        }
        ACTIVE.put(rocket.getUUID(), new Timeline(level, rocket, pending, position));
    }

    /** Called once per server tick; cheap when nothing is in flight. */
    public static void tick() {
        if (ACTIVE.isEmpty()) {
            return;
        }
        List<UUID> finished = new ArrayList<>();
        for (Map.Entry<UUID, Timeline> entry : ACTIVE.entrySet()) {
            Timeline timeline = entry.getValue();
            timeline.age++;
            boolean detonated = timeline.rocket.isRemoved() || !timeline.rocket.isAlive();
            if (!detonated && timeline.age < MFConfig.STAGE_TIMEOUT_TICKS.get()) {
                continue;
            }
            if (--timeline.delay > 0) {
                continue;
            }
            FireworkExplosion next = timeline.pending.poll();
            if (next == null) {
                finished.add(entry.getKey());
                continue;
            }
            Vec3 at = detonated ? timeline.rocket.position() : timeline.lastPosition;
            timeline.level.createFireworks(at.x, at.y, at.z, 0.0, 0.0, 0.0, List.of(next));
            timeline.level.playSound(null, at.x, at.y, at.z, SoundEvents.FIREWORK_ROCKET_BLAST,
                    SoundSource.AMBIENT, 3.0F, 1.0F);
            timeline.fired++;
            LOGGER.debug("morefireworks: staged detonation {} fired stage {}/{} at ({}, {}, {})",
                    entry.getKey(), timeline.fired, timeline.total, at.x, at.y, at.z);
            timeline.delay = MFConfig.STAGE_INTERVAL_TICKS.get();
            if (timeline.pending.isEmpty()) {
                finished.add(entry.getKey());
            }
        }
        for (UUID id : finished) {
            ACTIVE.remove(id);
        }
    }

    /** Drops every pending timeline, e.g. when a level is unloaded. */
    public static void clear() {
        ACTIVE.clear();
    }

    public static int activeCount() {
        return ACTIVE.size();
    }

    /**
     * Spawns the rocket entity for a spec: the entity itself only carries stage 0, the rest are
     * handed to {@link #track}.
     */
    public static void launch(ServerLevel level, net.minecraft.world.entity.player.Player player, ItemStack stack) {
        launchAt(level, stack, player.getEyePosition(), player.getLookAngle().scale(0.1));
    }

    /**
     * Spawns the rocket at an explicit position. Used both by the item (right-click in air launches
     * from the eyes, right-click on a block launches from the clicked face) and, in principle, by
     * dispensers.
     */
    public static void launchAt(ServerLevel level, ItemStack stack, Vec3 origin, Vec3 extraVelocity) {
        RocketSpec spec = stack.get(com.morefireworks.registry.MFDataComponents.ROCKET_SPEC.get());
        if (spec == null) {
            Fireworks vanilla = stack.get(DataComponents.FIREWORKS);
            spec = vanilla == null ? RocketSpec.plain(1) : VanillaBridge.toSpec(vanilla);
        }
        ItemStack visual = stack.copyWithCount(1);
        if (spec.isEmpty()) {
            visual.set(DataComponents.FIREWORKS, new Fireworks(spec.flight(), List.of()));
        } else {
            visual.set(DataComponents.FIREWORKS, VanillaBridge.singleStage(spec, 0));
        }

        FireworkRocketEntity rocket = new FireworkRocketEntity(level, origin.x, origin.y, origin.z, visual);
        if (extraVelocity.lengthSqr() > 0.0) {
            rocket.setDeltaMovement(rocket.getDeltaMovement().add(extraVelocity));
        }
        level.addFreshEntity(rocket);

        if (spec.stars().size() > 1) {
            track(level, rocket, spec.stars().subList(1, spec.stars().size()), origin);
        }
    }
}
