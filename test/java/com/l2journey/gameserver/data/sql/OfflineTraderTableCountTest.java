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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.Config;
import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.player.PrivateStoreType;
import com.l2journey.gameserver.model.olympiad.OlympiadManager;
import com.l2journey.gameserver.model.skill.AbnormalVisualEffect;
import com.l2journey.gameserver.network.GameClient;
import com.l2journey.gameserver.network.serverpackets.ServerPacket;

/**
 * Atomic offline-trade counter updates (java:S3078).
 */
class OfflineTraderTableCountTest
{
	private boolean previousTradeEnable;
	private boolean previousCraftEnable;
	private boolean previousPeaceZone;
	private boolean previousDualbox;
	private boolean previousNameColor;
	private boolean previousRealtime;
	private List<AbnormalVisualEffect> previousEffects;
	
	@BeforeEach
	void snapshotConfig()
	{
		previousTradeEnable = Config.OFFLINE_TRADE_ENABLE;
		previousCraftEnable = Config.OFFLINE_CRAFT_ENABLE;
		previousPeaceZone = Config.OFFLINE_MODE_IN_PEACE_ZONE;
		previousDualbox = Config.DUALBOX_COUNT_OFFLINE_TRADERS;
		previousNameColor = Config.OFFLINE_SET_NAME_COLOR;
		previousRealtime = Config.STORE_OFFLINE_TRADE_IN_REALTIME;
		previousEffects = new ArrayList<>(Config.OFFLINE_ABNORMAL_EFFECTS);
		World.OFFLINE_TRADE_COUNT.set(0);
	}
	
	@AfterEach
	void restoreConfig()
	{
		Config.OFFLINE_TRADE_ENABLE = previousTradeEnable;
		Config.OFFLINE_CRAFT_ENABLE = previousCraftEnable;
		Config.OFFLINE_MODE_IN_PEACE_ZONE = previousPeaceZone;
		Config.DUALBOX_COUNT_OFFLINE_TRADERS = previousDualbox;
		Config.OFFLINE_SET_NAME_COLOR = previousNameColor;
		Config.STORE_OFFLINE_TRADE_IN_REALTIME = previousRealtime;
		Config.OFFLINE_ABNORMAL_EFFECTS.clear();
		Config.OFFLINE_ABNORMAL_EFFECTS.addAll(previousEffects);
		World.OFFLINE_TRADE_COUNT.set(0);
	}
	
	@Test
	void restoreOfflineTradersSetsCountFromEmptyResult() throws Exception
	{
		Config.STORE_OFFLINE_TRADE_IN_REALTIME = true;
		World.OFFLINE_TRADE_COUNT.set(7);
		
		final Connection connection = mock(Connection.class);
		final Statement statement = mock(Statement.class);
		final ResultSet resultSet = mock(ResultSet.class);
		final PreparedStatement items = mock(PreparedStatement.class);
		when(connection.createStatement()).thenReturn(statement);
		when(statement.executeQuery(anyString())).thenReturn(resultSet);
		when(resultSet.next()).thenReturn(false);
		when(connection.prepareStatement(anyString())).thenReturn(items);
		
		try (MockedStatic<DatabaseFactory> database = mockStatic(DatabaseFactory.class))
		{
			database.when(DatabaseFactory::getConnection).thenReturn(connection);
			OfflineTraderTable.getInstance().restoreOfflineTraders();
		}
		
		assertEquals(0, World.OFFLINE_TRADE_COUNT.get());
	}
	
	@Test
	void removeTraderDecrementsCountWhenDeleteFails()
	{
		World.OFFLINE_TRADE_COUNT.set(3);
		
		try (MockedStatic<DatabaseFactory> database = mockStatic(DatabaseFactory.class))
		{
			database.when(DatabaseFactory::getConnection).thenThrow(new RuntimeException("db down"));
			OfflineTraderTable.getInstance().removeTrader(42);
		}
		
		assertEquals(2, World.OFFLINE_TRADE_COUNT.get());
	}
	
	@Test
	void enteredOfflineModeIncrementsCount()
	{
		Config.OFFLINE_TRADE_ENABLE = true;
		Config.OFFLINE_MODE_IN_PEACE_ZONE = false;
		Config.DUALBOX_COUNT_OFFLINE_TRADERS = true;
		Config.OFFLINE_SET_NAME_COLOR = false;
		Config.STORE_OFFLINE_TRADE_IN_REALTIME = false;
		Config.OFFLINE_ABNORMAL_EFFECTS.clear();
		
		final GameClient client = mock(GameClient.class);
		when(client.isDetached()).thenReturn(false);
		doNothing().when(client).close(any(ServerPacket.class));
		doNothing().when(client).setDetached(true);
		
		final Player player = mock(Player.class);
		when(player.isInOlympiadMode()).thenReturn(false);
		when(player.isRegisteredOnEvent()).thenReturn(false);
		when(player.isJailed()).thenReturn(false);
		when(player.getVehicle()).thenReturn(null);
		when(player.getPrivateStoreType()).thenReturn(PrivateStoreType.SELL);
		when(player.getClient()).thenReturn(client);
		when(player.getSummon()).thenReturn(null);
		when(player.getOfflineStartTime()).thenReturn(1L);
		doNothing().when(player).leaveParty();
		doNothing().when(player).storeMe();
		
		final OlympiadManager olympiadManager = mock(OlympiadManager.class);
		when(olympiadManager.unRegisterNoble(player)).thenReturn(false);
		
		try (MockedStatic<OlympiadManager> olympiad = mockStatic(OlympiadManager.class))
		{
			olympiad.when(OlympiadManager::getInstance).thenReturn(olympiadManager);
			assertTrue(OfflineTraderTable.getInstance().enteredOfflineMode(player));
		}
		
		assertEquals(1, World.OFFLINE_TRADE_COUNT.get());
	}
}
