#include <android/bitmap.h>
#include <stdlib.h>

#define toInt(pValue) \
    (0xff & (int32_t) pValue)
#define max(pValue1, pValue2) \
    (pValue1 < pValue2) ? pValue2 : pValue1
#define clamp(pValue, pLowest, pHighest) \
    ((pValue < 0) ? pLowest : (pValue > pHighest) ? pHighest : pValue)
#define color(pColorR, pColorG, pColorB) \
    (0xFF000000 | ((pColorB << 6)  & 0x00FF0000) \
                | ((pColorG >> 2)  & 0x0000FF00) \
                | ((pColorR >> 10) & 0x000000FF))

void JNICALL decode(JNIEnv *pEnv, jclass pClass, jobject pTarget, jbyteArray pSource, jint pFilter) {
    // Retrieves bitmap information and locks it for drawing
    AndroidBitmapInfo bitmapInfo;
    uint32_t *bitmapContent;
    if (AndroidBitmap_getInfo(pEnv, pTarget, &bitmapInfo) < 0) {
        abort();
    }
    if (bitmapInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        abort();
    }
    if (AndroidBitmap_lockPixels(pEnv, pTarget, (void **)&bitmapContent) < 0) {
        abort();
    }

    // Accesses source array data
    jbyte *source = (*pEnv)->GetPrimitiveArrayCritical(pEnv, pSource, 0);
    if (source == NULL) {
        abort();
    }

    int32_t frameSize = bitmapInfo.width * bitmapInfo.height;
    int32_t yIndex, uvIndex, x, y;
    int32_t colorY, colorU, colorV;
    int32_t colorR, colorG, colorB;
    int32_t y1192;

    /*
     * Processes each pixel and converts YUV to RGB color.
     * Algorithm originates from the Ketai open source project.
     * See http://ketai.googlecode.com/
     */
    for (y = 0, yIndex = 0; y < bitmapInfo.height; ++y) {
        colorU = 0;
        colorV = 0;

        /*
         * Y is divided by 2 because UVs are subsampled vertically.
         * This means that two consecutives iterations refer to the
         * same UV line (e.g when Y=0 and Y=1).
         */
        uvIndex = frameSize + (y >> 1) * bitmapInfo.width;

        for (x = 0; x < bitmapInfo.width; ++x, ++yIndex) {
            /*
             * Retrieves YUV components. UVs are subsampled
             * horizontally too, hence %2 (1 UV for 2 Y)
             */
            colorY = max(toInt(source[yIndex]) - 16, 0);
            if (!(x % 2)) {
                colorV = toInt(source[uvIndex++] - 128);
                colorU = toInt(source[uvIndex++] - 128);
            }

            // Computes R, G and B from Y, U and V
            y1192 = 1192 * colorY;
            colorR = (y1192 + 1634 * colorY);
            colorG = (y1192 - 833 * colorV - 400 * colorU);
            colorB = (y1192 + 2066 * colorU);

            colorR = clamp(colorR, 0, 262143);
            colorG = clamp(colorG, 0, 262143);
            colorB = clamp(colorB, 0, 262143);

            // Combines R, G, B and A into the final pixel color
            bitmapContent[yIndex] = color(colorR, colorG, colorB);
            bitmapContent[yIndex] &= pFilter;
        }
    }

    // Unlocks the bitmap and releases the Java array when finished
    (*pEnv)->ReleasePrimitiveArrayCritical(pEnv, pSource, source, 0);
    if (AndroidBitmap_unlockPixels(pEnv, pTarget) < 0) {
        abort();
    }
}

static JNINativeMethod gMethodRegistry[] = {
        {"decode", "(Landroid/graphics/Bitmap;[BI)V", (void *)decode}
};

static int gMethodRegistrySize = sizeof(gMethodRegistry) / sizeof(gMethodRegistry[0]);

JNIEXPORT jint JNI_OnLoad(JavaVM *pVM, void *reserved) {
    JNIEnv *env;
    if ((*pVM)->GetEnv(pVM, (void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        abort();
    }

    jclass LiveCameraActivity = (*env)->FindClass(env, "com/example/brovkoroman/livecamera/LiveCameraActivity");
    if (LiveCameraActivity == NULL) {
        abort();
    }
    (*env)->RegisterNatives(env, LiveCameraActivity, gMethodRegistry, 1);
    (*env)->DeleteLocalRef(env, LiveCameraActivity);

    return JNI_VERSION_1_6;
}