package com.morefireworks.naming;

import com.morefireworks.core.RgbColors;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A compact, deterministic vocabulary for firework names.
 *
 * <p>The vocabulary deliberately lives in code instead of the client language files: firework
 * names are generated on the server as well (commands, item names sent over the network, logs),
 * and a name must never depend on the locale of the machine that produced it. Minecraft's
 * language files are still used for everything that is a fixed UI string.
 */
public final class NameVocabulary {

    /** The sixteen vanilla dye colours, with exact RGB values and both language names. */
    public record DyeColor(int rgb, String zhFull, String zhShort, String en) {
    }

    // Values mirror DyeColor's texture diffuse colours. They are verified against the
    // Minecraft 26.2 sources; see README "Colour names".
    public static final List<DyeColor> DYE_COLORS = List.of(
            new DyeColor(0xF9FFFE, "白色", "白", "white"),
            new DyeColor(0xF9801D, "橙色", "橙", "orange"),
            new DyeColor(0xC74EBD, "品红色", "品红", "magenta"),
            new DyeColor(0x3AB3DA, "淡蓝色", "淡蓝", "light blue"),
            new DyeColor(0xFED83D, "黄色", "黄", "yellow"),
            new DyeColor(0x80C71F, "黄绿色", "黄绿", "lime"),
            new DyeColor(0xF38BAA, "粉红色", "粉", "pink"),
            new DyeColor(0x474F52, "灰色", "灰", "gray"),
            new DyeColor(0x9D9D97, "淡灰色", "淡灰", "light gray"),
            new DyeColor(0x169C9C, "青色", "青", "cyan"),
            new DyeColor(0x8932B8, "紫色", "紫", "purple"),
            new DyeColor(0x3C44AA, "蓝色", "蓝", "blue"),
            new DyeColor(0x835432, "棕色", "棕", "brown"),
            new DyeColor(0x5E7C16, "绿色", "绿", "green"),
            new DyeColor(0xB02E26, "红色", "红", "red"),
            new DyeColor(0x1D1D21, "黑色", "黑", "black"));

    private static final Map<Integer, DyeColor> BY_RGB;

    static {
        Map<Integer, DyeColor> map = new HashMap<>();
        for (DyeColor color : DYE_COLORS) {
            map.put(color.rgb(), color);
        }
        BY_RGB = Map.copyOf(map);
    }

    private static final String[] ZH_DIGITS = {"零", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十"};
    private static final String[] ROMAN = {"0", "I", "II", "III"};

    private NameVocabulary() {
    }

    /** The dye colour matching exactly, or {@code null}. */
    public static DyeColor dyeColor(int rgb) {
        return BY_RGB.get(rgb);
    }

    /**
     * Renders a colour as a name token. Vanilla dye colours use their name; everything else falls
     * back to {@code #RRGGBB}, which is always lossless.
     *
     * @param compact whether the single-character Chinese form should be used
     */
    public static String color(int rgb, NamingLanguage language, boolean compact) {
        DyeColor dye = dyeColor(rgb);
        if (dye == null) {
            return RgbColors.toHex(rgb);
        }
        return switch (language) {
            case ZH_CN -> compact ? dye.zhShort() : dye.zhFull();
            case EN_US -> dye.en();
        };
    }

    /**
     * The translation-key suffix of the dye matching exactly, or {@code null} for an arbitrary
     * 24-bit colour. Used by {@link NameComponents} to emit translatable colour names.
     */
    public static String dyeNameId(int rgb) {
        DyeColor dye = BY_RGB.get(rgb);
        if (dye == null) {
            return null;
        }
        return dye.en().replace(' ', '_');
    }

    /** Chinese numeral for small counts, used by the compact name ("双闪烁", "四拖尾"). */
    public static String zhNumeral(int value) {        if (value < 0 || value >= ZH_DIGITS.length) {
            return Integer.toString(value);
        }
        if (value == 2) {
            return "双";
        }
        return ZH_DIGITS[value];
    }

    public static String roman(int value) {
        if (value < 0 || value >= ROMAN.length) {
            return Integer.toString(value);
        }
        return ROMAN[value];
    }

    public static String tier(int flight, NamingLanguage language) {
        return switch (language) {
            // A tier is written with a plain digit ("二级"), never with the compact numeral
            // ("双级"), which is reserved for effect counts such as "双闪烁".
            case ZH_CN -> (flight >= 0 && flight < ZH_DIGITS.length ? ZH_DIGITS[flight] : Integer.toString(flight)) + "级";
            case EN_US -> "tier " + roman(flight);
        };
    }

    public static String shape(com.morefireworks.core.StarShape shape, NamingLanguage language) {
        return switch (language) {
            case ZH_CN -> switch (shape) {
                case SMALL_BALL -> "小球形";
                case LARGE_BALL -> "大球形";
                case STAR -> "星形";
                case CREEPER -> "苦力怕形";
                case BURST -> "爆裂形";
            };
            case EN_US -> switch (shape) {
                case SMALL_BALL -> "small ball";
                case LARGE_BALL -> "large ball";
                case STAR -> "star";
                case CREEPER -> "creeper";
                case BURST -> "burst";
            };
        };
    }

    public static String flicker(NamingLanguage language) {
        return language == NamingLanguage.ZH_CN ? "闪烁" : "flicker";
    }

    public static String trail(NamingLanguage language) {
        return language == NamingLanguage.ZH_CN ? "拖尾" : "trail";
    }

    public static String gradient(NamingLanguage language) {
        return language == NamingLanguage.ZH_CN ? "渐变" : "gradient";
    }

    public static String compoundSuffix(NamingLanguage language) {
        return language == NamingLanguage.ZH_CN ? "复合烟花火箭" : "compound firework rocket";
    }

    public static String plainSuffix(NamingLanguage language) {
        return language == NamingLanguage.ZH_CN ? "普通烟花火箭" : "firework rocket";
    }

    public static String stageSeparator(NamingLanguage language) {
        return language == NamingLanguage.ZH_CN ? "·" : "·";
    }

    static String lower(String text) {
        return text.toLowerCase(Locale.ROOT);
    }
}
