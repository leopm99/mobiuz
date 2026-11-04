package handlers.communityboard;

import com.l2journey.Config;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.handler.CommunityBoardHandler;
import com.l2journey.gameserver.handler.IParseBoardHandler;
import com.l2journey.gameserver.model.StatSet;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.olympiad.Hero;
import com.l2journey.gameserver.network.serverpackets.MagicSkillUse;

/**
 * @author Debug
 */

public class HeroBoard implements IParseBoardHandler
{
	private static final String[] COMMANDS =
	{
		"_bbspurchase_hero",
		"_bbspurchase_hero_yes"
	};
	
	private static final int ITEM_ID = Config.HERO_SERVICE_ITEM_ID;
	private static final long ITEM_COUNT = Config.HERO_SERVICE_ITEM_COUNT;
	
	@Override
	public String[] getCommunityBoardCommands()
	{
		return COMMANDS;
	}
	
	@Override
	public boolean parseCommunityBoardCommand(String command, Player player)
	{
		if (!Config.HERO_SERVICE_ENABLED)
		{
			CommunityBoardHandler.separateAndSend("<html><body><center><br><font color=\"FF0000\">The heroism purchase service is currently disabled.</font></center></body></html>", player);
			return true;
		}
		
		StringBuilder sb = new StringBuilder("<html><body><center>");
		
		if (command.equals("_bbspurchase_hero"))
		{
			sb.append("<br><font color=LEVEL>Do you want to become a Hero?</font><br><br>");
			sb.append("Cost: <font color=LEVEL>").append(String.format("%,d", ITEM_COUNT)).append("</font> Adena.<br><br>");
			sb.append("<table><tr>");
			sb.append("<td><button value=\"Yes\" action=\"bypass _bbspurchase_hero_yes\" width=80 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
			sb.append("<td><button value=\"No\" action=\"bypass _bbshome\" width=80 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
			sb.append("</tr></table>");
		}
		else if (command.equals("_bbspurchase_hero_yes"))
		{
			sb.append("<br><font color=LEVEL>Heroism Purchase</font><br><br>");
			
			if (player.isHero())
			{
				sb.append("You are already a Hero.<br>");
			}
			else
			{
				long playerCount = player.getInventory().getInventoryItemCount(ITEM_ID, -1);
				if (playerCount >= ITEM_COUNT)
				{
					Item destroyed = player.getInventory().destroyItemByItemId(ItemProcessType.FEE, ITEM_ID, ITEM_COUNT, player, null);
					
					if (destroyed != null)
					{
						StatSet newHero = new StatSet();
						newHero.set("char_id", player.getObjectId());
						newHero.set("char_name", player.getName());
						newHero.set("class_id", player.getBaseClass());
						newHero.set("count", 1);
						newHero.set("played", 1);
						newHero.set("claimed", false);
						
						Hero.getInstance().getHeroes().put(player.getObjectId(), newHero);
						Hero.getInstance().updateHeroes(false);
						Hero.getInstance().claimHero(player);
						
						player.broadcastPacket(new MagicSkillUse(player, 5103, 1, 0, 0));
						
						ItemTemplate item = ItemData.getInstance().getTemplate(ITEM_ID);
						String itemName = (item != null) ? item.getName() : ("ID: " + ITEM_ID);
						player.sendMessage("You have become a Hero for " + String.format("%,d", ITEM_COUNT) + " " + itemName + ".");
						sb.append("<font color=LEVEL>Congratulations! You are now a Hero!</font><br>");
					}
					else
					{
						sb.append("<font color=FF0000>Error deducting funds.</font><br>");
						player.sendMessage("Error processing the payment.");
					}
				}
				else
				{
					sb.append("You don’t have enough Adena.<br>You need: <font color=LEVEL>").append(String.format("%,d", ITEM_COUNT)).append("</font>.<br>");
					player.sendMessage("Not enough Adena. You need " + String.format("%,d", ITEM_COUNT) + ".");
				}
			}
			
			sb.append("<br><button value=\"Back\" action=\"bypass _bbshome\" width=80 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		}
		
		sb.append("</center></body></html>");
		CommunityBoardHandler.separateAndSend(sb.toString(), player);
		return true;
	}
}
