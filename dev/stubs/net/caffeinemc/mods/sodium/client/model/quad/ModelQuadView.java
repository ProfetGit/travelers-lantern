package net.caffeinemc.mods.sodium.client.model.quad;

/** Compile-time stand-in for Sodium's quad view (only what Traveler's Lantern calls). Never shipped. Sodium up to 0.9.3-beta.1 names the accessors getX; from beta.2 they are sodium$getX. */
public interface ModelQuadView {
    float getX(int i);

    float getY(int i);

    float getZ(int i);

    float sodium$getX(int i);

    float sodium$getY(int i);

    float sodium$getZ(int i);
}
