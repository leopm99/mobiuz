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
package com.l2journey.gameserver.model.skill;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.gameserver.model.EffectList;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.effects.AbstractEffect;
import com.l2journey.gameserver.model.effects.EffectTaskInfo;
import com.l2journey.gameserver.model.effects.EffectTickTask;
import com.l2journey.gameserver.model.skill.enums.SkillFinishType;
import com.l2journey.gameserver.taskmanagers.GameTimeTaskManager;

/**
 * Covers {@link BuffInfo#onTick(AbstractEffect)} early-exit when the effected left the world.
 */
class BuffInfoOnTickDespawnTest
{
	@Test
	void despawnedCancelsTickTaskAndStopsSkill() throws Exception
	{
		final Creature effector = mock(Creature.class);
		final Creature effected = mock(Creature.class);
		final EffectList effectList = mock(EffectList.class);
		final Skill skill = stubSkill();
		final AbstractEffect effect = mock(AbstractEffect.class);
		final ScheduledFuture<?> future = mock(ScheduledFuture.class);
		
		when(effected.isSpawned()).thenReturn(false);
		when(effected.isTeleporting()).thenReturn(false);
		when(effected.getEffectList()).thenReturn(effectList);
		
		final GameTimeTaskManager gameTime = mock(GameTimeTaskManager.class);
		when(gameTime.getGameTicks()).thenReturn(0);
		
		try (MockedStatic<GameTimeTaskManager> gameTimes = mockStatic(GameTimeTaskManager.class))
		{
			gameTimes.when(GameTimeTaskManager::getInstance).thenReturn(gameTime);
			
			final BuffInfo info = new BuffInfo(effector, effected, skill);
			putEffectTask(info, effect, future);
			
			info.onTick(effect);
			
			verify(future).cancel(true);
			verify(effectList).stopSkillEffects(SkillFinishType.REMOVED, skill);
			verify(effect, never()).onActionTime(any(), any(), any());
		}
	}
	
	@Test
	void despawnedWithoutScheduledTaskStillStopsSkill() throws Exception
	{
		final Creature effector = mock(Creature.class);
		final Creature effected = mock(Creature.class);
		final EffectList effectList = mock(EffectList.class);
		final Skill skill = stubSkill();
		final AbstractEffect effect = mock(AbstractEffect.class);
		
		when(effected.isSpawned()).thenReturn(false);
		when(effected.isTeleporting()).thenReturn(false);
		when(effected.getEffectList()).thenReturn(effectList);
		
		final GameTimeTaskManager gameTime = mock(GameTimeTaskManager.class);
		when(gameTime.getGameTicks()).thenReturn(0);
		
		try (MockedStatic<GameTimeTaskManager> gameTimes = mockStatic(GameTimeTaskManager.class))
		{
			gameTimes.when(GameTimeTaskManager::getInstance).thenReturn(gameTime);
			
			final BuffInfo info = new BuffInfo(effector, effected, skill);
			info.onTick(effect);
			
			verify(effectList).stopSkillEffects(SkillFinishType.REMOVED, skill);
			verify(effect, never()).onActionTime(any(), any(), any());
		}
	}
	
	@Test
	void despawnedWithNullFutureStillStopsSkill() throws Exception
	{
		final Creature effector = mock(Creature.class);
		final Creature effected = mock(Creature.class);
		final EffectList effectList = mock(EffectList.class);
		final Skill skill = stubSkill();
		final AbstractEffect effect = mock(AbstractEffect.class);
		
		when(effected.isSpawned()).thenReturn(false);
		when(effected.isTeleporting()).thenReturn(false);
		when(effected.getEffectList()).thenReturn(effectList);
		
		final GameTimeTaskManager gameTime = mock(GameTimeTaskManager.class);
		when(gameTime.getGameTicks()).thenReturn(0);
		
		try (MockedStatic<GameTimeTaskManager> gameTimes = mockStatic(GameTimeTaskManager.class))
		{
			gameTimes.when(GameTimeTaskManager::getInstance).thenReturn(gameTime);
			
			final BuffInfo info = new BuffInfo(effector, effected, skill);
			putEffectTask(info, effect, null);
			
			info.onTick(effect);
			
			verify(effectList).stopSkillEffects(SkillFinishType.REMOVED, skill);
			verify(effect, never()).onActionTime(any(), any(), any());
		}
	}
	
	@Test
	void teleportingSkipsDespawnGuard() throws Exception
	{
		final Creature effector = mock(Creature.class);
		final Creature effected = mock(Creature.class);
		final EffectList effectList = mock(EffectList.class);
		final Skill skill = stubSkill();
		final AbstractEffect effect = mock(AbstractEffect.class);
		
		when(effected.isSpawned()).thenReturn(false);
		when(effected.isTeleporting()).thenReturn(true);
		when(effected.getEffectList()).thenReturn(effectList);
		when(effect.onActionTime(effector, effected, skill)).thenReturn(true);
		
		final GameTimeTaskManager gameTime = mock(GameTimeTaskManager.class);
		when(gameTime.getGameTicks()).thenReturn(0);
		
		try (MockedStatic<GameTimeTaskManager> gameTimes = mockStatic(GameTimeTaskManager.class))
		{
			gameTimes.when(GameTimeTaskManager::getInstance).thenReturn(gameTime);
			
			final BuffInfo info = new BuffInfo(effector, effected, skill);
			info.onTick(effect);
			
			verify(effect).onActionTime(effector, effected, skill);
			verify(effectList, never()).stopSkillEffects(eq(SkillFinishType.REMOVED), eq(skill));
		}
	}
	
	@Test
	void spawnedAppliesOnActionTime() throws Exception
	{
		final Creature effector = mock(Creature.class);
		final Creature effected = mock(Creature.class);
		final EffectList effectList = mock(EffectList.class);
		final Skill skill = stubSkill();
		final AbstractEffect effect = mock(AbstractEffect.class);
		
		when(effected.isSpawned()).thenReturn(true);
		when(effected.isTeleporting()).thenReturn(false);
		when(effected.getEffectList()).thenReturn(effectList);
		when(effect.onActionTime(effector, effected, skill)).thenReturn(true);
		
		final GameTimeTaskManager gameTime = mock(GameTimeTaskManager.class);
		when(gameTime.getGameTicks()).thenReturn(0);
		
		try (MockedStatic<GameTimeTaskManager> gameTimes = mockStatic(GameTimeTaskManager.class))
		{
			gameTimes.when(GameTimeTaskManager::getInstance).thenReturn(gameTime);
			
			final BuffInfo info = new BuffInfo(effector, effected, skill);
			info.onTick(effect);
			
			verify(effect).onActionTime(effector, effected, skill);
			verify(effectList, never()).stopSkillEffects(eq(SkillFinishType.REMOVED), eq(skill));
		}
	}
	
	private static Skill stubSkill()
	{
		final Skill skill = mock(Skill.class);
		when(skill.isPassive()).thenReturn(false);
		when(skill.isToggle()).thenReturn(false);
		when(skill.isStatic()).thenReturn(true);
		when(skill.isDebuff()).thenReturn(false);
		when(skill.isAbnormalInstant()).thenReturn(false);
		when(skill.getAbnormalTime()).thenReturn(30);
		return skill;
	}
	
	@SuppressWarnings("unchecked")
	private static void putEffectTask(BuffInfo info, AbstractEffect effect, ScheduledFuture<?> future) throws Exception
	{
		final Field tasksField = BuffInfo.class.getDeclaredField("_tasks");
		tasksField.setAccessible(true);
		final Map<AbstractEffect, EffectTaskInfo> tasks = (Map<AbstractEffect, EffectTaskInfo>) tasksField.get(info);
		tasks.put(effect, new EffectTaskInfo(mock(EffectTickTask.class), future));
	}
}
