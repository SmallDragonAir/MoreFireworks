package com.morefireworks.core;

/**
 * The five explosion shapes a firework star can have.
 *
 * <p>The enum constant order is part of the on-disk format (it is used as the compact ordinal in
 * the network/disk encoding), so <strong>never reorder these constants</strong>; append new shapes
 * at the end if Minecraft ever adds more.
 */
public enum StarShape {
    SMALL_BALL("small_ball"),
    LARGE_BALL("large_ball"),
    STAR("star"),
    CREEPER("creeper"),
    BURST("burst");

    private final String id;

    StarShape(String id) {
        this.id = id;
    }

    /** Stable string id, used by the codec and by the language files. */
    public String id() {
        return id;
    }

    public static StarShape byId(String id) {
        for (StarShape shape : values()) {
            if (shape.id.equals(id)) {
                return shape;
            }
        }
        throw new IllegalArgumentException("Unknown firework shape id: " + id);
    }

    /** Safe lookup that never throws, used when decoding untrusted data. */
    public static StarShape byIdOrNull(String id) {
        for (StarShape shape : values()) {
            if (shape.id.equals(id)) {
                return shape;
            }
        }
        return null;
    }
}
