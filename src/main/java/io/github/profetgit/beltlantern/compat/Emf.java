package io.github.profetgit.beltlantern.compat;

import net.minecraft.client.model.Model;

/**
 * Entity Model Features (the mod behind Fresh Animations): when a resource pack gives the player model its own
 * animation, EMF swaps the model's root part for its own class. Such a pack already moves the hips when walking, so
 * the swing adds less stride motion of its own. Checked by class name, so EMF stays optional and unlinked.
 */
public final class Emf {
    private Emf() {
    }

    public static boolean animated(Model<?> model) {
        return model.root().getClass().getName().startsWith("traben.entity_model_features");
    }
}
