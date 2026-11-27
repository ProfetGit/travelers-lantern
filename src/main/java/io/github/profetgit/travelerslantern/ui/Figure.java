package io.github.profetgit.travelerslantern.ui;

//? if >=26.3 {
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import org.joml.Matrix4f;
import org.joml.Vector3f;

// The player in their own skin. Each skin pixel is its own quad, and the outer layer stands off the body as little
// blocks, one a pixel (transparent pixels draw nothing).
final class Figure {
    // part, min corner and size (model pixels), the skin's offsets of the base and outer layer, how far the outer one stands off
    private record Part(int part, float x, float y, float z, int w, int h, int d, int u, int v, int ou, int ov, float lift) {
    }

    private static java.util.function.Supplier<PlayerSkin> lookup;

    private Figure() {
    }

    static PlayerSkin skin() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) return mc.player.getSkin();
        if (lookup == null) lookup = mc.getSkinManager().createLookup(mc.getGameProfile(), false);
        return lookup.get();
    }

    static void add(Mesh mesh, Cam cam, Matrix4f toWorld, Matrix4f[] pose, float[] light) {
        PlayerSkin skin = skin();
        boolean slim = skin.model() == PlayerModelType.SLIM;
        Identifier tex = skin.body().texturePath();
        int arm = slim ? 3 : 4;
        Part[] parts = {
            new Part(0, -4, -8, -4, 8, 8, 8, 0, 0, 32, 0, 0.55F),
            new Part(1, -4, 0, -2, 8, 12, 4, 16, 16, 16, 32, 0.35F),
            new Part(2, slim ? -2 : -3, -2, -2, arm, 12, 4, 40, 16, 40, 32, 0.35F),
            new Part(3, -1, -2, -2, arm, 12, 4, 32, 48, 48, 48, 0.35F),
            new Part(4, -2, 0, -2, 4, 12, 4, 0, 16, 0, 32, 0.35F),
            new Part(5, -2, 0, -2, 4, 12, 4, 16, 48, 0, 48, 0.35F)
        };
        for (Part p : parts) {
            Matrix4f m = new Matrix4f(toWorld).mul(pose[p.part()]);
            layer(mesh, cam, tex, m, p, p.u(), p.v(), 0, light);
            layer(mesh, cam, tex, m, p, p.ou(), p.ov(), p.lift(), light);
        }
    }

    private static void layer(Mesh mesh, Cam cam, Identifier tex, Matrix4f m, Part p, int u, int v, float lift, float[] light) {
        float x0 = p.x(), y0 = p.y(), z0 = p.z(), x1 = x0 + p.w(), y1 = y0 + p.h(), z1 = z0 + p.d();
        int w = p.w(), h = p.h(), d = p.d();
        // origin, the texture's u and v directions, normal (model space: y down, front -z), texture rectangle, shade
        face(mesh, cam, tex, m, p, lift, light, v3(x0, y0, z0), v3(w, 0, 0), v3(0, h, 0), v3(0, 0, -1), u + d, v + d, w, h, 0.92F);
        face(mesh, cam, tex, m, p, lift, light, v3(x1, y0, z1), v3(-w, 0, 0), v3(0, h, 0), v3(0, 0, 1), u + 2 * d + w, v + d, w, h, 0.72F);
        face(mesh, cam, tex, m, p, lift, light, v3(x0, y0, z1), v3(0, 0, -d), v3(0, h, 0), v3(-1, 0, 0), u, v + d, d, h, 0.78F);
        face(mesh, cam, tex, m, p, lift, light, v3(x1, y0, z0), v3(0, 0, d), v3(0, h, 0), v3(1, 0, 0), u + d + w, v + d, d, h, 0.78F);
        face(mesh, cam, tex, m, p, lift, light, v3(x0, y0, z1), v3(w, 0, 0), v3(0, 0, -d), v3(0, -1, 0), u + d, v, w, d, 1.0F);
        face(mesh, cam, tex, m, p, lift, light, v3(x0, y1, z1), v3(w, 0, 0), v3(0, 0, -d), v3(0, 1, 0), u + d + w, v, w, d, 0.6F);
    }

    private static Vector3f v3(float x, float y, float z) {
        return new Vector3f(x, y, z);
    }

    private static void face(Mesh mesh, Cam cam, Identifier tex, Matrix4f m, Part p, float lift, float[] light, Vector3f o, Vector3f du,
                             Vector3f dv, Vector3f n, int su, int sv, int sw, int sh, float shade) {
        boolean front = cam.faces(m.transformDirection(n, new Vector3f()));
        if (!front && lift == 0) return;
        Vector3f centre = v3(p.x() + p.w() / 2F, p.y() + p.h() / 2F, p.z() + p.d() / 2F);
        Vector3f grow = v3(1 + 2 * lift / p.w(), 1 + 2 * lift / p.h(), 1 + 2 * lift / p.d());
        for (int j = 0; j < sh; j++) {
            for (int i = 0; i < sw; i++) {
                Vector3f[] top = {
                    corner(o, du, dv, (float) i / sw, (float) j / sh, centre, grow),
                    corner(o, du, dv, (float) i / sw, (j + 1F) / sh, centre, grow),
                    corner(o, du, dv, (i + 1F) / sw, (j + 1F) / sh, centre, grow),
                    corner(o, du, dv, (i + 1F) / sw, (float) j / sh, centre, grow)
                };
                float tu = (su + i + 0.5F) / 64, tv = (sv + j + 0.5F) / 64;
                if (front) quad(mesh, cam, tex, m, top, tu, tv, shade, light);
                if (lift == 0) continue;
                Vector3f middle = new Vector3f(top[0]).add(top[2]).mul(0.5F);
                for (int e = 0; e < 4; e++) {
                    Vector3f a = top[e], b = top[(e + 1) % 4];
                    Vector3f out = new Vector3f(a).add(b).mul(0.5F).sub(middle);
                    if (!cam.faces(m.transformDirection(out))) continue;
                    Vector3f down = new Vector3f(n).mul(-lift);
                    quad(mesh, cam, tex, m, new Vector3f[] {a, b, new Vector3f(b).add(down), new Vector3f(a).add(down)}, tu, tv, shade * 0.68F, light);
                }
            }
        }
    }

    private static Vector3f corner(Vector3f o, Vector3f du, Vector3f dv, float a, float b, Vector3f centre, Vector3f grow) {
        Vector3f q = new Vector3f(o).add(du.x * a + dv.x * b, du.y * a + dv.y * b, du.z * a + dv.z * b);
        return q.sub(centre).mul(grow).add(centre);
    }

    private static void quad(Mesh mesh, Cam cam, Identifier tex, Matrix4f m, Vector3f[] model, float u, float v, float shade, float[] light) {
        Vector3f[] s = new Vector3f[4];
        for (int i = 0; i < 4; i++) s[i] = cam.project(m.transformPosition(new Vector3f(model[i]).div(16)));
        int c = ARGB.colorFromFloat(1, Math.min(1, light[0] * shade), Math.min(1, light[1] * shade), Math.min(1, light[2] * shade));
        mesh.add(tex, s, new float[] {u, u, u, u}, new float[] {v, v, v, v}, new int[] {c, c, c, c});
    }
}
//?}
