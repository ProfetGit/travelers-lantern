package io.github.profetgit.travelerslantern.ui;

//? if >=26.3 {
import io.github.profetgit.profetsettings.Ctx;
import io.github.profetgit.profetsettings.Gfx;
import io.github.profetgit.travelerslantern.Config;
import io.github.profetgit.travelerslantern.Lanterns;
import io.github.profetgit.travelerslantern.client.PreviewRig;
import io.github.profetgit.travelerslantern.client.Swing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

// The walker seen from beside, close: which hip the lantern hangs on, or (with stops) the clink when it knocks a leg.
public final class WalkScene extends Stage {
    private static final double LOOP = 8 * PreviewRig.STRIDE;
    // the ground's pattern repeats after the blocks one loop walks
    private static final int BLOCKS = 19;
    // the clink scene: walk this long, ease to a stop, turn aside and back (that knocks the lantern on the leg), set off again
    private static final double WALK = 2.5 * PreviewRig.STRIDE, EASE = 0.12, STAND = 1.25, TURN = 0.15, ASIDE = 0.55;
    private static final Identifier NOTE = Identifier.withDefaultNamespace("textures/particle/note.png");

    // test hooks: how many clinks the last baked loop had, and when the first one was
    public static int bakedBumps = -1, firstBumpMs = 0;

    private final boolean stops;
    private final double period;
    private PreviewRig.Frame[] frames;
    private boolean bakedLeft;
    private float yaw = Float.NaN;
    private double yawAt;
    private int heard = -1;

    public WalkScene(boolean stops) {
        this.stops = stops;
        this.period = stops ? LOOP + STAND + EASE : LOOP;
    }

    private PreviewRig.Move move(double t, boolean left) {
        if (!stops) return new PreviewRig.Move(0, 0, 0, t, 1);
        double a = ramp(t, WALK, EASE), b = ramp(t, WALK + EASE + STAND, EASE);
        double lost = integral(t, WALK) - integral(t, WALK + EASE + STAND);
        double out = WALK + EASE + 0.15;
        // turning toward the lantern's side keeps it in view
        double turned = ramp(t, out, TURN) - ramp(t, out + ASIDE, TURN);
        return new PreviewRig.Move(0, 0, (float) ((left ? 90 : -90) * turned), t - lost, (float) (1 - a + b));
    }

    // 0 to 1 over {@code over} seconds from {@code from}
    private static double ramp(double t, double from, double over) {
        return Math.max(0, Math.min(1, (t - from) / over));
    }

    private static double integral(double t, double from) {
        double u = t - from;
        return u <= 0 ? 0 : u < EASE ? u * u / (2 * EASE) : u - EASE / 2;
    }

    @Override
    public void render(Gfx g, Ctx c, int x, int y, int w, int h) {
        Config cfg = Config.get();
        boolean left = cfg.leftSide;
        // the side switch turns the camera round instead of restarting the walk
        float time = time(c, (float) (period * 1000), stops ? cfg.clinks : 0, stops ? cfg.clinkVolume : 0);
        if (frames == null || bakedLeft != left) {
            frames = PreviewRig.bake(t -> move(t, left), period, left);
            bakedLeft = left;
            if (stops) {
                int n = 0;
                for (int i = frames.length - 1; i >= 0; i--) {
                    if (frames[i].bump() < 0) continue;
                    n++;
                    firstBumpMs = i * 1000 / PreviewRig.FPS;
                }
                bakedBumps = n;
            }
        }
        int now = (int) (time * PreviewRig.FPS / 1000) % frames.length;
        PreviewRig.Frame f = PreviewRig.sample(frames, time, move(period, left).walked());

        float want = left ? -48 : 62;
        if (Float.isNaN(yaw) || debugTime >= 0) yaw = want;
        else yaw += (want - yaw) * (float) (1 - Math.exp(-Math.max(0, c.now - yawAt) / 90));
        yawAt = c.now;

        int ox = x + (w - W) / 2;
        backdrop(g, ox, y);
        Cam cam = new Cam(yaw, 20, 64, ox + 99, y + 75, 0, 0.9F, 0);
        Mesh mesh = new Mesh();
        double scroll = (f.move().walked() * BLOCKS / LOOP) % BLOCKS;
        int base = (int) Math.floor(scroll);
        float level = cfg.clientLight ? 15 : 0;
        float cx = ox + 99, cy = y + 112;
        for (int ix = -6; ix <= 6; ix++) {
            for (int iz = base - 7; iz <= base + 8; iz++) {
                int kind = (ix * 73856093 ^ Math.floorMod(iz, BLOCKS) * 19349663) >>> 3;
                float gz = (float) (iz - scroll);
                Props.ground(mesh, cam, ix - 0.5F, gz, kind, 4, (px, pz, s) -> {
                    float d2 = px * px + pz * pz;
                    float[] col = Props.lit(level * Math.max(0, 1 - d2 / 70), 0);
                    return ARGB.colorFromFloat(Props.oval(s.x, s.y, cx, cy, 96, 36, 0.65F), col[0], col[1], col[2]);
                });
            }
        }
        mesh.draw(g, x, y, w, h, false);
        Props.shadow(mesh, cam, 0, 0, 0);
        mesh.draw(g, x, y, w, h, false);
        BlockState lantern = Props.lantern();
        float[] light = cfg.clientLight ? new float[] {0.92F, 0.82F, 0.72F} : new float[] {0.34F, 0.36F, 0.46F};
        Figure.add(mesh, cam, PreviewRig.toWorld(f.move().yaw()), PreviewRig.pose(f.move()), light);
        Props.lantern(mesh, cam, lantern, f.lantern());
        mesh.draw(g, x, y, w, h, true);
        Vector3f at = cam.project(new Vector3f(f.weight()));
        float[] tint = Lanterns.flame(lantern);
        halo(g, at.x, at.y, 30, ARGB.colorFromFloat(0.22F, tint[0], tint[1], tint[2]));

        if (stops) {
            boolean loud = cfg.clinks && cfg.clinkVolume > 0;
            if (loud) {
                clinks(g, cam, now, cfg.clinkVolume / 100F);
                sound(now);
            } else {
                note(g, x, y, w, h, t("off.clinks"));
            }
        } else if (!cfg.clientLight) {
            note(g, x, y, w, h, t("off.light"));
        }
    }

    // vanilla's note particle popping off the lantern for each knock, as big as the knock and the volume
    private void clinks(Gfx g, Cam cam, int now, float volume) {
        for (int back = 0; back < 22; back++) {
            PreviewRig.Frame f = frames[Math.floorMod(now - back, frames.length)];
            if (f.bump() < 0) continue;
            float age = back / (float) PreviewRig.FPS;
            Vector3f at = cam.project(new Vector3f(f.weight()));
            float size = (0.45F + 0.55F * volume) * (0.8F + 0.3F * f.bump()) * Math.min(1, (back + 1) / 3F) * 2.1F;
            float alpha = back < 14 ? 1 : 1 - (back - 14) / 8F;
            // out past the lantern, away from the body
            float side = at.x < cam.project(new Vector3f(0, 0.9F, 0)).x ? -1 : 1;
            g.pushTranslate(at.x + side * (17 + 14 * age) - 4 * size, at.y - 8 - 34 * age - 4 * size);
            g.g.pose().scale(size, size);
            g.g.blit(RenderPipelines.GUI_TEXTURED, NOTE, 0, 0, 0, 0, 8, 8, 8, 8, g.a(ARGB.colorFromFloat(alpha, 1, 0.82F, 0.36F)));
            g.pop();
        }
    }

    // the clink itself, at the loudness it has in the game, once per knock while the clock runs
    private void sound(int now) {
        if (debugTime >= 0) return;
        if (heard >= 0) {
            int passed = Math.floorMod(now - heard, frames.length);
            for (int i = 1; i <= Math.min(passed, 15); i++) {
                PreviewRig.Frame f = frames[(heard + i) % frames.length];
                if (f.bump() >= 0) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.LANTERN_HIT, 1.75F, Swing.loudness(f.bump())));
                }
            }
        }
        heard = now;
    }
}
//?}
