package io.github.profetgit.beltlantern.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

/** B hangs a lantern on the belt or takes it off. Added to the options by OptionsMixin (no loader API needed). */
public final class Keys {
    public static final KeyMapping TOGGLE = new KeyMapping("key.beltlantern.toggle", InputConstants.KEY_B, KeyMapping.Category.GAMEPLAY);

    private Keys() {
    }
}
