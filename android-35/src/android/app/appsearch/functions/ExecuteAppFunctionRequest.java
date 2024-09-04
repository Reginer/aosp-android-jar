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

package android.app.appsearch.functions;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.app.appsearch.GenericDocument;
import android.app.appsearch.flags.Flags;
import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.app.appsearch.safeparcel.GenericDocumentParcel;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Represents a request to execute a specific app function.
 *
 * @see AppFunctionManager#executeAppFunction(ExecuteAppFunctionRequest, Executor, Consumer)
 */
@FlaggedApi(Flags.FLAG_ENABLE_APP_FUNCTIONS)
@SafeParcelable.Class(creator = "ExecuteAppFunctionRequestCreator")
public final class ExecuteAppFunctionRequest extends AbstractSafeParcelable implements Parcelable {
    @NonNull
    public static final Parcelable.Creator<ExecuteAppFunctionRequest> CREATOR =
            new ExecuteAppFunctionRequestCreator();

    @Field(id = 1, getter = "getTargetPackageName")
    @NonNull
    private final String mTargetPackageName;

    @Field(id = 2, getter = "getFunctionIdentifier")
    @NonNull
    private final String mFunctionIdentifier;

    /**
     * {@link GenericDocument} is not a Parcelable, so storing it as a GenericDocumentParcel here.
     */
    @Field(id = 3)
    @NonNull
    final GenericDocumentParcel mParameters;

    @Field(id = 4, getter = "getExtras")
    @NonNull
    private final Bundle mExtras;

    @NonNull private final GenericDocument mParametersCached;

    /** Returns the package name of the app that hosts the function. */
    @NonNull
    public String getTargetPackageName() {
        return mTargetPackageName;
    }

    /** Returns the unique string identifier of the app function to be executed. */
    @NonNull
    public String getFunctionIdentifier() {
        return mFunctionIdentifier;
    }

    /**
     * Returns the parameters required to invoke this function. Within this {@link GenericDocument},
     * the property names are the names of the function parameters and the property values are the
     * values of those parameters
     *
     * <p>The document may have missing parameters. Developers are advised to implement defensive
     * handling measures.
     */
    @NonNull
    public GenericDocument getParameters() {
        return mParametersCached;
    }

    /** Returns additional metadata relevant to this function execution request. */
    @NonNull
    public Bundle getExtras() {
        return mExtras;
    }

    private ExecuteAppFunctionRequest(
            @NonNull String targetPackageName,
            @NonNull String functionIdentifier,
            @NonNull GenericDocument document,
            @NonNull Bundle extras) {
        mTargetPackageName = Objects.requireNonNull(targetPackageName);
        mFunctionIdentifier = Objects.requireNonNull(functionIdentifier);
        mParametersCached = Objects.requireNonNull(document);
        mParameters = mParametersCached.getDocumentParcel();
        mExtras = Objects.requireNonNull(extras);
    }

    @Constructor
    ExecuteAppFunctionRequest(
            @Param(id = 1) @NonNull String targetPackageName,
            @Param(id = 2) @NonNull String functionIdentifier,
            @Param(id = 3) @NonNull GenericDocumentParcel parameters,
            @Param(id = 4) @NonNull Bundle extras) {
        mTargetPackageName = Objects.requireNonNull(targetPackageName);
        mFunctionIdentifier = Objects.requireNonNull(functionIdentifier);
        mParameters = Objects.requireNonNull(parameters);
        mParametersCached = new GenericDocument(mParameters);
        mExtras = Objects.requireNonNull(extras);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        ExecuteAppFunctionRequestCreator.writeToParcel(this, dest, flags);
    }

    /** The builder for creating {@link ExecuteAppFunctionRequest} instances. */
    @FlaggedApi(Flags.FLAG_ENABLE_APP_FUNCTIONS)
    public static final class Builder {
        @NonNull private final String mPackageName;
        @NonNull private final String mFunctionIdentifier;
        @NonNull private GenericDocument mParameters = GenericDocument.EMPTY;
        @NonNull private Bundle mExtras = Bundle.EMPTY;

        /**
         * Creates a new instance of this builder class.
         *
         * @param packageName The package name of the target app providing the app function to
         *     invoke.
         * @param functionIdentifier The identifier used by the {@link AppFunctionService} from the
         *     target app to uniquely identify the function to be invoked.
         */
        public Builder(@NonNull String packageName, @NonNull String functionIdentifier) {
            mPackageName = Objects.requireNonNull(packageName);
            mFunctionIdentifier = Objects.requireNonNull(functionIdentifier);
        }

        /**
         * Sets parameters for invoking the app function. Within this {@link GenericDocument}, the
         * property names are the names of the function parameters and the property values are the
         * values of those parameters. Defaults to an empty {@link GenericDocument} if not set.
         */
        @NonNull
        public Builder setParameters(@NonNull GenericDocument parameters) {
            mParameters = parameters;
            return this;
        }

        /**
         * Sets the additional metadata relevant to this function execution request. Defaults to an
         * empty {@link Bundle} if not set.
         */
        @NonNull
        public Builder setExtras(@NonNull Bundle extras) {
            mExtras = extras;
            return this;
        }

        /** Constructs a new {@link ExecuteAppFunctionRequest} from the contents of this builder. */
        @NonNull
        public ExecuteAppFunctionRequest build() {
            return new ExecuteAppFunctionRequest(
                    mPackageName, mFunctionIdentifier, mParameters, mExtras);
        }
    }
}
