package com.morefireworks.core;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RocketRandomizerTest {

    @Test
    void sameSeedGivesTheSameFirework() {
        for (long seed : new long[]{0L, 1L, -7L, 20260101L, Long.MAX_VALUE, Long.MIN_VALUE}) {
            assertEquals(RocketRandomizer.random(seed), RocketRandomizer.random(seed));
            assertEquals(RocketRandomizer.complex(seed), RocketRandomizer.complex(seed));
            assertEquals(RocketRandomizer.extreme(seed), RocketRandomizer.extreme(seed));
        }
    }

    @Test
    void differentSeedsGiveDifferentFireworks() {
        Set<String> seen = new HashSet<>();
        for (long seed = 0; seed < 32; seed++) {
            seen.add(SpecCodec.encode(RocketRandomizer.random(seed)));
        }
        assertTrue(seen.size() > 24, "expected mostly distinct results, got " + seen.size());
    }

    @Test
    void everyRandomSpecIsLegal() {
        for (long seed = 0; seed < 500; seed++) {
            for (RocketSpec spec : List.of(RocketRandomizer.random(seed),
                    RocketRandomizer.complex(seed), RocketRandomizer.extreme(seed))) {
                assertTrue(spec.flight() >= RocketSpec.MIN_FLIGHT);
                assertTrue(spec.flight() <= RocketSpec.MAX_FLIGHT);
                assertTrue(spec.starCount() <= RocketSpec.maxStars(spec.flight()));
                assertFalse(spec.isEmpty());
                for (StarSpec star : spec.stars()) {
                    assertTrue(star.colors().size() >= StarSpec.MIN_COLORS);
                    assertTrue(star.colors().size() <= StarSpec.MAX_COLORS);
                    assertTrue(star.fadeColors().size() <= StarSpec.MAX_FADE_COLORS);
                    for (int rgb : star.colors()) {
                        assertTrue(RgbColors.isValid(rgb));
                    }
                    for (int rgb : star.fadeColors()) {
                        assertTrue(RgbColors.isValid(rgb));
                    }
                }
                // Everything the randomiser produces must survive the canonical text format.
                assertEquals(spec, SpecCodec.decode(SpecCodec.encode(spec)));
            }
        }
    }

    @Test
    void complexHasSeveralStagesAndBothEffects() {
        for (long seed = 0; seed < 100; seed++) {
            RocketSpec spec = RocketRandomizer.complex(seed);
            assertTrue(spec.starCount() >= 2, "complex rockets explode in several stages");
            assertTrue(spec.stars().stream().anyMatch(StarSpec::flicker));
            assertTrue(spec.stars().stream().anyMatch(StarSpec::trail));
            assertTrue(spec.stars().stream().anyMatch(StarSpec::hasFadeColors));
        }
    }

    @Test
    void extremeIsTheLargestLegalRocket() {
        RocketSpec spec = RocketRandomizer.extreme(42L);
        assertEquals(1, spec.flight(), "flight 1 allows the most stars");
        assertEquals(7, spec.starCount());
        for (StarSpec star : spec.stars()) {
            assertEquals(8, star.colors().size());
            assertEquals(8, star.fadeColors().size());
            assertTrue(star.flicker());
            assertTrue(star.trail());
        }
    }

    @Test
    void randomisersNeverExceedTheGlobalStarLimit() {
        assertTrue(RocketSpec.maxStars(1) <= FireworkLimits.MAX_STARS);
    }
}
