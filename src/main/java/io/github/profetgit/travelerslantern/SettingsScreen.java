package io.github.profetgit.travelerslantern;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

/**
 * The settings button in Mod Menu / the loader's mod list: the client keys of the config file. Changes apply at once
 * (every key is read live) and are written when the screen closes. Labels are plain English: without Fabric API a
 * Fabric mod's language files aren't loaded.
 */
public final class SettingsScreen extends OptionsSubScreen {
    public SettingsScreen(Screen parent) {
        super(parent, Minecraft.getInstance().options, Component.literal("Traveler's Lantern"));
    }

//? if >=1.21 {
    @Override
    protected void addOptions() {
        list.addSmall(options());
    }
//?} else {
/*    private net.minecraft.client.gui.components.OptionsList list;

    @Override
    protected void init() {
        list = new net.minecraft.client.gui.components.OptionsList(minecraft, width, height, 32, height - 32, 25);
        list.addSmall(options());
        addWidget(list);
        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(net.minecraft.network.chat.CommonComponents.GUI_DONE,
            b -> minecraft.setScreen(lastScreen)).bounds(width / 2 - 100, height - 27, 200, 20).build());
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        list.render(g, mouseX, mouseY, partial);
        g.drawCenteredString(font, title, width / 2, 20, 0xFFFFFF);
        super.render(g, mouseX, mouseY, partial);
    }
*///?}

    static OptionInstance<?>[] options() {
        Config c = Config.get();
        return new OptionInstance<?>[] {
            OptionInstance.createBoolean("Clink sounds", OptionInstance.cachedConstantTooltip(Component.literal("The lantern clinks when it knocks against your leg. Off: silent on the belt.")),
                c.clinks, v -> c.clinks = v),
            new OptionInstance<>("Clink volume", OptionInstance.cachedConstantTooltip(Component.literal("How loud the clinks are.")),
                (caption, v) -> Component.literal("Clink volume: " + v * 10 + "%"), new OptionInstance.IntRange(0, 10),
                Math.max(0, Math.min(10, Math.round(c.clinkVolume / 10f))), v -> c.clinkVolume = v * 10),
            OptionInstance.createBoolean("Left hip", OptionInstance.cachedConstantTooltip(Component.literal("The lantern hangs on your left hip. Off: the right one.")),
                c.leftSide, v -> c.leftSide = v),
            OptionInstance.createBoolean("Lantern light", OptionInstance.cachedConstantTooltip(Component.literal("Belt lanterns light what you see.")),
                c.clientLight, v -> c.clientLight = v),
            OptionInstance.createBoolean("Smooth light", OptionInstance.cachedConstantTooltip(Component.literal("The light moves smoothly with the lantern. Off: it moves a block at a time, which is cheaper on slow machines.")),
                c.smoothLight, v -> c.smoothLight = v),
            new OptionInstance<>("Light updates", OptionInstance.cachedConstantTooltip(Component.literal("How often smooth light follows the lantern. Higher is smoother and costs more.")),
                (caption, v) -> Component.literal("Light updates: " + v + " / s"), new OptionInstance.IntRange(20, 60),
                Math.max(20, Math.min(60, c.lightUpdatesPerSecond)), v -> c.lightUpdatesPerSecond = v),
            OptionInstance.createBoolean("Softer in daylight", OptionInstance.cachedConstantTooltip(Component.literal("In daylight or a lit room the light glows softly. Off: full strength.")),
                c.softInDaylight, v -> c.softInDaylight = v)};
    }

    @Override
    public void removed() {
        Config.save();
        super.removed();
    }
}
