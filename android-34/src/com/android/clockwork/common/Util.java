package com.android.clockwork.common;

import java.io.Closeable;
import java.io.IOException;

/**
 * A class that works around deficiencies in some Java class library methods. For the most part,
 * the class library is good, so the methods in this class should be kept to a minimum.
 */
public class Util {
    // Do not instantiate.
    private Util() {}

    /** A close method for the Closeable interface that doesn't throw a checked exception. */
    public static boolean close(final Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }
}
