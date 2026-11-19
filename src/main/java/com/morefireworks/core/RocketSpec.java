package com.morefireworks.core;

import java.util.List;

/**
 * A complete firework rocket description: a flight duration plus an ordered sequence of stars.
 *
 * <p>This is the single source of truth for the mod. The vanilla {@code Fireworks} data component
 * is always <em>derived</em> from this record when an item is created, which guarantees that the
 * vanilla rendering path and the mod's own naming/counting path can never disagree.
 *
 * <p>An empty star list is legal and describes a plain rocket without any explosion. The
 * combinatorics module counts those separately, exactly as the specification requires.
 *
 * @param flight rocket flight level, 1..{@value #MAX_FLIGHT}
 * @param stars  ordered explosion sequence, 0..{@link #maxStars(int)} entries
 */
public record RocketSpec(int flight, List<StarSpec> stars) {

    public static final int MIN_FLIGHT = 1;
    public static final int MAX_FLIGHT = 3;

    public RocketSpec {
        if (flight < MIN_FLIGHT || flight > MAX_FLIGHT) {
            throw new IllegalArgumentException(
                    "flight must be " + MIN_FLIGHT + ".." + MAX_FLIGHT + ", got " + flight);
        }
        if (stars == null) {
            throw new IllegalArgumentException("stars must not be null");
        }
        int max = maxStars(flight);
        if (stars.size() > max) {
            throw new IllegalArgumentException("flight " + flight + " allows at most " + max
                    + " stars, got " + stars.size());
        }
        for (int i = 0; i < stars.size(); i++) {
            if (stars.get(i) == null) {
                throw new IllegalArgumentException("stars[" + i + "] is null");
            }
        }
        stars = List.copyOf(stars);
    }

    /**
     * The crafting-table constraint from vanilla: a rocket of flight level {@code d} can hold at
     * most {@code 8 - d} stars, i.e. 7 / 6 / 5 for levels 1 / 2 / 3.
     */
    public static int maxStars(int flight) {
        if (flight < MIN_FLIGHT || flight > MAX_FLIGHT) {
            throw new IllegalArgumentException("flight out of range: " + flight);
        }
        return 8 - flight;
    }

    public boolean isEmpty() {
        return stars.isEmpty();
    }

    public int starCount() {
        return stars.size();
    }

    /**
     * Builds a "canonical" rocket: the star list is kept in the given order (order is data, not a
     * presentation detail), so this simply validates and copies.
     */
    public static RocketSpec of(int flight, List<StarSpec> stars) {
        return new RocketSpec(flight, stars);
    }

    public static RocketSpec plain(int flight) {
        return new RocketSpec(flight, List.of());
    }

    public String describe() {
        StringBuilder sb = new StringBuilder();
        sb.append("flight=").append(flight).append(" stars=[");
        for (int i = 0; i < stars.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(stars.get(i).describe());
        }
        return sb.append(']').toString();
    }
}
