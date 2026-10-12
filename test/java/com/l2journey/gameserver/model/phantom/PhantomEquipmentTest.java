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

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.actor.Player;

/**
 * Unit tests for {@link PhantomEquipment} progression guard and kit contents.
 */
class PhantomEquipmentTest
{
	private static final int TATTOO_OF_DIVINE = 496;
	private static final List<String> KITS_THAT_MUST_NOT_USE_CHEST_TATTOO = List.of(
		"ROBE_KIT_B",
		"LIGHT_KIT_B",
		"HEAVY_KIT_B",
		"ROBE_KIT_A",
		"LIGHT_KIT_A",
		"HEAVY_KIT_A",
		"ROBE_KIT_S",
		"LIGHT_KIT_S",
		"HEAVY_KIT_S",
		"ROBE_KIT_S84",
		"LIGHT_KIT_S84",
		"HEAVY_KIT_S84");
	
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
	
	@Test
	void gradeKitsDoNotIncludeTattooOfDivineChest() throws Exception
	{
		for (String kitName : KITS_THAT_MUST_NOT_USE_CHEST_TATTOO)
		{
			int[] kit = readKit(kitName);
			assertTrue(Arrays.stream(kit).noneMatch(id -> id == TATTOO_OF_DIVINE), () -> kitName + " must not contain Tattoo of Divine (" + TATTOO_OF_DIVINE + "), which overwrites the chest slot");
		}
	}
	
	private static int[] readKit(String fieldName) throws Exception
	{
		Field field = PhantomEquipment.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		return (int[]) field.get(null);
	}
}
