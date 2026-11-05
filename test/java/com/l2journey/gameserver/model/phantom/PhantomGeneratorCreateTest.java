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
package com.l2journey.gameserver.model.phantom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import com.l2journey.PhantomConfig;
import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.model.actor.Player;

/**
 * Covers PhantomGenerator createInGame success and GM error messaging.
 */
class PhantomGeneratorCreateTest
{
	private static final int FIXED_CHAR_ID = 4242;
	
	@TempDir
	Path _temp;
	
	@Test
	void createInGamePersistsRolledPhantomAndNotifiesGm() throws Exception
	{
		final Connection connection = mock(Connection.class);
		final PreparedStatement statement = mock(PreparedStatement.class);
		final Player gm = mock(Player.class);
		final Path xmlPath = _temp.resolve("PhantomPlayers.xml");
		when(connection.prepareStatement(anyString())).thenReturn(statement);
		
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(anyInt())).thenReturn(0);
			rnd.when(() -> Rnd.pick(any(String[].class))).thenAnswer(invocation ->
			{
				final String[] values = invocation.getArgument(0);
				return values[0];
			});
			rnd.when(() -> Rnd.pick(any(int[].class))).thenAnswer(invocation ->
			{
				final int[] values = invocation.getArgument(0);
				return values[0];
			});
			PhantomGenerator.createInGame(1, gm, xmlPath, () -> connection, () -> FIXED_CHAR_ID);
			
			assertTrue(PhantomConfig.PHANTOM_IDS.contains(FIXED_CHAR_ID));
			verify(statement).setInt(1, FIXED_CHAR_ID);
			verify(statement).setString(3, "Alas");
			verify(statement).setInt(11, 0);
			verify(statement).setInt(12, 0);
			verify(statement).setInt(13, 0);
			verify(statement).setInt(14, 0);
			verify(statement).setInt(18, 0);
			verify(statement).setInt(19, 10);
			verify(statement).setInt(20, 10);
			verify(statement).addBatch();
			verify(statement).executeBatch();
			verify(gm).sendMessage("¡Éxito! Se han creado 1 Phantoms Magos/Chamanes nuevos.");
			assertTrue(Files.readString(xmlPath).contains("charId=\"" + FIXED_CHAR_ID + "\""));
		}
		finally
		{
			PhantomConfig.PHANTOM_IDS.remove(Integer.valueOf(FIXED_CHAR_ID));
		}
	}
	
	@Test
	void rollPhantomAttributesMapsClassToRace()
	{
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.pick(any(String[].class))).thenReturn("X");
			rnd.when(() -> Rnd.get(anyInt())).thenReturn(0);
			rnd.when(() -> Rnd.pick(any(int[].class))).thenReturn(10, 25, 38, 49);
			
			assertEquals(0, PhantomGenerator.rollPhantomAttributes().race());
			assertEquals(1, PhantomGenerator.rollPhantomAttributes().race());
			assertEquals(2, PhantomGenerator.rollPhantomAttributes().race());
			assertEquals(3, PhantomGenerator.rollPhantomAttributes().race());
		}
	}
	
	@Test
	void createInGameNotifiesGmWhenConnectionFails()
	{
		final Logger logger = Logger.getLogger(PhantomGenerator.class.getName());
		final List<LogRecord> records = new ArrayList<>();
		final Handler handler = recordingHandler(records);
		logger.addHandler(handler);
		final Level previous = logger.getLevel();
		logger.setLevel(Level.ALL);
		final Player gm = mock(Player.class);
		final RuntimeException failure = new RuntimeException("unavailable");
		try
		{
			PhantomGenerator.createInGame(1, gm, _temp.resolve("unused.xml"), () ->
			{
				throw failure;
			}, () -> FIXED_CHAR_ID);
			verify(gm).sendMessage("Error creando phantoms: unavailable");
			assertTrue(records.stream().anyMatch(record -> record.getLevel() == Level.SEVERE));
		}
		finally
		{
			logger.removeHandler(handler);
			logger.setLevel(previous);
		}
	}
	
	private static Handler recordingHandler(List<LogRecord> records)
	{
		return new Handler()
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
	}
}
