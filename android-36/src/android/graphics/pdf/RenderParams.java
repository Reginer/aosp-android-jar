/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.graphics.pdf;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.graphics.pdf.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represents a set of parameters that will be used to render a page of the PDF document.
 */
@FlaggedApi(Flags.FLAG_ENABLE_PDF_VIEWER)
public final class RenderParams {
    /**
     * Mode to render the content for display on a screen.
     */
    public static final int RENDER_MODE_FOR_DISPLAY = 1;

    /**
     * Mode to render the content for printing.
     */
    public static final int RENDER_MODE_FOR_PRINT = 2;

    // LINT.IfChange
    /**
     * Flag to enable rendering of text annotation on the page.
     *
     * @see RenderParams#getRenderFlags()
     * @see RenderParams.Builder#setRenderFlags(int)
     */
    public static final int FLAG_RENDER_TEXT_ANNOTATIONS = 1 << 1;

    /**
     * Flag to enable rendering of highlight annotation on the page.
     *
     * @see RenderParams#getRenderFlags()
     * @see RenderParams.Builder#setRenderFlags(int)
     */
    public static final int FLAG_RENDER_HIGHLIGHT_ANNOTATIONS = 1 << 2;

    /**
     * Flag to enable rendering of stamp annotation on the page.
     *
     * @see RenderParams#getRenderFlags()
     * @see RenderParams.Builder#setRenderFlags(int)
     */
    @FlaggedApi(Flags.FLAG_ENABLE_EDIT_PDF_STAMP_ANNOTATIONS)
    public static final int FLAG_RENDER_STAMP_ANNOTATIONS = 1 << 3;

    /**
     * Flag to enable rendering of freetext annotation on the page.
     *
     * @see RenderParams#getRenderFlags()
     * @see RenderParams.Builder#setRenderFlags(int)
     */
    @FlaggedApi(Flags.FLAG_ENABLE_EDIT_PDF_TEXT_ANNOTATIONS)
    public static final int FLAG_RENDER_FREETEXT_ANNOTATIONS = 1 << 4;
    // LINT.ThenChange(packages/providers/MediaProvider/pdf/framework/libs/pdfClient/page.h)
    private final int mRenderMode;
    private final int mRenderFlags;

    private RenderParams(int renderMode, int renderFlags) {
        this.mRenderMode = renderMode;
        this.mRenderFlags = renderFlags;
    }

    private static int getRenderMask() {

        int renderMask = FLAG_RENDER_TEXT_ANNOTATIONS | FLAG_RENDER_HIGHLIGHT_ANNOTATIONS;
        if (android.graphics.pdf.flags.readonly.Flags.enableEditPdfTextAnnotations()) {
            renderMask |= FLAG_RENDER_FREETEXT_ANNOTATIONS;
        }
        if (android.graphics.pdf.flags.readonly.Flags.enableEditPdfStampAnnotations()) {
            renderMask |= FLAG_RENDER_STAMP_ANNOTATIONS;
        }
        return renderMask;
    }

    /**
     * Returns the render mode.
     */
    @RenderMode
    public int getRenderMode() {
        return mRenderMode;
    }

    /**
     * Returns the bitmask of the render flags.
     */
    @RenderFlags
    public int getRenderFlags() {
        return mRenderFlags;
    }

    /** @hide */
    public int getRenderAnnotations() {
        return mRenderFlags & getRenderMask();
    }

    /** @hide */
    @IntDef({
            RENDER_MODE_FOR_DISPLAY,
            RENDER_MODE_FOR_PRINT
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface RenderMode {
    }

    /** @hide */
    @IntDef(flag = true, prefix = {"FLAG_"}, value = {
            FLAG_RENDER_TEXT_ANNOTATIONS,
            FLAG_RENDER_HIGHLIGHT_ANNOTATIONS,
            FLAG_RENDER_STAMP_ANNOTATIONS,
            FLAG_RENDER_FREETEXT_ANNOTATIONS
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface RenderFlags {
    }

    /**
     * Builder for constructing {@link RenderParams}.
     */
    public static final class Builder {

        private final int mRenderMode;

        @RenderFlags
        private int mRenderFlags;

        /**
         * Create a builder for constructing a {@link RenderParams} object with the render mode.
         *
         * @param renderMode render mode for the content.
         */
        public Builder(@RenderMode int renderMode) {
            this.mRenderMode = renderMode;
        }

        /**
         * Sets the state of the render flag.
         * See {@link #setRenderFlags(int, int)} for usage information.
         *
         * @param renderFlags the bitmask of the render flag should be enabled, or {@code 0} to
         *                    disable all flags.
         * @see #setRenderFlags(int, int)
         * @see #getRenderFlags()
         */
        @NonNull
        public Builder setRenderFlags(@RenderFlags int renderFlags) {
            setRenderFlags(renderFlags, getRenderMask());
            return this;
        }

        /**
         * Sets the state of the render flag specified by the mask. To change all render flags at
         * once, see {@link #setRenderFlags(int)}.
         * <p>
         * When a render flag is enabled, it will be displayed on the updated
         * {@link android.graphics.Bitmap} of the renderer.
         * <p>
         * Multiple indicator types may be enabled or disabled by passing the logical OR of the
         * desired flags. If multiple flags are specified, they
         * will all be set to the same enabled state.
         * <p>
         * For example, to enable the render text annotations flag:
         * {@code setRenderFlags(FLAG_RENDER_TEXT_ANNOTATIONS, FLAG_RENDER_TEXT_ANNOTATIONS)}
         * <p>
         * To disable the render text annotations flag:
         * {@code setRenderFlags(0, FLAG_RENDER_TEXT_ANNOTATIONS)}
         *
         * @param renderFlags the render flag, or the logical OR of multiple
         *                    render flags. One or more of:
         *                    <ul>
         *                      <li>{@link #FLAG_RENDER_TEXT_ANNOTATIONS}</li>
         *                      <li>{@link #FLAG_RENDER_HIGHLIGHT_ANNOTATIONS}</li>
         *                    </ul>
         * @see #setRenderFlags(int)
         * @see #getRenderFlags()
         */
        @NonNull
        public Builder setRenderFlags(@RenderFlags int renderFlags, @RenderFlags int mask) {
            // Sanitize the mask
            mask &= getRenderMask();

            // Mask the flags
            renderFlags &= mask;

            // Merge with non-masked flags
            this.mRenderFlags = renderFlags | (this.mRenderFlags & ~mask);
            return this;
        }

        /**
         * Builds the {@link RenderParams} after the optional values has been set.
         *
         * @return the newly constructed {@link RenderParams} object
         */
        @NonNull
        public RenderParams build() {
            return new RenderParams(this.mRenderMode, this.mRenderFlags);
        }
    }
}
