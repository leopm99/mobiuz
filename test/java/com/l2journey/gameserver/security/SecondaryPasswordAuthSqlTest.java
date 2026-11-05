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
package com.l2journey.gameserver.security;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.data.xml.SecondaryAuthData;
import com.l2journey.gameserver.network.GameClient;

/**
 * Regression tests for SecondaryPasswordAuth SQL constant names and bindings.
 */
class SecondaryPasswordAuthSqlTest
{
	private static final String SEC_AUTH_SELECT = "SELECT var, value FROM account_gsdata WHERE account_name=? AND var LIKE 'secauth_%'";
	private static final String SEC_AUTH_INSERT = "INSERT INTO account_gsdata VALUES (?, ?, ?)";
	private static final String SEC_AUTH_UPDATE = "UPDATE account_gsdata SET value=? WHERE account_name=? AND var=?";
	private static final String INSERT_ATTEMPT = "INSERT INTO account_gsdata VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE value=?";
	private static final String ACCOUNT = "testacct";
	private static final String INITIAL_PIN = "102938";
	private static final String CHANGED_PIN = "203948";
	
	@Test
	void selectInsertAndUpdateBindAccountName() throws Exception
	{
		final Connection connection = mock(Connection.class);
		final PreparedStatement selectStatement = mock(PreparedStatement.class);
		final PreparedStatement insertStatement = mock(PreparedStatement.class);
		final PreparedStatement attemptStatement = mock(PreparedStatement.class);
		final PreparedStatement updateStatement = mock(PreparedStatement.class);
		final ResultSet selectResult = mock(ResultSet.class);
		final GameClient client = mock(GameClient.class);
		final SecondaryAuthData authData = mock(SecondaryAuthData.class);
		
		when(client.getAccountName()).thenReturn(ACCOUNT);
		when(authData.isForbiddenPassword(anyString())).thenReturn(false);
		when(authData.getMaxAttempts()).thenReturn(5);
		when(connection.prepareStatement(eq(SEC_AUTH_SELECT))).thenReturn(selectStatement);
		when(connection.prepareStatement(eq(SEC_AUTH_INSERT))).thenReturn(insertStatement);
		when(connection.prepareStatement(eq(INSERT_ATTEMPT))).thenReturn(attemptStatement);
		when(connection.prepareStatement(eq(SEC_AUTH_UPDATE))).thenReturn(updateStatement);
		when(selectStatement.executeQuery()).thenReturn(selectResult);
		when(selectResult.next()).thenReturn(false);
		
		try (MockedStatic<DatabaseFactory> database = mockStatic(DatabaseFactory.class);
			MockedStatic<SecondaryAuthData> secondaryAuth = mockStatic(SecondaryAuthData.class))
		{
			database.when(DatabaseFactory::getConnection).thenReturn(connection);
			secondaryAuth.when(SecondaryAuthData::getInstance).thenReturn(authData);
			
			final SecondaryPasswordAuth auth = new SecondaryPasswordAuth(client);
			assertTrue(auth.savePassword(INITIAL_PIN));
			assertTrue(auth.changePassword(INITIAL_PIN, CHANGED_PIN));
		}
		
		verify(connection).prepareStatement(SEC_AUTH_SELECT);
		verify(selectStatement).setString(1, ACCOUNT);
		verify(connection).prepareStatement(SEC_AUTH_INSERT);
		verify(insertStatement).setString(1, ACCOUNT);
		verify(connection).prepareStatement(SEC_AUTH_UPDATE);
		verify(updateStatement).setString(2, ACCOUNT);
	}
}
