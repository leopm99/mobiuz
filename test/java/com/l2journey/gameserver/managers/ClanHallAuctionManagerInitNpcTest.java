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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.commons.threads.ThreadPool;

/**
 * Unit tests for {@link ClanHallAuctionManager#initNPC(int)}.
 */
class ClanHallAuctionManagerInitNpcTest
{
	private static final String INSERT_SQL = "INSERT INTO auction (id, sellerId, sellerName, sellerClanName, itemType, itemId, itemObjectId, itemName, itemQuantity, startingBid, currentBid, endDate) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
	
	@Test
	void initNpcInsertsSeedRowWithLongBinds() throws Exception
	{
		final Connection connection = mock(Connection.class);
		final Statement loadStatement = mock(Statement.class);
		final ResultSet loadResult = mock(ResultSet.class);
		when(connection.createStatement()).thenReturn(loadStatement);
		when(loadStatement.executeQuery(anyString())).thenReturn(loadResult);
		when(loadResult.next()).thenReturn(false);
		
		final Map<String, PreparedStatement> prepared = new LinkedHashMap<>();
		when(connection.prepareStatement(anyString())).thenAnswer(invocation ->
		{
			final String sql = invocation.getArgument(0);
			final PreparedStatement statement = mock(PreparedStatement.class);
			final ResultSet resultSet = mock(ResultSet.class);
			when(resultSet.next()).thenReturn(false);
			when(statement.executeQuery()).thenReturn(resultSet);
			prepared.put(sql, statement);
			return statement;
		});
		
		try (MockedStatic<DatabaseFactory> database = mockStatic(DatabaseFactory.class);
			MockedStatic<ThreadPool> threads = mockStatic(ThreadPool.class))
		{
			database.when(DatabaseFactory::getConnection).thenReturn(connection);
			threads.when(() -> ThreadPool.schedule(any(Runnable.class), anyLong())).thenReturn(mock(ScheduledFuture.class));
			
			final ClanHallAuctionManager manager = new ClanHallAuctionManager();
			manager.initNPC(22);
			
			final PreparedStatement insert = prepared.get(INSERT_SQL);
			verify(insert).setInt(1, 22);
			verify(insert).setInt(2, 0);
			verify(insert).setString(3, "NPC");
			verify(insert).setString(4, "NPC Clan");
			verify(insert).setString(5, "ClanHall");
			verify(insert).setInt(6, 22);
			verify(insert).setInt(7, 0);
			verify(insert).setString(8, "Moonstone Hall");
			verify(insert).setLong(9, 1L);
			verify(insert).setLong(10, 20000000L);
			verify(insert).setLong(11, 0L);
			verify(insert).setLong(12, 1073037600000L);
			verify(insert).executeUpdate();
		}
	}
	
	@Test
	void initNpcSkipsUnknownId() throws Exception
	{
		final Connection connection = mock(Connection.class);
		final Statement loadStatement = mock(Statement.class);
		final ResultSet loadResult = mock(ResultSet.class);
		when(connection.createStatement()).thenReturn(loadStatement);
		when(loadStatement.executeQuery(anyString())).thenReturn(loadResult);
		when(loadResult.next()).thenReturn(false);
		
		try (MockedStatic<DatabaseFactory> database = mockStatic(DatabaseFactory.class))
		{
			database.when(DatabaseFactory::getConnection).thenReturn(connection);
			
			final ClanHallAuctionManager manager = new ClanHallAuctionManager();
			manager.initNPC(9999);
			
			verify(connection, never()).prepareStatement(contains("INSERT INTO auction"));
		}
	}
	
	@Test
	void initNpcSwallowsInsertFailure() throws Exception
	{
		final Connection connection = mock(Connection.class);
		final Statement loadStatement = mock(Statement.class);
		final ResultSet loadResult = mock(ResultSet.class);
		when(connection.createStatement()).thenReturn(loadStatement);
		when(loadStatement.executeQuery(anyString())).thenReturn(loadResult);
		when(loadResult.next()).thenReturn(false);
		
		final PreparedStatement insert = mock(PreparedStatement.class);
		doThrow(new SQLException("insert failed")).when(insert).executeUpdate();
		when(connection.prepareStatement(INSERT_SQL)).thenReturn(insert);
		
		try (MockedStatic<DatabaseFactory> database = mockStatic(DatabaseFactory.class))
		{
			database.when(DatabaseFactory::getConnection).thenReturn(connection);
			
			final ClanHallAuctionManager manager = new ClanHallAuctionManager();
			manager.initNPC(22);
			
			verify(insert).executeUpdate();
		}
	}
}
