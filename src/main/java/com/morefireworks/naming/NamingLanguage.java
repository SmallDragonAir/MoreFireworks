package com.morefireworks.naming;

import java.util.Locale;

/** Languages the structured firework names can be rendered in. */
public enum NamingLanguage {
    ZH_CN("zh_cn"),
    EN_US("en_us");

    private final String id;

    NamingLanguage(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static NamingLanguage byId(String id) {
        String normalized = id.toLowerCase(Locale.ROOT).replace('-', '_');
        for (NamingLanguage language : values()) {
            if (language.id.equals(normalized)) {
                return language;
            }
        }
        throw new IllegalArgumentException("Unknown naming language: " + id);
    }

    /** Picks the language from a Minecraft locale code such as {@code zh_cn} or {@code en_us}. */
    public static NamingLanguage fromMinecraftLocale(String locale) {
        if (locale == null) {
            return EN_US;
        }
        String normalized = locale.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("zh")) {
            return ZH_CN;
        }
        return EN_US;
    }
}
