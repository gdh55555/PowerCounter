package com.lab.powercounter.util;

import android.util.Log;

/**
 * Created by Admin on 2016/7/12.
 */
public class NativeLoader {
    private static final String TAG = "NativeLoader";

    private static boolean loadOk = false;

    static {
        try {
            System.loadLibrary("bindings");
            loadOk = true;
        } catch (SecurityException e) {
            Log.w(TAG, "Failed to load jni dll, will fall back on pure java");
            loadOk = false;
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "Failed to load jni dll, will fall back on pure java");
            loadOk = false;
        }
    }

    public static boolean jniLoaded() {
        return loadOk;
    }
}
