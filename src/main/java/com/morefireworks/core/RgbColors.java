package com.morefireworks.core;

import java.util.Arrays;

/**
 * Small helpers for the 24-bit RGB colour space used by firework stars.
 */
public final class RgbColors {
    /** Number of distinct 24-bit colours, i.e. 2^24. */
    public static final int COLOR_COUNT = 0x1_000_000;
    public static final int MAX_COMPONENT = 0xFF;

    private RgbColors() {
    }

    public static boolean isValid(int rgb) {
        return rgb >= 0 && rgb < COLOR_COUNT;
    }

    public static int of(int red, int green, int blue) {
        checkComponent(red, "red");
        checkComponent(green, "green");
        checkComponent(blue, "blue");
        return (red << 16) | (green << 8) | blue;
    }

    public static int red(int rgb) {
        return (rgb >> 16) & MAX_COMPONENT;
    }

    public static int green(int rgb) {
        return (rgb >> 8) & MAX_COMPONENT;
    }

    public static int blue(int rgb) {
        return rgb & MAX_COMPONENT;
    }

    /** Formats a colour as {@code #RRGGBB}. */
    public static String toHex(int rgb) {
        return String.format(java.util.Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }

    /**
     * Parses {@code #RRGGBB}, {@code RRGGBB} or {@code 0xRRGGBB}.
     *
     * @throws IllegalArgumentException if the text is not a valid 24-bit colour
     */
    public static int parse(String text) {
        String normalized = text.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        } else if (normalized.startsWith("0x") || normalized.startsWith("0X")) {
            normalized = normalized.substring(2);
        }
        if (normalized.length() != 6) {
            throw new IllegalArgumentException("Colour must have exactly 6 hex digits: " + text);
        }
        int value = 0;
        for (int i = 0; i < 6; i++) {
            int digit = Character.digit(normalized.charAt(i), 16);
            if (digit < 0) {
                throw new IllegalArgumentException("Invalid hex digit in colour: " + text);
            }
            value = (value << 4) | digit;
        }
        return value;
    }

    private static void checkComponent(int component, String name) {
        if (component < 0 || component > MAX_COMPONENT) {
            throw new IllegalArgumentException(name + " channel out of range 0..255: " + component);
        }
    }

    /**
     * Sorts a colour array copy; only used by tests and diagnostics, the mod itself always keeps
     * the author-provided order.
     */
    public static int[] sortedCopy(int[] colors) {
        int[] copy = Arrays.copyOf(colors, colors.length);
        Arrays.sort(copy);
        return copy;
    }
}
