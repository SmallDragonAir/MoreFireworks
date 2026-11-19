package com.morefireworks.core;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FireworkCombinatoricsTest {

    @Test
    void colourSpaceIsTwoToTheTwentyFour() {
        assertEquals(BigInteger.valueOf(16_777_216L), FireworkCombinatorics.R);
        assertEquals(0x1_000_000, RgbColors.COLOR_COUNT);
    }

    @Test
    void geometricSumMatchesTheNaiveSum() {
        BigInteger base = BigInteger.valueOf(7);
        BigInteger expected = BigInteger.ZERO;
        for (int i = 1; i <= 5; i++) {
            expected = expected.add(base.pow(i));
        }
        assertEquals(expected, FireworkCombinatorics.geometricSum(base, 1, 5));
        assertEquals(BigInteger.ONE, FireworkCombinatorics.geometricSum(base, 0, 0));
        assertEquals(BigInteger.ZERO, FireworkCombinatorics.geometricSum(base, 3, 2));
    }

    @Test
    void starCountMatchesTheClosedForm() {
        BigInteger r = FireworkCombinatorics.R;
        BigInteger initial = BigInteger.ZERO;
        for (int i = 1; i <= 8; i++) {
            initial = initial.add(r.pow(i));
        }
        BigInteger fade = BigInteger.ZERO;
        for (int j = 0; j <= 8; j++) {
            fade = fade.add(r.pow(j));
        }
        BigInteger expected = BigInteger.valueOf(5L * 2L * 2L).multiply(initial).multiply(fade);
        assertEquals(expected, FireworkCombinatorics.starCount());
    }

    @Test
    void rocketPerFlightRespectsTheStarCap() {
        BigInteger stars = FireworkCombinatorics.starCount();
        BigInteger expected = BigInteger.ZERO;
        BigInteger power = BigInteger.ONE;
        for (int n = 1; n <= 7; n++) {
            power = power.multiply(stars);
            if (n <= 7) {
                // flight 1 allows 7 stars
                expected = expected.add(power);
            }
        }
        assertEquals(expected, FireworkCombinatorics.rocketCountForFlight(1));

        // Flight 3 allows only 5 stars, so it must be strictly smaller.
        assertTrue(FireworkCombinatorics.rocketCountForFlight(3)
                .compareTo(FireworkCombinatorics.rocketCountForFlight(1)) < 0);
    }

    @Test
    void totalIsTheSumOfItsParts() {
        BigInteger sum = BigInteger.ZERO;
        for (int flight = 1; flight <= 3; flight++) {
            sum = sum.add(FireworkCombinatorics.rocketCountForFlight(flight));
        }
        assertEquals(sum, FireworkCombinatorics.rocketCount());
        assertEquals(sum.add(BigInteger.valueOf(3)), FireworkCombinatorics.totalItemVariants());
        assertEquals(BigInteger.valueOf(3), FireworkCombinatorics.plainRocketCount());
    }

    @Test
    void totalHasHundredsOfDigits() {
        FireworkCombinatorics.Report report = FireworkCombinatorics.report();
        int digits = report.getTotalDecimalDigits();
        assertTrue(digits > 200, "expected a very large variant space, got " + digits + " digits");
        assertTrue(report.scientific().contains("e+"), report.scientific());
    }

    @Test
    void scientificNotationIsExactEnough() {
        assertEquals("1.0000e+0", FireworkCombinatorics.scientific(BigInteger.ONE));
        assertEquals("1.0000e+3", FireworkCombinatorics.scientific(BigInteger.valueOf(1000)));
        assertEquals("1.2346e+5", FireworkCombinatorics.scientific(BigInteger.valueOf(123456)));
        // Rounding must carry into the next decade instead of printing 10.0000e+5.
        assertEquals("1.0000e+6", FireworkCombinatorics.scientific(BigInteger.valueOf(999_999)));
        // A 819 digit number still renders in constant time.
        assertTrue(FireworkCombinatorics.scientific(FireworkCombinatorics.rocketCount()).startsWith("1.8873e+818"));
    }

    @Test
    void visuallyDistinctCountIsSmallerButPositive() {
        assertTrue(FireworkCombinatorics.visuallyDistinctStarCount().signum() > 0);
        assertTrue(FireworkCombinatorics.visuallyDistinctStarCount()
                .compareTo(FireworkCombinatorics.starCount()) < 0);
        assertTrue(FireworkCombinatorics.visuallyDistinctRocketCount().signum() > 0);
    }

    @Test
    void multisetCountMatchesPascalsIdentityForSmallCases() {
        // C(R + k - 1, k) for k = 1 is exactly R.
        assertEquals(FireworkCombinatorics.R, FireworkCombinatorics.multisetCount(1));
        // For k = 2 it is R(R+1)/2.
        BigInteger r = FireworkCombinatorics.R;
        assertEquals(r.multiply(r.add(BigInteger.ONE)).divide(BigInteger.TWO),
                FireworkCombinatorics.multisetCount(2));
    }

    @Test
    void reportFieldsAreConsistent() {
        FireworkCombinatorics.Report report = FireworkCombinatorics.report();
        assertEquals(FireworkCombinatorics.starCount(), report.starsPerType());
        assertEquals(FireworkCombinatorics.totalItemVariants(), report.totalItems());
        assertEquals(FireworkCombinatorics.orderedInitialColorLists().multiply(
                FireworkCombinatorics.orderedFadeColorLists()).multiply(BigInteger.valueOf(20)),
                report.starsPerType());
    }

    @Test
    void limitsAreRespected() {
        assertEquals(7, RocketSpec.maxStars(1));
        assertEquals(6, RocketSpec.maxStars(2));
        assertEquals(5, RocketSpec.maxStars(3));
        assertThrows(IllegalArgumentException.class,
                () -> RocketSpec.of(3, List.of(new StarSpec(StarShape.STAR, List.of(1), List.of(), false, false),
                        new StarSpec(StarShape.STAR, List.of(1), List.of(), false, false),
                        new StarSpec(StarShape.STAR, List.of(1), List.of(), false, false),
                        new StarSpec(StarShape.STAR, List.of(1), List.of(), false, false),
                        new StarSpec(StarShape.STAR, List.of(1), List.of(), false, false),
                        new StarSpec(StarShape.STAR, List.of(1), List.of(), false, false))));
    }
}
