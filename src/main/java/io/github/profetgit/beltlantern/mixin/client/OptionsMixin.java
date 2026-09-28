package io.github.profetgit.beltlantern.mixin.client;

import io.github.profetgit.beltlantern.client.Keys;
import java.util.Arrays;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the key to the options before they load, so its binding is saved and shows in Controls (every loader). */
@Mixin(Options.class)
public abstract class OptionsMixin {
    @Shadow
    @Final
    @Mutable
    public KeyMapping[] keyMappings;

    @Inject(method = "load", at = @At("HEAD"))
    private void beltlantern$keys(CallbackInfo ci) {
        for (KeyMapping k : keyMappings) if (k == Keys.TOGGLE) return;
        KeyMapping[] more = Arrays.copyOf(keyMappings, keyMappings.length + 1);
        more[keyMappings.length] = Keys.TOGGLE;
        keyMappings = more;
    }
}
