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
package com.l2journey.gameserver.model.phantom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.network.GameClient;
import com.l2journey.gameserver.network.serverpackets.CreatureSay;
import com.l2journey.gameserver.network.serverpackets.ServerPacket;

/**
 * Unit tests for town conversation gates in {@link PhantomChat}.
 */
class PhantomChatTownTest
{
	@AfterEach
	void tearDown()
	{
		PhantomChat.clearRuntimeState();
	}

	@Test
	void townChatEligibilityAndRollGates()
	{
		assertFalse(PhantomChat.isEligibleForTownChat(null));

		Player dead = mock(Player.class);
		when(dead.isDead()).thenReturn(true);
		assertFalse(PhantomChat.isEligibleForTownChat(dead));

		Player offline = mock(Player.class);
		when(offline.isDead()).thenReturn(false);
		when(offline.isOnline()).thenReturn(false);
		assertFalse(PhantomChat.isEligibleForTownChat(offline));

		Player ready = mock(Player.class);
		when(ready.isDead()).thenReturn(false);
		when(ready.isOnline()).thenReturn(true);
		assertTrue(PhantomChat.isEligibleForTownChat(ready));

		assertTrue(PhantomChat.isTownChatCooldownReady(100000L, 0L));
		assertFalse(PhantomChat.isTownChatCooldownReady(10000L, 9000L));
		assertTrue(PhantomChat.passesTownChatRoll(0));
		assertTrue(PhantomChat.passesTownChatRoll(9));
		assertFalse(PhantomChat.passesTownChatRoll(10));

		PhantomChat.attemptBotConversation(null);
		PhantomChat.attemptBotConversation(dead);
	}

	@Test
	void attemptBotConversationGreetsNearestPhantom()
	{
		Player bot = readyBot(1, 0, 0, 0);
		Player partner = nearbyPhantom(2, 100, 0, 0);
		Player dead = nearbyPhantom(3, 50, 0, 0);
		when(dead.isDead()).thenReturn(true);
		Player real = nearbyPhantom(4, 40, 0, 0);
		when(real.getClient()).thenReturn(mock(GameClient.class));
		Player high = nearbyPhantom(5, 30, 0, 400);
		Player far = nearbyPhantom(6, 2000, 0, 0);
		World world = mock(World.class);
		when(world.getVisibleObjects(bot, Player.class)).thenReturn(Arrays.asList(null, bot, dead, real, high, far, partner));

		try (MockedStatic<World> worlds = mockStatic(World.class);
			MockedStatic<Rnd> rnd = mockStatic(Rnd.class);
			MockedStatic<ThreadPool> pool = mockStatic(ThreadPool.class))
		{
			worlds.when(World::getInstance).thenReturn(world);
			rnd.when(() -> Rnd.get(anyInt())).thenReturn(0);
			rnd.when(() -> Rnd.get(anyInt(), anyInt())).thenReturn(0);
			pool.when(() -> ThreadPool.schedule(any(Runnable.class), any(long.class))).thenReturn(null);

			PhantomChat.attemptBotConversation(bot);
			verify(bot).broadcastPacket(any());

			PhantomChat.attemptBotConversation(bot);
			verify(bot).broadcastPacket(any());
		}
	}

	@Test
	void attemptBotConversationStopsWhenRollOrPartnerMisses()
	{
		Player bot = readyBot(9, 0, 0, 0);
		World world = mock(World.class);
		when(world.getVisibleObjects(bot, Player.class)).thenReturn(List.of());

		try (MockedStatic<World> worlds = mockStatic(World.class);
			MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			worlds.when(World::getInstance).thenReturn(world);
			rnd.when(() -> Rnd.get(anyInt())).thenReturn(0);
			PhantomChat.attemptBotConversation(bot);
			verify(bot, never()).broadcastPacket(any());
		}

		PhantomChat.clearRuntimeState();
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(anyInt())).thenReturn(50);
			PhantomChat.attemptBotConversation(bot);
			verify(bot, never()).broadcastPacket(any());
		}
	}

	@Test
	void townReplyIsAGreetingAndSkipsTradeLines() throws Exception
	{
		PhantomChat.scheduleTownReply(null);

		Player dead = readyBot(3, 0, 0, 0);
		when(dead.isDead()).thenReturn(true);
		PhantomChat.scheduleTownReply(dead);
		verify(dead, never()).broadcastPacket(any());

		Player partner = readyBot(2, 10, 0, 0);
		when(partner.isOnline()).thenReturn(false);
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class);
			MockedStatic<ThreadPool> pool = mockStatic(ThreadPool.class))
		{
			rnd.when(() -> Rnd.get(anyInt())).thenReturn(0);
			rnd.when(() -> Rnd.get(anyInt(), anyInt())).thenReturn(0);
			pool.when(() -> ThreadPool.schedule(any(Runnable.class), any(long.class))).thenAnswer(invocation ->
			{
				invocation.getArgument(0, Runnable.class).run();
				return null;
			});
			PhantomChat.scheduleTownReply(partner);
			verify(partner, never()).broadcastPacket(any());

			when(partner.isOnline()).thenReturn(true);
			PhantomChat.scheduleTownReply(partner);
		}

		ArgumentCaptor<ServerPacket> packet = ArgumentCaptor.forClass(ServerPacket.class);
		verify(partner).broadcastPacket(packet.capture());
		assertEquals("todo bien", sayText(packet.getValue()));
	}

	private static String sayText(ServerPacket packet) throws Exception
	{
		Field text = CreatureSay.class.getDeclaredField("_text");
		text.setAccessible(true);
		return (String) text.get(packet);
	}

	private static Player readyBot(int objectId, int x, int y, int z)
	{
		Player bot = mock(Player.class);
		when(bot.isDead()).thenReturn(false);
		when(bot.isOnline()).thenReturn(true);
		when(bot.getObjectId()).thenReturn(objectId);
		when(bot.getName()).thenReturn("Bot" + objectId);
		when(bot.getX()).thenReturn(x);
		when(bot.getY()).thenReturn(y);
		when(bot.getZ()).thenReturn(z);
		return bot;
	}

	private static Player nearbyPhantom(int objectId, int x, int y, int z)
	{
		Player other = readyBot(objectId, x, y, z);
		when(other.getClient()).thenReturn(null);
		return other;
	}
}
