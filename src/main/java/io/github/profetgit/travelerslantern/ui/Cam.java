package io.github.profetgit.travelerslantern.ui;

//? if >=26.3 {
import org.joml.Matrix4f;
import org.joml.Vector3f;

// A straight-on (orthographic) camera: turned {@code yaw}, tilted down {@code pitch}, {@code scale} GUI pixels a block,
// with the world point {@code at} on the screen point (cx, cy).
final class Cam {
    final Matrix4f view;
    private final float scale, cx, cy;
    private final Vector3f at;

    Cam(float yaw, float pitch, float scale, float cx, float cy, float ax, float ay, float az) {
        this.view = new Matrix4f().rotateX((float) Math.toRadians(pitch)).rotateY((float) Math.toRadians(yaw));
        this.scale = scale;
        this.cx = cx;
        this.cy = cy;
        this.at = new Vector3f(ax, ay, az);
    }

    // screen x, y and depth (larger is nearer)
    Vector3f project(Vector3f world) {
        Vector3f q = view.transformDirection(world.sub(at, new Vector3f()));
        return new Vector3f(cx + q.x * scale, cy - q.y * scale, q.z);
    }

    boolean faces(Vector3f worldNormal) {
        return view.transformDirection(worldNormal, new Vector3f()).z > 1e-5F;
    }
}
//?}
