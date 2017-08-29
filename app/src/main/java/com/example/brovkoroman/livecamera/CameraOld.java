package com.example.brovkoroman.livecamera;

import android.hardware.Camera;

/**
 * Created by brovkoroman on 29.08.17.
 */

@SuppressWarnings("deprecation")
public class CameraOld implements CameraSupport {

    private Camera mCamera;

    @Override
    public CameraSupport open() {
        mCamera = Camera.open();
        return this;
    }

    @Override
    public CameraSupport open(final int cameraId) {
        mCamera = Camera.open(cameraId);
        return this;
    }
}
