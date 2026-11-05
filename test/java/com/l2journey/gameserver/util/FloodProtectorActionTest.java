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
package com.l2journey.gameserver.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.gameserver.network.GameClient;
import com.l2journey.gameserver.taskmanagers.GameTimeTaskManager;

/**
 * Interval burst characterization for {@link FloodProtectorAction#canPerformAction()}.
 */
class FloodProtectorActionTest
{
	@Test
	void firstAttemptPassesThenBurstBlockedUntilInterval()
	{
		final AtomicInteger tick = new AtomicInteger(100);
		final GameTimeTaskManager time = mock(GameTimeTaskManager.class);
		when(time.getGameTicks()).thenAnswer(invocation -> tick.get());
		
		final GameClient client = mock(GameClient.class);
		when(client.getPlayer()).thenReturn(null);
		
		final FloodProtectorConfig config = new FloodProtectorConfig("TestFlood");
		config.floodProtectionInterval = 5;
		config.logFlooding = false;
		config.punishmentLimit = 0;
		
		try (MockedStatic<GameTimeTaskManager> times = mockStatic(GameTimeTaskManager.class))
		{
			times.when(GameTimeTaskManager::getInstance).thenReturn(time);
			
			final FloodProtectorAction action = new FloodProtectorAction(client, config);
			
			assertTrue(action.canPerformAction());
			assertFalse(action.canPerformAction());
			
			tick.set(104);
			assertFalse(action.canPerformAction());
			
			tick.set(105);
			assertTrue(action.canPerformAction());
		}
	}
}
