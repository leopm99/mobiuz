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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.File;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import com.l2journey.Config;
import com.l2journey.commons.database.DatabaseFactory;

/**
 * Covers ItemAuctionManager XmlFactory call site.
 */
class ItemAuctionManagerXmlFactoryTest
{
	@TempDir
	File tempDir;
	
	private File previousDatapackRoot;
	private boolean previousEnabled;
	
	@BeforeEach
	void saveConfig()
	{
		previousDatapackRoot = Config.DATAPACK_ROOT;
		previousEnabled = Config.ALT_ITEM_AUCTION_ENABLED;
	}
	
	@AfterEach
	void restoreConfig()
	{
		Config.DATAPACK_ROOT = previousDatapackRoot;
		Config.ALT_ITEM_AUCTION_ENABLED = previousEnabled;
	}
	
	@Test
	void constructorLoadsXmlThroughSecureFactory() throws Exception
	{
		Config.DATAPACK_ROOT = tempDir;
		Config.ALT_ITEM_AUCTION_ENABLED = true;
		Files.createDirectories(new File(tempDir, "data").toPath());
		Files.writeString(new File(tempDir, "data/ItemAuctions.xml").toPath(), "<?xml version=\"1.0\" encoding=\"UTF-8\"?><list/>", StandardCharsets.UTF_8);
		
		final Connection connection = mock(Connection.class);
		final Statement statement = mock(Statement.class);
		final ResultSet resultSet = mock(ResultSet.class);
		when(connection.createStatement()).thenReturn(statement);
		when(statement.executeQuery(anyString())).thenReturn(resultSet);
		when(resultSet.next()).thenReturn(false);
		
		try (MockedStatic<DatabaseFactory> database = mockStatic(DatabaseFactory.class))
		{
			database.when(DatabaseFactory::getConnection).thenReturn(connection);
			final Constructor<ItemAuctionManager> constructor = ItemAuctionManager.class.getDeclaredConstructor();
			constructor.setAccessible(true);
			final ItemAuctionManager manager = assertDoesNotThrow(() -> constructor.newInstance());
			assertNotNull(manager);
		}
	}
}
