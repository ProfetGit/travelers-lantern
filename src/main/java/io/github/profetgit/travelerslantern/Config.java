package io.github.profetgit.travelerslantern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;

/** Settings in config/travelers_lantern.json (the game directory; on a server, the server directory). */
public final class Config {
    /**
     * Server: belt lanterns light up for players without the mod too (the server shows them an invisible light block
     * that follows the lantern; nothing is placed in the world). Players with the mod light every lantern themselves.
     */
    public boolean light = true;
    /** Client: belt lanterns light what you see. */
    public boolean clientLight = true;
    /**
     * Client: the light moves smoothly with the lantern (chunk meshes near it are rebuilt as it moves). Off, it moves a
     * block at a time: a light block only this client has, or the server's (cheaper on slow machines).
     */
    public boolean smoothLight = true;
    /**
     * Client: how often smooth light follows a moving lantern (20 to 60 times a second). Each update rebuilds the chunk
     * meshes around the lantern, so a higher rate looks smoother and costs more.
     */
    public int lightUpdatesPerSecond = 60;
    /**
     * Client: the smooth light follows the light around it. In daylight or a lit room it glows softly (about half as
     * bright, a shorter reach); at night, underground and in the dark it is at full strength and reaches a bit farther.
     */
    public boolean adaptiveLight = true;
    /** Client, on servers without the mod: the belt is on (it shows a lantern from your inventory). */
    public boolean clientBelt = false;
    /** Client: the lantern hangs on the left hip (false: the right one). */
    public boolean leftSide = true;
    /** Client: the lantern clinks quietly when it knocks against a leg. */
    public boolean clinks = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Config current;
    private static Path file;

    public static synchronized Config get() {
        if (current == null) load(Path.of("."));
        return current;
    }

    public static synchronized void load(Path gameDir) {
        Path f = gameDir.resolve("config").resolve("travelers_lantern.json").toAbsolutePath().normalize();
        if (current != null && f.equals(file)) return;
        file = f;
        Config c = null;
        try {
            if (Files.isRegularFile(f)) c = GSON.fromJson(Files.readString(f), Config.class);
        } catch (Exception e) {
            TravelersLantern.LOG.warn("Traveler's Lantern: cannot read {}, using defaults ({})", f, e.toString());
        }
        current = c == null ? new Config() : c;
        // dev runs (ModTest) pick the light model without touching the player's config
        String smooth = System.getProperty("travelers_lantern.demo.smooth", "");
        if (!smooth.isEmpty()) current.smoothLight = Boolean.parseBoolean(smooth);
        String rate = System.getProperty("travelers_lantern.demo.rate", "");
        if (!rate.isEmpty()) current.lightUpdatesPerSecond = Integer.parseInt(rate);
        current.lightUpdatesPerSecond = Math.max(20, Math.min(60, current.lightUpdatesPerSecond));
        save();
    }

    public static synchronized void save() {
        if (current == null || file == null) return;
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(current));
        } catch (Exception e) {
            TravelersLantern.LOG.warn("Traveler's Lantern: cannot write {} ({})", file, e.toString());
        }
    }
}
