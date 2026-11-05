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
package com.l2journey.gameserver.taskmanagers.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import com.l2journey.Config;
import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.managers.MailManager;
import com.l2journey.gameserver.model.Message;
import com.l2journey.gameserver.model.itemcontainer.Mail;

/**
 * Birthday mail placeholder substitution must treat $c1 and $s1 as literal text.
 */
class TaskBirthdayTest
{
	private String previousMailText;
	private String previousMailSubject;
	private int previousGift;
	
	@BeforeEach
	void saveConfig()
	{
		previousMailText = Config.ALT_BIRTHDAY_MAIL_TEXT;
		previousMailSubject = Config.ALT_BIRTHDAY_MAIL_SUBJECT;
		previousGift = Config.ALT_BIRTHDAY_GIFT;
	}
	
	@AfterEach
	void restoreConfig()
	{
		Config.ALT_BIRTHDAY_MAIL_TEXT = previousMailText;
		Config.ALT_BIRTHDAY_MAIL_SUBJECT = previousMailSubject;
		Config.ALT_BIRTHDAY_GIFT = previousGift;
	}
	
	@Test
	void giveBirthdayGiftsSubstitutesPlaceholdersLiterally() throws Exception
	{
		Config.ALT_BIRTHDAY_MAIL_TEXT = "Hello $c1 ($s1)";
		Config.ALT_BIRTHDAY_MAIL_SUBJECT = "Happy Birthday!";
		Config.ALT_BIRTHDAY_GIFT = 22187;
		
		final Connection connection = mock(Connection.class);
		final PreparedStatement statement = mock(PreparedStatement.class);
		final ResultSet resultSet = mock(ResultSet.class);
		final Mail attachments = mock(Mail.class);
		final MailManager mailManager = mock(MailManager.class);
		final List<String> contents = new ArrayList<>();
		
		when(connection.prepareStatement(anyString())).thenReturn(statement);
		when(statement.executeQuery()).thenReturn(resultSet);
		when(resultSet.next()).thenReturn(true, true, false);
		when(resultSet.getInt("charId")).thenReturn(1001, 1002);
		when(resultSet.getString("char_name")).thenReturn("Alice", "Alice$");
		when(resultSet.getInt("age")).thenReturn(3, 3);
		
		try (MockedStatic<DatabaseFactory> database = mockStatic(DatabaseFactory.class);
			MockedStatic<MailManager> mailManagers = mockStatic(MailManager.class);
			MockedConstruction<Message> messages = mockConstruction(Message.class, (mock, context) ->
			{
				when(mock.createAttachments()).thenReturn(attachments);
				contents.add((String) context.arguments().get(2));
			}))
		{
			database.when(DatabaseFactory::getConnection).thenReturn(connection);
			mailManagers.when(MailManager::getInstance).thenReturn(mailManager);
			
			final int giftCount = new TaskBirthday().giveBirthdayGifts(1_700_000_000_000L);
			
			assertEquals(2, giftCount);
			assertEquals(List.of("Hello Alice (3)", "Hello Alice$ (3)"), contents);
			assertEquals(2, messages.constructed().size());
			verify(mailManager).sendMessage(messages.constructed().get(0));
			verify(mailManager).sendMessage(messages.constructed().get(1));
		}
	}
}
