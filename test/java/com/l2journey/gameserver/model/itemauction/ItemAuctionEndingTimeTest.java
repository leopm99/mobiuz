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
package com.l2journey.gameserver.model.itemauction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.l2journey.Config;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.network.serverpackets.ServerPacket;

/**
 * Atomic ending-time extensions for {@link ItemAuction} (java:S3078).
 */
class ItemAuctionEndingTimeTest
{
	private static final long EXTEND_5 = TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES);
	private static final long EXTEND_3 = TimeUnit.MILLISECONDS.convert(3, TimeUnit.MINUTES);
	private static final long EXTEND_CONFIG = 60_000L;
	
	@Test
	void onPlayerBidExtendsEndingTimeThroughAllPhases() throws Exception
	{
		final long previousConfig = Config.ALT_ITEM_AUCTION_TIME_EXTENDS_ON_BID;
		Config.ALT_ITEM_AUCTION_TIME_EXTENDS_ON_BID = EXTEND_CONFIG;
		try
		{
			final ItemAuction auction = mock(ItemAuction.class, CALLS_REAL_METHODS);
			doNothing().when(auction).broadcastToAllBidders(any(ServerPacket.class));
			
			final long baseEnding = System.currentTimeMillis();
			setField(auction, "_endingTime", baseEnding);
			setField(auction, "_highestBid", new ItemAuctionBid(1, 100));
			setField(auction, "_auctionEndingExtendState", ItemAuctionExtendState.INITIAL);
			
			invokeOnPlayerBid(auction, player(10), new ItemAuctionBid(10, 100));
			assertEquals(ItemAuctionExtendState.EXTEND_BY_5_MIN, getField(auction, "_auctionEndingExtendState"));
			assertEquals(baseEnding + EXTEND_5, (long) getField(auction, "_endingTime"));
			
			invokeOnPlayerBid(auction, player(11), new ItemAuctionBid(11, 100));
			assertEquals(ItemAuctionExtendState.EXTEND_BY_3_MIN, getField(auction, "_auctionEndingExtendState"));
			assertEquals(baseEnding + EXTEND_5 + EXTEND_3, (long) getField(auction, "_endingTime"));
			
			invokeOnPlayerBid(auction, player(12), new ItemAuctionBid(12, 100));
			assertEquals(ItemAuctionExtendState.EXTEND_BY_CONFIG_PHASE_A, getField(auction, "_auctionEndingExtendState"));
			assertEquals(baseEnding + EXTEND_5 + EXTEND_3 + EXTEND_CONFIG, (long) getField(auction, "_endingTime"));
			
			setField(auction, "_scheduledAuctionEndingExtendState", ItemAuctionExtendState.EXTEND_BY_CONFIG_PHASE_B);
			invokeOnPlayerBid(auction, player(13), new ItemAuctionBid(13, 100));
			assertEquals(ItemAuctionExtendState.EXTEND_BY_CONFIG_PHASE_B, getField(auction, "_auctionEndingExtendState"));
			assertEquals(baseEnding + EXTEND_5 + EXTEND_3 + (2 * EXTEND_CONFIG), (long) getField(auction, "_endingTime"));
			
			setField(auction, "_scheduledAuctionEndingExtendState", ItemAuctionExtendState.EXTEND_BY_CONFIG_PHASE_A);
			invokeOnPlayerBid(auction, player(14), new ItemAuctionBid(14, 100));
			assertEquals(ItemAuctionExtendState.EXTEND_BY_CONFIG_PHASE_A, getField(auction, "_auctionEndingExtendState"));
			assertEquals(baseEnding + EXTEND_5 + EXTEND_3 + (3 * EXTEND_CONFIG), (long) getField(auction, "_endingTime"));
		}
		finally
		{
			Config.ALT_ITEM_AUCTION_TIME_EXTENDS_ON_BID = previousConfig;
		}
	}
	
	private static Player player(int objectId)
	{
		final Player player = mock(Player.class);
		when(player.getObjectId()).thenReturn(objectId);
		return player;
	}
	
	private static void invokeOnPlayerBid(ItemAuction auction, Player player, ItemAuctionBid bid) throws Exception
	{
		final Method method = ItemAuction.class.getDeclaredMethod("onPlayerBid", Player.class, ItemAuctionBid.class);
		method.setAccessible(true);
		method.invoke(auction, player, bid);
	}
	
	private static void setField(Object target, String name, Object value) throws Exception
	{
		final Field field = ItemAuction.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
	
	@SuppressWarnings("unchecked")
	private static <T> T getField(Object target, String name) throws Exception
	{
		final Field field = ItemAuction.class.getDeclaredField(name);
		field.setAccessible(true);
		return (T) field.get(target);
	}
}
