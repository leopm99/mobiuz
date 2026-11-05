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
package com.l2journey.gameserver.model.actor.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.PetData;
import com.l2journey.gameserver.model.PetLevelData;

/**
 * Unit tests for null-safe pet data access in {@link Pet}.
 */
class PetNullGuardTest
{
	private static final int HUNGRY_LIMIT = 55;
	private static final int MAX_FEED = 100;
	private static final short SOUL_SHOTS = 3;
	private static final short SPIRIT_SHOTS = 2;
	
	private Pet pet;
	private PetData petData;
	private PetLevelData levelData;
	
	@BeforeEach
	void setUp()
	{
		pet = mock(Pet.class, CALLS_REAL_METHODS);
		petData = mock(PetData.class);
		levelData = mock(PetLevelData.class);
		doReturn(HUNGRY_LIMIT).when(petData).getHungryLimit();
		doReturn(MAX_FEED).when(levelData).getPetMaxFeed();
		doReturn(SOUL_SHOTS).when(levelData).getPetSoulShot();
		doReturn(SPIRIT_SHOTS).when(levelData).getPetSpiritShot();
	}
	
	@Test
	void isHungryWhenFedBelowLimit()
	{
		doReturn(petData).when(pet).getPetData();
		doReturn(levelData).when(pet).getPetLevelData();
		pet._curFed = 50;
		assertTrue(pet.isHungry());
	}
	
	@Test
	void isNotHungryWhenFedAtOrAboveLimit()
	{
		doReturn(petData).when(pet).getPetData();
		doReturn(levelData).when(pet).getPetLevelData();
		pet._curFed = 55;
		assertFalse(pet.isHungry());
	}
	
	@Test
	void isHungryReturnsFalseWhenPetDataMissing()
	{
		doReturn(null).when(pet).getPetData();
		doReturn(levelData).when(pet).getPetLevelData();
		pet._curFed = 0;
		assertFalse(pet.isHungry());
	}
	
	@Test
	void isHungryReturnsFalseWhenLevelDataMissing()
	{
		doReturn(petData).when(pet).getPetData();
		doReturn(null).when(pet).getPetLevelData();
		pet._curFed = 0;
		assertFalse(pet.isHungry());
	}
	
	@Test
	void soulShotsUseLevelDataWhenPresent()
	{
		doReturn(levelData).when(pet).getPetLevelData();
		assertEquals(SOUL_SHOTS, pet.getSoulShotsPerHit());
	}
	
	@Test
	void soulShotsReturnOneWhenLevelDataMissing()
	{
		doReturn(null).when(pet).getPetLevelData();
		assertEquals(1, pet.getSoulShotsPerHit());
	}
	
	@Test
	void spiritShotsUseLevelDataWhenPresent()
	{
		doReturn(levelData).when(pet).getPetLevelData();
		assertEquals(SPIRIT_SHOTS, pet.getSpiritShotsPerHit());
	}
	
	@Test
	void spiritShotsReturnOneWhenLevelDataMissing()
	{
		doReturn(null).when(pet).getPetLevelData();
		assertEquals(1, pet.getSpiritShotsPerHit());
	}
}
