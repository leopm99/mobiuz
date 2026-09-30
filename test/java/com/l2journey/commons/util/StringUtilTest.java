/*
 * Copyright (c) 2025 L2Journey Project
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 * ---
 * 
 * Portions of this software are derived from the L2JMobius Project, 
 * shared under the MIT License. The original license terms are preserved where 
 * applicable..
 * 
 */
package com.l2journey.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.StringTokenizer;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link StringUtil}.
 */
class StringUtilTest
{
	private enum SampleEnum
	{
		ENUM_CONSTANT_NAME
	}
	
	@Test
	void parseIntReturnsParsedValue()
	{
		assertEquals(42, StringUtil.parseInt("42", -1));
	}
	
	@Test
	void parseIntReturnsDefaultOnBadInput()
	{
		assertEquals(-1, StringUtil.parseInt("not-a-number", -1));
		assertEquals(-1, StringUtil.parseInt(null, -1));
	}
	
	@Test
	void parseNextIntReturnsDefaultWhenEmptyOrInvalid()
	{
		assertEquals(7, StringUtil.parseNextInt(new StringTokenizer(""), 7));
		assertEquals(7, StringUtil.parseNextInt(new StringTokenizer("abc"), 7));
	}
	
	@Test
	void parseNextIntReturnsParsedToken()
	{
		assertEquals(12, StringUtil.parseNextInt(new StringTokenizer("12 34"), -1));
	}
	
	@Test
	void isNumericRejectsNullEmptyAndLeadingMinus()
	{
		assertFalse(StringUtil.isNumeric(null));
		assertFalse(StringUtil.isNumeric(""));
		assertFalse(StringUtil.isNumeric("-1"));
		assertTrue(StringUtil.isNumeric("123"));
	}
	
	@Test
	void isIntegerAcceptsLeadingMinus()
	{
		assertTrue(StringUtil.isInteger("-1"));
		assertTrue(StringUtil.isInteger("123"));
		assertFalse(StringUtil.isInteger(null));
		assertFalse(StringUtil.isInteger(""));
		assertFalse(StringUtil.isInteger("1.5"));
	}
	
	@Test
	void isAlphaNumericRejectsNullEmptyAndPunctuation()
	{
		assertFalse(StringUtil.isAlphaNumeric(null));
		assertFalse(StringUtil.isAlphaNumeric(""));
		assertFalse(StringUtil.isAlphaNumeric("ab-cd"));
		assertTrue(StringUtil.isAlphaNumeric("abc123"));
	}
	
	@Test
	void capitalizeFirstLeavesNullAndEmptyUnchanged()
	{
		assertNull(StringUtil.capitalizeFirst(null));
		assertEquals("", StringUtil.capitalizeFirst(""));
	}
	
	@Test
	void capitalizeFirstLowercasesRemainder()
	{
		assertEquals("Hello", StringUtil.capitalizeFirst("hELLO"));
	}
	
	@Test
	void separateWordsSplitsCamelCase()
	{
		assertEquals("Camel Case", StringUtil.separateWords("CamelCase"));
	}
	
	@Test
	void enumToStringFormatsConstantName()
	{
		assertEquals("Enum Constant Name", StringUtil.enumToString(SampleEnum.ENUM_CONSTANT_NAME));
	}
	
	@Test
	void concatRendersNullArgumentAsNullLiteral()
	{
		assertEquals("anullb", StringUtil.concat("a", null, "b"));
	}
	
	@Test
	void implodeJoinsWithDelimiter()
	{
		assertEquals("a,b,c", StringUtil.implode(Arrays.asList("a", "b", "c"), ","));
		assertEquals("a|b", StringUtil.implode(new String[]
		{
			"a",
			"b"
		}, "|"));
	}
}
