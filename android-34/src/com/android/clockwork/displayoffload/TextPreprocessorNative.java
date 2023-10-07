package com.android.clockwork.displayoffload;

import com.android.clockwork.displayoffload.TextPreprocessor.ITextPreprocessorNative;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Helper class for isolating JNI logic from rest of
 * {@link com.android.clockwork.displayoffload.TextPreprocessor} for better testability.
 */
public class TextPreprocessorNative implements ITextPreprocessorNative {
    // jni
    static {
        System.loadLibrary("display-offload-jni");
    }

    @Override
    public void shapeText(String text, float fontSize,
            int fontid, int fontIndex,
            ByteBuffer fontData, ArrayList<Integer> glyphs, ArrayList<Float> positions,
            boolean useTabularNum) {
        shapeTextNative(text, fontSize, fontid, fontIndex, fontData, glyphs, positions,
                useTabularNum);
    }

    @Override
    public byte[] subsetTtf(ByteBuffer fontData, int fontIndex,
            int[] glyphIds, int[] codepoints){
        return subsetTtfNative(fontData, fontIndex,glyphIds, codepoints);
    }

    private static native void shapeTextNative(String text, float fontSize,
            int fontid, int fontIndex,
            ByteBuffer fontData, ArrayList<Integer> glyphs, ArrayList<Float> positions,
            boolean useTabularNum);

    private static native byte[] subsetTtfNative(ByteBuffer fontData, int fontIndex,
            int[] glyphIds, int[] codepoints);
}
