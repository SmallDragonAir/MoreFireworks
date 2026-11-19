package com.morefireworks.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Canonical text form of a {@link RocketSpec}.
 *
 * <p>This is the <b>one</b> disk and network representation: a single ASCII string. Keeping the
 * representation textual rather than mirroring Minecraft's nested {@code Fireworks} structure has
 * three benefits:</p>
 * <ul>
 *   <li>it is trivially deterministic — equal specs always produce the same bytes;</li>
 *   <li>it survives Minecraft version changes without data fixing, because it does not depend on
 *       any vanilla codec;</li>
 *   <li>it is human readable in {@code /data get} output, which makes bug reports useful.</li>
 * </ul>
 *
 * <h2>Grammar</h2>
 * <pre>
 *   spec   := "v1;" flight ";" star ("|" star)*        (empty star list allowed: "v1;2;")
 *   flight := "1" | "2" | "3"
 *   star   := shape ":" colors ":" fades ":" flags
 *   shape  := small_ball | large_ball | star | creeper | burst
 *   colors := hex ("," hex)*        ; 1..8 entries
 *   fades  := hex ("," hex)*        ; 0..8 entries
 *   hex    := 6 lowercase hex digits
 *   flags  := ( "f" | "t" )*        ; 'f' = flicker, 't' = trail, canonical order f then t
 * </pre>
 *
 * Parsing is strict: anything outside the grammar, out of range, or beyond the configured limits is
 * rejected with an {@link IllegalArgumentException} instead of being silently repaired, so corrupt
 * or hostile data components can never enter the world state.
 */
public final class SpecCodec {

    /** Bumped only if the grammar changes incompatibly. */
    public static final String VERSION = "v1";

    private static final char FIELD = ';';
    private static final char STAR = '|';
    private static final char PART = ':';
    private static final char COLOR = ',';

    private SpecCodec() {
    }

    // ------------------------------------------------------------------
    // Encoding
    // ------------------------------------------------------------------

    public static String encode(RocketSpec spec) {
        StringBuilder sb = new StringBuilder(64);
        sb.append(VERSION).append(FIELD).append(spec.flight()).append(FIELD);
        for (int i = 0; i < spec.stars().size(); i++) {
            if (i > 0) {
                sb.append(STAR);
            }
            StarSpec star = spec.stars().get(i);
            sb.append(star.shape().id()).append(PART);
            appendColors(sb, star.colors());
            sb.append(PART);
            appendColors(sb, star.fadeColors());
            sb.append(PART);
            if (star.flicker()) {
                sb.append('f');
            }
            if (star.trail()) {
                sb.append('t');
            }
        }
        return sb.toString();
    }

    private static void appendColors(StringBuilder sb, List<Integer> colors) {
        for (int i = 0; i < colors.size(); i++) {
            if (i > 0) {
                sb.append(COLOR);
            }
            sb.append(String.format("%06x", colors.get(i) & 0xFFFFFF));
        }
    }

    // ------------------------------------------------------------------
    // Decoding
    // ------------------------------------------------------------------

    public static RocketSpec decode(String text) {
        if (text == null) {
            throw new IllegalArgumentException("spec text is null");
        }
        if (text.length() > FireworkLimits.MAX_SPEC_CHARS) {
            throw new IllegalArgumentException("spec text too long: " + text.length());
        }
        int firstField = text.indexOf(FIELD);
        if (firstField < 0 || !VERSION.equals(text.substring(0, firstField))) {
            throw new IllegalArgumentException("unsupported spec version");
        }
        int secondField = text.indexOf(FIELD, firstField + 1);
        if (secondField < 0) {
            throw new IllegalArgumentException("missing star section");
        }
        int flight = parseInt(text.substring(firstField + 1, secondField), "flight");
        String starSection = text.substring(secondField + 1);

        List<StarSpec> stars = new ArrayList<>();
        if (!starSection.isEmpty()) {
            for (String starText : starSection.split("\\" + STAR, -1)) {
                stars.add(decodeStar(starText));
            }
        }
        return RocketSpec.of(flight, stars);
    }

    /** Encodes a single star as {@code shape:colors:fades:flags}, for the star item component. */
    public static String encodeStar(StarSpec star) {
        StringBuilder sb = new StringBuilder(32);
        sb.append(star.shape().id()).append(PART);
        appendColors(sb, star.colors());
        sb.append(PART);
        appendColors(sb, star.fadeColors());
        sb.append(PART);
        if (star.flicker()) {
            sb.append('f');
        }
        if (star.trail()) {
            sb.append('t');
        }
        return sb.toString();
    }

    public static StarSpec decodeStar(String text) {
        if (text == null || text.length() > FireworkLimits.MAX_SPEC_CHARS) {
            throw new IllegalArgumentException("bad star text");
        }
        return decodeStarFields(text);
    }

    private static StarSpec decodeStarFields(String text) {
        String[] parts = text.split(String.valueOf(PART), -1);
        if (parts.length != 4) {
            throw new IllegalArgumentException("star must have 4 fields: " + text);
        }
        StarShape shape = StarShape.byId(parts[0]);
        List<Integer> colors = decodeColors(parts[1]);
        List<Integer> fades = decodeColors(parts[2]);
        String flags = parts[3];
        boolean flicker = false;
        boolean trail = false;
        for (int i = 0; i < flags.length(); i++) {
            switch (flags.charAt(i)) {
                case 'f' -> flicker = true;
                case 't' -> trail = true;
                default -> throw new IllegalArgumentException("unknown flag: " + flags.charAt(i));
            }
        }
        return new StarSpec(shape, colors, fades, flicker, trail);
    }

    private static List<Integer> decodeColors(String text) {
        List<Integer> colors = new ArrayList<>();
        if (text.isEmpty()) {
            return colors;
        }
        for (String hex : text.split(String.valueOf(COLOR), -1)) {
            colors.add(parseColor(hex));
        }
        return colors;
    }

    private static int parseColor(String hex) {
        if (hex.length() != 6) {
            throw new IllegalArgumentException("colour must be 6 hex digits: " + hex);
        }
        int value = 0;
        for (int i = 0; i < 6; i++) {
            int digit = Character.digit(hex.charAt(i), 16);
            if (digit < 0) {
                throw new IllegalArgumentException("bad hex digit in colour: " + hex);
            }
            value = (value << 4) | digit;
        }
        return value;
    }

    private static int parseInt(String text, String what) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("bad " + what + ": " + text);
        }
    }
}
