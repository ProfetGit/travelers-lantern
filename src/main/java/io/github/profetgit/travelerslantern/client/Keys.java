package io.github.profetgit.travelerslantern.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

/** B hangs a lantern on the belt or takes it off. Added to the options by OptionsMixin (no loader API needed). */
public final class Keys {
    //? if >=1.21.9 {
    public static final KeyMapping TOGGLE = new KeyMapping("key.travelers_lantern.toggle", InputConstants.KEY_B, KeyMapping.Category.GAMEPLAY);
    //?} else {
    /*public static final KeyMapping TOGGLE = new KeyMapping("key.travelers_lantern.toggle", InputConstants.KEY_B, "key.categories.gameplay");
    *///?}

    private Keys() {
    }
}
