package com.morefireworks.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure assembly rules shared by the crafting recipe and the tests.
 *
 * <p>Keeping this logic out of the recipe class means it can be unit tested without a Minecraft
 * runtime, and it documents the two rules that make the crafting table able to author ordered
 * fireworks: the order of the stars is the order of the stages, and the flight tier limits how many
 * stages fit.</p>
 */
public final class RocketAssembly {

    private RocketAssembly() {
    }

    /**
     * Appends {@code added} to the shell's existing stages in list order, stopping at the maximum
     * number of stars the shell's flight tier allows.
     */
    public static RocketSpec merge(RocketSpec shell, List<StarSpec> added) {
        int flight = shell.flight();
        int max = RocketSpec.maxStars(flight);
        List<StarSpec> stages = new ArrayList<>(shell.stars());
        for (StarSpec spec : added) {
            if (stages.size() >= max) {
                break;
            }
            stages.add(spec);
        }
        if (stages.isEmpty()) {
            return RocketSpec.plain(flight);
        }
        return RocketSpec.of(flight, stages);
    }
}
