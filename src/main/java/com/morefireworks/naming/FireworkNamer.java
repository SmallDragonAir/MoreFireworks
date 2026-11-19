package com.morefireworks.naming;

import com.morefireworks.core.RocketSpec;
import com.morefireworks.core.StarSpec;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Turns a {@link RocketSpec} into a structured, deterministic name.
 *
 * <h2>Two forms, one identity</h2>
 * The <b>full</b> name is a lossless structural description modelled on substitutive chemical
 * nomenclature: it states the flight tier, then every distinct star with its position(s), shape,
 * ordered initial colours, optional gradient colours and effects. Runs of identical stars are
 * merged into a position range, exactly like {@code 2,3-dimethyl...} merges locants.
 *
 * <pre>
 *   三级{1–2星形[红/蓝→黄](闪烁,拖尾)}·{3大球形[#12AB34]}复合烟花火箭
 *   tier III{1–2 star[red/blue→yellow](flicker,trail)}·{3 large ball[#12AB34]}compound firework rocket
 * </pre>
 *
 * The <b>short</b> name is a human friendly summary and may collide:
 *
 * <pre>
 *   三级-双闪烁-四拖尾-红蓝渐变-星形-大球形复合烟花火箭
 * </pre>
 *
 * Both forms are pure functions of the spec — no randomness, no locale dependency, no clock — so
 * the same parameters always produce the same name, on client and server alike.
 */
public final class FireworkNamer {

    /** Above this many characters a tooltip should show the wrapped/abridged form. */
    public static final int TOOLTIP_SOFT_LIMIT = 80;

    private FireworkNamer() {
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /** Convenience method returning the final rendered name. */
    public static String name(RocketSpec spec, NamingLanguage language, NameMode mode) {
        StringBuilder sb = new StringBuilder(64);
        for (NameSegment segment : segments(spec, language, mode)) {
            sb.append(segment.text());
        }
        return sb.toString();
    }

    /**
     * Renders the name as addressable segments. Joining every {@code text()} yields exactly the
     * same string as {@link #name(RocketSpec, NamingLanguage, NameMode)}.
     */
    public static List<NameSegment> segments(RocketSpec spec, NamingLanguage language, NameMode mode) {
        return mode == NameMode.FULL
                ? fullSegments(spec, language)
                : shortSegments(spec, language);
    }

    // ------------------------------------------------------------------
    // Full (structural) form
    // ------------------------------------------------------------------

    public static List<NameSegment> fullSegments(RocketSpec spec, NamingLanguage language) {
        List<NameSegment> out = new ArrayList<>();
        out.add(NameSegment.of(NameSegment.SegmentKind.TIER, NameVocabulary.tier(spec.flight(), language)));

        if (spec.isEmpty()) {
            appendSuffix(out, NameVocabulary.plainSuffix(language), language);
            return out;
        }

        boolean spaced = language == NamingLanguage.EN_US;
        for (StarRun run : StarRun.group(spec.stars())) {
            if (spaced) {
                out.add(NameSegment.of(NameSegment.SegmentKind.SEPARATOR, " "));
            }
            out.add(NameSegment.of(NameSegment.SegmentKind.SEPARATOR, "{"));
            out.add(NameSegment.of(NameSegment.SegmentKind.POSITION, run.positionText()));
            if (spaced) {
                out.add(NameSegment.of(NameSegment.SegmentKind.SEPARATOR, " "));
            }
            out.add(NameSegment.of(NameSegment.SegmentKind.SHAPE,
                    NameVocabulary.shape(run.spec().shape(), language)));
            if (spaced) {
                out.add(NameSegment.of(NameSegment.SegmentKind.SEPARATOR, " "));
            }
            out.add(NameSegment.of(NameSegment.SegmentKind.COLORS, colorText(run.spec(), language)));
            if (run.spec().flicker() || run.spec().trail()) {
                if (spaced) {
                    out.add(NameSegment.of(NameSegment.SegmentKind.SEPARATOR, " "));
                }
                out.add(NameSegment.of(NameSegment.SegmentKind.EFFECTS, effectText(run.spec(), language)));
            }
            out.add(NameSegment.of(NameSegment.SegmentKind.SEPARATOR, "}"));
        }

        appendSuffix(out, NameVocabulary.compoundSuffix(language), language);
        return out;
    }

    /**
     * Appends the trailing noun phrase. Chinese writes the suffix straight onto the previous
     * segment, while English needs a space, which is why the separator is language dependent.
     */
    private static void appendSuffix(List<NameSegment> out, String suffix, NamingLanguage language) {
        if (language == NamingLanguage.EN_US) {
            out.add(NameSegment.of(NameSegment.SegmentKind.SEPARATOR, " "));
        }
        out.add(NameSegment.of(NameSegment.SegmentKind.SUFFIX, suffix));
    }

    private static String colorText(StarSpec star, NamingLanguage language) {
        StringBuilder sb = new StringBuilder();
        appendColorList(sb, star.colors(), language);
        if (star.hasFadeColors()) {
            sb.append(language == NamingLanguage.ZH_CN ? "→" : "→");
            appendColorList(sb, star.fadeColors(), language);
        }
        return sb.toString();
    }

    private static void appendColorList(StringBuilder sb, List<Integer> colors, NamingLanguage language) {
        for (int i = 0; i < colors.size(); i++) {
            if (i > 0) {
                sb.append('/');
            }
            sb.append(NameVocabulary.color(colors.get(i), language, false));
        }
    }

    private static String effectText(StarSpec star, NamingLanguage language) {
        List<String> effects = new ArrayList<>(2);
        if (star.flicker()) {
            effects.add(NameVocabulary.flicker(language));
        }
        if (star.trail()) {
            effects.add(NameVocabulary.trail(language));
        }
        return "(" + String.join(",", effects) + ")";
    }

    // ------------------------------------------------------------------
    // Short (summary) form
    // ------------------------------------------------------------------

    public static List<NameSegment> shortSegments(RocketSpec spec, NamingLanguage language) {
        List<NameSegment> out = new ArrayList<>();
        out.add(NameSegment.of(NameSegment.SegmentKind.TIER, NameVocabulary.tier(spec.flight(), language)));

        if (spec.isEmpty()) {
            appendSuffix(out, NameVocabulary.plainSuffix(language), language);
            return out;
        }

        int flickerCount = 0;
        int trailCount = 0;
        for (StarSpec star : spec.stars()) {
            if (star.flicker()) {
                flickerCount++;
            }
            if (star.trail()) {
                trailCount++;
            }
        }

        if (flickerCount > 0) {
            out.add(NameSegment.of(NameSegment.SegmentKind.SEPARATOR, "-"));
            out.add(NameSegment.of(NameSegment.SegmentKind.MODIFIER,
                    modifierText(flickerCount, NameVocabulary.flicker(language), language)));
        }
        if (trailCount > 0) {
            out.add(NameSegment.of(NameSegment.SegmentKind.SEPARATOR, "-"));
            out.add(NameSegment.of(NameSegment.SegmentKind.MODIFIER,
                    modifierText(trailCount, NameVocabulary.trail(language), language)));
        }

        String colors = shortColorText(spec, language);
        if (!colors.isEmpty()) {
            out.add(NameSegment.of(NameSegment.SegmentKind.SEPARATOR, "-"));
            out.add(NameSegment.of(NameSegment.SegmentKind.COLORS, colors));
        }

        String shapes = shortShapeText(spec, language);
        if (!shapes.isEmpty()) {
            out.add(NameSegment.of(NameSegment.SegmentKind.SEPARATOR, "-"));
            out.add(NameSegment.of(NameSegment.SegmentKind.SHAPE, shapes));
        }

        appendSuffix(out, NameVocabulary.compoundSuffix(language), language);
        return out;
    }

    private static String modifierText(int count, String noun, NamingLanguage language) {
        return switch (language) {
            // Chinese omits the numeral for one ("闪烁"), matching how the language normally reads.
            case ZH_CN -> (count == 1 ? "" : NameVocabulary.zhNumeral(count)) + noun;
            case EN_US -> count + " " + noun;
        };
    }

    private static String shortColorText(RocketSpec spec, NamingLanguage language) {
        Set<Integer> distinct = new LinkedHashSet<>();
        boolean hasFade = false;
        for (StarSpec star : spec.stars()) {
            distinct.addAll(star.colors());
            if (star.hasFadeColors()) {
                hasFade = true;
                distinct.addAll(star.fadeColors());
            }
        }
        if (distinct.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (int rgb : distinct) {
            if (shown == 3) {
                sb.append(language == NamingLanguage.ZH_CN ? "等" : "+");
                break;
            }
            if (shown > 0 && language == NamingLanguage.EN_US) {
                sb.append('/');
            }
            sb.append(NameVocabulary.color(rgb, language, language == NamingLanguage.ZH_CN));
            shown++;
        }
        if (hasFade) {
            if (language == NamingLanguage.EN_US) {
                sb.append(' ');
            }
            sb.append(NameVocabulary.gradient(language));
        }
        return sb.toString();
    }

    private static String shortShapeText(RocketSpec spec, NamingLanguage language) {
        Set<com.morefireworks.core.StarShape> distinct = new LinkedHashSet<>();
        for (StarSpec star : spec.stars()) {
            distinct.add(star.shape());
        }
        List<String> names = new ArrayList<>(distinct.size());
        for (com.morefireworks.core.StarShape shape : distinct) {
            names.add(NameVocabulary.shape(shape, language));
        }
        String separator = language == NamingLanguage.ZH_CN ? "-" : "/";
        return String.join(separator, names);
    }

    // ------------------------------------------------------------------
    // Run merging
    // ------------------------------------------------------------------

    /**
     * A maximal run of consecutive identical stars, e.g. stars 0..1 in
     * {@code [A, A, B]} form the run {@code 1–2}.
     */
    public record StarRun(int startIndex, int endIndex, StarSpec spec) {

        public int count() {
            return endIndex - startIndex + 1;
        }

        public String positionText() {
            if (startIndex == endIndex) {
                return Integer.toString(startIndex + 1);
            }
            return (startIndex + 1) + "–" + (endIndex + 1);
        }

        public static List<StarRun> group(List<StarSpec> stars) {
            List<StarRun> runs = new ArrayList<>();
            int i = 0;
            while (i < stars.size()) {
                int j = i;
                while (j + 1 < stars.size() && stars.get(j + 1).equals(stars.get(i))) {
                    j++;
                }
                runs.add(new StarRun(i, j, stars.get(i)));
                i = j + 1;
            }
            return runs;
        }
    }

    /**
     * Whether the short form of two different specs collides. Exposed for tests and for the
     * documentation command, which reports the (expected) collision rate of summary names.
     */
    public static boolean sameShortName(RocketSpec a, RocketSpec b, NamingLanguage language) {
        return name(a, language, NameMode.SHORT).equals(name(b, language, NameMode.SHORT));
    }
}
