package io.github.profetgit.travelerslantern.ui;

//? if >=26.3 {
import io.github.profetgit.travelerslantern.Lanterns;
import io.github.profetgit.travelerslantern.client.BeltClient;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Vector3f;

// The lantern, the ground it lights and the shadow under the walker, from the game's own models and textures.
final class Props {
    // what a point of the ground at (x, z) looks like: its colour, alpha included
    interface Shade {
        int at(float x, float z, Vector3f screen);
    }

    private static final String[] STONES = {"stone", "cobblestone", "mossy_cobblestone"};

    private Props() {
    }

    // the lantern on the player's belt, else the plain one
    static BlockState lantern() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            ItemStack s = BeltClient.beltOf(mc.player);
            if (Lanterns.is(s)) return Lanterns.hanging(s);
        }
        return Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
    }

    static void lantern(Mesh mesh, Cam cam, BlockState state, Matrix4f toWorld) {
        List<BlockStateModelPart> parts = new ArrayList<>();
        Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state).collectParts(RandomSource.create(42), parts);
        for (BlockStateModelPart part : parts) {
            for (int d = -1; d < Direction.values().length; d++) {
                for (BakedQuad q : part.getQuads(d < 0 ? null : Direction.values()[d])) {
                    Vector3f[] w = new Vector3f[4];
                    for (int i = 0; i < 4; i++) w[i] = toWorld.transformPosition(new Vector3f(q.position(i)));
                    Vector3f n = new Vector3f(w[1]).sub(w[0]).cross(new Vector3f(w[2]).sub(w[0]));
                    if (!cam.faces(n)) continue;
                    float shade = switch (q.direction()) {
                        case UP -> 1;
                        case DOWN -> 0.7F;
                        case NORTH, SOUTH -> 0.95F;
                        default -> 0.86F;
                    };
                    int c = ARGB.colorFromFloat(1, shade, shade, shade);
                    Vector3f[] s = new Vector3f[4];
                    float[] u = new float[4], v = new float[4];
                    for (int i = 0; i < 4; i++) {
                        s[i] = cam.project(w[i]);
                        u[i] = UVPair.unpackU(q.packedUV(i));
                        v[i] = UVPair.unpackV(q.packedUV(i));
                    }
                    mesh.add(q.materialInfo().sprite().atlasLocation(), s, u, v, new int[] {c, c, c, c});
                }
            }
        }
    }

    // the ground block whose top has its corner at (x, z), cut into cut x cut pieces; {@code kind} picks its stone
    static void ground(Mesh mesh, Cam cam, float x, float z, int kind, int cut, Shade shade) {
        int h = Math.floorMod(kind, 97);
        String name = STONES[h < 9 ? 2 : h < 24 ? 1 : 0];
        TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS)
            .getSprite(Identifier.withDefaultNamespace("block/" + name));
        for (int a = 0; a < cut; a++) {
            for (int b = 0; b < cut; b++) {
                float[][] at = {{a, b}, {a, b + 1}, {a + 1, b + 1}, {a + 1, b}};
                Vector3f[] s = new Vector3f[4];
                float[] u = new float[4], v = new float[4];
                int[] c = new int[4];
                boolean seen = false;
                for (int i = 0; i < 4; i++) {
                    float fx = at[i][0] / cut, fz = at[i][1] / cut;
                    s[i] = cam.project(new Vector3f(x + fx, 0, z + fz));
                    u[i] = sprite.getU0() + (sprite.getU1() - sprite.getU0()) * fx;
                    v[i] = sprite.getV0() + (sprite.getV1() - sprite.getV0()) * fz;
                    c[i] = shade.at(x + fx, z + fz, s[i]);
                    seen |= (c[i] >>> 24) > 1;
                }
                if (seen) mesh.add(TextureAtlas.LOCATION_BLOCKS, s, u, v, c);
            }
        }
    }

    static void shadow(Mesh mesh, Cam cam, float x, float z, float yaw) {
        Matrix4f m = new Matrix4f().translate(x, 0, z).rotateY((float) Math.toRadians(-yaw));
        float hw = 0.45F, hl = 0.62F;
        float[][] at = {{-hw, -hl}, {-hw, hl}, {hw, hl}, {hw, -hl}};
        float[] u = {0, 1, 1, 0}, v = {0, 0, 1, 1};
        Vector3f[] s = new Vector3f[4];
        for (int i = 0; i < 4; i++) s[i] = cam.project(m.transformPosition(new Vector3f(at[i][0], 0, at[i][1])));
        int c = 0x90FFFFFF;
        mesh.add(Stage.art("shadow_item"), s, u, v, new int[] {c, c, c, c});
    }

    // 1 inside an ellipse of the box, easing to 0 at its edge; {@code soft} is the share of the radius the easing takes
    static float oval(float x, float y, float cx, float cy, float rx, float ry, float soft) {
        float d = (float) Math.hypot((x - cx) / rx, (y - cy) / ry);
        float u = Math.max(0, Math.min(1, (1 - d) / soft));
        return u * u * (3 - 2 * u);
    }

    private static final float[] NIGHT = {0.115F, 0.125F, 0.185F}, DAY = {0.80F, 0.82F, 0.86F};

    // a surface's colour under block light {@code level} (0 to 15) and {@code sky} daylight (0 night to 1 day)
    static float[] lit(float level, float sky) {
        float f = Math.max(0, Math.min(1, level / 15));
        float b = f / (4 - 3 * f) * 0.75F + f * 0.25F;
        float[] warm = {b * 1.02F, b * ((b * 0.6F + 0.4F) * 0.6F + 0.4F) * 0.86F, b * (b * b * 0.6F + 0.4F) * 0.66F};
        float[] out = new float[3];
        for (int i = 0; i < 3; i++) {
            float base = NIGHT[i] + (DAY[i] - NIGHT[i]) * sky;
            out[i] = Math.min(1, base + warm[i] * (1 - base * 0.8F));
        }
        return out;
    }
}
//?}
