package handlers.communityboard;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import com.l2journey.Config;
import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.cache.ImagesCache;
import com.l2journey.gameserver.cache.HtmCache;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.data.xml.NpcData;
import com.l2journey.gameserver.managers.GlobalVariablesManager;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.StatSet;
import com.l2journey.gameserver.model.variables.PlayerVariables;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.actor.enums.npc.DropType;
import com.l2journey.gameserver.model.actor.holders.npc.DropGroupHolder;
import com.l2journey.gameserver.model.actor.holders.npc.DropHolder;
import com.l2journey.gameserver.model.actor.templates.NpcTemplate;
import com.l2journey.gameserver.model.dropcalculator.DropInfoFunctions;
import com.l2journey.gameserver.model.dropcalculator.DropInfoHandler;
import com.l2journey.gameserver.model.dropcalculator.DropInfoHolder;
import com.l2journey.gameserver.network.serverpackets.ServerPacket;
import com.l2journey.gameserver.network.serverpackets.ShowBoard;
import com.l2journey.gameserver.util.DDSConverter;

class DropCalculatorCompatibilityTest
{
	@Test
	void itemDetailsWorkBeforeAnyMonsterSearchAndWithoutSavedNavigation() throws Exception
	{
		final boolean enabled = Config.ENABLE_DROP_CALCULATOR;
		final List<Integer> restricted = Config.RESTRICTED_MOB_DROPLIST_IDS;
		Config.ENABLE_DROP_CALCULATOR = true;
		Config.RESTRICTED_MOB_DROPLIST_IDS = List.of();
		try (var npcs = mockStatic(NpcData.class); var items = mockStatic(ItemData.class); var html = mockStatic(HtmCache.class); var images = mockStatic(ImagesCache.class); var functions = mockStatic(DropInfoFunctions.class))
		{
			final NpcData data = mock(NpcData.class);
			npcs.when(NpcData::getInstance).thenReturn(data);
			final NpcTemplate npc = mock(NpcTemplate.class);
			when(npc.getName()).thenReturn("Elite Dragon Knight");
			when(data.getTemplate(100)).thenReturn(npc);
			final ItemData itemData = mock(ItemData.class);
			items.when(ItemData::getInstance).thenReturn(itemData);
			final ItemTemplate crystal = mock(ItemTemplate.class);
			when(crystal.getId()).thenReturn(9549);
			when(crystal.getName()).thenReturn("Earth Crystal");
			when(itemData.getTemplate(9549)).thenReturn(crystal);
			final HtmCache cache = mock(HtmCache.class);
			html.when(HtmCache::getInstance).thenReturn(cache);
			when(cache.getHtm(any(Player.class), anyString())).thenAnswer(i -> Files.readString(Path.of("dist/game").resolve((String) i.getArgument(1))));
			images.when(ImagesCache::getInstance).thenReturn(mock(ImagesCache.class));
			functions.when(() -> DropInfoFunctions.getDropChance(any(), anyInt(), anyBoolean())).thenReturn("10");
			final StatSet state = new StatSet();
			final PlayerVariables variables = mock(PlayerVariables.class);
			when(variables.getInt(anyString())).thenAnswer(i -> state.getInt(i.getArgument(0)));
			when(variables.getInt(anyString(), anyInt())).thenAnswer(i -> state.getInt(i.getArgument(0), i.getArgument(1)));
			when(variables.getString(anyString(), anyString())).thenAnswer(i -> state.getString(i.getArgument(0), i.getArgument(1)));
			final Player player = mock(Player.class);
			when(player.getVariables()).thenReturn(variables);
			// State produced by the item route, without DCMonsterSort from a monster search.
			state.set("DCItemId", 9549);
			state.set("DCMonstersPage", 1);
			state.set("DCMonster2Sort", 0);
			final var board = new DropCalculatorBoard();
			assertTrue(board.parseCommunityBoardCommand("_bbssearchdropMonsterDetailsByItem_100", player));
			final var packets = ArgumentCaptor.forClass(ServerPacket.class);
			verify(player, times(3)).sendPacket(packets.capture());
			final Field content = ShowBoard.class.getDeclaredField("_content");
			content.setAccessible(true);
			assertTrue(packets.getAllValues().stream().anyMatch(packet -> {
				try { return ((String) content.get(packet)).contains("Earth Crystal"); }
				catch (IllegalAccessException e) { throw new AssertionError(e); }
			}));
			// A direct link also works when no page or sorting variables exist yet.
			state.getSet().clear();
			assertTrue(board.parseCommunityBoardCommand("_bbssearchdropMonsterDetailsByName_100", player));
			verify(player, never()).sendMessage(anyString());
		}
		finally
		{
			Config.ENABLE_DROP_CALCULATOR = enabled;
			Config.RESTRICTED_MOB_DROPLIST_IDS = restricted;
		}
	}

	@Test
	void indexCombinesGroupPercentagesAndRebuildsAfterReload()
	{
		final List<Integer> original = Config.RESTRICTED_MOB_DROPLIST_IDS;
		Config.RESTRICTED_MOB_DROPLIST_IDS = List.of();
		try (var mocked = mockStatic(NpcData.class))
		{
			final NpcData data = mock(NpcData.class);
			mocked.when(NpcData::getInstance).thenReturn(data);
			final AtomicLong revision = new AtomicLong(1);
			when(data.getRevision()).thenAnswer(i -> revision.get());
			final NpcTemplate npc = mock(NpcTemplate.class);
			when(npc.getId()).thenReturn(100);
			when(npc.getName()).thenReturn("Monster");
			when(npc.getLevel()).thenReturn((byte) 40);
			when(data.getTemplate(100)).thenReturn(npc);
			when(data.getTemplates(any())).thenAnswer(i -> {
				java.util.function.Predicate<NpcTemplate> predicate = i.getArgument(0);
				return predicate.test(npc) ? List.of(npc) : List.of();
			});
			final DropGroupHolder group = new DropGroupHolder(20);
			group.getDropList().add(new DropHolder(DropType.DROP, 57, 1, 2, 50));
			when(npc.getDropGroups()).thenReturn(List.of(group));
			when(npc.getSpoilList()).thenReturn(List.of(new DropHolder(DropType.SPOIL, 4037, 1, 1, 75)));
			final DropInfoHandler index = new DropInfoHandler();
			assertEquals(10d, index.getDrop(57).getFirst().getChance());
			assertEquals(75d, index.getDrop(4037).getFirst().getChance());
			assertTrue(index.getDrop(4037).getFirst().isSweep());
			assertEquals(1, index.getDropsCount(100, false));
			assertEquals(1, index.searchNpcs("MONSTER").size());
			index.getDrop(57).clear();
			index.getInfo().clear();
			assertEquals(1, index.getDrop(57).size());
			when(npc.getDropGroups()).thenReturn(List.of());
			revision.incrementAndGet();
			assertTrue(index.getDrop(57).isEmpty());
			Config.RESTRICTED_MOB_DROPLIST_IDS = List.of(100);
			assertTrue(index.getDrop(4037).isEmpty());
		}
		finally { Config.RESTRICTED_MOB_DROPLIST_IDS = original; }
	}

	@Test
	void sortingDoesNotMutateTheInput()
	{
		final var first = new DropInfoHolder(1, "A", (byte) 40, 1, 1, 10, false);
		final var second = new DropInfoHolder(2, "B", (byte) 50, 1, 1, 90, false);
		final var input = new ArrayList<>(List.of(first, second));
		assertEquals(List.of(second, first), DropInfoFunctions.sortMonsters2(input, 2));
		assertEquals(List.of(first, second), input);
	}

	@Test
	void invalidPagesAndMalformedCommandsAreRejectedBeforeRendering()
	{
		final boolean enabled = Config.ENABLE_DROP_CALCULATOR;
		Config.ENABLE_DROP_CALCULATOR = true;
		try
		{
			final Player player = mock(Player.class);
			final var board = new DropCalculatorBoard();
			for (String command : List.of("_bbssearchdropItemsByName_Adena_0", "_bbssearchdropMonstersByName_Wolf_-1", "_bbssearchdropItemsByName_Adena_2147483647", "_bbssearchdropItemsByName_Adena_1_99", "_bbssearchdropMonsterDetailsByItem", "_bbssearchNpcDropList_DEATH"))
			{
				assertFalse(board.parseCommunityBoardCommand(command, player), command);
			}
			verify(player, never()).sendPacket(any(ServerPacket.class));
		}
		finally { Config.ENABLE_DROP_CALCULATOR = enabled; }
	}

	@Test
	void boardPacketChunksStillReassembleTheExactHtml() throws Exception
	{
		for (int length : List.of(8179, 8180, 16360, 24539))
		{
			final Player player = mock(Player.class);
			final String html = "x".repeat(length);
			new DropCalculatorBoard().separateAndSend(html, player);
			final var packets = ArgumentCaptor.forClass(ServerPacket.class);
			verify(player, times(3)).sendPacket(packets.capture());
			final Field content = ShowBoard.class.getDeclaredField("_content");
			content.setAccessible(true);
			final StringBuilder assembled = new StringBuilder();
			for (ServerPacket packet : packets.getAllValues())
			{
				final String value = (String) content.get(packet);
				final String fragment = value.substring(value.indexOf('\b') + 1);
				if (!fragment.equals("null")) { assembled.append(fragment); }
			}
			assertEquals(html, assembled.toString());
		}
	}

	@Test
	void imageAllocationPreservesExistingMappingsAndNumericNpcIds(@TempDir Path temporary) throws Exception
	{
		final var originalRoot = Config.DATAPACK_ROOT;
		Config.DATAPACK_ROOT = temporary.toFile();
		final Path images = Files.createDirectories(temporary.resolve("data/images/custom"));
		for (String name : List.of("1000000000.png", "old.png", "legacy.png", "new.png", "noextension")) { Files.write(images.resolve(name), new byte[] { 1 }); }
		try (var globals = mockStatic(GlobalVariablesManager.class); var database = mockStatic(DatabaseFactory.class); var converter = mockStatic(DDSConverter.class))
		{
			final var variables = mock(GlobalVariablesManager.class);
			final Map<String, Object> values = new HashMap<>();
			values.put("ImageCache-old.png", 1000000001);
			globals.when(GlobalVariablesManager::getInstance).thenReturn(variables);
			when(variables.getSet()).thenReturn(values);
			when(variables.getInt(anyString(), anyInt())).thenAnswer(i -> ((Number) values.getOrDefault(i.getArgument(0), i.getArgument(1))).intValue());
			doAnswer(i -> { values.put(i.getArgument(0), i.getArgument(1)); return null; }).when(variables).set(anyString(), anyInt());
			doAnswer(i -> { values.put(i.getArgument(0), Integer.parseInt(i.getArgument(1))); return null; }).when(variables).set(anyString(), anyString());
			final Connection con = mock(Connection.class, RETURNS_DEEP_STUBS);
			database.when(DatabaseFactory::getConnection).thenReturn(con);
			final ResultSet tables = mock(ResultSet.class);
			when(con.getMetaData().getTables(any(), isNull(), eq("server_variables"), any())).thenReturn(tables);
			when(tables.next()).thenReturn(true, false);
			when(tables.getString("TABLE_NAME")).thenReturn("server_variables");
			final ResultSet rows = mock(ResultSet.class);
			when(con.prepareStatement("SELECT name, value FROM server_variables WHERE name LIKE 'ImageCache-%'").executeQuery()).thenReturn(rows);
			when(rows.next()).thenReturn(true, true, false);
			when(rows.getString("name")).thenReturn("ImageCache-old.png", "ImageCache-legacy.png");
			when(rows.getString("value")).thenReturn("1000000003");
			converter.when(() -> DDSConverter.convertToDDS(any(java.io.File.class))).thenAnswer(i -> ByteBuffer.wrap(new byte[] { 1 }));
			final var constructor = ImagesCache.class.getDeclaredConstructor();
			constructor.setAccessible(true);
			final ImagesCache cache = constructor.newInstance();
			assertEquals(1000000000, cache.getImageId("1000000000.png"));
			assertEquals(1000000001, cache.getImageId("old.png"));
			assertEquals(1000000002, cache.getImageId("new.png"));
			assertEquals(1000000003, cache.getImageId("legacy.png"));
			assertEquals(0, cache.getImageId("noextension"));
			verify(variables, never()).storeMe();
		}
		finally { Config.DATAPACK_ROOT = originalRoot; }
	}
}
