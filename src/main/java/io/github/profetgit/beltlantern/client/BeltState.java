package io.github.profetgit.beltlantern.client;

import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

/** Extra fields on AvatarRenderState (AvatarRenderStateMixin). */
public interface BeltState {
    /** The lantern hanging on this avatar's belt (hanging=true), or null. */
    BlockState beltlantern$lantern();

    void beltlantern$setLantern(BlockState state);

    /** The pose the entity's render started from (the camera offset), set at LivingEntityRenderer.submit. */
    Matrix4f beltlantern$root();
}
