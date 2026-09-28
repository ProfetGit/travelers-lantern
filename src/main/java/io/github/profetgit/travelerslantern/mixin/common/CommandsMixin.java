package io.github.profetgit.travelerslantern.mixin.common;

import com.mojang.brigadier.CommandDispatcher;
import io.github.profetgit.travelerslantern.server.BeltCommand;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Registers /travelerslantern without a loader API (the same on every loader). */
@Mixin(Commands.class)
public abstract class CommandsMixin {
    @Shadow
    @Final
    private CommandDispatcher<CommandSourceStack> dispatcher;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void travelerslantern$register(Commands.CommandSelection selection, CommandBuildContext context, CallbackInfo ci) {
        BeltCommand.register(dispatcher);
    }
}
