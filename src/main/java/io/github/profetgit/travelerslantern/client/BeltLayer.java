package io.github.profetgit.travelerslantern.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.profetgit.travelerslantern.Config;
import io.github.profetgit.travelerslantern.compat.Emf;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Draws the belt lantern on players (and mannequins), hanging at the side of the hip. It follows the body part as the
 * game (or a resource pack's animation, through Entity Model Features) posed it this frame.
 */
public final class BeltLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    /** The lantern at this fraction of block size: about a sixth of the player's height. */
    static final float SCALE = 0.42F;
    /**
     * Hook on the body part, in model pixels: x to the hip's side (+x is the player's left), y down, z forward (-). It
     * hangs just outside the hip, below the hand, so the thigh swings past it and the arm swings above it.
     */
    static final float HOOK_X = 4.5F, HOOK_Y = 12.4F, HOOK_Z = -0.3F;
    /** From the hook (the top of the hanging model's handle) to the middle of the lantern's body, in blocks. */
    static final float LENGTH = 11.5F / 16 * SCALE;
    /** Half the lantern body's width, in blocks. */
    static final float RADIUS = 3F / 16 * SCALE;

    /** Test hooks: lanterns drawn on the local player and on other players so far. */
    public static int drawnLocal, drawnOthers;

    public BeltLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack ps, SubmitNodeCollector c, int light, AvatarRenderState st, float yRot, float xRot) {
        BlockState lantern = ((BeltState) st).travelerslantern$lantern();
        if (lantern == null || st.isSpectator) return;
        PlayerModel m = getParentModel();
        boolean left = Config.get().leftSide;
        float side = left ? 1 : -1;

        Matrix4f toWorld = new Matrix4f(((BeltState) st).travelerslantern$root()).invert().mul(ps.last().pose());
        ps.pushPose();
        m.root().translateAndRotate(ps);
        m.body.translateAndRotate(ps);
        ps.translate(side * HOOK_X / 16, HOOK_Y / 16, HOOK_Z / 16);
        Matrix4f hook = new Matrix4f(((BeltState) st).travelerslantern$root()).invert().mul(ps.last().pose());

        Swing.Box[] boxes = {
            box(m, m.body, 0, 6, 0, 4, 6, 2, 0),
            box(m, left ? m.leftLeg : m.rightLeg, 0, 6, 0, 2, 6, 2, left ? 1 : -1)
        };
        Quaternionf q = Swing.orient(st.id, BeltClient.worldPass(), st.ageInTicks, st.bodyRot, st.x, st.y, st.z,
            hook, toWorld, boxes, LENGTH * st.scale, RADIUS, st.walkAnimationPos, st.walkAnimationSpeed, Emf.animated(m) ? 0.25F : 1F, side);
        ps.rotateAround(q, 0, 0, 0);
        // block space has +y up; the model's +y points down
        ps.rotateAround(Axis.XP.rotationDegrees(180), 0, 0, 0);
        ps.scale(SCALE, SCALE, SCALE);
        ps.translate(-0.5F, -1.0F, -0.5F);
        int lit = LightCoordsUtil.pack(Math.max(LightCoordsUtil.block(light), lantern.getLightEmission()), LightCoordsUtil.sky(light));
        LanternModels.submit(lantern, ps, c, lit, st.outlineColor);
        if (net.minecraft.client.Minecraft.getInstance().player != null && st.id == net.minecraft.client.Minecraft.getInstance().player.getId()) drawnLocal++;
        else drawnOthers++;
        ps.popPose();
    }

    /** A body box in model space: centre and half size in pixels, in the part's own frame. */
    private static Swing.Box box(PlayerModel m, ModelPart part, float cx, float cy, float cz, float hx, float hy, float hz, int out) {
        PoseStack p = new PoseStack();
        m.root().translateAndRotate(p);
        part.translateAndRotate(p);
        return new Swing.Box(new Matrix4f(p.last().pose()), cx / 16, cy / 16, cz / 16, hx / 16, hy / 16, hz / 16, out);
    }
}
