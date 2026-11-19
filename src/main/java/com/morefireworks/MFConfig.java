package com.morefireworks;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The safety limits that are worth tuning at runtime.
 *
 * <p>The structural limits (8 colours, 7/6/5 stages, 1024-character specs) are compile-time
 * constants on purpose: they are part of the data format, and letting a config file change them
 * would make saved fireworks unreadable. The limits here only affect how many staged detonations
 * may run at once and how fast they play, which is exactly the kind of thing a server owner needs
 * to tune for their hardware.</p>
 */
public final class MFConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MAX_ACTIVE_TIMELINES = BUILDER
            .comment("How many multi-stage detonations may be in flight at the same time.",
                    "Launches beyond this limit still fire their first stage and simply drop the rest,",
                    "which is preferable to stalling the server.")
            .defineInRange("maxActiveTimelines", 64, 1, 4096);

    public static final ModConfigSpec.IntValue STAGE_INTERVAL_TICKS = BUILDER
            .comment("Ticks between two staged explosions of the same rocket.")
            .defineInRange("stageIntervalTicks", 6, 1, 100);

    public static final ModConfigSpec.IntValue STAGE_TIMEOUT_TICKS = BUILDER
            .comment("Ticks after which a staged detonation finishes even if the rocket entity",
                    "never disappears (for example because it was unloaded or removed by a command).")
            .defineInRange("stageTimeoutTicks", 200, 20, 6000);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private MFConfig() {
    }
}
