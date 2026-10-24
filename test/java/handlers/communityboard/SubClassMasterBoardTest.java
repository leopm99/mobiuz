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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.Config;
import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.data.enums.CategoryType;
import com.l2journey.gameserver.data.xml.CategoryData;
import com.l2journey.gameserver.data.xml.ClassListData;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.handler.CommunityBoardHandler;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.creature.Race;
import com.l2journey.gameserver.model.actor.enums.player.PlayerClass;
import com.l2journey.gameserver.model.actor.holders.player.SubClassHolder;
import com.l2journey.gameserver.model.events.EventDispatcher;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.olympiad.OlympiadManager;
import com.l2journey.gameserver.model.quest.QuestState;

class SubClassMasterBoardTest
{
	private final List<MockedStatic<?>> _mocks = new ArrayList<>();
	private final Map<Field, Object> _config = new HashMap<>();
	private final Map<Integer, SubClassHolder> _subclasses = new TreeMap<>();
	private final List<String> _pages = new ArrayList<>();
	private final AtomicInteger _active = new AtomicInteger();
	private Player _player;
	private SubClassMasterBoard _board;
	private Connection _connection;
	private final List<String> _sql = new ArrayList<>();

	private <T> MockedStatic<T> staticMock(Class<T> type)
	{
		final MockedStatic<T> result = mockStatic(type);
		_mocks.add(result);
		return result;
	}

	@BeforeEach
	void setUp() throws Exception
	{
		for (String name : List.of("ENABLE_COMMUNITY_BOARD", "COMMUNITYBOARD_ENABLED", "SUBCLASS_MASTER_BOARD", "SUBCLASS_BUY_ITEM_ID", "SUBCLASS_BUY_ITEM_COUNT", "MAX_SUBCLASS", "ALT_GAME_SUBCLASS_WITHOUT_QUESTS", "COMMUNITYBOARD_KARMA_DISABLED", "COMMUNITYBOARD_PEACE_ONLY", "COMMUNITYBOARD_BLACKSMITH", "COLOR_CHANGE_SERVICE_ENABLED"))
		{
			final Field field = Config.class.getField(name);
			_config.put(field, field.get(null));
		}
		Config.COLOR_CHANGE_SERVICE_ENABLED = false;
		Config.ENABLE_COMMUNITY_BOARD = true;
		Config.COMMUNITYBOARD_ENABLED = true;
		Config.SUBCLASS_MASTER_BOARD = true;
		Config.SUBCLASS_BUY_ITEM_ID = 57;
		Config.SUBCLASS_BUY_ITEM_COUNT = 0;
		Config.MAX_SUBCLASS = 3;
		Config.ALT_GAME_SUBCLASS_WITHOUT_QUESTS = true;
		Config.COMMUNITYBOARD_KARMA_DISABLED = true;
		Config.COMMUNITYBOARD_PEACE_ONLY = false;

		final CategoryData categories = mock(CategoryData.class);
		when(categories.getCategoryByType(CategoryType.THIRD_CLASS_GROUP)).thenReturn(Arrays.stream(PlayerClass.values()).filter(c -> c.level() == 2).map(PlayerClass::getId).collect(Collectors.toSet()));
		when(categories.isInCategory(any(), anyInt())).thenAnswer(i -> PlayerClass.getPlayerClass(i.getArgument(1)).level() >= 2);
		staticMock(CategoryData.class).when(CategoryData::getInstance).thenReturn(categories);
		staticMock(ClassListData.class).when(ClassListData::getInstance).thenReturn(mock(ClassListData.class));
		staticMock(OlympiadManager.class).when(OlympiadManager::getInstance).thenReturn(mock(OlympiadManager.class));
		staticMock(EventDispatcher.class).when(EventDispatcher::getInstance).thenReturn(mock(EventDispatcher.class));
		final MockedStatic<CommunityBoardHandler> community = staticMock(CommunityBoardHandler.class);
		community.when(CommunityBoardHandler::getInstance).thenReturn(mock(CommunityBoardHandler.class));
		community.when(() -> CommunityBoardHandler.separateAndSend(anyString(), any())).thenAnswer(i -> { _pages.add(i.getArgument(0)); return null; });

		_player = mock(Player.class, RETURNS_DEEP_STUBS);
		when(_player.getLang()).thenReturn("br");
		when(_player.getTransformation()).thenReturn(null);
		when(_player.getLevel()).thenReturn(80);
		when(_player.getBaseClass()).thenReturn(PlayerClass.DUELIST.getId());
		when(_player.getActiveClass()).thenReturn(PlayerClass.DUELIST.getId());
		when(_player.getPlayerClass()).thenReturn(PlayerClass.DUELIST);
		when(_player.getRace()).thenReturn(Race.HUMAN);
		when(_player.getSubClasses()).thenReturn(_subclasses);
		when(_player.getTotalSubClasses()).thenAnswer(_ -> _subclasses.size());
		when(_player.getClassIndex()).thenAnswer(_ -> _active.get());
		doAnswer(i -> { _active.set(i.getArgument(0)); return null; }).when(_player).setActiveClass(anyInt());
		when(_player.isInventoryUnder90(true)).thenReturn(true);
		when(_player.getClient().getFloodProtectors().canChangeSubclass()).thenReturn(true);
		_board = new SubClassMasterBoard();
	}

	@AfterEach
	void tearDown() throws Exception
	{
		for (int i = _mocks.size() - 1; i >= 0; i--) { _mocks.get(i).close(); }
		for (var entry : _config.entrySet()) { entry.getKey().set(null, entry.getValue()); }
	}

	private boolean command(String action)
	{
		return _board.parseCommunityBoardCommand("_bbssubclassmaster" + action, _player);
	}

	private void subclass(int slot, PlayerClass type, int level)
	{
		final SubClassHolder sub = mock(SubClassHolder.class);
		when(sub.getId()).thenReturn(type.getId());
		when(sub.getClassIndex()).thenReturn(slot);
		when(sub.getLevel()).thenReturn((byte) level);
		_subclasses.put(slot, sub);
	}

	private void fee()
	{
		Config.SUBCLASS_BUY_ITEM_COUNT = 100;
		final ItemData items = mock(ItemData.class);
		final ItemTemplate adena = mock(ItemTemplate.class);
		when(adena.getName()).thenReturn("Adena");
		when(items.getTemplate(57)).thenReturn(adena);
		staticMock(ItemData.class).when(ItemData::getInstance).thenReturn(items);
		when(_player.getInventory().getInventoryItemCount(57, -1)).thenReturn(100L);
		when(_player.destroyItemByItemId(ItemProcessType.FEE, 57, 100L, _player, true)).thenReturn(true);
	}

	private void database(boolean fail) throws Exception
	{
		_connection = mock(Connection.class);
		staticMock(DatabaseFactory.class).when(DatabaseFactory::getConnection).thenReturn(_connection);
		when(_connection.prepareStatement(anyString())).thenAnswer(i -> {
			final String sql = i.getArgument(0);
			_sql.add(sql);
			final PreparedStatement statement = mock(PreparedStatement.class);
			if (fail && sql.contains("character_skills_save")) { when(statement.executeUpdate()).thenThrow(new SQLException("Test failure")); }
			else { when(statement.executeUpdate()).thenReturn(1); }
			return statement;
		});
	}

	@Test
	void disabledFeatureRejectsForgedAction()
	{
		Config.SUBCLASS_MASTER_BOARD = false;
		assertFalse(command(";add_subclass;9"));
		verify(_player, never()).addSubClass(anyInt(), anyInt());
	}

	@Test
	void malformedBypassesCannotChangeClasses()
	{
		for (String action : List.of(";add_subclassExtra;9", ";add_subclass;9;extra", ";switch_subclass;abc", ";change_to_base;0", ";remove_subclass;"))
		{
			assertFalse(command(action));
		}
		verify(_player, never()).addSubClass(anyInt(), anyInt());
		verify(_player, never()).setActiveClass(anyInt());
	}

	@Test
	void combatAndChangingClassBlockActions()
	{
		when(_player.isInCombat()).thenReturn(true);
		assertFalse(command(";change_to_base"));
		when(_player.isInCombat()).thenReturn(false);
		when(_player.isChangingClass()).thenReturn(true);
		assertFalse(command(";change_to_base"));
		verify(_player, never()).setActiveClass(anyInt());
	}

	@Test
	void underLevelAndForbiddenClassAreNotCharged()
	{
		fee();
		when(_player.getLevel()).thenReturn(74);
		command(";add_subclass;9");
		when(_player.getLevel()).thenReturn(80);
		command(";add_subclass;51"); // Overlord is never available.
		command(";add_subclass;-1");
		verify(_player, never()).destroyItemByItemId(any(), anyInt(), anyLong(), any(), anyBoolean());
	}

	@Test
	void questRequirementsAreRespectedBeforePayment()
	{
		fee();
		Config.ALT_GAME_SUBCLASS_WITHOUT_QUESTS = false;
		command(";add_subclass;9");
		verify(_player, never()).addSubClass(anyInt(), anyInt());
		verify(_player, never()).destroyItemByItemId(any(), anyInt(), anyLong(), any(), anyBoolean());
		final QuestState completed = mock(QuestState.class);
		when(completed.isCompleted()).thenReturn(true);
		when(_player.getQuestState(anyString())).thenReturn(completed);
		when(_player.addSubClass(9, 1)).thenReturn(true);
		command(";add_subclass;9");
		verify(_player).addSubClass(9, 1);
	}

	@Test
	void failedCreationRefundsFeeAndDoesNotActivate()
	{
		fee();
		command(";add_subclass;9");
		verify(_player).destroyItemByItemId(ItemProcessType.FEE, 57, 100L, _player, true);
		verify(_player).addItem(ItemProcessType.REFUND, 57, 100L, _player, true);
		verify(_player, never()).setActiveClass(anyInt());
	}

	@Test
	void insufficientFundsNeverCreatesSubclass()
	{
		fee();
		when(_player.getInventory().getInventoryItemCount(57, -1)).thenReturn(99L);
		command(";add_subclass;9");
		verify(_player, never()).addSubClass(anyInt(), anyInt());
	}

	@Test
	void creationReusesFirstFreeSlotAndChargesOnce()
	{
		fee();
		subclass(2, PlayerClass.PALADIN, 75);
		when(_player.addSubClass(9, 1)).thenReturn(true);
		command(";add_subclass;9");
		verify(_player).addSubClass(9, 1);
		verify(_player).setActiveClass(1);
		verify(_player).destroyItemByItemId(ItemProcessType.FEE, 57, 100L, _player, true);
		verify(_player, never()).addItem(eq(ItemProcessType.REFUND), anyInt(), anyLong(), any(), anyBoolean());
	}

	@Test
	void lowExistingSubclassAndDuplicatePreventAddition()
	{
		subclass(1, PlayerClass.HAWKEYE, 74);
		command(";add_subclass;9");
		subclass(1, PlayerClass.HAWKEYE, 75);
		command(";add_subclass;9");
		verify(_player, never()).addSubClass(anyInt(), anyInt());
	}

	@Test
	void missingSlotCannotSwitchButBaseCanBeActivated()
	{
		_active.set(1);
		command(";switch_subclass;99");
		verify(_player, never()).setActiveClass(99);
		command(";change_to_base");
		assertEquals(0, _active.get());
	}

	@Test
	void removalConfirmationDoesNotDelete()
	{
		subclass(1, PlayerClass.HAWKEYE, 75);
		command(";confirm_remove;1");
		assertTrue(_subclasses.containsKey(1));
		assertTrue(_pages.getLast().contains("Confirmar remocao"));
		assertTrue(_pages.getLast().contains("remove_subclass;1"));
	}

	@Test
	void removalCommitsAllClassDataBeforeUpdatingMemory() throws Exception
	{
		database(false);
		subclass(1, PlayerClass.HAWKEYE, 75);
		_active.set(1);
		command(";remove_subclass;1");
		assertEquals(0, _active.get());
		assertFalse(_subclasses.containsKey(1));
		assertEquals(5, _sql.size());
		assertTrue(_sql.getFirst().contains("character_hennas WHERE charId=? AND class_index=?"));
		verify(_connection).commit();
		verify(_connection, never()).rollback();
	}

	@Test
	void failedDeletionRollsBackAndPreservesSubclass() throws Exception
	{
		database(true);
		subclass(1, PlayerClass.HAWKEYE, 75);
		command(";remove_subclass;1");
		assertTrue(_subclasses.containsKey(1));
		verify(_connection).rollback();
		verify(_connection, never()).commit();
		verify(_player, never()).sendMessage("Subclasse removida com sucesso.");
	}

	@Test
	void mainPageSupportsBothLanguagesAndFitsBoardPacketLimit()
	{
		command("");
		assertTrue(_pages.getLast().contains("Classe Atual"));
		assertTrue(_pages.getLast().length() < 8192);
		when(_player.getLang()).thenReturn("en");
		command("");
		assertTrue(_pages.getLast().contains("Current Class"));
		assertTrue(_pages.getLast().contains("add_subclass;9"));
		assertFalse(_pages.getLast().contains("add_subclass;51\""));
	}
	@Test
	void elvesCannotSelectDarkElfClassesAndKnightsCannotSelectSiblingKnights()
	{
		when(_player.getBaseClass()).thenReturn(PlayerClass.PALADIN.getId());
		when(_player.getPlayerClass()).thenReturn(PlayerClass.PALADIN);
		command(";add_subclass;6"); // Dark Avenger: same knight group.
		when(_player.getRace()).thenReturn(Race.ELF);
		when(_player.getBaseClass()).thenReturn(PlayerClass.SPELLSINGER.getId());
		when(_player.getPlayerClass()).thenReturn(PlayerClass.SPELLSINGER);
		command(";add_subclass;33"); // Shillien Elder: Dark Elf.
		verify(_player, never()).addSubClass(anyInt(), anyInt());
	}

	@Test
	void kamaelInspectorRequiresSecondSubclassAtLevel75()
	{
		when(_player.getRace()).thenReturn(Race.KAMAEL);
		when(_player.getBaseClass()).thenReturn(PlayerClass.BERSERKER.getId());
		when(_player.getPlayerClass()).thenReturn(PlayerClass.BERSERKER);
		command(";add_subclass;135");
		verify(_player, never()).addSubClass(anyInt(), anyInt());
		subclass(1, PlayerClass.MALE_SOULBREAKER, 75);
		subclass(2, PlayerClass.ARBALESTER, 75);
		when(_player.addSubClass(135, 3)).thenReturn(true);
		command(";add_subclass;135");
		verify(_player).addSubClass(135, 3);
	}

	@Test
	void invalidFeeItemCannotMakeSubclassFree()
	{
		fee();
		Config.SUBCLASS_BUY_ITEM_ID = 0;
		command(";add_subclass;9");
		verify(_player, never()).addSubClass(anyInt(), anyInt());
	}

	@Test
	void failedBaseSwitchDoesNotDeleteActiveSubclass() throws Exception
	{
		database(false);
		subclass(1, PlayerClass.HAWKEYE, 75);
		_active.set(1);
		doNothing().when(_player).setActiveClass(0);
		command(";remove_subclass;1");
		assertTrue(_subclasses.containsKey(1));
		verifyNoInteractions(_connection);
	}

	@Test
	void homepageLinkFollowsFeatureFlag() throws Exception
	{
		final var method = HomeBoard.class.getDeclaredMethod("buildHotFeaturesBlock", int.class, String.class);
		method.setAccessible(true);
		Config.COMMUNITYBOARD_BLACKSMITH = false;
		final String enabled = (String) method.invoke(null, 720, "br");
		assertTrue(enabled.contains("Serviços"));
		assertTrue(enabled.contains("merchant/blacksmithMain.html"));
		assertFalse(enabled.contains("bypass _bbssubclassmaster"));
		Config.SUBCLASS_MASTER_BOARD = false;
		assertFalse(((String) method.invoke(null, 720, "br")).contains("merchant/blacksmithMain.html"));
	}

	@Test
	void servicesWrapIntoRowsAndRespectIndependentFeatureFlags() throws Exception
	{
		final var method = HomeBoard.class.getDeclaredMethod("buildServicesRows", String.class);
		method.setAccessible(true);
		Config.COMMUNITYBOARD_BLACKSMITH = true;
		String html = (String) method.invoke(null, "br");
		assertEquals(2, html.split("<tr>", -1).length - 1);
		assertTrue(html.contains("merchant/warehouse.html"));
		assertTrue(html.contains("merchant/symbolMaker.html"));
		assertTrue(html.contains("merchant/blacksmith.html"));
		assertTrue(html.contains("bypass _bbssubclassmaster"));
		assertTrue(html.contains("Gerencie suas subclasses."));
		Config.COMMUNITYBOARD_BLACKSMITH = false;
		html = (String) method.invoke(null, "en");
		assertFalse(html.contains("merchant/"));
		assertTrue(html.contains("Manage your subclasses."));
		Config.SUBCLASS_MASTER_BOARD = false;
		assertTrue(((String) method.invoke(null, "en")).contains("No services are currently available."));
	}

	/** Check the emitted HTML tree after normalizing the client's void tags/attributes. */
	private static void assertClientHtmlStructure(String html) throws Exception
	{
		assertTrue(html.length() < 8192, "Keep the subclass page short enough for one ShowBoard content packet");
		final String xml = html.replaceAll("<(br1?|img|button)([^>]*)>", "<$1$2/>")
			.replaceAll("(\\s[a-zA-Z][a-zA-Z0-9]*)=([^\\s\"'=<>`]+)", "$1=\"$2\"");
		final var factory = javax.xml.parsers.DocumentBuilderFactory.newDefaultInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD, "");
		factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
		final var document = factory.newDocumentBuilder().parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
		final var tables = document.getElementsByTagName("table");
		for (int i = 0; i < tables.getLength(); i++)
		{
			assertEquals(0, ((org.w3c.dom.Element) tables.item(i)).getElementsByTagName("table").getLength(), "Use independent tables instead of nested page layout tables");
		}
	}

	@Test
	void everySubclassChoiceRemainsReachableAcrossSmallPages() throws Exception
	{
		final var pattern = java.util.regex.Pattern.compile("add_subclass;(\\d+)\"");
		final var choices = new java.util.HashSet<Integer>();
		command("");
		final String first = _pages.getLast();
		final var pages = java.util.regex.Pattern.compile("Pagina 1/(\\d+)").matcher(first);
		assertTrue(pages.find());
		final int total = Integer.parseInt(pages.group(1));
		assertTrue(total > 1);
		for (int page = 1; page <= total; page++)
		{
			assertTrue(command(";page;" + page));
			final String html = _pages.getLast();
			assertClientHtmlStructure(html);
			final var matcher = pattern.matcher(html);
			int count = 0;
			while (matcher.find())
			{
				assertTrue(choices.add(Integer.parseInt(matcher.group(1))), "No duplicate class choices across pages");
				count++;
			}
			assertTrue((count > 0) && (count <= 6));
		}
		final var expected = Arrays.stream(PlayerClass.values()).filter(c -> (c.level() == 2) && (c.getRace() != Race.KAMAEL) && !java.util.Set.of(PlayerClass.GLADIATOR, PlayerClass.OVERLORD, PlayerClass.WARSMITH).contains(c)).map(PlayerClass::getId).collect(Collectors.toSet());
		assertEquals(expected, choices);
		final String last = _pages.getLast();
		command(";page;2147483647");
		assertEquals(last, _pages.getLast());
		command(";page;-1");
		assertEquals(first, _pages.getLast());
		assertFalse(command(";page;invalid"));
		verify(_player, never()).addSubClass(anyInt(), anyInt());
		verify(_player, never()).setActiveClass(anyInt());
	}

	@Test
	void classAndItemNamesCannotBreakHtmlTextOrButtonAttributes() throws Exception
	{
		when(ClassListData.getInstance().getClass(anyInt())).thenAnswer(i -> new com.l2journey.gameserver.model.actor.holders.player.ClassInfoHolder(PlayerClass.getPlayerClass(i.getArgument(0)), "Class <broken> & \"quoted\"", null));
		fee();
		when(ItemData.getInstance().getTemplate(57).getName()).thenReturn("Coin <broken> & \"quoted\"");
		when(_player.getLang()).thenReturn("en");
		command("");
		assertClientHtmlStructure(_pages.getLast());
		assertFalse(_pages.getLast().contains("<broken>"));
		assertFalse(_pages.getLast().contains("Classes & Subclasses"));
		assertTrue(_pages.getLast().contains("&lt;broken&gt; &amp; &quot;quoted&quot;"));
		subclass(1, PlayerClass.HAWKEYE, 75);
		command(";confirm_remove;1");
		assertClientHtmlStructure(_pages.getLast());
	}

	@Test
	void realClassNamesRenderInBothLanguagesForAllRacesAndExistingSubclassSlots() throws Exception
	{
		final var factory = javax.xml.parsers.DocumentBuilderFactory.newDefaultInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD, "");
		factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
		final var nodes = factory.newDocumentBuilder().parse(new java.io.File("dist/game/data/stats/chars/classList.xml")).getElementsByTagName("class");
		final Map<Integer, com.l2journey.gameserver.model.actor.holders.player.ClassInfoHolder> names = new HashMap<>();
		for (int i = 0; i < nodes.getLength(); i++)
		{
			final var node = (org.w3c.dom.Element) nodes.item(i);
			final int id = Integer.parseInt(node.getAttribute("classId"));
			names.put(id, new com.l2journey.gameserver.model.actor.holders.player.ClassInfoHolder(PlayerClass.getPlayerClass(id), node.getAttribute("name"), null));
		}
		when(ClassListData.getInstance().getClass(anyInt())).thenAnswer(i -> names.get(i.getArgument(0)));
		for (String lang : java.util.List.of("en", "br"))
		{
			when(_player.getLang()).thenReturn(lang);
			for (PlayerClass base : java.util.List.of(PlayerClass.SAGITTARIUS, PlayerClass.MOONLIGHT_SENTINEL, PlayerClass.GHOST_SENTINEL, PlayerClass.TITAN, PlayerClass.FORTUNE_SEEKER, PlayerClass.DOOMBRINGER))
			{
				when(_player.getRace()).thenReturn(base.getRace());
				when(_player.getPlayerClass()).thenReturn(base);
				when(_player.getBaseClass()).thenReturn(base.getId());
				when(_player.getActiveClass()).thenReturn(base.getId());
				for (boolean female : java.util.List.of(false, true))
				{
					when(_player.getAppearance().isFemale()).thenReturn(female);
					command("");
					assertClientHtmlStructure(_pages.getLast());
				}
			}
		}
		subclass(1, PlayerClass.HAWKEYE, 75);
		subclass(2, PlayerClass.PALADIN, 75);
		subclass(3, PlayerClass.WARLOCK, 75);
		_active.set(1);
		command("");
		assertClientHtmlStructure(_pages.getLast());
		assertTrue(_pages.getLast().contains("confirm_remove;3"));
	}

	@Test
	void reportedEnglishDuelist85WithWarlockSubclassRendersEveryPageSafely() throws Exception
	{
		when(_player.getLang()).thenReturn("en");
		when(_player.getLevel()).thenReturn(85);
		when(ClassListData.getInstance().getClass(anyInt())).thenAnswer(invocation -> {
			final PlayerClass type = PlayerClass.getPlayerClass(invocation.getArgument(0));
			final String name = type == PlayerClass.DUELIST ? "Duelist" : type == PlayerClass.WARLOCK ? "Warlock" : type.name();
			return new com.l2journey.gameserver.model.actor.holders.player.ClassInfoHolder(type, name, null);
		});
		// The subclass level was not specified: cover newly added and leveled cases.
		for (int level : java.util.List.of(40, 75))
		{
			subclass(1, PlayerClass.WARLOCK, level);
			assertTrue(command(""));
			assertTrue(_pages.getLast().contains("Duelist (Lv. 85)"));
			assertTrue(_pages.getLast().contains("[Sub #1] Warlock"));
			assertTrue(_pages.getLast().contains("switch_subclass;1"));
			assertTrue(_pages.getLast().contains("confirm_remove;1"));
			assertTrue(_pages.getLast().contains("Your Classes and Subclasses"));
			final var pages = java.util.regex.Pattern.compile("Page 1/(\\d+)").matcher(_pages.getLast());
			assertTrue(pages.find());
			final int total = Integer.parseInt(pages.group(1));
			for (int page = 1; page <= total; page++)
			{
				assertTrue(command(";page;" + page));
				assertClientHtmlStructure(_pages.getLast());
				assertFalse(_pages.getLast().contains("add_subclass;14\""), "Do not offer the existing Warlock subclass again");
			}
		}
	}

}
