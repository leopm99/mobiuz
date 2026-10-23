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
package handlers.communityboard;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.Config;
import com.l2journey.gameserver.cache.HtmCache;
import com.l2journey.gameserver.data.SpawnTable;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.data.xml.NpcData;
import com.l2journey.gameserver.handler.CommunityBoardHandler;
import com.l2journey.gameserver.model.Spawn;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.npc.DropType;
import com.l2journey.gameserver.model.actor.templates.NpcTemplate;
import com.l2journey.gameserver.model.dropcalculator.DropInfoHandler;
import com.l2journey.gameserver.model.dropcalculator.DropInfoHolder;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.skill.Skill;

class DropSearchBoardTest
{
	private final List<MockedStatic<?>> _mocks = new ArrayList<>();
	private final Map<Field, Object> _settings = new HashMap<>();
	private final List<String> _pages = new ArrayList<>();
	private Player _player;
	private NpcTemplate _npc;
	private ItemTemplate _item;
	private DropInfoHandler _index;
	private DropSearchBoard _board;
	private SpawnTable _spawns;

	private <T> MockedStatic<T> staticMock(Class<T> type)
	{
		final var value = mockStatic(type);
		_mocks.add(value);
		return value;
	}

	@BeforeEach
	void setUp() throws Exception
	{
		for (String name : List.of("ENABLE_COMMUNITY_BOARD", "COMMUNITYBOARD_ENABLED", "COMMUNITYBOARD_DROP_SEARCH", "PREMIUM_SYSTEM_ENABLED", "RATE_DROP_CHANCE_BY_ID", "RATE_DROP_AMOUNT_BY_ID", "RATE_DEATH_DROP_CHANCE_MULTIPLIER", "RATE_DEATH_DROP_AMOUNT_MULTIPLIER", "RATE_SPOIL_DROP_CHANCE_MULTIPLIER", "RATE_SPOIL_DROP_AMOUNT_MULTIPLIER"))
		{
			final Field f = Config.class.getField(name);
			_settings.put(f, f.get(null));
			if (Map.class.isAssignableFrom(f.getType())) { f.set(null, new HashMap<Integer, Float>()); }
			else if (f.getType() == float.class) { f.setFloat(null, 1); }
		}
		Config.ENABLE_COMMUNITY_BOARD = true;
		Config.COMMUNITYBOARD_ENABLED = true;
		Config.COMMUNITYBOARD_DROP_SEARCH = true;
		Config.PREMIUM_SYSTEM_ENABLED = false;
		_player = mock(Player.class, RETURNS_DEEP_STUBS);
		when(_player.getLang()).thenReturn("br");
		when(_player.getStat().getBonusDropRateMultiplier()).thenReturn(1d);
		when(_player.getStat().getBonusDropAmountMultiplier()).thenReturn(1d);
		when(_player.getStat().getBonusDropAdenaMultiplier()).thenReturn(1d);
		when(_player.getStat().getBonusSpoilRateMultiplier()).thenReturn(1d);
		_npc = mock(NpcTemplate.class);
		when(_npc.getId()).thenReturn(101);
		when(_npc.getName()).thenReturn("Goblin");
		when(_npc.getType()).thenReturn("Monster");
		when(_npc.getLevel()).thenReturn((byte) 15);
		_item = mock(ItemTemplate.class);
		when(_item.getId()).thenReturn(100);
		when(_item.getName()).thenReturn("Sword");
		final NpcData npcs = mock(NpcData.class);
		when(npcs.getTemplate(101)).thenReturn(_npc);
		staticMock(NpcData.class).when(NpcData::getInstance).thenReturn(npcs);
		final ItemData items = mock(ItemData.class);
		when(items.getTemplate(100)).thenReturn(_item);
		staticMock(ItemData.class).when(ItemData::getInstance).thenReturn(items);
		final HtmCache cache = mock(HtmCache.class);
		final String english = Files.readString(Path.of("dist/game/data/html/CommunityBoard/dropsearch/main.html"));
		final String portuguese = Files.readString(Path.of("dist/game/data/lang/br/data/html/CommunityBoard/dropsearch/main.html"));
		when(cache.getHtm(any(Player.class), anyString())).thenAnswer(i -> "br".equals(((Player) i.getArgument(0)).getLang()) ? portuguese : english);
		staticMock(HtmCache.class).when(HtmCache::getInstance).thenReturn(cache);
		final var cb = staticMock(CommunityBoardHandler.class);
		cb.when(CommunityBoardHandler::getInstance).thenReturn(mock(CommunityBoardHandler.class));
		cb.when(() -> CommunityBoardHandler.separateAndSend(anyString(), any())).thenAnswer(i -> { _pages.add(i.getArgument(0)); return null; });
		_spawns = mock(SpawnTable.class);
		staticMock(SpawnTable.class).when(SpawnTable::getInstance).thenReturn(_spawns);
		_index = mock(DropInfoHandler.class);
		when(_index.searchItems(anyString(), anyInt())).thenReturn(List.of(_item));
		when(_index.searchNpcs(anyString(), anyInt())).thenReturn(List.of(_npc));
		final List<DropInfoHolder> drops = List.of(new DropInfoHolder(_npc, 100, DropType.DROP, 1, 2, 5, 1), new DropInfoHolder(_npc, 100, DropType.SPOIL, 2, 3, 10, 0));
		when(_index.getDrop(100)).thenReturn(drops);
		when(_index.getNpcDrops(101)).thenReturn(drops);
		_board = new DropSearchBoard(_index);
	}

	@AfterEach
	void tearDown() throws Exception
	{
		for (int i = _mocks.size() - 1; i >= 0; i--) { _mocks.get(i).close(); }
		for (var e : _settings.entrySet()) { e.getKey().set(null, e.getValue()); }
	}

	private void request(String command)
	{
		assertTrue(_board.parseCommunityBoardCommand(command, _player), command);
		if (!_pages.isEmpty())
		{
			assertTrue(_pages.getLast().length() < 16383);
			assertFalse(_pages.getLast().contains("%calculator_body%"));
		}
	}

	@Test
	void homeUsesLocalizedCompleteTemplate()
	{
		request("_bbsdrop");
		assertTrue(_pages.getLast().contains("Calculadora de Drops"));
		when(_player.getLang()).thenReturn("en");
		request("_bbsdrop");
		assertTrue(_pages.getLast().contains("Drop Calculator"));
	}

	@Test
	void disabledFeatureRejectsDirectForgedBypasses()
	{
		Config.COMMUNITYBOARD_DROP_SEARCH = false;
		assertFalse(_board.parseCommunityBoardCommand("_bbsdrop;item;100;1;0", _player));
		assertTrue(_pages.isEmpty());
	}

	@Test
	void searchByItemAndNpcAndLegacyCommandsUseExpectedQuery()
	{
		request("_bbs_search_item Sword of Light");
		verify(_index).searchItems("Sword of Light", 0);
		request("_bbsdrop;npcs;1;0;goblin");
		verify(_index).searchNpcs("goblin", 0);
		assertTrue(_pages.getLast().contains("_bbsdrop;npc;101;ALL;1"));
		request("_bbs_search_drop 100 1 $order $level");
		assertTrue(_pages.getLast().contains("Goblin"));
	}

	@Test
	void searchQueryIsRetainedForPaginationAndSortWithoutStoringPlayerVariables()
	{
		request("_bbsdrop;items;1;0;Sword of Light");
		request("_bbsdrop;items;2;1");
		verify(_index).searchItems("Sword of Light", 1);
		final Player other = mock(Player.class);
		when(other.getLang()).thenReturn("en");
		assertTrue(_board.parseCommunityBoardCommand("_bbsdrop;items;1;0", other));
		verify(_index).searchItems("", 0);
	}

	@Test
	void searchesEscapeUserInputAndItemLabels()
	{
		when(_item.getName()).thenReturn("<Sword>&");
		request("_bbsdrop;items;1;0;<injected>");
		assertTrue(_pages.getLast().contains("&lt;injected&gt;"));
		assertTrue(_pages.getLast().contains("&lt;Sword&gt;&amp;"));
		assertFalse(_pages.getLast().contains("<injected>"));
	}

	@Test
	void itemSourceRowsShowGroupChanceAndCorrectDropTypes()
	{
		request("_bbsdrop;item;100;1;0");
		assertTrue(_pages.getLast().contains("5%"));
		assertTrue(_pages.getLast().contains("Drop G1"));
		assertTrue(_pages.getLast().contains("Spoil"));
		assertTrue(_pages.getLast().contains("_bbsdrop;npc;101;ALL;1"));
	}

	@Test
	void npcDropAndSpoilViewsRemainSeparate()
	{
		request("_bbsdrop;npc;101;DEATH;1");
		assertTrue(_pages.getLast().contains("Drop G1"));
		assertFalse(_pages.getLast().contains("10%"));
		request("_bbsdrop;npc;101;SPOIL;1");
		assertTrue(_pages.getLast().contains("10%"));
		assertFalse(_pages.getLast().contains("Drop G1"));
		request("_bbssearchNpcDropList_SPOIL_101_1");
		assertTrue(_pages.getLast().contains("10%"));
	}

	@Test
	void statisticsAndSkillsSupportEmptyListsAndPagination()
	{
		request("_bbsdrop;stats;101");
		assertTrue(_pages.getLast().contains("P.Atk / M.Atk"));
		request("_bbsdrop;skills;101;1");
		assertTrue(_pages.getLast().contains("Nenhum resultado"));
		final Map<Integer, Skill> skills = new LinkedHashMap<>();
		IntStream.rangeClosed(1, 11).forEach(id -> {
			Skill skill = mock(Skill.class);
			when(skill.getId()).thenReturn(id);
			when(skill.getName()).thenReturn("Skill_" + id);
			when(skill.getLevel()).thenReturn(1);
			skills.put(id, skill);
		});
		when(_npc.getSkills()).thenReturn(skills);
		request("_bbsdrop;skills;101;1");
		assertTrue(_pages.getLast().contains("Skill_1"));
		assertFalse(_pages.getLast().contains("Skill_11"));
		request("_bbsdrop;skills;101;2");
		assertTrue(_pages.getLast().contains("Skill_11"));
		request("_bbssearchShowSkills_101_0");
		assertFalse(_pages.getLast().contains("Skill_11"));
	}

	@Test
	void invalidRequestsReturnMessagesInsteadOfExceptions()
	{
		for (String command : List.of("_bbsdrop;item;100", "_bbsdrop;item;-1;1;0", "_bbsdrop;item;100;1;9", "_bbsdrop;npc;101;WRONG;1", "_bbsdrop;stats;999", "_bbsdrop;map;abc", "_bbsdrop;npcs;1;-1", "_bbsdrop;skills;101;99999999999999", "_bbssearchNpcDropList_", "_bbssearchShowSkills_101_2147483647", "_bbsdrop;items;1;0;" + "x".repeat(65)))
		{
			assertFalse(_board.parseCommunityBoardCommand(command, _player), command);
		}
	}

	@Test
	void emptyAndOutOfRangePagesRenderSafely()
	{
		when(_index.searchItems("missing", 0)).thenReturn(List.of());
		request("_bbsdrop;items;2147483647;0;missing");
		assertTrue(_pages.getLast().contains("Nenhum resultado"));
		request("_bbsdrop;item;100;-10;0");
		assertTrue(_pages.getLast().contains("Página 1/1"));
	}

	@Test
	void mapAddsOnlyRadarMarkerAndMissingSpawnIsReported()
	{
		request("_bbsdrop;map;101");
		assertEquals(0, _pages.size());
		final Spawn spawn = mock(Spawn.class);
		when(spawn.getX()).thenReturn(10);
		when(spawn.getY()).thenReturn(20);
		when(spawn.getZ()).thenReturn(30);
		when(_spawns.getAnySpawn(101)).thenReturn(spawn);
		request("_bbs_npc_trace 101");
		verify(_player.getRadar()).addMarker(10, 20, 30);
	}
}
