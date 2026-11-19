package com.morefireworks.naming;

import com.morefireworks.core.RocketSpec;
import com.morefireworks.core.StarShape;
import com.morefireworks.core.StarSpec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds the in-game display form of a name as a tree of {@link Component}s.
 *
 * <p>Everything that has a translation key is emitted as a {@code translatable} component, so the
 * client's own language selection decides between Chinese and English and switching languages
 * updates every existing stack immediately. Only genuinely language neutral data — numbers,
 * position ranges, {@code #RRGGBB} colours — is emitted as literal text.</p>
 *
 * <p>This mirrors {@link FireworkNamer}, which produces the plain-text form used by commands,
 * logging and unit tests.</p>
 */
public final class NameComponents {

    private static final ChatFormatting MUTED = ChatFormatting.DARK_GRAY;
    private static final ChatFormatting ACCENT = ChatFormatting.AQUA;

    private NameComponents() {
    }

    // ------------------------------------------------------------------
    // Rocket names
    // ------------------------------------------------------------------

    /** The compact name shown as the item title. */
    public static MutableComponent shortName(RocketSpec spec) {
        MutableComponent out = Component.empty();
        out.append(tier(spec.flight()));
        if (spec.isEmpty()) {
            out.append(Component.literal(" ")).append(Component.translatable("morefireworks.suffix.plain"));
            return out;
        }

        int flicker = 0;
        int trail = 0;
        for (StarSpec star : spec.stars()) {
            if (star.flicker()) {
                flicker++;
            }
            if (star.trail()) {
                trail++;
            }
        }
        if (flicker > 0) {
            out.append(Component.literal("-")).append(modifier(flicker, "morefireworks.effect.flicker"));
        }
        if (trail > 0) {
            out.append(Component.literal("-")).append(modifier(trail, "morefireworks.effect.trail"));
        }

        List<Component> colours = new ArrayList<>();
        Set<Integer> seen = new LinkedHashSet<>();
        boolean gradient = false;
        for (StarSpec star : spec.stars()) {
            for (int rgb : star.colors()) {
                if (seen.add(rgb)) {
                    colours.add(color(rgb));
                }
            }
            if (star.hasFadeColors()) {
                gradient = true;
                for (int rgb : star.fadeColors()) {
                    if (seen.add(rgb)) {
                        colours.add(color(rgb));
                    }
                }
            }
        }
        if (!colours.isEmpty()) {
            out.append(Component.literal("-"));
            for (int i = 0; i < Math.min(colours.size(), 3); i++) {
                if (i > 0) {
                    out.append(Component.literal("/"));
                }
                out.append(colours.get(i));
            }
            if (colours.size() > 3) {
                out.append(Component.translatable("morefireworks.etc"));
            }
            if (gradient) {
                out.append(Component.translatable("morefireworks.gradient"));
            }
        }

        Set<StarShape> shapes = new LinkedHashSet<>();
        for (StarSpec star : spec.stars()) {
            shapes.add(star.shape());
        }
        out.append(Component.literal("-"));
        boolean first = true;
        for (StarShape shape : shapes) {
            if (!first) {
                out.append(Component.literal("-"));
            }
            out.append(shape(shape));
            first = false;
        }
        out.append(Component.translatable("morefireworks.suffix.compound"));
        return out;
    }

    /**
     * The full, lossless name split into displayable lines: one header line, then one line per run
     * of identical stars. Splitting by structure instead of by character count guarantees that a
     * tooltip never overflows and never breaks a token in half.
     */
    public static List<Component> fullNameLines(RocketSpec spec) {
        List<Component> lines = new ArrayList<>();
        MutableComponent header = Component.empty()
                .append(tier(spec.flight()).withStyle(ChatFormatting.GOLD))
                .append(Component.literal(" "))
                .append(Component.translatable(spec.isEmpty()
                        ? "morefireworks.suffix.plain"
                        : "morefireworks.suffix.compound").withStyle(ChatFormatting.GOLD));
        lines.add(header);

        if (spec.isEmpty()) {
            return lines;
        }

        for (FireworkNamer.StarRun run : FireworkNamer.StarRun.group(spec.stars())) {
            StarSpec star = run.spec();
            MutableComponent line = Component.empty()
                    .append(Component.literal(run.positionText() + " ").withStyle(MUTED))
                    .append(shape(star.shape()).withStyle(ACCENT))
                    .append(Component.literal(" "));
            for (int i = 0; i < star.colors().size(); i++) {
                if (i > 0) {
                    line.append(Component.literal("/"));
                }
                line.append(color(star.colors().get(i)));
            }
            if (star.hasFadeColors()) {
                line.append(Component.literal("→"));
                for (int i = 0; i < star.fadeColors().size(); i++) {
                    if (i > 0) {
                        line.append(Component.literal("/"));
                    }
                    line.append(color(star.fadeColors().get(i)));
                }
            }
            if (star.flicker() || star.trail()) {
                line.append(Component.literal(" ("));
                if (star.flicker()) {
                    line.append(Component.translatable("morefireworks.effect.flicker"));
                    if (star.trail()) {
                        line.append(Component.literal(", "));
                    }
                }
                if (star.trail()) {
                    line.append(Component.translatable("morefireworks.effect.trail"));
                }
                line.append(Component.literal(")"));
            }
            lines.add(line.withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    // ------------------------------------------------------------------
    // Star names
    // ------------------------------------------------------------------

    public static MutableComponent starName(StarSpec star) {
        MutableComponent out = Component.empty();
        for (int i = 0; i < star.colors().size(); i++) {
            if (i > 0) {
                out.append(Component.literal("/"));
            }
            out.append(color(star.colors().get(0 + i)));
        }
        out.append(shape(star.shape()));
        if (star.hasFadeColors()) {
            out.append(Component.translatable("morefireworks.gradient"));
        }
        if (star.flicker()) {
            out.append(Component.translatable("morefireworks.effect.flicker"));
        }
        if (star.trail()) {
            out.append(Component.translatable("morefireworks.effect.trail"));
        }
        return out;
    }

    public static List<Component> starLines(StarSpec star) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("morefireworks.star.shape",
                shape(star.shape())).withStyle(ChatFormatting.AQUA));
        MutableComponent colours = Component.empty();
        for (int i = 0; i < star.colors().size(); i++) {
            if (i > 0) {
                colours.append(Component.literal("/"));
            }
            colours.append(color(star.colors().get(i)));
        }
        if (star.hasFadeColors()) {
            colours.append(Component.literal(" → "));
            for (int i = 0; i < star.fadeColors().size(); i++) {
                if (i > 0) {
                    colours.append(Component.literal("/"));
                }
                colours.append(color(star.fadeColors().get(i)));
            }
        }
        lines.add(Component.translatable("morefireworks.star.colours", colours).withStyle(ChatFormatting.GRAY));
        if (star.flicker() || star.trail()) {
            MutableComponent effects = Component.empty();
            if (star.flicker()) {
                effects.append(Component.translatable("morefireworks.effect.flicker"));
            }
            if (star.trail()) {
                if (star.flicker()) {
                    effects.append(Component.literal(", "));
                }
                effects.append(Component.translatable("morefireworks.effect.trail"));
            }
            lines.add(Component.translatable("morefireworks.star.effects", effects).withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    // ------------------------------------------------------------------
    // Vocabulary
    // ------------------------------------------------------------------

    public static MutableComponent tier(int flight) {
        return Component.translatable("morefireworks.tier." + flight);
    }

    public static MutableComponent shape(StarShape shape) {
        return Component.translatable("morefireworks.shape." + shape.id());
    }

    private static MutableComponent modifier(int count, String effectKey) {
        return Component.translatable("morefireworks.modifier",
                Component.translatable("morefireworks.numeral." + Math.min(count, 7)),
                Component.translatable(effectKey));
    }

    /** A named dye colour where one exists, {@code #RRGGBB} otherwise. */
    public static MutableComponent color(int rgb) {
        String dye = NameVocabulary.dyeNameId(rgb);
        if (dye != null) {
            return Component.translatable("morefireworks.color." + dye)
                    .withStyle(style -> style.withColor(rgb));
        }
        return Component.literal(String.format("#%06X", rgb))
                .withStyle(style -> style.withColor(rgb));
    }
}
