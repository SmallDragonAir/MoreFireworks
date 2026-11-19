package com.morefireworks.core;

import java.util.List;

/**
 * One firework star: the smallest unit of the parameter space.
 *
 * <p>The record is an immutable, canonical value. Two stars are equal exactly when all five
 * parameters are equal — the order of the colour lists is significant and is preserved, because
 * the author chose it and because the naming system reports it.
 *
 * @param shape      the explosion shape
 * @param colors     1..{@value #MAX_COLORS} ordered 24-bit initial colours
 * @param fadeColors 0..{@value #MAX_COLORS} ordered 24-bit fade (gradient) colours
 * @param flicker    whether the star twinkles
 * @param trail      whether the star leaves a trail
 */
public record StarSpec(
        StarShape shape,
        List<Integer> colors,
        List<Integer> fadeColors,
        boolean flicker,
        boolean trail) {

    public static final int MIN_COLORS = 1;
    public static final int MAX_COLORS = 8;
    public static final int MIN_FADE_COLORS = 0;
    public static final int MAX_FADE_COLORS = 8;

    public StarSpec {
        if (shape == null) {
            throw new IllegalArgumentException("shape must not be null");
        }
        colors = validateColors(colors, "colors", MIN_COLORS, MAX_COLORS);
        fadeColors = validateColors(fadeColors, "fadeColors", MIN_FADE_COLORS, MAX_FADE_COLORS);
    }

    private static List<Integer> validateColors(List<Integer> input, String name, int min, int max) {
        if (input == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        if (input.size() < min || input.size() > max) {
            throw new IllegalArgumentException(
                    name + " must contain " + min + ".." + max + " entries, got " + input.size());
        }
        for (int i = 0; i < input.size(); i++) {
            Integer value = input.get(i);
            if (value == null) {
                throw new IllegalArgumentException(name + "[" + i + "] is null");
            }
            if (!RgbColors.isValid(value)) {
                throw new IllegalArgumentException(
                        name + "[" + i + "] is not a 24-bit RGB colour: " + value);
            }
        }
        return List.copyOf(input);
    }

    public static StarSpec of(StarShape shape, int... colors) {
        Integer[] boxed = new Integer[colors.length];
        for (int i = 0; i < colors.length; i++) {
            boxed[i] = colors[i];
        }
        return new StarSpec(shape, List.of(boxed), List.of(), false, false);
    }

    public StarSpec withFadeColors(int... fade) {
        Integer[] boxed = new Integer[fade.length];
        for (int i = 0; i < fade.length; i++) {
            boxed[i] = fade[i];
        }
        return new StarSpec(shape, colors, List.of(boxed), flicker, trail);
    }

    public StarSpec withFlicker(boolean value) {
        return new StarSpec(shape, colors, fadeColors, value, trail);
    }

    public StarSpec withTrail(boolean value) {
        return new StarSpec(shape, colors, fadeColors, flicker, value);
    }

    public boolean hasFadeColors() {
        return !fadeColors.isEmpty();
    }

    /**
     * A stable, compact textual form used for logging, test names and for the component's
     * human-readable mirror. Not used for identity.
     */
    public String describe() {
        StringBuilder sb = new StringBuilder();
        sb.append(shape.id()).append('(');
        appendColors(sb, colors);
        if (!fadeColors.isEmpty()) {
            sb.append("->");
            appendColors(sb, fadeColors);
        }
        if (flicker) {
            sb.append(";flicker");
        }
        if (trail) {
            sb.append(";trail");
        }
        return sb.append(')').toString();
    }

    private static void appendColors(StringBuilder sb, List<Integer> list) {
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append('/');
            }
            sb.append(RgbColors.toHex(list.get(i)));
        }
    }
}
