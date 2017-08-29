package com.example.brovkoroman.livecamera;

import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera;
import android.hardware.camera2.*;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Size;
import android.view.TextureView;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;

public class LiveCameraActivity extends AppCompatActivity
        implements TextureView.SurfaceTextureListener, Camera.PreviewCallback {

    // Used to load the 'livecamera' library on application startup.
    static {
        System.loadLibrary("livecamera");
    }

    private CameraSupport mCamera;

    private TextureView mTextureView;
    private byte[] mVideoSource;
    private ImageView mImageViewR, mImageViewG, mImageViewB;
    private Bitmap mImageR, mImageG, mImageB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_camera);

        mTextureView = (TextureView) findViewById(R.id.preview);
        mImageViewR = (ImageView) findViewById(R.id.imageViewR);
        mImageViewG = (ImageView) findViewById(R.id.imageViewG);
        mImageViewB = (ImageView) findViewById(R.id.imageViewB);

        mTextureView.setSurfaceTextureListener(this);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Ignored
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Ignored
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture pSurface, int pWidth, int pHeight) {
        mCamera = CameraFactory.cameraSupport(this);
        mCamera.open();


        openCamera();
    }

    private void openCamera() {
        mCamera = CameraFactory.cameraSupport(this);
        mCamera.open();
        try {
            mCamera.setPreviewTexture(pSurface);
            mCamera.setPreviewCallbackWithBuffer(this);

            /*
             * Sets landscape mode to avoid complications related to
             * screen orientation handling
             */
            mCamera.setDisplayOrientation(0);

            // Finds a suitable resolution
            Size size = findBestResolution(pWidth, pHeight);
            PixelFormat pixelFormat = new PixelFormat();
            PixelFormat.getPixelFormatInfo(mCamera.getParameters().getPreviewFormat(), pixelFormat);
            int sourceSize = size.width * size.height * pixelFormat.bitsPerPixel / 8;

            // Set-up camera size and video format. YCbCr_420_SP
            // should be the default on Android anyway
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(size.width, size.height);
            parameters.setPreviewFormat(PixelFormat.YCbCr_420_SP);
            mCamera.setParameters(parameters);

            // Prepares video buffer and bitmap buffers
            mVideoSource = new byte[sourceSize];
            mImageR = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888);
            mImageG = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888);
            mImageB = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888);
            mImageViewR.setImageBitmap(mImageR);
            mImageViewG.setImageBitmap(mImageG);
            mImageViewB.setImageBitmap(mImageB);

            // Starts receiving pictures from the camera
            mCamera.addCallbackBuffer(mVideoSource);
            mCamera.startPreview();
        } catch (IOException ioe) {
            mCamera.release();
            mCamera = null;
            throw new IllegalStateException();
        }

    }

    private Size findBestResolution(int pWidth, int pHeight) {
        List<Size> sizes = mCamera.getParameters().getSupportedPreviewSizes();
        /*
         * Finds the biggest resolution which fits the screen
         * Else, returns the first resolution found
         */
        Size selectedSize = mCamera.new Size(0, 0);
        for (Size size : sizes) {
            if ((size.width <= pWidth) &&
                    (size.height <= pHeight) &&
                    (size.width >= selectedSize.width) &&
                    (size.height >= selectedSize.height)) {
                selectedSize = size;
            }
        }
        /*
         * Previous code assume that there is a preview size smaller
         * than screen size. If not, hopefully the Android API
         * guarantees that at least one preview size is available
         */
        if ((selectedSize.width == 0) ||
                (selectedSize.height == 0)) {
            selectedSize = sizes.get(0);
        }

        return selectedSize;
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        // Releases camera which is a shared resource
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            /*
             *  These variables can take a lot of memory. Get rid of
             *  them as fast as we can
             */
            mCamera = null;
            mVideoSource = null;
            mImageR.recycle(); mImageR = null;
            mImageG.recycle(); mImageG = null;
            mImageB.recycle(); mImageB = null;
        }

        return true;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        /*
         * New data has been received from camera. Processes it and
         * requests surface to be redrawn right after
         */
        if (mCamera != null) {
            decode(mImageR, data, 0xFFFF0000);
            decode(mImageG, data, 0xFF00FF00);
            decode(mImageB, data, 0xFF0000FF);
            mImageViewR.invalidate();
            mImageViewG.invalidate();
            mImageViewB.invalidate();

            mCamera.addCallbackBuffer(mVideoSource);
        }
    }

    public native void decode(Bitmap pTarget, byte[] pSource, int pFilter);

    /**
     * Opens the camera specified by {@link Camera2BasicFragment#mCameraId}.
     */
    private void openCamera(int width, int height) {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return;
        }
        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

}
