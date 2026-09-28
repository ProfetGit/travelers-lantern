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

    /** Light level the lantern gives (15 for a lantern, 10 for a soul lantern). */
    public static int light(ItemStack s) {
        return Math.max(0, Math.min(15, hanging(s).getLightEmission()));
    }
}
