/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.l2journey.gameserver.model.actor.instance;

import com.l2journey.Config;
import com.l2journey.gameserver.cache.HtmCache;
import com.l2journey.gameserver.data.enums.CategoryType;
import com.l2journey.gameserver.data.xml.CategoryData;
import com.l2journey.gameserver.data.xml.ClassListData;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.creature.InstanceType;
import com.l2journey.gameserver.model.actor.enums.creature.Race;
import com.l2journey.gameserver.model.actor.enums.player.PlayerClass;
import com.l2journey.gameserver.model.actor.templates.NpcTemplate;
import com.l2journey.gameserver.model.clan.Clan;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.item.holders.ItemHolder;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2journey.gameserver.network.serverpackets.TutorialCloseHtml;
import com.l2journey.gameserver.network.serverpackets.TutorialShowHtml;
import com.l2journey.gameserver.network.serverpackets.TutorialShowQuestionMark;

/**
 * @version $Revision: 1.4.2.1.2.7 $ $Date: 2005/03/27 15:29:32 $
 */
public class ClassMaster extends Merchant
{
	/**
	 * Creates a class master.
	 * @param template the class master NPC template
	 */
	public ClassMaster(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.ClassMaster);
	}
	
	@Override
	public String getHtmlPath(int npcId, int value)
	{
		String pom = "";
		if (value == 0)
		{
			pom = Integer.toString(npcId);
		}
		else
		{
			pom = npcId + "-" + value;
		}
		return "data/html/classmaster/" + pom + ".htm";
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (command.startsWith("1stClass"))
		{
			showHtmlMenu(player, getObjectId(), 1);
		}
		else if (command.startsWith("2ndClass"))
		{
			showHtmlMenu(player, getObjectId(), 2);
		}
		else if (command.startsWith("3rdClass"))
		{
			showHtmlMenu(player, getObjectId(), 3);
		}
		else if (command.startsWith("change_class"))
		{
			final int val = Integer.parseInt(command.substring(13));
			if (checkAndChangeClass(player, val))
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player, "data/html/classmaster/ok.htm");
				html.replace("%name%", ClassListData.getInstance().getClass(val).getClientCode());
				html.replace("%RaceIcon%", getRaceIcon(player));
				player.sendPacket(html);
			}
		}
		else if (command.startsWith("become_noble"))
		{
			if (!player.isNoble())
			{
				player.setNoble(true);
				player.updateUserInfo();
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player, "data/html/classmaster/nobleok.htm");
				player.sendPacket(html);
			}
		}
		else if (command.startsWith("learn_skills"))
		{
			player.giveAvailableSkills(Config.AUTO_LEARN_FS_SKILLS, true, Config.AUTO_LEARN_SKILLS_WITHOUT_ITEMS);
		}
		else if (command.startsWith("increase_clan_level"))
		{
			final Clan clan = player.getClan();
			if (!player.isClanLeader())
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player, "data/html/classmaster/noclanleader.htm");
				player.sendPacket(html);
			}
			else if (clan.getLevel() >= 5)
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player, "data/html/classmaster/noclanlevel.htm");
				player.sendPacket(html);
			}
			else
			{
				clan.changeLevel(5);
			}
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	
	public static void onTutorialLink(Player player, String request)
	{
		if (!Config.ALTERNATE_CLASS_MASTER || (request == null) || !request.startsWith("CO") || !player.getClient().getFloodProtectors().canUseServerBypass())
		{
			return;
		}
		
		try
		{
			final int val = Integer.parseInt(request.substring(2));
			checkAndChangeClass(player, val);
		}
		catch (NumberFormatException _)
		{
			// Ignore.
		}
		player.sendPacket(TutorialCloseHtml.STATIC_PACKET);
	}
	
	public static void onTutorialQuestionMark(Player player, int number)
	{
		if (!Config.ALTERNATE_CLASS_MASTER || (number != 1001))
		{
			return;
		}
		
		showTutorialHtml(player);
	}
	
	public static void showQuestionMark(Player player)
	{
		if (!Config.ALTERNATE_CLASS_MASTER)
		{
			return;
		}
		
		final PlayerClass classId = player.getPlayerClass();
		if ((getMinLevel(classId.level()) > player.getLevel()) || !Config.CLASS_MASTER_SETTINGS.isAllowed(classId.level() + 1))
		{
			return;
		}
		
		player.sendPacket(new TutorialShowQuestionMark(1001));
	}
	
	private void showHtmlMenu(Player player, int objectId, int level)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(objectId);
		if (!Config.ALLOW_CLASS_MASTERS)
		{
			html.setFile(player, "data/html/classmaster/disabled.htm");
		}
		else if (!Config.CLASS_MASTER_SETTINGS.isAllowed(level))
		{
			final int jobLevel = player.getPlayerClass().level();
			final StringBuilder sb = new StringBuilder(100);
			sb.append("<html><body>");
			switch (jobLevel)
			{
				case 0:
				{
					if (Config.CLASS_MASTER_SETTINGS.isAllowed(1))
					{
						sb.append("Come back here when you reached level 20 to change your class.<br>");
					}
					else if (Config.CLASS_MASTER_SETTINGS.isAllowed(2))
					{
						sb.append("Come back after your first occupation change.<br>");
					}
					else if (Config.CLASS_MASTER_SETTINGS.isAllowed(3))
					{
						sb.append("Come back after your second occupation change.<br>");
					}
					else
					{
						sb.append("I can't change your occupation.<br>");
					}
					break;
				}
				case 1:
				{
					if (Config.CLASS_MASTER_SETTINGS.isAllowed(2))
					{
						sb.append("Come back here when you reached level 40 to change your class.<br>");
					}
					else if (Config.CLASS_MASTER_SETTINGS.isAllowed(3))
					{
						sb.append("Come back after your second occupation change.<br>");
					}
					else
					{
						sb.append("I can't change your occupation.<br>");
					}
					break;
				}
				case 2:
				{
					if (Config.CLASS_MASTER_SETTINGS.isAllowed(3))
					{
						sb.append("Come back here when you reached level 76 to change your class.<br>");
					}
					else
					{
						sb.append("I can't change your occupation.<br>");
					}
					break;
				}
				case 3:
				{
					sb.append("There is no class change available for you anymore.<br>");
					break;
				}
			}
			sb.append("</body></html>");
			html.setHtml(sb.toString());
		}
		else
		{
			final PlayerClass currentClassId = player.getPlayerClass();
			if (currentClassId.level() >= level)
			{
				html.setFile(player, "data/html/classmaster/nomore.htm");
			}
			else
			{
				final int minLevel = getMinLevel(currentClassId.level());
				if ((player.getLevel() >= minLevel) || Config.ALLOW_ENTIRE_TREE)
				{
					final StringBuilder menu = new StringBuilder(100);
					for (PlayerClass cid : PlayerClass.values())
					{
						if ((cid == PlayerClass.INSPECTOR) && (player.getTotalSubClasses() < 2))
						{
							continue;
						}
						if (validateClassId(currentClassId, cid) && (cid.level() == level))
						{
							menu.append("<a action=\"bypass -h npc_%objectId%_change_class " + cid.getId() + "\">" + ClassListData.getInstance().getClass(cid).getClientCode() + "</a><br>");
						}
					}
					
					if (!menu.isEmpty())
					{
						html.setFile(player, "data/html/classmaster/template.htm");
						html.replace("%name%", ClassListData.getInstance().getClass(currentClassId).getClientCode());
						html.replace("%RaceIcon%", getRaceIcon(player));
						html.replace("%menu%", menu.toString());
					}
					else
					{
						html.setFile(player, "data/html/classmaster/comebacklater.htm");
						html.replace("%level%", String.valueOf(getMinLevel(level - 1)));
					}
				}
				else
				{
					if (minLevel < Integer.MAX_VALUE)
					{
						html.setFile(player, "data/html/classmaster/comebacklater.htm");
						html.replace("%level%", String.valueOf(minLevel));
					}
					else
					{
						html.setFile(player, "data/html/classmaster/nomore.htm");
					}
				}
			}
		}
		
		html.replace("%objectId%", String.valueOf(objectId));
		html.replace("%req_items%", getRequiredItems(level));
		player.sendPacket(html);
	}
	
	private static void showTutorialHtml(Player player)
	{
		final PlayerClass currentClassId = player.getPlayerClass();
		if ((getMinLevel(currentClassId.level()) > player.getLevel()) && !Config.ALLOW_ENTIRE_TREE)
		{
			return;
		}
		
		String msg = HtmCache.getInstance().getHtm(player, "data/html/classmaster/tutorialtemplate.htm");
		msg = msg.replace("%name%", ClassListData.getInstance().getClass(currentClassId).getClassName()); // getEscapedClientCode());
		msg = msg.replace("%RaceIcon%", getRaceIcon(player));
		
		final StringBuilder menu = new StringBuilder(100);
		for (PlayerClass cid : PlayerClass.values())
		{
			if ((cid == PlayerClass.INSPECTOR) && (player.getTotalSubClasses() < 2))
			{
				continue;
			}
			if (validateClassId(currentClassId, cid))
			{
				menu.append("<a action=\"link CO" + cid.getId() + "\">" + ClassListData.getInstance().getClass(cid).getClassName() + "</a><br>"); // getEscapedClientCode());
			}
		}
		
		msg = msg.replace("%menu%", menu.toString());
		msg = msg.replace("%req_items%", getRequiredItems(currentClassId.level() + 1));
		player.sendPacket(new TutorialShowHtml(msg));
	}
	
	private static boolean checkAndChangeClass(Player player, int value)
	{
		final PlayerClass currentClassId = player.getPlayerClass();
		if (((getMinLevel(currentClassId.level()) > player.getLevel()) && !Config.ALLOW_ENTIRE_TREE) || !validateClassId(currentClassId, value))
		{
			return false;
		}
		
		final int newJobLevel = currentClassId.level() + 1;
		
		// Weight/Inventory check
		if (!Config.CLASS_MASTER_SETTINGS.getRewardItems(newJobLevel).isEmpty() && !player.isInventoryUnder90(false))
		{
			player.sendPacket(SystemMessageId.UNABLE_TO_PROCESS_THIS_REQUEST_UNTIL_YOUR_INVENTORY_S_WEIGHT_AND_SLOT_COUNT_ARE_LESS_THAN_80_PERCENT_OF_CAPACITY);
			return false;
		}
		
		// check if player have all required items for class transfer
		for (ItemHolder holder : Config.CLASS_MASTER_SETTINGS.getRequireItems(newJobLevel))
		{
			if (player.getInventory().getInventoryItemCount(holder.getId(), -1) < holder.getCount())
			{
				player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
				return false;
			}
		}
		
		// get all required items for class transfer
		for (ItemHolder holder : Config.CLASS_MASTER_SETTINGS.getRequireItems(newJobLevel))
		{
			if (!player.destroyItemByItemId(ItemProcessType.FEE, holder.getId(), holder.getCount(), player, true))
			{
				return false;
			}
		}
		
		// reward player with items
		for (ItemHolder holder : Config.CLASS_MASTER_SETTINGS.getRewardItems(newJobLevel))
		{
			player.addItem(ItemProcessType.REWARD, holder.getId(), holder.getCount(), player, true);
		}
		
		player.setPlayerClass(value);
		
		if (player.isSubClassActive())
		{
			player.getSubClasses().get(player.getClassIndex()).setPlayerClass(player.getActiveClass());
		}
		else
		{
			player.setBaseClass(player.getActiveClass());
		}
		
		player.broadcastUserInfo();
		
		if (Config.CLASS_MASTER_SETTINGS.isAllowed(player.getPlayerClass().level() + 1) && Config.ALTERNATE_CLASS_MASTER && (((player.getPlayerClass().level() == 1) && (player.getLevel() >= 40)) || (CategoryData.getInstance().isInCategory(CategoryType.THIRD_CLASS_GROUP, player.getPlayerClass().getId()) && (player.getLevel() >= 76))))
		{
			showQuestionMark(player);
		}
		
		return true;
	}
	
	/**
	 * @param level - current skillId level (0 - start, 1 - first, etc)
	 * @return minimum player level required for next class transfer
	 */
	private static int getMinLevel(int level)
	{
		switch (level)
		{
			case 0:
			{
				return 20;
			}
			case 1:
			{
				return 40;
			}
			case 2:
			{
				return 76;
			}
			default:
			{
				return Integer.MAX_VALUE;
			}
		}
	}
	
	/**
	 * Returns true if class change is possible
	 * @param oldCID current player ClassId
	 * @param value new class index
	 * @return
	 */
	private static boolean validateClassId(PlayerClass oldCID, int value)
	{
		return validateClassId(oldCID, PlayerClass.getPlayerClass(value));
	}
	
	/**
	 * Returns true if class change is possible
	 * @param oldCID current player ClassId
	 * @param newCID new ClassId
	 * @return true if class change is possible
	 */
	private static boolean validateClassId(PlayerClass oldCID, PlayerClass newCID)
	{
		if ((newCID == null) || (newCID.getRace() == null))
		{
			return false;
		}
		
		return (oldCID == newCID.getParent()) || (Config.ALLOW_ENTIRE_TREE && newCID.childOf(oldCID));
	}
	
	private static String getRequiredItems(int level)
	{
		if ((Config.CLASS_MASTER_SETTINGS.getRequireItems(level) == null) || Config.CLASS_MASTER_SETTINGS.getRequireItems(level).isEmpty())
		{
			return "<tr><td>none</td></tr>";
		}
		final StringBuilder sb = new StringBuilder();
		for (ItemHolder holder : Config.CLASS_MASTER_SETTINGS.getRequireItems(level))
		{
			sb.append("<tr><td><font color=\"LEVEL\">" + holder.getCount() + "</font></td><td>" + ItemData.getInstance().getTemplate(holder.getId()).getName() + "</td></tr>");
		}
		return sb.toString();
	}
	
	private static String getRaceIcon(Player player)
	{
		final Race race = player.getRace();
		if (race == null)
		{
			return "icon.skill0000";
		}
		
		switch (race)
		{
			case HUMAN:
				return "icon.skillhuman";
			case ELF:
				return "icon.skillelf";
			case DARK_ELF:
				return "icon.skilldarkelf";
			case ORC:
				return "icon.skillorc";
			case DWARF:
				return "icon.skilldwarf";
			case KAMAEL:
				return "icon.skillkamael";
			default:
				return "icon.skill0000";
		}
	}
}
