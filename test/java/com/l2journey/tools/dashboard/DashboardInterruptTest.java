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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

/**
 * Covers interrupt restoration in Dashboard process waits (java:S2142).
 */
class DashboardInterruptTest
{
	@Test
	void awaitProcessRestoresInterruptFlag()
	{
		try
		{
			Dashboard.awaitProcess(() ->
			{
				throw new InterruptedException("test");
			});
			assertTrue(Thread.currentThread().isInterrupted());
		}
		finally
		{
			Thread.interrupted();
		}
	}
	
	@Test
	void awaitProcessIgnoresNonInterruptFailures()
	{
		assertDoesNotThrow(() -> Dashboard.awaitProcess(() ->
		{
			throw new IOException("missing taskkill");
		}));
	}
	
	@Test
	void queryMetricsRestoresInterruptFlag()
	{
		try
		{
			Dashboard.queryMetrics(1L, () ->
			{
				throw new InterruptedException("test");
			});
			assertTrue(Thread.currentThread().isInterrupted());
		}
		finally
		{
			Thread.interrupted();
		}
	}
	
	@Test
	void queryMetricsIgnoresNonInterruptFailures()
	{
		assertDoesNotThrow(() ->
		{
			Dashboard.queryMetrics(1L, () ->
			{
				throw new IOException("powershell unavailable");
			});
		});
	}
	
	@Test
	void queryMetricsReadsThreadCount()
	{
		assertDoesNotThrow(() ->
		{
			Dashboard.queryMetrics(1L, () -> 4);
		});
	}
	
	@Test
	void heapUsedRestoresInterruptFlag()
	{
		try
		{
			assertEquals(0L, Dashboard.heapUsed(() ->
			{
				throw new InterruptedException("test");
			}));
			assertTrue(Thread.currentThread().isInterrupted());
		}
		finally
		{
			Thread.interrupted();
		}
	}
	
	@Test
	void heapUsedIgnoresNonInterruptFailures()
	{
		assertEquals(0L, Dashboard.heapUsed(() ->
		{
			throw new IOException("jcmd unavailable");
		}));
	}
	
	@Test
	void heapUsedParsesMegabytes()
	{
		assertEquals(104857600L, Dashboard.heapUsed(() -> "used 100 M"));
	}
	
	@Test
	void heapUsedParsesGigabytesAndKilobytes()
	{
		assertEquals(2147483648L, Dashboard.heapUsed(() -> "used 2 G"));
		assertEquals(2048L, Dashboard.heapUsed(() -> "used 2 K"));
	}
}
