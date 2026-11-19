package com.morefireworks.naming;

import com.morefireworks.core.RocketSpec;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Small access-ordered LRU cache for rendered names.
 *
 * <p>Name rendering is pure and cheap, but tooltips are re-evaluated every frame while an item is
 * hovered, and a container full of fireworks can hold dozens of stacks. Caching the rendered string
 * keeps that off the render thread's critical path without ever growing with the variant space: the
 * cache holds at most {@link #CAPACITY} names no matter how many variants exist.</p>
 */
public final class FireworkNameCache {

    public static final int CAPACITY = 512;

    private static final Map<Key, String> CACHE = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Key, String> eldest) {
            return size() > CAPACITY;
        }
    };

    private FireworkNameCache() {
    }

    private record Key(RocketSpec spec, NamingLanguage language, NameMode mode) {
    }

    public static String name(RocketSpec spec, NamingLanguage language, NameMode mode) {
        Key key = new Key(spec, language, mode);
        synchronized (CACHE) {
            String cached = CACHE.get(key);
            if (cached != null) {
                return cached;
            }
            String rendered = FireworkNamer.name(spec, language, mode);
            CACHE.put(key, rendered);
            return rendered;
        }
    }

    /** Drops all cached names; used by {@code /reload} style resets and by tests. */
    public static void clear() {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }

    public static int size() {
        synchronized (CACHE) {
            return CACHE.size();
        }
    }
}
