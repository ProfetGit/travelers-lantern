package io.github.profetgit.beltlantern.client;

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

    private SodiumLight() {
    }

    public static void apply(ModelQuadView quad, BlockPos pos, QuadLightData out) {
        if (!DynamicLight.active()) return;
        quads++;
        float x0 = quad.getX(0), y0 = quad.getY(0), z0 = quad.getZ(0);
        float ax = quad.getX(1) - x0, ay = quad.getY(1) - y0, az = quad.getZ(1) - z0;
        float bx = quad.getX(2) - x0, by = quad.getY(2) - y0, bz = quad.getZ(2) - z0;
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
            double x = pos.getX() + quad.getX(i), y = pos.getY() + quad.getY(i), z = pos.getZ() + quad.getZ(i);
            out.lm[i] = faced ? DynamicLight.applyFace(out.lm[i], x, y, z, nx, ny, nz) : DynamicLight.apply(out.lm[i], x, y, z);
        }
    }
}
