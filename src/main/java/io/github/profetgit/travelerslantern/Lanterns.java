package io.github.profetgit.travelerslantern;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;

/** What counts as a lantern: any item that places a LanternBlock (vanilla, soul, the copper ones, modded ones). */
public final class Lanterns {
    private Lanterns() {
    }

    public static boolean is(ItemStack s) {
        return s != null && !s.isEmpty() && s.getItem() instanceof BlockItem b && b.getBlock() instanceof LanternBlock;
    }

    /** The block the lantern hangs as (hanging=true, so the model has its handle loop up). */
    public static BlockState hanging(ItemStack s) {
        BlockState st = ((BlockItem) s.getItem()).getBlock().defaultBlockState();
        return st.hasProperty(LanternBlock.HANGING) ? st.setValue(LanternBlock.HANGING, true) : st;
    }

    /**
     * The colour of the lantern's light as a shader pack's held-light uniform wants it (red, green, blue, 0 to 1), from
     * the flame in each vanilla texture: amber for a lantern, blue-cyan for a soul lantern, green for every copper
     * lantern (waxed or oxidized, the flame doesn't change). Only Iris's held light carries a colour; the world's block
     * light is a single brightness.
     */
    public static float[] color(ItemStack s) {
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
        if (id.contains("soul")) return new float[] {0.35F, 0.90F, 0.95F};
        if (id.contains("copper")) return new float[] {0.50F, 0.95F, 0.55F};
        return new float[] {1.00F, 0.78F, 0.40F};
    }

    /** Light level the lantern gives (15 for a lantern, 10 for a soul lantern). */
    public static int light(ItemStack s) {
        return Math.max(0, Math.min(15, hanging(s).getLightEmission()));
    }
}
