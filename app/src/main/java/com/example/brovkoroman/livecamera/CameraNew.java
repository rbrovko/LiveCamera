package com.example.brovkoroman.livecamera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.util.Size;
import android.view.Surface;

import java.util.Arrays;

/**
 * Created by brovkoroman on 29.08.17.
 */

public class CameraNew implements CameraSupport {

    /**
     * ID of the current {@link CameraDevice}
     */
    private String mCameraId;
    /**
     * A {@link CameraCaptureSession } for camera preview
     */
    private CameraCaptureSession mCaptureSession;
    /**
     * A reference to the opened {@link CameraDevice}
     */
    private CameraDevice mCameraDevice;
    /**
     * The {@link android.util.Size} of camera preview
     */
    private Size mPreviewSize;

    private CameraManager mManager;

    public CameraNew(final Context context) {
        mManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

    @Override
    public CameraSupport open() {
        return open(getBackCameraId());
    }

    @Override
    public CameraSupport open(int cameraId) {
        String[] cameraIds = mManager.getCameraIdList();
        return open(cameraIds[cameraId]);
    }

    private CameraSupport open(String cameraId) {

        mCameraId = cameraId;

        try {
            mManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    CameraNew.this.mCameraDevice = camera;
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    camera.close();
                    CameraNew.this.mCameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice camera, @IntDef(value = {CameraDevice.StateCallback.ERROR_CAMERA_IN_USE, CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE, CameraDevice.StateCallback.ERROR_CAMERA_DISABLED, CameraDevice.StateCallback.ERROR_CAMERA_DEVICE, CameraDevice.StateCallback.ERROR_CAMERA_SERVICE}) int error) {
                    camera.close();
                    CameraNew.this.mCameraDevice = null;
                }
            }, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return this;
    }

    private String getBackCameraId() {
        for (final String cameraId : mManager.getCameraIdList()) {
            CameraCharacteristics characteristics = mManager.getCameraCharacteristics(cameraId);
            int cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (cOrientation == CameraCharacteristics.LENS_FACING_BACK) {
                return cameraId;
            }
        }
        return null;

    }

    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // Flash is automatically enabled when necessary.
                                setAutoFlash(mPreviewRequestBuilder);

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        mCaptureCallback, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            showToast("Failed");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

}
