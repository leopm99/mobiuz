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
package com.l2journey.tools.accountmanager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.Config;
import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.commons.enums.ServerMode;

/**
 * Exit-path coverage for the account-manager menu loop (java:S2189).
 */
class SQLAccountManagerTest
{
	@BeforeEach
	void resetStaticFields() throws Exception
	{
		setStatic("_uname", "");
		setStatic("_pass", "");
		setStatic("_level", "");
		setStatic("_mode", "");
	}
	
	@Test
	void mainReturnsOnExitChoice() throws Exception
	{
		runMain("5\n");
	}
	
	@Test
	void mainReturnsAfterInvalidThenExitChoice() throws Exception
	{
		runMain("x\n5\n");
	}
	
	private static void runMain(String input) throws Exception
	{
		final InputStream originalIn = System.in;
		try (MockedStatic<Config> config = mockStatic(Config.class);
			MockedStatic<DatabaseFactory> database = mockStatic(DatabaseFactory.class))
		{
			config.when(() -> Config.load(any(ServerMode.class))).thenAnswer(_ -> null);
			database.when(DatabaseFactory::init).thenAnswer(_ -> null);
			System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
			assertDoesNotThrow(() -> SQLAccountManager.main(new String[0]));
		}
		finally
		{
			System.setIn(originalIn);
		}
	}
	
	private static void setStatic(String name, String value) throws Exception
	{
		final Field field = SQLAccountManager.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(null, value);
	}
}
