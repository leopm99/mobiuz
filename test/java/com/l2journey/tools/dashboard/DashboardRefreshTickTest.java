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
package com.l2journey.tools.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

/**
 * Atomic refresh-tick increments for {@link Dashboard} (java:S3078).
 */
class DashboardRefreshTickTest
{
	@Test
	void refreshIncrementsTickAndCoversBothModuloBranches() throws Exception
	{
		final Dashboard dashboard = new Dashboard();
		final Field queryingStatus = Dashboard.class.getDeclaredField("_queryingStatus");
		queryingStatus.setAccessible(true);
		((AtomicBoolean) queryingStatus.get(dashboard)).set(true);
		
		final Field queryingMetrics = Dashboard.class.getDeclaredField("_queryingMetrics");
		queryingMetrics.setAccessible(true);
		((AtomicBoolean) queryingMetrics.get(dashboard)).set(true);
		
		final Method refresh = Dashboard.class.getDeclaredMethod("refresh");
		refresh.setAccessible(true);
		refresh.invoke(dashboard);
		refresh.invoke(dashboard);
		refresh.invoke(dashboard);
		
		final Field refreshTick = Dashboard.class.getDeclaredField("_refreshTick");
		refreshTick.setAccessible(true);
		assertEquals(3, refreshTick.getInt(dashboard));
	}
}
