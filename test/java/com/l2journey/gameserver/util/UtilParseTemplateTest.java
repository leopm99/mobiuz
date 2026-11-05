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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Util#parseTemplate(String)} (java:S8786).
 */
class UtilParseTemplateTest
{
	@Test
	void noMarkersYieldsOnlyRemainder()
	{
		final String html = "<html><body>plain</body></html>";
		final Map<Integer, String> tpls = Util.parseTemplate(html);
		assertEquals(1, tpls.size());
		assertEquals(html, tpls.get(0));
	}
	
	@Test
	void severalTemplatesKeepRemainderOrder()
	{
		final String html = "A<!--TEMPLET1firstTEMPLET-->B<!--TEMPLET2TEMPLET-->C<!--TEMPLET3line1\nline2TEMPLET-->D";
		final Map<Integer, String> tpls = Util.parseTemplate(html);
		assertEquals("first", tpls.get(1));
		assertEquals("", tpls.get(2));
		assertEquals("line1\nline2", tpls.get(3));
		assertEquals("ABCD", tpls.get(0));
	}
	
	@Test
	void falseOpenMarkerBeforeRealTemplateStillMatches()
	{
		final Map<Integer, String> tpls = Util.parseTemplate("<!--TEMPLET <!--TEMPLET1bodyTEMPLET-->");
		assertEquals("body", tpls.get(1));
		assertEquals("<!--TEMPLET ", tpls.get(0));
	}
	
	@Test
	void digitsWithoutCloserLeaveOriginalInRemainder()
	{
		final String html = "prefix<!--TEMPLET1body";
		final Map<Integer, String> tpls = Util.parseTemplate(html);
		assertFalse(tpls.containsKey(1));
		assertEquals(html, tpls.get(0));
	}
	
	@Test
	void digitsAtEndWithoutCloserLeaveOriginalInRemainder()
	{
		final String html = "<!--TEMPLET12";
		final Map<Integer, String> tpls = Util.parseTemplate(html);
		assertFalse(tpls.containsKey(12));
		assertEquals(html, tpls.get(0));
	}
}
