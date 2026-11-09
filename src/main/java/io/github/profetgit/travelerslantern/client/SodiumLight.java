package io.github.profetgit.travelerslantern.client;

import net.caffeinemc.mods.sodium.client.model.light.data.QuadLightData;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.minecraft.core.BlockPos;

/**
 * Sodium builds chunk meshes with its own light pipelines, which keep whole light levels per block; the lanterns' light
 * is added afterwards per vertex, at the vertex's exact position, so it stays smooth. Only loaded when Sodium is.
 */
public final class SodiumLight {
    /** Test hook: quads Sodium lit while a lantern was on. */
    public static volatile long quads;

    /** Sodium 0.9.3-beta.2 renamed the quad accessors getX to sodium$getX; a call to the missing one is a NoSuchMethodError. */
    private interface Reader {
        float x(ModelQuadView q, int i);

        float y(ModelQuadView q, int i);

        float z(ModelQuadView q, int i);
    }

    private static final Reader READER = prefixed() ? new Reader() {
        public float x(ModelQuadView q, int i) { return q.sodium$getX(i); }
        public float y(ModelQuadView q, int i) { return q.sodium$getY(i); }
        public float z(ModelQuadView q, int i) { return q.sodium$getZ(i); }
    } : new Reader() {
        public float x(ModelQuadView q, int i) { return q.getX(i); }
        public float y(ModelQuadView q, int i) { return q.getY(i); }
        public float z(ModelQuadView q, int i) { return q.getZ(i); }
    };

    private static boolean prefixed() {
        try {
            ModelQuadView.class.getMethod("sodium$getX", int.class);
            return true;
        } catch (Throwable absent) {
            return false;
        }
    }

    private SodiumLight() {
    }

    public static void apply(ModelQuadView quad, BlockPos pos, QuadLightData out) {
        if (!DynamicLight.active()) return;
        quads++;
        Reader r = READER;
        float x0 = r.x(quad, 0), y0 = r.y(quad, 0), z0 = r.z(quad, 0);
        float ax = r.x(quad, 1) - x0, ay = r.y(quad, 1) - y0, az = r.z(quad, 1) - z0;
        float bx = r.x(quad, 2) - x0, by = r.y(quad, 2) - y0, bz = r.z(quad, 2) - z0;
        // the face normal from the winding (counter-clockwise seen from outside)
        float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        boolean faced = len > 1e-6F;
        if (faced) {
            nx /= len;
            ny /= len;
            nz /= len;
        }
        for (int i = 0; i < 4; i++) {
            double x = pos.getX() + r.x(quad, i), y = pos.getY() + r.y(quad, i), z = pos.getZ() + r.z(quad, i);
            out.lm[i] = faced ? DynamicLight.applyFace(out.lm[i], x, y, z, nx, ny, nz) : DynamicLight.apply(out.lm[i], x, y, z);
        }
    }
}
