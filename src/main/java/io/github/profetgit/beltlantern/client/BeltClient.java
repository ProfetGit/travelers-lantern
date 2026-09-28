package io.github.profetgit.beltlantern.client;

import io.github.profetgit.beltlantern.BeltLantern;
import io.github.profetgit.beltlantern.Config;
import io.github.profetgit.beltlantern.Lanterns;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Client side. Which belt counts depends on the server: when the server has the mod (it lists /beltlantern among its
 * commands; singleplayer always does), the belt is the server's and the key runs the command. On any other server the
 * belt is this client's own (VirtualBelt) and its light is the client-side one (GhostLight).
 */
public final class BeltClient {
    private static boolean serverMod;
    private static boolean worldPass;

    private BeltClient() {
    }

    public static void init() {
        Config.load(Minecraft.getInstance().gameDirectory.toPath());
    }

    public static boolean serverHasMod() {
        return serverMod;
    }

    public static void tick(Minecraft mc) {
        ClientPacketListener c = mc.getConnection();
        serverMod = c != null && c.getCommands().getRoot().getChild(BeltLantern.COMMAND) != null;
        LocalPlayer p = mc.player;
        while (Keys.TOGGLE.consumeClick()) {
            if (p == null || mc.gameMode == null) continue;
            if (serverMod) c.sendCommand(BeltLantern.COMMAND);
            else VirtualBelt.toggle(mc, p);
        }
        GhostLight.tick(mc, !serverMod);
    }

    /** What hangs on this entity's belt. */
    public static ItemStack beltOf(LivingEntity e) {
        Minecraft mc = Minecraft.getInstance();
        if (!serverMod && e == mc.player) return VirtualBelt.lantern(mc.player);
        ItemStack s = e.getItemBySlot(EquipmentSlot.BODY);
        return Lanterns.is(s) ? s : ItemStack.EMPTY;
    }

    /** True while the level's entities are being submitted (not the inventory preview or another GUI render). */
    public static boolean worldPass() {
        return worldPass;
    }

    public static void setWorldPass(boolean on) {
        worldPass = on;
        if (on) Swing.newFrame();
    }
}
