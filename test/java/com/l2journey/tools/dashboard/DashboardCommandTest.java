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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Covers Dashboard absolute-path process helpers without opening the Swing UI.
 */
class DashboardCommandTest
{
	@TempDir
	Path _temp;
	
	@Test
	void startServerProcessUsesAbsoluteCmdOrFailsCleanly() throws Exception
	{
		Process process = null;
		try
		{
			process = Dashboard.startServerProcess(_temp, "missing-start.bat");
			assertTrue(process.waitFor(10, TimeUnit.SECONDS));
		}
		catch (IOException _)
		{
			// Linux CI has no System32\\cmd.exe; the absolute-path lookup fails before start.
		}
		finally
		{
			if (process != null)
			{
				process.destroyForcibly();
			}
		}
	}
	
	@Test
	void startPowershellUsesAbsoluteBinaryOrFailsCleanly() throws Exception
	{
		runPowershell(true);
		runPowershell(false);
	}
	
	private static void runPowershell(boolean redirectErrorStream) throws Exception
	{
		Process process = null;
		try
		{
			process = Dashboard.startPowershell(redirectErrorStream, "Write-Output 1");
			assertTrue(process.waitFor(10, TimeUnit.SECONDS));
		}
		catch (IOException _)
		{
			// Linux CI has no Windows PowerShell binary under System32.
		}
		finally
		{
			if (process != null)
			{
				process.destroyForcibly();
			}
		}
	}
	
	@Test
	void killTreeIgnoresMissingOrUnreachablePid()
	{
		assertDoesNotThrow(() -> Dashboard.killTree(2_147_483_646L));
	}
	
	@Test
	void heapUsedReturnsNonNegativeForCurrentProcess()
	{
		assertTrue(Dashboard.heapUsed(ProcessHandle.current().pid()) >= 0);
	}
}
