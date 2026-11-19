package com.morefireworks.naming;

import com.morefireworks.core.RocketSpec;
import com.morefireworks.core.StarShape;
import com.morefireworks.core.StarSpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FireworkNamerTest {

    private static RocketSpec showcase() {
        return RocketSpec.of(3, List.of(
                new StarSpec(StarShape.STAR, List.of(0xB02E26, 0x3C44AA), List.of(0xFED83D), true, true),
                new StarSpec(StarShape.STAR, List.of(0xB02E26, 0x3C44AA), List.of(0xFED83D), true, true),
                new StarSpec(StarShape.LARGE_BALL, List.of(0x3AB3DA), List.of(), false, true),
                new StarSpec(StarShape.SMALL_BALL, List.of(0x80C71F), List.of(), false, false)));
    }

    @Test
    void namingIsDeterministic() {
        for (NamingLanguage language : NamingLanguage.values()) {
            for (NameMode mode : NameMode.values()) {
                assertEquals(FireworkNamer.name(showcase(), language, mode),
                        FireworkNamer.name(showcase(), language, mode));
            }
        }
    }

    @Test
    void segmentsJoinToTheName() {
        for (NamingLanguage language : NamingLanguage.values()) {
            for (NameMode mode : NameMode.values()) {
                StringBuilder sb = new StringBuilder();
                for (NameSegment segment : FireworkNamer.segments(showcase(), language, mode)) {
                    sb.append(segment.text());
                }
                assertEquals(FireworkNamer.name(showcase(), language, mode), sb.toString());
            }
        }
    }

    @Test
    void shortNameMatchesTheDocumentedShape() {        String zh = FireworkNamer.name(showcase(), NamingLanguage.ZH_CN, NameMode.SHORT);
        assertTrue(zh.startsWith("三级"), zh);
        assertTrue(zh.contains("闪烁"), zh);
        assertTrue(zh.contains("拖尾"), zh);
        assertTrue(zh.contains("渐变"), zh);
        assertTrue(zh.endsWith("复合烟花火箭"), zh);
        assertTrue(zh.contains("-"), "the short form is dash separated: " + zh);
    }

    @Test
    void fullNameMergesIdenticalRuns() {        String full = FireworkNamer.name(showcase(), NamingLanguage.ZH_CN, NameMode.FULL);
        assertTrue(full.contains("1–2"), full);
        assertTrue(full.contains("{"), full);
        assertTrue(full.contains("}"), full);
    }

    @Test
    void languagesDiffer() {
        assertNotEquals(FireworkNamer.name(showcase(), NamingLanguage.ZH_CN, NameMode.FULL),
                FireworkNamer.name(showcase(), NamingLanguage.EN_US, NameMode.FULL));
    }

    @Test
    void starOrderChangesTheFullName() {
        RocketSpec a = RocketSpec.of(1, List.of(
                new StarSpec(StarShape.STAR, List.of(0xFF0000), List.of(), false, false),
                new StarSpec(StarShape.CREEPER, List.of(0x0000FF), List.of(), false, false)));
        RocketSpec b = RocketSpec.of(1, List.of(
                new StarSpec(StarShape.CREEPER, List.of(0x0000FF), List.of(), false, false),
                new StarSpec(StarShape.STAR, List.of(0xFF0000), List.of(), false, false)));
        assertNotEquals(FireworkNamer.name(a, NamingLanguage.ZH_CN, NameMode.FULL),
                FireworkNamer.name(b, NamingLanguage.ZH_CN, NameMode.FULL));
    }

    @Test
    void arbitraryColoursFallBackToHex() {
        RocketSpec spec = RocketSpec.of(1, List.of(
                new StarSpec(StarShape.STAR, List.of(0x12AB34), List.of(), false, false)));
        String full = FireworkNamer.name(spec, NamingLanguage.EN_US, NameMode.FULL);
        assertTrue(full.contains("#12AB34"), full);
    }

    @Test
    void plainRocketHasNoStageList() {
        String zh = FireworkNamer.name(RocketSpec.plain(2), NamingLanguage.ZH_CN, NameMode.FULL);
        assertTrue(zh.contains("普通烟花火箭"), zh);
        assertFalse(zh.contains("{"), zh);
    }

    @Test
    void shortNamesMayCollideAndThatIsDocumented() {
        // Same summary information, different underlying data: the short form intentionally collides.
        RocketSpec a = RocketSpec.of(1, List.of(
                new StarSpec(StarShape.STAR, List.of(0xFF0000), List.of(), false, false)));
        RocketSpec b = RocketSpec.of(1, List.of(
                new StarSpec(StarShape.STAR, List.of(0xFF0000, 0xFF0000), List.of(), false, false)));
        assertTrue(FireworkNamer.sameShortName(a, b, NamingLanguage.ZH_CN));
        assertNotEquals(FireworkNamer.name(a, NamingLanguage.ZH_CN, NameMode.FULL),
                FireworkNamer.name(b, NamingLanguage.ZH_CN, NameMode.FULL));
    }

    @Test
    void nameCacheReturnsTheSameString() {
        FireworkNameCache.clear();
        String first = FireworkNameCache.name(showcase(), NamingLanguage.ZH_CN, NameMode.FULL);
        String second = FireworkNameCache.name(showcase(), NamingLanguage.ZH_CN, NameMode.FULL);
        assertEquals(first, second);
        assertEquals(1, FireworkNameCache.size());
        FireworkNameCache.clear();
        assertEquals(0, FireworkNameCache.size());
    }

    /**
     * The compact Chinese name follows the shape required by the specification:
     * {@code 三级-双闪烁-四拖尾-红蓝渐变-星形-大球形复合烟花火箭}.
     */
    @Test
    void chineseShortNameMatchesTheSpecificationExample() {
        StarSpec flickerTrail = new StarSpec(StarShape.STAR, List.of(0xB02E26), List.of(0x3C44AA), true, true);
        StarSpec trailBlue = new StarSpec(StarShape.LARGE_BALL, List.of(0x3C44AA), List.of(), false, true);
        StarSpec plainBlue = new StarSpec(StarShape.LARGE_BALL, List.of(0x3C44AA), List.of(), false, false);
        RocketSpec spec = RocketSpec.of(3, List.of(
                flickerTrail, flickerTrail, trailBlue, trailBlue, plainBlue));
        assertEquals("三级-双闪烁-四拖尾-红蓝渐变-星形-大球形复合烟花火箭",
                FireworkNamer.name(spec, NamingLanguage.ZH_CN, NameMode.SHORT));
    }

    /** English needs spaces where Chinese concatenates; the full form must stay readable. */
    @Test
    void englishFullNameSeparatesItsParts() {
        RocketSpec spec = RocketSpec.of(1, List.of(
                new StarSpec(StarShape.STAR, List.of(0xB02E26), List.of(0xFED83D), true, false)));
        String full = FireworkNamer.name(spec, NamingLanguage.EN_US, NameMode.FULL);
        assertTrue(full.contains("tier I {1 star red→yellow (flicker)}"), full);
        assertTrue(full.endsWith(" compound firework rocket"), full);
    }

    /** A count of one is left implicit in Chinese, exactly like the specification example. */
    @Test
    void chineseOmitsTheNumeralForSingleEffects() {
        RocketSpec spec = RocketSpec.of(1, List.of(
                new StarSpec(StarShape.STAR, List.of(0xB02E26), List.of(), true, true)));
        assertEquals("一级-闪烁-拖尾-红-星形复合烟花火箭",
                FireworkNamer.name(spec, NamingLanguage.ZH_CN, NameMode.SHORT));
    }

    /** Tiers use plain digits; a flight level 2 rocket is "二级", never "双级". */
    @Test
    void chineseTiersUsePlainDigits() {
        assertEquals("二级普通烟花火箭",
                FireworkNamer.name(RocketSpec.plain(2), NamingLanguage.ZH_CN, NameMode.FULL));
        assertEquals("三级", FireworkNamer.name(RocketSpec.plain(3), NamingLanguage.ZH_CN, NameMode.SHORT)
                .substring(0, 2));
    }
}
