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
package com.l2journey.gameserver.data;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.ScheduledFuture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.Config;
import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.commons.threads.ThreadPool;

/**
 * Covers BotReportTable XmlFactory SAX call site.
 */
class BotReportXmlFactoryTest
{
	private static final Path XML = Path.of("config", "security", "botreportpunishments.xml");
	
	private boolean previousEnable;
	private boolean xmlExisted;
	private String[] previousResetHour;
	
	@BeforeEach
	void prepare() throws Exception
	{
		previousEnable = Config.BOTREPORT_ENABLE;
		previousResetHour = Config.BOTREPORT_RESETPOINT_HOUR;
		Config.BOTREPORT_ENABLE = true;
		Config.BOTREPORT_RESETPOINT_HOUR = new String[]
		{
			"00",
			"00"
		};
		xmlExisted = Files.exists(XML);
		Files.createDirectories(XML.getParent());
		Files.writeString(XML, "<?xml version=\"1.0\" encoding=\"UTF-8\"?><list/>", StandardCharsets.UTF_8);
	}
	
	@AfterEach
	void restore() throws Exception
	{
		Config.BOTREPORT_ENABLE = previousEnable;
		Config.BOTREPORT_RESETPOINT_HOUR = previousResetHour;
		if (!xmlExisted)
		{
			Files.deleteIfExists(XML);
		}
	}
	
	@Test
	void constructorParsesPunishmentsThroughSecureSaxFactory() throws Exception
	{
		final Connection connection = mock(Connection.class);
		final Statement statement = mock(Statement.class);
		final ResultSet resultSet = mock(ResultSet.class);
		when(connection.createStatement()).thenReturn(statement);
		when(statement.executeQuery(anyString())).thenReturn(resultSet);
		when(resultSet.next()).thenReturn(false);
		
		try (MockedStatic<DatabaseFactory> database = mockStatic(DatabaseFactory.class);
			MockedStatic<ThreadPool> threads = mockStatic(ThreadPool.class))
		{
			database.when(DatabaseFactory::getConnection).thenReturn(connection);
			threads.when(() -> ThreadPool.schedule(org.mockito.ArgumentMatchers.any(Runnable.class), anyLong())).thenReturn(mock(ScheduledFuture.class));
			
			final Constructor<BotReportTable> constructor = BotReportTable.class.getDeclaredConstructor();
			constructor.setAccessible(true);
			final BotReportTable table = assertDoesNotThrow(() -> constructor.newInstance());
			assertNotNull(table);
		}
	}
}
