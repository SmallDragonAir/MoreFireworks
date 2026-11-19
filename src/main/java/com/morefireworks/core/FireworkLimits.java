package com.morefireworks.core;

/**
 * Hard safety limits. Every value here is a bound on <em>work per object</em>, never on the number
 * of possible objects: the variant space stays astronomically large while any single firework
 * remains cheap to store, name, sync and render.
 *
 * <p>The numbers are deliberately small. A malicious or corrupted data component can request at
 * most {@link #MAX_STARS} explosions of {@link #MAX_COLORS} + {@link #MAX_FADE_COLORS} colours,
 * which bounds both the packet size and the particle burst.</p>
 */
public final class FireworkLimits {

    /** Longest canonical spec string we will ever parse or produce. */
    public static final int MAX_SPEC_CHARS = 1024;

    /** Hard cap on explosions per rocket, independent of the flight tier cap. */
    public static final int MAX_STARS = 7;

    /** Initial colours per star (vanilla supports 8). */
    public static final int MAX_COLORS = StarSpec.MAX_COLORS;

    /** Fade colours per star. */
    public static final int MAX_FADE_COLORS = StarSpec.MAX_FADE_COLORS;

    /** Highest flight tier. */
    public static final int MAX_FLIGHT = RocketSpec.MAX_FLIGHT;

    /** Ticks between consecutive staged explosions, so the order is actually visible. */
    public static final int STAGE_INTERVAL_TICKS = 6;

    /** Extra ticks of grace before a stage fires if the rocket entity is still alive. */
    public static final int STAGE_TIMEOUT_TICKS = 200;

    /** Max simultaneous staged rocket timelines a single level may track. */
    public static final int MAX_ACTIVE_TIMELINES = 64;

    /** Max characters rendered in a tooltip before it is split into a wrapped block. */
    public static final int MAX_TOOLTIP_CHARS = 1024;

    private FireworkLimits() {
    }
}
