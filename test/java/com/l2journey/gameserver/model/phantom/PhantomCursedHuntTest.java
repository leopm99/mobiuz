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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.actor.Player;

/**
 * Unit tests for {@link PhantomCursedHunt} decision helpers.
 */
class PhantomCursedHuntTest
{
	@AfterEach
	void tearDown()
	{
		PhantomEngine.activePhantoms.clear();
		PhantomCursedHunt.clear();
	}
	
	@Test
	void rankSquadCapsByLimitAndOrdersByLevelDistance()
	{
		Player exact = mockPlayer(1, 40);
		Player nearHighId = mockPlayer(30, 41);
		Player nearLowId = mockPlayer(20, 41);
		Player far = mockPlayer(10, 50);
		
		List<Player> ranked = PhantomCursedHunt.rankSquad(List.of(exact, nearHighId, nearLowId, far), 40, -1, 3);
		
		assertEquals(3, ranked.size());
		assertEquals(1, ranked.get(0).getObjectId());
		assertEquals(20, ranked.get(1).getObjectId());
		assertEquals(30, ranked.get(2).getObjectId());
	}
	
	@Test
	void rankSquadExcludesWielderAndHonorsZeroLimit()
	{
		Player wielder = mockPlayer(5, 40);
		Player other = mockPlayer(6, 40);
		
		assertTrue(PhantomCursedHunt.rankSquad(List.of(wielder, other), 40, 5, 0).isEmpty());
		assertEquals(List.of(other), PhantomCursedHunt.rankSquad(List.of(wielder, other), 40, 5, 8));
	}
	
	@Test
	void isSquadCandidateRejectsUnderleveledDeadOfflineWielderAndCursed()
	{
		Player under = mockPlayer(1, 20);
		when(under.isOnline()).thenReturn(true);
		when(under.isDead()).thenReturn(false);
		PhantomEngine.activePhantoms.add(under);
		assertFalse(PhantomCursedHunt.isSquadCandidate(under, -1));
		
		Player ok = mockPlayer(2, 21);
		when(ok.isOnline()).thenReturn(true);
		when(ok.isDead()).thenReturn(false);
		when(ok.isCursedWeaponEquipped()).thenReturn(false);
		PhantomEngine.activePhantoms.add(ok);
		assertTrue(PhantomCursedHunt.isSquadCandidate(ok, -1));
		assertFalse(PhantomCursedHunt.isSquadCandidate(ok, 2));
		
		when(ok.isCursedWeaponEquipped()).thenReturn(true);
		assertFalse(PhantomCursedHunt.isSquadCandidate(ok, -1));
		
		when(ok.isCursedWeaponEquipped()).thenReturn(false);
		when(ok.isDead()).thenReturn(true);
		assertFalse(PhantomCursedHunt.isLivingHunterCandidate(ok));
		when(ok.isDead()).thenReturn(false);
		when(ok.isOnline()).thenReturn(false);
		assertFalse(PhantomCursedHunt.isLivingHunterCandidate(ok));
	}
	
	@Test
	void chooseLooterPrefersKillerThenClosest()
	{
		Map<Integer, Double> distances = new HashMap<>();
		distances.put(10, 500.0);
		distances.put(20, 100.0);
		
		assertEquals(99, PhantomCursedHunt.chooseLooterId(99, true, distances));
		assertEquals(20, PhantomCursedHunt.chooseLooterId(99, false, distances));
		assertEquals(-1, PhantomCursedHunt.chooseLooterId(99, false, Map.of()));
	}
	
	@Test
	void preferRealTargetWhenAvailable()
	{
		assertTrue(PhantomCursedHunt.preferRealTarget(true));
		assertFalse(PhantomCursedHunt.preferRealTarget(false));
	}
	
	@Test
	void passesWielderTargetRulesRejectsRestrictedCases()
	{
		assertTrue(PhantomCursedHunt.passesWielderTargetRules(false, false, false, false, true, false, false));
		assertFalse(PhantomCursedHunt.passesWielderTargetRules(true, false, false, false, true, false, false));
		assertFalse(PhantomCursedHunt.passesWielderTargetRules(false, true, false, false, true, false, false));
		assertFalse(PhantomCursedHunt.passesWielderTargetRules(false, false, true, false, true, false, false));
		assertFalse(PhantomCursedHunt.passesWielderTargetRules(false, false, false, true, true, false, false));
		assertFalse(PhantomCursedHunt.passesWielderTargetRules(false, false, false, false, false, false, false));
		assertFalse(PhantomCursedHunt.passesWielderTargetRules(false, false, false, false, true, true, false));
		assertFalse(PhantomCursedHunt.passesWielderTargetRules(false, false, false, false, true, false, true));
	}
	
	@Test
	void noteWeaponActivatedHoldsOnlyForPhantomOwner()
	{
		long now = System.currentTimeMillis();
		PhantomCursedHunt.noteWeaponActivated(8190, 55, true);
		assertTrue(PhantomCursedHunt.holdsForPhantomWielder(8190, now + 1000L, true));
		assertFalse(PhantomCursedHunt.holdsForPhantomWielder(8190, now + 1000L, false));
		
		PhantomCursedHunt.noteWeaponActivated(8190, 55, false);
		assertFalse(PhantomCursedHunt.holdsForPhantomWielder(8190, now + 1000L, true));
	}
	
	@Test
	void shouldYieldChaseToFarmingInPeace()
	{
		assertTrue(PhantomCursedHunt.shouldYieldChaseToFarming(true));
		assertFalse(PhantomCursedHunt.shouldYieldChaseToFarming(false));
	}
	
	@Test
	void unregisterBotDropsDesignatedLooter()
	{
		PhantomCursedHunt.DESIGNATED_LOOTER.put(8190, 42);
		PhantomCursedHunt.unregisterBot(42);
		assertFalse(PhantomCursedHunt.DESIGNATED_LOOTER.containsKey(8190));
	}
	
	@Test
	void clearResetsActiveWeaponCache()
	{
		PhantomCursedHunt.clear();
		assertFalse(PhantomCursedHunt.hasActiveWeapon());
	}
	
	private static Player mockPlayer(int objectId, int level)
	{
		Player p = mock(Player.class);
		when(p.getObjectId()).thenReturn(objectId);
		when(p.getLevel()).thenReturn(level);
		return p;
	}
}
