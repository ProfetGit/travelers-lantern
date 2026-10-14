package io.github.profetgit.travelerslantern.mixin;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

/**
 * Sodium changed the parameters of its light pipelines' calculate between builds (three directions and a boolean, or two
 * directions and two booleans), and a handler that doesn't match fails the mixin with "Invalid descriptor" even with
 * require = 0, which crashes the game. Each Sodium mixin has one variant per signature; only the one that matches the
 * installed Sodium is applied, none if it's something else.
 */
public final class MixinPlugin implements IMixinConfigPlugin {
    private static final String HEAD = "(Lnet/caffeinemc/mods/sodium/client/model/quad/ModelQuadView;Lnet/minecraft/core/BlockPos;"
            + "Lnet/caffeinemc/mods/sodium/client/model/light/data/QuadLightData;";
    private static final String THREE_DIRECTIONS = HEAD + "Lnet/minecraft/core/Direction;Lnet/minecraft/core/Direction;Lnet/minecraft/core/Direction;Z)V";
    private static final String TWO_DIRECTIONS = HEAD + "Lnet/minecraft/core/Direction;Lnet/minecraft/core/Direction;ZZ)V";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!mixinClassName.contains(".SodiumFlat") && !mixinClassName.contains(".SodiumSmooth")) return true;
        String wanted = mixinClassName.endsWith("MixinB") ? TWO_DIRECTIONS : THREE_DIRECTIONS;
        try {
            ClassNode node = MixinService.getService().getBytecodeProvider().getClassNode(targetClassName);
            for (MethodNode m : node.methods) {
                if (m.name.equals("calculate") && m.desc.equals(wanted)) return true;
            }
        } catch (Throwable absent) {
        }
        return false;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
