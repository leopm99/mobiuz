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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.instance.Monster;
import com.l2journey.gameserver.model.groups.Party;
import com.l2journey.gameserver.model.phantom.PhantomChat;
import com.l2journey.gameserver.model.phantom.PhantomCursedHunt;
import com.l2journey.gameserver.model.phantom.PhantomEngine;
import com.l2journey.gameserver.model.phantom.PhantomGeo;
import com.l2journey.gameserver.model.phantom.PhantomMood;
import com.l2journey.gameserver.model.phantom.PhantomState;
import com.l2journey.gameserver.model.phantom.PhantomWalk;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.zone.ZoneId;

/**
 * Unit tests for the October phantom spoil and Inquisitor decisions in {@link PhantomAI}.
 */
class PhantomAIOctoberTest
{
	@AfterEach
	void tearDown()
	{
		PhantomAI.clearRuntimeState();
		PhantomChat.clearRuntimeState();
	}

	@Test
	void greetWhileRestingOnlyTalksInPeace()
	{
		PhantomAI.greetWhileRestingInTown(null);

		Player quiet = mock(Player.class);
		when(quiet.isInsideZone(ZoneId.PEACE)).thenReturn(false);
		PhantomAI.greetWhileRestingInTown(quiet);

		Player resting = mock(Player.class);
		when(resting.isInsideZone(ZoneId.PEACE)).thenReturn(true);
		when(resting.isDead()).thenReturn(true);
		PhantomAI.greetWhileRestingInTown(resting);
	}

	@Test
	void sweepBeforeLootKeepsOrDropsTheLockedCorpse()
	{
		Player bot = spoiler();
		Monster corpse = spoiledCorpse(bot);
		Skill sweep = mock(Skill.class);
		CreatureAI ai = mock(CreatureAI.class);
		when(bot.getKnownSkill(42)).thenReturn(sweep);
		when(bot.isSkillDisabled(sweep)).thenReturn(false);
		when(bot.getAI()).thenReturn(ai);
		when(bot.isMoving()).thenReturn(false);
		when(bot.getX()).thenReturn(0);
		when(bot.getY()).thenReturn(0);
		when(bot.getZ()).thenReturn(0);
		when(corpse.getX()).thenReturn(40);
		when(corpse.getY()).thenReturn(0);
		when(corpse.getZ()).thenReturn(0);

		PhantomAI.rememberFarmTarget(7, corpse);
		assertTrue(PhantomAI.consumeSweepBeforeLoot(bot, 7));
		assertSame(corpse, PhantomAI.resolveLockedFarmTarget(bot, 7));

		when(bot.getKnownSkill(42)).thenReturn(sweep);
		PhantomAI.rememberFarmTarget(7, corpse);
		assertTrue(PhantomAI.consumeDeadFarmTarget(bot, 7, corpse));
		assertSame(corpse, PhantomAI.resolveLockedFarmTarget(bot, 7));

		when(bot.getKnownSkill(42)).thenReturn(null);
		assertFalse(PhantomAI.consumeSweepBeforeLoot(bot, 7));
		assertNull(PhantomAI.resolveLockedFarmTarget(bot, 7));

		Monster foreign = spoiledCorpse(bot);
		when(foreign.checkSpoilOwner(bot, false)).thenReturn(false);
		when(foreign.isSpoiled()).thenReturn(false);
		when(foreign.isSweepActive()).thenReturn(false);
		PhantomAI.rememberFarmTarget(7, foreign);
		assertFalse(PhantomAI.consumeSweepBeforeLoot(bot, 7));
		assertNull(PhantomAI.resolveLockedFarmTarget(bot, 7));

		PhantomAI.rememberFarmTarget(7, null);
		assertFalse(PhantomAI.consumeDeadFarmTarget(bot, 7, null));
		assertFalse(PhantomAI.consumeDeadFarmTarget(bot, 7, liveMonster()));
	}

	@Test
	void townRestAndPartyLeashStopTheThinkTick()
	{
		Player bot = mock(Player.class);
		when(bot.isDead()).thenReturn(false);
		when(bot.isOnline()).thenReturn(false);
		when(bot.getObjectId()).thenReturn(42);
		when(bot.isCursedWeaponEquipped()).thenReturn(false);
		when(bot.isInsideZone(ZoneId.PEACE)).thenReturn(true);
		when(bot.getX()).thenReturn(0);
		when(bot.getY()).thenReturn(0);
		when(bot.getZ()).thenReturn(0);
		when(bot.getSummon()).thenReturn(null);
		when(bot.getLevel()).thenReturn(40);

		boolean wasRunning = PhantomEngine.isRunning;
		PhantomEngine.isRunning = true;
		PhantomEngine.activePhantoms.add(bot);
		try (MockedStatic<PhantomMood> mood = mockStatic(PhantomMood.class);
			MockedStatic<PhantomWalk> walk = mockStatic(PhantomWalk.class);
			MockedStatic<PhantomCursedHunt> hunt = mockStatic(PhantomCursedHunt.class))
		{
			mood.when(() -> PhantomMood.shouldPauseThink(any())).thenReturn(false);
			hunt.when(PhantomCursedHunt::hasActiveWeapon).thenReturn(false);
			walk.when(() -> PhantomWalk.update(any(), anyBoolean())).thenAnswer(invocation -> null);
			walk.when(() -> PhantomWalk.isResting(any())).thenReturn(true);
			PhantomAI.thinkAndFarm(bot);

			walk.when(() -> PhantomWalk.isResting(any())).thenReturn(false);
			Party party = mock(Party.class);
			Player leader = mock(Player.class);
			when(bot.getParty()).thenReturn(party);
			when(party.getLeader()).thenReturn(leader);
			when(leader.getX()).thenReturn(20000);
			when(leader.getY()).thenReturn(0);
			when(leader.getZ()).thenReturn(0);
			PhantomAI.thinkAndFarm(bot);
			verify(bot).teleToLocation(20000, 0, 0);
		}
		finally
		{
			PhantomEngine.activePhantoms.remove(bot);
			PhantomEngine.isRunning = wasRunning;
			PhantomState.unregister(42);
		}
	}

	@Test
	void farmLootSweepReturnsBeforeSearching() throws Exception
	{
		Player bot = spoiler();
		Monster corpse = spoiledCorpse(bot);
		Skill sweep = mock(Skill.class);
		CreatureAI ai = mock(CreatureAI.class);
		when(bot.getKnownSkill(42)).thenReturn(sweep);
		when(bot.isSkillDisabled(sweep)).thenReturn(false);
		when(bot.getAI()).thenReturn(ai);
		when(bot.isMoving()).thenReturn(false);
		when(bot.getX()).thenReturn(0);
		when(bot.getY()).thenReturn(0);
		when(bot.getZ()).thenReturn(0);
		when(corpse.getX()).thenReturn(40);
		when(corpse.getY()).thenReturn(0);
		when(corpse.getZ()).thenReturn(0);
		PhantomAI.rememberFarmTarget(7, corpse);
		invokeFarmLoot(bot, 7);
		assertSame(corpse, PhantomAI.resolveLockedFarmTarget(bot, 7));

		PhantomAI.rememberFarmTarget(7, null);
		when(ai.getIntention()).thenReturn(Intention.IDLE);
		World worldInstance = mock(World.class);
		try (MockedStatic<World> world = mockStatic(World.class))
		{
			world.when(World::getInstance).thenReturn(worldInstance);
			when(worldInstance.getVisibleObjectsInRange(any(), any(), anyInt())).thenReturn(List.of());
			invokeFarmLoot(bot, 7);
		}
		verify(ai).setIntention(Intention.ACTIVE);
		PhantomState.unregister(7);
	}

	@Test
	void attackTargetUsesSpoilThenInquisitorBeforeAWand()
	{
		Monster live = liveMonster();
		when(live.isMonster()).thenReturn(true);
		when(live.isSpoiled()).thenReturn(false);
		when(live.isPlayer()).thenReturn(false);
		when(live.getX()).thenReturn(0);
		when(live.getY()).thenReturn(0);
		when(live.getZ()).thenReturn(0);

		try (MockedStatic<PhantomGeo> geo = mockStatic(PhantomGeo.class))
		{
			geo.when(() -> PhantomGeo.canSeeTarget(any(), any())).thenReturn(true);

			Player spoiler = spoiler();
			Skill spoil = mock(Skill.class);
			CreatureAI spoilerAi = mock(CreatureAI.class);
			when(spoil.getMpConsume()).thenReturn(10);
			when(spoiler.getKnownSkill(254)).thenReturn(spoil);
			when(spoiler.isSkillDisabled(spoil)).thenReturn(false);
			when(spoiler.getCurrentMp()).thenReturn(100.0);
			when(spoiler.getAI()).thenReturn(spoilerAi);
			when(spoiler.getX()).thenReturn(0);
			when(spoiler.getY()).thenReturn(0);
			when(spoiler.getZ()).thenReturn(0);
			PhantomAI.attackTarget(spoiler, live);
			verify(spoilerAi).setIntention(Intention.CAST, spoil, live);

			Player bishop = mock(Player.class);
			CreatureAI bishopAi = mock(CreatureAI.class);
			Skill surrender = mock(Skill.class);
			when(bishop.getActiveClass()).thenReturn(16);
			when(bishop.getObjectId()).thenReturn(16);
			when(bishop.isCursedWeaponEquipped()).thenReturn(false);
			when(bishop.isDead()).thenReturn(false);
			when(bishop.isCastingNow()).thenReturn(false);
			when(bishop.isInStance()).thenReturn(true);
			when(bishop.getCurrentMp()).thenReturn(100.0);
			when(bishop.getAI()).thenReturn(bishopAi);
			when(bishopAi.getIntention()).thenReturn(Intention.ACTIVE);
			when(bishop.getX()).thenReturn(0);
			when(bishop.getY()).thenReturn(0);
			when(bishop.getZ()).thenReturn(0);
			when(surrender.getMpConsume()).thenReturn(10);
			when(bishop.getTransformSkill(1524)).thenReturn(surrender);
			when(bishop.getTransformSkill(1523)).thenReturn(null);
			when(bishop.isSkillDisabled(surrender)).thenReturn(false);
			when(live.isAffectedBySkill(1524)).thenReturn(false);
			PhantomAI.attackTarget(bishop, live);
			verify(bishopAi).setIntention(Intention.CAST, surrender, live);

			when(bishop.isInStance()).thenReturn(false);
			when(bishop.isMageClass()).thenReturn(true);
			when(bishop.getLevel()).thenReturn(40);
			when(bishop.getCurrentMpPercent()).thenReturn(100);
			when(bishop.getAllSkills()).thenReturn(List.of());
			PhantomAI.attackTarget(bishop, live);
			verify(bishopAi).setIntention(Intention.ATTACK, live);
		}
	}

	@Test
	void deadFarmTargetIsSweptOrForgotten()
	{
		Player bot = spoiler();
		Monster corpse = spoiledCorpse(bot);
		when(bot.getKnownSkill(42)).thenReturn(null);
		PhantomAI.rememberFarmTarget(8, corpse);
		assertTrue(PhantomAI.consumeDeadFarmTarget(bot, 8, corpse));
		assertNull(PhantomAI.resolveLockedFarmTarget(bot, 8));

		Skill sweep = mock(Skill.class);
		when(bot.getKnownSkill(42)).thenReturn(sweep);
		when(bot.isSkillDisabled(sweep)).thenReturn(true);
		PhantomAI.rememberFarmTarget(8, corpse);
		assertTrue(PhantomAI.consumeSweepBeforeLoot(bot, 8));
		assertSame(corpse, PhantomAI.resolveLockedFarmTarget(bot, 8));

		when(bot.isSkillDisabled(sweep)).thenReturn(false);
		when(bot.isCastingNow()).thenReturn(true);
		assertTrue(PhantomAI.consumeDeadFarmTarget(bot, 8, corpse));
		assertSame(corpse, PhantomAI.resolveLockedFarmTarget(bot, 8));
	}

	@Test
	void spoilAndInquisitorCastsConsumeTheAttack()
	{
		assertFalse(PhantomAI.castSpoilOrInquisitor(null, mock(Monster.class)));
		assertFalse(PhantomAI.castSpoilOrInquisitor(spoiler(), null));
		assertFalse(PhantomAI.castInquisitorAttack(null, mock(Monster.class)));

		Player spoiler = spoiler();
		Monster live = liveMonster();
		Skill spoil = mock(Skill.class);
		when(spoil.getMpConsume()).thenReturn(10);
		CreatureAI ai = mock(CreatureAI.class);
		when(spoiler.getKnownSkill(254)).thenReturn(spoil);
		when(spoiler.isSkillDisabled(spoil)).thenReturn(false);
		when(spoiler.getCurrentMp()).thenReturn(100.0);
		when(spoiler.getAI()).thenReturn(ai);
		when(live.isMonster()).thenReturn(true);
		when(live.isSpoiled()).thenReturn(false);
		assertTrue(PhantomAI.castSpoilOrInquisitor(spoiler, live));

		Player bishop = mock(Player.class);
		Skill form = mock(Skill.class);
		when(form.getMpConsume()).thenReturn(10);
		when(bishop.isCursedWeaponEquipped()).thenReturn(false);
		when(bishop.isDead()).thenReturn(false);
		when(bishop.isCastingNow()).thenReturn(false);
		when(bishop.isInStance()).thenReturn(false);
		when(bishop.getKnownSkill(1520)).thenReturn(form);
		when(bishop.isSkillDisabled(form)).thenReturn(false);
		when(bishop.getCurrentMp()).thenReturn(100.0);
		when(bishop.getAI()).thenReturn(ai);
		when(bishop.getActiveClass()).thenReturn(16);
		assertTrue(PhantomAI.castSpoilOrInquisitor(bishop, live));

		when(bishop.isCursedWeaponEquipped()).thenReturn(true);
		assertFalse(PhantomAI.castSpoilOrInquisitor(bishop, live));
		assertFalse(PhantomAI.castInquisitorAttack(bishop, live));

		when(bishop.isCursedWeaponEquipped()).thenReturn(false);
		when(bishop.isInStance()).thenReturn(true);
		Skill surrender = mock(Skill.class);
		when(surrender.getMpConsume()).thenReturn(10);
		when(bishop.getTransformSkill(1524)).thenReturn(surrender);
		when(bishop.getTransformSkill(1523)).thenReturn(null);
		when(bishop.isSkillDisabled(surrender)).thenReturn(false);
		when(bishop.getAI()).thenReturn(ai);
		when(ai.getIntention()).thenReturn(Intention.ACTIVE);
		when(live.isDead()).thenReturn(false);
		when(live.isAffectedBySkill(1524)).thenReturn(false);
		assertTrue(PhantomAI.castInquisitorAttack(bishop, live));
	}

	private static void invokeFarmLoot(Player bot, int botId) throws Exception
	{
		Method method = PhantomAI.class.getDeclaredMethod("handleFarmLootOrIdle", Player.class, int.class);
		method.setAccessible(true);
		method.invoke(null, bot, botId);
	}

	private static Player spoiler()
	{
		Player bot = mock(Player.class);
		when(bot.getActiveClass()).thenReturn(55);
		when(bot.getObjectId()).thenReturn(7);
		return bot;
	}

	private static Monster spoiledCorpse(Player bot)
	{
		Monster mob = mock(Monster.class);
		when(mob.isDead()).thenReturn(true);
		when(mob.isSpawned()).thenReturn(true);
		when(mob.isSpoiled()).thenReturn(true);
		when(mob.isSweepActive()).thenReturn(false);
		when(mob.checkSpoilOwner(bot, false)).thenReturn(true);
		when(mob.isAttackable()).thenReturn(true);
		return mob;
	}

	private static Monster liveMonster()
	{
		Monster mob = mock(Monster.class);
		when(mob.isDead()).thenReturn(false);
		return mob;
	}
}
