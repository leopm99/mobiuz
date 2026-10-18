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
package com.l2journey.gameserver.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ScheduledFuture;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.commons.threads.ThreadPool;

/**
 * Unit tests for {@link FishingChampionshipManager#restoreData()}.
 */
class FishingChampionshipRestoreDataTest
{
	@Test
	void restoreDataLoadsTemporaryAndWinningFishers() throws Exception
	{
		final Connection connection = mock(Connection.class);
		final PreparedStatement statement = mock(PreparedStatement.class);
		final ResultSet resultSet = mock(ResultSet.class);
		when(connection.prepareStatement(anyString())).thenReturn(statement);
		when(statement.executeQuery()).thenReturn(resultSet);
		when(resultSet.next()).thenReturn(true, true, false, true, true, false);
		when(resultSet.getInt("rewarded")).thenReturn(0, 2, 0, 2);
		when(resultSet.getString("player_name")).thenReturn("tmp", "win", "tmp", "win");
		when(resultSet.getDouble("fish_length")).thenReturn(10.5, 20.5, 10.5, 20.5);
		
		final GlobalVariablesManager variables = mock(GlobalVariablesManager.class);
		when(variables.getLong(eq("fishChampionshipEnd"), eq(0L))).thenReturn(System.currentTimeMillis() + 3_600_000L);
		
		try (MockedStatic<DatabaseFactory> database = mockStatic(DatabaseFactory.class);
			MockedStatic<GlobalVariablesManager> globals = mockStatic(GlobalVariablesManager.class);
			MockedStatic<ThreadPool> threads = mockStatic(ThreadPool.class))
		{
			database.when(DatabaseFactory::getConnection).thenReturn(connection);
			globals.when(GlobalVariablesManager::getInstance).thenReturn(variables);
			threads.when(() -> ThreadPool.schedule(any(Runnable.class), anyLong())).thenReturn(mock(ScheduledFuture.class));
			
			final FishingChampionshipManager manager = FishingChampionshipManager.getInstance();
			manager._tmpPlayers.clear();
			manager._winPlayers.clear();
			manager.restoreData();
			
			assertEquals(1, manager._tmpPlayers.size());
			assertEquals(1, manager._winPlayers.size());
		}
	}
}
