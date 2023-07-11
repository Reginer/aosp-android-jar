/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.modules.utils;

import androidx.test.filters.SmallTest;

import libcore.util.HexEncoding;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
@SmallTest
public class BytesMatcherTest extends TestCase {
    @Test
    public void testEmpty() throws Exception {
        BytesMatcher matcher = BytesMatcher.decode("");
        assertFalse(matcher.test(HexEncoding.decode("cafe")));
        assertFalse(matcher.test(HexEncoding.decode("")));
    }

    @Test
    public void testExact() throws Exception {
        BytesMatcher matcher = BytesMatcher.decode("+cafe");
        assertTrue(matcher.test(HexEncoding.decode("cafe")));
        assertFalse(matcher.test(HexEncoding.decode("beef")));
        assertFalse(matcher.test(HexEncoding.decode("ca")));
        assertFalse(matcher.test(HexEncoding.decode("cafe00")));
    }

    @Test
    public void testMask() throws Exception {
        BytesMatcher matcher = BytesMatcher.decode("+cafe/ff00");
        assertTrue(matcher.test(HexEncoding.decode("cafe")));
        assertTrue(matcher.test(HexEncoding.decode("ca88")));
        assertFalse(matcher.test(HexEncoding.decode("beef")));
        assertFalse(matcher.test(HexEncoding.decode("ca")));
        assertFalse(matcher.test(HexEncoding.decode("cafe00")));
    }

    @Test
    public void testPrefix() throws Exception {
        BytesMatcher matcher = BytesMatcher.decode("⊆cafe,⊆beef/ff00");
        assertTrue(matcher.test(HexEncoding.decode("cafe")));
        assertFalse(matcher.test(HexEncoding.decode("caff")));
        assertTrue(matcher.test(HexEncoding.decode("cafecafe")));
        assertFalse(matcher.test(HexEncoding.decode("ca")));
        assertTrue(matcher.test(HexEncoding.decode("beef")));
        assertTrue(matcher.test(HexEncoding.decode("beff")));
        assertTrue(matcher.test(HexEncoding.decode("beffbeff")));
        assertFalse(matcher.test(HexEncoding.decode("be")));
    }

    @Test
    public void testSerialize_Empty() throws Exception {
        BytesMatcher matcher = new BytesMatcher();
        matcher = BytesMatcher.decode(BytesMatcher.encode(matcher));

        // Also very empty and null values
        BytesMatcher.decode("");
        BytesMatcher.decode(null);
    }

    @Test
    public void testSerialize_Exact() throws Exception {
        BytesMatcher matcher = new BytesMatcher();
        matcher.addExactRejectRule(HexEncoding.decode("cafe00112233"),
                HexEncoding.decode("ffffff000000"));
        matcher.addExactRejectRule(HexEncoding.decode("beef00112233"),
                null);
        matcher.addExactAcceptRule(HexEncoding.decode("000000000000"),
                HexEncoding.decode("000000000000"));

        assertFalse(matcher.test(HexEncoding.decode("cafe00ffffff")));
        assertFalse(matcher.test(HexEncoding.decode("beef00112233")));
        assertTrue(matcher.test(HexEncoding.decode("beef00ffffff")));

        // Bounce through serialization pass and confirm it still works
        matcher = BytesMatcher.decode(BytesMatcher.encode(matcher));

        assertFalse(matcher.test(HexEncoding.decode("cafe00ffffff")));
        assertFalse(matcher.test(HexEncoding.decode("beef00112233")));
        assertTrue(matcher.test(HexEncoding.decode("beef00ffffff")));
    }

    @Test
    public void testSerialize_Prefix() throws Exception {
        BytesMatcher matcher = new BytesMatcher();
        matcher.addExactRejectRule(HexEncoding.decode("aa"), null);
        matcher.addExactAcceptRule(HexEncoding.decode("bb"), null);
        matcher.addPrefixAcceptRule(HexEncoding.decode("aa"), null);
        matcher.addPrefixRejectRule(HexEncoding.decode("bb"), null);

        assertFalse(matcher.test(HexEncoding.decode("aa")));
        assertTrue(matcher.test(HexEncoding.decode("bb")));
        assertTrue(matcher.test(HexEncoding.decode("aaaa")));
        assertFalse(matcher.test(HexEncoding.decode("bbbb")));

        // Bounce through serialization pass and confirm it still works
        matcher = BytesMatcher.decode(BytesMatcher.encode(matcher));

        assertFalse(matcher.test(HexEncoding.decode("aa")));
        assertTrue(matcher.test(HexEncoding.decode("bb")));
        assertTrue(matcher.test(HexEncoding.decode("aaaa")));
        assertFalse(matcher.test(HexEncoding.decode("bbbb")));
    }

    @Test
    public void testOrdering_RejectFirst() throws Exception {
        BytesMatcher matcher = BytesMatcher.decode("-ff/0f,+ff/f0");
        assertFalse(matcher.test(HexEncoding.decode("ff")));
        assertTrue(matcher.test(HexEncoding.decode("f0")));
        assertFalse(matcher.test(HexEncoding.decode("0f")));
    }

    @Test
    public void testOrdering_AcceptFirst() throws Exception {
        BytesMatcher matcher = BytesMatcher.decode("+ff/f0,-ff/0f");
        assertTrue(matcher.test(HexEncoding.decode("ff")));
        assertTrue(matcher.test(HexEncoding.decode("f0")));
        assertFalse(matcher.test(HexEncoding.decode("0f")));
    }
}
