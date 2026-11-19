package com.morefireworks.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SpecCodecTest {

    private static RocketSpec sample() {
        return RocketSpec.of(2, List.of(
                new StarSpec(StarShape.STAR, List.of(0xFF0000, 0x00FF00), List.of(0x0000FF), true, false),
                new StarSpec(StarShape.CREEPER, List.of(0x123456), List.of(), false, true),
                new StarSpec(StarShape.BURST, List.of(0xFFFFFF, 0x000000, 0x808080),
                        List.of(0x111111, 0x222222), true, true)));
    }

    @Test
    void roundTripsExactly() {
        RocketSpec spec = sample();
        String text = SpecCodec.encode(spec);
        assertEquals(spec, SpecCodec.decode(text));
    }

    @Test
    void encodingIsCanonical() {
        RocketSpec spec = sample();
        String once = SpecCodec.encode(spec);
        String twice = SpecCodec.encode(SpecCodec.decode(once));
        assertEquals(once, twice);
        assertTrue(once.startsWith("v1;2;"));
    }

    @Test
    void plainRocketRoundTrips() {
        RocketSpec plain = RocketSpec.plain(3);
        assertEquals(plain, SpecCodec.decode(SpecCodec.encode(plain)));
    }

    @Test
    void starOrderIsPreserved() {
        RocketSpec a = RocketSpec.of(1, List.of(
                new StarSpec(StarShape.STAR, List.of(0xFF0000), List.of(), false, false),
                new StarSpec(StarShape.LARGE_BALL, List.of(0x0000FF), List.of(), false, false)));
        RocketSpec b = RocketSpec.of(1, List.of(
                new StarSpec(StarShape.LARGE_BALL, List.of(0x0000FF), List.of(), false, false),
                new StarSpec(StarShape.STAR, List.of(0xFF0000), List.of(), false, false)));
        assertNotEquals(SpecCodec.encode(a), SpecCodec.encode(b));
        assertEquals(a, SpecCodec.decode(SpecCodec.encode(a)));
        assertEquals(b, SpecCodec.decode(SpecCodec.encode(b)));
    }

    @Test
    void colourOrderIsPreserved() {
        RocketSpec a = RocketSpec.of(1, List.of(
                new StarSpec(StarShape.STAR, List.of(0xFF0000, 0x00FF00), List.of(), false, false)));
        RocketSpec b = RocketSpec.of(1, List.of(
                new StarSpec(StarShape.STAR, List.of(0x00FF00, 0xFF0000), List.of(), false, false)));
        assertNotEquals(SpecCodec.encode(a), SpecCodec.encode(b));
    }

    @Test
    void rejectsGarbage() {
        assertThrows(IllegalArgumentException.class, () -> SpecCodec.decode(""));
        assertThrows(IllegalArgumentException.class, () -> SpecCodec.decode("v2;1;"));
        assertThrows(IllegalArgumentException.class, () -> SpecCodec.decode("v1;9;"));
        assertThrows(IllegalArgumentException.class, () -> SpecCodec.decode("v1;1;nonsense"));
        assertThrows(IllegalArgumentException.class, () -> SpecCodec.decode("v1;1;star:zzzzzz::"));
        assertThrows(IllegalArgumentException.class, () -> SpecCodec.decode("v1;1;star:ff0000::x"));
        assertThrows(IllegalArgumentException.class, () -> SpecCodec.decode(null));
    }

    @Test
    void rejectsTooManyStarsForFlightTier() {
        StarSpec star = new StarSpec(StarShape.STAR, List.of(0xFF0000), List.of(), false, false);
        // Flight 1 allows at most 7 stars; seven is accepted, eight must be rejected.
        RocketSpec.of(1, List.of(star, star, star, star, star, star, star));
        String eight = "v1;1;" + String.join("|", java.util.Collections.nCopies(8, "star:ff0000::"));
        assertThrows(IllegalArgumentException.class, () -> SpecCodec.decode(eight));
    }

    @Test
    void rejectsOversizedInput() {
        String huge = "v1;1;" + "star:ff0000::|".repeat(1000);
        assertThrows(IllegalArgumentException.class, () -> SpecCodec.decode(huge));
    }

    @Test
    void starEncodingRoundTrips() {
        StarSpec star = new StarSpec(StarShape.LARGE_BALL, List.of(0xABCDEF), List.of(0x012345, 0x6789AB),
                true, true);
        assertEquals(star, SpecCodec.decodeStar(SpecCodec.encodeStar(star)));
    }
}
