/*
 * Copyright (C) 2019 The Android Open Source Project
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
package android.compat.annotation;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates that this non-SDK interface is used by apps. A non-SDK interface is a
 * class member (field or method) that is not part of the public SDK. Since the
 * member is not part of the SDK, usage by apps is not supported.
 *
 * <h2>If you are an Android App developer</h2>
 *
 * This annotation indicates that you may be able to access the member at runtime, but that
 * this access is discouraged and not supported by Android. If there is a value
 * for {@link #maxTargetSdk()} on the annotation, access will be restricted based
 * on the {@code targetSdkVersion} value set in your manifest.
 *
 * <p>Fields and methods annotated with this are likely to be restricted, changed
 * or removed in future Android releases. If you rely on these members for
 * functionality that is not otherwise supported by Android, consider filing a
 * <a href="http://g.co/dev/appcompat">feature request</a>.
 *
 * <h2>If you are an Android OS developer</h2>
 *
 * This annotation acts as a heads up that changing a given method or field
 * may affect apps, potentially breaking them when the next Android version is
 * released. In some cases, for members that are heavily used, this annotation
 * may imply restrictions on changes to the member.
 *
 * <p>This annotation also results in access to the member being permitted by the
 * runtime, with a warning being generated in debug builds. Which apps can access
 * the member is determined by the value of {@link #maxTargetSdk()}.
 *
 * <p>For more details, see go/UnsupportedAppUsage.
 *
 * {@hide}
 */
@Retention(CLASS)
@Target({CONSTRUCTOR, METHOD, FIELD, TYPE})
@Repeatable(UnsupportedAppUsage.Container.class)
public @interface UnsupportedAppUsage {

    /**
     * Associates a bug tracking the work to add a public alternative to this API. Optional.
     *
     * @return ID of the associated tracking bug
     */
    long trackingBug() default 0;

    /**
     * Indicates that usage of this API is limited to apps based on their target SDK version.
     *
     * <p>Access to the API is allowed if the targetSdkVersion in the apps manifest is no greater
     * than this value. Access checks are performed at runtime.
     *
     * <p>This is used to give app developers a grace period to migrate off a non-SDK interface.
     * When making Android version N, existing APIs can have a maxTargetSdk of N-1 added to them.
     * Developers must then migrate off the API when their app is updated in future, but it will
     * continue working in the meantime.
     *
     * <p>Possible values are:
     * <ul>
     *     <li>
     *         An API level like {@link VersionCodes#O} - in which case the API is available up to
     *         and including the specified release. Or, in other words, access to the API is
     *         blocked (unavailable) from the next API level from the one specified.
     *     </li>
     *     <li>
     *         absent (default value) - All apps can access this API, but doing so may result in
     *         warnings in the log, UI warnings (on developer builds) and/or strictmode violations.
     *         The API is likely to be further restricted in future.
     *     </li>
     *
     * </ul>
     *
     * @return The maximum value for an apps targetSdkVersion in order to access this API.
     */
    int maxTargetSdk() default Integer.MAX_VALUE;

    /**
     * The signature of an implicit (not present in the source) member that forms part of the
     * hiddenapi.
     *
     * <p>Allows access to non-SDK API elements that are not represented in the input source to be
     * managed.
     *
     * <p>This must only be used when applying the annotation to a type, using it in any other
     * situation is an error.
     *
     * @return A dex API signature.
     */
    String implicitMember() default "";

    /**
     * Public API alternatives to this API.
     *
     * <p>If non-empty, the string must be a description of the public API alternative(s) to this
     * API. The explanation must contain at least one Javadoc link tag to public API methods or
     * fields. e.g.:
     * {@literal @UnsupportedAppUsage(publicAlternatives="Use {@link foo.bar.Baz#bat()} instead.")}
     *
     * <p>Any elements that can be deduced can be omitted, e.g.:
     * <ul>
     *      <li>
     *          the class, if it's the same as for the annotated element.
     *      </li>
     *      <li>
     *          the package name, if it's the same as for the annotated element.
     *      </li>
     *      <li>
     *          the method parameters, if there is only one method with that name in the given
     *          package and class.
     *      </li>
     * </ul>
     * @return A Javadoc-formatted string.
     */
    @SuppressWarnings("JavadocReference")
    String publicAlternatives() default "";

    /**
     * Container for {@link UnsupportedAppUsage} that allows it to be applied repeatedly to types.
     */
    @Retention(CLASS)
    @Target(TYPE)
    @interface Container {
        UnsupportedAppUsage[] value();
    }

    /**
     * Internal usage only.
     *
     * Override the default source position when generating an index of the annotations.
     *
     * <p>This is intended for use by tools that generate java source code, to point to the
     * original source position of the annotation, rather than the position within the generated
     * code. It should never be set manually.
     *
     * <p>The format of the value is "path/to/file:startline:startcol:endline:endcol" indicating
     * the position of the annotation itself.
     */
    String overrideSourcePosition() default "";

    /**
     * Internal usage only.
     *
     * For debug use only. The expected dex signature to be generated for this API, used to verify
     * parts of the build process.
     *
     * @return A dex API signature.
     */
    String expectedSignature() default "";
}
