package io.github.profetgit.beltlantern.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import io.github.profetgit.beltlantern.BeltLantern;
import io.github.profetgit.beltlantern.Config;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * /beltlantern hangs a lantern on your belt or takes it off (what the key does; it also works from a client without
 * the mod). /beltlantern light [true|false] (operators) turns the world light on or off.
 */
public final class BeltCommand {
    private BeltCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal(BeltLantern.COMMAND)
            .executes(ctx -> Belt.toggle(ctx.getSource().getPlayerOrException()))
            .then(Commands.literal("light").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("Belt lantern light is " + (Config.get().light ? "on" : "off")), false);
                    return Config.get().light ? 1 : 0;
                })
                .then(Commands.argument("on", BoolArgumentType.bool()).executes(ctx -> {
                    boolean on = BoolArgumentType.getBool(ctx, "on");
                    Config.get().light = on;
                    Config.save();
                    if (!on) Lights.releaseAllNow();
                    ctx.getSource().sendSuccess(() -> Component.literal("Belt lantern light turned " + (on ? "on" : "off")), true);
                    return 1;
                }))));
    }
}
