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

import java.io.File;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.l2journey.Config;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.stats.BaseStat;

/**
 * Characterization of {@link ConditionPlayerBaseStats}.
 */
class ConditionPlayerBaseStatsTest
{
	private static File previousDatapackRoot;
	
	static
	{
		// Must run before MethodSource touches BaseStat (loads statBonus.xml).
		previousDatapackRoot = Config.DATAPACK_ROOT;
		Config.DATAPACK_ROOT = new File("dist/game");
	}
	
	@BeforeAll
	static void setDatapackRoot()
	{
		// BaseStat requires data/stats/statBonus.xml under DATAPACK_ROOT.
		Config.DATAPACK_ROOT = new File("dist/game");
	}
	
	@AfterAll
	static void restoreDatapackRoot()
	{
		Config.DATAPACK_ROOT = previousDatapackRoot;
	}
	
	static Stream<Arguments> baseStats()
	{
		return Stream.of(
			Arguments.of(BaseStat.INT),
			Arguments.of(BaseStat.STR),
			Arguments.of(BaseStat.CON),
			Arguments.of(BaseStat.DEX),
			Arguments.of(BaseStat.MEN),
			Arguments.of(BaseStat.WIT));
	}
	
	@ParameterizedTest
	@MethodSource("baseStats")
	void returnsTrueWhenStatAtLeastValue(BaseStat stat)
	{
		final Player player = mock(Player.class);
		when(player.asPlayer()).thenReturn(player);
		stubStat(player, stat, 40);
		
		final ConditionPlayerBaseStats condition = new ConditionPlayerBaseStats(player, stat, 40);
		assertTrue(condition.testImpl(player, null, null, null));
	}
	
	@ParameterizedTest
	@MethodSource("baseStats")
	void returnsFalseWhenStatBelowValue(BaseStat stat)
	{
		final Player player = mock(Player.class);
		when(player.asPlayer()).thenReturn(player);
		stubStat(player, stat, 39);
		
		final ConditionPlayerBaseStats condition = new ConditionPlayerBaseStats(player, stat, 40);
		assertFalse(condition.testImpl(player, null, null, null));
	}
	
	@Test
	void returnsFalseForBaseStatNone()
	{
		final Player player = mock(Player.class);
		when(player.asPlayer()).thenReturn(player);
		
		final ConditionPlayerBaseStats condition = new ConditionPlayerBaseStats(player, BaseStat.NONE, 1);
		assertFalse(condition.testImpl(player, null, null, null));
	}
	
	@Test
	void returnsFalseWhenAsPlayerNull()
	{
		final Creature effector = mock(Creature.class);
		when(effector.asPlayer()).thenReturn(null);
		
		final ConditionPlayerBaseStats condition = new ConditionPlayerBaseStats(effector, BaseStat.STR, 1);
		assertFalse(condition.testImpl(effector, null, null, null));
	}
	
	private static void stubStat(Player player, BaseStat stat, int value)
	{
		switch (stat)
		{
			case INT:
			{
				when(player.getINT()).thenReturn(value);
				break;
			}
			case STR:
			{
				when(player.getSTR()).thenReturn(value);
				break;
			}
			case CON:
			{
				when(player.getCON()).thenReturn(value);
				break;
			}
			case DEX:
			{
				when(player.getDEX()).thenReturn(value);
				break;
			}
			case MEN:
			{
				when(player.getMEN()).thenReturn(value);
				break;
			}
			case WIT:
			{
				when(player.getWIT()).thenReturn(value);
				break;
			}
			default:
			{
				break;
			}
		}
	}
}
