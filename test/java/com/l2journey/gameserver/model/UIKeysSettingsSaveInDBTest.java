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
package com.l2journey.gameserver.model;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.data.xml.UIData;

/**
 * Unit tests for {@link UIKeysSettings#saveInDB()}.
 */
class UIKeysSettingsSaveInDBTest
{
	private static final int PLAYER_ID = 1001;
	private static final String CATEGORIES_SQL = "REPLACE INTO character_ui_categories (`charId`, `catId`, `order`, `cmdId`) VALUES (?,?,?,?)";
	private static final String ACTIONS_SQL = "REPLACE INTO character_ui_actions (`charId`, `cat`, `order`, `cmd`, `key`, `tgKey1`, `tgKey2`, `show`) VALUES (?,?,?,?,?,?,?,?)";
	
	@Test
	void saveInDBBatchesCategoryAndActionRows() throws Exception
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
		
		final UIData uiData = mock(UIData.class);
		when(uiData.getCategories()).thenReturn(new HashMap<>());
		when(uiData.getKeys()).thenReturn(new HashMap<>());
		
		try (MockedStatic<DatabaseFactory> database = mockStatic(DatabaseFactory.class);
			MockedStatic<UIData> uiDataStatic = mockStatic(UIData.class))
		{
			database.when(DatabaseFactory::getConnection).thenReturn(connection);
			uiDataStatic.when(UIData::getInstance).thenReturn(uiData);
			
			final UIKeysSettings settings = new UIKeysSettings(PLAYER_ID);
			final Map<Integer, List<Integer>> categories = new HashMap<>();
			categories.put(1, List.of(10, 11));
			final Map<Integer, List<ActionKey>> keys = new HashMap<>();
			keys.put(1, List.of(new ActionKey(1, 20, 30, 40, 50, 1)));
			settings.storeAll(categories, keys);
			settings.saveInDB();
			
			final PreparedStatement categoriesStatement = statements.get(CATEGORIES_SQL);
			verify(categoriesStatement, times(2)).setInt(1, PLAYER_ID);
			verify(categoriesStatement, times(2)).setInt(2, 1);
			verify(categoriesStatement).setInt(3, 0);
			verify(categoriesStatement).setInt(4, 10);
			verify(categoriesStatement).setInt(3, 1);
			verify(categoriesStatement).setInt(4, 11);
			verify(categoriesStatement, times(2)).addBatch();
			verify(categoriesStatement).executeBatch();
			
			final PreparedStatement actionsStatement = statements.get(ACTIONS_SQL);
			verify(actionsStatement).setInt(1, PLAYER_ID);
			verify(actionsStatement).setInt(2, 1);
			verify(actionsStatement).setInt(3, 0);
			verify(actionsStatement).setInt(4, 20);
			verify(actionsStatement).setInt(5, 30);
			verify(actionsStatement).setInt(6, 40);
			verify(actionsStatement).setInt(7, 50);
			verify(actionsStatement).setInt(8, 1);
			verify(actionsStatement).addBatch();
			verify(actionsStatement).executeBatch();
			assertTrue(settings.isSaved());
		}
	}
	
	@Test
	void saveInDBSkipsEmptyMaps() throws Exception
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
		
		final UIData uiData = mock(UIData.class);
		when(uiData.getCategories()).thenReturn(new HashMap<>());
		when(uiData.getKeys()).thenReturn(new HashMap<>());
		
		try (MockedStatic<DatabaseFactory> database = mockStatic(DatabaseFactory.class);
			MockedStatic<UIData> uiDataStatic = mockStatic(UIData.class))
		{
			database.when(DatabaseFactory::getConnection).thenReturn(connection);
			uiDataStatic.when(UIData::getInstance).thenReturn(uiData);
			
			final UIKeysSettings settings = new UIKeysSettings(PLAYER_ID);
			settings.storeAll(Collections.emptyMap(), Collections.emptyMap());
			settings.saveInDB();
			
			verify(connection, never()).prepareStatement(contains("REPLACE INTO character_ui_categories"));
			verify(connection, never()).prepareStatement(contains("REPLACE INTO character_ui_actions"));
			assertTrue(settings.isSaved());
		}
	}
	
	@Test
	void saveInDBMarksSavedWhenBatchThrows() throws Exception
	{
		final Connection connection = mock(Connection.class);
		final PreparedStatement loadStatement = mock(PreparedStatement.class);
		final ResultSet loadResult = mock(ResultSet.class);
		when(loadResult.next()).thenReturn(false);
		when(loadStatement.executeQuery()).thenReturn(loadResult);
		when(connection.prepareStatement(contains("SELECT"))).thenReturn(loadStatement);
		
		final PreparedStatement categoriesStatement = mock(PreparedStatement.class);
		doThrow(new SQLException("batch failed")).when(categoriesStatement).executeBatch();
		when(connection.prepareStatement(CATEGORIES_SQL)).thenReturn(categoriesStatement);
		
		final UIData uiData = mock(UIData.class);
		when(uiData.getCategories()).thenReturn(new HashMap<>());
		when(uiData.getKeys()).thenReturn(new HashMap<>());
		
		try (MockedStatic<DatabaseFactory> database = mockStatic(DatabaseFactory.class);
			MockedStatic<UIData> uiDataStatic = mockStatic(UIData.class))
		{
			database.when(DatabaseFactory::getConnection).thenReturn(connection);
			uiDataStatic.when(UIData::getInstance).thenReturn(uiData);
			
			final UIKeysSettings settings = new UIKeysSettings(PLAYER_ID);
			final Map<Integer, List<Integer>> categories = new HashMap<>();
			categories.put(1, List.of(10));
			settings.storeAll(categories, Collections.emptyMap());
			settings.saveInDB();
			
			assertTrue(settings.isSaved());
		}
	}
	
	@Test
	void saveInDBMarksSavedWhenActionsBatchThrows() throws Exception
	{
		final Connection connection = mock(Connection.class);
		final PreparedStatement loadStatement = mock(PreparedStatement.class);
		final ResultSet loadResult = mock(ResultSet.class);
		when(loadResult.next()).thenReturn(false);
		when(loadStatement.executeQuery()).thenReturn(loadResult);
		when(connection.prepareStatement(contains("SELECT"))).thenReturn(loadStatement);
		
		final PreparedStatement actionsStatement = mock(PreparedStatement.class);
		doThrow(new SQLException("actions batch failed")).when(actionsStatement).executeBatch();
		when(connection.prepareStatement(ACTIONS_SQL)).thenReturn(actionsStatement);
		
		final UIData uiData = mock(UIData.class);
		when(uiData.getCategories()).thenReturn(new HashMap<>());
		when(uiData.getKeys()).thenReturn(new HashMap<>());
		
		try (MockedStatic<DatabaseFactory> database = mockStatic(DatabaseFactory.class);
			MockedStatic<UIData> uiDataStatic = mockStatic(UIData.class))
		{
			database.when(DatabaseFactory::getConnection).thenReturn(connection);
			uiDataStatic.when(UIData::getInstance).thenReturn(uiData);
			
			final UIKeysSettings settings = new UIKeysSettings(PLAYER_ID);
			final Map<Integer, List<ActionKey>> keys = new HashMap<>();
			keys.put(1, List.of(new ActionKey(1, 20, 30, 40, 50, 1)));
			settings.storeAll(Collections.emptyMap(), keys);
			settings.saveInDB();
			
			assertTrue(settings.isSaved());
		}
	}
	
	@Test
	void saveInDBReturnsEarlyWhenAlreadySaved() throws Exception
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
		
		final UIData uiData = mock(UIData.class);
		when(uiData.getCategories()).thenReturn(new HashMap<>());
		when(uiData.getKeys()).thenReturn(new HashMap<>());
		
		try (MockedStatic<DatabaseFactory> database = mockStatic(DatabaseFactory.class);
			MockedStatic<UIData> uiDataStatic = mockStatic(UIData.class))
		{
			database.when(DatabaseFactory::getConnection).thenReturn(connection);
			uiDataStatic.when(UIData::getInstance).thenReturn(uiData);
			
			final UIKeysSettings settings = new UIKeysSettings(PLAYER_ID);
			settings.saveInDB();
			
			verify(connection, never()).prepareStatement(contains("REPLACE INTO"));
			assertTrue(settings.isSaved());
		}
	}
	
	@Test
	void saveInDBSkipsMapsWithOnlyEmptyOrNullLists() throws Exception
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
		
		final UIData uiData = mock(UIData.class);
		when(uiData.getCategories()).thenReturn(new HashMap<>());
		when(uiData.getKeys()).thenReturn(new HashMap<>());
		
		try (MockedStatic<DatabaseFactory> database = mockStatic(DatabaseFactory.class);
			MockedStatic<UIData> uiDataStatic = mockStatic(UIData.class))
		{
			database.when(DatabaseFactory::getConnection).thenReturn(connection);
			uiDataStatic.when(UIData::getInstance).thenReturn(uiData);
			
			final UIKeysSettings settings = new UIKeysSettings(PLAYER_ID);
			final Map<Integer, List<Integer>> categories = new HashMap<>();
			categories.put(1, Collections.emptyList());
			categories.put(2, null);
			final Map<Integer, List<ActionKey>> keys = new HashMap<>();
			keys.put(1, Collections.emptyList());
			keys.put(2, null);
			settings.storeAll(categories, keys);
			settings.saveInDB();
			
			verify(connection, never()).prepareStatement(contains("REPLACE INTO"));
			assertTrue(settings.isSaved());
		}
	}
}
