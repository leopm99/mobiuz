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
package com.l2journey.commons.util;

import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helpers for JUL call sites that need a throwable plus a lazily built message.
 * Stock {@link Logger} exposes {@code warning(Supplier)} etc., but not
 * {@code warning(Supplier, Throwable)}.
 */
public final class LogUtil
{
	private LogUtil()
	{
	}
	
	public static void severe(Logger logger, Throwable thrown, Supplier<String> message)
	{
		logger.log(Level.SEVERE, thrown, message);
	}
	
	public static void warning(Logger logger, Throwable thrown, Supplier<String> message)
	{
		logger.log(Level.WARNING, thrown, message);
	}
	
	public static void info(Logger logger, Throwable thrown, Supplier<String> message)
	{
		logger.log(Level.INFO, thrown, message);
	}
	
	public static void config(Logger logger, Throwable thrown, Supplier<String> message)
	{
		logger.log(Level.CONFIG, thrown, message);
	}
	
	public static void fine(Logger logger, Throwable thrown, Supplier<String> message)
	{
		logger.log(Level.FINE, thrown, message);
	}
	
	public static void finer(Logger logger, Throwable thrown, Supplier<String> message)
	{
		logger.log(Level.FINER, thrown, message);
	}
	
	public static void finest(Logger logger, Throwable thrown, Supplier<String> message)
	{
		logger.log(Level.FINEST, thrown, message);
	}
}
