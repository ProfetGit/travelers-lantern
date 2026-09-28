package io.github.profetgit.beltlantern.client;

import io.github.profetgit.beltlantern.Config;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The lantern as a pendulum: a weight on a rod hanging from the belt hook, simulated in world space, so everything the
 * hook does (walking, turning, jumping, a resource pack's body animation) swings it. Gravity is the game's item
 * gravity; the rod keeps its length; the weight bumps off the body, the hook-side leg and arm, and solid ground. A bump
 * is inelastic with a little bounce, and a moving limb hands on its own speed, so a stride kicks the lantern forward.
 * A second spring lets the lantern lag behind when the body turns. The clock is the entity's own age (ticks plus the
 * partial tick), so a paused game or frozen ticks stop it, and fixed 240 Hz steps keep it the same at any frame rate.
 */
public final class Swing {
    /** Blocks per second squared: a little heavier than item gravity (16), for a snappier swing. */
    static final double GRAVITY = Double.parseDouble(System.getProperty("beltlantern.tune.gravity", "24"));
    /** Damping per second of the swing (the lantern's speed relative to the hook: the hook's friction). */
    static final double DAMP = Double.parseDouble(System.getProperty("beltlantern.tune.damp", "2.0"));
    /** Bounce off a limb: the share of the approach speed it leaves with. */
    static final double BOUNCE = Double.parseDouble(System.getProperty("beltlantern.tune.bounce", "0.15"));
    /** How much of a limb's own speed a bump hands on (the hip and the cloth give a little). */
    static final double CARRY = Double.parseDouble(System.getProperty("beltlantern.tune.carry", "0.5"));
    /** Friction while touching a limb: sliding speed lost per unit of speed the bump took away. */
    static final double FRICTION = Double.parseDouble(System.getProperty("beltlantern.tune.friction", "0.5"));
    /** The handle's hinge: the lantern can't swing further than this from straight down. */
    static final double MAX_SWING = Math.toRadians(72);
    /**
     * The hips' own motion in a stride, in blocks at full walking speed: the lantern's hip swings forward and back with
     * its leg (the pelvis turns), dips at each footfall and sways toward the standing leg. Vanilla's body doesn't move
     * like this, so the swing adds it; a resource pack that animates the body (Fresh Animations) moves the hook itself.
     */
    static final double HIP_SURGE = Double.parseDouble(System.getProperty("beltlantern.tune.surge", "0.08")), HIP_DIP = 0.04, HIP_SWAY = 0.02;
    static final double STEP = 1.0 / 240;
    /** Twist spring (per second squared) and its damping (per second). */
    static final double TWIST_K = 70, TWIST_C = 5;
    static final double MAX_TWIST = Math.toRadians(50);
    /** Impact speed (blocks per second) for a clink. */
    static final double CLINK_SPEED = 0.8;

    private static final Int2ObjectOpenHashMap<Swing> ALL = new Int2ObjectOpenHashMap<>();
    private static int frame;
    private static ClientLevel level;

    /** World position of the weight, where it was one step ago, and the hook. */
    double bx, by, bz, px, py, pz, hx, hy, hz;
    double t = -1;
    /** The lantern's world yaw (radians), its speed, and the body yaw it follows. */
    double yaw, yawVel, bodyYaw;
    /** The body boxes (model to entity-relative world) and the entity's position at the last step. */
    Matrix4f[] lastBoxes;
    double lox, loy, loz;
    /** Stride phase (radians) and strength (0 to 1) at the last step, and where the hook was with the stride added. */
    double phase, stride, sx, sy, sz;
    int seen;
    double lastClink = -10, impact;
    /** The last rod direction in the hook's own frame, reused by passes that don't step (GUI, shadows). */
    final Vector3f local = new Vector3f(0, 1, 0);
    float twist;

    /**
     * One box of the body: the part's matrix in model space, centre and half size in the part's frame, in blocks. Out
     * 0 pushes the weight out the shortest way; +1 or -1 only out through the +x or -x side (a thigh: the lantern rests
     * against its outside, and the stride slides past it instead of flinging it).
     */
    record Box(Matrix4f part, float cx, float cy, float cz, float hx, float hy, float hz, int out) {
    }

    private Swing() {
    }

    public static void newFrame() {
        frame++;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != level) {
            level = mc.level;
            ALL.clear();
        }
        if ((frame & 255) == 0) ALL.values().removeIf(s -> frame - s.seen > 600);
    }

    /** The weight's world position for an entity simulated in the last half second, or null. */
    public static Vec3 bobOf(int entityId) {
        Swing s = ALL.get(entityId);
        if (s == null || s.t < 0 || frame - s.seen > 30) return null;
        return new Vec3(s.bx, s.by, s.bz);
    }

    /** Test hook: weight xyz, hook xyz, lantern yaw, body yaw (degrees), twist (radians); null if not simulated. */
    public static double[] debug(int entityId) {
        Swing s = ALL.get(entityId);
        if (s == null || s.t < 0) return null;
        return new double[] {s.bx, s.by, s.bz, s.hx, s.hy, s.hz, s.yaw, Math.toDegrees(s.bodyYaw), s.twist};
    }

    /**
     * Steps the pendulum (world pass only) and returns the rotation that turns the hook frame's +y (down along the body)
     * onto the rod, twist included.
     *
     * @param hook    hook frame to entity-relative world space (the camera taken out), in blocks
     * @param toWorld model space to entity-relative world space
     */
    public static Quaternionf orient(int id, boolean world, float age, float bodyRotDeg, double ox, double oy, double oz,
                                     Matrix4fc hook, Matrix4fc toWorld, Box[] boxes, float length, float radius,
                                     float walkPos, float walkSpeed, float hips, float side) {
        Swing s = ALL.computeIfAbsent(id, k -> new Swing());
        s.seen = frame;
        if (world) s.step(age / 20.0, bodyRotDeg, ox, oy, oz, hook, toWorld, boxes, length, radius, walkPos * 0.6662, Math.min(1, walkSpeed) * hips, side);
        return new Quaternionf().rotationTo(0, 1, 0, s.local.x, s.local.y, s.local.z).rotateY(s.twist);
    }

    private void step(double now, float bodyRotDeg, double ox, double oy, double oz, Matrix4fc hook, Matrix4fc toWorld,
                      Box[] boxes, float length, float radius, double walkPhase, double walkAmp, float side) {
        Vector3f h = hook.transformPosition(new Vector3f());
        double nx = ox + h.x, ny = oy + h.y, nz = oz + h.z;
        double body = Math.toRadians(bodyRotDeg);
        Matrix4f[] now3 = new Matrix4f[boxes.length];
        for (int i = 0; i < boxes.length; i++) now3[i] = new Matrix4f(toWorld).mul(boxes[i].part());
        double dt = now - t;
        boolean jump = t < 0 || dt < 0 || dt > 0.5 || sq(nx - hx, ny - hy, nz - hz) > 16 || lastBoxes == null
            || lastBoxes.length != boxes.length;
        if (jump) {
            hx = nx;
            hy = ny;
            hz = nz;
            bx = px = nx;
            by = py = ny - length;
            bz = pz = nz;
            yaw = bodyYaw = body;
            yawVel = 0;
            t = now;
            dt = 0;
            lastBoxes = now3;
            lox = ox;
            loy = oy;
            loz = oz;
            phase = walkPhase;
            stride = walkAmp;
            sx = nx;
            sy = ny;
            sz = nz;
        }
        if (dt > 0) {
            int n = Math.min(60, (int) Math.ceil(dt / STEP));
            double hs = dt / n, damp = Math.exp(-DAMP * hs), g = GRAVITY * hs * hs;
            double lastBody = bodyYaw, dBody = wrap(body - lastBody);
            impact = 0;
            Matrix4f[] before = lastBoxes, cur = new Matrix4f[boxes.length];
            for (int i = 0; i < boxes.length; i++) cur[i] = new Matrix4f(before[i]);
            double cox = lox, coy = loy, coz = loz;
            double dPhase = walkPhase - phase;
            if (Math.abs(dPhase) > 3) dPhase = 0; // the walk animation restarted
            for (int k = 1; k <= n; k++) {
                double f = (double) k / n;
                double ax = hx + (nx - hx) * f, ay = hy + (ny - hy) * f, az = hz + (nz - hz) * f;
                // the hips' stride motion, in the body's frame: forward with this side's leg, down at footfalls, sideways
                double ph = phase + dPhase * f, amp = stride + (walkAmp - stride) * f, yw = lastBody + dBody * f;
                double surge = HIP_SURGE * amp * Math.cos(ph) * side, dip = HIP_DIP * amp * (Math.cos(2 * ph) - 1) / 2;
                double sway = HIP_SWAY * amp * Math.sin(ph);
                ax += -Math.sin(yw) * surge + Math.cos(yw) * sway;
                ay += dip;
                az += Math.cos(yw) * surge + Math.sin(yw) * sway;
                double hvx = ax - sx, hvy = ay - sy, hvz = az - sz;
                sx = ax;
                sy = ay;
                sz = az;
                // the body boxes and the entity's position, part way through the frame
                Matrix4f[] prev = cur;
                cur = new Matrix4f[boxes.length];
                for (int i = 0; i < boxes.length; i++) cur[i] = before[i].lerp(now3[i], (float) f, new Matrix4f());
                double pox = cox, poy = coy, poz = coz;
                cox = lox + (ox - lox) * f;
                coy = loy + (oy - loy) * f;
                coz = loz + (oz - loz) * f;

                // the swing (speed relative to the hook) is damped, not the speed the lantern shares with its wearer
                double vx = hvx + (bx - px - hvx) * damp, vy = hvy + (by - py - hvy) * damp, vz = hvz + (bz - pz - hvz) * damp;
                px = bx;
                py = by;
                pz = bz;
                bx += vx;
                by += vy - g;
                bz += vz;
                rod(ax, ay, az, length);
                for (int i = 0; i < boxes.length; i++) collide(boxes[i], cur[i], prev[i], cox, coy, coz, pox, poy, poz, hs, radius);
                ground(radius, hs);
                rod(ax, ay, az, length);
                hinge(ax, ay, az, length, hs);
                // the lantern's yaw lags the body's (the hook turns it through a springy loop)
                double target = lastBody + dBody * f;
                double acc = TWIST_K * wrap(target - yaw) - TWIST_C * yawVel;
                yawVel += acc * hs;
                yaw += yawVel * hs;
            }
            bodyYaw = body;
            double tw = wrap(yaw - body);
            if (Math.abs(tw) > MAX_TWIST) {
                yaw = body + Math.copySign(MAX_TWIST, tw);
                yawVel = 0;
            }
            if (impact > CLINK_SPEED && now - lastClink > 0.35 && Config.get().clinks) clink(now, Math.min(1, (impact - CLINK_SPEED) / 1.5));
            t = now;
            phase = walkPhase;
            stride = walkAmp;
            lastBoxes = now3;
            lox = ox;
            loy = oy;
            loz = oz;
        }
        hx = nx;
        hy = ny;
        hz = nz;
        // the rod in the hook's own frame, for the renderer
        Vector3f d = new Vector3f((float) (bx - hx), (float) (by - hy), (float) (bz - hz));
        new Matrix4f(hook).invert().transformDirection(d);
        if (d.lengthSquared() > 1e-10) local.set(d.normalize());
        twist = (float) wrap(yaw - body);
    }

    /** Keeps the weight at rod length from the hook. */
    private void rod(double ax, double ay, double az, double length) {
        double dx = bx - ax, dy = by - ay, dz = bz - az;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-9) {
            by = ay - length;
            return;
        }
        double k = length / len;
        bx = ax + dx * k;
        by = ay + dy * k;
        bz = az + dz * k;
    }

    /**
     * Moves the weight out of one box (the shortest way out of the box grown by the weight's radius) and fixes its
     * velocity: the approach relative to the box's surface stops, with a little bounce, and a surface moving into the
     * weight carries it along.
     */
    private void collide(Box b, Matrix4f m, Matrix4f mPrev, double ox, double oy, double oz, double pox, double poy, double poz,
                         double hs, float radius) {
        Matrix4f inv = new Matrix4f(m).invert();
        Vector3f p = inv.transformPosition(new Vector3f((float) (bx - ox), (float) (by - oy), (float) (bz - oz)));
        float dx = p.x - b.cx(), dy = p.y - b.cy(), dz = p.z - b.cz();
        float ex = b.hx() + radius - Math.abs(dx), ey = b.hy() + radius - Math.abs(dy), ez = b.hz() + radius - Math.abs(dz);
        if (ex <= 0 || ey <= 0 || ez <= 0) return;
        if (b.out() != 0) p.x = b.cx() + b.out() * (b.hx() + radius);
        else if (ex <= ey && ex <= ez) p.x += Math.copySign(ex, dx);
        else if (ey <= ez) p.y += Math.copySign(ey, dy);
        else p.z += Math.copySign(ez, dz);
        Vector3f w = m.transformPosition(new Vector3f(p));
        Vector3f wPrev = mPrev.transformPosition(new Vector3f(p));
        double nbx = ox + w.x, nby = oy + w.y, nbz = oz + w.z;
        // the surface point's velocity, and the push direction as the contact normal
        double sx = CARRY * (nbx - (pox + wPrev.x)) / hs, sy = CARRY * (nby - (poy + wPrev.y)) / hs, sz = CARRY * (nbz - (poz + wPrev.z)) / hs;
        double nx = nbx - bx, ny = nby - by, nz = nbz - bz;
        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
        double vx = (bx - px) / hs, vy = (by - py) / hs, vz = (bz - pz) / hs;
        if (len > 1e-9) {
            nx /= len;
            ny /= len;
            nz /= len;
            double rx = vx - sx, ry = vy - sy, rz = vz - sz;
            double vn = rx * nx + ry * ny + rz * nz;
            if (vn < 0) {
                impact = Math.max(impact, -vn);
                double k = (1 + BOUNCE) * vn;
                vx -= k * nx;
                vy -= k * ny;
                vz -= k * nz;
                // friction: the sliding part of the relative speed loses up to FRICTION times what the bump took
                double tx = rx - vn * nx, ty = ry - vn * ny, tz = rz - vn * nz;
                double tl = Math.sqrt(tx * tx + ty * ty + tz * tz);
                if (tl > 1e-9) {
                    double cut = Math.min(tl, FRICTION * -vn) / tl;
                    vx -= tx * cut;
                    vy -= ty * cut;
                    vz -= tz * cut;
                }
            }
        }
        bx = nbx;
        by = nby;
        bz = nbz;
        px = bx - vx * hs;
        py = by - vy * hs;
        pz = bz - vz * hs;
    }

    /**
     * The hinge stop: past MAX_SWING from straight down the weight is put back on the limit and its outward speed
     * stops (the handle hits the hook).
     */
    private void hinge(double ax, double ay, double az, double length, double hs) {
        double dx = bx - ax, dy = by - ay, dz = bz - az;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        double angle = Math.atan2(horiz, -dy);
        if (angle <= MAX_SWING || horiz < 1e-9) return;
        double vx = (bx - px) / hs, vy = (by - py) / hs, vz = (bz - pz) / hs;
        double ux = dx / horiz, uz = dz / horiz;
        bx = ax + ux * Math.sin(MAX_SWING) * length;
        by = ay - Math.cos(MAX_SWING) * length;
        bz = az + uz * Math.sin(MAX_SWING) * length;
        // the outward normal of the limit cone at this point: away from straight down, across the rod
        double nx = ux * Math.cos(MAX_SWING), ny = Math.sin(MAX_SWING), nz = uz * Math.cos(MAX_SWING);
        double vn = vx * nx + vy * ny + vz * nz;
        if (vn > 0) {
            vx -= vn * nx;
            vy -= vn * ny;
            vz -= vn * nz;
        }
        px = bx - vx * hs;
        py = by - vy * hs;
        pz = bz - vz * hs;
    }

    /** Keeps the weight above solid ground (a bed, the floor of a crawl space). */
    private void ground(float radius, double hs) {
        ClientLevel lv = level;
        if (lv == null) return;
        BlockPos at = BlockPos.containing(bx, by - radius, bz);
        BlockState st = lv.getBlockState(at);
        if (st.isAir()) return;
        VoxelShape shape = st.getCollisionShape(lv, at);
        if (shape.isEmpty()) return;
        double top = at.getY() + shape.max(Direction.Axis.Y);
        if (by - radius >= top) return;
        double vy = Math.max(0, (by - py) / hs);
        by = top + radius;
        py = by - vy * hs;
    }

    private void clink(double now, double strength) {
        ClientLevel lv = level;
        if (lv == null) return;
        lastClink = now;
        lv.playLocalSound(bx, by, bz, SoundEvents.LANTERN_HIT, SoundSource.PLAYERS, (float) (0.05 + 0.1 * strength),
            1.6F + (float) (Math.random() * 0.3), false);
    }

    private static double sq(double x, double y, double z) {
        return x * x + y * y + z * z;
    }

    private static double wrap(double a) {
        while (a > Math.PI) a -= 2 * Math.PI;
        while (a < -Math.PI) a += 2 * Math.PI;
        return a;
    }
}
