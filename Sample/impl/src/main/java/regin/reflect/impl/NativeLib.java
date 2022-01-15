package regin.reflect.impl;

public class NativeLib {

    // Used to load the 'impl' library on application startup.
    static {
        System.loadLibrary("impl");
    }

    /**
     * A native method that is implemented by the 'impl' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}