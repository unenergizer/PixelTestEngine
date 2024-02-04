package com.mmobuilder.voxel;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;

public final class CameraUtils {
    private static final int RESOLUTION_FACTOR = 750;

    public static OrthographicCamera createCamera(int viewportWidth, int viewportHeight) {
        OrthographicCamera camera = new OrthographicCamera(
            (float) viewportWidth / RESOLUTION_FACTOR,
            (float) viewportHeight / RESOLUTION_FACTOR);
        camera.near = 0.01f;
        camera.far = 100f;
        camera.position.set(0f, 10f, -5f);
        camera.direction.rotate(Vector3.X, -55.0F);
        camera.direction.rotate(Vector3.Y, 45.0F);
        camera.up.set(Vector3.Y);
        return camera;
    }
}
