package com.morefireworks.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The crafting table's assembly rules: stars keep the order they were placed in, and the flight tier
 * decides how many fit.
 */
class RocketAssemblyTest {

    private static StarSpec star(int rgb) {
        return new StarSpec(StarShape.STAR, List.of(rgb), List.of(), false, false);
    }

    @Test
    void appendedStarsKeepTheirOrder() {
        RocketSpec shell = RocketSpec.plain(1);
        RocketSpec merged = RocketAssembly.merge(shell, List.of(star(0x010203), star(0x040506)));
        assertEquals(2, merged.starCount());
        assertEquals(List.of(star(0x010203), star(0x040506)), merged.stars());
    }

    @Test
    void existingStagesComeFirst() {
        StarSpec first = star(0xB02E26);
        RocketSpec shell = RocketSpec.of(2, List.of(first));
        RocketSpec merged = RocketAssembly.merge(shell, List.of(star(0x3C44AA)));
        assertEquals(List.of(first, star(0x3C44AA)), merged.stars());
        assertEquals(2, merged.flight());
    }

    @Test
    void flightTierLimitsTheNumberOfStages() {
        List<StarSpec> seven = List.of(star(1), star(2), star(3), star(4), star(5), star(6), star(7));
        assertEquals(7, RocketAssembly.merge(RocketSpec.plain(1), seven).starCount());
        assertEquals(6, RocketAssembly.merge(RocketSpec.plain(2), seven).starCount());
        assertEquals(5, RocketAssembly.merge(RocketSpec.plain(3), seven).starCount());
    }

    @Test
    void existingStagesCountTowardsTheLimit() {
        RocketSpec shell = RocketSpec.of(3, List.of(star(1), star(2), star(3), star(4)));
        RocketSpec merged = RocketAssembly.merge(shell, List.of(star(5), star(6)));
        assertEquals(5, merged.starCount());
        assertEquals(star(5), merged.stars().get(4));
    }

    @Test
    void emptyAdditionLeavesTheShellAlone() {
        RocketSpec shell = RocketSpec.of(2, List.of(star(0x123456)));
        RocketSpec merged = RocketAssembly.merge(shell, List.of());
        assertEquals(shell, merged);
    }

    @Test
    void plainShellStaysPlainWhenNothingIsAdded() {
        RocketSpec merged = RocketAssembly.merge(RocketSpec.plain(2), List.of());
        assertTrue(merged.isEmpty());
        assertEquals(2, merged.flight());
    }

    @Test
    void mergingIsDeterministic() {
        RocketSpec shell = RocketSpec.of(1, List.of(star(0xABCDEF)));
        List<StarSpec> added = List.of(star(0x111111), star(0x222222));
        assertEquals(RocketAssembly.merge(shell, added), RocketAssembly.merge(shell, added));
        assertSame(RocketAssembly.merge(shell, added).stars().get(0), shell.stars().get(0));
    }
}
