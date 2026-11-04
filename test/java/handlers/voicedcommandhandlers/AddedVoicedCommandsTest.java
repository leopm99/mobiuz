package handlers.voicedcommandhandlers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.l2journey.Config;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.data.SpawnTable;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.data.xml.NpcData;
import com.l2journey.gameserver.managers.CastleManager;
import com.l2journey.gameserver.managers.GrandBossManager;
import com.l2journey.gameserver.managers.SiegeManager;
import com.l2journey.gameserver.managers.ZoneManager;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.Spawn;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.templates.NpcTemplate;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.quest.QuestState;
import com.l2journey.gameserver.model.stats.Formulas;
import com.l2journey.gameserver.model.zone.ZoneId;
import com.l2journey.gameserver.network.serverpackets.ServerPacket;
import com.l2journey.gameserver.util.Broadcast;

class AddedVoicedCommandsTest
{
	private final Map<Field, Object> _config = new HashMap<>();

	@BeforeEach
	void saveConfig() throws Exception
	{
		for (String name : List.of("COMMAND_RES", "PRICE_RESS", "ITEM_ID_RESS", "ALLOW_GOTOCL_COMMAND", "ALLOW_GOTOPL_COMMAND", "TOCL_ITEM_CONSUME_ID", "TOCL_ITEM_CONSUME_COUNT", "TOMOB_COMMAND_ENABLED", "TOMOB_COMMAND", "TOMOB_FREE_TP", "TOMOB_TP_ITEM_ID", "TOMOB_TP_ITEM_COUNT", "TOMOB_PEACE_ZONE_ONLY", "TOMOB_TP_TIME"))
		{
			final Field field = Config.class.getField(name);
			_config.put(field, field.get(null));
		}
	}

	@AfterEach
	void restoreConfig() throws Exception
	{
		for (var entry : _config.entrySet()) { entry.getKey().set(null, entry.getValue()); }
	}

	@Test
	void resurrectionRegistersAndCannotReviveWithoutSuccessfulPayment()
	{
		Config.COMMAND_RES = true;
		Config.ITEM_ID_RESS = 57;
		Config.PRICE_RESS = 10;
		final Player player = mock(Player.class);
		when(player.isDead()).thenReturn(true);
		final var handler = new ResVCmd();
		assertArrayEquals(new String[] { "res" }, handler.getVoicedCommandList());
		assertFalse(handler.useVoicedCommand("res", player, null));
		verify(player, never()).doRevive(anyDouble());
		try (var formulas = mockStatic(Formulas.class))
		{
			formulas.when(() -> Formulas.calculateSkillResurrectRestorePercent(100, player)).thenReturn(100d);
			when(player.destroyItemByItemId(ItemProcessType.FEE, 57, 10, player, true)).thenReturn(true);
			assertTrue(handler.useVoicedCommand("res", player, null));
			final var order = inOrder(player);
			order.verify(player, times(2)).destroyItemByItemId(ItemProcessType.FEE, 57, 10, player, true);
			order.verify(player).doRevive(100d);
		}
	}

	@Test
	void partyTeleportWorksWithClanCommandDisabledAndChargesBeforeTeleporting()
	{
		Config.ALLOW_GOTOCL_COMMAND = false;
		Config.ALLOW_GOTOPL_COMMAND = true;
		Config.TOCL_ITEM_CONSUME_ID = 57;
		Config.TOCL_ITEM_CONSUME_COUNT = 10;
		try (var world = mockStatic(World.class); var bosses = mockStatic(GrandBossManager.class); var items = mockStatic(ItemData.class))
		{
			final World instance = mock(World.class);
			world.when(World::getInstance).thenReturn(instance);
			bosses.when(GrandBossManager::getInstance).thenReturn(mock(GrandBossManager.class));
			items.when(ItemData::getInstance).thenReturn(mock(ItemData.class));
			final Player player = mock(Player.class, RETURNS_DEEP_STUBS);
			when(player.getClan()).thenReturn(null);
			when(player.getParty().getLeaderObjectId()).thenReturn(123);
			final Player leader = mock(Player.class);
			when(leader.isOnline()).thenReturn(true);
			when(leader.getLocation()).thenReturn(new Location(1, 2, 3));
			when(instance.getPlayer(123)).thenReturn(leader);
			final var handler = new GoToClanLeader();
			assertFalse(handler.useVoicedCommand("gotopl", player, null));
			verify(player, never()).teleToLocation(any(Location.class));
			when(player.destroyItemByItemId(ItemProcessType.FEE, 57, 10, player, true)).thenReturn(true);
			assertTrue(handler.useVoicedCommand("gotopl", player, null));
			final var order = inOrder(player);
			order.verify(player, times(2)).destroyItemByItemId(ItemProcessType.FEE, 57, 10, player, true);
			order.verify(player).teleToLocation(leader.getLocation());
		}
	}

	@Test
	void mobTeleportRequiresPaymentAndPeaceZoneAndCancelsWhenStateChanges()
	{
		Config.TOMOB_COMMAND_ENABLED = true;
		Config.TOMOB_COMMAND = "mob";
		Config.TOMOB_FREE_TP = false;
		Config.TOMOB_TP_ITEM_ID = 57;
		Config.TOMOB_TP_ITEM_COUNT = 10;
		Config.TOMOB_TP_TIME = 10;
		Config.TOMOB_PEACE_ZONE_ONLY = true;
		try (var npcs = mockStatic(NpcData.class); var spawns = mockStatic(SpawnTable.class); var bosses = mockStatic(GrandBossManager.class); var sieges = mockStatic(SiegeManager.class); var zones = mockStatic(ZoneManager.class); var items = mockStatic(ItemData.class); var threads = mockStatic(ThreadPool.class); var broadcasts = mockStatic(Broadcast.class))
		{
			final NpcData data = mock(NpcData.class);
			npcs.when(NpcData::getInstance).thenReturn(data);
			final NpcTemplate npc = mock(NpcTemplate.class);
			when(npc.getId()).thenReturn(100);
			when(npc.getName()).thenReturn("Wolf");
			when(data.getTemplateByName("Wolf")).thenReturn(npc);
			final SpawnTable table = mock(SpawnTable.class);
			spawns.when(SpawnTable::getInstance).thenReturn(table);
			when(table.getSpawns(100)).thenReturn(Set.of(mock(Spawn.class)));
			bosses.when(GrandBossManager::getInstance).thenReturn(mock(GrandBossManager.class));
			sieges.when(SiegeManager::getInstance).thenReturn(mock(SiegeManager.class));
			zones.when(ZoneManager::getInstance).thenReturn(mock(ZoneManager.class));
			final ItemData itemData = mock(ItemData.class);
			items.when(ItemData::getInstance).thenReturn(itemData);
			final ItemTemplate currency = mock(ItemTemplate.class);
			when(currency.getName()).thenReturn("Adena");
			when(itemData.getTemplate(57)).thenReturn(currency);
			final Player player = mock(Player.class, RETURNS_DEEP_STUBS);
			when(player.isOnline()).thenReturn(true);
			final var handler = new ToMob();
			assertFalse(handler.useVoicedCommand("mob", player, "Wolf"));
			verify(player, never()).destroyItemByItemId(any(), anyInt(), anyLong(), any(), anyBoolean());
			when(player.isInsideZone(ZoneId.PEACE)).thenReturn(true);
			assertFalse(handler.useVoicedCommand("mob", player, "Wolf"));
			threads.verifyNoInteractions();
			when(player.destroyItemByItemId(ItemProcessType.FEE, 57, 10, player, true)).thenReturn(true);
			final AtomicReference<Runnable> pending = new AtomicReference<>();
			threads.when(() -> ThreadPool.schedule(any(Runnable.class), eq(10000L))).thenAnswer(i -> { pending.set(i.getArgument(0)); return mock(ScheduledFuture.class); });
			assertTrue(handler.useVoicedCommand("mob", player, "Wolf"));
			assertFalse(handler.useVoicedCommand("mob", player, "Wolf"));
			when(player.isInCombat()).thenReturn(true);
			pending.get().run();
			verify(player, never()).teleToLocation(any(Location.class));
			verify(player).enableAllSkills();
			verify(player).addItem(ItemProcessType.REFUND, 57, 10, player, true);
		}
	}

	@Test
	void quizCommandListingDoesNotStartAnEvent()
	{
		try (var threads = mockStatic(ThreadPool.class))
		{
			assertArrayEquals(new String[] { "quiz", "1", "2", "3" }, new Quiz().getVoicedCommandList());
			threads.verifyNoInteractions();
		}
	}

	@Test
	void sevenBossesDistinguishesUnclaimedRewardFromCompletedQuest() throws Exception
	{
		final var build = SevenRB.class.getDeclaredMethod("buildHtml", QuestState.class);
		build.setAccessible(true);
		final QuestState quest = mock(QuestState.class);
		when(quest.isCond(2)).thenReturn(true);
		assertTrue(((String) build.invoke(null, quest)).contains("claim your reward"));
		when(quest.isCond(2)).thenReturn(false);
		when(quest.isCompleted()).thenReturn(true);
		assertTrue(((String) build.invoke(null, quest)).contains("quest is completed"));
	}

	@Test
	void unknownCityTeleportDoesNotUseNullDestination()
	{
		final Player player = mock(Player.class);
		assertFalse(new TeleportsVCmd().useVoicedCommand("unknown", player, null));
		verify(player, never()).teleToLocation(any(Location.class), anyBoolean());
	}
}
