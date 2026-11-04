package handlers.communityboard;

import com.l2journey.Config;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.handler.CommunityBoardHandler;
import com.l2journey.gameserver.handler.IParseBoardHandler;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.network.serverpackets.MagicSkillUse;

/**
 * @author Debug
 */

public class NoblessBoard implements IParseBoardHandler
{
	private static final String[] COMMANDS =
	{
		"_bbspurchase_noblesse",
		"_bbspurchase_noblesse_yes"
	};
	
	final int ITEM_ID = Config.NOBLESSE_ITEM_ID;
	final long ITEM_COUNT = Config.NOBLESSE_ITEM_COUNT;
	final int TIARA_ID = Config.NOBLESSE_TIARA_ID;
	
	@Override
	public String[] getCommunityBoardCommands()
	{
		return COMMANDS;
	}
	
	@Override
	public boolean parseCommunityBoardCommand(String command, Player player)
	{
		if (!Config.NOBLESSE_SERVICE)
		{
			CommunityBoardHandler.separateAndSend("<html><body><center><br><font color=FF0000>The nobility service is disabled.</font><br></center></body></html>", player);
			return false;
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("<html><body><center>");
		
		if (command.equals("_bbspurchase_noblesse"))
		{
			sb.append("<br><font color=LEVEL>Do you want to become a noble?</font><br><br>");
			sb.append("Cost: <font color=LEVEL>").append(String.format("%,d", ITEM_COUNT)).append("</font> Adena.<br><br>");
			sb.append("<table><tr>");
			sb.append("<td><button value=\"Yes\" action=\"bypass _bbspurchase_noblesse_yes\" width=80 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
			sb.append("<td><button value=\"No\" action=\"bypass _bbshome\" width=80 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
			sb.append("</tr></table>");
		}
		else if (command.equals("_bbspurchase_noblesse_yes"))
		{
			sb.append("<br><font color=LEVEL>Nobility Purchase</font><br><br>");
			
			if (player.isNoble())
			{
				sb.append("You are already a noble.<br>");
			}
			else
			{
				long playerCount = player.getInventory().getInventoryItemCount(ITEM_ID, -1);
				if (playerCount >= ITEM_COUNT)
				{
					Item destroyed = player.getInventory().destroyItemByItemId(ItemProcessType.FEE, ITEM_ID, ITEM_COUNT, player, null);
					
					if (destroyed != null)
					{
						player.setNoble(true);
						player.broadcastPacket(new MagicSkillUse(player, 5103, 1, 0, 0));
						player.addItem(ItemProcessType.REWARD, TIARA_ID, 1, player, true);
						player.broadcastUserInfo();
						
						ItemTemplate item = ItemData.getInstance().getTemplate(ITEM_ID);
						String itemName = (item != null) ? item.getName() : ("ID: " + ITEM_ID);
						player.sendMessage("You have become a noble for " + String.format("%,d", ITEM_COUNT) + " " + itemName + ".");
						
						sb.append("<font color=LEVEL>Congratulations! You have become a noble!</font><br>");
					}
					else
					{
						sb.append("<font color=FF0000>Error deducting funds.</font><br>");
						player.sendMessage("Error deducting funds.");
					}
				}
				else
				{
					sb.append("You don’t have enough funds.<br>You need: <font color=LEVEL>").append(String.format("%,d", ITEM_COUNT)).append("</font> Adena.<br>");
					player.sendMessage("Not enough Adena. Required: " + String.format("%,d", ITEM_COUNT) + " Adena.");
				}
			}
			sb.append("<br><button value=\"Back\" action=\"bypass _bbshome\" width=80 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		}
		
		sb.append("</center></body></html>");
		CommunityBoardHandler.separateAndSend(sb.toString(), player);
		return true;
	}
}
