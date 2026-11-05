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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.ai.CreatureAI;
import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.instance.Monster;
import com.l2journey.gameserver.model.skill.Skill;

/**
 * Unit tests for {@link PhantomSpoil}.
 */
class PhantomSpoilTest
{
	@Test
	void isSpoilerRecognizesScavengerLine()
	{
		Player scavenger = mock(Player.class);
		when(scavenger.getActiveClass()).thenReturn(54);
		assertTrue(PhantomSpoil.isSpoiler(scavenger));

		Player bounty = mock(Player.class);
		when(bounty.getActiveClass()).thenReturn(55);
		assertTrue(PhantomSpoil.isSpoiler(bounty));

		Player fortune = mock(Player.class);
		when(fortune.getActiveClass()).thenReturn(117);
		assertTrue(PhantomSpoil.isSpoiler(fortune));

		Player other = mock(Player.class);
		when(other.getActiveClass()).thenReturn(1);
		assertFalse(PhantomSpoil.isSpoiler(other));
		assertFalse(PhantomSpoil.isSpoiler(null));
	}

	@Test
	void shouldSweepCorpseRequiresOwnerAndSpoilState()
	{
		Player bot = mock(Player.class);
		when(bot.getActiveClass()).thenReturn(55);
		Monster mob = mock(Monster.class);
		when(mob.isDead()).thenReturn(true);
		when(mob.isSpawned()).thenReturn(true);
		when(mob.isSpoiled()).thenReturn(true);
		when(mob.isSweepActive()).thenReturn(false);
		when(mob.checkSpoilOwner(bot, false)).thenReturn(true);

		assertTrue(PhantomSpoil.shouldSweepCorpse(bot, mob));

		when(mob.checkSpoilOwner(bot, false)).thenReturn(false);
		assertFalse(PhantomSpoil.shouldSweepCorpse(bot, mob));

		when(mob.checkSpoilOwner(bot, false)).thenReturn(true);
		when(mob.isSpoiled()).thenReturn(false);
		when(mob.isSweepActive()).thenReturn(false);
		assertFalse(PhantomSpoil.shouldSweepCorpse(bot, mob));
	}

	@Test
	void trySpoilCastsWhenReadyOnLiveUnspoiledMonster()
	{
		Player bot = mock(Player.class);
		CreatureAI ai = mock(CreatureAI.class);
		Monster mob = mock(Monster.class);
		Skill spoil = mock(Skill.class);
		when(spoil.getMpConsume()).thenReturn(10);
		when(bot.getActiveClass()).thenReturn(54);
		when(bot.getKnownSkill(254)).thenReturn(spoil);
		when(bot.isSkillDisabled(spoil)).thenReturn(false);
		when(bot.getCurrentMp()).thenReturn(100.0);
		when(bot.getAI()).thenReturn(ai);
		when(mob.isMonster()).thenReturn(true);
		when(mob.isDead()).thenReturn(false);
		when(mob.isSpoiled()).thenReturn(false);

		assertTrue(PhantomSpoil.trySpoil(bot, mob));
		verify(ai).setIntention(eq(Intention.CAST), eq(spoil), eq(mob));
	}

	@Test
	void trySpoilSkipsAlreadySpoiledOrWrongClass()
	{
		Player fighter = mock(Player.class);
		when(fighter.getActiveClass()).thenReturn(1);
		Monster mob = mock(Monster.class);
		when(mob.isMonster()).thenReturn(true);
		when(mob.isDead()).thenReturn(false);
		assertFalse(PhantomSpoil.trySpoil(fighter, mob));

		Player bot = mock(Player.class);
		when(bot.getActiveClass()).thenReturn(55);
		when(mob.isSpoiled()).thenReturn(true);
		assertFalse(PhantomSpoil.trySpoil(bot, mob));
	}

	@Test
	void trySweepApproachesThenCastsWhenClose()
	{
		Player bot = mock(Player.class);
		CreatureAI ai = mock(CreatureAI.class);
		Monster mob = mock(Monster.class);
		Skill sweep = mock(Skill.class);
		Location far = new Location(1000, 0, 0);
		when(bot.getActiveClass()).thenReturn(55);
		when(bot.getKnownSkill(42)).thenReturn(sweep);
		when(bot.isSkillDisabled(sweep)).thenReturn(false);
		when(bot.getAI()).thenReturn(ai);
		when(bot.getX()).thenReturn(0);
		when(bot.getY()).thenReturn(0);
		when(bot.getZ()).thenReturn(0);
		when(bot.isMoving()).thenReturn(false);
		when(mob.isDead()).thenReturn(true);
		when(mob.isSpawned()).thenReturn(true);
		when(mob.isSpoiled()).thenReturn(true);
		when(mob.isSweepActive()).thenReturn(false);
		when(mob.checkSpoilOwner(bot, false)).thenReturn(true);
		when(mob.getX()).thenReturn(1000);
		when(mob.getY()).thenReturn(0);
		when(mob.getZ()).thenReturn(0);
		when(mob.getLocation()).thenReturn(far);

		assertTrue(PhantomSpoil.trySweep(bot, mob));
		verify(ai).setIntention(eq(Intention.MOVE_TO), eq(far));

		when(mob.getX()).thenReturn(50);
		when(mob.getLocation()).thenReturn(new Location(50, 0, 0));
		assertTrue(PhantomSpoil.trySweep(bot, mob));
		verify(ai).setIntention(eq(Intention.CAST), eq(sweep), eq(mob));
	}

	@Test
	void trySweepRejectsUnspawnedUnspoiledOrMissingSkill()
	{
		Player bot = mock(Player.class);
		when(bot.getActiveClass()).thenReturn(54);
		Monster mob = mock(Monster.class);
		when(mob.isDead()).thenReturn(true);
		when(mob.isSpawned()).thenReturn(false);
		assertFalse(PhantomSpoil.trySweep(bot, mob));

		when(mob.isSpawned()).thenReturn(true);
		when(mob.isSpoiled()).thenReturn(false);
		when(mob.isSweepActive()).thenReturn(false);
		assertFalse(PhantomSpoil.trySweep(bot, mob));

		when(mob.isSpoiled()).thenReturn(true);
		when(mob.checkSpoilOwner(bot, false)).thenReturn(true);
		when(bot.getKnownSkill(42)).thenReturn(null);
		assertFalse(PhantomSpoil.trySweep(bot, mob));

		Skill sweep = mock(Skill.class);
		when(bot.getKnownSkill(42)).thenReturn(sweep);
		when(bot.isSkillDisabled(sweep)).thenReturn(true);
		assertFalse(PhantomSpoil.trySweep(bot, mob));
	}

	@Test
	void trySweepRejectsForeignSpoil()
	{
		Player bot = mock(Player.class);
		Monster mob = mock(Monster.class);
		when(bot.getActiveClass()).thenReturn(117);
		when(mob.isDead()).thenReturn(true);
		when(mob.isSpawned()).thenReturn(true);
		when(mob.isSpoiled()).thenReturn(true);
		when(mob.checkSpoilOwner(bot, false)).thenReturn(false);
		assertFalse(PhantomSpoil.trySweep(bot, mob));
	}

	@Test
	void trySpoilAndSweepHandleDisabledSkillAndMoving()
	{
		Player bot = mock(Player.class);
		CreatureAI ai = mock(CreatureAI.class);
		Monster mob = mock(Monster.class);
		Skill spoil = mock(Skill.class);
		Skill sweep = mock(Skill.class);
		when(spoil.getMpConsume()).thenReturn(50);
		when(bot.getActiveClass()).thenReturn(54);
		when(bot.getKnownSkill(254)).thenReturn(spoil);
		when(bot.isSkillDisabled(spoil)).thenReturn(true);
		when(bot.getCurrentMp()).thenReturn(100.0);
		when(mob.isMonster()).thenReturn(true);
		when(mob.isDead()).thenReturn(false);
		when(mob.isSpoiled()).thenReturn(false);
		assertTrue(PhantomSpoil.trySpoil(bot, mob));

		when(bot.isCastingNow()).thenReturn(true);
		assertTrue(PhantomSpoil.trySpoil(bot, mob));
		when(bot.isCastingNow()).thenReturn(false);

		when(bot.isSkillDisabled(spoil)).thenReturn(false);
		when(bot.getCurrentMp()).thenReturn(10.0);
		assertFalse(PhantomSpoil.trySpoil(bot, mob));

		when(bot.getKnownSkill(42)).thenReturn(sweep);
		when(bot.isSkillDisabled(sweep)).thenReturn(false);
		when(bot.getAI()).thenReturn(ai);
		when(bot.getX()).thenReturn(0);
		when(bot.getY()).thenReturn(0);
		when(bot.getZ()).thenReturn(0);
		when(bot.isMoving()).thenReturn(true);
		when(mob.isDead()).thenReturn(true);
		when(mob.isSpawned()).thenReturn(true);
		when(mob.isSpoiled()).thenReturn(false);
		when(mob.isSweepActive()).thenReturn(true);
		when(mob.checkSpoilOwner(bot, false)).thenReturn(true);
		when(mob.getX()).thenReturn(40);
		when(mob.getY()).thenReturn(0);
		when(mob.getZ()).thenReturn(0);
		assertTrue(PhantomSpoil.trySweep(bot, mob));
		verify(bot).stopMove(null);
	}

	@Test
	void spoilWaitsOnReuseAndDoesNotRestartTheCast()
	{
		Player bot = mock(Player.class);
		CreatureAI ai = mock(CreatureAI.class);
		Monster mob = mock(Monster.class);
		Skill spoil = mock(Skill.class);
		when(bot.getActiveClass()).thenReturn(54);
		when(bot.getAI()).thenReturn(ai);
		when(bot.getKnownSkill(254)).thenReturn(null);
		when(mob.isMonster()).thenReturn(true);
		when(mob.isDead()).thenReturn(false);
		when(mob.isSpoiled()).thenReturn(false);
		assertFalse(PhantomSpoil.trySpoil(bot, mob));

		when(spoil.getMpConsume()).thenReturn(10);
		when(bot.getKnownSkill(254)).thenReturn(spoil);
		when(bot.isSkillDisabled(spoil)).thenReturn(false);
		when(bot.getCurrentMp()).thenReturn(100.0);
		when(ai.getIntention()).thenReturn(Intention.CAST);
		assertTrue(PhantomSpoil.trySpoil(bot, mob));
		verify(ai, never()).setIntention(eq(Intention.CAST), eq(spoil), eq(mob));
	}

	@Test
	void sweepCooldownKeepsAnOwnedCorpseAndIgnoresTheRest()
	{
		assertFalse(PhantomSpoil.isSweepOnCooldown(null, null));

		Player bot = mock(Player.class);
		Monster mob = mock(Monster.class);
		when(bot.getActiveClass()).thenReturn(55);
		when(mob.isDead()).thenReturn(true);
		when(mob.isSpawned()).thenReturn(true);
		when(mob.isSpoiled()).thenReturn(false);
		when(mob.isSweepActive()).thenReturn(false);
		assertFalse(PhantomSpoil.isSweepOnCooldown(bot, mob));

		when(mob.isSpoiled()).thenReturn(true);
		when(mob.checkSpoilOwner(bot, false)).thenReturn(true);
		when(bot.isCastingNow()).thenReturn(true);
		assertTrue(PhantomSpoil.isSweepOnCooldown(bot, mob));

		when(bot.isCastingNow()).thenReturn(false);
		when(bot.getKnownSkill(42)).thenReturn(null);
		assertFalse(PhantomSpoil.isSweepOnCooldown(bot, mob));

		Skill sweep = mock(Skill.class);
		when(bot.getKnownSkill(42)).thenReturn(sweep);
		when(bot.isSkillDisabled(sweep)).thenReturn(true);
		assertTrue(PhantomSpoil.isSweepOnCooldown(bot, mob));

		when(bot.isSkillDisabled(sweep)).thenReturn(false);
		assertFalse(PhantomSpoil.isSweepOnCooldown(bot, mob));
	}
}
