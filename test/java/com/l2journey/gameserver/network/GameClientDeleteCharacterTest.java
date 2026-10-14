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
package com.l2journey.gameserver.network;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.data.sql.CharInfoTable;

/**
 * Unit tests for {@link GameClient#deleteCharByObjId(int)} summon cleanup.
 */
class GameClientDeleteCharacterTest
{
	private static final String DELETE_SUMMONS = "DELETE FROM character_summons WHERE ownerId=?";
	private static final int OBJECT_ID = 268470123;
	
	@Test
	void deleteCharByObjIdRemovesCharacterSummons() throws Exception
	{
		Connection connection = mock(Connection.class);
		Map<String, PreparedStatement> statements = new HashMap<>();
		when(connection.prepareStatement(anyString())).thenAnswer(invocation ->
		{
			final String sql = invocation.getArgument(0);
			final PreparedStatement statement = mock(PreparedStatement.class);
			statements.put(sql, statement);
			return statement;
		});
		
		CharInfoTable charInfoTable = mock(CharInfoTable.class);
		try (MockedStatic<CharInfoTable> charInfo = mockStatic(CharInfoTable.class);
			MockedStatic<DatabaseFactory> database = mockStatic(DatabaseFactory.class))
		{
			charInfo.when(CharInfoTable::getInstance).thenReturn(charInfoTable);
			database.when(DatabaseFactory::getConnection).thenReturn(connection);
			
			GameClient.deleteCharByObjId(OBJECT_ID);
		}
		
		PreparedStatement summonsDelete = statements.get(DELETE_SUMMONS);
		verify(connection).prepareStatement(DELETE_SUMMONS);
		verify(summonsDelete).setInt(1, OBJECT_ID);
		verify(summonsDelete).execute();
	}
	
	@Test
	void deleteCharByObjIdIgnoresNegativeObjectId() throws Exception
	{
		Connection connection = mock(Connection.class);
		CharInfoTable charInfoTable = mock(CharInfoTable.class);
		try (MockedStatic<CharInfoTable> charInfo = mockStatic(CharInfoTable.class);
			MockedStatic<DatabaseFactory> database = mockStatic(DatabaseFactory.class))
		{
			charInfo.when(CharInfoTable::getInstance).thenReturn(charInfoTable);
			database.when(DatabaseFactory::getConnection).thenReturn(connection);
			
			GameClient.deleteCharByObjId(-1);
		}
		
		verify(connection, never()).prepareStatement(anyString());
		verify(charInfoTable, never()).removeName(-1);
	}
}
