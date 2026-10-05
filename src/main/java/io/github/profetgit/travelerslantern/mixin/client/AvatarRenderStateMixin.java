package io.github.profetgit.travelerslantern.mixin.client;

//? if >=1.21.2 {
import io.github.profetgit.travelerslantern.client.BeltState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public abstract class AvatarRenderStateMixin implements BeltState {
    @Unique
    private BlockState travelerslantern$lantern;
    @Unique
    private final Matrix4f travelerslantern$root = new Matrix4f();

    @Override
    public BlockState travelerslantern$lantern() {
        return travelerslantern$lantern;
    }

    @Override
    public void travelerslantern$setLantern(BlockState state) {
        travelerslantern$lantern = state;
    }

    @Override
    public Matrix4f travelerslantern$root() {
        return travelerslantern$root;
    }
}
//?} else {
/*import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

// Render states arrived in 1.21.2: nothing to add here, kept so the mixin list is the same for every version.
@Mixin(Minecraft.class)
public abstract class AvatarRenderStateMixin {
}
*///?}
