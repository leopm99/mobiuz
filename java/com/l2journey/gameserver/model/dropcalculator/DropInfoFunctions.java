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
package com.l2journey.gameserver.model.dropcalculator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.l2journey.gameserver.cache.HtmCache;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.data.xml.NpcData;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.holders.npc.DropGroupHolder;
import com.l2journey.gameserver.model.actor.holders.npc.DropHolder;
import com.l2journey.gameserver.model.actor.templates.NpcTemplate;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.stats.MoveType;
import com.l2journey.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2journey.gameserver.util.Util;

/**
 * @author vGodFather
 */
public class DropInfoFunctions
{
	private static final Logger LOGGER = Logger.getLogger(DropInfoFunctions.class.getName());
	
	public static String getMinMaxDropCounts(NpcTemplate monster, int itemId, boolean drop)
	{
		long min = getDropMinMaxCounts(monster, itemId, drop, true);
		long max = getDropMinMaxCounts(monster, itemId, drop, false);
		String formattedCounts = "[" + min + "..." + max + ']';
		return formattedCounts;
	}
	
	private static long getDropMinMaxCounts(NpcTemplate template, int itemId, boolean isSpoil, boolean min)
	{
		long chance = 0;
		for (DropInfoHolder drop : DropInfoHandler.getInstance().getDrop(itemId))
		{
			if (drop.getNpcId() == template.getId())
			{
				if (drop.isSweep() && isSpoil)
				{
					if (min)
					{
						return drop.getMin();
					}
					return drop.getMax();
				}
				else if (!drop.isSweep() && !isSpoil)
				{
					if (min)
					{
						return drop.getMin();
					}
					return drop.getMax();
				}
			}
		}
		return chance;
	}
	
	public static String getDropChance(NpcTemplate template, int itemId, boolean isSpoil)
	{
		double chance = 0.0;
		for (DropInfoHolder drop : DropInfoHandler.getInstance().getDrop(itemId))
		{
			if (drop.getNpcId() == template.getId())
			{
				if (drop.isSweep() && isSpoil)
				{
					return Util.formatDouble(drop.getChance(), "#.##");
				}
				else if (!drop.isSweep() && !isSpoil)
				{
					return Util.formatDouble(drop.getChance(), "#.##");
				}
			}
		}
		return Util.formatDouble(chance, "#.##");
	}
	
	public static int getDropsCount(NpcTemplate template, boolean spoil)
	{
		return DropInfoHandler.getInstance().getDropsCount(template.getId(), spoil);
	}

	public static void showNpcDropList(Player activeChar, String dropTypeString, int npcId, int page)
	{
		final NpcTemplate npcTemplate = NpcData.getInstance().getTemplate(npcId);
		if (npcTemplate == null)
		{
			activeChar.sendMessage("Unknown NPC template ID " + npcId);
			return;
		}
		
		// Determina o tipo de drop a ser exibido
		final boolean isSpoil = !dropTypeString.equalsIgnoreCase("CORPSE") && !dropTypeString.equalsIgnoreCase("DEATH");
		
		// Coleta todos os drops relevantes em uma lista unica para paginacao
		final List<Object> allDrops = new ArrayList<>();
		if (isSpoil)
		{
			if (npcTemplate.getSpoilList() != null)
			{
				allDrops.addAll(npcTemplate.getSpoilList());
			}
		}
		else
		{
			// Para drop normal, adiciona tanto os drops de grupo quanto os individuais
			if (npcTemplate.getDropGroups() != null)
			{
				allDrops.addAll(npcTemplate.getDropGroups());
			}
			if (npcTemplate.getDropList() != null)
			{
				allDrops.addAll(npcTemplate.getDropList());
			}
		}
		
		final StringBuilder replyMSG = new StringBuilder(2900);
		replyMSG.append("<html><title>Show droplist page ");
		replyMSG.append(page);
		replyMSG.append("</title><body><br1><center><font color=\"LEVEL\">");
		replyMSG.append(npcTemplate.getName());
		replyMSG.append(" (");
		replyMSG.append(npcId);
		replyMSG.append(")<br>");
		
		if (isSpoil)
		{
			replyMSG.append("<button value=\"Show Drops\" action=\"bypass -h _bbssearchNpcDropList_DEATH_").append(npcId).append("_1\" width=280 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		}
		else
		{
			replyMSG.append("<button value=\"Show Spoils\" action=\"bypass -h _bbssearchNpcDropList_SPOIL_").append(npcId).append("_1\" width=280 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		}
		
		int myPage = 1;
		int i = 0;
		int shown = 0;
		boolean hasMore = false;
		
		if (!allDrops.isEmpty())
		{
			int category = 0; // Contador para o ID do grupo
			for (Object dropObject : allDrops)
			{
				if (shown == 10)
				{
					hasMore = true;
					break;
				}
				
				if (dropObject instanceof DropHolder) // Se for um drop individual (normal ou spoil)
				{
					final DropHolder drop = (DropHolder) dropObject;
					
					// A logica para pular paginas e a mesma do original
					if (myPage != page)
					{
						i++;
						if (i == 10)
						{
							myPage++;
							i = 0;
						}
						continue;
					}
					
					final ItemTemplate item = ItemData.getInstance().getTemplate(drop.getItemId());
					if (item == null)
					{
						continue;
					}
					if (DropInfoHandler.HERBS.contains(item.getId()))
					{
						continue;
					}
					
					replyMSG.append("<br><center><img src=\"l2ui.squaregray\" width=\"280\" height=\"2\"></center>");
					replyMSG.append("<center><table border=0 cellpadding=0 cellspacing=0 width=\"280\" height=\"40\" bgcolor=\"2E2E2E\">");
					replyMSG.append("<tr>");
					replyMSG.append("<td FIXWIDTH=32>");
					replyMSG.append("<table>");
					replyMSG.append("<tr>");
					replyMSG.append("<td width=32 align=right valign=top>");
					replyMSG.append("<table border=0 cellspacing=0 cellpadding=0 width=32 height=32 background=\"").append(item.getIcon()).append("\">");
					replyMSG.append("<tr>");
					replyMSG.append("<td width=32 height=32 align=center valign=top>");
					replyMSG.append("<button action=\"bypass -h _bbssearchdropMonstersByItem_").append(item.getId()).append("_1\" width=32 height=32 back=\"L2UI_CT1.ItemWindow_DF_Frame_Down\" fore=\"L2UI_CT1.ItemWindow_DF_Frame\">");
					replyMSG.append("</td>");
					replyMSG.append("</tr>");
					replyMSG.append("</table>");
					replyMSG.append("</td>");
					replyMSG.append("</tr>");
					replyMSG.append("</table>");
					replyMSG.append("</td>");
					replyMSG.append("<td FIXWIDTH=200 align=center valign=top>");
					replyMSG.append("<font color=\"F4FA58\" name=\"hs9\">").append(item.getName()).append("</font>");
					// L2Journey stores drop and group chances as percentages.
					final double chance = drop.getChance();
					replyMSG.append("<br1><font color=\"5882FA\">Chances ").append(isSpoil ? "Spoil" : "Drop").append(": ").append(String.format("%.2f", chance)).append("% Group: N/A ID: N/A");
					replyMSG.append("</td>");
					replyMSG.append("</tr>");
					replyMSG.append("</table>");
					replyMSG.append("</center>");
					shown++;
					
				}
				else if (dropObject instanceof DropGroupHolder) // Se for um grupo de drops
				{
					category++;
					final DropGroupHolder group = (DropGroupHolder) dropObject;
					for (DropHolder drop : group.getDropList())
					{
						if (myPage != page)
						{
							i++;
							if (i == 10)
							{
								myPage++;
								i = 0;
							}
							continue;
						}
						if (shown == 10)
						{
							hasMore = true;
							break;
						}
						
						final ItemTemplate item = ItemData.getInstance().getTemplate(drop.getItemId());
						if (item == null)
						{
							continue;
						}
						if (DropInfoHandler.HERBS.contains(item.getId()))
						{
							continue;
						}
						
						// --- CONSTRUCAO DO HTML (COPIA EXATA DO ORIGINAL) ---
						replyMSG.append("<br><center><img src=\"l2ui.squaregray\" width=\"280\" height=\"2\"></center>");
						replyMSG.append("<center><table border=0 cellpadding=0 cellspacing=0 width=\"280\" height=\"40\" bgcolor=\"2E2E2E\">");
						replyMSG.append("<tr>");
						replyMSG.append("<td FIXWIDTH=32>");
						replyMSG.append("<table>");
						replyMSG.append("<tr>");
						replyMSG.append("<td width=32 align=right valign=top>");
						replyMSG.append("<table border=0 cellspacing=0 cellpadding=0 width=32 height=32 background=\"").append(item.getIcon()).append("\">");
						replyMSG.append("<tr>");
						replyMSG.append("<td width=32 height=32 align=center valign=top>");
						replyMSG.append("<button action=\"bypass -h _bbssearchdropMonstersByItem_").append(item.getId()).append("_1\" width=32 height=32 back=\"L2UI_CT1.ItemWindow_DF_Frame_Down\" fore=\"L2UI_CT1.ItemWindow_DF_Frame\">");
						replyMSG.append("</td>");
						replyMSG.append("</tr>");
						replyMSG.append("</table>");
						replyMSG.append("</td>");
						replyMSG.append("</tr>");
						replyMSG.append("</table>");
						replyMSG.append("</td>");
						replyMSG.append("<td FIXWIDTH=200 align=center valign=top>");
						replyMSG.append("<font color=\"F4FA58\" name=\"hs9\">").append(item.getName()).append("</font>");
						final double itemChance = drop.getChance();
						final double groupChance = group.getChance();
						replyMSG.append("<br1><font color=\"5882FA\">Chances ").append(isSpoil ? "Spoil" : "Drop").append(": ").append(String.format("%.2f", itemChance)).append("% Group: ").append(String.format("%.2f", groupChance)).append("% ID: ").append(category);
						replyMSG.append("</td>");
						replyMSG.append("</tr>");
						replyMSG.append("</table>");
						replyMSG.append("</center>");
						
						shown++;
					}
				}
				
				if (hasMore)
				{
					break;
				}
			}
			
		}
		
		replyMSG.append("<br><table width=280 bgcolor=666666 border=0><tr>");
		
		if (page > 1)
		{
			replyMSG.append("<td width=120><button action=\"bypass -h _bbssearchNpcDropList_" + activeChar.getVariables().getString("DCDropType", "DEATH") + "_" + npcId + "_" + (page - 1) + "\" width=16 height=16 back=\"L2UI_CH3.shortcut_prev_down\" fore=\"L2UI_CH3.shortcut_prev\"></td>");
			
			if (!hasMore)
			{
				replyMSG.append("<td width=100 align=center>Page ");
				replyMSG.append(page);
				replyMSG.append("</td><td width=70></td></tr>");
			}
		}
		if (hasMore)
		{
			if (page <= 1)
			{
				replyMSG.append("<td width=120></td>");
			}
			
			replyMSG.append("<td width=100 align=center>Page ");
			replyMSG.append(page);
			replyMSG.append("</td>");
			
			replyMSG.append("<td width=120 align=right><button action=\"bypass -h _bbssearchNpcDropList_" + activeChar.getVariables().getString("DCDropType", "DEATH") + "_" + npcId + "_" + (page + 1) + "\" width=16 height=16 back=\"L2UI_CH3.shortcut_next_down\" fore=\"L2UI_CH3.shortcut_next\"></td></tr>");
		}
		replyMSG.append("</table>");
		
		NpcHtmlMessage adminReply = new NpcHtmlMessage();
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
	
	public static void showNpcSkillList(Player activeChar, int npcId, int page)
	{
		NpcTemplate npcData = NpcData.getInstance().getTemplate(npcId);
		if (npcData == null)
		{
			activeChar.sendMessage("Template id unknown: " + npcId);
			return;
		}
		
		Map<Integer, Skill> skills = new HashMap<>(npcData.getSkills());
		int _skillsize = skills.size();
		
		int MaxSkillsPerPage = 20;
		int MaxPages = _skillsize / MaxSkillsPerPage;
		if (_skillsize > (MaxSkillsPerPage * MaxPages))
		{
			MaxPages++;
		}
		
		if (page > MaxPages)
		{
			page = MaxPages;
		}
		
		int SkillsStart = MaxSkillsPerPage * page;
		int SkillsEnd = _skillsize;
		if ((SkillsEnd - SkillsStart) > MaxSkillsPerPage)
		{
			SkillsEnd = SkillsStart + MaxSkillsPerPage;
		}
		
		StringBuffer replyMSG = new StringBuffer("<html><title>NPC Skill List</title><body><center><font color=\"LEVEL\">");
		replyMSG.append(npcData.getName());
		replyMSG.append(" (");
		replyMSG.append(npcData.getId());
		replyMSG.append("): ");
		replyMSG.append(_skillsize);
		replyMSG.append(" skills</font></center><table width=300 bgcolor=666666><tr>");
		
		for (int x = 0; x < MaxPages; x++)
		{
			int pagenr = x + 1;
			if (page == x)
			{
				replyMSG.append("<td>Page ");
				replyMSG.append(pagenr);
				replyMSG.append("</td>");
			}
			else
			{
				replyMSG.append("<td><a action=\"bypass -h _bbssearchShowSkills_");
				replyMSG.append(npcData.getId());
				replyMSG.append("_");
				replyMSG.append(x);
				replyMSG.append("\"> Page ");
				replyMSG.append(pagenr);
				replyMSG.append(" </a></td>");
			}
		}
		
		replyMSG.append("</tr></table><br><table width=\"100%\" border=0><tr><td>Skill name [skill id-skill lvl]</td></tr>");
		Iterator<Skill> skillite = skills.values().iterator();
		
		for (int i = 0; i < SkillsStart; i++)
		{
			if (skillite.hasNext())
			{
				skillite.next();
			}
		}
		
		int cnt = SkillsStart;
		Skill sk;
		while (skillite.hasNext())
		{
			cnt++;
			if (cnt > SkillsEnd)
			{
				break;
			}
			
			sk = skillite.next();
			replyMSG.append("<tr><td width=240>");
			if (!sk.hasEffects())
			{
				replyMSG.append("<font color=\"777777\">" + sk.getName() + "</font>");
			}
			else
			{
				replyMSG.append(sk.getName());
			}
			replyMSG.append(" [");
			replyMSG.append(sk.getId());
			replyMSG.append("-");
			replyMSG.append(sk.getLevel());
			replyMSG.append("]</td></tr>");
		}
		
		NpcHtmlMessage adminReply = new NpcHtmlMessage();
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
	
	public static void showStats(Player activeChar, int npcId)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage();
		String html1 = HtmCache.getInstance().getHtm(activeChar, "data/html/CommunityBoard/dropcalculator/bbs_mobStats.html");
		
		if (html1 == null)
		{
			activeChar.sendMessage("Error: The HTML file for monster stats is missing.");
			LOGGER.warning("DropInfoFunctions: Could not find HTML file: data/html/CommunityBoard/dropcalculator/bbs_mobStats.html");
			return;
		}
		
		NpcTemplate target = NpcData.getInstance().getTemplate(npcId);
		
		if (target != null)
		{
			// Combat Stats
			html1 = html1.replace("%mobname%", (target.getName()));
			html1 = html1.replace("%patk%", Util.formatDouble(target.getBasePAtk(), "#.##"));
			html1 = html1.replace("%matk%", Util.formatDouble(target.getBaseMAtk(), "#.##"));
			html1 = html1.replace("%pdef%", Util.formatDouble(target.getBasePDef(), "#.##"));
			html1 = html1.replace("%mdef%", Util.formatDouble(target.getBaseMDef(), "#.##"));
			html1 = html1.replace("%accu%", "N/A");
			html1 = html1.replace("%evas%", "N/A");
			html1 = html1.replace("%crit%", Util.formatDouble(target.getBaseCritRate(), "#.##"));
			html1 = html1.replace("%rspd%", Util.formatDouble(target.getBaseMoveSpeed(MoveType.RUN), "#.##"));
			html1 = html1.replace("%aspd%", Util.formatDouble(target.getBasePAtkSpd(), "#.##"));
			html1 = html1.replace("%cspd%", Util.formatDouble(target.getBaseMAtkSpd(), "#.##"));
			
			// Basic Stats
			html1 = html1.replace("%str%", Util.formatDouble(target.getBaseSTR(), "#.##"));
			html1 = html1.replace("%dex%", Util.formatDouble(target.getBaseDEX(), "#.##"));
			html1 = html1.replace("%con%", Util.formatDouble(target.getBaseCON(), "#.##"));
			html1 = html1.replace("%int%", Util.formatDouble(target.getBaseINT(), "#.##"));
			html1 = html1.replace("%wit%", Util.formatDouble(target.getBaseWIT(), "#.##"));
			html1 = html1.replace("%men%", Util.formatDouble(target.getBaseMEN(), "#.##"));
			
			// Elements Stats
			html1 = html1.replace("%ele_atk%", "N/A");
			html1 = html1.replace("%ele_atk_value%", "N/A");
			html1 = html1.replace("%ele_dfire%", Util.formatDouble(target.getBaseFireRes(), "#.##"));
			html1 = html1.replace("%ele_dwater%", Util.formatDouble(target.getBaseWaterRes(), "#.##"));
			html1 = html1.replace("%ele_dwind%", Util.formatDouble(target.getBaseWindRes(), "#.##"));
			html1 = html1.replace("%ele_dearth%", Util.formatDouble(target.getBaseEarthRes(), "#.##"));
			html1 = html1.replace("%ele_dholy%", Util.formatDouble(target.getBaseHolyRes(), "#.##"));
			html1 = html1.replace("%ele_ddark%", Util.formatDouble(target.getBaseDarkRes(), "#.##"));
		}
		
		html.setHtml(html1.toString());
		activeChar.sendPacket(html);
	}
	
	public static int getDroplistsCountByItemId(int itemId, boolean isSpoil)
	{
		int dropCounts = 0;
		for (DropInfoHolder drop : DropInfoHandler.getInstance().getDrop(itemId))
		{
			if (drop.isSweep() && isSpoil)
			{
				dropCounts++;
			}
			else if (!drop.isSweep() && !isSpoil)
			{
				dropCounts++;
			}
		}
		return dropCounts;
	}
	
	public static String getName(String name)
	{
		if (name.length() > 20)
		{
			return name.substring(0, 19) + "...";
		}
		return name;
	}
	

	public static Npc getAliveNpc(int npcId)
	{
		
		return World.getInstance().getVisibleObjects().stream().filter(o -> o.isNpc() && (o.getId() == npcId)).map(o -> o.asNpc()).filter(npc -> !npc.isAlikeDead()).findFirst().orElse(null);
	}
	
	public static List<ItemTemplate> getItemsByNameContainingString(String itemName)
	{
		final List<ItemTemplate> itemsByName = new ArrayList<>();
		final String searchString = itemName.toLowerCase(java.util.Locale.ROOT);
		
		for (ItemTemplate item : ItemData.getInstance().getAllItems())
		{
			
			if ((item != null) && item.getName().toLowerCase(java.util.Locale.ROOT).contains(searchString))
			{
				
				if (!DropInfoHandler.getInstance().getDrop(item.getId()).isEmpty())
				{
					itemsByName.add(item);
				}
			}
		}
		return itemsByName;
	}
	
	public static List<NpcTemplate> getNpcsContainingString(String monsterName)
	{
		return DropInfoHandler.getInstance().searchNpcs(monsterName);
	}

	public static List<ItemTemplate> sortItems(List<ItemTemplate> itemsByName, int sort)
	{
		Collections.sort(itemsByName, new ItemComparator(sort));
		return itemsByName;
	}
	
	private static class ItemComparator implements Comparator<ItemTemplate>, Serializable
	{
		private static final long serialVersionUID = -6389059445439769861L;
		private final int sort;
		
		protected ItemComparator(int sort)
		{
			this.sort = sort;
		}
		
		@Override
		public int compare(ItemTemplate o1, ItemTemplate o2)
		{
			switch (sort)
			{
				case 0: // By name
					return o1.getName().compareTo(o2.getName());
				case 1: // By drops count
					return Integer.compare(getDroplistsCountByItemId(o2.getId(), false), getDroplistsCountByItemId(o1.getId(), false));
				case 2:// By spoil count
					return Integer.compare(getDroplistsCountByItemId(o2.getId(), true), getDroplistsCountByItemId(o1.getId(), true));
				default:
					return o1.getName().compareTo(o2.getName());
			}
		}
	}
	
	public static List<NpcTemplate> sortMonsters(List<NpcTemplate> npcTemplates, int sort)
	{
		Collections.sort(npcTemplates, new MonsterComparator(sort));
		return npcTemplates;
	}
	
	private static class MonsterComparator implements Comparator<NpcTemplate>, Serializable
	{
		private static final long serialVersionUID = 2116090903265145828L;
		private final int sort;
		
		protected MonsterComparator(int sort)
		{
			this.sort = sort;
		}
		
		@Override
		public int compare(NpcTemplate o1, NpcTemplate o2)
		{
			switch (sort)
			{
				case 0: // By name
					return o1.getName().compareTo(o2.getName());
				case 1:// By drops count
					return Integer.compare(getDropsCount(o2, false), getDropsCount(o1, false));
				case 2:// By spoil count
					return Integer.compare(getDropsCount(o2, true), getDropsCount(o1, true));
				default:
					return o1.getName().compareTo(o2.getName());
			}
		}
	}
	
	public static List<DropInfoHolder> sortMonsters2(List<DropInfoHolder> templates, int sort)
	{
		templates = new ArrayList<>(templates);
		Collections.sort(templates, new MonstersComparator(sort));
		return templates;
	}
	
	private static class MonstersComparator implements Comparator<DropInfoHolder>, Serializable
	{
		private static final long serialVersionUID = -3803552841261367731L;
		private final int sort;
		
		protected MonstersComparator(int sort)
		{
			this.sort = sort;
		}
		
		@Override
		public int compare(DropInfoHolder o1, DropInfoHolder o2)
		{
			switch (sort)
			{
				case 0: // By name
					return o1.getName().compareTo(o2.getName());
				case 1:// By level
					return Integer.compare(o2.getLevel(), o1.getLevel());
				case 2:// By chance
					return Double.compare(o2.getChance(), o1.getChance());
				case 3:// By type
					return Boolean.compare(o2.isSweep(), o1.isSweep());
				case 4:// By drop count
					return Long.compare(o2.getMin() + o2.getMax(), o1.getMin() + o1.getMax());
				default:
					return o1.getName().compareTo(o2.getName());
			}
		}
	}
}