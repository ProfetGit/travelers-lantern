package io.github.profetgit.travelerslantern.mixin.client;

//? if >=26.3 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The settings previews draw their own quads into the GUI (ui/Mesh). */
@Mixin(GuiGraphicsExtractor.class)
public interface GuiGraphicsAccessor {
    @Accessor("guiRenderState")
    GuiRenderState travelerslantern$guiState();
}
//?} else {
/*import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

// Only the 26.3 settings previews need this; the mixin list is shared by every version.
@Mixin(Minecraft.class)
public abstract class GuiGraphicsAccessor {
}
*///?}
