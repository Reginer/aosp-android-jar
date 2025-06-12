/*
 * Copyright (C) 2012 The Android Open Source Project
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

package android.renderscript;

/**
 * Intrinsic Gausian blur filter. Applies a gaussian blur of the
 * specified radius to all elements of an allocation.
 *
 * @deprecated Renderscript has been deprecated in API level 31. Please refer to the <a
 * href="https://developer.android.com/guide/topics/renderscript/migration-guide">migration
 * guide</a> for the proposed alternatives.
 **/
@Deprecated
public final class ScriptIntrinsicBlur extends ScriptIntrinsic {
    private final float[] mValues = new float[9];
    private Allocation mInput;

    private ScriptIntrinsicBlur(long id, RenderScript rs) {
        super(id, rs);
    }

    /**
     * Create an intrinsic for applying a blur to an allocation. The
     * default radius is 5.0.
     *
     * Supported elements types are {@link Element#U8},
     * {@link Element#U8_4}.
     *
     * @param rs The RenderScript context
     * @param e Element type for inputs and outputs
     *
     * @return ScriptIntrinsicBlur
     */
    public static ScriptIntrinsicBlur create(RenderScript rs, Element e) {
        if ((!e.isCompatible(Element.U8_4(rs))) && (!e.isCompatible(Element.U8(rs)))) {
            throw new RSIllegalArgumentException("Unsupported element type.");
        }
        long id = rs.nScriptIntrinsicCreate(5, e.getID(rs));
        ScriptIntrinsicBlur sib = new ScriptIntrinsicBlur(id, rs);
        sib.setRadius(5.f);
        return sib;
    }

    /**
     * Set the input of the blur.
     * Must match the element type supplied during create.
     *
     * @param ain The input allocation
     */
    public void setInput(Allocation ain) {
        if (ain.getType().getY() == 0) {
            throw new RSIllegalArgumentException("Input set to a 1D Allocation");
        }
        Element e = ain.getElement();
        if ((!e.isCompatible(Element.U8_4(mRS))) && (!e.isCompatible(Element.U8(mRS)))) {
            throw new RSIllegalArgumentException("Unsupported element type.");
        }
        mInput = ain;
        setVar(1, ain);
    }

    /**
     * Set the radius of the Blur.
     *
     * Supported range 0 < radius <= 25
     *
     * @param radius The radius of the blur
     */
    public void setRadius(float radius) {
        if (radius <= 0 || radius > 25) {
            throw new RSIllegalArgumentException("Radius out of range (0 < r <= 25).");
        }
        setVar(0, radius);
    }

    /**
     * Apply the filter to the input and save to the specified
     * allocation.
     *
     * @param aout Output allocation. Must match creation element
     *             type.
     */
    public void forEach(Allocation aout) {
        if (aout.getType().getY() == 0) {
            throw new RSIllegalArgumentException("Output is a 1D Allocation");
        }
        forEach(0, (Allocation) null, aout, null);
    }

    /**
     * Apply the filter to the input and save to the specified
     * allocation.
     *
     * @param aout Output allocation. Must match creation element
     *             type.
     * @param opt LaunchOptions for clipping
     */
    public void forEach(Allocation aout, Script.LaunchOptions opt) {
        if (aout.getType().getY() == 0) {
            throw new RSIllegalArgumentException("Output is a 1D Allocation");
        }
        forEach(0, (Allocation) null, aout, null, opt);
    }


    /**
     * Get a KernelID for this intrinsic kernel.
     *
     * @return Script.KernelID The KernelID object.
     */
    public Script.KernelID getKernelID() {
        return createKernelID(0, 2, null, null);
    }

    /**
     * Get a FieldID for the input field of this intrinsic.
     *
     * @return Script.FieldID The FieldID object.
     */
    public Script.FieldID getFieldID_Input() {
        return createFieldID(1, null);
    }
}
