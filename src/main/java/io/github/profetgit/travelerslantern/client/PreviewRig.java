package io.github.profetgit.travelerslantern.client;

//? if >=26.3 {
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The settings preview's walking player: vanilla's walk pose, and the belt lantern on the mod's own pendulum with the
 * same hook, body boxes and renderer transform as BeltLayer, baked over one loop at 60 frames a second.
 */
public final class PreviewRig {
    public static final int FPS = 60;
    /** Vanilla's walk animation speed for a walking player. */
    private static final float SPEED = 0.86F;
    /** Seconds of one stride (two steps) at that speed. */
    public static final double STRIDE = 2 * Math.PI / (0.6662 * 20 * SPEED);

    /** Where the walker is: position, heading (degrees), seconds walked this loop, and how much of a walk (0 standing, 1 walking). */
    public record Move(double x, double z, float yaw, double walked, float amount) {
    }

    /** A loop's motion; at(period) must give the whole loop's seconds walked. */
    public interface Path {
        Move at(double seconds);
    }

    /** One frame: the walker, the lantern block's transform and its weight (both in world blocks), a clink's strength or -1. */
    public record Frame(Move move, Matrix4f lantern, Vector3f weight, float bump) {
    }

    private PreviewRig() {
    }

    /** Model space (y down, the player facing -z) to blocks around the feet, as LivingEntityRenderer does for a player. */
    public static Matrix4f toWorld(float yaw) {
        return new Matrix4f().rotateY((float) Math.toRadians(180 - yaw)).scale(-1, -1, 1).scale(0.9375F).translate(0, -1.501F, 0);
    }

    /** The body parts in model space, in blocks: head, body, right arm, left arm, right leg, left leg, in vanilla's walk. */
    public static Matrix4f[] pose(Move m) {
        double phase = m.walked() * 20 * SPEED * 0.6662;
        float amp = SPEED * m.amount();
        float swing = (float) Math.cos(phase), other = (float) Math.cos(phase + Math.PI);
        return new Matrix4f[] {
            part(0, 0, 0, 0),
            part(0, 0, 0, 0),
            part(-5, 2, 0, other * amp),
            part(5, 2, 0, swing * amp),
            part(-1.9F, 12, 0, swing * 1.4F * amp),
            part(1.9F, 12, 0, other * 1.4F * amp)
        };
    }

    private static Matrix4f part(float x, float y, float z, float pitch) {
        return new Matrix4f().translate(x / 16, y / 16, z / 16).rotateX(pitch);
    }

    /**
     * The walker {@code ms} into the loop, between the two baked frames around it (a screen draws at its own rate);
     * {@code walked} is the whole loop's seconds walked. The clink is the earlier frame's.
     */
    public static Frame sample(Frame[] frames, double ms, double walked) {
        double at = ms * FPS / 1000;
        int i = (int) at;
        float f = (float) (at - i);
        Frame a = frames[i % frames.length], b = frames[(i + 1) % frames.length];
        Move m = a.move(), n = b.move();
        // the loop's last frame leads into its first, a whole loop's walk later
        double ahead = (i + 1) % frames.length == 0 ? walked : 0;
        float turn = ((n.yaw() - m.yaw()) % 360 + 540) % 360 - 180;
        Move move = new Move(m.x() + (n.x() - m.x()) * f, m.z() + (n.z() - m.z()) * f, m.yaw() + turn * f,
            m.walked() + (n.walked() + ahead - m.walked()) * f, m.amount() + (n.amount() - m.amount()) * f);
        return new Frame(move, a.lantern().lerp(b.lantern(), f, new Matrix4f()), a.weight().lerp(b.weight(), f, new Vector3f()), a.bump());
    }

    public static Frame[] bake(Path path, double period, boolean left) {
        int n = (int) Math.round(period * FPS);
        double loop = path.at(period).walked();
        Swing swing = Swing.preview();
        Frame[] out = new Frame[n];
        float side = left ? 1 : -1;
        // two loops to settle, then the one that is kept
        for (int i = -2 * n; i < n; i++) {
            Move at = path.at(Math.floorMod(i, n) / (double) FPS);
            Move m = new Move(at.x(), at.z(), at.yaw(), at.walked() + Math.floorDiv(i, n) * loop, at.amount());
            Matrix4f world = toWorld(m.yaw());
            Matrix4f[] parts = pose(m);
            Matrix4f hook = new Matrix4f(world).mul(parts[1]).translate(side * BeltLayer.HOOK_X / 16, BeltLayer.HOOK_Y / 16, BeltLayer.HOOK_Z / 16);
            Swing.Box[] boxes = {
                new Swing.Box(parts[1], 0, 6F / 16, 0, 4F / 16, 6F / 16, 2F / 16, 0),
                new Swing.Box(parts[left ? 5 : 4], 0, 6F / 16, 0, 2F / 16, 6F / 16, 2F / 16, left ? 1 : -1)
            };
            Quaternionf q = swing.previewStep((i + 3.0 * n) / FPS, m.yaw(), m.x(), 0, m.z(), hook, world, boxes,
                BeltLayer.LENGTH, BeltLayer.RADIUS, m.walked() * 20 * SPEED * 0.6662, SPEED * m.amount(), side);
            if (i < 0) continue;
            Matrix4f lantern = new Matrix4f().translate((float) m.x(), 0, (float) m.z()).mul(hook).rotate(q).rotateX((float) Math.PI)
                .scale(BeltLayer.SCALE).translate(-0.5F, -1, -0.5F);
            out[i] = new Frame(m, lantern, new Vector3f((float) swing.bx, (float) swing.by, (float) swing.bz), (float) swing.previewBump());
        }
        return out;
    }
}
//?}
