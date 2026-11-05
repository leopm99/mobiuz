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
package com.l2journey.gameserver.model.actor.instance;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

/**
 * Covers UCManager severe-log helper without constructing a live NPC instance.
 */
class UCManagerLogTest
{
	@Test
	void logRegisterFailureEmitsSevere()
	{
		final Logger logger = Logger.getLogger(UCManager.class.getName());
		final List<LogRecord> records = new ArrayList<>();
		final Handler handler = new Handler()
		{
			@Override
			public void publish(LogRecord record)
			{
				records.add(record);
			}
			
			@Override
			public void flush()
			{
			}
			
			@Override
			public void close()
			{
			}
		};
		logger.addHandler(handler);
		final Level previous = logger.getLevel();
		logger.setLevel(Level.ALL);
		try
		{
			UCManager.logRegisterFailure(new IllegalStateException("register failed"));
			assertTrue(records.stream().anyMatch(record -> record.getLevel() == Level.SEVERE));
		}
		finally
		{
			logger.removeHandler(handler);
			logger.setLevel(previous);
		}
	}
}
