package handlers.voicedcommandhandlers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.Config;
import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.managers.InstanceManager;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.instancezone.InstanceWorld;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.olympiad.OlympiadManager;
import com.l2journey.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2journey.gameserver.network.serverpackets.ServerPacket;

class InstanceReuseClearCmdTest
{
	private final Map<Field, Object> _config = new HashMap<>();
	private final List<MockedStatic<?>> _mocks = new ArrayList<>();
	private final Map<Integer, Long> _times = new ConcurrentHashMap<>();
	private final List<String> _pages = new ArrayList<>();
	private InstanceManager _manager;
	private Player _player;
	private InstanceReuseClearCmd _command;
	private long _reuse;

	@BeforeEach
	void setUp() throws Exception
	{
		for (String name : List.of("INSTANCE_REUSE_CLEAR_ENABLED", "INSTANCE_REUSE_CLEAR_ITEM_ID", "INSTANCE_REUSE_CLEAR_ITEM_COUNT", "INSTANCE_REUSE_CLEAR_ALLOWED_IDS"))
		{
			final Field field = Config.class.getField(name);
			_config.put(field, field.get(null));
		}
		Config.INSTANCE_REUSE_CLEAR_ENABLED = true;
		Config.INSTANCE_REUSE_CLEAR_ITEM_ID = 57;
		Config.INSTANCE_REUSE_CLEAR_ITEM_COUNT = 1000;
		Config.INSTANCE_REUSE_CLEAR_ALLOWED_IDS = Set.of();
		var instances = mockStatic(InstanceManager.class);
		_mocks.add(instances);
		_manager = mock(InstanceManager.class);
		instances.when(InstanceManager::getInstance).thenReturn(_manager);
		when(_manager.getAllInstanceTimes(1)).thenReturn(_times);
		when(_manager.getInstanceIdName(anyInt())).thenReturn("UnknownInstance");
		when(_manager.getInstanceIdName(139)).thenReturn("Kamaloka");
		_reuse = System.currentTimeMillis() + 3600000;
		_times.put(139, _reuse);
		var olympiad = mockStatic(OlympiadManager.class);
		_mocks.add(olympiad);
		olympiad.when(OlympiadManager::getInstance).thenReturn(mock(OlympiadManager.class));
		var items = mockStatic(ItemData.class);
		_mocks.add(items);
		final ItemData data = mock(ItemData.class);
		items.when(ItemData::getInstance).thenReturn(data);
		final ItemTemplate currency = mock(ItemTemplate.class);
		when(currency.getName()).thenReturn("Adena");
		when(data.getTemplate(57)).thenReturn(currency);
		_player = mock(Player.class);
		when(_player.getObjectId()).thenReturn(1);
		when(_player.isOnline()).thenReturn(true);
		doAnswer(i -> { if (i.getArgument(0) instanceof NpcHtmlMessage packet) { _pages.add(packet.getHtml()); } return null; }).when(_player).sendPacket(any(ServerPacket.class));
		_command = new InstanceReuseClearCmd();
	}

	@AfterEach
	void tearDown() throws Exception
	{
		for (int i = _mocks.size() - 1; i >= 0; i--) { _mocks.get(i).close(); }
		for (var entry : _config.entrySet()) { entry.getKey().set(null, entry.getValue()); }
	}

	private String request()
	{
		assertTrue(_command.useVoicedCommand("clearinstance", _player, "139"));
		final var token = Pattern.compile("confirm ([a-f0-9-]+)").matcher(_pages.getLast());
		assertTrue(token.find());
		return token.group(1);
	}

	@Test
	void menuUsesPlayerBypassesAndOmitsExpiredOrRestrictedEntries()
	{
		_times.put(140, System.currentTimeMillis() - 1);
		assertTrue(_command.useVoicedCommand("instancereuse", _player, null));
		assertTrue(_pages.getLast().contains("voice .clearinstance 139"));
		assertFalse(_pages.getLast().contains("admin_"));
		Config.INSTANCE_REUSE_CLEAR_ALLOWED_IDS = Set.of(140);
		assertTrue(_command.useVoicedCommand("instancereuse", _player, null));
		assertTrue(_pages.getLast().contains("No active instance reuses"));
	}

	@Test
	void disabledOrUnsafeStateCannotCharge()
	{
		Config.INSTANCE_REUSE_CLEAR_ENABLED = false;
		assertFalse(_command.useVoicedCommand("clearinstance", _player, "139"));
		Config.INSTANCE_REUSE_CLEAR_ENABLED = true;
		when(_player.getInstanceId()).thenReturn(5);
		assertFalse(_command.useVoicedCommand("clearinstance", _player, "139"));
		when(_player.getInstanceId()).thenReturn(0);
		when(_manager.getPlayerWorld(_player)).thenReturn(mock(InstanceWorld.class));
		assertFalse(_command.useVoicedCommand("clearinstance", _player, "139"));
		verify(_player, never()).destroyItemByItemId(any(), anyInt(), anyLong(), any(), anyBoolean());
	}

	@Test
	void invalidOrExpiredReuseCannotBePurchased()
	{
		for (String args : List.of("abc", "139 extra", "-1", "140", "confirm forged", "page 0"))
		{
			assertFalse(_command.useVoicedCommand("clearinstance", _player, args), args);
		}
		_times.put(139, System.currentTimeMillis() - 1);
		assertFalse(_command.useVoicedCommand("clearinstance", _player, "139"));
		verify(_player, never()).destroyItemByItemId(any(), anyInt(), anyLong(), any(), anyBoolean());
	}

	@Test
	void confirmationIsRequiredAndCanOnlyBeUsedOnce()
	{
		final String token = request();
		verify(_player, never()).destroyItemByItemId(any(), anyInt(), anyLong(), any(), anyBoolean());
		when(_player.destroyItemByItemId(ItemProcessType.FEE, 57, 1000, _player, true)).thenReturn(true);
		when(_manager.deleteInstanceTimeIfMatches(1, 139, _reuse)).thenAnswer(i -> { _times.remove(139); return true; });
		assertTrue(_command.useVoicedCommand("clearinstance", _player, "confirm " + token));
		assertFalse(_command.useVoicedCommand("clearinstance", _player, "confirm " + token));
		final var order = inOrder(_player, _manager);
		order.verify(_player).destroyItemByItemId(ItemProcessType.FEE, 57, 1000, _player, true);
		order.verify(_manager).deleteInstanceTimeIfMatches(1, 139, _reuse);
		verify(_player, times(1)).destroyItemByItemId(ItemProcessType.FEE, 57, 1000, _player, true);
	}

	@Test
	void insufficientPaymentDoesNotDeleteReuse()
	{
		assertFalse(_command.useVoicedCommand("clearinstance", _player, "confirm " + request()));
		verify(_manager, never()).deleteInstanceTimeIfMatches(anyInt(), anyInt(), anyLong());
	}

	@Test
	void failedDeletionRefundsPayment()
	{
		final String token = request();
		when(_player.destroyItemByItemId(ItemProcessType.FEE, 57, 1000, _player, true)).thenReturn(true);
		when(_player.addItem(ItemProcessType.REFUND, 57, 1000, _player, true)).thenReturn(mock(Item.class));
		assertFalse(_command.useVoicedCommand("clearinstance", _player, "confirm " + token));
		verify(_player).addItem(ItemProcessType.REFUND, 57, 1000, _player, true);
		assertTrue(_times.containsKey(139));
	}

	@Test
	void changedReuseOrPriceInvalidatesConfirmationBeforeCharging()
	{
		String token = request();
		_times.put(139, _reuse + 1000);
		assertFalse(_command.useVoicedCommand("clearinstance", _player, "confirm " + token));
		token = request();
		Config.INSTANCE_REUSE_CLEAR_ITEM_COUNT++;
		assertFalse(_command.useVoicedCommand("clearinstance", _player, "confirm " + token));
		verify(_player, never()).destroyItemByItemId(any(), anyInt(), anyLong(), any(), anyBoolean());
	}

	@Test
	void managerChangesMemoryOnlyWhenExactDatabaseRowWasDeleted() throws Exception
	{
		final InstanceManager manager = mock(InstanceManager.class, CALLS_REAL_METHODS);
		doReturn(_times).when(manager).getAllInstanceTimes(1);
		try (var database = mockStatic(DatabaseFactory.class))
		{
			final Connection con = mock(Connection.class);
			final PreparedStatement statement = mock(PreparedStatement.class);
			database.when(DatabaseFactory::getConnection).thenReturn(con);
			when(con.prepareStatement(anyString())).thenReturn(statement);
			when(statement.executeUpdate()).thenReturn(0, 1);
			assertFalse(manager.deleteInstanceTimeIfMatches(1, 139, _reuse));
			assertEquals(_reuse, _times.get(139));
			assertTrue(manager.deleteInstanceTimeIfMatches(1, 139, _reuse));
			assertFalse(_times.containsKey(139));
			verify(statement, times(2)).setLong(3, _reuse);
			verify(con, times(2)).prepareStatement(contains("AND time=? AND time>?"));
		}
	}

	@Test
	void managerSqlFailureKeepsReuse() throws Exception
	{
		final InstanceManager manager = mock(InstanceManager.class, CALLS_REAL_METHODS);
		doReturn(_times).when(manager).getAllInstanceTimes(1);
		try (var database = mockStatic(DatabaseFactory.class))
		{
			final Connection con = mock(Connection.class);
			database.when(DatabaseFactory::getConnection).thenReturn(con);
			when(con.prepareStatement(anyString())).thenThrow(new SQLException("Test database failure"));
			assertFalse(manager.deleteInstanceTimeIfMatches(1, 139, _reuse));
			assertEquals(_reuse, _times.get(139));
		}
	}
}
