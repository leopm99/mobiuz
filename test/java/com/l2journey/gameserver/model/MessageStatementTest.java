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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Message#applyInsertParameters(Message, PreparedStatement)}.
 */
class MessageStatementTest
{
	@Test
	void applyInsertParametersBindsAllColumns() throws Exception
	{
		final ResultSet rset = mock(ResultSet.class);
		when(rset.getInt("messageId")).thenReturn(10);
		when(rset.getInt("senderId")).thenReturn(20);
		when(rset.getInt("receiverId")).thenReturn(30);
		when(rset.getString("subject")).thenReturn("subject");
		when(rset.getString("content")).thenReturn("content");
		when(rset.getLong("expiration")).thenReturn(1000L);
		when(rset.getLong("reqAdena")).thenReturn(50L);
		when(rset.getString("hasAttachments")).thenReturn("false");
		when(rset.getString("isUnread")).thenReturn("true");
		when(rset.getString("isDeletedBySender")).thenReturn("false");
		when(rset.getString("isDeletedByReceiver")).thenReturn("false");
		when(rset.getInt("sendBySystem")).thenReturn(0);
		when(rset.getString("isReturned")).thenReturn("false");
		
		final Message message = new Message(rset);
		final PreparedStatement statement = mock(PreparedStatement.class);
		
		Message.applyInsertParameters(message, statement);
		
		verify(statement).setInt(1, 10);
		verify(statement).setInt(2, 20);
		verify(statement).setInt(3, 30);
		verify(statement).setString(4, "subject");
		verify(statement).setString(5, "content");
		verify(statement).setLong(6, 1000L);
		verify(statement).setLong(7, 50L);
		verify(statement).setString(8, "false");
		verify(statement).setString(9, "true");
		verify(statement).setString(10, "false");
		verify(statement).setString(11, "false");
		verify(statement).setString(12, "0");
		verify(statement).setString(13, "false");
	}
}
