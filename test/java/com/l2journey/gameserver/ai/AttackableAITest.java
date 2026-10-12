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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.l2journey.Config;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.Spawn;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.instance.Monster;

/**
 * Regression tests for aggro-distance leash behavior in {@link AttackableAI}.
 */
class AttackableAITest
{
	private boolean _enabled;
	private boolean _raids;
	private boolean _instances;
	private boolean _restoreLife;
	private int _range;
	private int _raidRange;
	
	@BeforeEach
	void saveConfig()
	{
		_enabled = Config.AGGRO_DISTANCE_CHECK_ENABLED;
		_raids = Config.AGGRO_DISTANCE_CHECK_RAIDS;
		_instances = Config.AGGRO_DISTANCE_CHECK_INSTANCES;
		_restoreLife = Config.AGGRO_DISTANCE_CHECK_RESTORE_LIFE;
		_range = Config.AGGRO_DISTANCE_CHECK_RANGE;
		_raidRange = Config.AGGRO_DISTANCE_CHECK_RAID_RANGE;
	}
	
	@AfterEach
	void restoreConfig()
	{
		Config.AGGRO_DISTANCE_CHECK_ENABLED = _enabled;
		Config.AGGRO_DISTANCE_CHECK_RAIDS = _raids;
		Config.AGGRO_DISTANCE_CHECK_INSTANCES = _instances;
		Config.AGGRO_DISTANCE_CHECK_RESTORE_LIFE = _restoreLife;
		Config.AGGRO_DISTANCE_CHECK_RANGE = _range;
		Config.AGGRO_DISTANCE_CHECK_RAID_RANGE = _raidRange;
	}
	
	@Test
	void thinkAttackKeepsFightingRaidWhenRaidCheckIsDisabled()
	{
		Config.AGGRO_DISTANCE_CHECK_ENABLED = true;
		Config.AGGRO_DISTANCE_CHECK_RAIDS = false;
		Config.AGGRO_DISTANCE_CHECK_INSTANCES = false;
		Config.AGGRO_DISTANCE_CHECK_RESTORE_LIFE = false;
		Config.AGGRO_DISTANCE_CHECK_RANGE = 1500;
		Config.AGGRO_DISTANCE_CHECK_RAID_RANGE = 3000;
		
		Monster npc = monsterBeyondSpawn(true, 0, 5000.0);
		when(npc.isCoreAIDisabled()).thenReturn(false);
		when(npc.getMostHated()).thenReturn(null);
		
		TestableAttackableAI ai = new TestableAttackableAI(npc);
		ai.invokeThinkAttack();
		
		verify(npc).getMostHated();
		verify(npc, never()).abortAttack();
		verify(npc, never()).clearAggroList();
	}
	
	@Test
	void thinkAttackKeepsFightingInstanceMobWhenInstanceCheckIsDisabled()
	{
		Config.AGGRO_DISTANCE_CHECK_ENABLED = true;
		Config.AGGRO_DISTANCE_CHECK_RAIDS = false;
		Config.AGGRO_DISTANCE_CHECK_INSTANCES = false;
		Config.AGGRO_DISTANCE_CHECK_RESTORE_LIFE = false;
		Config.AGGRO_DISTANCE_CHECK_RANGE = 1500;
		Config.AGGRO_DISTANCE_CHECK_RAID_RANGE = 3000;
		
		Monster npc = monsterBeyondSpawn(false, 7, 5000.0);
		when(npc.isCoreAIDisabled()).thenReturn(false);
		when(npc.getMostHated()).thenReturn(null);
		
		TestableAttackableAI ai = new TestableAttackableAI(npc);
		ai.invokeThinkAttack();
		
		verify(npc).getMostHated();
		verify(npc, never()).abortAttack();
		verify(npc, never()).clearAggroList();
	}
	
	@Test
	void thinkAttackReturnsHomeForWorldMonsterBeyondLeash()
	{
		Config.AGGRO_DISTANCE_CHECK_ENABLED = true;
		Config.AGGRO_DISTANCE_CHECK_RAIDS = false;
		Config.AGGRO_DISTANCE_CHECK_INSTANCES = false;
		Config.AGGRO_DISTANCE_CHECK_RESTORE_LIFE = false;
		Config.AGGRO_DISTANCE_CHECK_RANGE = 1500;
		Config.AGGRO_DISTANCE_CHECK_RAID_RANGE = 3000;
		
		Location spawnLoc = new Location(0, 0, 0);
		Spawn spawn = mock(Spawn.class);
		when(spawn.getChaseRange()).thenReturn(0);
		when(spawn.getLocation()).thenReturn(spawnLoc);
		
		Monster npc = mock(Monster.class);
		when(npc.asAttackable()).thenReturn(npc);
		when(npc.asMonster()).thenReturn(npc);
		when(npc.isCastingNow()).thenReturn(false);
		when(npc.isMonster()).thenReturn(true);
		when(npc.isWalker()).thenReturn(false);
		when(npc.isRaid()).thenReturn(false);
		when(npc.getInstanceId()).thenReturn(0);
		when(npc.getSpawn()).thenReturn(spawn);
		when(npc.calculateDistance2D(spawnLoc)).thenReturn(5000.0);
		when(npc.hasAI()).thenReturn(true);
		CreatureAI creatureAi = mock(CreatureAI.class);
		when(npc.getAI()).thenReturn(creatureAi);
		Set<Creature> attackBy = ConcurrentHashMap.newKeySet();
		when(npc.getAttackByList()).thenReturn(attackBy);
		when(npc.hasMinions()).thenReturn(false);
		
		TestableAttackableAI ai = new TestableAttackableAI(npc);
		ai.invokeThinkAttack();
		
		verify(npc).abortAttack();
		verify(npc).clearAggroList();
		verify(npc, never()).getMostHated();
		verify(creatureAi).setIntention(Intention.MOVE_TO, spawnLoc);
	}
	
	private static Monster monsterBeyondSpawn(boolean raid, int instanceId, double distance)
	{
		Location spawnLoc = new Location(0, 0, 0);
		Spawn spawn = mock(Spawn.class);
		when(spawn.getChaseRange()).thenReturn(0);
		when(spawn.getLocation()).thenReturn(spawnLoc);
		
		Monster npc = mock(Monster.class);
		when(npc.asAttackable()).thenReturn(npc);
		when(npc.asMonster()).thenReturn(npc);
		when(npc.isCastingNow()).thenReturn(false);
		when(npc.isMonster()).thenReturn(true);
		when(npc.isWalker()).thenReturn(false);
		when(npc.isRaid()).thenReturn(raid);
		when(npc.getInstanceId()).thenReturn(instanceId);
		when(npc.getSpawn()).thenReturn(spawn);
		when(npc.calculateDistance2D(spawnLoc)).thenReturn(distance);
		return npc;
	}
	
	private static final class TestableAttackableAI extends AttackableAI
	{
		private TestableAttackableAI(Monster creature)
		{
			super(creature);
		}
		
		private void invokeThinkAttack()
		{
			thinkAttack();
		}
		
		@Override
		public void setIntention(Intention intention, Object arg0, Object arg1)
		{
			// Avoid follow, world-region, and packet work when the attack think continues.
		}
	}
}
