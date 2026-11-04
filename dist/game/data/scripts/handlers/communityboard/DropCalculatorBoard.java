/*
 * Copyright (C) 2004-2015 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.communityboard;

import java.util.List;
import java.util.StringTokenizer;

import com.l2journey.Config;
import com.l2journey.gameserver.cache.HtmCache;
import com.l2journey.gameserver.cache.ImagesCache;
import com.l2journey.gameserver.data.SpawnTable;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.data.xml.NpcData;
import com.l2journey.gameserver.handler.IParseBoardHandler;
import com.l2journey.gameserver.model.Spawn;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.npc.DropType;
import com.l2journey.gameserver.model.actor.templates.NpcTemplate;
import com.l2journey.gameserver.model.dropcalculator.DropInfoFunctions;
import com.l2journey.gameserver.model.dropcalculator.DropInfoHandler;
import com.l2journey.gameserver.model.dropcalculator.DropInfoHolder;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.zone.ZoneId;
import com.l2journey.gameserver.network.enums.ChatType;
import com.l2journey.gameserver.network.serverpackets.CreatureSay;
import com.l2journey.gameserver.network.serverpackets.RadarControl;
import com.l2journey.gameserver.network.serverpackets.ShowBoard;
import com.l2journey.gameserver.util.Util;

import handlers.bypasshandlers.NpcViewMod;

/**
 * @author vGodFather
 */
public class DropCalculatorBoard implements IParseBoardHandler
{
	
	@Override
	public String[] getCommunityBoardCommands()
	{
		return new String[]
		{
			"_bbssearchdropItemsByName",
			"_bbssearchdropMonstersByItem",
			"_bbssearchdropMonsterDetailsByItem",
			"_bbssearchdropMonstersByName",
			"_bbssearchdropMonsterDetailsByName",
			"_bbssearchNpcDropList",
			"_bbssearchShowSkills",
			"_bbssearchdropCalc"
		};
	}
	
	@Override
	public boolean parseCommunityBoardCommand(String command, Player activeChar)
	{
		if (!Config.ENABLE_DROP_CALCULATOR)
		{
			activeChar.sendMessage("Drop Calculator is disabled by admin.");
			return false;
		}
		
		try
		{
			validateCommand(command);
		}
		catch (IllegalArgumentException | java.util.NoSuchElementException e)
		{
			activeChar.sendMessage("Invalid Drop Calculator request.");
			return false;
		}

		try
		{
			if (command.equals("_bbssearchdropCalc"))
			{
				showMainPage(activeChar);
				return true;
			}
			else if (command.startsWith("_bbssearchdropItemsByName"))
			{
				StringTokenizer st = new StringTokenizer(command, "_");
				st.nextToken();
				
				if (!st.hasMoreTokens())
				{
					showMainPage(activeChar);
					return true;
				}
				String itemName = st.nextToken().trim();
				int itemsPage = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 1;
				int sortMethod = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 0;
				separateAndSend(showDropItemsByNamePage(activeChar, itemName, itemsPage, sortMethod), activeChar);
				return true;
			}
			else if (command.startsWith("_bbssearchdropMonstersByItem"))
			{
				StringTokenizer st = new StringTokenizer(command, "_");
				st.nextToken();
				
				int itemId = Integer.parseInt(st.nextToken());
				int monstersPage = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 1;
				int sortMethod = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 0;
				separateAndSend(showDropMonstersByItem(activeChar, itemId, monstersPage, sortMethod), activeChar);
				return true;
			}
			else if (command.startsWith("_bbssearchdropMonstersByName"))
			{
				StringTokenizer st = new StringTokenizer(command, "_");
				st.nextToken();
				
				if (!st.hasMoreTokens())
				{
					showMainPage(activeChar);
					return true;
				}
				String monsterName = st.nextToken().trim();
				int monsterPage = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 1;
				int sortMethod = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 0;
				separateAndSend(showDropMonstersByName(activeChar, monsterName, monsterPage, sortMethod), activeChar);
				return true;
			}
			
			else if (command.startsWith("_bbssearchdropMonsterDetailsByItem") || command.startsWith("_bbssearchdropMonsterDetailsByName"))
			{
				StringTokenizer st = new StringTokenizer(command, "_");
				st.nextToken();
				
				int monsterId = Integer.parseInt(st.nextToken());
				
				if (st.hasMoreTokens())
				{
					
					manageButton(activeChar, Integer.parseInt(st.nextToken()), monsterId);
				}
				else
				{
					
					String html;
					if (command.startsWith("_bbssearchdropMonsterDetailsByItem"))
					{
						html = showdropMonsterDetailsByItem(activeChar, monsterId);
					}
					else
					{
						html = showDropMonsterDetailsByName(activeChar, monsterId);
					}
					separateAndSend(html, activeChar);
				}
				return true;
			}
			
			else if (command.startsWith("_bbssearchNpcDropList"))
			{
				String[] parts = command.split("_");
				activeChar.getVariables().set("DCDropType", parts[2]);
				DropInfoFunctions.showNpcDropList(activeChar, parts[2], Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
				return true;
			}
			else if (command.startsWith("_bbssearchShowSkills"))
			{
				String[] parts = command.split("_");
				DropInfoFunctions.showNpcSkillList(activeChar, Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
				return true;
			}
			
			// Se nenhum comando corresponder, nao faz nada.
			return false;
		}
		catch (RuntimeException e)
		{
			java.util.logging.Logger.getLogger(DropCalculatorBoard.class.getName()).log(java.util.logging.Level.WARNING, "Could not process Drop Calculator command: " + command, e);
			activeChar.sendMessage("Could not complete the Drop Calculator request. Please contact a GM.");
			return false;
		}
	}

	private static void validateCommand(String command)
	{
		final String[] parts = command.split("_", -1);
		if (parts.length < 2) { throw new IllegalArgumentException(); }
		final String name = parts[1];
		if (name.equals("bbssearchdropCalc")) { return; }
		final boolean search = name.equals("bbssearchdropItemsByName") || name.equals("bbssearchdropMonstersByName");
		if (search && (parts.length == 2)) { return; }
		if (parts.length < 3) { throw new IllegalArgumentException(); }
		if (search || name.equals("bbssearchdropMonstersByItem"))
		{
			if (parts.length > 5) { throw new IllegalArgumentException(); }
			if (!search && ItemData.getInstance().getTemplate(Integer.parseInt(parts[2])) == null) { throw new IllegalArgumentException(); }
			final int page = parts.length > 3 ? Integer.parseInt(parts[3]) : 1;
			final int sort = parts.length > 4 ? Integer.parseInt(parts[4]) : 0;
			if ((page < 1) || (page > (Integer.MAX_VALUE / 12)) || (sort < 0) || (sort > (search ? 2 : 4))) { throw new IllegalArgumentException(); }
			return;
		}
		final boolean dropList = name.equals("bbssearchNpcDropList");
		final boolean skills = name.equals("bbssearchShowSkills");
		if (dropList && (parts.length != 5)) { throw new IllegalArgumentException(); }
		if (skills && (parts.length != 4)) { throw new IllegalArgumentException(); }
		final int npcId = Integer.parseInt(parts[dropList ? 3 : 2]);
		if ((NpcData.getInstance().getTemplate(npcId) == null) || ((Config.RESTRICTED_MOB_DROPLIST_IDS != null) && Config.RESTRICTED_MOB_DROPLIST_IDS.contains(npcId))) { throw new IllegalArgumentException(); }
		if (dropList || skills)
		{
			final int page = Integer.parseInt(parts[dropList ? 4 : 3]);
			if ((page < (dropList ? 1 : 0)) || (page > (Integer.MAX_VALUE / 20))) { throw new IllegalArgumentException(); }
			if (dropList && !(parts[2].equalsIgnoreCase("DEATH") || parts[2].equalsIgnoreCase("CORPSE") || parts[2].equalsIgnoreCase("SPOIL"))) { throw new IllegalArgumentException(); }
		}
		else if ((parts.length > 4) || ((parts.length == 4) && ((Integer.parseInt(parts[3]) < 1) || (Integer.parseInt(parts[3]) > 5)))) { throw new IllegalArgumentException(); }
	}

	private void showMainPage(Player player)
	{
		String html = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/dropcalculator/bbs_dropCalcMain.html");
		separateAndSend(html, player);
	}
	
	private String showDropMonstersByName(Player player, String monsterName, int page, int sort)
	{
		player.getVariables().set("DCMonsterSort", sort);
		player.getVariables().set("DCMonsterName", monsterName);
		player.getVariables().set("DCMonstersPage", page);
		String html = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/dropcalculator/bbs_dropMonstersByName.html");
		return replaceMonstersByName(html, monsterName, page, sort);
	}
	
	private static String replaceMonstersByName(String html, String monsterName, int page, int sort)
	{
		String newHtml = html;
		
		List<NpcTemplate> npcTemplates = DropInfoFunctions.getNpcsContainingString(monsterName);
		npcTemplates = DropInfoFunctions.sortMonsters(npcTemplates, sort);
		
		int npcIndex = 0;
		
		for (int i = 0; i < 12; i++)
		{
			npcIndex = i + ((page - 1) * 12);
			NpcTemplate npc = npcTemplates.size() > npcIndex ? npcTemplates.get(npcIndex) : null;
			
			newHtml = newHtml.replace("<?name_" + i + "?>", npc != null ? DropInfoFunctions.getName(npc.getName()) : "...");
			newHtml = newHtml.replace("<?drop_" + i + "?>", npc != null ? String.valueOf(DropInfoFunctions.getDropsCount(npc, false)) : "...");
			newHtml = newHtml.replace("<?spoil_" + i + "?>", npc != null ? String.valueOf(DropInfoFunctions.getDropsCount(npc, true)) : "...");
			newHtml = newHtml.replace("<?bp_" + i + "?>", npc != null ? "<button value=\"show\" action=\"bypass _bbssearchdropMonsterDetailsByName_" + npc.getId() + "\" width=40 height=12 back=\"L2UI_CT1.ListCTRL_DF_Title_Down\" fore=\"L2UI_CT1.ListCTRL_DF_Title\">" : "...");
		}
		
		newHtml = newHtml.replace("<?previous?>", page > 1 ? "<button action=\"bypass _bbssearchdropMonstersByName_" + monsterName + "_" + (page - 1) + "_" + sort + "\" width=16 height=16 back=\"L2UI_CH3.shortcut_prev_down\" fore=\"L2UI_CH3.shortcut_prev\">" : "<br>");
		newHtml = newHtml.replace("<?next?>", npcTemplates.size() > (npcIndex + 1) ? "<button action=\"bypass _bbssearchdropMonstersByName_" + monsterName + "_" + (page + 1) + "_" + sort + "\" width=16 height=16 back=\"L2UI_CH3.shortcut_next_down\" fore=\"L2UI_CH3.shortcut_next\">" : "<br>");
		
		newHtml = newHtml.replace("<?search?>", monsterName);
		newHtml = newHtml.replace("<?size?>", Util.formatAdena(npcTemplates.size()));
		newHtml = newHtml.replace("<?page?>", String.valueOf(page));
		newHtml = newHtml.replace("<?monsterName?>", sort == 0 ? "<font color=\"bbbbbb\">Monster Name</font>" : "<a action=\"bypass _bbssearchdropMonstersByName_" + monsterName + "_" + page + "_" + 0 + "\"><font color=\"bbbbbb\">Monster Name</font></a>");
		newHtml = newHtml.replace("<?droppingItems?>", sort == 1 ? "<font color=\"bbbbbb\">Dropping Items</font>" : "<a action=\"bypass _bbssearchdropMonstersByName_" + monsterName + "_" + page + "_" + 1 + "\"><font color=\"bbbbbb\">Dropping Items</font></a>");
		newHtml = newHtml.replace("<?spoilingItems?>", sort == 2 ? "<font color=\"bbbbbb\">Spoiling Items</font>" : "<a action=\"bypass _bbssearchdropMonstersByName_" + monsterName + "_" + page + "_" + 2 + "\"><font color=\"bbbbbb\">Spoiling Items</font></a>");
		
		return newHtml;
	}
	
	private String showDropItemsByNamePage(Player player, String itemName, int page, int sort)
	{
		player.getVariables().set("DCItemSort", sort);
		player.getVariables().set("DCItemName", itemName);
		player.getVariables().set("DCItemsPage", page);
		String html = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/dropcalculator/bbs_dropItemsByName.html");
		return replaceItemsByNamePage(html, itemName, page, sort);
	}
	
	private String replaceItemsByNamePage(String html, String itemName, int page, int sort)
	{
		String newHtml = html;
		
		List<ItemTemplate> itemsByName = DropInfoFunctions.getItemsByNameContainingString(itemName);
		itemsByName = DropInfoFunctions.sortItems(itemsByName, sort);
		
		int itemIndex = 0;
		
		for (int i = 0; i < 12; i++)
		{
			itemIndex = i + ((page - 1) * 12);
			ItemTemplate item = itemsByName.size() > itemIndex ? itemsByName.get(itemIndex) : null;
			
			newHtml = newHtml.replace("<?name_" + i + "?>", item != null ? DropInfoFunctions.getName(item.getName()) : "...");
			newHtml = newHtml.replace("<?drop_" + i + "?>", item != null ? String.valueOf(DropInfoFunctions.getDroplistsCountByItemId(item.getId(), false)) : "...");
			newHtml = newHtml.replace("<?spoil_" + i + "?>", item != null ? String.valueOf(DropInfoFunctions.getDroplistsCountByItemId(item.getId(), true)) : "...");
			newHtml = newHtml.replace("<?bp_" + i + "?>", item != null ? "<button value=\"show\" action=\"bypass _bbssearchdropMonstersByItem_" + item.getId() + "_1\" width=40 height=12 back=\"L2UI_CT1.ListCTRL_DF_Title_Down\" fore=\"L2UI_CT1.ListCTRL_DF_Title\">" : "...");
		}
		
		newHtml = newHtml.replace("<?previous?>", page > 1 ? "<button action=\"bypass _bbssearchdropItemsByName_" + itemName + "_" + (page - 1) + "_" + sort + "\" width=16 height=16 back=\"L2UI_CH3.shortcut_prev_down\" fore=\"L2UI_CH3.shortcut_prev\">" : "<br>");
		newHtml = newHtml.replace("<?next?>", itemsByName.size() > (itemIndex + 1) ? "<button action=\"bypass _bbssearchdropItemsByName_" + itemName + "_" + (page + 1) + "_" + sort + "\" width=16 height=16 back=\"L2UI_CH3.shortcut_next_down\" fore=\"L2UI_CH3.shortcut_next\">" : "<br>");
		
		newHtml = newHtml.replace("<?search?>", itemName);
		newHtml = newHtml.replace("<?size?>", Util.formatAdena(itemsByName.size()));
		newHtml = newHtml.replace("<?page?>", String.valueOf(page));
		newHtml = newHtml.replace("<?itemName?>", sort == 0 ? "<font color=\"bbbbbb\">Name</font>" : "<a action=\"bypass _bbssearchdropItemsByName_" + itemName + "_" + page + "_" + 0 + "\"><font color=\"bbbbbb\">Name</font></a>");
		newHtml = newHtml.replace("<?dropLists?>", sort == 1 ? "<font color=\"bbbbbb\">Number of Drop Lists</font>" : "<a action=\"bypass _bbssearchdropItemsByName_" + itemName + "_" + page + "_" + 1 + "\"><font color=\"bbbbbb\">Number of Drop Lists</font></a>");
		newHtml = newHtml.replace("<?spoilLists?>", sort == 2 ? "<font color=\"bbbbbb\">Number of Spoil Lists</font>" : "<a action=\"bypass _bbssearchdropItemsByName_" + itemName + "_" + page + "_" + 2 + "\"><font color=\"bbbbbb\">Number of Spoil Lists</font></a>");
		return newHtml;
	}
	
	private String showDropMonstersByItem(Player player, int itemId, int page, int sort)
	{
		player.getVariables().set("DCMonster2Sort", sort);
		player.getVariables().set("DCItemId", itemId);
		player.getVariables().set("DCMonstersPage", page);
		String html = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/dropcalculator/bbs_dropMonstersByItem.html");
		return replaceMonstersByItemPage(player, html, itemId, page, sort);
	}
	
	private String replaceMonstersByItemPage(Player player, String html, int itemId, int page, int sort)
	{
		String newHtml = html;
		
		List<DropInfoHolder> templates = DropInfoHandler.getInstance().getDrop(itemId);
		templates = DropInfoFunctions.sortMonsters2(templates, sort);
		
		int npcIndex = 0;
		
		for (int i = 0; i < 12; i++)
		{
			npcIndex = i + ((page - 1) * 12);
			DropInfoHolder drops = templates.size() > npcIndex ? templates.get(npcIndex) : null;
			NpcTemplate npc = templates.size() > npcIndex ? NpcData.getInstance().getTemplate(templates.get(npcIndex).getNpcId()) : null;
			
			newHtml = newHtml.replace("<?name_" + i + "?>", npc != null ? DropInfoFunctions.getName(npc.getName()) : "...");
			newHtml = newHtml.replace("<?level_" + i + "?>", npc != null ? String.valueOf(npc.getLevel()) : "...");
			newHtml = newHtml.replace("<?type_" + i + "?>", (npc != null) && (drops != null) ? drops.isSweep() ? "Spoil" : "Drop" : "...");
			newHtml = newHtml.replace("<?count_" + i + "?>", (npc != null) && (drops != null) ? DropInfoFunctions.getMinMaxDropCounts(npc, itemId, drops.isSweep()) : "...");
			newHtml = newHtml.replace("<?chance_" + i + "?>", (npc != null) && (drops != null) ? DropInfoFunctions.getDropChance(npc, itemId, drops.isSweep()) : "...");
			newHtml = newHtml.replace("<?bp_" + i + "?>", npc != null ? "<button value=\"show\" action=\"bypass _bbssearchdropMonsterDetailsByItem_" + npc.getId() + "\" width=40 height=12 back=\"L2UI_CT1.ListCTRL_DF_Title_Down\" fore=\"L2UI_CT1.ListCTRL_DF_Title\">" : "...");
		}
		
		newHtml = newHtml.replace("<?previous?>", page > 1 ? "<button action=\"bypass _bbssearchdropMonstersByItem_" + itemId + "_" + (page - 1) + "_" + sort + "\" width=16 height=16 back=\"L2UI_CH3.shortcut_prev_down\" fore=\"L2UI_CH3.shortcut_prev\">" : "<br>");
		newHtml = newHtml.replace("<?next?>", templates.size() > (npcIndex + 1) ? "<button action=\"bypass _bbssearchdropMonstersByItem_" + itemId + "_" + (page + 1) + "_" + sort + "\" width=16 height=16 back=\"L2UI_CH3.shortcut_next_down\" fore=\"L2UI_CH3.shortcut_next\">" : "<br>");
		
		newHtml = newHtml.replace("<?search?>", player.getVariables().getString("DCItemName", ItemData.getInstance().getTemplate(itemId).getName()));
		newHtml = newHtml.replace("<?item?>", ItemData.getInstance().getTemplate(itemId).getName());
		newHtml = newHtml.replace("<?size?>", Util.formatAdena(templates.size()));
		newHtml = newHtml.replace("<?back?>", String.valueOf(player.getVariables().getInt("DCItemsPage", 1)));
		newHtml = newHtml.replace("<?page?>", String.valueOf(page));
		
		newHtml = newHtml.replace("<?monsterName?>", sort == 0 ? "<font color=\"bbbbbb\">Name</font>" : "<a action=\"bypass _bbssearchdropMonstersByItem_" + itemId + "_" + page + "_" + 0 + "\"><font color=\"bbbbbb\">Name</font></a>");
		newHtml = newHtml.replace("<?level?>", sort == 1 ? "<font color=\"bbbbbb\">Level</font>" : "<a action=\"bypass _bbssearchdropMonstersByItem_" + itemId + "_" + page + "_" + 1 + "\"><font color=\"bbbbbb\">Level</font></a>");
		newHtml = newHtml.replace("<?chance?>", sort == 2 ? "<font color=\"bbbbbb\">Chance</font>" : "<a action=\"bypass _bbssearchdropMonstersByItem_" + itemId + "_" + page + "_" + 2 + "\"><font color=\"bbbbbb\">Chance</font></a>");
		newHtml = newHtml.replace("<?type?>", sort == 3 ? "<font color=\"bbbbbb\">Type</font>" : "<a action=\"bypass _bbssearchdropMonstersByItem_" + itemId + "_" + page + "_" + 3 + "\"><font color=\"bbbbbb\">Type</font></a>");
		newHtml = newHtml.replace("<?count?>", sort == 4 ? "<font color=\"bbbbbb\">Count [Min...Max]</font>" : "<a action=\"bypass _bbssearchdropMonstersByItem_" + itemId + "_" + page + "_" + 4 + "\"><font color=\"bbbbbb\">Count [Min...Max]</font></a>");
		
		newHtml = newHtml.replace("<?sort?>", String.valueOf(player.getVariables().getInt("DCItemSort", 0)));
		return newHtml;
	}
	
	private String showdropMonsterDetailsByItem(Player player, int monsterId)
	{
		String html = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/dropcalculator/bbs_dropMonsterDetailsByItem.html");
		return replaceMonsterDetails(player, html, monsterId);
	}
	
	private String showDropMonsterDetailsByName(Player player, int monsterId)
	{
		String html = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/dropcalculator/bbs_dropMonsterDetailsByName.html");
		return replaceMonsterDetails(player, html, monsterId);
	}
	
	private String replaceMonsterDetails(Player player, String html, int monsterId)
	{
		String newHtml = html;
		
		int itemId = player.getVariables().getInt("DCItemId", -1);
		NpcTemplate template = NpcData.getInstance().getTemplate(monsterId);
		ItemTemplate item = itemId > -1 ? ItemData.getInstance().getTemplate(itemId) : null;
		
		newHtml = newHtml.replace("<?name?>", player.getVariables().getString("DCMonsterName", ""));
		newHtml = newHtml.replace("<?monster_name?>", template.getName());
		newHtml = newHtml.replace("<?item?>", item != null ? item.getName() : "...");
		newHtml = newHtml.replace("<?item_id?>", item != null ? String.valueOf(item.getId()) : "...");
		newHtml = newHtml.replace("<?back?>", String.valueOf(player.getVariables().getInt("DCMonstersPage", 1)));
		newHtml = newHtml.replace("<?monster?>", String.valueOf(monsterId));
		newHtml = newHtml.replace("<?level?>", String.valueOf(template.getLevel()));
		newHtml = newHtml.replace("<?aggro?>", template.isAggressive() ? "TRUE" : "FALSE");
		
		newHtml = newHtml.replace("<?hp?>", Util.formatAdena((int) template.getBaseHpMax()));
		newHtml = newHtml.replace("<?mp?>", Util.formatAdena((int) template.getBaseMpMax()));
		
		newHtml = newHtml.replace("<?drop?>", item != null ? DropInfoFunctions.getDropChance(template, item.getId(), false) : "...");
		newHtml = newHtml.replace("<?spoil?>", item != null ? DropInfoFunctions.getDropChance(template, item.getId(), true) : "...");
		
		newHtml = newHtml.replace("<?droping?>", String.valueOf(DropInfoFunctions.getDropsCount(template, false)));
		newHtml = newHtml.replace("<?spoiling?>", String.valueOf(DropInfoFunctions.getDropsCount(template, true)));
		
		newHtml = newHtml.replace("<?sort?>", String.valueOf(player.getVariables().getInt("DCMonsterSort", 0)));
		newHtml = newHtml.replace("<?sort2?>", String.valueOf(player.getVariables().getInt("DCMonster2Sort", 0)));
		
		newHtml = newHtml.replace("<?image?>", "Crest.crest_" + String.valueOf(Config.SERVER_ID) + "_" + String.valueOf(monsterId));
		
		ImagesCache.getInstance().sendImageToPlayer(player, monsterId);
		return newHtml;
	}
	
	private void manageButton(Player player, int buttonId, int monsterId)
	{
		// Trying to find an alive instance of the NPC to get the objectId.
		final Npc npc = DropInfoFunctions.getAliveNpc(monsterId);
		if (npc == null)
		{
			player.sendMessage("There's no alive instances of this monster atm.");
			return;
		}
		switch (buttonId)
		{
			case 1:
				player.sendPacket(new RadarControl(2, 2, 0, 0, 0));
				break;
			case 2:// Show Drops
				final String backBypass = "_bbssearchdropMonsterDetailsByName_" + monsterId;
				NpcViewMod.sendNpcDropList(player, npc, DropType.DROP, 0, backBypass);
				break;
			case 3:// Teleport To Monster
				if (Config.ENABLE_TELEPORT_FUNCTION)
				{
					if (Config.ALLOW_TELEPORT_FROM_PEACE_ZONE_ONLY && !player.isInsideZone(ZoneId.PEACE))
					{
						player.sendMessage("Teleport is only allowed from peace zones only.");
						return;
					}
					
					Npc aliveInstance = DropInfoFunctions.getAliveNpc(monsterId);
					if ((aliveInstance != null) && !Config.RESTRICTED_TELEPORT_IDS.contains(aliveInstance.getId()))
					{
						if (!Config.ALLOW_FREE_TELEPORT && !player.destroyItemByItemId(ItemProcessType.FEE, Config.TELEPORT_PRICE[0], Config.TELEPORT_PRICE[1], player, true))
						{
							player.sendMessage("Incorrect item count.");
							return;
						}
						
						player.teleToLocation(aliveInstance.getLocation());
					}
					else
					{
						player.sendMessage("Monster isn't alive or teleport is not allowed.");
					}
				}
				else
				{
					player.sendMessage("Teleport function is disabled.");
				}
				break;
			case 4:// Show Monster on Map
				player.sendPacket(new CreatureSay(player, ChatType.PARTYROOM_COMMANDER, "Info", "Open Map to see Locations"));
				
				for (Spawn loc : SpawnTable.getInstance().getSpawns(monsterId))
				{
					player.sendPacket(new RadarControl(0, 1, loc.getX(), loc.getY(), loc.getZ()));
				}
				break;
			case 5: // Npc stats
				DropInfoFunctions.showStats(player, monsterId);
				break;
		}
	}
	
	protected void separateAndSend(String html, Player acha)
	{
		html = html.replace("\t", "");
		if (html.length() < 8180)
		{
			acha.sendPacket(new ShowBoard(html, "101"));
			acha.sendPacket(new ShowBoard(null, "102"));
			acha.sendPacket(new ShowBoard(null, "103"));
		}
		else if (html.length() < (8180 * 2))
		{
			acha.sendPacket(new ShowBoard(html.substring(0, 8180), "101"));
			acha.sendPacket(new ShowBoard(html.substring(8180, html.length()), "102"));
			acha.sendPacket(new ShowBoard(null, "103"));
		}
		else if (html.length() < (8180 * 3))
		{
			acha.sendPacket(new ShowBoard(html.substring(0, 8180), "101"));
			acha.sendPacket(new ShowBoard(html.substring(8180, 8180 * 2), "102"));
			acha.sendPacket(new ShowBoard(html.substring(8180 * 2, html.length()), "103"));
		}
	}
	
}
