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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.concurrent.ScheduledFuture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.actor.Player;

/**
 * Unit tests for {@link PhantomHuntingSpots#travelToLevelSpot}.
 */
class PhantomHuntingSpotsTest
{
	private boolean _debugWasOn;
	
	@BeforeEach
	void setUp()
	{
		_debugWasOn = PhantomManager.isDebugMode();
		if (_debugWasOn)
		{
			PhantomManager.toggleDebug();
		}
		PhantomEngine.clearPendingReturns();
		PhantomState.clear();
	}
	
	@AfterEach
	void tearDown()
	{
		PhantomEngine.clearPendingReturns();
		PhantomState.clear();
		if (_debugWasOn != PhantomManager.isDebugMode())
		{
			PhantomManager.toggleDebug();
		}
	}
	
	@Test
	void nullSpotReturnsFalse()
	{
		Player bot = mock(Player.class);
		assertFalse(PhantomHuntingSpots.travelToLevelSpot(bot, null, "x"));
	}
	
	@Test
	void spotSetsHuntBandAndStartsReturnRelocate()
	{
		Player bot = mock(Player.class);
		when(bot.getObjectId()).thenReturn(55);
		when(bot.getName()).thenReturn("Hunter");
		when(bot.getLevel()).thenReturn(42);
		Location spot = new Location(1000, 2000, 3000);
		ScheduledFuture<?> future = mock(ScheduledFuture.class);
		
		try (MockedStatic<PhantomGeo> geo = mockStatic(PhantomGeo.class); MockedStatic<ThreadPool> threadPool = mockStatic(ThreadPool.class))
		{
			geo.when(() -> PhantomGeo.getNpcLikeSpawn(spot)).thenReturn(null);
			threadPool.when(() -> ThreadPool.schedule(any(Runnable.class), anyLong())).thenReturn(future);
			
			assertTrue(PhantomHuntingSpots.travelToLevelSpot(bot, spot, "hunt"));
			assertEquals(40, PhantomState.HUNT_LEVEL_BAND.get(55));
			assertTrue(PhantomState.RETURN_UNTIL.containsKey(55));
		}
	}
}
