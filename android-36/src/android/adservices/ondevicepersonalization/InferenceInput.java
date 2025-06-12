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

package android.adservices.ondevicepersonalization;

import static com.android.ondevicepersonalization.internal.util.ByteArrayUtil.deserializeObject;
import static com.android.ondevicepersonalization.internal.util.ByteArrayUtil.serializeObject;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.SuppressLint;

import com.android.adservices.ondevicepersonalization.flags.Flags;
import com.android.internal.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Contains all the information needed for a run of model inference. The input of {@link
 * ModelManager#run}.
 */
public final class InferenceInput {
    /** The configuration that controls runtime interpreter behavior. */
    @NonNull private Params mParams;

    /**
     * A byte array that holds input data. The inputs should be in the same order as inputs of the
     * model.
     *
     * <p>For LiteRT, this field is mapped to inputs of runForMultipleInputsOutputs:
     * https://www.tensorflow.org/lite/api_docs/java/org/tensorflow/lite/InterpreterApi#parameters_9
     *
     * <pre>{@code
     * String[] input0 = {"foo", "bar"}; // string tensor shape is [2].
     * int[] input1 = new int[]{3, 2, 1}; // int tensor shape is [3].
     * Object[] inputData = {input0, input1, ...};
     * byte[] data = serializeObject(inputData);
     * }</pre>
     *
     * <p>For Executorch model, this field is a serialized EValue array.
     *
     * @hide
     */
    @NonNull private byte[] mData;

    /**
     * The number of input examples. Adopter can set this field to run batching inference. The batch
     * size is 1 by default. The batch size should match the input data size.
     */
    private int mBatchSize = 1;

    /**
     * The empty InferenceOutput representing the expected output structure. For LiteRT, the
     * inference code will verify whether this expected output structure matches model output
     * signature.
     *
     * <p>If a model produce string tensors:
     *
     * <pre>{@code
     * String[] output = new String[3][2];  // Output tensor shape is [3, 2].
     * HashMap<Integer, Object> outputs = new HashMap<>();
     * outputs.put(0, output);
     * expectedOutputStructure = new InferenceOutput.Builder().setDataOutputs(outputs).build();
     * }</pre>
     */
    @NonNull private InferenceOutput mExpectedOutputStructure;

    public static class Params {
        /** A {@link KeyValueStore} where pre-trained model is stored. */
        @NonNull private KeyValueStore mKeyValueStore;

        /** The key of the table where the corresponding value stores a pre-trained model. */
        @NonNull private String mModelKey;

        /** The model inference will run on CPU. */
        public static final int DELEGATE_CPU = 1;

        /**
         * The delegate to run model inference.
         *
         * @hide
         */
        @IntDef(
                prefix = "DELEGATE_",
                value = {DELEGATE_CPU})
        @Retention(RetentionPolicy.SOURCE)
        public @interface Delegate {}

        /**
         * The delegate to run model inference. If not set, the default value is {@link
         * #DELEGATE_CPU}.
         */
        private @Delegate int mDelegateType = DELEGATE_CPU;

        /** The model is a tensorflow lite model. */
        public static final int MODEL_TYPE_TENSORFLOW_LITE = 1;

        /** The model is an executorch model. */
        @FlaggedApi(Flags.FLAG_EXECUTORCH_INFERENCE_API_ENABLED)
        public static final int MODEL_TYPE_EXECUTORCH = 2;

        /**
         * The type of the model.
         *
         * @hide
         */
        @IntDef(
                prefix = "MODEL_TYPE",
                value = {MODEL_TYPE_TENSORFLOW_LITE, MODEL_TYPE_EXECUTORCH})
        @Retention(RetentionPolicy.SOURCE)
        public @interface ModelType {}

        /**
         * The type of the pre-trained model. If not set, the default value is {@link
         * #MODEL_TYPE_TENSORFLOW_LITE} .
         */
        private @ModelType int mModelType = MODEL_TYPE_TENSORFLOW_LITE;

        /**
         * The number of threads used for intraop parallelism on CPU, must be positive number.
         * Adopters can set this field based on model architecture. The actual thread number depends
         * on system resources and other constraints.
         */
        private @IntRange(from = 1) int mRecommendedNumThreads = 1;

        /**
         * Creates a new Params.
         *
         * @param keyValueStore A {@link KeyValueStore} where pre-trained model is stored.
         * @param modelKey The key of the table where the corresponding value stores a pre-trained
         *     model.
         * @param delegateType The delegate to run model inference. If not set, the default value is
         *     {@link #DELEGATE_CPU}.
         * @param modelType The type of the pre-trained model. If not set, the default value is
         *     {@link #MODEL_TYPE_TENSORFLOW_LITE} .
         * @param recommendedNumThreads The number of threads used for intraop parallelism on CPU,
         *     must be positive number. Adopters can set this field based on model architecture. The
         *     actual thread number depends on system resources and other constraints.
         * @hide
         */
        public Params(
                @NonNull KeyValueStore keyValueStore,
                @NonNull String modelKey,
                @Delegate int delegateType,
                @ModelType int modelType,
                @IntRange(from = 1) int recommendedNumThreads) {
            this.mKeyValueStore = Objects.requireNonNull(keyValueStore);
            this.mModelKey = Objects.requireNonNull(modelKey);
            this.mDelegateType = delegateType;
            this.mModelType = modelType;

            if (!(mModelType == MODEL_TYPE_TENSORFLOW_LITE)
                    && !(mModelType == MODEL_TYPE_EXECUTORCH)) {
                throw new java.lang.IllegalArgumentException(
                        "modelType was "
                                + mModelType
                                + " but must be one of: "
                                + "MODEL_TYPE_TENSORFLOW_LITE("
                                + MODEL_TYPE_TENSORFLOW_LITE
                                + "), "
                                + "MODEL_TYPE_EXECUTORCH("
                                + MODEL_TYPE_EXECUTORCH
                                + ")");
            }

            this.mRecommendedNumThreads = recommendedNumThreads;
            Preconditions.checkState(
                    recommendedNumThreads >= 1,
                    "recommend thread number should be large or equal to 1");
        }

        /** A {@link KeyValueStore} where pre-trained model is stored. */
        public @NonNull KeyValueStore getKeyValueStore() {
            return mKeyValueStore;
        }

        /** The key of the table where the corresponding value stores a pre-trained model. */
        public @NonNull String getModelKey() {
            return mModelKey;
        }

        /**
         * The delegate to run model inference. If not set, the default value is {@link
         * #DELEGATE_CPU}.
         */
        public @Delegate int getDelegateType() {
            return mDelegateType;
        }

        /**
         * The type of the pre-trained model. If not set, the default value is {@link
         * #MODEL_TYPE_TENSORFLOW_LITE} .
         */
        public @ModelType int getModelType() {
            return mModelType;
        }

        /**
         * The number of threads used for intraop parallelism on CPU, must be positive number.
         * Adopters can set this field based on model architecture. The actual thread number depends
         * on system resources and other constraints.
         */
        public @IntRange(from = 1) int getRecommendedNumThreads() {
            return mRecommendedNumThreads;
        }

        @Override
        public boolean equals(@android.annotation.Nullable Object o) {
            // You can override field equality logic by defining either of the methods like:
            // boolean fieldNameEquals(Params other) { ... }
            // boolean fieldNameEquals(FieldType otherValue) { ... }

            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            @SuppressWarnings("unchecked")
            Params that = (Params) o;
            //noinspection PointlessBooleanExpression
            return true
                    && java.util.Objects.equals(mKeyValueStore, that.mKeyValueStore)
                    && java.util.Objects.equals(mModelKey, that.mModelKey)
                    && mDelegateType == that.mDelegateType
                    && mModelType == that.mModelType
                    && mRecommendedNumThreads == that.mRecommendedNumThreads;
        }

        @Override
        public int hashCode() {
            // You can override field hashCode logic by defining methods like:
            // int fieldNameHashCode() { ... }

            int _hash = 1;
            _hash = 31 * _hash + java.util.Objects.hashCode(mKeyValueStore);
            _hash = 31 * _hash + java.util.Objects.hashCode(mModelKey);
            _hash = 31 * _hash + mDelegateType;
            _hash = 31 * _hash + mModelType;
            _hash = 31 * _hash + mRecommendedNumThreads;
            return _hash;
        }

        /** A builder for {@link Params} */
        @SuppressWarnings("WeakerAccess")
        public static final class Builder {

            private @NonNull KeyValueStore mKeyValueStore;
            private @NonNull String mModelKey;
            private @Delegate int mDelegateType;
            private @ModelType int mModelType;
            private @IntRange(from = 1) int mRecommendedNumThreads;

            private long mBuilderFieldsSet = 0L;

            /**
             * Creates a new Builder.
             *
             * @param keyValueStore a {@link KeyValueStore} where pre-trained model is stored.
             * @param modelKey key of the table where the corresponding value stores a pre-trained
             *     model.
             */
            public Builder(@NonNull KeyValueStore keyValueStore, @NonNull String modelKey) {
                mKeyValueStore = Objects.requireNonNull(keyValueStore);
                mModelKey = Objects.requireNonNull(modelKey);
            }

            /** A {@link KeyValueStore} where pre-trained model is stored. */
            public @NonNull Builder setKeyValueStore(@NonNull KeyValueStore value) {
                mBuilderFieldsSet |= 0x1;
                mKeyValueStore = value;
                return this;
            }

            /** The key of the table where the corresponding value stores a pre-trained model. */
            public @NonNull Builder setModelKey(@NonNull String value) {
                mBuilderFieldsSet |= 0x2;
                mModelKey = value;
                return this;
            }

            /**
             * The delegate to run model inference. If not set, the default value is {@link
             * #DELEGATE_CPU}.
             */
            public @NonNull Builder setDelegateType(@Delegate int value) {
                mBuilderFieldsSet |= 0x4;
                mDelegateType = value;
                return this;
            }

            /**
             * The type of the pre-trained model. If not set, the default value is {@link
             * #MODEL_TYPE_TENSORFLOW_LITE} .
             */
            public @NonNull Builder setModelType(@ModelType int value) {
                mBuilderFieldsSet |= 0x8;
                mModelType = value;
                return this;
            }

            /**
             * The number of threads used for intraop parallelism on CPU, must be positive number.
             * Adopters can set this field based on model architecture. The actual thread number
             * depends on system resources and other constraints.
             */
            public @NonNull Builder setRecommendedNumThreads(@IntRange(from = 1) int value) {
                mBuilderFieldsSet |= 0x10;
                mRecommendedNumThreads = value;
                return this;
            }

            /** Builds the instance. This builder should not be touched after calling this! */
            public @NonNull Params build() {
                mBuilderFieldsSet |= 0x20; // Mark builder used

                if ((mBuilderFieldsSet & 0x4) == 0) {
                    mDelegateType = DELEGATE_CPU;
                }
                if ((mBuilderFieldsSet & 0x8) == 0) {
                    mModelType = MODEL_TYPE_TENSORFLOW_LITE;
                }
                if ((mBuilderFieldsSet & 0x10) == 0) {
                    mRecommendedNumThreads = 1;
                }
                Params o =
                        new Params(
                                mKeyValueStore,
                                mModelKey,
                                mDelegateType,
                                mModelType,
                                mRecommendedNumThreads);
                return o;
            }
        }
    }

    /* package-private */ InferenceInput(
            @NonNull Params params,
            @NonNull byte[] data,
            int batchSize,
            @NonNull InferenceOutput expectedOutputStructure) {
        this.mParams = Objects.requireNonNull(params);
        this.mData = Objects.requireNonNull(data);
        this.mBatchSize = batchSize;
        this.mExpectedOutputStructure = Objects.requireNonNull(expectedOutputStructure);
    }

    /** The configuration that controls runtime interpreter behavior. */
    public @NonNull Params getParams() {
        return mParams;
    }

    /**
     * A byte array that holds input data. The inputs should be in the same order as inputs of the
     * model.
     *
     * <p>For LiteRT, this field is mapped to inputs of runForMultipleInputsOutputs:
     * https://www.tensorflow.org/lite/api_docs/java/org/tensorflow/lite/InterpreterApi#parameters_9
     *
     * <pre>{@code
     * String[] input0 = {"foo", "bar"}; // string tensor shape is [2].
     * int[] input1 = new int[]{3, 2, 1}; // int tensor shape is [3].
     * Object[] inputData = {input0, input1, ...};
     * byte[] data = serializeObject(inputData);
     * }</pre>
     *
     * <p>For Executorch model, this field is a serialized EValue array.
     */
    @FlaggedApi(Flags.FLAG_EXECUTORCH_INFERENCE_API_ENABLED)
    public @NonNull byte[] getData() {
        return mData;
    }

    /**
     * Note: use {@link InferenceInput#getData()} instead.
     *
     * <p>An array of input data. The inputs should be in the same order as inputs of the model.
     *
     * <p>For example, if a model takes multiple inputs:
     *
     * <pre>{@code
     * String[] input0 = {"foo", "bar"}; // string tensor shape is [2].
     * int[] input1 = new int[]{3, 2, 1}; // int tensor shape is [3].
     * Object[] inputData = {input0, input1, ...};
     * }</pre>
     *
     * For LiteRT, this field is mapped to inputs of runForMultipleInputsOutputs:
     * https://www.tensorflow.org/lite/api_docs/java/org/tensorflow/lite/InterpreterApi#parameters_9
     */
    @SuppressLint("ArrayReturn")
    public @NonNull Object[] getInputData() {
        return (Object[]) deserializeObject(mData);
    }

    /**
     * The number of input examples. Adopter can set this field to run batching inference. The batch
     * size is 1 by default. The batch size should match the input data size.
     */
    public int getBatchSize() {
        return mBatchSize;
    }

    /**
     * The empty InferenceOutput representing the expected output structure. For LiteRT, the
     * inference code will verify whether this expected output structure matches model output
     * signature.
     *
     * <p>If a model produce string tensors:
     *
     * <pre>{@code
     * String[] output = new String[3][2];  // Output tensor shape is [3, 2].
     * HashMap<Integer, Object> outputs = new HashMap<>();
     * outputs.put(0, output);
     * expectedOutputStructure = new InferenceOutput.Builder().setDataOutputs(outputs).build();
     * }</pre>
     */
    public @NonNull InferenceOutput getExpectedOutputStructure() {
        return mExpectedOutputStructure;
    }

    @Override
    public boolean equals(@android.annotation.Nullable Object o) {
        // You can override field equality logic by defining either of the methods like:
        // boolean fieldNameEquals(InferenceInput other) { ... }
        // boolean fieldNameEquals(FieldType otherValue) { ... }

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        @SuppressWarnings("unchecked")
        InferenceInput that = (InferenceInput) o;
        //noinspection PointlessBooleanExpression
        return true
                && java.util.Objects.equals(mParams, that.mParams)
                && java.util.Arrays.equals(mData, that.mData)
                && mBatchSize == that.mBatchSize
                && java.util.Objects.equals(
                        mExpectedOutputStructure, that.mExpectedOutputStructure);
    }

    @Override
    public int hashCode() {
        // You can override field hashCode logic by defining methods like:
        // int fieldNameHashCode() { ... }

        int _hash = 1;
        _hash = 31 * _hash + java.util.Objects.hashCode(mParams);
        _hash = 31 * _hash + java.util.Arrays.hashCode(mData);
        _hash = 31 * _hash + mBatchSize;
        _hash = 31 * _hash + java.util.Objects.hashCode(mExpectedOutputStructure);
        return _hash;
    }

    /** A builder for {@link InferenceInput} */
    @SuppressWarnings("WeakerAccess")
    public static final class Builder {

        private @NonNull Params mParams;
        private @NonNull byte[] mData;
        private int mBatchSize;
        private @NonNull InferenceOutput mExpectedOutputStructure =
                new InferenceOutput.Builder().build();

        private long mBuilderFieldsSet = 0L;

        /**
         * Note: use {@link InferenceInput.Builder#Builder(Params, byte[])} instead.
         *
         * <p>Creates a new Builder for LiteRT model inference input. For LiteRT, inputData field is
         * mapped to inputs of runForMultipleInputsOutputs:
         * https://www.tensorflow.org/lite/api_docs/java/org/tensorflow/lite/InterpreterApi#parameters_9
         * The inputs should be in the same order as inputs * of the model. *
         *
         * <p>For example, if a model takes multiple inputs: *
         *
         * <pre>{@code
         *  String[] input0 = {"foo", "bar"}; // string tensor shape is [2].
         * int[] input1 = new int[]{3, 2, 1}; // int tensor shape is [3].
         * Object[] inputData = {input0, input1, ...};
         * }</pre>
         *
         * For LiteRT, the inference code will verify whether the expected output structure matches
         * model output signature.
         *
         * <p>If a model produce string tensors:
         *
         * <pre>{@code
         * String[] output = new String[3][2];  // Output tensor shape is [3, 2].
         * HashMap<Integer, Object> outputs = new HashMap<>();
         * outputs.put(0, output);
         * expectedOutputStructure = new InferenceOutput.Builder().setDataOutputs(outputs).build();
         *
         * }</pre>
         *
         * @param params configuration that controls runtime interpreter behavior.
         * @param inputData an array of input data.
         * @param expectedOutputStructure an empty InferenceOutput representing the expected output
         *     structure.
         */
        public Builder(
                @NonNull Params params,
                @SuppressLint("ArrayReturn") @NonNull Object[] inputData,
                @NonNull InferenceOutput expectedOutputStructure) {
            mParams = Objects.requireNonNull(params);
            mData = serializeObject(Objects.requireNonNull(inputData));
            mExpectedOutputStructure = Objects.requireNonNull(expectedOutputStructure);
        }

        /**
         * Creates a new Builder with provided runtime parameters and input data.
         *
         * <p>For LiteRT, inputData field is mapped to inputs of runForMultipleInputsOutputs:
         * https://www.tensorflow.org/lite/api_docs/java/org/tensorflow/lite/InterpreterApi#parameters_9
         * For example, if a model takes multiple inputs:
         *
         * <pre>{@code
         * String[] input0 = {"foo", "bar"}; // string tensor shape is [2].
         * int[] input1 = new int[]{3, 2, 1}; // int tensor shape is [3].
         * Object[] data = {input0, input1, ...};
         * byte[] inputData = serializeObject(data);
         * }</pre>
         *
         * <p>For Executorch, inputData field is mapped to a serialized EValue array.
         *
         * @param params configuration that controls runtime interpreter behavior.
         * @param inputData byte array that holds serialized input data.
         */
        @FlaggedApi(Flags.FLAG_EXECUTORCH_INFERENCE_API_ENABLED)
        public Builder(@NonNull Params params, @NonNull byte[] inputData) {
            mParams = Objects.requireNonNull(params);
            mData = Objects.requireNonNull(inputData);
        }

        /** The configuration that controls runtime interpreter behavior. */
        public @NonNull Builder setParams(@NonNull Params value) {
            mBuilderFieldsSet |= 0x1;
            mParams = value;
            return this;
        }

        /**
         * A byte array that holds input data. The inputs should be in the same order as inputs of
         * the model.
         *
         * <p>For LiteRT, this field is mapped to inputs of runForMultipleInputsOutputs:
         * https://www.tensorflow.org/lite/api_docs/java/org/tensorflow/lite/InterpreterApi#parameters_9
         *
         * <pre>{@code
         * String[] input0 = {"foo", "bar"}; // string tensor shape is [2].
         * int[] input1 = new int[]{3, 2, 1}; // int tensor shape is [3].
         * Object[] data = {input0, input1, ...};
         * byte[] inputData = serializeObject(data);
         * }</pre>
         *
         * <p>For Executorch model, this field is a serialized EValue array.
         */
        @FlaggedApi(Flags.FLAG_EXECUTORCH_INFERENCE_API_ENABLED)
        public @NonNull Builder setInputData(@NonNull byte[] value) {
            mBuilderFieldsSet |= 0x2;
            mData = value;
            return this;
        }

        /**
         * Note: use {@link InferenceInput.Builder#setInputData(byte[])} instead.
         *
         * <p>An array of input data. The inputs should be in the same order as inputs of the model.
         *
         * <p>For example, if a model takes multiple inputs:
         *
         * <pre>{@code
         * String[] input0 = {"foo", "bar"}; // string tensor shape is [2].
         * int[] input1 = new int[]{3, 2, 1}; // int tensor shape is [3].
         * Object[] inputData = {input0, input1, ...};
         * }</pre>
         *
         * For LiteRT, this field is mapped to inputs of runForMultipleInputsOutputs:
         * https://www.tensorflow.org/lite/api_docs/java/org/tensorflow/lite/InterpreterApi#parameters_9
         */
        public @NonNull Builder setInputData(@NonNull Object... value) {
            mBuilderFieldsSet |= 0x2;
            mData = serializeObject(value);
            return this;
        }

        /**
         * The number of input examples. Adopter can set this field to run batching inference. The
         * batch size is 1 by default. The batch size should match the input data size.
         */
        public @NonNull Builder setBatchSize(int value) {
            mBuilderFieldsSet |= 0x4;
            mBatchSize = value;
            return this;
        }

        /**
         * The empty InferenceOutput representing the expected output structure. It's only required
         * by LiteRT model. For LiteRT, the inference code will verify whether this expected output
         * structure matches model output signature.
         *
         * <p>If a model produce string tensors:
         *
         * <pre>{@code
         * String[] output = new String[3][2];  // Output tensor shape is [3, 2].
         * HashMap<Integer, Object> outputs = new HashMap<>();
         * outputs.put(0, output);
         * expectedOutputStructure = new InferenceOutput.Builder().setDataOutputs(outputs).build();
         * }</pre>
         */
        public @NonNull Builder setExpectedOutputStructure(@NonNull InferenceOutput value) {
            mBuilderFieldsSet |= 0x8;
            mExpectedOutputStructure = value;
            return this;
        }

        /** @hide */
        private void validateInputData() {
            Preconditions.checkArgument(
                    mData.length > 0, "Input data should not be empty for InferenceInput.");
        }

        /** @hide */
        private void validateOutputStructure() {
            // ExecuTorch model doesn't require set output structure.
            if (mParams.getModelType() != Params.MODEL_TYPE_TENSORFLOW_LITE) {
                return;
            }
            Preconditions.checkArgument(
                    !mExpectedOutputStructure.getDataOutputs().isEmpty()
                            || mExpectedOutputStructure.getData().length > 0,
                    "ExpectedOutputStructure field is required for TensorflowLite model.");
        }

        /** Builds the instance. This builder should not be touched after calling this! */
        public @NonNull InferenceInput build() {

            mBuilderFieldsSet |= 0x10; // Mark builder used

            if ((mBuilderFieldsSet & 0x4) == 0) {
                mBatchSize = 1;
            }
            validateInputData();
            validateOutputStructure();
            InferenceInput o =
                    new InferenceInput(mParams, mData, mBatchSize, mExpectedOutputStructure);
            return o;
        }
    }
}
