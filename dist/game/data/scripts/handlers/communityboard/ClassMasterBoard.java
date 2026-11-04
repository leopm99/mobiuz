package handlers.communityboard;

import java.util.List;

import com.l2journey.Config;
import com.l2journey.gameserver.data.xml.ClassListData;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.handler.CommunityBoardHandler;
import com.l2journey.gameserver.handler.IParseBoardHandler;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.player.PlayerClass;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.MagicSkillLaunched;
import com.l2journey.gameserver.network.serverpackets.MagicSkillUse;

/**
 * @author Debug
 */

public class ClassMasterBoard implements IParseBoardHandler
{
	private static final String[] COMMANDS =
	{
		"_bbsclassmaster",
		"_bbsclassmaster;change_class"
	};
	
	@Override
	public String[] getCommunityBoardCommands()
	{
		return COMMANDS;
	}
	
	private int getMinLevelForClassChange(int classLevel)
	{
		switch (classLevel)
		{
			case 0:
				return 20;
			case 1:
				return 40;
			case 2:
				return 76;
			default:
				return Integer.MAX_VALUE;
		}
	}
	
	@Override
	public boolean parseCommunityBoardCommand(String command, Player player)
	{
		if (command.equals("_bbsclassmaster"))
		{
			String html = getClassManagerMenu(player);
			CommunityBoardHandler.separateAndSend(html, player);
			return true;
		}
		else if (command.startsWith("_bbsclassmaster;change_class"))
		{
			String[] params = command.split(";");
			if (params.length > 3)
			{
				int classId = Integer.parseInt(params[2]);
				int itemId = Integer.parseInt(params[3]);
				
				PlayerClass currentClass = PlayerClass.getPlayerClass(player.getPlayerClass().getId());
				PlayerClass targetClass = PlayerClass.getPlayerClass(classId);
				
				if (player.getClassIndex() != 0)
				{
					player.sendMessage("Class change is only available for the main profession.");
					return true;
				}
				
				if (!currentClass.getNextClasses().contains(targetClass))
				{
					player.sendMessage("The selected class is invalid.");
					return true;
				}
				
				boolean paid = false;
				List<Config.ItemPrice> prices = Config.CLASS_MASTER_PRICES.get(currentClass.level());
				if ((prices != null) && !prices.isEmpty())
				{
					for (Config.ItemPrice price : prices)
					{
						if (price.itemId == itemId)
						{
							if (player.getInventory().getInventoryItemCount(price.itemId, -1) >= price.amount)
							{
								player.getInventory().destroyItemByItemId(ItemProcessType.BUY, price.itemId, price.amount, player, null);
								ItemTemplate item = ItemData.getInstance().getTemplate(price.itemId);
								String itemName = (item != null) ? item.getName() : ("ID: " + price.itemId);
								player.sendMessage("Profession change has been paid.: " + price.amount + " " + itemName);
								paid = true;
							}
							else
							{
								player.sendMessage("Not enough items to pay with the selected currency.");
							}
							break;
						}
					}
					
					if (!paid)
					{
						return true;
					}
				}
				
				player.setBaseClass(classId);
				player.setClassTemplate(classId);
				player.broadcastUserInfo();
				player.sendMessage("Class changed to: " + ClassListData.getInstance().getClass(player.getPlayerClass()).getClassName());
				
				player.sendPacket(SystemMessageId.CONGRATULATIONS_YOU_VE_COMPLETED_A_CLASS_TRANSFER);
				player.broadcastPacket(new MagicSkillUse(player, 5103, 1, 0, 0));
				player.broadcastPacket(new MagicSkillLaunched(player, 5103, 1));
				player.broadcastSocialAction(3);
				
				if (targetClass.getNextClasses().isEmpty())
				{
					player.sendMessage("No more professions available.");
				}
				
				String html = getClassManagerMenu(player);
				CommunityBoardHandler.separateAndSend(html, player);
			}
			return true;
		}
		return false;
	}
	
	public String getClassManagerMenu(Player player)
	{
		StringBuilder htm = new StringBuilder();
		PlayerClass currentClass = PlayerClass.getPlayerClass(player.getPlayerClass().getId());
		
		htm.append("<html noscrollbar><body><center><br><br>");
		htm.append("<font name=\"hs12\" color=\"CDB67F\">Class Master</font><br><br>");
		
		if (!Config.CLASS_MASTER_BOARD)
		{
			htm.append("<font color=\"FF5555\">Class master is disabled!</font>");
		}
		else
		{
			int playerLevel = player.getLevel();
			int minLevel = getMinLevelForClassChange(currentClass.level());
			
			if (currentClass.getNextClasses().isEmpty())
			{
				htm.append("<font color=\"AAAAAA\">You have already reached the maximum profession level.</font>");
			}
			else if (playerLevel >= minLevel)
			{
				List<Config.ItemPrice> prices = Config.CLASS_MASTER_PRICES.get(currentClass.level());
				
				if ((prices != null) && !prices.isEmpty())
				{
					for (PlayerClass nextClass : currentClass.getNextClasses())
					{
						String className = ClassListData.getInstance().getClass(nextClass.getId()).getClassName();
						
						for (Config.ItemPrice price : prices)
						{
							ItemTemplate item = ItemData.getInstance().getTemplate(price.itemId);
							String itemName = (item != null) ? item.getName() : "ID: " + price.itemId;
							String icon = ((item != null) && (item.getIcon() != null)) ? item.getIcon() : "icon.etc_question_mark_i00";
							
							htm.append(String.format("<table><tr><td><img src=\"%s\" width=32 height=32></td>" + "<td><button value=\"%s for %d %s\" action=\"bypass -h _bbsclassmaster;change_class;%d;%d\" width=200 height=32 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td></tr></table><br>", icon, className, price.amount, itemName, nextClass.getId(), price.itemId));
						}
					}
				}
			}
			else
			{
				htm.append("<font color=\"AAAAAA\">Level up to <font color=\"CDB67F\">").append(minLevel).append("</font>, to change the class.</font>");
			}
		}
		
		htm.append("<br><button value=\"Back\" action=\"bypass -h _bbshome\" width=150 height=30 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" />");
		htm.append("</center></body></html>");
		return htm.toString();
	}
}
