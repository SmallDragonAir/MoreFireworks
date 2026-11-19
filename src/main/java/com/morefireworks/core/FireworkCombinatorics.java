package com.morefireworks.core;

import java.math.BigInteger;

/**
 * Exact combinatorial model of the More Fireworks parameter space.
 *
 * <h2>Single star</h2>
 * With {@code R = 2^24} colours, a star is determined by
 * <ul>
 *   <li>5 explosion shapes,</li>
 *   <li>2 flicker states,</li>
 *   <li>2 trail states,</li>
 *   <li>an ordered list of 1..8 initial colours,</li>
 *   <li>an ordered list of 0..8 fade colours.</li>
 * </ul>
 * Hence
 * <pre>
 *   E = 5 * 2 * 2 * (Σ<sub>i=1..8</sub> R^i) * (Σ<sub>j=0..8</sub> R^j)
 * </pre>
 *
 * <h2>Rocket</h2>
 * A rocket of flight level {@code d} holds at most {@code 8 - d} stars and the order of the stars
 * is significant, so with repetition allowed the number of rockets of level {@code d} carrying at
 * least one star is {@code Σ<sub>n=1..8-d</sub> E^n}, and
 * <pre>
 *   T = Σ<sub>d=1..3</sub> Σ<sub>n=1..8-d</sub> E^n
 * </pre>
 * Plain rockets (no star at all) contribute exactly {@code 3} more variants (one per flight
 * level) and are reported separately.
 *
 * <p>Nothing here ever enumerates a combination: every value is computed with {@link BigInteger}
 * arithmetic from closed forms, so the numbers stay exact no matter how large they get.
 */
public final class FireworkCombinatorics {

    /** Number of distinct 24-bit colours: 2^24. */
    public static final BigInteger R = BigInteger.valueOf(RgbColors.COLOR_COUNT);

    public static final int MAX_INITIAL_COLORS = StarSpec.MAX_COLORS;
    public static final int MAX_FADE_COLORS = StarSpec.MAX_FADE_COLORS;
    public static final int SHAPE_COUNT = StarShape.values().length;
    public static final int FLICKER_COUNT = 2;
    public static final int TRAIL_COUNT = 2;

    private FireworkCombinatorics() {
    }

    // ------------------------------------------------------------------
    // Building blocks
    // ------------------------------------------------------------------

    /**
     * Computes {@code Σ_{i=from..to} base^i} exactly.
     *
     * <p>Uses the closed form {@code (base^(to+1) - base^from) / (base - 1)} so the cost is
     * logarithmic in {@code to} rather than linear.
     */
    public static BigInteger geometricSum(BigInteger base, int from, int to) {
        if (from > to) {
            return BigInteger.ZERO;
        }
        if (base.signum() == 0) {
            return from == 0 ? BigInteger.ONE : BigInteger.ZERO;
        }
        BigInteger numerator = base.pow(to + 1).subtract(base.pow(from));
        return numerator.divide(base.subtract(BigInteger.ONE));
    }

    /** Number of ordered initial-colour lists of length 1..8: {@code Σ_{i=1..8} R^i}. */
    public static BigInteger orderedInitialColorLists() {
        return geometricSum(R, StarSpec.MIN_COLORS, MAX_INITIAL_COLORS);
    }

    /** Number of ordered fade-colour lists of length 0..8: {@code Σ_{j=0..8} R^j}. */
    public static BigInteger orderedFadeColorLists() {
        return geometricSum(R, StarSpec.MIN_FADE_COLORS, MAX_FADE_COLORS);
    }

    /** The per-star combination count {@code E}. */
    public static BigInteger starCount() {
        return BigInteger.valueOf((long) SHAPE_COUNT * FLICKER_COUNT * TRAIL_COUNT)
                .multiply(orderedInitialColorLists())
                .multiply(orderedFadeColorLists());
    }

    /** Number of rockets of one flight level that carry at least one star. */
    public static BigInteger rocketCountForFlight(int flight) {
        int maxStars = RocketSpec.maxStars(flight);
        BigInteger stars = starCount();
        BigInteger total = BigInteger.ZERO;
        BigInteger current = BigInteger.ONE;
        for (int n = 1; n <= maxStars; n++) {
            current = current.multiply(stars);
            total = total.add(current);
        }
        return total;
    }

    /** {@code T}: all rockets that carry at least one star, summed over the three flight levels. */
    public static BigInteger rocketCount() {
        BigInteger total = BigInteger.ZERO;
        for (int flight = RocketSpec.MIN_FLIGHT; flight <= RocketSpec.MAX_FLIGHT; flight++) {
            total = total.add(rocketCountForFlight(flight));
        }
        return total;
    }

    /** Number of plain rockets without any explosion (one per flight level). */
    public static BigInteger plainRocketCount() {
        return BigInteger.valueOf(RocketSpec.MAX_FLIGHT - RocketSpec.MIN_FLIGHT + 1L);
    }

    /** Everything a player can hold in their hand: star-carrying rockets plus plain rockets. */
    public static BigInteger totalItemVariants() {
        return rocketCount().add(plainRocketCount());
    }

    // ------------------------------------------------------------------
    // Visual equivalence model
    // ------------------------------------------------------------------

    /**
     * Number of colour multisets of size {@code 0..maxLen} drawn from {@code R} colours.
     *
     * <p>A vanilla explosion picks a random colour from its list for every single particle, so the
     * <em>order</em> of the list is not observable, while the <em>multiplicity</em> of a colour is
     * (it changes how often that colour appears). The observable object is therefore a multiset,
     * and the number of multisets of size {@code k} is {@code C(R + k - 1, k)}.
     */
    public static BigInteger colorMultisets(int minLen, int maxLen) {
        BigInteger total = BigInteger.ZERO;
        for (int k = minLen; k <= maxLen; k++) {
            total = total.add(multisetCount(k));
        }
        return total;
    }

    /** {@code C(R + k - 1, k)}, computed with the small-{@code k} product form. */
    public static BigInteger multisetCount(int k) {
        BigInteger result = BigInteger.ONE;
        for (int i = 1; i <= k; i++) {
            result = result.multiply(R.add(BigInteger.valueOf(i - 1L)))
                    .divide(BigInteger.valueOf(i));
        }
        return result;
    }

    /**
     * Number of <em>visually distinguishable</em> star effects.
     *
     * <p>Equal to {@code E} except that the ordered colour lists are replaced by colour multisets.
     */
    public static BigInteger visuallyDistinctStarCount() {
        return BigInteger.valueOf((long) SHAPE_COUNT * FLICKER_COUNT * TRAIL_COUNT)
                .multiply(colorMultisets(StarSpec.MIN_COLORS, MAX_INITIAL_COLORS))
                .multiply(colorMultisets(StarSpec.MIN_FADE_COLORS, MAX_FADE_COLORS));
    }

    /** Number of visually distinguishable star-carrying rockets (same model as {@link #rocketCount()}). */
    public static BigInteger visuallyDistinctRocketCount() {
        BigInteger stars = visuallyDistinctStarCount();
        BigInteger total = BigInteger.ZERO;
        for (int flight = RocketSpec.MIN_FLIGHT; flight <= RocketSpec.MAX_FLIGHT; flight++) {
            BigInteger current = BigInteger.ONE;
            for (int n = 1; n <= RocketSpec.maxStars(flight); n++) {
                current = current.multiply(stars);
                total = total.add(current);
            }
        }
        return total;
    }

    // ------------------------------------------------------------------
    // Reporting
    // ------------------------------------------------------------------

    /** A full snapshot of every number the {@code /morefireworks count} command prints. */
    public record Report(
            BigInteger colorSpace,
            int maxInitialColors,
            int maxFadeColors,
            BigInteger orderedInitialLists,
            BigInteger orderedFadeLists,
            BigInteger starsPerType,
            BigInteger rocketPerFlight1,
            BigInteger rocketPerFlight2,
            BigInteger rocketPerFlight3,
            BigInteger starRockets,
            BigInteger plainRockets,
            BigInteger totalItems,
            BigInteger visualStars,
            BigInteger visualRockets) {

        public int getTotalDecimalDigits() {
            return decimalDigits(totalItems);
        }

        /** {@code totalItems} in scientific notation, e.g. {@code 3.71e+302}. */
        public String scientific() {
            return FireworkCombinatorics.scientific(totalItems);
        }
    }

    /** Number of decimal digits of a positive value (1 for zero). */
    public static int decimalDigits(BigInteger value) {
        return value.abs().toString().length();
    }

    /**
     * Scientific notation with four significant digits, computed without ever converting the whole
     * number: only the leading eighteen digits are needed for the mantissa, and the exponent follows
     * from the digit count.
     */
    public static String scientific(BigInteger value) {
        if (value.signum() == 0) {
            return "0";
        }
        String digits = value.abs().toString();
        int exponent = digits.length() - 1;
        String head = digits.substring(0, Math.min(18, digits.length()));
        double mantissa = Double.parseDouble(head) / Math.pow(10, head.length() - 1);
        String formatted = String.format(java.util.Locale.ROOT, "%.4f", mantissa);
        if (formatted.startsWith("10")) {
            // Rounding carried into a new decade, e.g. 9.99999 -> 10.0000.
            formatted = "1.0000";
            exponent++;
        }
        return formatted + "e" + (exponent >= 0 ? "+" : "") + exponent;
    }

    public static Report report() {
        BigInteger starCount = starCount();
        return new Report(
                R,
                MAX_INITIAL_COLORS,
                MAX_FADE_COLORS,
                orderedInitialColorLists(),
                orderedFadeColorLists(),
                starCount,
                rocketCountForFlight(1),
                rocketCountForFlight(2),
                rocketCountForFlight(3),
                rocketCount(),
                plainRocketCount(),
                totalItemVariants(),
                visuallyDistinctStarCount(),
                visuallyDistinctRocketCount());
    }
}
