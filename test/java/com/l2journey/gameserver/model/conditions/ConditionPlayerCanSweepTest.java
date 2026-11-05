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
package com.l2journey.gameserver.model.conditions;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.l2journey.Config;
import com.l2journey.gameserver.model.actor.Attackable;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.itemcontainer.PlayerInventory;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.network.SystemMessageId;

/**
 * Characterization of {@link ConditionPlayerCanSweep} spoil and corpse checks.
 */
class ConditionPlayerCanSweepTest
{
	@Test
	void nonPlayerReturnsFalse()
	{
		final Creature effector = mock(Creature.class);
		when(effector.isPlayer()).thenReturn(false);
		
		final ConditionPlayerCanSweep condition = new ConditionPlayerCanSweep(true);
		assertFalse(condition.testImpl(effector, null, mock(Skill.class), null));
	}
	
	@Test
	void nullSkillReturnsFalse()
	{
		final Player player = mockPlayer();
		
		final ConditionPlayerCanSweep condition = new ConditionPlayerCanSweep(true);
		assertFalse(condition.testImpl(player, null, null, null));
	}
	
	@Test
	void deadNotSpoiledSendsSweeperFailedPacket()
	{
		final Player sweeper = mockPlayer();
		final Skill skill = mock(Skill.class);
		final Attackable target = mock(Attackable.class);
		when(skill.getTargetList(sweeper)).thenReturn(List.of(target));
		when(target.isAttackable()).thenReturn(true);
		when(target.asAttackable()).thenReturn(target);
		when(target.isDead()).thenReturn(true);
		when(target.isSpoiled()).thenReturn(false);
		
		final ConditionPlayerCanSweep condition = new ConditionPlayerCanSweep(true);
		assertFalse(condition.testImpl(sweeper, null, skill, null));
		
		verify(sweeper).sendPacket(SystemMessageId.SWEEPER_FAILED_TARGET_NOT_SPOILED);
	}
	
	@Test
	void spoiledCorpseAllowsSweepWhenChecksPass()
	{
		Config.CORPSE_CONSUME_SKILL_ALLOWED_TIME_BEFORE_DECAY = 2000;
		
		final Player sweeper = mockPlayer();
		final Skill skill = mock(Skill.class);
		final Attackable target = mock(Attackable.class);
		final PlayerInventory inventory = mock(PlayerInventory.class);
		final List<ItemTemplate> loot = Collections.emptyList();
		when(skill.getTargetList(sweeper)).thenReturn(List.of(target));
		when(target.isAttackable()).thenReturn(true);
		when(target.asAttackable()).thenReturn(target);
		when(target.isDead()).thenReturn(true);
		when(target.isSpoiled()).thenReturn(true);
		when(target.checkSpoilOwner(sweeper, true)).thenReturn(true);
		when(target.isOldCorpse(eq(sweeper), eq(Config.CORPSE_CONSUME_SKILL_ALLOWED_TIME_BEFORE_DECAY), eq(true))).thenReturn(false);
		when(target.getSpoilLootItems()).thenReturn(loot);
		when(sweeper.getInventory()).thenReturn(inventory);
		when(inventory.checkInventorySlotsAndWeight(loot, true, true)).thenReturn(true);
		
		final ConditionPlayerCanSweep condition = new ConditionPlayerCanSweep(true);
		assertTrue(condition.testImpl(sweeper, null, skill, null));
		
		verify(target).checkSpoilOwner(sweeper, true);
		verify(target).isOldCorpse(sweeper, Config.CORPSE_CONSUME_SKILL_ALLOWED_TIME_BEFORE_DECAY, true);
		verify(inventory).checkInventorySlotsAndWeight(loot, true, true);
	}
	
	@Test
	void spoiledCorpseRejectsWhenNotSpoilOwner()
	{
		Config.CORPSE_CONSUME_SKILL_ALLOWED_TIME_BEFORE_DECAY = 2000;
		
		final Player sweeper = mockPlayer();
		final Skill skill = mock(Skill.class);
		final Attackable target = mock(Attackable.class);
		final PlayerInventory inventory = mock(PlayerInventory.class);
		when(skill.getTargetList(sweeper)).thenReturn(List.of(target));
		when(target.isAttackable()).thenReturn(true);
		when(target.asAttackable()).thenReturn(target);
		when(target.isDead()).thenReturn(true);
		when(target.isSpoiled()).thenReturn(true);
		when(target.checkSpoilOwner(any(Player.class), anyBoolean())).thenReturn(false);
		when(target.isOldCorpse(any(Player.class), anyInt(), anyBoolean())).thenReturn(false);
		when(target.getSpoilLootItems()).thenReturn(Collections.emptyList());
		when(sweeper.getInventory()).thenReturn(inventory);
		when(inventory.checkInventorySlotsAndWeight(any(), anyBoolean(), anyBoolean())).thenReturn(true);
		
		final ConditionPlayerCanSweep condition = new ConditionPlayerCanSweep(true);
		assertFalse(condition.testImpl(sweeper, null, skill, null));
		
		verify(target).checkSpoilOwner(sweeper, true);
	}
	
	private static Player mockPlayer()
	{
		final Player player = mock(Player.class);
		when(player.isPlayer()).thenReturn(true);
		when(player.asPlayer()).thenReturn(player);
		return player;
	}
}
