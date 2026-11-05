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
package com.l2journey.commons.threads;

/**
 * Interrupt-safe {@link Thread#sleep(long)}.
 * Callers must stop the wait sequence when this method returns {@code false}.
 */
public final class Pauses
{
	private Pauses()
	{
	}
	
	/**
	 * Sleeps for the given duration. On interrupt, restores the interrupt flag and returns {@code false}.
	 * @param millis duration in milliseconds
	 * @return {@code true} if the sleep completed, {@code false} if interrupted
	 */
	public static boolean sleep(long millis)
	{
		try
		{
			Thread.sleep(millis);
			return true;
		}
		catch (InterruptedException _)
		{
			Thread.currentThread().interrupt();
			return false;
		}
	}
}
