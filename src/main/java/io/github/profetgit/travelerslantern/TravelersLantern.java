package io.github.profetgit.travelerslantern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Common entry: runs on both sides. Client setup lives in client/BeltClient, started by a client mixin. */
public final class TravelersLantern {
    public static final String MOD_ID = "travelers_lantern";
    /** The server command; a client that sees it in the server's command tree knows the server has the mod. */
    public static final String COMMAND = "travelerslantern";
    public static final Logger LOG = LoggerFactory.getLogger("Traveler's Lantern");
    private static String loader = "?";

    private TravelersLantern() {
    }

    public static void init(String loaderName) {
        loader = loaderName;
        LOG.info("Traveler's Lantern loaded on {}", loader);
    }

    public static String loader() {
        return loader;
    }
}
