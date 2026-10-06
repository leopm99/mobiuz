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
package handlers.communityboard;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import com.l2journey.Config;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.cache.HtmCache;
import com.l2journey.gameserver.data.SpawnTable;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.data.xml.NpcData;
import com.l2journey.gameserver.handler.CommunityBoardHandler;
import com.l2journey.gameserver.handler.IParseBoardHandler;
import com.l2journey.gameserver.model.Spawn;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.npc.DropType;
import com.l2journey.gameserver.model.actor.holders.npc.DropGroupHolder;
import com.l2journey.gameserver.model.actor.holders.npc.DropHolder;
import com.l2journey.gameserver.model.actor.stat.PlayerStat;
import com.l2journey.gameserver.model.actor.templates.NpcTemplate;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.itemcontainer.Inventory;
import com.l2journey.gameserver.network.serverpackets.RadarControl;
import com.l2journey.gameserver.network.serverpackets.ShowMiniMap;

/**
 * @author yksdtc
 */
public class DropSearchBoard implements IParseBoardHandler
{
	private static final String[] COMMAND =
	{
		"_bbs_search_item",
		"_bbs_search_drop",
		"_bbs_npc_trace"
	};
	
	private class CBDropHolder
	{
		final int itemId;
		final int npcId;
		final byte npcLevel;
		final long min;
		final long max;
		final double chance;
		final boolean isSpoil;
		final boolean isRaid;
		
		public CBDropHolder(NpcTemplate npcTemplate, DropHolder dropHolder)
		{
			isSpoil = dropHolder.getDropType() == DropType.SPOIL;
			itemId = dropHolder.getItemId();
			npcId = npcTemplate.getId();
			npcLevel = npcTemplate.getLevel();
			min = dropHolder.getMin();
			max = dropHolder.getMax();
			chance = dropHolder.getChance();
			isRaid = npcTemplate.getType().equals("RaidBoss") || npcTemplate.getType().equals("GrandBoss");
		}
		
		/**
		 * only for debug
		 */
		@Override
		public String toString()
		{
			return "DropHolder [itemId=" + itemId + ", npcId=" + npcId + ", npcLevel=" + npcLevel + ", min=" + min + ", max=" + max + ", chance=" + chance + ", isSpoil=" + isSpoil + "]";
		}
	}
	
	private final Map<Integer, List<CBDropHolder>> DROP_INDEX_CACHE = new HashMap<>();
	
	// nonsupport items
	private static final Set<Integer> BLOCK_ID = new HashSet<>();
	static
	{
		BLOCK_ID.add(Inventory.ADENA_ID);
	}
	
	public DropSearchBoard()
	{
		buildDropIndex();
	}
	
	private void buildDropIndex()
	{
		NpcData.getInstance().getTemplates(npc -> npc.getDropGroups() != null).forEach(npcTemplate ->
		{
			for (DropGroupHolder dropGroup : npcTemplate.getDropGroups())
			{
				final double chance = dropGroup.getChance() / 100;
				for (DropHolder dropHolder : dropGroup.getDropList())
				{
					addToDropList(npcTemplate, new DropHolder(dropHolder.getDropType(), dropHolder.getItemId(), dropHolder.getMin(), dropHolder.getMax(), dropHolder.getChance() * chance));
				}
			}
		});
		NpcData.getInstance().getTemplates(npc -> npc.getDropList() != null).forEach(npcTemplate ->
		{
			for (DropHolder dropHolder : npcTemplate.getDropList())
			{
				addToDropList(npcTemplate, dropHolder);
			}
		});
		NpcData.getInstance().getTemplates(npc -> npc.getSpoilList() != null).forEach(npcTemplate ->
		{
			for (DropHolder dropHolder : npcTemplate.getSpoilList())
			{
				addToDropList(npcTemplate, dropHolder);
			}
		});
		
		DROP_INDEX_CACHE.values().forEach(l -> l.sort((d1, d2) -> Byte.compare(d1.npcLevel, d2.npcLevel)));
	}
	
	private void addToDropList(NpcTemplate npcTemplate, DropHolder dropHolder)
	{
		if (BLOCK_ID.contains(dropHolder.getItemId()))
		{
			return;
		}
		
		List<CBDropHolder> dropList = DROP_INDEX_CACHE.get(dropHolder.getItemId());
		if (dropList == null)
		{
			dropList = new ArrayList<>();
			DROP_INDEX_CACHE.put(dropHolder.getItemId(), dropList);
		}
		
		dropList.add(new CBDropHolder(npcTemplate, dropHolder));
	}
	
	@Override
	public boolean parseCommunityBoardCommand(String command, Player player)
	{
		final String[] params = command.split(" ");
		String html = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/dropsearch/main.html");
		if (html == null)
		{
			return true;
		}
		switch (params[0])
		{
			case "_bbs_search_item":
			{
				String itemName = buildItemName(params);
				if ((itemName == null) || itemName.trim().isEmpty())
				{
					itemName = "Adena"; // Item padrão
				}
				
				final String result = buildItemSearchResult(itemName);
				html = html.replace("%searchResult%", result);
				break;
			}
			case "_bbs_search_drop":
			{
				final DecimalFormat chanceFormat = new DecimalFormat("0.###");
				final int itemId = Integer.parseInt(params[1]);
				int page = Integer.parseInt(params[2]);
				final List<CBDropHolder> list = DROP_INDEX_CACHE.get(itemId);
				final int perPage = 10;
				int pages = ((list.size() + perPage) - 1) / perPage;
				if (pages == 0)
				{
					pages++;
				}
				
				final int start = (page - 1) * perPage;
				final int end = Math.min(list.size(), start + perPage);
				final StringBuilder builder = new StringBuilder();
				
				// Table header.
				builder.append("<tr>");
				builder.append("<td width=30 align=CENTER><font color=\"LEVEL\">Lvl</font></td>");
				builder.append("<td width=160><font color=\"LEVEL\">Monster</font></td>");
				builder.append("<td width=90 align=CENTER><font color=\"LEVEL\">Amount</font></td>");
				builder.append("<td width=70 align=CENTER><font color=\"LEVEL\">Chance</font></td>");
				builder.append("<td width=60 align=CENTER><font color=\"LEVEL\">Method</font></td>");
				builder.append("<td width=70 align=CENTER><font color=\"LEVEL\">Type</font></td>");
				builder.append("</tr>");
				
				final PlayerStat stat = player.getStat();
				final double dropAmountAdenaEffectBonus = stat.getBonusDropAdenaMultiplier();
				final double dropAmountEffectBonus = stat.getBonusDropAmountMultiplier();
				final double dropRateEffectBonus = stat.getBonusDropRateMultiplier();
				final double spoilRateEffectBonus = stat.getBonusSpoilRateMultiplier();
				for (int index = start; index < end; index++)
				{
					final CBDropHolder cbDropHolder = list.get(index);
					
					// real time server rate calculations
					double rateChance = 1;
					double rateAmount = 1;
					if (cbDropHolder.isSpoil)
					{
						rateChance = Config.RATE_SPOIL_DROP_CHANCE_MULTIPLIER;
						rateAmount = Config.RATE_SPOIL_DROP_AMOUNT_MULTIPLIER;
						
						// also check premium rates if available
						if (Config.PREMIUM_SYSTEM_ENABLED && player.hasPremiumStatus())
						{
							rateChance *= Config.PREMIUM_RATE_SPOIL_CHANCE;
							rateAmount *= Config.PREMIUM_RATE_SPOIL_AMOUNT;
						}
						
						// bonus spoil rate effect
						rateChance *= spoilRateEffectBonus;
					}
					else
					{
						final ItemTemplate item = ItemData.getInstance().getTemplate(cbDropHolder.itemId);
						if (Config.RATE_DROP_CHANCE_BY_ID.get(cbDropHolder.itemId) != null)
						{
							rateChance *= Config.RATE_DROP_CHANCE_BY_ID.get(cbDropHolder.itemId);
							if ((cbDropHolder.itemId == Inventory.ADENA_ID) && (rateChance > 100))
							{
								rateChance = 100;
							}
						}
						else if (item.hasExImmediateEffect())
						{
							rateChance *= Config.RATE_HERB_DROP_CHANCE_MULTIPLIER;
						}
						else if (cbDropHolder.isRaid)
						{
							rateChance *= Config.RATE_RAID_DROP_CHANCE_MULTIPLIER;
						}
						else
						{
							rateChance *= Config.RATE_DEATH_DROP_CHANCE_MULTIPLIER;
						}
						
						if (Config.RATE_DROP_AMOUNT_BY_ID.get(cbDropHolder.itemId) != null)
						{
							rateAmount *= Config.RATE_DROP_AMOUNT_BY_ID.get(cbDropHolder.itemId);
						}
						else if (item.hasExImmediateEffect())
						{
							rateAmount *= Config.RATE_HERB_DROP_AMOUNT_MULTIPLIER;
						}
						else if (cbDropHolder.isRaid)
						{
							rateAmount *= Config.RATE_RAID_DROP_AMOUNT_MULTIPLIER;
						}
						else
						{
							rateAmount *= Config.RATE_DEATH_DROP_AMOUNT_MULTIPLIER;
						}
						
						// also check premium rates if available
						if (Config.PREMIUM_SYSTEM_ENABLED && player.hasPremiumStatus())
						{
							if (Config.PREMIUM_RATE_DROP_CHANCE_BY_ID.get(cbDropHolder.itemId) != null)
							{
								rateChance *= Config.PREMIUM_RATE_DROP_CHANCE_BY_ID.get(cbDropHolder.itemId);
							}
							else if (item.hasExImmediateEffect())
							{
								// TODO: Premium herb chance? :)
							}
							else if (cbDropHolder.isRaid)
							{
								// TODO: Premium raid chance? :)
							}
							else
							{
								rateChance *= Config.PREMIUM_RATE_DROP_CHANCE;
							}
							
							if (Config.PREMIUM_RATE_DROP_AMOUNT_BY_ID.get(cbDropHolder.itemId) != null)
							{
								rateAmount *= Config.PREMIUM_RATE_DROP_AMOUNT_BY_ID.get(cbDropHolder.itemId);
							}
							else if (item.hasExImmediateEffect())
							{
								// TODO: Premium herb amount? :)
							}
							else if (cbDropHolder.isRaid)
							{
								// TODO: Premium raid amount? :)
							}
							else
							{
								rateAmount *= Config.PREMIUM_RATE_DROP_AMOUNT;
							}
						}
						
						// bonus drop amount effect
						rateAmount *= dropAmountEffectBonus;
						if (item.getId() == Inventory.ADENA_ID)
						{
							rateAmount *= dropAmountAdenaEffectBonus;
						}
						// bonus drop rate effect
						rateChance *= dropRateEffectBonus;
					}
					
					// Format min-max amounts (round to integer; if equal, show single value).
					final long minAmount = Math.round(cbDropHolder.min * rateAmount);
					final long maxAmount = Math.round(cbDropHolder.max * rateAmount);
					final String amountStr = (minAmount == maxAmount) ? String.valueOf(minAmount) : (minAmount + "-" + maxAmount);
					
					// Format chance: ceiling rounding, max 3 decimal places.
					final double chanceValue = cbDropHolder.chance * rateChance;
					final double chanceRounded = Math.ceil(chanceValue * 1000.0) / 1000.0;
					final String chanceStr = chanceFormat.format(chanceRounded);
					
					// Type column: Raid (highlighted) vs Monster.
					final String typeStr = cbDropHolder.isRaid ? "<font color=\"FF6347\">Raid</font>" : "Monster";
					final String dropTypeStr = cbDropHolder.isSpoil ? "<font color=\"6FA8DC\">Spoil</font>" : "<font color=\"B6D7A8\">Drop</font>";
					
					builder.append("<tr>");
					builder.append("<td width=30 align=CENTER>").append(cbDropHolder.npcLevel).append("</td>");
					builder.append("<td width=160>").append("<a action=\"bypass _bbs_npc_trace " + cbDropHolder.npcId + "\">").append("&@").append(cbDropHolder.npcId).append(";").append("</a>").append("</td>");
					builder.append("<td width=90 align=CENTER>").append(amountStr).append("</td>");
					builder.append("<td width=70 align=CENTER>").append(chanceStr).append("%</td>");
					builder.append("<td width=60 align=CENTER>").append(dropTypeStr).append("</td>");
					builder.append("<td width=70 align=CENTER>").append(typeStr).append("</td>");
					builder.append("</tr>");
				}
				
				html = html.replace("%searchResult%", builder.toString());
				builder.setLength(0);
				
				builder.append("<tr>");
				for (page = 1; page <= pages; page++)
				{
					builder.append("<td>").append("<a action=\"bypass -h _bbs_search_drop " + itemId + " " + page + " $order $level\">").append(page).append("</a>").append("</td>");
				}
				builder.append("</tr>");
				html = html.replace("%pages%", builder.toString());
				break;
			}
			case "_bbs_npc_trace":
			{
				final int npcId = Integer.parseInt(params[1]);
				final Spawn spawn = SpawnTable.getInstance().getAnySpawn(npcId);
				if (spawn == null)
				{
					player.sendMessage("Cannot find any spawn. Maybe dropped by a boss or instance monster.");
				}
				else
				{
					player.sendPacket(new ShowMiniMap(-1));
					ThreadPool.schedule(() ->
					{
						player.getRadar().addMarker(spawn.getX(), spawn.getY(), spawn.getZ());
						player.sendPacket(new RadarControl(0, 2, spawn.getX(), spawn.getY(), spawn.getZ()));
					}, 500);
				}
				break;
			}
		}
		
		if (html != null)
		{
			CommunityBoardHandler.separateAndSend(html, player);
		}
		
		return false;
	}
	
	/**
	 * @param itemName
	 * @return
	 */
	private String buildItemSearchResult(String itemName)
	{
		int limit = 0;
		final Set<Integer> existInDropData = DROP_INDEX_CACHE.keySet();
		final List<ItemTemplate> items = new ArrayList<>();
		for (ItemTemplate item : ItemData.getInstance().getAllItems())
		{
			if ((item == null) || !existInDropData.contains(item.getId()))
			{
				continue;
			}
			
			if (item.getName().toLowerCase().contains(itemName.toLowerCase()))
			{
				items.add(item);
				limit++;
			}
			
			if (limit == 18)
			{
				break;
			}
		}
		
		if (items.isEmpty())
		{
			return "<tr><td width=100 align=CENTER>No Match</td></tr>";
		}
		
		int line = 0;
		
		final StringBuilder builder = new StringBuilder(items.size() * 28);
		int i = 0;
		for (ItemTemplate item : items)
		{
			i++;
			if (i == 1)
			{
				line++;
				builder.append("<tr>");
			}
			
			String icon = item.getIcon();
			if (icon == null)
			{
				icon = "icon.etc_question_mark_i00";
			}
			
			builder.append("<td width=30 background=\"" + icon + "\">");
			builder.append("<button value=\" \" action=\"bypass _bbs_search_drop " + item.getId() + " 1 $order $level\" width=30 height=30 back=\"\" fore=\"\">");
			builder.append("</td>");
			builder.append("<td width=200>");
			builder.append("&#").append(item.getId()).append(";");
			builder.append("</td>");
			
			if (i == 2)
			{
				builder.append("</tr>");
				i = 0;
			}
		}
		
		if ((i % 2) == 1)
		{
			builder.append("</tr>");
		}
		
		if (line < 9)
		{
			for (i = 0; i < (9 - line); i++)
			{
				builder.append("<tr><td height=36></td></tr>");
			}
		}
		
		return builder.toString();
	}
	
	/**
	 * @param params
	 * @return
	 */
	private String buildItemName(String[] params)
	{
		final StringJoiner joiner = new StringJoiner(" ");
		for (int i = 1; i < params.length; i++)
		{
			joiner.add(params[i]);
		}
		return joiner.toString();
	}
	
	@Override
	public String[] getCommunityBoardCommands()
	{
		return COMMAND;
	}
}
