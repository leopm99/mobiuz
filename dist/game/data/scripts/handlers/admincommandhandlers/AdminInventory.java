package handlers.admincommandhandlers;

import java.util.Collection;
import java.util.StringTokenizer;

import com.l2journey.gameserver.handler.IAdminCommandHandler;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.network.serverpackets.NpcHtmlMessage;

public class AdminInventory implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_show_inventory",
		"admin_delete_item"
	};
	
	@Override
	public boolean useAdminCommand(String command, Player player)
	{
		if (!player.isGM())
		{
			return false;
		}
		
		if ((player.getTarget() == null))
		{
			player.sendMessage("Select a target");
			return false;
		}
		
		if (!player.getTarget().isPlayer())
		{
			player.sendMessage("Target need to be player");
			return false;
		}
		
		StringTokenizer st = new StringTokenizer(command);
		st.nextToken();
		if (command.startsWith(ADMIN_COMMANDS[0]))
		{
			if (st.hasMoreTokens())
			{
				int page = Integer.parseInt(st.nextToken());
				showItemsPage(player, page);
			}
			else
			{
				showItemsPage(player, 0);
			}
		}
		else if (command.contains(ADMIN_COMMANDS[1]))
		{
			if (!st.hasMoreTokens())
			{
				player.sendSysMessage("Usage: //delete_item <objectId>");
				return false;
			}
			final int objectId = Integer.parseInt(st.nextToken());
			final Item item = player.getInventory().getItemByObjectId(objectId);
			if (item == null)
			{
				player.sendSysMessage("Item not found.");
				return false;
			}
			player.destroyItem(ItemProcessType.DESTROY, objectId, item.getCount(), null, true);
			showItemsPage(player, 0);
		}
		
		return true;
	}
	
	private static void showItemsPage(Player player, int pageValue)
	{
		final Collection<Item> items = player.getInventory().getItems();
		final int maxItemsPerPage = 6;
		final int newLineBreak = 10;
		
		if (items == null)
		{
			return;
		}
		
		int maxPages = items.size() / maxItemsPerPage;
		if (items.size() > (maxItemsPerPage * maxPages))
		{
			maxPages++;
		}
		
		int page = pageValue;
		if (page > maxPages)
		{
			page = maxPages;
		}
		
		int itemsStart = maxItemsPerPage * page;
		int itemsEnd = items.size();
		if ((itemsEnd - itemsStart) > maxItemsPerPage)
		{
			itemsEnd = itemsStart + maxItemsPerPage;
		}
		
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(0);
		adminReply.setFile(player, "data/html/admin/inventory.htm");
		adminReply.replace("%PLAYER_NAME%", player.getName());
		
		StringBuilder sbPages = new StringBuilder();
		for (int x = 0; x < maxPages; x++)
		{
			int pagenr = x + 1;
			if ((x > 0) && ((x % newLineBreak) == 0))
			{
				sbPages.append("</tr><tr>");
			}
			
			sbPages.append("<td><button value=\"" + String.valueOf(pagenr) + "\" action=\"bypass -h admin_show_inventory " + String.valueOf(x) + "\" width=20 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		}
		
		adminReply.replace("%PAGES%", sbPages.toString());
		
		StringBuilder sbItems = new StringBuilder();
		int cont = 0;
		for (Item item : items)
		{
			cont++;
			if ((cont >= itemsStart) && (cont <= (itemsStart + maxItemsPerPage)))
			{
				sbItems.append("<tr>");
				sbItems.append("<td><button action=\"bypass -h admin_delete_item " + String.valueOf(item.getObjectId()) + "\" width=20 height=20 back=\"L2UI_ct1.RadarMap_DF_MinusBtn_Down\" fore=\"L2UI_ct1.RadarMap_DF_MinusBtn\">" + "</td>");
				sbItems.append("<td><img src=\"" + item.getTemplate().getIcon() + "\" width=32 height=32></td>");
				sbItems.append("<td width=50>" + item.getName() + "</td>");
				sbItems.append("</tr>");
				sbItems.append("<tr><td></td>");
				sbItems.append("<td></td>");
				sbItems.append("<td></td></tr>");
			}
		}
		
		adminReply.replace("%ITEMS%", sbItems.toString());
		
		player.sendPacket(adminReply);
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
