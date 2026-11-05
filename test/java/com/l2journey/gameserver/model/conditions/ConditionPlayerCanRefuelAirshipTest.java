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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.instance.AirShip;
import com.l2journey.gameserver.model.actor.instance.ControllableAirShip;
import com.l2journey.gameserver.model.skill.Skill;

/**
 * Characterization of {@link ConditionPlayerCanRefuelAirship} fuel capacity checks.
 */
class ConditionPlayerCanRefuelAirshipTest
{
	@Test
	void nullPlayerReturnsFalse()
	{
		final Creature effector = mock(Creature.class);
		when(effector.asPlayer()).thenReturn(null);
		
		final ConditionPlayerCanRefuelAirship condition = new ConditionPlayerCanRefuelAirship(10);
		assertFalse(condition.testImpl(effector, null, mock(Skill.class), null));
	}
	
	@Test
	void nullAirshipReturnsFalse()
	{
		final Player player = mockPlayer();
		when(player.getAirShip()).thenReturn(null);
		
		final ConditionPlayerCanRefuelAirship condition = new ConditionPlayerCanRefuelAirship(10);
		assertFalse(condition.testImpl(player, null, mock(Skill.class), null));
	}
	
	@Test
	void nonControllableAirshipReturnsFalse()
	{
		final Player player = mockPlayer();
		final AirShip airShip = mock(AirShip.class);
		when(player.getAirShip()).thenReturn(airShip);
		
		final ConditionPlayerCanRefuelAirship condition = new ConditionPlayerCanRefuelAirship(10);
		assertFalse(condition.testImpl(player, null, mock(Skill.class), null));
	}
	
	@Test
	void fuelPlusValueAboveMaxReturnsFalse()
	{
		final Player player = mockPlayer();
		final ControllableAirShip airShip = mock(ControllableAirShip.class);
		when(player.getAirShip()).thenReturn(airShip);
		when(airShip.getFuel()).thenReturn(90);
		when(airShip.getMaxFuel()).thenReturn(100);
		
		final ConditionPlayerCanRefuelAirship condition = new ConditionPlayerCanRefuelAirship(20);
		assertFalse(condition.testImpl(player, null, mock(Skill.class), null));
	}
	
	@Test
	void controllableAirshipWithinMaxFuelReturnsTrue()
	{
		final Player player = mockPlayer();
		final ControllableAirShip airShip = mock(ControllableAirShip.class);
		when(player.getAirShip()).thenReturn(airShip);
		when(airShip.getFuel()).thenReturn(50);
		when(airShip.getMaxFuel()).thenReturn(100);
		
		final ConditionPlayerCanRefuelAirship condition = new ConditionPlayerCanRefuelAirship(50);
		assertTrue(condition.testImpl(player, null, mock(Skill.class), null));
	}
	
	private static Player mockPlayer()
	{
		final Player player = mock(Player.class);
		when(player.isPlayer()).thenReturn(true);
		when(player.asPlayer()).thenReturn(player);
		return player;
	}
}
