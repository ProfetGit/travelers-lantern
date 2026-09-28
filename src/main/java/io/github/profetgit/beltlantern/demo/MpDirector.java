package io.github.profetgit.beltlantern.demo;

import io.github.profetgit.beltlantern.Lanterns;
import io.github.profetgit.beltlantern.client.BeltClient;
import io.github.profetgit.beltlantern.client.BeltLayer;
import io.github.profetgit.beltlantern.client.Keys;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;

/**
 * The director on a server (dev/server/mp.py). -Dbeltlantern.demo.mp=wear: this client hangs a lantern, walks and takes
 * it off, and checks what it sees (on a vanilla server the client-only belt and light, on a modded one the server's).
 * mp=watch: this client stands by and checks that it draws the other player's lantern. The script ops the players
 * and sets the time.
 */
final class MpDirector {
    static final String ROLE = System.getProperty("beltlantern.demo.mp", "wear");
    static final int SECONDS = Integer.getInteger("beltlantern.demo.mp.seconds", 60);
    /** Players the wearer waits for (itself and any watchers) before it starts, at most two minutes. */
    static final int PLAYERS = Integer.getInteger("beltlantern.demo.mp.players", 1);
    static int tick = -1, drawnAtStart, othersAtStart, startAt = -1;
    /** The script reads these lines to check the server's side. */
    static BlockPos lastLight;

    private MpDirector() {
    }

    static void onTick(Minecraft mc) {
        tick++;
        if (tick == 1) {
            ModTestHook.audit();
            Director.scene = 0;
            System.out.println("[bldemo] joined a server as " + ROLE + ", server mod " + BeltClient.serverHasMod());
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            Director.record();
        }
        if ("watch".equals(ROLE)) {
            if (tick == 60) othersAtStart = BeltLayer.drawnOthers;
            if (tick >= SECONDS * 20) {
                int n = BeltLayer.drawnOthers - othersAtStart;
                Director.check("mp", "sees_other", n > 0, n + " lanterns drawn on other players");
                int lit = io.github.profetgit.beltlantern.client.DynamicLight.mostOthers;
                Director.check("mp", "lights_other", lit > 0, lit + " other players' lanterns lit at once by this client");
                Director.finish();
            }
            return;
        }
        boolean serverMod = BeltClient.serverHasMod();
        // the script ops the player and gives it two lanterns first; a watcher must be in before anything happens
        if (startAt < 0) {
            if (tick >= 100 && (mc.level.players().size() >= PLAYERS || tick > 2400)) {
                startAt = tick;
                System.out.println("[bldemo] starting with " + mc.level.players().size() + " players in view");
            }
            return;
        }
        int t = tick - startAt;
        if (t == 0) {
            drawnAtStart = BeltLayer.drawnLocal;
            mc.player.getInventory().setSelectedSlot(0);
            send(mc, "tp @s 0.5 -60 0.5 -90 0");
        }
        if (t == 10) KeyMapping.click(Keys.TOGGLE.getDefaultKey());
        if (t == 40) {
            check(mc, "belt", !BeltClient.beltOf(mc.player).isEmpty(), "belt " + BeltClient.beltOf(mc.player) + ", server mod " + serverMod);
            int held = Lanterns.is(mc.player.getMainHandItem()) ? mc.player.getMainHandItem().getCount() : 0;
            // client-only: the whole stack left the hand (a shift-click); on a modded server one lantern went to the belt
            check(mc, "hand", serverMod ? held == 1 : held == 0, held + " lanterns left in the hand");
            check(mc, "drawn", BeltLayer.drawnLocal > drawnAtStart, (BeltLayer.drawnLocal - drawnAtStart) + " draws");
            lightCheck(mc, serverMod, "light");
        }
        if (t > 50 && t < 90) Director.hold(mc.options.keyUp, true);
        if (t == 90) Director.release(mc);
        if (t == 110) {
            lightCheck(mc, serverMod, "light_moved");
            check(mc, "rebuilt", io.github.profetgit.beltlantern.client.DynamicLight.busyTicks > 10,
                io.github.profetgit.beltlantern.client.DynamicLight.busyTicks + " ticks rebuilt the light, "
                    + io.github.profetgit.beltlantern.client.DynamicLight.rebuilt + " sections");
        }
        if (t == 120) KeyMapping.click(Keys.TOGGLE.getDefaultKey());
        if (t == 150) {
            check(mc, "off", BeltClient.beltOf(mc.player).isEmpty(), "belt " + BeltClient.beltOf(mc.player));
            ItemStack hand = mc.player.getMainHandItem();
            check(mc, "back_in_hand", Lanterns.is(hand), "hand " + hand);
            int lights = lightsNear(mc);
            check(mc, "dark", lights == 0, lights + " light blocks near the player");
            System.out.println("[bldemo] player at " + mc.player.blockPosition().toShortString());
        }
        if (t == 170) Director.finish();
    }

    /** This client lights its own lantern: the player and the ground beside it are lit, and no light block is involved. */
    static void lightCheck(Minecraft mc, boolean serverMod, String name) {
        float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        int self = mc.getEntityRenderDispatcher().getPackedLightCoords(mc.player, partial);
        BlockPos ground = mc.player.blockPosition().relative(net.minecraft.core.Direction.NORTH, 2);
        int floor = net.minecraft.util.LightCoordsUtil.getLightCoords(mc.level, ground);
        int blocks = lightsNear(mc);
        check(mc, name, (self & 0xFFFF) >= 13 * 16 && (floor & 0xFFFF) >= 9 * 16 && blocks == 0,
            String.format(java.util.Locale.ROOT, "player lit %.2f, ground %.2f, %d light blocks on this client (server mod %b)",
                (self & 0xFFFF) / 16F, (floor & 0xFFFF) / 16F, blocks, serverMod));
        net.minecraft.world.phys.Vec3 bob = io.github.profetgit.beltlantern.client.Swing.bobOf(mc.player.getId());
        if (bob != null) {
            BlockPos c = BlockPos.containing(bob);
            lastLight = c;
            System.out.println("[bldemo] lantern cell " + c.getX() + " " + c.getY() + " " + c.getZ() + " " + name);
        }
    }

    static BlockPos nearestLight(Minecraft mc) {
        BlockPos best = null;
        double d = 99;
        for (BlockPos p : BlockPos.betweenClosed(mc.player.blockPosition().offset(-3, -2, -3), mc.player.blockPosition().offset(3, 3, 3))) {
            if (mc.level.getBlockState(p).is(Blocks.LIGHT) && p.distSqr(mc.player.blockPosition()) < d) {
                d = p.distSqr(mc.player.blockPosition());
                best = p.immutable();
            }
        }
        return best;
    }

    static int lightsNear(Minecraft mc) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(mc.player.blockPosition().offset(-4, -3, -4), mc.player.blockPosition().offset(4, 4, 4))) {
            if (mc.level.getBlockState(p).is(Blocks.LIGHT)) n++;
        }
        return n;
    }

    static void send(Minecraft mc, String command) {
        mc.player.connection.sendCommand(command.startsWith("/") ? command.substring(1) : command);
    }

    static void check(Minecraft mc, String name, boolean pass, String detail) {
        Director.check("mp", name, pass, detail);
    }
}
