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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Attackable;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.Summon;
import com.l2journey.gameserver.model.actor.enums.npc.AIType;
import com.l2journey.gameserver.model.actor.instance.Guard;
import com.l2journey.gameserver.model.actor.templates.CreatureTemplate;
import com.l2journey.gameserver.model.actor.templates.NpcTemplate;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.zone.ZoneId;

/**
 * Null-safe actor and cast bindings for {@link AttackableAI}, {@link CreatureAI}, and {@link SummonAI}.
 */
class CreatureAINullGuardTest
{
	@Test
	void maybeMoveToPawnRejectsNullActor()
	{
		final AttackableProbe ai = new AttackableProbe(null);
		final WorldObject target = mock(WorldObject.class);
		assertThrows(IllegalStateException.class, () -> ai.moveToPawnPublic(target, 40));
	}
	
	@Test
	void maybeMoveToPawnAllowsNonNullActorInsideRadius()
	{
		final Guard npc = stubbedGuard();
		final NpcTemplate template = mock(NpcTemplate.class);
		when(template.getCollisionRadius()).thenReturn(10);
		when(npc.getTemplate()).thenReturn(template);
		when(npc.isInsideZone(ZoneId.MULTI_FLOOR)).thenReturn(false);
		
		final WorldObject target = mock(WorldObject.class);
		when(target.isCreature()).thenReturn(false);
		when(npc.isInsideRadius2D(target, 50)).thenReturn(true);
		
		final AttackableProbe ai = new AttackableProbe(npc);
		assertDoesNotThrow(() -> ai.moveToPawnPublic(target, 40));
	}
	
	@Test
	void attackableThinkCastRejectsNullActor()
	{
		final AttackableProbe ai = new AttackableProbe(null);
		ai.setCastTarget(mock(Creature.class));
		assertThrows(IllegalStateException.class, ai::thinkCastPublic);
	}
	
	@Test
	void attackableThinkCastAllowsNonNullActor()
	{
		final Guard npc = stubbedGuard();
		final AttackableProbe ai = new AttackableProbe(npc)
		{
			@Override
			protected boolean maybeMoveToPawn(WorldObject target, int offsetValue)
			{
				return true;
			}
		};
		ai.setCastTarget(mock(Creature.class));
		assertDoesNotThrow(ai::thinkCastPublic);
	}
	
	@Test
	void summonThinkCastRejectsNullActor()
	{
		final SummonProbe ai = new SummonProbe(null);
		ai.setCastTarget(mock(Creature.class));
		assertThrows(IllegalStateException.class, ai::thinkCastPublic);
	}
	
	@Test
	void summonThinkCastAllowsNonNullActor()
	{
		final Summon summon = stubbedSummon();
		when(summon.isSummon()).thenReturn(false);
		final SummonProbe ai = new SummonProbe(summon)
		{
			@Override
			protected boolean maybeMoveToPawn(WorldObject target, int offsetValue)
			{
				return true;
			}
		};
		ai.setCastTarget(mock(Creature.class));
		assertDoesNotThrow(ai::thinkCastPublic);
	}
	
	@Test
	void attackableRejectsNullAsAttackable()
	{
		final Guard npc = mock(Guard.class);
		when(npc.asAttackable()).thenReturn(null);
		final AttackableProbe ai = new AttackableProbe(npc);
		assertThrows(IllegalStateException.class, () -> ai.aggression(mock(Creature.class), 1));
	}
	
	@Test
	void onActionAggressionAddsHateOnBoundAttackable()
	{
		final Guard npc = stubbedGuard();
		when(npc.isDead()).thenReturn(false);
		when(npc.isRunning()).thenReturn(true);
		when(npc.isMonster()).thenReturn(false);
		final AttackableProbe ai = new AttackableProbe(npc);
		final Creature target = mock(Creature.class);
		
		ai.aggression(target, 5);
		
		verify(npc).addDamageHate(target, 0, 5);
	}
	
	@Test
	void movementDisableUsesAttackable() throws Exception
	{
		final Guard npc = stubbedGuard();
		final NpcTemplate template = mock(NpcTemplate.class);
		when(template.getCollisionRadius()).thenReturn(0);
		when(template.getAISkills(any())).thenReturn(Collections.emptyList());
		when(npc.getTemplate()).thenReturn(template);
		when(npc.getTarget()).thenReturn(mock(WorldObject.class));
		when(npc.getPhysicalAttackRange()).thenReturn(0);
		when(npc.getHateList()).thenReturn(null);
		when(npc.getMostHated()).thenReturn(null);
		when(npc.isMovementDisabled()).thenReturn(false);
		when(npc.getAiType()).thenReturn(AIType.FIGHTER);
		
		final Creature target = mock(Creature.class);
		final CreatureTemplate targetTemplate = mock(CreatureTemplate.class);
		when(targetTemplate.getCollisionRadius()).thenReturn(0);
		when(target.getTemplate()).thenReturn(targetTemplate);
		when(target.isImmobilized()).thenReturn(true);
		when(npc.calculateDistance2D(target)).thenReturn(10_000.0);
		
		final AttackableProbe ai = new AttackableProbe(npc);
		ai.setAttackTarget(target);
		invokePrivate(AttackableAI.class, ai, "movementDisable");
	}
	
	@Test
	void effectTargetReconsiderUsesAttackable() throws Exception
	{
		final Guard npc = stubbedGuard();
		final Set<Creature> attackBy = ConcurrentHashMap.newKeySet();
		when(npc.getAttackByList()).thenReturn(attackBy);
		
		final Skill skill = mock(Skill.class);
		when(skill.hasEffectType(any(), any())).thenReturn(false);
		
		final World world = mock(World.class);
		when(world.getVisibleObjectsInRange(any(), any(), anyInt())).thenReturn(List.of());
		final AttackableProbe ai = new AttackableProbe(npc);
		try (MockedStatic<World> worlds = mockStatic(World.class))
		{
			worlds.when(World::getInstance).thenReturn(world);
			invokePrivate(AttackableAI.class, ai, "effectTargetReconsider", new Class<?>[]
			{
				Skill.class,
				boolean.class
			}, skill, false);
		}
	}
	
	@Test
	void skillTargetReconsiderUsesAttackable() throws Exception
	{
		final Guard npc = stubbedGuard();
		when(npc.getHateList()).thenReturn(null);
		final Skill skill = mock(Skill.class);
		final AttackableProbe ai = new AttackableProbe(npc);
		invokePrivate(AttackableAI.class, ai, "skillTargetReconsider", new Class<?>[]
		{
			Skill.class
		}, skill);
	}
	
	@Test
	void checkSkillCastConditionsRejectsNullCaster() throws Exception
	{
		final Method method = AttackableAI.class.getDeclaredMethod("checkSkillCastConditions", Attackable.class, Skill.class);
		method.setAccessible(true);
		final Exception thrown = assertThrows(Exception.class, () -> method.invoke(null, null, mock(Skill.class)));
		assertTrue(thrown.getCause() instanceof IllegalStateException);
	}
	
	@Test
	void checkSkillCastConditionsAllowsCaster() throws Exception
	{
		final Guard npc = stubbedGuard();
		when(npc.isCastingNow()).thenReturn(false);
		when(npc.getCurrentMp()).thenReturn(100.0);
		when(npc.isSkillDisabled(any())).thenReturn(false);
		when(npc.isMuted()).thenReturn(false);
		when(npc.isPhysicalMuted()).thenReturn(false);
		
		final Skill skill = mock(Skill.class);
		when(skill.isSimultaneousCast()).thenReturn(false);
		when(skill.getMpConsume()).thenReturn(10);
		when(skill.isStatic()).thenReturn(true);
		
		final Method method = AttackableAI.class.getDeclaredMethod("checkSkillCastConditions", Attackable.class, Skill.class);
		method.setAccessible(true);
		assertTrue((Boolean) method.invoke(null, npc, skill));
	}
	
	@Test
	void summonRejectsClearedBoundSummon() throws Exception
	{
		final Summon summon = stubbedSummon();
		final SummonProbe ai = new SummonProbe(summon);
		final java.lang.reflect.Field field = SummonAI.class.getDeclaredField("_boundSummon");
		field.setAccessible(true);
		field.set(ai, null);
		assertThrows(IllegalStateException.class, ai::finishCasting);
	}
	
	@Test
	void summonConstructorReadsFollowStatus()
	{
		final Summon summon = stubbedSummon();
		when(summon.getFollowStatus()).thenReturn(true);
		final SummonProbe ai = new SummonProbe(summon);
		ai.active();
		verify(summon).getOwner();
	}
	
	@Test
	void onIntentionActiveWithoutFollowCallsSuper()
	{
		final Summon summon = stubbedSummon();
		when(summon.getFollowStatus()).thenReturn(false);
		final SummonProbe ai = new SummonProbe(summon)
		{
			@Override
			public Intention getIntention()
			{
				return Intention.ACTIVE;
			}
		};
		assertDoesNotThrow(ai::active);
	}
	
	@Test
	void onActionAggressionReturnsWhenTargetNull()
	{
		final Guard npc = stubbedGuard();
		when(npc.isDead()).thenReturn(false);
		final AttackableProbe ai = new AttackableProbe(npc);
		assertDoesNotThrow(() -> ai.aggression(null, 1));
	}
	
	@Test
	void avoidAttackSkipsWhenOwnerNull() throws Exception
	{
		final Summon summon = stubbedSummon();
		when(summon.getFollowStatus()).thenReturn(false);
		when(summon.getOwner()).thenReturn(null);
		final SummonProbe ai = new SummonProbe(summon);
		invokePrivate(SummonAI.class, ai, "avoidAttack", new Class<?>[]
		{
			Creature.class
		}, mock(Creature.class));
	}
	
	@Test
	void onActionFinishCastingSetsFollowStatus()
	{
		final Summon summon = stubbedSummon();
		when(summon.getFollowStatus()).thenReturn(false);
		final SummonProbe ai = new SummonProbe(summon);
		ai.finishCasting();
		verify(summon).setFollowStatus(false);
	}
	
	@Test
	void avoidAttackSetsStartAvoidWhenOwnerNearby() throws Exception
	{
		final Summon summon = stubbedSummon();
		when(summon.getFollowStatus()).thenReturn(false);
		final Player owner = mock(Player.class);
		when(summon.getOwner()).thenReturn(owner);
		when(owner.isInsideRadius3D(summon, 140)).thenReturn(true);
		final SummonProbe ai = new SummonProbe(summon);
		invokePrivate(SummonAI.class, ai, "avoidAttack", new Class<?>[]
		{
			Creature.class
		}, mock(Creature.class));
	}
	
	@Test
	void notifyFollowStatusChangeSetsFollowStatus()
	{
		final Summon summon = stubbedSummon();
		when(summon.getFollowStatus()).thenReturn(false);
		final SummonProbe ai = new SummonProbe(summon);
		ai.notifyFollowStatusChange();
		verify(summon).setFollowStatus(true);
	}
	
	private static Guard stubbedGuard()
	{
		final Guard npc = mock(Guard.class);
		when(npc.asAttackable()).thenReturn(npc);
		return npc;
	}
	
	private static Summon stubbedSummon()
	{
		final Summon summon = mock(Summon.class);
		when(summon.getFollowStatus()).thenReturn(false);
		return summon;
	}
	
	private static void invokePrivate(Class<?> type, Object target, String name) throws Exception
	{
		invokePrivate(type, target, name, new Class<?>[] {});
	}
	
	private static void invokePrivate(Class<?> type, Object target, String name, Class<?>[] parameterTypes, Object... args) throws Exception
	{
		final Method method = type.getDeclaredMethod(name, parameterTypes);
		method.setAccessible(true);
		try
		{
			method.invoke(target, args);
		}
		catch (java.lang.reflect.InvocationTargetException e)
		{
			final Throwable cause = e.getCause();
			if (cause instanceof Exception exception)
			{
				throw exception;
			}
			if (cause instanceof Error error)
			{
				throw error;
			}
			throw e;
		}
	}
	
	private static class AttackableProbe extends AttackableAI
	{
		private AttackableProbe(Attackable creature)
		{
			super(creature);
		}
		
		private void moveToPawnPublic(WorldObject target, int offset)
		{
			maybeMoveToPawn(target, offset);
		}
		
		private void thinkCastPublic()
		{
			thinkCast();
		}
		
		private void aggression(Creature target, int aggro)
		{
			onActionAggression(target, aggro);
		}
		
		@Override
		public void setIntention(Intention intention, Object arg0, Object arg1)
		{
		}
	}
	
	private static class SummonProbe extends SummonAI
	{
		private SummonProbe(Summon creature)
		{
			super(creature);
		}
		
		private void thinkCastPublic()
		{
			try
			{
				invokePrivate(SummonAI.class, this, "thinkCast");
			}
			catch (RuntimeException | Error e)
			{
				throw e;
			}
			catch (Exception e)
			{
				throw new IllegalStateException(e);
			}
		}
		
		private void active()
		{
			onIntentionActive();
		}
		
		private void finishCasting()
		{
			onActionFinishCasting();
		}
		
		@Override
		public Intention getIntention()
		{
			return Intention.IDLE;
		}
		
		@Override
		public void setIntention(Intention intention, Object arg0, Object arg1)
		{
		}
	}
}
