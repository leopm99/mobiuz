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
package com.l2journey.gameserver.model.olympiad;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.EventsConfig;

/**
 * Covers interrupt restoration in {@link Olympiad#waitForPendingBattles()} (java:S2142).
 */
class OlympiadInterruptTest
{
	private boolean savedEnabled;
	
	@BeforeAll
	static void loadEventsConfigDefaults()
	{
		EventsConfig.load();
	}
	
	@BeforeEach
	void disableOlympiad()
	{
		savedEnabled = EventsConfig.OLYMPIAD_ENABLED;
		EventsConfig.OLYMPIAD_ENABLED = false;
	}
	
	@AfterEach
	void restoreOlympiad()
	{
		EventsConfig.OLYMPIAD_ENABLED = savedEnabled;
	}
	
	@Test
	void waitForPendingBattlesRestoresInterruptAndReturns()
	{
		final OlympiadGameManager gameManager = mock(OlympiadGameManager.class);
		when(gameManager.isBattleStarted()).thenReturn(true);
		
		try (MockedStatic<OlympiadGameManager> gameManagers = mockStatic(OlympiadGameManager.class))
		{
			gameManagers.when(OlympiadGameManager::getInstance).thenReturn(gameManager);
			
			final Olympiad olympiad = new Olympiad();
			Thread.currentThread().interrupt();
			try
			{
				olympiad.waitForPendingBattles();
				assertTrue(Thread.currentThread().isInterrupted());
			}
			finally
			{
				Thread.interrupted();
			}
		}
	}
	
	@Test
	void waitForPendingBattlesReturnsWhenNoBattle()
	{
		final OlympiadGameManager gameManager = mock(OlympiadGameManager.class);
		when(gameManager.isBattleStarted()).thenReturn(false);
		
		try (MockedStatic<OlympiadGameManager> gameManagers = mockStatic(OlympiadGameManager.class))
		{
			gameManagers.when(OlympiadGameManager::getInstance).thenReturn(gameManager);
			
			final Olympiad olympiad = new Olympiad();
			olympiad.waitForPendingBattles();
			verify(gameManager).isBattleStarted();
		}
	}
}
