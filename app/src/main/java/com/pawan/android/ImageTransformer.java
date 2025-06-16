package com.pawan.android;

public class ImageTransformer {

    // Used to load the 'real-time-edge-detection' library on application startup.
    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("real-time-edge-detection");
    }
    /**
     * A native method that is implemented by the 'real_time_edge_detection' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    public native byte[] transformImage(byte[] imageData, int width, int height);
}
