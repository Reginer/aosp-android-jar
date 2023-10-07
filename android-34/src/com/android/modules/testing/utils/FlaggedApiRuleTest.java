/**
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

import static com.android.modules.testing.utils.FlaggedApiRule.UsesFlaggedApi;
import static org.junit.Assert.assertTrue;

import org.junit.runners.Parameterized;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.Rule;

@RunWith(Parameterized.class)
public class FlaggedApiRuleTest {
    @Rule
    @Parameterized.Parameter
    public FlaggedApiRule mFlaggedApiRule;

    @Parameterized.Parameters
    public static Object[] data() {
        return new FlaggedApiRule[]{
            new FlaggedApiRule.DoNothingRule(),
            new FlaggedApiRule.SkipTestsAnnotatedWithUsesFlaggedApiRule(),
        };
    }

    @Test
    @UsesFlaggedApi
    public void testIsSkippedIfRuleSaysSo() {
        // This @Test will be called twice, with two different FlaggedApiRule objects (see the
        // @Parameterized.Parameters annotated method above). One of the Rules will make JUnit skip
        // this @Test: assert this is the case by checking the current @Rule when the method is
        // actually called.
        assertTrue(mFlaggedApiRule instanceof FlaggedApiRule.DoNothingRule);
    }
}
