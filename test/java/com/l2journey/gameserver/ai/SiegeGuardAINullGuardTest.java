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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.enums.npc.AIType;
import com.l2journey.gameserver.model.actor.instance.Defender;
import com.l2journey.gameserver.model.actor.templates.NpcTemplate;
import com.l2journey.gameserver.taskmanagers.GameTimeTaskManager;


/**
 * Null-safe defender binding for {@link FortSiegeGuardAI} and {@link SiegeGuardAI}.
 */
class SiegeGuardAINullGuardTest
{
	enum Kind
	{
		FORT,
		CASTLE
	}
	
	static Stream<Arguments> kinds()
	{
		return Stream.of(Arguments.of(Kind.FORT), Arguments.of(Kind.CASTLE));
	}
	
	@Test
	void requireRejectsNullAndReturnsSameDefender() throws Exception
	{
		assertThrows(IllegalStateException.class, () -> SiegeGuardActors.require(null));
		
		final Defender defender = mockDefender();
		assertSame(defender, SiegeGuardActors.require(defender));
		
		final Constructor<SiegeGuardActors> ctor = SiegeGuardActors.class.getDeclaredConstructor();
		ctor.setAccessible(true);
		ctor.newInstance();
	}
	
	@ParameterizedTest
	@MethodSource("kinds")
	void constructorRejectsNullDefender(Kind kind)
	{
		assertThrows(IllegalStateException.class, () -> create(kind, null));
	}
	
	@ParameterizedTest
	@MethodSource("kinds")
	void constructorBindsDefender(Kind kind) throws Exception
	{
		final Defender defender = mockDefender();
		final Probe ai = create(kind, defender);
		assertEquals(defender, guardField(kind).get(ai));
	}
	
	@ParameterizedTest
	@MethodSource("kinds")
	void clearedGuardThrowsOnAttacked(Kind kind) throws Exception
	{
		final Defender defender = mockDefender();
		final Probe ai = create(kind, defender);
		guardField(kind).set(ai, null);
		
		final GameTimeTaskManager time = mock(GameTimeTaskManager.class);
		when(time.getGameTicks()).thenReturn(100);
		try (MockedStatic<GameTimeTaskManager> times = mockStatic(GameTimeTaskManager.class))
		{
			times.when(GameTimeTaskManager::getInstance).thenReturn(time);
			assertThrows(IllegalStateException.class, () -> ai.attacked(mock(Creature.class)));
		}
	}
	
	@ParameterizedTest
	@MethodSource("kinds")
	void thinkActiveUsesBoundDefender(Kind kind) throws Exception
	{
		final Defender defender = mockDefender();
		final Probe ai = create(kind, defender);
		globalAggro(kind).setInt(ai, 0);
		when(defender.getMostHated()).thenReturn(null);
		
		final World world = mock(World.class);
		doNothing().when(world).forEachVisibleObjectInRange(any(), any(), anyInt(), any());
		try (MockedStatic<World> worlds = mockStatic(World.class))
		{
			worlds.when(World::getInstance).thenReturn(world);
			invokePrivate(kind, ai, "thinkActive");
		}
		
		verify(defender).getMostHated();
		verify(defender, never()).asAttackable();
	}
	
	@ParameterizedTest
	@MethodSource("kinds")
	void onActionAttackedAddsHateOnBoundDefender(Kind kind) throws Exception
	{
		final Defender defender = mockDefender();
		when(defender.isNpc()).thenReturn(true);
		when(defender.isAttackable()).thenReturn(false);
		final Probe ai = create(kind, defender);
		final Creature attacker = mock(Creature.class);
		
		final GameTimeTaskManager time = mock(GameTimeTaskManager.class);
		when(time.getGameTicks()).thenReturn(100);
		try (MockedStatic<GameTimeTaskManager> times = mockStatic(GameTimeTaskManager.class))
		{
			times.when(GameTimeTaskManager::getInstance).thenReturn(time);
			ai.attacked(attacker);
		}
		
		verify(defender).addDamageHate(attacker, 0, 1);
		verify(defender, never()).asAttackable();
	}
	
	@ParameterizedTest
	@MethodSource("kinds")
	void onActionAggressionAddsHateForTarget(Kind kind) throws Exception
	{
		final Defender defender = mockDefender();
		final Probe ai = create(kind, defender);
		final Creature target = mock(Creature.class);
		when(defender.getHating(target)).thenReturn(0L);
		when(defender.getMostHated()).thenReturn(null);
		
		ai.aggression(target, 5);
		
		verify(defender).addDamageHate(target, 0, 5);
	}
	
	@ParameterizedTest
	@MethodSource("kinds")
	void onActionAggressionNullTargetReadsMostHated(Kind kind) throws Exception
	{
		final Defender defender = mockDefender();
		final Probe ai = create(kind, defender);
		when(defender.getMostHated()).thenReturn(null);
		
		ai.aggression(null, -1);
		
		verify(defender).getMostHated();
	}
	
	@ParameterizedTest
	@MethodSource("kinds")
	void factionNotifyReturnsWhenClansNull(Kind kind) throws Exception
	{
		final Defender defender = mockDefender();
		when(defender.getTemplate().getClans()).thenReturn(null);
		final Probe ai = create(kind, defender);
		ai.setAttackTarget(mock(Creature.class));
		
		invokePrivate(kind, ai, "factionNotifyAndSupport");
		
		verify(defender, never()).asNpc();
	}
	
	@ParameterizedTest
	@MethodSource("kinds")
	void factionNotifyReturnsWhenAttackTargetNull(Kind kind) throws Exception
	{
		final Defender defender = mockDefender();
		when(defender.getTemplate().getClans()).thenReturn(Collections.singleton(1));
		final Probe ai = create(kind, defender);
		ai.setAttackTarget(null);
		
		invokePrivate(kind, ai, "factionNotifyAndSupport");
	}
	
	@ParameterizedTest
	@MethodSource("kinds")
	void factionNotifyReturnsWhenTargetInvulnerable(Kind kind) throws Exception
	{
		final Defender defender = mockDefender();
		when(defender.getTemplate().getClans()).thenReturn(Collections.singleton(1));
		final Creature target = mock(Creature.class);
		when(target.isInvul()).thenReturn(true);
		final Probe ai = create(kind, defender);
		ai.setAttackTarget(target);
		
		invokePrivate(kind, ai, "factionNotifyAndSupport");
	}
	
	@ParameterizedTest
	@MethodSource("kinds")
	void factionNotifySkipsLoopWhenNoVisibleCreatures(Kind kind) throws Exception
	{
		final Defender defender = mockDefender();
		when(defender.getTemplate().getClans()).thenReturn(Collections.singleton(1));
		final Creature target = mock(Creature.class);
		when(target.isInvul()).thenReturn(false);
		final Probe ai = create(kind, defender);
		ai.setAttackTarget(target);
		
		final World world = mock(World.class);
		when(world.getVisibleObjectsInRange(any(), any(), anyInt())).thenReturn(List.of());
		try (MockedStatic<World> worlds = mockStatic(World.class))
		{
			worlds.when(World::getInstance).thenReturn(world);
			invokePrivate(kind, ai, "factionNotifyAndSupport");
		}
	}
	
	private static Defender mockDefender()
	{
		final NpcTemplate template = mock(NpcTemplate.class);
		when(template.getAIType()).thenReturn(AIType.FIGHTER);
		when(template.getClans()).thenReturn(null);
		
		final Defender defender = mock(Defender.class);
		when(defender.getTemplate()).thenReturn(template);
		when(defender.getAllSkills()).thenReturn(Collections.emptyList());
		when(defender.getPhysicalAttackRange()).thenReturn(40);
		when(defender.isConfused()).thenReturn(false);
		when(defender.isRunning()).thenReturn(true);
		return defender;
	}
	
	private static Probe create(Kind kind, Defender defender)
	{
		return kind == Kind.FORT ? new FortProbe(defender) : new CastleProbe(defender);
	}
	
	private static Field guardField(Kind kind) throws Exception
	{
		final Field field = (kind == Kind.FORT ? FortSiegeGuardAI.class : SiegeGuardAI.class).getDeclaredField(kind == Kind.FORT ? "_fortGuard" : "_castleGuard");
		field.setAccessible(true);
		return field;
	}
	
	private static Field globalAggro(Kind kind) throws Exception
	{
		final Field field = (kind == Kind.FORT ? FortSiegeGuardAI.class : SiegeGuardAI.class).getDeclaredField("_globalAggro");
		field.setAccessible(true);
		return field;
	}
	
	private static void invokePrivate(Kind kind, Object ai, String name) throws Exception
	{
		final Method method = (kind == Kind.FORT ? FortSiegeGuardAI.class : SiegeGuardAI.class).getDeclaredMethod(name);
		method.setAccessible(true);
		method.invoke(ai);
	}
	
	private interface Probe
	{
		void attacked(Creature attacker);
		
		void aggression(Creature target, int aggro);
		
		void setAttackTarget(Creature target);
	}
	
	private static final class FortProbe extends FortSiegeGuardAI implements Probe
	{
		private FortProbe(Defender creature)
		{
			super(creature);
		}
		
		@Override
		public void attacked(Creature attacker)
		{
			onActionAttacked(attacker);
		}
		
		@Override
		public void aggression(Creature target, int aggro)
		{
			onActionAggression(target, aggro);
		}
		
		@Override
		public void setAttackTarget(Creature target)
		{
			super.setAttackTarget(target);
		}
		
		@Override
		public void setIntention(Intention intention, Object arg0, Object arg1)
		{
		}
	}
	
	private static final class CastleProbe extends SiegeGuardAI implements Probe
	{
		private CastleProbe(Defender creature)
		{
			super(creature);
		}
		
		@Override
		public void attacked(Creature attacker)
		{
			onActionAttacked(attacker);
		}
		
		@Override
		public void aggression(Creature target, int aggro)
		{
			onActionAggression(target, aggro);
		}
		
		@Override
		public void setAttackTarget(Creature target)
		{
			super.setAttackTarget(target);
		}
		
		@Override
		public void setIntention(Intention intention, Object arg0, Object arg1)
		{
		}
	}
}
