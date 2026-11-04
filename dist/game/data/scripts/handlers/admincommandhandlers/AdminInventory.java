package handlers.admincommandhandlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import com.l2journey.gameserver.data.xml.AdminData;
import com.l2journey.gameserver.handler.IAdminCommandHandler;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.network.serverpackets.NpcHtmlMessage;

/** Inventory pages and confirmed deletions are bound to an explicit character and item snapshot. */
public class AdminInventory implements IAdminCommandHandler
{
	private static final String SHOW = "admin_show_inventory";
	private static final String DELETE = "admin_inventory_delete";
	private static final int PAGE_SIZE = 6;
	private static final long VIEW_LIFETIME = 300000;
	private final Map<Player, View> _views = Collections.synchronizedMap(new WeakHashMap<>());
	private record View(int ownerId, int page, String token, long expires, Map<Integer, Long> items) { }

	@Override
	public String[] getAdminCommandList() { return new String[] { SHOW, DELETE }; }

	@Override
	public boolean useAdminCommand(String command, Player gm)
	{
		if (gm == null || command == null || !gm.isGM()) { return false; }
		final String[] args = command.trim().split("\\s+");
		if (!args[0].equals(SHOW) && !args[0].equals(DELETE)) { return false; }
		// Recheck here because admin commands can be queued or confirmed after permissions change.
		if (!AdminData.getInstance().hasAccess(args[0], gm.getAccessLevel()) ||
			(args[0].equals(DELETE) && !AdminData.getInstance().hasAccess("admin_delete_item", gm.getAccessLevel())))
		{
			gm.sendMessage("You don't have the access rights to use this command!");
			return false;
		}
		try
		{
			if (args[0].equals(DELETE)) { return delete(gm, args); }
			final Player owner;
			final int page;
			if (args.length == 1)
			{
				owner = gm.getTarget() instanceof Player target ? target : null;
				page = 0;
			}
			else if (args.length == 4 && args[1].equals("player"))
			{
				owner = World.getInstance().getPlayer(Integer.parseInt(args[2]));
				page = Integer.parseInt(args[3]);
			}
			else if (args.length == 3 && args[1].equals("name"))
			{
				owner = World.getInstance().getPlayer(args[2]);
				page = 0;
			}
			else if (args.length == 2 && !args[1].equals("name") && !args[1].equals("player"))
			{
				owner = World.getInstance().getPlayer(args[1]);
				page = 0;
			}
			else
			{
				_views.remove(gm);
				gm.sendMessage("Usage: //show_inventory [name <playerName>]");
				return false;
			}
			if (owner == null || !owner.isOnline() || page < 0)
			{
				_views.remove(gm);
				gm.sendMessage("Player not found or not online. Select a player or enter a name.");
				return false;
			}
			showItemsPage(gm, owner, page);
			return true;
		}
		catch (NumberFormatException e)
		{
			if (args[0].equals(SHOW)) { _views.remove(gm); }
			gm.sendMessage("Invalid inventory request.");
			return false;
		}
	}

	private boolean delete(Player gm, String[] args)
	{
		if (args.length != 6) { gm.sendMessage("Reopen the inventory and select the item again."); return false; }
		final int ownerId = Integer.parseInt(args[1]);
		final int objectId = Integer.parseInt(args[2]);
		final long count = Long.parseLong(args[3]);
		final int page = Integer.parseInt(args[4]);
		synchronized (_views)
		{
			final View view = _views.get(gm);
			if (view == null || view.expires() < System.currentTimeMillis() || view.ownerId() != ownerId ||
				view.page() != page || !view.token().equals(args[5]) || count <= 0 ||
				!Long.valueOf(count).equals(view.items().get(objectId)))
			{
				gm.sendMessage("This inventory selection is invalid or expired. Reopen the inventory.");
				return false;
			}
			// Consume the selection before mutation; duplicate confirmations cannot replay it.
			_views.remove(gm);
		}
		final Player owner = World.getInstance().getPlayer(ownerId);
		if (owner == null || !owner.isOnline()) { gm.sendMessage("Player is no longer online."); return false; }
		final Item item = owner.getInventory().getItemByObjectId(objectId);
		if (item == null) { gm.sendMessage("Item is no longer in this character's inventory."); return false; }
		synchronized (item)
		{
			if (item.getOwnerId() != ownerId || owner.getInventory().getItemByObjectId(objectId) != item || item.getCount() != count)
			{
				gm.sendMessage("The item or its quantity has changed. Reopen the inventory.");
				return false;
			}
			if (!owner.destroyItem(ItemProcessType.DESTROY, objectId, count, gm, true))
			{
				gm.sendMessage("Item could not be deleted.");
				return false;
			}
		}
		gm.sendMessage("Item deleted from " + owner.getName() + ".");
		showItemsPage(gm, owner, page);
		return true;
	}

	private void showItemsPage(Player gm, Player owner, int requestedPage)
	{
		final var items = new ArrayList<>(owner.getInventory().getItems());
		items.sort(Comparator.comparingInt(Item::getObjectId));
		final int pages = Math.max(1, (items.size() + PAGE_SIZE - 1) / PAGE_SIZE);
		final int page = Math.min(requestedPage, pages - 1);
		final String token = UUID.randomUUID().toString();
		final Map<Integer, Long> selection = new HashMap<>();
		final StringBuilder pageHtml = new StringBuilder();
		for (int i = 0; i < pages; i++)
		{
			if (i > 0 && i % 10 == 0) { pageHtml.append("</tr><tr>"); }
			pageHtml.append("<td><button value=\"").append(i + 1).append("\" action=\"bypass -h ").append(SHOW)
				.append(" player ").append(owner.getObjectId()).append(' ').append(i)
				.append("\" width=20 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		}
		final StringBuilder itemHtml = new StringBuilder();
		final int end = Math.min(items.size(), (page + 1) * PAGE_SIZE);
		for (int i = page * PAGE_SIZE; i < end; i++)
		{
			final Item item = items.get(i);
			final long count = item.getCount();
			if (item.getOwnerId() != owner.getObjectId() || count <= 0) { continue; }
			selection.put(item.getObjectId(), count);
			itemHtml.append("<tr><td><button action=\"bypass -h ").append(DELETE).append(' ').append(owner.getObjectId())
				.append(' ').append(item.getObjectId()).append(' ').append(count).append(' ').append(page).append(' ').append(token)
				.append("\" width=20 height=20 back=\"L2UI_ct1.RadarMap_DF_MinusBtn_Down\" fore=\"L2UI_ct1.RadarMap_DF_MinusBtn\"></td>")
				.append("<td><img src=\"").append(item.getTemplate().getIcon()).append("\" width=32 height=32></td>")
				.append("<td width=50>").append(escape(item.getName())).append(" x").append(count).append("</td></tr>");
		}
		_views.put(gm, new View(owner.getObjectId(), page, token, System.currentTimeMillis() + VIEW_LIFETIME, Map.copyOf(selection)));
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(gm, "data/html/admin/inventory.htm");
		html.replace("%PLAYER_NAME%", escape(owner.getName()));
		html.replace("%PAGES%", pageHtml.toString());
		html.replace("%ITEMS%", itemHtml.toString());
		gm.sendPacket(html);
	}

	private static String escape(String value)
	{
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}
}
