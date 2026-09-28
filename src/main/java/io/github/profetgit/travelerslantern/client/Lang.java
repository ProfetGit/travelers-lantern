package io.github.profetgit.travelerslantern.client;

import java.util.Map;

/**
 * English fallbacks for the mod's few translation keys: Fabric without Fabric API doesn't load a mod's assets, so the
 * lang file only reaches the game on NeoForge and Forge.
 */
public final class Lang {
    private static final Map<String, String> EN = Map.of("key.travelers_lantern.toggle", "Traveler's Lantern: hang / take off");

    private Lang() {
    }

    public static String fallback(String key) {
        return key.startsWith("key.travelers_lantern.") ? EN.get(key) : null;
    }
}
