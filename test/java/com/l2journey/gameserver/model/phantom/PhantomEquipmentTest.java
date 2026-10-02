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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.actor.Player;

/**
 * Unit tests for {@link PhantomEquipment} progression guard.
 */
class PhantomEquipmentTest
{
	@AfterEach
	void tearDown()
	{
		PhantomEquipment.clearRuntimeState();
	}
	
	@Test
	void nullBotIsRejected()
	{
		assertFalse(PhantomEquipment.beginProgressionCheck(null));
	}
	
	@Test
	void cursedWeaponSkipDoesNotReadObjectId()
	{
		Player bot = mock(Player.class);
		when(bot.isCursedWeaponEquipped()).thenReturn(true);
		
		assertFalse(PhantomEquipment.beginProgressionCheck(bot));
		verify(bot, never()).getObjectId();
	}
	
	@Test
	void throttleBlocksSecondCheckUntilCleared()
	{
		Player bot = mock(Player.class);
		when(bot.isCursedWeaponEquipped()).thenReturn(false);
		when(bot.getObjectId()).thenReturn(77);
		
		assertTrue(PhantomEquipment.beginProgressionCheck(bot));
		assertFalse(PhantomEquipment.beginProgressionCheck(bot));
		
		PhantomEquipment.unregisterBot(77);
		assertTrue(PhantomEquipment.beginProgressionCheck(bot));
		
		PhantomEquipment.clearRuntimeState();
		assertTrue(PhantomEquipment.beginProgressionCheck(bot));
	}
}
