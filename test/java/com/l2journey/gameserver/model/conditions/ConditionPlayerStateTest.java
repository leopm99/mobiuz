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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.player.PlayerState;

/**
 * Characterization of {@link ConditionPlayerState}.
 */
class ConditionPlayerStateTest
{
	static Stream<Arguments> stateCases()
	{
		return Stream.of(
			Arguments.of("restingPlayerTrue", PlayerState.RESTING, true, true),
			Arguments.of("restingPlayerFalse", PlayerState.RESTING, true, false),
			Arguments.of("restingNonPlayerRequiredFalse", PlayerState.RESTING, false, true),
			Arguments.of("restingNonPlayerRequiredTrue", PlayerState.RESTING, false, false),
			Arguments.of("movingTrue", PlayerState.MOVING, true, true),
			Arguments.of("movingFalse", PlayerState.MOVING, true, false),
			Arguments.of("runningTrue", PlayerState.RUNNING, true, true),
			Arguments.of("runningFalse", PlayerState.RUNNING, true, false),
			Arguments.of("standingPlayerTrue", PlayerState.STANDING, true, true),
			Arguments.of("standingPlayerFalse", PlayerState.STANDING, true, false),
			Arguments.of("standingNonPlayerTrue", PlayerState.STANDING, false, true),
			Arguments.of("standingNonPlayerFalse", PlayerState.STANDING, false, false),
			Arguments.of("flyingTrue", PlayerState.FLYING, true, true),
			Arguments.of("flyingFalse", PlayerState.FLYING, true, false),
			Arguments.of("behindTrue", PlayerState.BEHIND, true, true),
			Arguments.of("behindFalse", PlayerState.BEHIND, true, false),
			Arguments.of("frontTrue", PlayerState.FRONT, true, true),
			Arguments.of("frontFalse", PlayerState.FRONT, true, false),
			Arguments.of("chaoticPlayerTrue", PlayerState.CHAOTIC, true, true),
			Arguments.of("chaoticPlayerFalse", PlayerState.CHAOTIC, true, false),
			Arguments.of("chaoticNonPlayerRequiredFalse", PlayerState.CHAOTIC, false, true),
			Arguments.of("olympiadPlayerTrue", PlayerState.OLYMPIAD, true, true),
			Arguments.of("olympiadPlayerFalse", PlayerState.OLYMPIAD, true, false),
			Arguments.of("olympiadNonPlayerRequiredFalse", PlayerState.OLYMPIAD, false, true));
	}
	
	@ParameterizedTest(name = "{0}")
	@MethodSource("stateCases")
	void coversEachPlayerStateArm(String name, PlayerState state, boolean usePlayer, boolean expectedTruePath)
	{
		final Creature effected = mock(Creature.class);
		final Creature effector;
		final boolean required;
		final boolean expected;
		
		switch (state)
		{
			case RESTING:
			{
				if (usePlayer)
				{
					final Player player = mock(Player.class);
					when(player.asPlayer()).thenReturn(player);
					when(player.isSitting()).thenReturn(expectedTruePath);
					effector = player;
					required = true;
					expected = expectedTruePath;
				}
				else
				{
					effector = mock(Creature.class);
					when(effector.asPlayer()).thenReturn(null);
					required = !expectedTruePath;
					expected = !required;
				}
				break;
			}
			case MOVING:
			{
				effector = mock(Creature.class);
				when(effector.asPlayer()).thenReturn(null);
				when(effector.isMoving()).thenReturn(expectedTruePath);
				required = true;
				expected = expectedTruePath;
				break;
			}
			case RUNNING:
			{
				effector = mock(Creature.class);
				when(effector.asPlayer()).thenReturn(null);
				when(effector.isRunning()).thenReturn(expectedTruePath);
				required = true;
				expected = expectedTruePath;
				break;
			}
			case STANDING:
			{
				if (usePlayer)
				{
					final Player player = mock(Player.class);
					when(player.asPlayer()).thenReturn(player);
					// standing when neither sitting nor moving
					when(player.isSitting()).thenReturn(!expectedTruePath);
					when(player.isMoving()).thenReturn(false);
					effector = player;
					required = true;
					expected = expectedTruePath;
				}
				else
				{
					effector = mock(Creature.class);
					when(effector.asPlayer()).thenReturn(null);
					when(effector.isMoving()).thenReturn(!expectedTruePath);
					required = true;
					expected = expectedTruePath;
				}
				break;
			}
			case FLYING:
			{
				effector = mock(Creature.class);
				when(effector.asPlayer()).thenReturn(null);
				when(effector.isFlying()).thenReturn(expectedTruePath);
				required = true;
				expected = expectedTruePath;
				break;
			}
			case BEHIND:
			{
				effector = mock(Creature.class);
				when(effector.asPlayer()).thenReturn(null);
				when(effector.isBehind(effected)).thenReturn(expectedTruePath);
				required = true;
				expected = expectedTruePath;
				break;
			}
			case FRONT:
			{
				effector = mock(Creature.class);
				when(effector.asPlayer()).thenReturn(null);
				when(effector.isInFrontOf(effected)).thenReturn(expectedTruePath);
				required = true;
				expected = expectedTruePath;
				break;
			}
			case CHAOTIC:
			{
				if (usePlayer)
				{
					final Player player = mock(Player.class);
					when(player.asPlayer()).thenReturn(player);
					when(player.getKarma()).thenReturn(expectedTruePath ? 1 : 0);
					effector = player;
					required = true;
					expected = expectedTruePath;
				}
				else
				{
					effector = mock(Creature.class);
					when(effector.asPlayer()).thenReturn(null);
					required = false;
					expected = true;
				}
				break;
			}
			case OLYMPIAD:
			{
				if (usePlayer)
				{
					final Player player = mock(Player.class);
					when(player.asPlayer()).thenReturn(player);
					when(player.isInOlympiadMode()).thenReturn(expectedTruePath);
					effector = player;
					required = true;
					expected = expectedTruePath;
				}
				else
				{
					effector = mock(Creature.class);
					when(effector.asPlayer()).thenReturn(null);
					required = false;
					expected = true;
				}
				break;
			}
			default:
			{
				throw new IllegalStateException(name);
			}
		}
		
		final ConditionPlayerState condition = new ConditionPlayerState(state, required);
		assertEquals(expected, condition.testImpl(effector, effected, null, null), name);
	}
}
