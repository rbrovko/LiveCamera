LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := com_example_brovkoroman_livecamera_LiveCameraActivity
LOCAL_SRC_FILES := native-lib.cpp

include $(BUILD_SHARED_LIBRARY)