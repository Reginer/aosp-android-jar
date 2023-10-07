/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.modules.testing.utils;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;

import com.android.internal.annotations.VisibleForTesting;

import org.junit.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A JUnit Rule to skip tests that rely on <pre>@FlaggedApi</pre> symbols, if those symbols have
 * been configured to be hidden.
 *
 * <p>This rule prevents the tests from breaking when public symbols are made hidden because of
 * <pre>@FlaggedApi</pre>. To tell the JUnit framework which tests that rely on
 * <pre>@FlaggedApi</pre> symbols, annotate the test with <pre>@FlaggedApiRule.UsesFlaggedApi</pre>.
 *
 * <p>Example usage:
 *
 * <pre>
 * @RunWith(AndroidJUnit4.class)
 * public class Test {
 *     @Rule
 *     public final FlaggedApiRule mFlaggedApiRule = FlaggedApiRule.getInstance();
 *
 *     @Test
 *     @FlaggedApiRule.UsesFlaggedApi
 *     public void testFlaggedApiSymbol() throws Exception {
 *         // test that calls a @FlaggedApi method
 *     }
 *
 *     @Test
 *     public void testThatWillAlwaysRun() throws Eception {
 *         // test that calls a non-@FlaggedApi method
 *     }
 * }
 * </pre>
 *
 * <p>Note: because <pre>@FlaggedApiRule.UsesFlaggedApi</pre> can be completely skipped, make sure
 * to have other tests that verify the non-<pre>@FlaggedApi</pre> parts of your code, or your test
 * coverage may be smaller than expected.
 *
 * <p>Note: requires JUnit 4.
 *
 * @see android.annotation.FlaggedApi
 */
public abstract class FlaggedApiRule implements TestRule {
    private static final boolean SKIP_TESTS_ANNOTATED_FLAGGED_API = false;

    private static final FlaggedApiRule sInstance;

    static {
        if (SKIP_TESTS_ANNOTATED_FLAGGED_API) {
            sInstance = new SkipTestsAnnotatedWithUsesFlaggedApiRule();
        } else {
            sInstance = new DoNothingRule();
        }
    }

    public static FlaggedApiRule getInstance() {
        return sInstance;
    }

    @VisibleForTesting
    protected static final class SkipTestsAnnotatedWithUsesFlaggedApiRule extends FlaggedApiRule {
        @Override
        public Statement apply(Statement statement, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    if (description.getAnnotation(UsesFlaggedApi.class) != null) {
                        throw new AssumptionViolatedException("Skip @UsesFlaggedApi annotated test");
                    }
                    statement.evaluate();
                }
            };
        }
    }

    @VisibleForTesting
    protected static final class DoNothingRule extends FlaggedApiRule {
        @Override
        public Statement apply(Statement statement, Description description) {
            return statement;
        }
    }

    @Target({TYPE, FIELD, METHOD, CONSTRUCTOR, ANNOTATION_TYPE, PACKAGE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface UsesFlaggedApi {}
}
