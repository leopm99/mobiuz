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
package com.l2journey.gameserver.model.olympiad;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.EventsConfig;
import com.l2journey.commons.database.DatabaseFactory;

/**
 * Unit tests for Olympiad queries that bind {@link EventsConfig#OLYMPIAD_MIN_MATCHES}.
 */
class OlympiadMinMatchesQueryTest
{
	private static final int MIN_MATCHES = 17;
	
	private boolean savedEnabled;
	private int savedMinMatches;
	private boolean savedShowMonthly;
	
	@BeforeAll
	static void loadEventsConfigDefaults()
	{
		EventsConfig.load();
	}
	
	@BeforeEach
	void saveAndDisableOlympiad()
	{
		savedEnabled = EventsConfig.OLYMPIAD_ENABLED;
		savedMinMatches = EventsConfig.OLYMPIAD_MIN_MATCHES;
		savedShowMonthly = EventsConfig.OLYMPIAD_SHOW_MONTHLY_WINNERS;
		EventsConfig.OLYMPIAD_ENABLED = false;
		EventsConfig.OLYMPIAD_MIN_MATCHES = MIN_MATCHES;
		EventsConfig.OLYMPIAD_SHOW_MONTHLY_WINNERS = true;
	}
	
	@AfterEach
	void restoreEventsConfig()
	{
		EventsConfig.OLYMPIAD_ENABLED = savedEnabled;
		EventsConfig.OLYMPIAD_MIN_MATCHES = savedMinMatches;
		EventsConfig.OLYMPIAD_SHOW_MONTHLY_WINNERS = savedShowMonthly;
	}
	
	@Test
	void loadNoblesRankBindsMinMatches() throws Exception
	{
		final Connection connection = mock(Connection.class);
		final Map<String, PreparedStatement> statements = new LinkedHashMap<>();
		when(connection.prepareStatement(anyString())).thenAnswer(invocation ->
		{
			final String sql = invocation.getArgument(0);
			final PreparedStatement statement = mock(PreparedStatement.class);
			final ResultSet resultSet = mock(ResultSet.class);
			when(resultSet.next()).thenReturn(false);
			when(statement.executeQuery()).thenReturn(resultSet);
			statements.put(sql, statement);
			return statement;
		});
		
		try (MockedStatic<DatabaseFactory> database = mockStatic(DatabaseFactory.class))
		{
			database.when(DatabaseFactory::getConnection).thenReturn(connection);
			
			final Olympiad olympiad = new Olympiad();
			olympiad.loadNoblesRank();
			
			final PreparedStatement statement = statements.entrySet().stream()
				.filter(entry -> entry.getKey().contains("competitions_done >= ?"))
				.map(Map.Entry::getValue)
				.findFirst()
				.orElseThrow();
			verify(statement).setInt(1, MIN_MATCHES);
			verify(statement).executeQuery();
		}
	}
	
	@Test
	void getClassLeaderBoardBindsClassIdAndMinMatches() throws Exception
	{
		final Connection connection = mock(Connection.class);
		final PreparedStatement statement = mock(PreparedStatement.class);
		final ResultSet resultSet = mock(ResultSet.class);
		when(resultSet.next()).thenReturn(false);
		when(statement.executeQuery()).thenReturn(resultSet);
		when(connection.prepareStatement(contains("competitions_done >= ?"))).thenReturn(statement);
		
		try (MockedStatic<DatabaseFactory> database = mockStatic(DatabaseFactory.class))
		{
			database.when(DatabaseFactory::getConnection).thenReturn(connection);
			
			final Olympiad olympiad = new Olympiad();
			olympiad.getClassLeaderBoard(88);
			
			verify(statement).setInt(1, 88);
			verify(statement).setInt(2, MIN_MATCHES);
			verify(statement).executeQuery();
		}
	}
	
	@Test
	void sortHerosToBeBindsMinMatchesOnceBeforeClassLoop() throws Exception
	{
		final Connection connection = mock(Connection.class);
		final PreparedStatement statement = mock(PreparedStatement.class);
		final ResultSet resultSet = mock(ResultSet.class);
		when(resultSet.next()).thenReturn(false);
		when(statement.executeQuery()).thenReturn(resultSet);
		when(connection.prepareStatement(contains("competitions_won > 0"))).thenReturn(statement);
		
		try (MockedStatic<DatabaseFactory> database = mockStatic(DatabaseFactory.class))
		{
			database.when(DatabaseFactory::getConnection).thenReturn(connection);
			
			final Olympiad olympiad = new Olympiad();
			olympiad._period = 1;
			olympiad.sortHerosToBe();
			
			verify(statement).setInt(2, MIN_MATCHES);
			verify(statement, atLeastOnce()).setInt(1, 88);
			verify(statement, atLeastOnce()).executeQuery();
		}
	}
	
	@Test
	void getClassLeaderBoardBindsCurrentSoulhoundQuery() throws Exception
	{
		EventsConfig.OLYMPIAD_SHOW_MONTHLY_WINNERS = false;
		
		final Connection connection = mock(Connection.class);
		final PreparedStatement statement = mock(PreparedStatement.class);
		final ResultSet resultSet = mock(ResultSet.class);
		when(resultSet.next()).thenReturn(false);
		when(statement.executeQuery()).thenReturn(resultSet);
		when(connection.prepareStatement(contains("competitions_done >= ?"))).thenReturn(statement);
		
		try (MockedStatic<DatabaseFactory> database = mockStatic(DatabaseFactory.class))
		{
			database.when(DatabaseFactory::getConnection).thenReturn(connection);
			
			final Olympiad olympiad = new Olympiad();
			olympiad.getClassLeaderBoard(132);
			
			verify(statement).setInt(1, 132);
			verify(statement).setInt(2, MIN_MATCHES);
			verify(statement).executeQuery();
		}
	}
}
