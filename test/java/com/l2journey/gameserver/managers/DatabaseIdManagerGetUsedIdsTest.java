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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.Config;
import com.l2journey.commons.database.DatabaseFactory;

/**
 * Unit tests for {@link DatabaseIdManager#getUsedIds()}.
 */
class DatabaseIdManagerGetUsedIdsTest
{
	private int previousFirst;
	private int previousLast;
	
	@BeforeEach
	void saveConfig()
	{
		previousFirst = Config.FIRST_OBJECT_ID;
		previousLast = Config.LAST_OBJECT_ID;
		Config.FIRST_OBJECT_ID = 100;
		Config.LAST_OBJECT_ID = 200;
	}
	
	@AfterEach
	void restoreConfig()
	{
		Config.FIRST_OBJECT_ID = previousFirst;
		Config.LAST_OBJECT_ID = previousLast;
	}
	
	@Test
	void getUsedIdsCollectsInRangeIdsAndIgnoresOutOfRange() throws Exception
	{
		final Connection connection = mock(Connection.class);
		when(connection.createStatement()).thenAnswer(_ ->
		{
			final Statement statement = mock(Statement.class);
			final ResultSet resultSet = mock(ResultSet.class);
			final AtomicInteger row = new AtomicInteger(0);
			when(resultSet.next()).thenAnswer(__ -> row.incrementAndGet() <= 2);
			when(resultSet.getInt(1)).thenAnswer(__ -> row.get() == 1 ? 150 : 999);
			when(statement.executeQuery(anyString())).thenReturn(resultSet);
			return statement;
		});
		
		try (MockedStatic<DatabaseFactory> database = mockStatic(DatabaseFactory.class))
		{
			database.when(DatabaseFactory::getConnection).thenReturn(connection);
			
			final Set<Integer> usedIds = DatabaseIdManager.getUsedIds();
			assertTrue(usedIds.contains(150));
			assertFalse(usedIds.contains(999));
			assertEquals(1, usedIds.size());
		}
	}
}
