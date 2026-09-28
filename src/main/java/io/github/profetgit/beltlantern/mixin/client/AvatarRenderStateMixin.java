package io.github.profetgit.beltlantern.mixin.client;

import io.github.profetgit.beltlantern.client.BeltState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public abstract class AvatarRenderStateMixin implements BeltState {
    @Unique
    private BlockState beltlantern$lantern;
    @Unique
    private final Matrix4f beltlantern$root = new Matrix4f();

    @Override
    public BlockState beltlantern$lantern() {
        return beltlantern$lantern;
    }

    @Override
    public void beltlantern$setLantern(BlockState state) {
        beltlantern$lantern = state;
    }

    @Override
    public Matrix4f beltlantern$root() {
        return beltlantern$root;
    }
}
