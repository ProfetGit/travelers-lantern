package io.github.profetgit.travelerslantern.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The lantern's block model, resolved once per block state (and again after a resource reload), drawn through
 * vanilla's own block-model submit, which Sodium's renderer also handles.
 */
public final class LanternModels {
    private static final Map<BlockState, BlockModelRenderState> MODELS = new IdentityHashMap<>();
    private static BlockModelResolver resolver;
    private static Object modelSet;

    private LanternModels() {
    }

    public static void submit(BlockState state, PoseStack ps, SubmitNodeCollector c, int light, int outline) {
        Minecraft mc = Minecraft.getInstance();
        Object set = mc.getModelManager().getBlockModelSet();
        if (set != modelSet) {
            modelSet = set;
            MODELS.clear();
            resolver = null;
        }
        BlockModelRenderState rs = MODELS.get(state);
        if (rs == null) {
            if (resolver == null) resolver = new BlockModelResolver(mc.getModelManager());
            rs = new BlockModelRenderState();
            resolver.update(rs, state, BlockDisplayContext.create());
            MODELS.put(state, rs);
        }
        rs.submit(ps, c, light, OverlayTexture.NO_OVERLAY, outline);
    }
}
