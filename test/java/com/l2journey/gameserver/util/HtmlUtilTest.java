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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Characterization tests for the public gauge and paging helpers in {@link HtmlUtil}.
 */
class HtmlUtilTest
{
	@Test
	void getCpGaugeBuildsPercentageFractionAndClampedHtml()
	{
		final String percent50 = String.format("%.2f%%", 50.0);
		final String percent100 = String.format("%.2f%%", 100.0);
		assertEquals(gaugeHtml("L2UI_CT1.Gauges.Gauge_DF_Large_CP_bg_Center", "L2UI_CT1.Gauges.Gauge_DF_Large_CP_Center", 50, percentTable(percent50)), HtmlUtil.getCpGauge(100, 50, 100, true));
		assertEquals(gaugeHtml("L2UI_CT1.Gauges.Gauge_DF_Large_CP_bg_Center", "L2UI_CT1.Gauges.Gauge_DF_Large_CP_Center", 50, fractionTable(50, 100)), HtmlUtil.getCpGauge(100, 50, 100, false));
		assertEquals(gaugeHtml("L2UI_CT1.Gauges.Gauge_DF_Large_CP_bg_Center", "L2UI_CT1.Gauges.Gauge_DF_Large_CP_Center", 100, percentTable(percent100)), HtmlUtil.getCpGauge(100, 150, 100, true));
	}
	
	@Test
	void otherGaugesUseExpectedImagesAndSharedLabels()
	{
		final String percent50 = String.format("%.2f%%", 50.0);
		assertGauge(HtmlUtil.getHpGauge(100, 50, 100, true), "Gauge_DF_Large_HP", percent50);
		assertGauge(HtmlUtil.getHpWarnGauge(100, 50, 100, false), "Gauge_DF_Large_HPWarn", "50");
		assertGauge(HtmlUtil.getHpFillGauge(100, 50, 100, true), "Gauge_DF_Large_HPFill", percent50);
		assertGauge(HtmlUtil.getMpGauge(100, 50, 100, false), "Gauge_DF_Large_MP", "100");
		assertGauge(HtmlUtil.getExpGauge(100, 50, 100, true), "Gauge_DF_Large_EXP", percent50);
		assertGauge(HtmlUtil.getFoodGauge(100, 50, 100, false), "Gauge_DF_Large_Food", "50");
		
		final String weight = HtmlUtil.getWeightGauge(100, 50, 100, true);
		assertTrue(weight.contains("Gauge_DF_Large_Weight_bg_Center"));
		assertTrue(weight.contains(percent50));
		
		final String weightLevel = HtmlUtil.getWeightGauge(100, 50, 100, false, 3);
		assertTrue(weightLevel.contains("Gauge_DF_Large_Weight_bg_Center3"));
		assertTrue(weightLevel.contains("Gauge_DF_Large_Weight_Center3"));
		assertTrue(weightLevel.contains(">50<"));
	}
	
	@Test
	void countPageNumberHandlesExactRemainderZeroAndNonPositivePageSize()
	{
		assertEquals(2, HtmlUtil.countPageNumber(20, 10));
		assertEquals(3, HtmlUtil.countPageNumber(21, 10));
		assertEquals(0, HtmlUtil.countPageNumber(0, 10));
		assertEquals(0, HtmlUtil.countPageNumber(10, 0));
		assertEquals(0, HtmlUtil.countPageNumber(10, -1));
	}
	
	private static void assertGauge(String html, String imageToken, String label)
	{
		assertTrue(html.contains(imageToken + "_bg_Center") || html.contains(imageToken + "_Bg_Center"), html);
		assertTrue(html.contains(imageToken + "_Center"), html);
		assertTrue(html.contains(label), html);
	}
	
	private static String percentTable(String percent)
	{
		return "<table cellpadding=0 cellspacing=2><tr><td>" + percent + "</td></tr></table>";
	}
	
	private static String fractionTable(long current, long max)
	{
		return "<table cellpadding=0 cellspacing=0><tr><td width=45 align=right>" + current + "</td><td width=10 align=center>/</td><td width=45>" + max + "</td></tr></table>";
	}
	
	private static String gaugeHtml(String background, String image, long fillWidth, String labelHtml)
	{
		return "<table width=100 cellpadding=0 cellspacing=0>"
			+ "<tr><td background=\"" + background + "\">"
			+ "<img src=\"" + image + "\" width=" + fillWidth + " height=17>"
			+ "</td></tr>"
			+ "<tr><td align=center>"
			+ "<table cellpadding=0 cellspacing=-13>"
			+ "<tr><td>" + labelHtml + "</td></tr>"
			+ "</table>"
			+ "</td></tr>"
			+ "</table>";
	}
}
