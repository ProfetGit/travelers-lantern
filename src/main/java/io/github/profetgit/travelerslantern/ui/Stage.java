package io.github.profetgit.travelerslantern.ui;

//? if >=26.3 {
import io.github.profetgit.profetsettings.Ctx;
import io.github.profetgit.profetsettings.Gfx;
import io.github.profetgit.profetsettings.Preview;
import io.github.profetgit.profetsettings.Rows;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

// What the preview scenes share: the loop clock, the backdrop's soft pool of light and the "is off" note.
abstract class Stage extends Preview {
    // the art is made for a W x H GUI pixel box (the medium layout's box is a little narrower)
    static final int W = 198, H = 150;

    static Identifier art(String name) {
        return Identifier.fromNamespaceAndPath("travelers_lantern", "textures/gui/preview/" + name + ".png");
    }

    // restarts the loop when any of the values the scene depends on changes
    float time(Ctx c, float period, Object... settings) {
        return clock(c, Objects.hash(settings), period);
    }

    static String t(String key) {
        return I18n.get("travelers_lantern.preview." + key);
    }

    static void backdrop(Gfx g, int x, int y) {
        g.g.blit(RenderPipelines.GUI_TEXTURED, art("glow"), x, y, 0, 0, W, H, W * 2, H * 2, W * 2, H * 2, g.a(0xFFFFFFFF));
    }

    // a soft round light of one colour
    static void halo(Gfx g, float cx, float cy, float radius, int argb) {
        g.pushTranslate(cx - radius, cy - radius);
        g.g.pose().scale(radius / 32, radius / 32);
        g.g.blit(RenderPipelines.GUI_TEXTURED, art("halo"), 0, 0, 0, 0, 64, 64, 64, 64, g.a(argb));
        g.pop();
    }

    static void note(Gfx g, int x, int y, int w, int h, String line) {
        List<String> lines = Rows.wrap(g, line, w - 8, 2);
        int top = y + h - 3 - lines.size() * 10;
        for (int i = 0; i < lines.size(); i++) g.vanilla(lines.get(i), x + (w - g.vanillaWidth(lines.get(i))) / 2, top + i * 10 + 1, 0xFFF0A566, true);
    }
}
//?}
