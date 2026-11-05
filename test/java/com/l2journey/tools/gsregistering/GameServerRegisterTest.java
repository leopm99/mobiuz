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
package com.l2journey.tools.gsregistering;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.Config;
import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.commons.enums.ServerMode;
import com.l2journey.loginserver.GameServerTable;

/**
 * Exit-path coverage for the game-server register menu loop (java:S2189).
 */
class GameServerRegisterTest
{
	@Test
	void consoleUIReturnsOnExitChoice() throws Exception
	{
		final GameServerTable table = mock(GameServerTable.class);
		when(table.getServerNames()).thenReturn(Map.of(1, "Bartz"));
		
		final InputStream originalIn = System.in;
		try (MockedStatic<Config> config = mockStatic(Config.class);
			MockedStatic<DatabaseFactory> database = mockStatic(DatabaseFactory.class);
			MockedStatic<GameServerTable> gameServers = mockStatic(GameServerTable.class))
		{
			config.when(() -> Config.load(any(ServerMode.class))).thenAnswer(_ -> null);
			database.when(DatabaseFactory::init).thenAnswer(_ -> null);
			gameServers.when(GameServerTable::getInstance).thenReturn(table);
			System.setIn(new ByteArrayInputStream("5\n".getBytes(StandardCharsets.UTF_8)));
			
			final GameServerRegister register = new GameServerRegister();
			assertDoesNotThrow(register::consoleUI);
		}
		finally
		{
			System.setIn(originalIn);
		}
	}
}
