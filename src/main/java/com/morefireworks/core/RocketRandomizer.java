package com.morefireworks.core;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Generates legal {@link RocketSpec}s directly from a seed.
 *
 * <p>Nothing is enumerated: every field is sampled independently inside its legal range, so the cost
 * is proportional to the size of the generated firework, never to the (astronomical) size of the
 * variant space. The same seed always yields the same spec because the only randomness is a
 * {@link SplittableRandom} seeded by the caller.</p>
 */
public final class RocketRandomizer {

    private RocketRandomizer() {
    }

    /** Ordinary random firework: any legal flight tier, 1..3 explosions, vanilla-ish palette. */
    public static RocketSpec random(long seed) {
        SplittableRandom rng = new SplittableRandom(seed);
        int flight = 1 + rng.nextInt(RocketSpec.MAX_FLIGHT);
        int maxStars = RocketSpec.maxStars(flight);
        int stars = 1 + rng.nextInt(Math.min(maxStars, 3));
        List<StarSpec> list = new ArrayList<>(stars);
        for (int i = 0; i < stars; i++) {
            list.add(randomStar(rng, 1 + rng.nextInt(2), rng.nextInt(3) == 0 ? 1 : 0));
        }
        return RocketSpec.of(flight, list);
    }

    /** Complex firework: many explosions, real 24-bit colours, gradients and both effects. */
    public static RocketSpec complex(long seed) {
        SplittableRandom rng = new SplittableRandom(seed);
        int flight = 2 + rng.nextInt(2);
        int maxStars = RocketSpec.maxStars(flight);
        int stars = Math.max(3, maxStars - rng.nextInt(2));
        List<StarSpec> list = new ArrayList<>(stars);
        for (int i = 0; i < stars; i++) {
            StarSpec star = randomStar(rng, 2 + rng.nextInt(3), 1 + rng.nextInt(3));
            if (i == 0) {
                // The opening explosion always shows off both effects: "complex" promises them.
                star = new StarSpec(star.shape(), star.colors(), star.fadeColors(), true, true);
            }
            list.add(star);
        }
        return RocketSpec.of(flight, list);
    }

    /**
     * Extreme firework: saturates every legal limit — the most complex thing the game will accept
     * without violating the safety caps.
     */
    public static RocketSpec extreme(long seed) {
        SplittableRandom rng = new SplittableRandom(seed);
        int flight = 1; // 8 - 1 = 7 explosions, the maximum the caps allow
        int stars = RocketSpec.maxStars(flight);
        List<StarSpec> list = new ArrayList<>(stars);
        for (int i = 0; i < stars; i++) {
            StarShape shape = StarShape.values()[rng.nextInt(StarShape.values().length)];
            List<Integer> colors = new ArrayList<>(StarSpec.MAX_COLORS);
            for (int c = 0; c < StarSpec.MAX_COLORS; c++) {
                colors.add(randomRgb(rng));
            }
            List<Integer> fades = new ArrayList<>(StarSpec.MAX_FADE_COLORS);
            for (int c = 0; c < StarSpec.MAX_FADE_COLORS; c++) {
                fades.add(randomRgb(rng));
            }
            list.add(new StarSpec(shape, colors, fades, true, true));
        }
        return RocketSpec.of(flight, list);
    }

    private static StarSpec randomStar(SplittableRandom rng, int colors, int fades) {
        StarShape shape = StarShape.values()[rng.nextInt(StarShape.values().length)];
        List<Integer> colorList = new ArrayList<>(colors);
        for (int i = 0; i < colors; i++) {
            colorList.add(rng.nextInt(3) == 0 ? randomRgb(rng) : vanillaDye(rng));
        }
        List<Integer> fadeList = new ArrayList<>(fades);
        for (int i = 0; i < fades; i++) {
            fadeList.add(rng.nextInt(2) == 0 ? randomRgb(rng) : vanillaDye(rng));
        }
        return new StarSpec(shape, colorList, fadeList, rng.nextInt(2) == 0, rng.nextInt(2) == 0);
    }

    private static int randomRgb(SplittableRandom rng) {
        return rng.nextInt(RgbColors.COLOR_COUNT);
    }

    private static int vanillaDye(SplittableRandom rng) {
        return VANILLA_DYES[rng.nextInt(VANILLA_DYES.length)];
    }

    /** The sixteen vanilla dye colours, in the same order as {@code DyeColor}. */
    private static final int[] VANILLA_DYES = {
            0xF9FFFE, 0xF9801D, 0xC74EBD, 0x3AB3DA,
            0xFED83D, 0x80C71F, 0xF38BAA, 0x474F52,
            0x9D9D97, 0x169C9C, 0x8932B8, 0x3C44AA,
            0x835432, 0x5E7C16, 0xB02E26, 0x1D1D21,
    };
}
