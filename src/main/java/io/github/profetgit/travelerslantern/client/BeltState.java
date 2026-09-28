package io.github.profetgit.travelerslantern.client;

import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

/** Extra fields on AvatarRenderState (AvatarRenderStateMixin). */
public interface BeltState {
    /** The lantern hanging on this avatar's belt (hanging=true), or null. */
    BlockState travelerslantern$lantern();

    void travelerslantern$setLantern(BlockState state);

    /** The pose the entity's render started from (the camera offset), set at LivingEntityRenderer.submit. */
    Matrix4f travelerslantern$root();
}
