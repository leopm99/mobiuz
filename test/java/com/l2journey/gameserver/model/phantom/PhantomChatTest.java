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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.network.serverpackets.ServerPacket;

/**
 * Unit tests for {@link PhantomChat} typing delay.
 */
class PhantomChatTest
{
	@Test
	void calculateTypingTimeUsesLengthAndJitter()
	{
		assertEquals(1500, PhantomChat.calculateTypingTime(null, 0));
		assertEquals(1500 + (4 * 120) + 500, PhantomChat.calculateTypingTime("test", 500));
		assertEquals(1500 + 120, PhantomChat.calculateTypingTime("a", -10));
	}

	@Test
	void requestChatResponseDoesNotSendWhenDeadAfterDelay()
	{
		Player bot = mock(Player.class);
		when(bot.isDead()).thenReturn(false).thenReturn(true);
		when(bot.isOnline()).thenReturn(true);
		when(bot.getName()).thenReturn("Bot");
		AtomicReference<Runnable> scheduled = new AtomicReference<>();
		ScheduledFuture<?> future = mock(ScheduledFuture.class);

		try (MockedStatic<ThreadPool> threadPool = mockStatic(ThreadPool.class))
		{
			threadPool.when(() -> ThreadPool.schedule(any(Runnable.class), anyLong())).thenAnswer(inv ->
			{
				scheduled.set(inv.getArgument(0));
				return future;
			});

			PhantomChat.requestChatResponse(bot, null, false);
			assertTrue(scheduled.get() != null);
			scheduled.get().run();
			verify(bot, never()).broadcastPacket(any(ServerPacket.class));
		}
	}

	@Test
	void requestChatResponseSendsWhenStillAlive()
	{
		Player bot = mock(Player.class);
		when(bot.isDead()).thenReturn(false);
		when(bot.isOnline()).thenReturn(true);
		when(bot.getName()).thenReturn("Bot");
		AtomicReference<Runnable> scheduled = new AtomicReference<>();

		try (MockedStatic<ThreadPool> threadPool = mockStatic(ThreadPool.class))
		{
			threadPool.when(() -> ThreadPool.schedule(any(Runnable.class), anyLong())).thenAnswer(inv ->
			{
				scheduled.set(inv.getArgument(0));
				return mock(ScheduledFuture.class);
			});

			PhantomChat.requestChatResponse(bot, null, false);
			scheduled.get().run();
			verify(bot).broadcastPacket(any(ServerPacket.class));
		}
	}

	@Test
	void requestChatResponseIgnoresAlreadyDeadBot()
	{
		Player bot = mock(Player.class);
		when(bot.isDead()).thenReturn(true);
		PhantomChat.requestChatResponse(bot, null, false);
		verify(bot, never()).broadcastPacket(any(ServerPacket.class));
	}

	@Test
	void sayDeathMessageBroadcasts()
	{
		Player bot = mock(Player.class);
		when(bot.getName()).thenReturn("Bot");
		PhantomChat.sayDeathMessage(bot);
		verify(bot).broadcastPacket(any(ServerPacket.class));
		PhantomChat.sayDeathMessage(null);
	}

	@Test
	void privateReplyGoesToSender()
	{
		Player bot = mock(Player.class);
		Player sender = mock(Player.class);
		when(bot.getName()).thenReturn("Bot");
		PhantomChat.botReply(bot, sender, "hola", true);
		verify(sender).sendPacket(any(ServerPacket.class));
	}
}
