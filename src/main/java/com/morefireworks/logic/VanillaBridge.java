package com.morefireworks.logic;

import com.morefireworks.core.RocketSpec;
import com.morefireworks.core.StarShape;
import com.morefireworks.core.StarSpec;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;

import java.util.ArrayList;
import java.util.List;

/**
 * Translation between the mod's own {@link RocketSpec} model and vanilla's
 * {@link Fireworks} component.
 *
 * <p>Vanilla is kept in the loop on purpose: storing a matching {@code minecraft:fireworks}
 * component means vanilla tooltips, the vanilla rocket entity, dispensers and any other mod that
 * reads fireworks keep working. The custom component adds only what vanilla cannot express — the
 * ordered sequence of explosions and the strict validation of the parameters.</p>
 */
public final class VanillaBridge {

    private VanillaBridge() {
    }

    public static Fireworks toVanilla(RocketSpec spec) {
        List<FireworkExplosion> explosions = new ArrayList<>(spec.stars().size());
        for (StarSpec star : spec.stars()) {
            explosions.add(toExplosion(star));
        }
        return new Fireworks(spec.flight(), explosions);
    }

    /** Vanilla representation of a single stage, used to make the first explosion look native. */
    public static Fireworks singleStage(RocketSpec spec, int index) {
        return new Fireworks(spec.flight(), List.of(toExplosion(spec.stars().get(index))));
    }

    public static FireworkExplosion toExplosion(StarSpec star) {
        return new FireworkExplosion(shape(star.shape()), intList(star.colors()),
                intList(star.fadeColors()), star.trail(), star.flicker());
    }

    public static StarSpec toStar(FireworkExplosion explosion) {
        return new StarSpec(fromVanilla(explosion.shape()), toList(explosion.colors()),
                toList(explosion.fadeColors()), explosion.hasTwinkle(), explosion.hasTrail());
    }

    /** Best effort conversion of a plain vanilla rocket, so the mod can also inspect vanilla items. */
    public static RocketSpec toSpec(Fireworks fireworks) {
        List<StarSpec> stars = new ArrayList<>(fireworks.explosions().size());
        for (FireworkExplosion explosion : fireworks.explosions()) {
            stars.add(toStar(explosion));
        }
        return RocketSpec.of(Math.max(RocketSpec.MIN_FLIGHT, Math.min(RocketSpec.MAX_FLIGHT,
                fireworks.flightDuration())), stars);
    }

    private static IntList intList(List<Integer> values) {
        IntList list = new IntArrayList(values.size());
        for (int value : values) {
            list.add(value);
        }
        return list;
    }

    private static List<Integer> toList(IntList list) {
        List<Integer> values = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            values.add(list.getInt(i));
        }
        return values;
    }

    public static StarShape fromVanilla(FireworkExplosion.Shape shape) {
        return switch (shape) {
            case SMALL_BALL -> StarShape.SMALL_BALL;
            case LARGE_BALL -> StarShape.LARGE_BALL;
            case STAR -> StarShape.STAR;
            case CREEPER -> StarShape.CREEPER;
            case BURST -> StarShape.BURST;
        };
    }

    public static FireworkExplosion.Shape shape(StarShape shape) {
        return switch (shape) {
            case SMALL_BALL -> FireworkExplosion.Shape.SMALL_BALL;
            case LARGE_BALL -> FireworkExplosion.Shape.LARGE_BALL;
            case STAR -> FireworkExplosion.Shape.STAR;
            case CREEPER -> FireworkExplosion.Shape.CREEPER;
            case BURST -> FireworkExplosion.Shape.BURST;
        };
    }
}
