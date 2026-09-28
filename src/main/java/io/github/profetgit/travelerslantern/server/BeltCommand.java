package io.github.profetgit.travelerslantern.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import io.github.profetgit.travelerslantern.TravelersLantern;
import io.github.profetgit.travelerslantern.Config;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * /travelerslantern hangs a lantern on your belt or takes it off (what the key does; it also works from a client without
 * the mod). /travelerslantern light [true|false] (operators) turns the light shown to players without the mod on or off.
 * /travelerslantern client smooth|blocks is sent by the mod's client: whether it draws the lanterns' light itself.
 */
public final class BeltCommand {
    private BeltCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal(TravelersLantern.COMMAND)
            .executes(ctx -> Belt.toggle(ctx.getSource().getPlayerOrException()))
            .then(Commands.literal("light").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("Traveler's Lantern light is " + (Config.get().light ? "on" : "off")
                        + ": " + Lights.status()), false);
                    return Config.get().light ? 1 : 0;
                })
                .then(Commands.argument("on", BoolArgumentType.bool()).executes(ctx -> {
                    boolean on = BoolArgumentType.getBool(ctx, "on");
                    Config.get().light = on;
                    Config.save();
                    ctx.getSource().sendSuccess(() -> Component.literal("Traveler's Lantern light turned " + (on ? "on" : "off")), true);
                    return 1;
                })))
            .then(Commands.literal("client")
                .then(Commands.literal("smooth").executes(ctx -> {
                    Lights.setSmooth(ctx.getSource().getPlayerOrException(), true);
                    return 1;
                }))
                .then(Commands.literal("blocks").executes(ctx -> {
                    Lights.setSmooth(ctx.getSource().getPlayerOrException(), false);
                    return 1;
                }))));
    }
}
