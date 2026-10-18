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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.ScheduledFuture;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.model.Message;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.taskmanagers.MessageDeletionTaskManager;

/**
 * Unit tests for {@link MailManager#sendMessage(Message)}.
 */
class MailManagerSendMessageTest
{
	@Test
	void sendMessagePreparesInsertSqlAndAppliesParameters() throws Exception
	{
		final Connection connection = mock(Connection.class);
		final Statement loadStatement = mock(Statement.class);
		final ResultSet loadResult = mock(ResultSet.class);
		final PreparedStatement insertStatement = mock(PreparedStatement.class);
		when(connection.createStatement()).thenReturn(loadStatement);
		when(loadStatement.executeQuery(anyString())).thenReturn(loadResult);
		when(loadResult.next()).thenReturn(false);
		when(connection.prepareStatement(eq(Message.INSERT_SQL))).thenReturn(insertStatement);
		
		final ResultSet messageRs = mock(ResultSet.class);
		when(messageRs.getInt("messageId")).thenReturn(42);
		when(messageRs.getInt("senderId")).thenReturn(1);
		when(messageRs.getInt("receiverId")).thenReturn(2);
		when(messageRs.getString("subject")).thenReturn("subject");
		when(messageRs.getString("content")).thenReturn("content");
		when(messageRs.getLong("expiration")).thenReturn(1000L);
		when(messageRs.getLong("reqAdena")).thenReturn(0L);
		when(messageRs.getString("hasAttachments")).thenReturn("false");
		when(messageRs.getString("isUnread")).thenReturn("true");
		when(messageRs.getString("isDeletedBySender")).thenReturn("false");
		when(messageRs.getString("isDeletedByReceiver")).thenReturn("false");
		when(messageRs.getInt("sendBySystem")).thenReturn(0);
		when(messageRs.getString("isReturned")).thenReturn("false");
		final Message message = new Message(messageRs);
		
		final MessageDeletionTaskManager deletionManager = mock(MessageDeletionTaskManager.class);
		final World world = mock(World.class);
		when(world.getPlayer(2)).thenReturn(null);
		
		try (MockedStatic<DatabaseFactory> database = mockStatic(DatabaseFactory.class);
			MockedStatic<MessageDeletionTaskManager> deletions = mockStatic(MessageDeletionTaskManager.class);
			MockedStatic<World> worlds = mockStatic(World.class);
			MockedStatic<ThreadPool> threads = mockStatic(ThreadPool.class))
		{
			database.when(DatabaseFactory::getConnection).thenReturn(connection);
			deletions.when(MessageDeletionTaskManager::getInstance).thenReturn(deletionManager);
			worlds.when(World::getInstance).thenReturn(world);
			threads.when(() -> ThreadPool.scheduleAtFixedRate(org.mockito.ArgumentMatchers.any(Runnable.class), anyLong(), anyLong())).thenReturn(mock(ScheduledFuture.class));
			
			MailManager.getInstance().sendMessage(message);
			
			verify(connection).prepareStatement(Message.INSERT_SQL);
			verify(insertStatement).setInt(1, 42);
			verify(insertStatement).execute();
			verify(deletionManager).add(42, 1000L);
		}
	}
}
