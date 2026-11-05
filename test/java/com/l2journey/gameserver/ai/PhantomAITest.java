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
package com.l2journey.gameserver.ai;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ScheduledFuture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.PhantomConfig;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.instance.Monster;
import com.l2journey.gameserver.model.phantom.PhantomEngine;
import com.l2journey.gameserver.model.phantom.PhantomManager;
import com.l2journey.gameserver.model.phantom.PhantomMood;
import com.l2journey.gameserver.model.phantom.PhantomSpoil;
import com.l2journey.gameserver.model.phantom.PhantomState;

/**
 * Unit tests for cursed-hunt related {@link PhantomAI} helpers.
 */
class PhantomAITest
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
		PhantomEngine.isRunning = false;
		PhantomEngine.activePhantoms.clear();
		PhantomEngine.clearPendingReturns();
		PhantomAI.clearRuntimeState();
		PhantomState.clear();
		if (PhantomConfig.ROUTES.isEmpty())
		{
			PhantomConfig.ROUTES.add(new PhantomConfig.FarmZone(1, 85, 100, 200, 300, 0, 0, 0));
		}
	}
	
	@AfterEach
	void tearDown()
	{
		PhantomEngine.clearPendingReturns();
		PhantomEngine.activePhantoms.clear();
		PhantomEngine.isRunning = false;
		PhantomAI.clearRuntimeState();
		PhantomState.clear();
		if (_debugWasOn != PhantomManager.isDebugMode())
		{
			PhantomManager.toggleDebug();
		}
	}
	
	@Test
	void thinkAndFarmReturnsDuringReturnWindow()
	{
		Player bot = mock(Player.class);
		when(bot.getObjectId()).thenReturn(9);
		when(bot.isDead()).thenReturn(false);
		PhantomEngine.isRunning = true;
		PhantomEngine.activePhantoms.add(bot);
		PhantomState.RETURN_UNTIL.put(9, System.currentTimeMillis() + 5000L);
		
		PhantomAI.thinkAndFarm(bot);
		
		verify(bot, never()).getAI();
	}

	@Test
	void hasLiveLockedFarmTargetRejectsNullBot()
	{
		assertFalse(PhantomAI.hasLiveLockedFarmTarget(null));
	}
	
	@Test
	void handleCursedEarlyExitCastingWielderConsumesTick()
	{
		Player bot = mock(Player.class);
		when(bot.isCursedWeaponEquipped()).thenReturn(true);
		when(bot.isSitting()).thenReturn(false);
		when(bot.isCastingNow()).thenReturn(true);
		
		assertTrue(PhantomAI.handleCursedEarlyExit(bot));
		verify(bot, never()).getAI();
	}
	
	@Test
	void handleCursedEarlyExitNonHunterReturnsFalse()
	{
		Player bot = mock(Player.class);
		when(bot.isCursedWeaponEquipped()).thenReturn(false);
		
		assertFalse(PhantomAI.handleCursedEarlyExit(bot));
	}
	
	@Test
	void handleMoveStuckDoesNotRelocateDuringCursedActivity()
	{
		Player bot = mockMoveBot(15);
		CreatureAI ai = mock(CreatureAI.class);
		when(bot.getAI()).thenReturn(ai);
		Location pos = new Location(0, 0, 0);
		long now = System.currentTimeMillis();
		PhantomState.STUCK_TIMER.put(15, now - 60000L);
		PhantomState.LAST_POS.put(15, pos);
		
		try (MockedStatic<ThreadPool> threadPool = mockStatic(ThreadPool.class))
		{
			threadPool.when(() -> ThreadPool.schedule(any(Runnable.class), anyLong())).thenReturn(mock(ScheduledFuture.class));
			assertTrue(PhantomAI.handleMoveStuck(bot, 15, now, pos, pos, true));
			verify(bot, never()).spawnMe(anyInt(), anyInt(), anyInt());
			assertFalse(PhantomState.RETURN_UNTIL.containsKey(15));
		}
	}
	
	@Test
	void handleMoveStuckRelocatesWhenNotCursed()
	{
		Player bot = mockMoveBot(16);
		CreatureAI ai = mock(CreatureAI.class);
		when(bot.getAI()).thenReturn(ai);
		Location pos = new Location(0, 0, 0);
		long now = System.currentTimeMillis();
		PhantomState.STUCK_TIMER.put(16, now - 60000L);
		PhantomState.LAST_POS.put(16, pos);
		
		try (MockedStatic<ThreadPool> threadPool = mockStatic(ThreadPool.class))
		{
			threadPool.when(() -> ThreadPool.schedule(any(Runnable.class), anyLong())).thenReturn(mock(ScheduledFuture.class));
			assertFalse(PhantomAI.handleMoveStuck(bot, 16, now, pos, pos, false));
			assertTrue(PhantomState.RETURN_UNTIL.containsKey(16));
		}
	}
	
	@Test
	void handleIdleWatchdogSkipsRelocateDuringCursedActivity()
	{
		Player bot = mockMoveBot(17);
		CreatureAI ai = mock(CreatureAI.class);
		when(bot.getAI()).thenReturn(ai);
		when(bot.isSitting()).thenReturn(false);
		when(bot.isDead()).thenReturn(false);
		Location pos = new Location(5, 5, 5);
		long now = System.currentTimeMillis();
		PhantomState.LAST_ATTACK_TIME.put(17, now - 100000L);
		
		try (MockedStatic<ThreadPool> threadPool = mockStatic(ThreadPool.class))
		{
			threadPool.when(() -> ThreadPool.schedule(any(Runnable.class), anyLong())).thenReturn(mock(ScheduledFuture.class));
			assertTrue(PhantomAI.handleIdleWatchdog(bot, 17, now, pos, true));
			assertFalse(PhantomState.RETURN_UNTIL.containsKey(17));
		}
	}
	
	@Test
	void handleIdleWatchdogRelocatesWhenNotCursed()
	{
		Player bot = mockMoveBot(18);
		CreatureAI ai = mock(CreatureAI.class);
		when(bot.getAI()).thenReturn(ai);
		when(bot.isSitting()).thenReturn(false);
		when(bot.isDead()).thenReturn(false);
		Location pos = new Location(5, 5, 5);
		long now = System.currentTimeMillis();
		PhantomState.LAST_ATTACK_TIME.put(18, now - 100000L);
		
		try (MockedStatic<ThreadPool> threadPool = mockStatic(ThreadPool.class))
		{
			threadPool.when(() -> ThreadPool.schedule(any(Runnable.class), anyLong())).thenReturn(mock(ScheduledFuture.class));
			assertFalse(PhantomAI.handleIdleWatchdog(bot, 18, now, pos, false));
			assertTrue(PhantomState.RETURN_UNTIL.containsKey(18));
		}
	}
	
	@Test
	void skipPartyLeashHonorsHuntAndDistance()
	{
		assertTrue(PhantomAI.skipPartyLeash(true, true, false, 40, 20000));
		assertTrue(PhantomAI.skipPartyLeash(true, false, true, 20, 20000));
		assertTrue(PhantomAI.skipPartyLeash(false, false, false, 40, 5000));
		assertFalse(PhantomAI.skipPartyLeash(false, false, false, 40, 20000));
		assertFalse(PhantomAI.skipPartyLeash(true, false, true, 40, 20000));
	}
	
	@Test
	void shouldSkipTownVisitForWielderOrHunter()
	{
		assertTrue(PhantomAI.shouldSkipTownVisit(true, false));
		assertTrue(PhantomAI.shouldSkipTownVisit(false, true));
		assertTrue(PhantomAI.shouldSkipTownVisit(true, true));
		assertFalse(PhantomAI.shouldSkipTownVisit(false, false));
	}
	
	@Test
	void shouldPauseThinkForTownRestUnlessCursedActivity()
	{
		assertTrue(PhantomAI.shouldPauseThinkForTownRest(true, false, false));
		assertFalse(PhantomAI.shouldPauseThinkForTownRest(true, true, false));
		assertFalse(PhantomAI.shouldPauseThinkForTownRest(true, false, true));
		assertFalse(PhantomAI.shouldPauseThinkForTownRest(false, false, false));
	}

	@Test
	void isRangedKiterIncludesWarder()
	{
		assertTrue(PhantomAI.isRangedKiter(126));
		assertTrue(PhantomAI.isRangedKiter(9));
		assertFalse(PhantomAI.isRangedKiter(0));
	}

	@Test
	void isFarmCandidateRejectsForbiddenNames()
	{
		Monster questMob = mock(Monster.class);
		when(questMob.isDead()).thenReturn(false);
		when(questMob.isAttackable()).thenReturn(true);
		when(questMob.isRaid()).thenReturn(false);
		when(questMob.getName()).thenReturn("Quest Chest");
		when(questMob.getTemplate()).thenReturn(null);
		assertFalse(PhantomAI.isFarmCandidate(questMob));

		Monster titled = mock(Monster.class);
		com.l2journey.gameserver.model.actor.templates.NpcTemplate template = mock(com.l2journey.gameserver.model.actor.templates.NpcTemplate.class);
		when(titled.isDead()).thenReturn(false);
		when(titled.isAttackable()).thenReturn(true);
		when(titled.isRaid()).thenReturn(false);
		when(titled.getName()).thenReturn("Wolf");
		when(titled.getTemplate()).thenReturn(template);
		when(template.getTitle()).thenReturn("Quest");
		assertFalse(PhantomAI.isFarmCandidate(titled));
	}

	@Test
	void isCommittedToTargetRequiresAttackOrCast()
	{
		Monster mob = mock(Monster.class);
		Player idle = mock(Player.class);
		CreatureAI ai = mock(CreatureAI.class);
		when(idle.getTarget()).thenReturn(mob);
		when(idle.isAttackingNow()).thenReturn(false);
		when(idle.isCastingNow()).thenReturn(false);
		when(idle.getAI()).thenReturn(ai);
		when(ai.getIntention()).thenReturn(Intention.IDLE);
		assertFalse(PhantomAI.isCommittedToTarget(idle, mob));
		assertFalse(PhantomAI.isCommittedToTarget(null, mob));
		assertFalse(PhantomAI.isCommittedToTarget(idle, null));

		Player wrongTarget = mock(Player.class);
		when(wrongTarget.getTarget()).thenReturn(null);
		assertFalse(PhantomAI.isCommittedToTarget(wrongTarget, mob));

		when(idle.isAttackingNow()).thenReturn(true);
		assertTrue(PhantomAI.isCommittedToTarget(idle, mob));

		when(idle.isAttackingNow()).thenReturn(false);
		when(idle.isCastingNow()).thenReturn(true);
		assertTrue(PhantomAI.isCommittedToTarget(idle, mob));

		when(idle.isCastingNow()).thenReturn(false);
		when(ai.getIntention()).thenReturn(Intention.ATTACK);
		assertTrue(PhantomAI.isCommittedToTarget(idle, mob));

		when(ai.getIntention()).thenReturn(Intention.CAST);
		assertTrue(PhantomAI.isCommittedToTarget(idle, mob));
	}

	@Test
	void resolveLockedFarmTargetDropsWeakPrey()
	{
		Player bot = mock(Player.class);
		when(bot.getObjectId()).thenReturn(90);
		when(bot.getLevel()).thenReturn(61);
		Monster gremlin = mock(Monster.class);
		when(gremlin.isDead()).thenReturn(false);
		when(gremlin.isSpawned()).thenReturn(true);
		when(gremlin.isAttackable()).thenReturn(true);
		when(gremlin.getLevel()).thenReturn(1);
		PhantomAI.rememberFarmTarget(90, gremlin);

		try (MockedStatic<PhantomSpoil> spoil = mockStatic(PhantomSpoil.class))
		{
			spoil.when(() -> PhantomSpoil.shouldSweepCorpse(bot, gremlin)).thenReturn(false);
			assertNull(PhantomAI.resolveLockedFarmTarget(bot, 90));
			assertNull(PhantomAI.resolveLockedFarmTarget(bot, 90));
		}
	}

	@Test
	void isFarmCandidateAcceptsValidMonster()
	{
		assertFalse(PhantomAI.isFarmCandidate(null));

		Monster dead = mock(Monster.class);
		when(dead.isDead()).thenReturn(true);
		assertFalse(PhantomAI.isFarmCandidate(dead));

		Monster notAttackable = mock(Monster.class);
		when(notAttackable.isDead()).thenReturn(false);
		when(notAttackable.isAttackable()).thenReturn(false);
		assertFalse(PhantomAI.isFarmCandidate(notAttackable));

		Monster raid = mock(Monster.class);
		when(raid.isDead()).thenReturn(false);
		when(raid.isAttackable()).thenReturn(true);
		when(raid.isRaid()).thenReturn(true);
		assertFalse(PhantomAI.isFarmCandidate(raid));

		Monster ok = mock(Monster.class);
		when(ok.isDead()).thenReturn(false);
		when(ok.isAttackable()).thenReturn(true);
		when(ok.isRaid()).thenReturn(false);
		when(ok.getName()).thenReturn("Wolf");
		when(ok.getTemplate()).thenReturn(null);
		assertTrue(PhantomAI.isFarmCandidate(ok));
	}

	@Test
	void isTargetedByAnotherDetectsCommittedHolders()
	{
		Monster mob = mock(Monster.class);
		Player bot = mock(Player.class);
		when(bot.getSummon()).thenReturn(null);

		when(mob.getTarget()).thenReturn(bot);
		assertFalse(PhantomAI.isTargetedByAnother(bot, mob));

		Player holder = mock(Player.class);
		CreatureAI holderAi = mock(CreatureAI.class);
		when(holder.isPlayer()).thenReturn(true);
		when(holder.asPlayer()).thenReturn(holder);
		when(holder.getTarget()).thenReturn(mob);
		when(holder.isAttackingNow()).thenReturn(true);
		when(holder.isCastingNow()).thenReturn(false);
		when(holder.getAI()).thenReturn(holderAi);
		when(mob.getTarget()).thenReturn(holder);

		try (MockedStatic<com.l2journey.gameserver.model.World> world = mockStatic(com.l2journey.gameserver.model.World.class))
		{
			com.l2journey.gameserver.model.World worldInstance = mock(com.l2journey.gameserver.model.World.class);
			world.when(com.l2journey.gameserver.model.World::getInstance).thenReturn(worldInstance);
			when(worldInstance.getVisibleObjectsInRange(mob, Player.class, 1000)).thenReturn(java.util.List.of());
			assertTrue(PhantomAI.isTargetedByAnother(bot, mob));

			when(holder.isAttackingNow()).thenReturn(false);
			when(holderAi.getIntention()).thenReturn(Intention.IDLE);
			assertFalse(PhantomAI.isTargetedByAnother(bot, mob));

			Player nearby = mock(Player.class);
			CreatureAI nearbyAi = mock(CreatureAI.class);
			when(nearby.isAlikeDead()).thenReturn(false);
			when(nearby.getTarget()).thenReturn(mob);
			when(nearby.isAttackingNow()).thenReturn(false);
			when(nearby.isCastingNow()).thenReturn(false);
			when(nearby.getAI()).thenReturn(nearbyAi);
			when(nearbyAi.getIntention()).thenReturn(Intention.ATTACK);
			when(mob.getTarget()).thenReturn(null);
			when(worldInstance.getVisibleObjectsInRange(mob, Player.class, 1000)).thenReturn(java.util.List.of(nearby));
			assertTrue(PhantomAI.isTargetedByAnother(bot, mob));
		}
	}

	@Test
	void unregisterBotClearsCasualChatAndMood()
	{
		Player bot = mock(Player.class);
		when(bot.getObjectId()).thenReturn(77);
		PhantomMood.onGoodThing(bot);
		assertTrue(PhantomMood.getMood(bot) == PhantomMood.Mood.CHEERFUL);
		PhantomAI.unregisterBot(77);
		assertTrue(PhantomMood.getMood(bot) == PhantomMood.Mood.NEUTRAL);
		PhantomAI.clearRuntimeState();
	}

	@Test
	void thinkAndFarmReturnsWhenMoodPauses()
	{
		Player bot = mock(Player.class);
		when(bot.getObjectId()).thenReturn(44);
		when(bot.isDead()).thenReturn(false);
		when(bot.isInCombat()).thenReturn(true);
		when(bot.getCurrentHp()).thenReturn(100.0);
		when(bot.getMaxHp()).thenReturn(100);
		PhantomEngine.isRunning = true;
		PhantomEngine.activePhantoms.add(bot);
		assertTrue(PhantomMood.canReactNow(bot));
		assertTrue(PhantomMood.shouldPauseThink(bot));
		PhantomAI.thinkAndFarm(bot);
		verify(bot, never()).getAI();
		PhantomMood.clear();
	}
	
	private static Player mockMoveBot(int objectId)
	{
		Player bot = mock(Player.class);
		when(bot.getObjectId()).thenReturn(objectId);
		when(bot.getName()).thenReturn("Bot" + objectId);
		when(bot.getLevel()).thenReturn(40);
		when(bot.isOnline()).thenReturn(true);
		return bot;
	}
}
