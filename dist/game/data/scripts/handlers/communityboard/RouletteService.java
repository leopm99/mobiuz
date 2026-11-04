package handlers.communityboard;

import java.util.List;
import java.util.Random;

import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.handler.CommunityBoardHandler;
import com.l2journey.gameserver.handler.IParseBoardHandler;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;

/**
 * Roulette Service with pseudo-animation
 * @author Debug
 */
public class RouletteService implements IParseBoardHandler
{
	private static final String[] COMMANDS =
	{
		"_bbsroulette",
		"_bbsroulette;spin"
	};
	
	// List of awards (itemId, count, icon)
	private static final List<Reward> REWARDS = List.of(new Reward(57, 1_000_000, "icon.etc_adena_i00"), new Reward(4037, 10, "icon.etc_scroll_of_enchant_weapon_i05"), new Reward(3958, 1, "icon.armor_t74_u_i00"), new Reward(6622, 1, "icon.etc_charm_of_courage_i00"), new Reward(8752, 1, "icon.weapon_sword_of_miracle_i00"), new Reward(2395, 1, "icon.armor_t74_u_i00"));
	
	private static final Random RANDOM = new Random();
	
	@Override
	public String[] getCommunityBoardCommands()
	{
		return COMMANDS;
	}
	
	@Override
	public boolean parseCommunityBoardCommand(String command, Player player)
	{
		if (command.equals("_bbsroulette"))
		{
			showMainPage(player, null);
			return true;
		}
		else if (command.equals("_bbsroulette;spin"))
		{
			// Cost per scroll
			long price = 1_000_000; // 1,000,000 Adena
			if (player.getAdena() < price)
			{
				player.sendMessage("Not enough adena to spin the roulette wheel.");
				return true;
			}
			
			player.reduceAdena(ItemProcessType.CONSUME, price, player, true);
			
			// Random prize
			Reward reward = REWARDS.get(RANDOM.nextInt(REWARDS.size()));
			
			// Pseudo-animation: showing flashing screens
			animateSpin(player, reward);
			return true;
		}
		return false;
	}
	
	private void showMainPage(Player player, String resultMessage)
	{
		StringBuilder html = new StringBuilder();
		html.append("<html><body><center><br><br>");
		html.append("<table width=740 height=23 align=\"right\" valign=\"center\" background=\"l2ui_ct1.Windows_DF_Drawer_Bg\">");
		html.append("<tr>");
		html.append("<td width=16 align=\"center\" valign=\"top\">");
		html.append("<img src=\"L2UI_CT1.RadarMap_DF_iCN_Target01\" width=16 height=16></img>");
		html.append("</td>");
		html.append("<td width=734 align=\"center\" valign=\"center\"> <font name=\"CreditTextNormal\" color=\"CCFF99\">Welcome to L2-Turbo</font>");
		html.append("</td>");
		html.append("<td width=16 align=right valign=top>");
		html.append("<img src=\"L2UI_CT1.RadarMap_DF_iCN_Target01\" width=16 height=16></img>");
		html.append("</td>");
		html.append("</tr>");
		html.append("</table><br><br>");
		html.append("<font color=LEVEL>Prizes Roulette</font><br><br>");
		
		// Horizontal row of icons
		html.append("<table><tr>");
		html.append("<td><img src=\"icon.weapon_sword_of_miracle_i00\" width=32 height=32></td>");
		html.append("<td><img src=\"icon.armor_t74_u_i00\" width=32 height=32></td>");
		html.append("<td><img src=\"icon.etc_scroll_of_enchant_weapon_i05\" width=32 height=32></td>");
		html.append("</tr></table><br><br>");
		
		html.append("<button value=\"Spin the roulette wheel (1kk adena)\" action=\"bypass -h _bbsroulette;spin\" width=250 height=32 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"><br>");
		
		if (resultMessage != null)
		{
			html.append("<br><font color=\"LEVEL\">").append(resultMessage).append("</font><br>");
		}
		
		html.append("<br><button value=\"Back\" action=\"bypass -h _bbshome\" width=200 height=30 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"/>");
		html.append("</center></body></html>");
		
		CommunityBoardHandler.separateAndSend(html.toString(), player);
	}
	
	private void animateSpin(Player player, Reward reward)
	{
		// 3 stages of "flashing"
		for (int i = 0; i < 3; i++)
		{
			int finalI = i;
			ThreadPool.schedule(() ->
			{
				StringBuilder html = new StringBuilder();
				html.append("<html><body><center><br><br>");
				html.append("<font color=LEVEL>The Roulette is spining...</font><br><br>");
				
				// Horizontal table with random icons
				html.append("<table><tr>");
				for (int j = 0; j < REWARDS.size(); j++)
				{
					String icon = REWARDS.get((finalI + RANDOM.nextInt(REWARDS.size())) % REWARDS.size()).icon;
					html.append("<td><img src=\"").append(icon).append("\" width=32 height=32></td>");
				}
				html.append("</tr></table>");
				
				html.append("<br><br><font color=LEVEL>Wait...</font>");
				html.append("</center></body></html>");
				
				CommunityBoardHandler.separateAndSend(html.toString(), player);
			}, i * 1000);
		}
		
		// Final in 4 seconds
		ThreadPool.schedule(() ->
		{
			player.addItem(ItemProcessType.CONSUME, reward.itemId, reward.count, player, true);
			
			StringBuilder html = new StringBuilder();
			html.append("<html><body><center><br><br>");
			html.append("<font color=LEVEL>The roulette wheel stopped!</font><br><br>");
			
			// The won item is in the center
			html.append("<img src=\"").append(reward.icon).append("\" width=64 height=64><br><br>");
			html.append("<font color=\"LEVEL\">You won: ").append(reward.count).append(" items (ID: ").append(reward.itemId).append(")</font><br><br>");
			
			html.append("<button value=\"Spin some more\" action=\"bypass -h _bbsroulette;spin\" width=250 height=32 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"><br>");
			html.append("<button value=\"Back\" action=\"bypass -h _bbshome\" width=200 height=30 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"/>");
			
			html.append("</center></body></html>");
			
			CommunityBoardHandler.separateAndSend(html.toString(), player);
		}, 4000);
	}
	
	private static class Reward
	{
		final int itemId;
		final long count;
		final String icon;
		
		Reward(int itemId, long count, String icon)
		{
			this.itemId = itemId;
			this.count = count;
			this.icon = icon;
		}
	}
}
