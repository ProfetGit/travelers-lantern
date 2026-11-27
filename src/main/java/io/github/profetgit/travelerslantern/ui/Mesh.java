package io.github.profetgit.travelerslantern.ui;

//? if >=26.3 {
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import io.github.profetgit.profetsettings.Gfx;
import io.github.profetgit.travelerslantern.mixin.client.GuiGraphicsAccessor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Vector3f;

// Textured quads of a small 3D scene, drawn by the GUI: collected, painted far to near, one draw per run of a texture.
final class Mesh {
    private record Quad(Identifier tex, float[] x, float[] y, float[] u, float[] v, int[] colour, float depth) {
    }

    private final List<Quad> quads = new ArrayList<>();

    // corners as screen x, y and depth (larger is nearer), going round the quad; either direction
    void add(Identifier tex, Vector3f[] p, float[] u, float[] v, int[] colour) {
        float area = 0;
        for (int i = 0; i < 4; i++) area += p[i].x * p[(i + 1) % 4].y - p[(i + 1) % 4].x * p[i].y;
        float[] qx = new float[4], qy = new float[4], qu = new float[4], qv = new float[4];
        int[] qc = new int[4];
        float depth = 0;
        for (int i = 0; i < 4; i++) {
            // the GUI culls one winding
            int k = area < 0 ? i : (4 - i) % 4;
            qx[i] = p[k].x;
            qy[i] = p[k].y;
            qu[i] = u[k];
            qv[i] = v[k];
            qc[i] = colour[k];
            depth += p[k].z / 4;
        }
        quads.add(new Quad(tex, qx, qy, qu, qv, qc, depth));
    }

    // draws and empties the mesh, clipped to the box
    void draw(Gfx g, int x, int y, int w, int h, boolean sort) {
        if (sort) quads.sort(Comparator.comparingDouble(Quad::depth));
        Matrix3x2f pose = new Matrix3x2f(g.g.pose());
        ScreenRectangle area = new ScreenRectangle(x, y, w, h).transformMaxBounds(pose);
        for (int from = 0; from < quads.size(); ) {
            Identifier tex = quads.get(from).tex();
            int to = from + 1;
            while (to < quads.size() && quads.get(to).tex().equals(tex)) to++;
            AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(tex);
            List<Quad> run = new ArrayList<>(quads.subList(from, to));
            for (Quad q : run) for (int i = 0; i < 4; i++) q.colour()[i] = g.a(q.colour()[i]);
            ((GuiGraphicsAccessor) g.g).travelerslantern$guiState()
                .addGuiElement(new Batch(TextureSetup.singleTexture(texture.getTextureView(), texture.getSampler()), run, pose, area));
            from = to;
        }
        quads.clear();
    }

    private record Batch(TextureSetup textureSetup, List<Quad> quads, Matrix3x2f pose, ScreenRectangle area) implements GuiElementRenderState {
        @Override
        public void buildVertices(VertexConsumer out) {
            for (Quad q : quads) for (int i = 0; i < 4; i++) out.addVertexWith2DPose(pose, q.x()[i], q.y()[i]).setUv(q.u()[i], q.v()[i]).setColor(q.colour()[i]);
        }

        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.GUI_TEXTURED;
        }

        @Override
        public ScreenRectangle scissorArea() {
            return area;
        }

        @Override
        public ScreenRectangle bounds() {
            return area;
        }
    }
}
//?}
