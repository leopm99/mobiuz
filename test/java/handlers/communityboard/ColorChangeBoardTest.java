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
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.Config;
import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.handler.CommunityBoardHandler;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.variables.PlayerVariables;
import com.l2journey.gameserver.model.zone.ZoneId;

class ColorChangeBoardTest
{
	private final List<MockedStatic<?>> _mocks = new ArrayList<>();
	private final Map<Field, Object> _config = new HashMap<>();
	private final List<String> _pages = new ArrayList<>();
	private final Map<String, PreparedStatement> _statements = new HashMap<>();
	private Player _player;
	private Connection _connection;
	private ColorChangeBoard _board;

	private <T> MockedStatic<T> staticMock(Class<T> type)
	{
		final MockedStatic<T> result = mockStatic(type);
		_mocks.add(result);
		return result;
	}

	@BeforeEach
	void setUp() throws Exception
	{
		for (String name : List.of("ENABLE_COMMUNITY_BOARD", "COMMUNITYBOARD_ENABLED", "COLOR_CHANGE_SERVICE_ENABLED", "COLOR_CHANGE_COLORS", "COLOR_CHANGE_COLOR_NAMES", "COLOR_CHANGE_ITEM_ID", "COLOR_CHANGE_ITEM_AMOUNT", "COMMUNITYBOARD_KARMA_DISABLED", "COMMUNITYBOARD_PEACE_ONLY", "COMMUNITYBOARD_COMBAT_DISABLED", "COMMUNITYBOARD_BLACKSMITH", "SUBCLASS_MASTER_BOARD"))
		{
			final Field field = Config.class.getField(name);
			_config.put(field, field.get(null));
		}
		Config.ENABLE_COMMUNITY_BOARD = true;
		Config.COMMUNITYBOARD_ENABLED = true;
		Config.COLOR_CHANGE_SERVICE_ENABLED = true;
		Config.COLOR_CHANGE_COLORS = new String[] { "FF0000", "0000FF" };
		Config.COLOR_CHANGE_COLOR_NAMES = new String[] { "Red", "Blue" };
		Config.COLOR_CHANGE_ITEM_ID = 57;
		Config.COLOR_CHANGE_ITEM_AMOUNT = 100;
		Config.COMMUNITYBOARD_KARMA_DISABLED = true;
		Config.COMMUNITYBOARD_PEACE_ONLY = false;
		Config.COMMUNITYBOARD_COMBAT_DISABLED = true;
		_player = mock(Player.class, RETURNS_DEEP_STUBS);
		when(_player.getLang()).thenReturn("br");
		when(_player.getObjectId()).thenReturn(42);
		when(_player.getAppearance().getNameColor()).thenReturn(0xFFFFFF);
		when(_player.getAppearance().getTitleColor()).thenReturn(0xFFFFFF);
		when(_player.getClient().getFloodProtectors().canPerformTransaction()).thenReturn(true);
		when(_player.destroyItemByItemId(ItemProcessType.BUY, 57, 100L, _player, true)).thenReturn(true);
		final ItemData items = mock(ItemData.class);
		final ItemTemplate adena = mock(ItemTemplate.class);
		when(adena.getName()).thenReturn("Adena");
		when(items.getTemplate(57)).thenReturn(adena);
		staticMock(ItemData.class).when(ItemData::getInstance).thenReturn(items);
		final MockedStatic<CommunityBoardHandler> community = staticMock(CommunityBoardHandler.class);
		community.when(CommunityBoardHandler::getInstance).thenReturn(mock(CommunityBoardHandler.class));
		community.when(() -> CommunityBoardHandler.separateAndSend(anyString(), any())).thenAnswer(i -> { _pages.add(i.getArgument(0)); return null; });
		_connection = mock(Connection.class);
		staticMock(DatabaseFactory.class).when(DatabaseFactory::getConnection).thenReturn(_connection);
		when(_connection.prepareStatement(anyString())).thenAnswer(i -> {
			final String sql = i.getArgument(0);
			final PreparedStatement statement = mock(PreparedStatement.class);
			when(statement.executeUpdate()).thenReturn(1);
			_statements.put(sql, statement);
			return statement;
		});
		_board = new ColorChangeBoard();
	}

	@AfterEach
	void tearDown() throws Exception
	{
		for (int i = _mocks.size() - 1; i >= 0; i--) { _mocks.get(i).close(); }
		for (var entry : _config.entrySet()) { entry.getKey().set(null, entry.getValue()); }
	}

	private boolean command(String action)
	{
		return _board.parseCommunityBoardCommand("_bbcolorchange" + action, _player);
	}

	private void notCharged() throws Exception
	{
		verify(_player, never()).destroyItemByItemId(any(), anyInt(), anyLong(), any(), anyBoolean());
		verify(_connection, never()).prepareStatement(anyString());
	}

	@Test
	void rgbConversionMatchesClientBgrOrder()
	{
		assertEquals(0x0000FF, ColorChangeBoard.rgbToBgr("FF0000"));
		assertEquals(0xFF0000, ColorChangeBoard.rgbToBgr("0000FF"));
		assertEquals(0x563412, ColorChangeBoard.rgbToBgr("123456"));
	}

	@Test
	void forgedTypesUnknownColorsAndExtraParametersAreRejected() throws Exception
	{
		for (String action : List.of(";other;FF0000", ";nick;FFFFFF", ";nick;FF0000;extra", ";title", "Extra;nick;FF0000"))
		{
			assertFalse(command(action));
		}
		notCharged();
	}

	@Test
	void disabledServiceCannotBePurchased() throws Exception
	{
		Config.COLOR_CHANGE_SERVICE_ENABLED = false;
		assertFalse(command(";nick;FF0000"));
		notCharged();
	}

	@Test
	void invalidPaletteAndNegativeFeesAreRejectedBeforePayment() throws Exception
	{
		Config.COLOR_CHANGE_COLORS = new String[] { "garbage", "0000FF" };
		assertFalse(command(";nick;garbage"));
		Config.COLOR_CHANGE_COLORS = new String[] { "FF0000" };
		assertFalse(command(";nick;FF0000"));
		Config.COLOR_CHANGE_COLOR_NAMES = new String[] { "Red" };
		Config.COLOR_CHANGE_ITEM_AMOUNT = -1;
		assertFalse(command(";nick;FF0000"));
		notCharged();
	}

	@Test
	void insufficientPaymentDoesNotWriteColor() throws Exception
	{
		when(_player.destroyItemByItemId(ItemProcessType.BUY, 57, 100L, _player, true)).thenReturn(false);
		command(";nick;FF0000");
		verify(_connection, never()).prepareStatement(anyString());
		verify(_player.getAppearance(), never()).setNameColor(anyInt());
	}

	@Test
	void nameColorUsesExistingVariableTableAndUpdatesMemoryAfterCommit() throws Exception
	{
		assertTrue(command(";nick;ff0000"));
		assertEquals(2, _statements.size());
		final PreparedStatement insert = _statements.get("INSERT INTO character_variables (charId,var,val) VALUES (?,?,?)");
		verify(insert).setInt(1, 42);
		verify(insert).setString(2, PlayerVariables.COMMUNITY_NAME_COLOR);
		verify(insert).setString(3, "255");
		final var order = inOrder(_connection, _player.getVariables(), _player.getAppearance());
		order.verify(_connection).commit();
		order.verify(_player.getVariables()).set(PlayerVariables.COMMUNITY_NAME_COLOR, 255);
		order.verify(_player.getAppearance()).setNameColor(255);
		verify(_player.getAppearance(), never()).setTitleColor(anyInt());
		verify(_player).broadcastUserInfo();
	}

	@Test
	void titleColorUpdatesOnlyTargetCharacterAndTitle() throws Exception
	{
		command(";title;0000FF");
		final PreparedStatement statement = _statements.get("UPDATE characters SET title_color=? WHERE charId=?");
		verify(statement).setInt(1, 0xFF0000);
		verify(statement).setInt(2, 42);
		verify(_connection).commit();
		verify(_player.getAppearance()).setTitleColor(0xFF0000);
		verify(_player.getAppearance(), never()).setNameColor(anyInt());
	}

	@Test
	void databaseFailureRollsBackRefundsAndPreservesAppearance() throws Exception
	{
		when(_connection.prepareStatement(anyString())).thenThrow(new SQLException("Test failure"));
		command(";title;FF0000");
		verify(_connection).rollback();
		verify(_connection, never()).commit();
		verify(_player).addItem(ItemProcessType.REFUND, 57, 100L, _player, true);
		verify(_player.getAppearance(), never()).setTitleColor(anyInt());
		verify(_player, never()).broadcastUserInfo();
	}

	@Test
	void freeChangesSkipItemDestruction() throws Exception
	{
		Config.COLOR_CHANGE_ITEM_AMOUNT = 0;
		command(";nick;FF0000");
		verify(_player, never()).destroyItemByItemId(any(), anyInt(), anyLong(), any(), anyBoolean());
		verify(_connection).commit();
		assertTrue(_pages.getLast().contains("gratuitas"));
	}

	@Test
	void sameColorIsNotChargedAgain() throws Exception
	{
		when(_player.getAppearance().getNameColor()).thenReturn(255);
		command(";nick;FF0000");
		notCharged();
	}

	@Test
	void combatTradingAndFloodProtectionBlockPurchases() throws Exception
	{
		when(_player.isInCombat()).thenReturn(true);
		assertFalse(command(";title;FF0000"));
		when(_player.isInCombat()).thenReturn(false);
		when(_player.isProcessingTransaction()).thenReturn(true);
		assertFalse(command(";title;FF0000"));
		when(_player.isProcessingTransaction()).thenReturn(false);
		when(_player.getClient().getFloodProtectors().canPerformTransaction()).thenReturn(false);
		assertFalse(command(";title;FF0000"));
		notCharged();
	}

	@Test
	void communityKarmaAndPeaceRestrictionsApply() throws Exception
	{
		when(_player.getKarma()).thenReturn(1);
		assertFalse(command(""));
		when(_player.getKarma()).thenReturn(0);
		Config.COMMUNITYBOARD_PEACE_ONLY = true;
		assertFalse(command(""));
		when(_player.isInsideZone(ZoneId.PEACE)).thenReturn(true);
		assertTrue(command(""));
		notCharged();
	}

	@Test
	void menuLocalizesEscapesLabelsAndReturnsToServices()
	{
		Config.COLOR_CHANGE_COLOR_NAMES = new String[] { "Red", "<custom>" };
		command("");
		assertTrue(_pages.getLast().contains("Vermelho"));
		assertTrue(_pages.getLast().contains("&lt;custom&gt;"));
		assertTrue(_pages.getLast().contains("merchant/blacksmithMain.html"));
		when(_player.getLang()).thenReturn("en");
		command("");
		assertTrue(_pages.getLast().contains("Cost per change: 100 Adena"));
	}

	@Test
	void configChangesAreUsedWithoutReloadingHandlerClass()
	{
		Config.COLOR_CHANGE_COLORS = new String[] { "123456" };
		Config.COLOR_CHANGE_COLOR_NAMES = new String[] { "Custom" };
		command("");
		assertTrue(_pages.getLast().contains("nick;123456"));
		assertFalse(_pages.getLast().contains("nick;FF0000"));
	}

	@Test
	void serviceLinkAppearsWhenItIsTheOnlyEnabledService() throws Exception
	{
		Config.COMMUNITYBOARD_BLACKSMITH = false;
		Config.SUBCLASS_MASTER_BOARD = false;
		final var rows = HomeBoard.class.getDeclaredMethod("buildServicesRows", String.class);
		rows.setAccessible(true);
		assertTrue(((String) rows.invoke(null, "br")).contains("bypass _bbcolorchange"));
		final var home = HomeBoard.class.getDeclaredMethod("buildHotFeaturesBlock", int.class, String.class);
		home.setAccessible(true);
		assertTrue(((String) home.invoke(null, 720, "br")).contains("merchant/blacksmithMain.html"));
		Config.COLOR_CHANGE_SERVICE_ENABLED = false;
		assertFalse(((String) rows.invoke(null, "br")).contains("bypass _bbcolorchange"));
		assertFalse(((String) home.invoke(null, 720, "br")).contains("merchant/blacksmithMain.html"));
	}
}
