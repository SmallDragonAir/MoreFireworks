package com.morefireworks.naming;

/** One addressable piece of a generated name, so GUIs can render names hierarchically. */
public record NameSegment(SegmentKind kind, String text) {

    public enum SegmentKind {
        /** Flight tier, e.g. {@code 三级}. */
        TIER,
        /** Position or position range of a merged star run, e.g. {@code 1–2}. */
        POSITION,
        /** Explosion shape. */
        SHAPE,
        /** Colour list (initial and optionally gradient). */
        COLORS,
        /** Flicker / trail markers. */
        EFFECTS,
        /** Pure separator. */
        SEPARATOR,
        /** Trailing noun phrase. */
        SUFFIX,
        /** Aggregate modifiers used by the compact form, e.g. {@code 双闪烁}. */
        MODIFIER
    }

    public static NameSegment of(SegmentKind kind, String text) {
        return new NameSegment(kind, text);
    }

    @Override
    public String toString() {
        return text;
    }
}
