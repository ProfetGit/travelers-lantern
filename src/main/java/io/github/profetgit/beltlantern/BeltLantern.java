package io.github.profetgit.beltlantern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Common entry: runs on both sides. Client setup lives in client/BeltClient, started by a client mixin. */
public final class BeltLantern {
    public static final String MOD_ID = "beltlantern";
    /** The server command; a client that sees it in the server's command tree knows the server has the mod. */
    public static final String COMMAND = "beltlantern";
    public static final Logger LOG = LoggerFactory.getLogger("Belt Lantern");
    private static String loader = "?";

    private BeltLantern() {
    }

    public static void init(String loaderName) {
        loader = loaderName;
        LOG.info("Belt Lantern loaded on {}", loader);
    }

    public static String loader() {
        return loader;
    }
}
