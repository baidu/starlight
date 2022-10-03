/*
 * Copyright (c) 2019 Baidu, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package com.baidu.cloud.starlight.api.utils;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by liuruisen on 2020/3/19.
 */
public class StringUtilsTest {

    @Test
    public void parseInteger() {

        Integer result1 = StringUtils.parseInteger("123");
        Assert.assertTrue(result1 == 123);

        Integer result2 = StringUtils.parseInteger("");
        Assert.assertTrue(result2 == 0);
    }

    @Test
    public void isInteger() {
        Assert.assertTrue(StringUtils.isInteger("123"));
        Assert.assertFalse(StringUtils.isInteger(""));
        Assert.assertFalse(StringUtils.isInteger(null));
    }

    @Test
    public void isDigits() {
        Assert.assertTrue(StringUtils.isDigits("123123213"));
        Assert.assertFalse(StringUtils.isDigits(null));
        Assert.assertFalse(StringUtils.isDigits(""));
        Assert.assertFalse(StringUtils.isDigits("2e12312312dsada"));
    }

    @Test
    public void isEquals() {
        Assert.assertTrue(StringUtils.isEquals("123", "123"));
        Assert.assertFalse(StringUtils.isEquals(null, "123"));
        Assert.assertFalse(StringUtils.isEquals("123", null));
        Assert.assertTrue(StringUtils.isEquals(null, null));
        Assert.assertFalse(StringUtils.isEquals("23", "123213"));
    }

    @Test
    public void hasLength() {
        Assert.assertTrue(StringUtils.hasLength("123"));
        Assert.assertTrue(StringUtils.hasLength("   "));
        Assert.assertFalse(StringUtils.hasLength(""));
        Assert.assertFalse(StringUtils.hasLength(null));
    }

    @Test
    public void hasText() {
        Assert.assertFalse(StringUtils.hasText(null));
        Assert.assertFalse(StringUtils.hasText(""));
        Assert.assertFalse(StringUtils.hasText(" "));
        Assert.assertTrue(StringUtils.hasText("12345"));
    }

    @Test
    public void isNumeric() {
        Assert.assertFalse(StringUtils.isNumeric(null));
        Assert.assertTrue(StringUtils.isNumeric(""));
        Assert.assertFalse(StringUtils.isNumeric("  "));
        Assert.assertTrue(StringUtils.isNumeric("123"));
        Assert.assertFalse(StringUtils.isNumeric("12 3"));
        Assert.assertFalse(StringUtils.isNumeric("ab2c"));
        Assert.assertFalse(StringUtils.isNumeric("12-3"));
        Assert.assertFalse(StringUtils.isNumeric("12.3"));
    }

    @Test
    public void isEmpty() {
        Assert.assertTrue(StringUtils.isEmpty(null));
        Assert.assertTrue(StringUtils.isEmpty(""));
        Assert.assertFalse(StringUtils.isEmpty("   "));
        Assert.assertFalse(StringUtils.isEmpty("123n "));
    }

    @Test
    public void substringAfterLast() {
        String origin = "123213,,,,346";
        String compare = "346";
        String result = StringUtils.substringAfterLast(origin, ",");
        Assert.assertTrue(result.equals(compare));

        String result2 = StringUtils.substringAfterLast("", ",");
        Assert.assertTrue(result2.equals(""));

        String result3 = StringUtils.substringAfterLast(origin, "");
        Assert.assertTrue(result3.equals(""));
    }

    @Test
    public void substringBeforeLast() {
        String origin = "123213,,,,346";
        String compare = "123213,,,";
        String result = StringUtils.substringBeforeLast(origin, ",");
        Assert.assertTrue(result.equals(compare));

        String result2 = StringUtils.substringBeforeLast("", ",");
        Assert.assertTrue(result2.equals(""));

        String result3 = StringUtils.substringBeforeLast(origin, "");
        Assert.assertTrue(result3.equals(origin));

        String result4 = StringUtils.substringBeforeLast(null, ",");
        Assert.assertTrue(result4 == null);

        String result5 = StringUtils.substringBeforeLast(origin, null);
        Assert.assertTrue(result5.equals(origin));

        String result6 = StringUtils.substringBeforeLast(origin, ".");
        Assert.assertTrue(result6.equals(origin));
    }

    @Test
    public void substringBetween() {
        String origin = "123213,,,,346";
        String compare = "3213,,,,3";
        String result = StringUtils.substringBetween(origin, "12", "46");
        Assert.assertTrue(result.equals(compare));

        String result2 = StringUtils.substringBetween("", ",", null);
        Assert.assertTrue(result2 == null);

        String result3 = StringUtils.substringBetween(origin, "12", "2sadasd");
        Assert.assertTrue(result3 == null);
    }

    @Test
    public void trimAllWhitespace() {
        String origin = "1 2 3 \n 4 \t 5";
        String result = StringUtils.trimAllWhitespace(origin);
        Assert.assertTrue(result.equals("12345"));

        String result2 = StringUtils.trimAllWhitespace("");
        Assert.assertTrue(result2.equals(""));
    }

    @Test
    public void isBlank() {
        Assert.assertTrue(StringUtils.isBlank(null));
        Assert.assertTrue(StringUtils.isBlank(""));
        Assert.assertTrue(StringUtils.isBlank(" "));
        Assert.assertFalse(StringUtils.isBlank("bob"));
        Assert.assertFalse(StringUtils.isBlank("  bob  "));
    }

    @Test
    public void trimToEmpty() {
        Assert.assertEquals(StringUtils.trimToEmpty(null), "");
        Assert.assertEquals(StringUtils.trimToEmpty(""), "");
        Assert.assertEquals(StringUtils.trimToEmpty("       "), "");
        Assert.assertEquals(StringUtils.trimToEmpty("abc"), "abc");
        Assert.assertEquals(StringUtils.trimToEmpty("   abc   "), "abc");
    }

    @Test
    public void substringBefore() {
        Assert.assertEquals(StringUtils.substringBefore(null, "."), null);
        Assert.assertEquals(StringUtils.substringBefore("", "."), "");
        Assert.assertEquals(StringUtils.substringBefore("abc", "a"), "");
        Assert.assertEquals(StringUtils.substringBefore("abcba", "b"), "a");
        Assert.assertEquals(StringUtils.substringBefore("abc", "c"), "ab");
        Assert.assertEquals(StringUtils.substringBefore("abc", "d"), "abc");
        Assert.assertEquals(StringUtils.substringBefore("abc", ""), "");
        Assert.assertEquals(StringUtils.substringBefore("abc", null), "abc");
    }

    @Test
    public void substringAfter() {
        Assert.assertEquals(StringUtils.substringAfter(null, "23"), null);
        Assert.assertEquals(StringUtils.substringAfter("", "123"), "");
        Assert.assertEquals(StringUtils.substringAfter("23", null), "");
        Assert.assertEquals(StringUtils.substringAfter("abc", "a"), "bc");
        Assert.assertEquals(StringUtils.substringAfter("abcba", "b"), "cba");
        Assert.assertEquals(StringUtils.substringAfter("abc", "c"), "");
        Assert.assertEquals(StringUtils.substringAfter("abc", "d"), "");
        Assert.assertEquals(StringUtils.substringAfter("abc", ""), "abc");
    }

    @Test
    public void equalsIgnoreCase() {
        Assert.assertTrue(StringUtils.equalsIgnoreCase(null, null));
        Assert.assertFalse(StringUtils.equalsIgnoreCase(null, "abc"));
        Assert.assertFalse(StringUtils.equalsIgnoreCase("abc", null));
        Assert.assertTrue(StringUtils.equalsIgnoreCase("abc", "abc"));
        Assert.assertTrue(StringUtils.equalsIgnoreCase("abc", "ABC"));
        Assert.assertFalse(StringUtils.equalsIgnoreCase("abc", "ABDC"));
    }
}