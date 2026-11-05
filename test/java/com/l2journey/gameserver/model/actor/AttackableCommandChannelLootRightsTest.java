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
package com.l2journey.gameserver.model.actor;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.concurrent.ScheduledFuture;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;

import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.model.groups.CommandChannel;
import com.l2journey.gameserver.model.groups.Party;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.actor.status.CreatureStatus;

/**
 * Command-channel loot-rights updates must publish the channel only after scheduling.
 */
class AttackableCommandChannelLootRightsTest
{
	@Test
	void nullChannelLeavesLootRightsUnset()
	{
		try (MockedStatic<ThreadPool> threadPool = mockStatic(ThreadPool.class))
		{
			Attackable attackable = mock(Attackable.class, Answers.CALLS_REAL_METHODS);
			attackable.updateCommandChannelLootRights(null);
			
			assertNull(attackable.getFirstCommandChannelAttacked());
			threadPool.verifyNoInteractions();
		}
	}
	
	@Test
	void firstChannelSchedulesOnceAndStoresChannel()
	{
		CommandChannel channel = mock(CommandChannel.class);
		ScheduledFuture<?> future = mock(ScheduledFuture.class);
		
		try (MockedStatic<ThreadPool> threadPool = mockStatic(ThreadPool.class))
		{
			threadPool.when(() -> ThreadPool.schedule(any(Runnable.class), anyLong())).thenReturn(future);
			
			Attackable attackable = mock(Attackable.class, Answers.CALLS_REAL_METHODS);
			attackable.updateCommandChannelLootRights(channel);
			
			assertSame(channel, attackable.getFirstCommandChannelAttacked());
			threadPool.verify(() -> ThreadPool.schedule(any(Runnable.class), anyLong()), times(1));
		}
	}
	
	@Test
	void secondHitFromSameChannelRefreshesLastAttackWithoutScheduling()
	{
		CommandChannel channel = mock(CommandChannel.class);
		when(channel.hasSameLeader(any(CommandChannel.class))).thenReturn(true);
		
		try (MockedStatic<ThreadPool> threadPool = mockStatic(ThreadPool.class))
		{
			Attackable attackable = mock(Attackable.class, Answers.CALLS_REAL_METHODS);
			attackable.setFirstCommandChannelAttacked(channel);
			
			attackable.updateCommandChannelLootRights(channel);
			
			assertSame(channel, attackable.getFirstCommandChannelAttacked());
			assertTrue(attackable.getCommandChannelLastAttack() > 0);
			threadPool.verifyNoInteractions();
		}
	}
	
	@Test
	void secondHitFromDifferentChannelDoesNotSchedule()
	{
		CommandChannel first = mock(CommandChannel.class);
		CommandChannel other = mock(CommandChannel.class);
		when(other.hasSameLeader(any(CommandChannel.class))).thenReturn(false);
		
		try (MockedStatic<ThreadPool> threadPool = mockStatic(ThreadPool.class))
		{
			Attackable attackable = mock(Attackable.class, Answers.CALLS_REAL_METHODS);
			attackable.setFirstCommandChannelAttacked(first);
			
			attackable.updateCommandChannelLootRights(other);
			
			assertSame(first, attackable.getFirstCommandChannelAttacked());
			threadPool.verifyNoInteractions();
		}
	}
	
	@Test
	void reduceCurrentHpUpdatesLootRightsForRaid()
	{
		CommandChannel channel = mock(CommandChannel.class);
		Party party = mock(Party.class);
		Player attacker = mock(Player.class);
		when(attacker.getParty()).thenReturn(party);
		when(party.isInCommandChannel()).thenReturn(true);
		when(party.getCommandChannel()).thenReturn(channel);
		
		Attackable attackable = mock(Attackable.class, Answers.CALLS_REAL_METHODS);
		when(channel.meetRaidWarCondition(attackable)).thenReturn(true);
		when(attackable.isMonster()).thenReturn(false);
		doNothing().when(attackable).addDamage(any(Creature.class), anyInt(), nullable(Skill.class));
		attackable.setIsRaid(true);
		attackable.setStatus(mock(CreatureStatus.class));
		
		try (MockedStatic<ThreadPool> threadPool = mockStatic(ThreadPool.class))
		{
			threadPool.when(() -> ThreadPool.schedule(any(Runnable.class), anyLong())).thenReturn(mock(ScheduledFuture.class));
			
			attackable.reduceCurrentHp(10, attacker, true, false, null);
			
			assertSame(channel, attackable.getFirstCommandChannelAttacked());
		}
	}
}
