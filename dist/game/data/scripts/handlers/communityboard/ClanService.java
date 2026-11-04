package handlers.communityboard;

import com.l2journey.gameserver.handler.CommunityBoardHandler;
import com.l2journey.gameserver.handler.IParseBoardHandler;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.clan.Clan;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;

/**
 * @author Debug
 */

public class ClanService implements IParseBoardHandler
{
	private static final String[] COMMANDS =
	{
		"_bbsclan",
		"_bbsclan;levelup",
		"_bbsclan;reputation"
	};
	
	@Override
	public String[] getCommunityBoardCommands()
	{
		return COMMANDS;
	}
	
	@Override
	public boolean parseCommunityBoardCommand(String command, Player player)
	{
		if (!player.isClanLeader())
		{
			player.sendMessage("Only the clan leader can use this service.");
			return false;
		}
		
		final Clan clan = player.getClan();
		
		if (command.equals("_bbsclan"))
		{
			StringBuilder html = new StringBuilder();
			html.append("<html><body><center>");
			html.append("<font color=LEVEL>Clan Services</font><br><br>");
			html.append("Clan level: <font color=\"LEVEL\">").append(clan.getLevel()).append("</font><br>");
			html.append("Clan reputation: <font color=\"LEVEL\">").append(clan.getReputationScore()).append("</font><br><br>");
			html.append("<button value=\"Level Up (10,000,000 Adena)\" action=\"bypass -h _bbsclan;levelup\" width=250 height=32 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"><br>");
			html.append("<button value=\"Add 5000 Reputation (5,000,000 Adena)\" action=\"bypass -h _bbsclan;reputation\" width=250 height=32 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"><br>");
			html.append("<br><button value=\"Back\" action=\"bypass -h _bbshome\" width=200 height=30 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" />");
			html.append("</center></body></html>");
			
			CommunityBoardHandler.separateAndSend(html.toString(), player);
			return true;
		}
		else if (command.equals("_bbsclan;levelup"))
		{
			int currentLevel = clan.getLevel();
			int maxLevel = 11;
			
			if (currentLevel >= maxLevel)
			{
				player.sendMessage("Your clan has already reached the maximum level.");
				return true;
			}
			
			long price = 10_000_000; // 10,000,000 Adena
			if (player.getAdena() < price)
			{
				player.sendMessage("You do not have enough Adena.");
				return true;
			}
			
			player.reduceAdena(ItemProcessType.CONSUME, price, player, true);
			clan.changeLevel(currentLevel + 1);
			clan.updateClanInDB();
			
			player.sendMessage("Clan level increased to " + clan.getLevel());
			return true;
		}
		else if (command.equals("_bbsclan;reputation"))
		{
			long price = 5_000_000; // 5,000,000 Adena
			int reputation = 5000;
			
			if (player.getAdena() < price)
			{
				player.sendMessage("You do not have enough Adena.");
				return true;
			}
			
			player.reduceAdena(ItemProcessType.CONSUME, price, player, true);
			clan.addReputationScore(reputation);
			
			player.sendMessage("Clan reputation increased by " + reputation);
			return true;
		}
		
		return false;
	}
}
