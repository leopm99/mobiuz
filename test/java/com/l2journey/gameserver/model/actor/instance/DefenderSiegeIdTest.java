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
package com.l2journey.gameserver.model.actor.instance;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.siege.clanhalls.SiegableHall;
import com.l2journey.gameserver.model.zone.type.SiegeZone;

/**
 * Hall-only siege id resolution in {@link Defender} after removing unreachable {@code activeSiegeId = 0}.
 * Uses a constructor-bypassing mock so Npc status init does not load BaseStat XML.
 */
class DefenderSiegeIdTest
{
	private static final int HALL_ID = 81;
	
	@Test
	void isAutoAttackableUsesHallIdWhenFortAndCastleAreNull() throws Exception
	{
		final Player player = mock(Player.class);
		when(player.isPlayable()).thenReturn(true);
		when(player.asPlayer()).thenReturn(player);
		when(player.getSiegeState()).thenReturn((byte) 2);
		when(player.isRegisteredOnThisSiegeField(HALL_ID)).thenReturn(true);
		
		final Defender defender = newHallOnlyDefender();
		assertFalse(defender.isAutoAttackable(player));
		verify(player).isRegisteredOnThisSiegeField(HALL_ID);
	}
	
	@Test
	void addDamageHateSkipsRegisteredDefenderOnHallSiege() throws Exception
	{
		final Player player = mock(Player.class);
		when(player.isPlayable()).thenReturn(true);
		when(player.asPlayer()).thenReturn(player);
		when(player.getSiegeState()).thenReturn((byte) 2);
		when(player.isRegisteredOnThisSiegeField(HALL_ID)).thenReturn(true);
		
		final Defender defender = newHallOnlyDefender();
		assertDoesNotThrow(() -> defender.addDamageHate(player, 0, 1));
		verify(player).isRegisteredOnThisSiegeField(HALL_ID);
	}
	
	private static Defender newHallOnlyDefender() throws Exception
	{
		final SiegeZone siegeZone = mock(SiegeZone.class);
		when(siegeZone.isActive()).thenReturn(true);
		final SiegableHall hall = mock(SiegableHall.class);
		when(hall.getId()).thenReturn(HALL_ID);
		when(hall.getSiegeZone()).thenReturn(siegeZone);
		
		final Defender defender = mock(Defender.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
		setField(defender, "_fort", null);
		setField(defender, "_castle", null);
		setField(defender, "_hall", hall);
		return defender;
	}
	
	private static void setField(Object target, String name, Object value) throws Exception
	{
		final Field field = Defender.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
