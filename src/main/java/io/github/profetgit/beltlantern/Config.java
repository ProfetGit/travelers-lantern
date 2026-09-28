package io.github.profetgit.beltlantern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;

/** Settings in config/beltlantern.json (the game directory; on a server, the server directory). */
public final class Config {
    /** Server: a belt lantern lights the world around its wearer (an invisible light block that follows them). */
    public boolean light = true;
    /** Client, on servers without the mod: the lantern lights what you see (only on your screen). */
    public boolean clientLight = true;
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
        Path f = gameDir.resolve("config").resolve("beltlantern.json").toAbsolutePath().normalize();
        if (current != null && f.equals(file)) return;
        file = f;
        Config c = null;
        try {
            if (Files.isRegularFile(f)) c = GSON.fromJson(Files.readString(f), Config.class);
        } catch (Exception e) {
            BeltLantern.LOG.warn("Belt Lantern: cannot read {}, using defaults ({})", f, e.toString());
        }
        current = c == null ? new Config() : c;
        save();
    }

    public static synchronized void save() {
        if (current == null || file == null) return;
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(current));
        } catch (Exception e) {
            BeltLantern.LOG.warn("Belt Lantern: cannot write {} ({})", file, e.toString());
        }
    }
}
