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
package com.l2journey.gameserver.data.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.model.actor.Player;

/**
 * Unit tests for {@link UserVariables#unsetVar(String)}.
 */
class UserVariablesUnsetVarTest
{
	private static final int OBJECT_ID = 4242;
	
	@Test
	void unsetVarDeletesByObjectIdAndName() throws Exception
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
		
		final Player player = mock(Player.class);
		when(player.getObjectId()).thenReturn(OBJECT_ID);
		
		try (MockedStatic<DatabaseFactory> database = mockStatic(DatabaseFactory.class))
		{
			database.when(DatabaseFactory::getConnection).thenReturn(connection);
			
			final UserVariables variables = new UserVariables(player);
			variables.getVars().put("quest_flag", "1");
			variables.unsetVar("quest_flag");
			
			assertNull(variables.getVars().get("quest_flag"));
			
			final PreparedStatement delete = statements.entrySet().stream()
				.filter(entry -> entry.getKey().contains("DELETE") && entry.getKey().contains("name"))
				.map(Map.Entry::getValue)
				.findFirst()
				.orElseThrow();
			verify(delete).setInt(1, OBJECT_ID);
			verify(delete).setString(2, "quest_flag");
			verify(delete).execute();
		}
	}
	
	@Test
	void unsetVarSkipsEmptyName() throws Exception
	{
		final Connection connection = mock(Connection.class);
		when(connection.prepareStatement(anyString())).thenAnswer(invocation ->
		{
			final PreparedStatement statement = mock(PreparedStatement.class);
			final ResultSet resultSet = mock(ResultSet.class);
			when(resultSet.next()).thenReturn(false);
			when(statement.executeQuery()).thenReturn(resultSet);
			return statement;
		});
		
		final Player player = mock(Player.class);
		when(player.getObjectId()).thenReturn(OBJECT_ID);
		
		try (MockedStatic<DatabaseFactory> database = mockStatic(DatabaseFactory.class))
		{
			database.when(DatabaseFactory::getConnection).thenReturn(connection);
			
			final UserVariables variables = new UserVariables(player);
			variables.unsetVar("");
			variables.unsetVar(null);
		}
		
		verify(connection, never()).prepareStatement(contains("DELETE"));
	}
	
	@Test
	void setVarStillBindsThreeParameters() throws Exception
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
		
		final Player player = mock(Player.class);
		when(player.getObjectId()).thenReturn(OBJECT_ID);
		
		try (MockedStatic<DatabaseFactory> database = mockStatic(DatabaseFactory.class))
		{
			database.when(DatabaseFactory::getConnection).thenReturn(connection);
			
			final UserVariables variables = new UserVariables(player);
			variables.setVar("flag", "yes");
			
			assertEquals("yes", variables.getVars().get("flag"));
			
			final PreparedStatement replace = statements.entrySet().stream()
				.filter(entry -> entry.getKey().contains("REPLACE"))
				.map(Map.Entry::getValue)
				.findFirst()
				.orElseThrow();
			verify(replace).setInt(1, OBJECT_ID);
			verify(replace).setString(2, "flag");
			verify(replace).setString(3, "yes");
		}
	}
}
