package com.example.brovkoroman.livecamera;

/**
 * Created by brovkoroman on 29.08.17.
 */

public interface CameraSupport {
    CameraSupport open();
    CameraSupport open(int cameraId);
}
