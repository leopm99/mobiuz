package handlers.admincommandhandlers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import com.l2journey.gameserver.data.xml.AdminData;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.itemcontainer.PlayerInventory;
import com.l2journey.gameserver.network.serverpackets.NpcHtmlMessage;

class AdminInventoryTest
{
	private MockedStatic<AdminData> _adminStatic;
	private MockedStatic<World> _worldStatic;
	private MockedConstruction<NpcHtmlMessage> _html;
	private AdminData _permissions;
	private World _world;
	private Player _gm;
	private Player _owner;
	private PlayerInventory _inventory;
	private AdminInventory _handler;
	private final Map<String, String> _replacements = new HashMap<>();

	@BeforeEach
	void setup()
	{
		_permissions = mock(AdminData.class);
		_adminStatic = mockStatic(AdminData.class);
		_adminStatic.when(AdminData::getInstance).thenReturn(_permissions);
		when(_permissions.hasAccess(anyString(), any())).thenReturn(true);
		_world = mock(World.class);
		_worldStatic = mockStatic(World.class);
		_worldStatic.when(World::getInstance).thenReturn(_world);
		_html = mockConstruction(NpcHtmlMessage.class, (message, context) ->
			doAnswer(call -> { _replacements.put(call.getArgument(0), call.getArgument(1)); return null; })
				.when(message).replace(anyString(), anyString()));
		_gm = mock(Player.class);
		when(_gm.isGM()).thenReturn(true);
		when(_gm.getObjectId()).thenReturn(1);
		when(_gm.getName()).thenReturn("Admin");
		_owner = mock(Player.class);
		when(_owner.getObjectId()).thenReturn(42);
		when(_owner.getName()).thenReturn("Test1");
		when(_owner.isOnline()).thenReturn(true);
		when(_world.getPlayer("Test1")).thenReturn(_owner);
		when(_world.getPlayer(42)).thenReturn(_owner);
		_inventory = mock(PlayerInventory.class);
		when(_owner.getInventory()).thenReturn(_inventory);
		when(_inventory.getItems()).thenReturn(List.of());
		_handler = new AdminInventory();
	}

	@AfterEach
	void cleanup()
	{
		_html.close(); _worldStatic.close(); _adminStatic.close();
	}

	private Item item(int id, long count)
	{
		final Item item = mock(Item.class);
		when(item.getObjectId()).thenReturn(id);
		when(item.getOwnerId()).thenReturn(42);
		when(item.getCount()).thenReturn(count);
		when(item.getName()).thenReturn("Test item " + id);
		final ItemTemplate template = mock(ItemTemplate.class);
		when(item.getTemplate()).thenReturn(template);
		when(template.getIcon()).thenReturn("icon.etc_adena_i00");
		when(_inventory.getItemByObjectId(id)).thenReturn(item);
		return item;
	}

	private String selection()
	{
		final var matcher = Pattern.compile("bypass -h (admin_inventory_delete [^\"]+)").matcher(_replacements.get("%ITEMS%"));
		assertTrue(matcher.find());
		return matcher.group(1);
	}

	private Item open()
	{
		final Item item = item(1001, 20);
		when(_inventory.getItems()).thenReturn(List.of(item));
		assertTrue(_handler.useAdminCommand("admin_show_inventory name Test1", _gm));
		return item;
	}

	@Test
	void searchPassesNameAndShowsTheSearchedPlayersInventory() throws Exception
	{
		final String html = Files.readString(Path.of("dist/game/data/html/admin/itemcreation.htm"));
		assertTrue(html.contains("bypass admin_show_inventory name $qbox"));
		when(_gm.getTarget()).thenReturn(_gm);
		open();
		assertEquals("Test1", _replacements.get("%PLAYER_NAME%"));
		assertTrue(selection().startsWith("admin_inventory_delete 42 1001 20 0 "));
		verify(_gm, never()).getInventory();
	}

	@Test
	void paginationKeepsOwnerAndShowsExactlySixItemsWithoutOverlap()
	{
		final List<Item> items = new ArrayList<>();
		for (int i = 0; i < 7; i++) { items.add(item(1001 + i, 1)); }
		when(_inventory.getItems()).thenReturn(items);
		assertTrue(_handler.useAdminCommand("admin_show_inventory name Test1", _gm));
		assertEquals(6, _replacements.get("%ITEMS%").split("admin_inventory_delete", -1).length - 1);
		assertTrue(_replacements.get("%PAGES%").contains("admin_show_inventory player 42 1"));
		when(_gm.getTarget()).thenReturn(_gm);
		assertTrue(_handler.useAdminCommand("admin_show_inventory player 42 1", _gm));
		assertTrue(_replacements.get("%ITEMS%").contains("admin_inventory_delete 42 1007 1 1 "));
		assertFalse(_replacements.get("%ITEMS%").contains("1006"));
	}

	@Test
	void confirmationDeletesOnlyTheBoundObjectEvenAfterGmTargetChanges()
	{
		open();
		final String command = selection();
		when(_gm.getTarget()).thenReturn(_gm);
		when(_owner.destroyItem(ItemProcessType.DESTROY, 1001, 20L, _gm, true)).thenReturn(true);
		assertTrue(_handler.useAdminCommand(command, _gm));
		verify(_owner).destroyItem(ItemProcessType.DESTROY, 1001, 20L, _gm, true);
		verify(_gm, never()).getInventory();
		assertFalse(_handler.useAdminCommand(command, _gm));
		verify(_owner, times(1)).destroyItem(any(), anyInt(), anyLong(), any(), anyBoolean());
	}

	@Test
	void forgedOwnerObjectQuantityPageOrTokenCannotDelete()
	{
		open();
		final String command = selection();
		for (String altered : List.of(command.replace(" 42 ", " 99 "), command.replace(" 1001 ", " 1002 "),
			command.replace(" 20 ", " 21 "), command.replace(" 0 ", " 1 "), command + "forged"))
		{
			assertFalse(_handler.useAdminCommand(altered, _gm));
		}
		verify(_owner, never()).destroyItem(any(), anyInt(), anyLong(), any(), anyBoolean());
	}

	@Test
	void changedItemQuantityCannotDelete()
	{
		final Item item = open();
		final String command = selection();
		when(item.getCount()).thenReturn(19L);
		assertFalse(_handler.useAdminCommand(command, _gm));
		verify(_owner, never()).destroyItem(any(), anyInt(), anyLong(), any(), anyBoolean());
	}

	@Test
	void transferredItemCannotDeleteFromEitherCharacter()
	{
		final Item item = open();
		final String command = selection();
		when(item.getOwnerId()).thenReturn(99);
		assertFalse(_handler.useAdminCommand(command, _gm));
		verify(_owner, never()).destroyItem(any(), anyInt(), anyLong(), any(), anyBoolean());
		verify(_gm, never()).getInventory();
	}

	@Test
	void offlineOwnerNeverFallsBackToGm()
	{
		open();
		final String command = selection();
		when(_owner.isOnline()).thenReturn(false);
		assertFalse(_handler.useAdminCommand(command, _gm));
		verify(_owner, never()).destroyItem(any(), anyInt(), anyLong(), any(), anyBoolean());
		verify(_gm, never()).getInventory();
	}

	@Test
	void permissionRevokedDuringConfirmationPreventsDeletion()
	{
		open();
		final String command = selection();
		when(_permissions.hasAccess(eq("admin_delete_item"), any())).thenReturn(false);
		assertFalse(_handler.useAdminCommand(command, _gm));
		verify(_owner, never()).destroyItem(any(), anyInt(), anyLong(), any(), anyBoolean());
	}

	@Test
	void nonGmAndMalformedCommandsCannotMutateInventory()
	{
		when(_gm.isGM()).thenReturn(false);
		assertFalse(_handler.useAdminCommand("admin_show_inventory name Test1", _gm));
		when(_gm.isGM()).thenReturn(true);
		assertFalse(_handler.useAdminCommand("admin_inventory_delete", _gm));
		assertFalse(_handler.useAdminCommand("admin_show_inventory player nope 0", _gm));
		verify(_owner, never()).destroyItem(any(), anyInt(), anyLong(), any(), anyBoolean());
	}

	@Test
	void noDuplicateDeleteRegistrationAndConfirmationRemainsRequired() throws Exception
	{
		assertFalse(List.of(_handler.getAdminCommandList()).contains("admin_delete_item"));
		assertTrue(Files.readString(Path.of("dist/game/config/admin/admincommands.xml")).contains("command=\"inventory_delete\" accessLevel=\"100\" confirmDlg=\"true\""));
	}

    @Test
    void openingAnotherPageInvalidatesThePreviousConfirmation()
    {
        open();
        final String previous = selection();
        assertTrue(_handler.useAdminCommand("admin_show_inventory player 42 0", _gm));
        assertFalse(_handler.useAdminCommand(previous, _gm));
        verify(_owner, never()).destroyItem(any(), anyInt(), anyLong(), any(), anyBoolean());
    }

    @Test
    void anotherGmCannotReuseTheSelection()
    {
        open();
        final String command = selection();
        final Player other = mock(Player.class);
        when(other.isGM()).thenReturn(true);
        assertFalse(_handler.useAdminCommand(command, other));
        verify(_owner, never()).destroyItem(any(), anyInt(), anyLong(), any(), anyBoolean());
    }

    @Test
    void emptyOrFailedSearchInvalidatesTheSelectionWithoutFallingBackToTarget()
    {
        open();
        final String command = selection();
        when(_gm.getTarget()).thenReturn(_gm);
        assertFalse(_handler.useAdminCommand("admin_show_inventory name", _gm));
        assertFalse(_handler.useAdminCommand(command, _gm));
        assertFalse(_handler.useAdminCommand("admin_show_inventory name Missing", _gm));
        verify(_gm, never()).getInventory();
        verify(_owner, never()).destroyItem(any(), anyInt(), anyLong(), any(), anyBoolean());
    }
}
