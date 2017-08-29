package com.example.brovkoroman.livecamera;

import android.content.Context;
import android.os.Build;

/**
 * Created by brovkoroman on 29.08.17.
 */

public class CameraFactory {

    public static CameraSupport cameraSupport(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return new CameraNew(context);
        } else {
            return new CameraOld();
        }

    }
}
