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
package com.l2journey.gameserver.model.dropcalculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.Config;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.data.xml.NpcData;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.npc.DropType;
import com.l2journey.gameserver.model.actor.holders.npc.DropGroupHolder;
import com.l2journey.gameserver.model.actor.holders.npc.DropHolder;
import com.l2journey.gameserver.model.actor.templates.NpcTemplate;
import com.l2journey.gameserver.model.item.ItemTemplate;

class DropCalculatorTest
{
	private final Map<Field, Object> _settings = new HashMap<>();
	private final List<MockedStatic<?>> _mocks = new ArrayList<>();
	private final Map<Integer, ItemTemplate> _items = new HashMap<>();
	private Player _player;
	private NpcTemplate _npc;

	@BeforeEach
	void setUp() throws Exception
	{
		for (String name : List.of("PREMIUM_SYSTEM_ENABLED", "RATE_DROP_CHANCE_BY_ID", "RATE_DROP_AMOUNT_BY_ID", "PREMIUM_RATE_DROP_CHANCE_BY_ID", "PREMIUM_RATE_DROP_AMOUNT_BY_ID", "RATE_DEATH_DROP_CHANCE_MULTIPLIER", "RATE_DEATH_DROP_AMOUNT_MULTIPLIER", "RATE_RAID_DROP_CHANCE_MULTIPLIER", "RATE_RAID_DROP_AMOUNT_MULTIPLIER", "RATE_HERB_DROP_CHANCE_MULTIPLIER", "RATE_HERB_DROP_AMOUNT_MULTIPLIER", "RATE_SPOIL_DROP_CHANCE_MULTIPLIER", "RATE_SPOIL_DROP_AMOUNT_MULTIPLIER", "PREMIUM_RATE_DROP_CHANCE", "PREMIUM_RATE_DROP_AMOUNT", "PREMIUM_RATE_SPOIL_CHANCE", "PREMIUM_RATE_SPOIL_AMOUNT"))
		{
			final Field f = Config.class.getField(name);
			_settings.put(f, f.get(null));
			if (Map.class.isAssignableFrom(f.getType())) { f.set(null, new HashMap<Integer, Float>()); }
			else if (f.getType() == float.class) { f.setFloat(null, 1); }
			else if (f.getType() == double.class) { f.setDouble(null, 1); }
			else if (f.getType() == boolean.class) { f.setBoolean(null, false); }
		}
		_player = mock(Player.class, RETURNS_DEEP_STUBS);
		when(_player.getStat().getBonusDropRateMultiplier()).thenReturn(1d);
		when(_player.getStat().getBonusDropAmountMultiplier()).thenReturn(1d);
		when(_player.getStat().getBonusDropAdenaMultiplier()).thenReturn(1d);
		when(_player.getStat().getBonusSpoilRateMultiplier()).thenReturn(1d);
		_npc = npc(101, "Goblin", 15);
		final ItemData data = mock(ItemData.class);
		when(data.getTemplate(anyInt())).thenAnswer(i -> _items.get(i.getArgument(0)));
		final var mock = mockStatic(ItemData.class);
		_mocks.add(mock);
		mock.when(ItemData::getInstance).thenReturn(data);
		item(57, "Adena");
		item(100, "Sword");
	}

	@AfterEach
	void tearDown() throws Exception
	{
		for (int i = _mocks.size() - 1; i >= 0; i--) { _mocks.get(i).close(); }
		for (var entry : _settings.entrySet()) { entry.getKey().set(null, entry.getValue()); }
	}

	private NpcTemplate npc(int id, String name, int level)
	{
		final NpcTemplate result = mock(NpcTemplate.class);
		when(result.getId()).thenReturn(id);
		when(result.getName()).thenReturn(name);
		when(result.getLevel()).thenReturn((byte) level);
		when(result.getType()).thenReturn("Monster");
		return result;
	}

	private ItemTemplate item(int id, String name)
	{
		final ItemTemplate result = mock(ItemTemplate.class);
		when(result.getId()).thenReturn(id);
		when(result.getName()).thenReturn(name);
		_items.put(id, result);
		return result;
	}

	private DropInfoHolder drop(int id, boolean spoil, double chance)
	{
		return new DropInfoHolder(_npc, id, spoil ? DropType.SPOIL : DropType.DROP, 2, 5, chance, 0);
	}

	@Test
	void indexMultipliesGroupProbabilityAndKeepsSeparateSpoilAndNormalRolls()
	{
		final DropGroupHolder group = new DropGroupHolder(20);
		group.addDrop(new DropHolder(DropType.DROP, 100, 1, 2, 25));
		when(_npc.getDropGroups()).thenReturn(List.of(group));
		when(_npc.getDropList()).thenReturn(List.of(new DropHolder(DropType.DROP, 100, 3, 4, 10)));
		when(_npc.getSpoilList()).thenReturn(List.of(new DropHolder(DropType.SPOIL, 100, 5, 6, 30)));
		final DropInfoHandler index = new DropInfoHandler(List.of(_npc), _items::get);
		assertEquals(3, index.getDrop(100).size());
		assertEquals(5d, index.getDrop(100).getFirst().chance());
		assertEquals(1, index.getDrop(100).getFirst().group());
		assertTrue(index.getDrop(100).getLast().isSpoil());
		assertEquals(3, index.getNpcDrops(101).size());
	}

	@Test
	void herbsAndMissingItemsAreExcludedButAdenaIsSearchable()
	{
		final ItemTemplate herb = item(900, "Herb");
		when(herb.hasExImmediateEffect()).thenReturn(true);
		when(_npc.getDropList()).thenReturn(List.of(new DropHolder(DropType.DROP, 57, 1, 2, 100), new DropHolder(DropType.DROP, 900, 1, 1, 50), new DropHolder(DropType.DROP, 999, 1, 1, 50)));
		final DropInfoHandler index = new DropInfoHandler(List.of(_npc), _items::get);
		assertEquals(1, index.getNpcDrops(101).size());
		assertEquals(57, index.searchItems("adena", 0).getFirst().getId());
		assertTrue(index.getDrop(900).isEmpty());
	}

	@Test
	void unknownIdsReturnEmptyImmutableCollections()
	{
		final DropInfoHandler index = new DropInfoHandler(List.of(_npc), _items::get);
		assertTrue(index.getDrop(-1).isEmpty());
		assertTrue(index.getNpcDrops(-1).isEmpty());
		assertThrows(UnsupportedOperationException.class, () -> index.getNpcDrops(101).add(drop(100, false, 1)));
	}

	@Test
	void queriesMatchNamesAndExactIdsWithDeterministicSorting()
	{
		final NpcTemplate other = npc(102, "Alpha Goblin", 30);
		when(_npc.getDropList()).thenReturn(List.of(new DropHolder(DropType.DROP, 100, 1, 1, 10)));
		final DropInfoHandler index = new DropInfoHandler(List.of(_npc, other), _items::get);
		assertEquals(List.of(other, _npc), index.searchNpcs("goblin", 0));
		assertEquals(List.of(other, _npc), index.searchNpcs("GOBLIN", 1));
		assertEquals(List.of(_npc), index.searchNpcs("101", 0));
		assertEquals(100, index.searchItems("100", 0).getFirst().getId());
		assertTrue(index.searchItems("missing", 0).isEmpty());
	}

	@Test
	void npcDataRevisionTriggersLiveIndexRefresh()
	{
		final NpcData data = mock(NpcData.class);
		final var mock = mockStatic(NpcData.class);
		_mocks.add(mock);
		mock.when(NpcData::getInstance).thenReturn(data);
		when(data.getRevision()).thenReturn(1000L);
		when(data.getTemplates(any())).thenReturn(List.of(_npc));
		when(_npc.getDropList()).thenReturn(List.of(new DropHolder(DropType.DROP, 100, 1, 1, 10)));
		assertFalse(DropInfoHandler.getInstance().getDrop(100).isEmpty());
		when(data.getRevision()).thenReturn(1001L);
		when(data.getTemplates(any())).thenReturn(List.of());
		assertTrue(DropInfoHandler.getInstance().getDrop(100).isEmpty());
	}

	@Test
	void pageBoundsAndEmptyResultsAreSafe()
	{
		assertEquals(1, DropInfoFunctions.page(List.of(), -9, 10).number());
		assertTrue(DropInfoFunctions.page(List.of(), Integer.MAX_VALUE, 10).entries().isEmpty());
		final List<Integer> values = java.util.stream.IntStream.range(0, 21).boxed().toList();
		assertEquals(List.of(20), DropInfoFunctions.page(values, Integer.MAX_VALUE, 10).entries());
		assertEquals(0, DropInfoFunctions.page(values, -1, 10).entries().getFirst());
		assertEquals(3, DropInfoFunctions.page(values, 2, 10).total());
		assertThrows(IllegalArgumentException.class, () -> DropInfoFunctions.page(values, 1, 0));
	}

	@Test
	void chancesArePercentagesWithoutMillionScaleConversion()
	{
		assertEquals(25d, DropInfoFunctions.estimate(drop(100, false, 25), _player).chance());
		assertEquals("25%", DropInfoFunctions.percent(25));
		assertEquals("0.000003%", DropInfoFunctions.percent(0.000003));
		assertEquals("0.00000001%", DropInfoFunctions.percent(0.00000001));
	}

	@Test
	void normalRatesPremiumAndPlayerBonusesAreCombinedAndChanceCapsAt100()
	{
		Config.RATE_DEATH_DROP_CHANCE_MULTIPLIER = 2;
		Config.RATE_DEATH_DROP_AMOUNT_MULTIPLIER = 3;
		Config.PREMIUM_SYSTEM_ENABLED = true;
		Config.PREMIUM_RATE_DROP_CHANCE = 2;
		Config.PREMIUM_RATE_DROP_AMOUNT = 2;
		when(_player.hasPremiumStatus()).thenReturn(true);
		when(_player.getStat().getBonusDropRateMultiplier()).thenReturn(2d);
		when(_player.getStat().getBonusDropAmountMultiplier()).thenReturn(1.5d);
		final var adjusted = DropInfoFunctions.estimate(drop(100, false, 20), _player);
		assertEquals(100, adjusted.chance());
		assertEquals(18, adjusted.min());
		assertEquals(45, adjusted.max());
	}

	@Test
	void perItemOverridesAndAdenaBonusTakePrecedence()
	{
		Config.RATE_DEATH_DROP_CHANCE_MULTIPLIER = 20;
		Config.RATE_DEATH_DROP_AMOUNT_MULTIPLIER = 20;
		Config.RATE_DROP_CHANCE_BY_ID.put(57, 200f);
		Config.RATE_DROP_AMOUNT_BY_ID.put(57, 2f);
		when(_player.getStat().getBonusDropAdenaMultiplier()).thenReturn(3d);
		final var adjusted = DropInfoFunctions.estimate(drop(57, false, 0.5), _player);
		assertEquals(50, adjusted.chance());
		assertEquals(12, adjusted.min());
	}

	@Test
	void spoilUsesItsOwnRatesAndDoesNotUseDropAmountBuff()
	{
		Config.RATE_SPOIL_DROP_CHANCE_MULTIPLIER = 2;
		Config.RATE_SPOIL_DROP_AMOUNT_MULTIPLIER = 3;
		Config.PREMIUM_SYSTEM_ENABLED = true;
		Config.PREMIUM_RATE_SPOIL_CHANCE = 2;
		Config.PREMIUM_RATE_SPOIL_AMOUNT = 2;
		when(_player.hasPremiumStatus()).thenReturn(true);
		when(_player.getStat().getBonusSpoilRateMultiplier()).thenReturn(2d);
		when(_player.getStat().getBonusDropAmountMultiplier()).thenReturn(10d);
		final var adjusted = DropInfoFunctions.estimate(drop(100, true, 10), _player);
		assertEquals(80, adjusted.chance());
		assertEquals(12, adjusted.min());
	}

	@Test
	void raidUsesRaidRatesAndDoesNotGetGenericPremiumMultipliers()
	{
		when(_npc.getType()).thenReturn("RaidBoss");
		Config.RATE_RAID_DROP_CHANCE_MULTIPLIER = 3;
		Config.RATE_RAID_DROP_AMOUNT_MULTIPLIER = 2;
		Config.PREMIUM_SYSTEM_ENABLED = true;
		Config.PREMIUM_RATE_DROP_CHANCE = 10;
		Config.PREMIUM_RATE_DROP_AMOUNT = 10;
		when(_player.hasPremiumStatus()).thenReturn(true);
		final var adjusted = DropInfoFunctions.estimate(drop(100, false, 10), _player);
		assertEquals(30, adjusted.chance());
		assertEquals(4, adjusted.min());
	}

	@Test
	void quantitiesUseSameTruncationAsLootEngine()
	{
		Config.RATE_DEATH_DROP_AMOUNT_MULTIPLIER = 0.75f;
		final var adjusted = DropInfoFunctions.estimate(drop(100, false, 10), _player);
		assertEquals(1, adjusted.min());
		assertEquals(3, adjusted.max());
	}

	@Test
	void formattingEscapesHtmlAndUsesStableDecimalSeparator()
	{
		final Locale previous = Locale.getDefault();
		try
		{
			Locale.setDefault(Locale.GERMANY);
			assertEquals("1.25%", DropInfoFunctions.percent(1.25));
			assertEquals("&lt;script&gt;&amp;&quot;", DropInfoFunctions.escape("<script>&\""));
		}
		finally { Locale.setDefault(previous); }
	}
}
