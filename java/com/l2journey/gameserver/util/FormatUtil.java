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
package com.l2journey.gameserver.util;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utility class for formatting numerical values in a human-readable format.
 * @author Mobius
 */
public class FormatUtil
{
	private static final NumberFormat ADENA_FORMATTER = NumberFormat.getIntegerInstance(Locale.ENGLISH);
	private FormatUtil()
	{
	}

	/**
	 * Formats the specified amount using comma-separated digit grouping.<br>
	 * For example, an input of {@code 123456789} will be formatted as {@code 123,456,789}.
	 * @param amount the numerical amount to format
	 * @return the formatted string representation of the amount with comma separators
	 */
	public static String formatAdena(long amount)
	{
		synchronized (ADENA_FORMATTER)
		{
			return ADENA_FORMATTER.format(amount);
		}
	}
}
