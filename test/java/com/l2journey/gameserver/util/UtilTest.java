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
 */
package com.l2journey.gameserver.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.l2journey.gameserver.model.Location;

/**
 * Characterization tests for the pure helpers listed in issue #46.
 */
class UtilTest
{
	private static final double DELTA = 1e-9;
	
	@Test
	void calculateAngleFromCardinalDirections()
	{
		assertEquals(0.0, Util.calculateAngleFrom(0, 0, 1, 0), DELTA);
		assertEquals(90.0, Util.calculateAngleFrom(0, 0, 0, 1), DELTA);
		assertEquals(180.0, Util.calculateAngleFrom(0, 0, -1, 0), DELTA);
		assertEquals(270.0, Util.calculateAngleFrom(0, 0, 0, -1), DELTA);
	}
	
	@Test
	void calculateAngleFromLocationalDelegates()
	{
		final Location from = new Location(0, 0, 0);
		final Location to = new Location(0, 1, 0);
		assertEquals(90.0, Util.calculateAngleFrom(from, to), DELTA);
	}
	
	@Test
	void convertHeadingAndDegreeRoundTripCardinals()
	{
		assertEquals(0.0, Util.convertHeadingToDegree(0), DELTA);
		assertEquals(90.0, Util.convertHeadingToDegree(16383), 0.01);
		assertEquals(180.0, Util.convertHeadingToDegree(32767), 0.01);
		assertEquals(270.0, Util.convertHeadingToDegree(49151), 0.01);
		
		assertEquals(0, Util.convertDegreeToClientHeading(0));
		assertEquals(16383, Util.convertDegreeToClientHeading(90));
		assertEquals(32767, Util.convertDegreeToClientHeading(180));
		assertEquals(49151, Util.convertDegreeToClientHeading(270));
		assertEquals(16383, Util.convertDegreeToClientHeading(-270));
	}
	
	@Test
	void convertHeadingToRadianZeroIsNegativeHalfPi()
	{
		assertEquals(-Math.PI / 2, Util.convertHeadingToRadian(0), DELTA);
	}
	
	@Test
	void calculateHeadingFromCardinalDirections()
	{
		assertEquals(0, Util.calculateHeadingFrom(0, 0, 1, 0));
		assertEquals(16383, Util.calculateHeadingFrom(0, 0, 0, 1));
		assertEquals(32767, Util.calculateHeadingFrom(0, 0, -1, 0));
		assertEquals(49151, Util.calculateHeadingFrom(0, 0, 0, -1));
		
		assertEquals(0, Util.calculateHeadingFrom(1.0, 0.0));
		assertEquals(16383, Util.calculateHeadingFrom(0.0, 1.0));
		assertEquals(32767, Util.calculateHeadingFrom(-1.0, 0.0));
		assertEquals(49151, Util.calculateHeadingFrom(0.0, -1.0));
		
		final Location from = new Location(10, 20, 0);
		final Location to = new Location(10, 21, 0);
		assertEquals(16383, Util.calculateHeadingFrom(from, to));
	}
	
	@Test
	void calculateDistanceUses345Triangle()
	{
		assertEquals(5.0, Util.calculateDistance(0, 0, 3, 4), DELTA);
		assertEquals(5.0, Util.calculateDistance(0, 0, 0, 3, 4, 0, false), DELTA);
		assertEquals(5.0, Util.calculateDistance(0, 0, 0, 3, 4, 12, false), DELTA);
		assertEquals(13.0, Util.calculateDistance(0, 0, 0, 3, 4, 12, true), DELTA);
		
		assertEquals(25.0, Util.calculateDistance(0, 0, 0, 3, 4, 0, false, true), DELTA);
		assertEquals(5.0, Util.calculateDistance(0, 0, 0, 3, 4, 0, false, false), DELTA);
		assertEquals(169.0, Util.calculateDistance(0, 0, 0, 3, 4, 12, true, true), DELTA);
		
		final Location a = new Location(0, 0, 0);
		final Location b = new Location(3, 4, 12);
		assertEquals(5.0, Util.calculateDistance(a, b, false, false), DELTA);
		assertEquals(13.0, Util.calculateDistance(a, b, true, false), DELTA);
	}
	
	@Test
	void capitalizeFirstHandlesNullEmptyLetterAndNonLetter()
	{
		assertNull(Util.capitalizeFirst(null));
		assertEquals("", Util.capitalizeFirst(""));
		assertEquals("Hello", Util.capitalizeFirst("hello"));
		assertEquals("1abc", Util.capitalizeFirst("1abc"));
	}
	
	@Test
	void capitalizeWordsCapitalizesEachWord()
	{
		assertNull(Util.capitalizeWords(null));
		assertEquals("", Util.capitalizeWords(""));
		assertEquals("Hello World", Util.capitalizeWords("hello world"));
	}
	
	@Test
	void capitalizeWordsThrowsWhenStringEndsWithWhitespace()
	{
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> Util.capitalizeWords("hello "));
	}
	
	@Test
	void countWordsSplitsOnWhitespace()
	{
		assertEquals(2, Util.countWords("hello world"));
		assertEquals(1, Util.countWords(""));
		assertEquals(1, Util.countWords("   "));
		assertEquals(3, Util.countWords("  a  b   c "));
	}
	
	@Test
	void implodeStringJoinsIterable()
	{
		assertEquals("a,b,c", Util.implodeString(Arrays.asList("a", "b", "c"), ","));
		assertEquals("", Util.implodeString(Arrays.asList(), ","));
	}
	
	@Test
	void implodeJoinsArrayAndStripsOnlyLastDelimiterCharacter()
	{
		assertEquals("", Util.implode(new String[0], ","));
		assertEquals("a,b,c", Util.implode(new String[]
		{
			"a",
			"b",
			"c"
		}, ","));
		assertEquals("a||b|", Util.implode(new String[]
		{
			"a",
			"b"
		}, "||"));
	}
	
	@Test
	void roundToUsesMathRoundForOneOrFewerPlaces()
	{
		assertEquals(4.0f, Util.roundTo(3.6f, 0), DELTA);
		assertEquals(4.0f, Util.roundTo(3.6f, 1), DELTA);
		assertEquals(3.46f, Util.roundTo(3.456f, 2), 1e-5);
	}
	
	@Test
	void isDigitRejectsNullEmptyAndNonDigits()
	{
		assertFalse(Util.isDigit(null));
		assertFalse(Util.isDigit(""));
		assertFalse(Util.isDigit("12a"));
		assertTrue(Util.isDigit("123"));
	}
	
	@Test
	void isAlphaNumericRejectsNullEmptyAndSymbols()
	{
		assertFalse(Util.isAlphaNumeric(null));
		assertFalse(Util.isAlphaNumeric(""));
		assertFalse(Util.isAlphaNumeric("ab-1"));
		assertTrue(Util.isAlphaNumeric("Ab12"));
	}
	
	@Test
	void formatAdenaGroupsWithCommas()
	{
		assertEquals("1,234,567", Util.formatAdena(1234567L));
	}
	
	@Test
	void formatDoubleUsesProvidedPattern()
	{
		assertEquals("3.14", Util.formatDouble(3.14159, "0.00"));
	}
	
	@Test
	void formatDateReturnsNullForNullAndFormatsCalendarDate()
	{
		assertNull(Util.formatDate(null, "yyyy-MM-dd"));
		final Calendar calendar = new GregorianCalendar(2024, Calendar.MARCH, 5);
		final Date date = calendar.getTime();
		assertEquals("2024-03-05", Util.formatDate(date, "yyyy-MM-dd"));
	}
	
	@Test
	void containsUsesReferenceEqualityForObjectArrays()
	{
		final String shared = "same";
		final String[] array = new String[]
		{
			shared,
			"other"
		};
		assertTrue(Util.contains(array, shared));
		assertFalse(Util.contains(array, new String("same")));
		assertFalse(Util.contains(array, "missing"));
	}
	
	@Test
	void containsFindsIntValues()
	{
		assertTrue(Util.contains(new int[]
		{
			1,
			2,
			3
		}, 2));
		assertFalse(Util.contains(new int[]
		{
			1,
			2,
			3
		}, 4));
	}
	
	@Test
	void containsStringArrayRespectsIgnoreCase()
	{
		final String[] array = new String[]
		{
			"Alpha",
			"Beta"
		};
		assertTrue(Util.contains(array, "Alpha", false));
		assertFalse(Util.contains(array, "alpha", false));
		assertTrue(Util.contains(array, "alpha", true));
		assertFalse(Util.contains(array, "gamma", true));
	}
	
	@Test
	void startsWithMatchesPrefixes()
	{
		assertTrue(Util.startsWith(new String[]
		{
			"bypass -h",
			"other"
		}, "bypass"));
		assertFalse(Util.startsWith(new String[]
		{
			"bypass -h"
		}, "other"));
	}
	
	@Test
	void formatTimeFormatsUnitsAndZero()
	{
		assertEquals("now", Util.formatTime(0));
		assertEquals("1d 1h 1m 1s", Util.formatTime(90061));
		assertEquals("1h 1m 1s", Util.formatTime(-3661));
	}
	
	@Test
	void getStackTraceIncludesExceptionMessage()
	{
		final String stack = Util.getStackTrace(new IllegalStateException("boom"));
		assertTrue(stack.contains("IllegalStateException"));
		assertTrue(stack.contains("boom"));
	}
	
	@Test
	void getRangeHandlesInvertedEqualAndSpan()
	{
		assertNull(Util.getRange(5, 3));
		assertArrayEquals(new int[]
		{
			3
		}, Util.getRange(3, 3));
		assertArrayEquals(new int[]
		{
			1,
			2,
			3
		}, Util.getRange(1, 3));
		
		assertNull(Util.getRange(5L, 3L));
		assertArrayEquals(new long[]
		{
			3L
		}, Util.getRange(3L, 3L));
		assertArrayEquals(new long[]
		{
			1L,
			2L,
			3L
		}, Util.getRange(1L, 3L));
	}
	
	@Test
	void minMaxAndIndexHelpersFindExtremes()
	{
		assertEquals(-2, Util.min(3, -2, 7, 0));
		assertEquals(1, Util.min(5, 10, 1));
		assertEquals(7, Util.max(3, -2, 7, 0));
		assertEquals(10, Util.max(5, 1, 10));
		assertEquals(-2L, Util.min(3L, -2L, 7L));
		assertEquals(1L, Util.min(5L, 10L, 1L));
		assertEquals(7L, Util.max(3L, -2L, 7L));
		assertEquals(10L, Util.max(5L, 1L, 10L));
		assertEquals(-2.5f, Util.min(3f, -2.5f, 7f), DELTA);
		assertEquals(1f, Util.min(5f, 10f, 1f), DELTA);
		assertEquals(7f, Util.max(3f, -2.5f, 7f), DELTA);
		assertEquals(10f, Util.max(5f, 1f, 10f), DELTA);
		assertEquals(-2.5d, Util.min(3d, -2.5d, 7d), DELTA);
		assertEquals(1d, Util.min(5d, 10d, 1d), DELTA);
		assertEquals(7d, Util.max(3d, -2.5d, 7d), DELTA);
		assertEquals(10d, Util.max(5d, 1d, 10d), DELTA);
		
		assertEquals(1, Util.getIndexOfMinValue(3, -1, 7, 2));
		assertEquals(2, Util.getIndexOfMaxValue(3, -1, 7, 2));
	}
	
	@Test
	void mapAndConstrainClampThenScale()
	{
		assertEquals(0, Util.map(-10, 0, 100, 0, 10));
		assertEquals(5, Util.map(50, 0, 100, 0, 10));
		assertEquals(10, Util.map(150, 0, 100, 0, 10));
		
		assertEquals(0L, Util.map(-10L, 0L, 100L, 0L, 10L));
		assertEquals(5L, Util.map(50L, 0L, 100L, 0L, 10L));
		assertEquals(0.0, Util.map(-1.0, 0.0, 1.0, 0.0, 10.0), DELTA);
		assertEquals(5.0, Util.map(0.5, 0.0, 1.0, 0.0, 10.0), DELTA);
		
		assertEquals(0, Util.constrain(-1, 0, 10));
		assertEquals(5, Util.constrain(5, 0, 10));
		assertEquals(10, Util.constrain(11, 0, 10));
		assertEquals(0L, Util.constrain(-1L, 0L, 10L));
		assertEquals(10L, Util.constrain(11L, 0L, 10L));
		assertEquals(0.0, Util.constrain(-1.0, 0.0, 10.0), DELTA);
		assertEquals(10.0, Util.constrain(11.0, 0.0, 10.0), DELTA);
	}
	
	@Test
	void packAndUnpackRoundTripAndRejectInvalidInput() throws Exception
	{
		final int packed = Util.packInt(new int[]
		{
			1,
			2,
			3,
			4
		}, 8);
		assertEquals(0x01020304, packed);
		assertArrayEquals(new int[]
		{
			1,
			2,
			3,
			4
		}, Util.unpackInt(packed, 8));
		assertEquals(0x01020000, Util.packInt(new int[]
		{
			1,
			2
		}, 8));
		
		assertArrayEquals(new int[]
		{
			1,
			2,
			3,
			4,
			5,
			6,
			7,
			8
		}, Util.unpackLong(0x0102030405060708L, 8));
		
		final Exception overflow = assertThrows(Exception.class, () -> Util.packInt(new int[]
		{
			1,
			2,
			3,
			4,
			5
		}, 8));
		assertEquals("Overflow", overflow.getMessage());
		
		final Exception overload = assertThrows(Exception.class, () -> Util.packInt(new int[]
		{
			256
		}, 8));
		assertEquals("Overload, value is out of range", overload.getMessage());
	}
	
	@Test
	void isIntegerRecognizesDigitCharactersOnly()
	{
		assertTrue(Util.isInteger('0'));
		assertTrue(Util.isInteger('9'));
		assertFalse(Util.isInteger('a'));
		assertFalse(Util.isInteger('-'));
	}
	
	@Test
	void getRelativePathReturnsSuffixUnderBase(@TempDir File tempDir) throws Exception
	{
		final File child = new File(tempDir, "sub/file.txt");
		Files.createDirectories(child.getParentFile().toPath());
		Files.writeString(child.toPath(), "x");
		final String relative = Util.getRelativePath(tempDir, child);
		assertTrue(relative.replace('\\', '/').endsWith("sub/file.txt"));
	}
}
