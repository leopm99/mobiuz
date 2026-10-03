/*
 * Copyright (c) 2025 L2Journey Project
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 * ---
 * 
 * Portions of this software are derived from the L2JMobius Project, 
 * shared under the MIT License. The original license terms are preserved where 
 * applicable..
 * 
 */
package custom.SellBuff;

import java.util.Collections;
import java.util.StringTokenizer;

import com.l2journey.Config;
import com.l2journey.commons.util.StringUtil;
import com.l2journey.gameserver.data.holders.SellBuffHolder;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.handler.BypassHandler;
import com.l2journey.gameserver.handler.IBypassHandler;
import com.l2journey.gameserver.handler.IVoicedCommandHandler;
import com.l2journey.gameserver.handler.VoicedCommandHandler;
import com.l2journey.gameserver.managers.SellBuffsManager;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.quest.Quest;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.util.LocationUtil;

/**
 * Sell Buffs voice command
 * @author St3eT
 */
public class SellBuff implements IVoicedCommandHandler, IBypassHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"sellbuff",
		"sellbuffs",
	};
	
	private static final String[] BYPASS_COMMANDS =
	{
		"sellbuffadd",
		"sellbuffaddskill",
		"sellbuffedit",
		"sellbuffchangeprice",
		"sellbuffremove",
		"sellbuffbuymenu",
		"sellbuffbuyskill",
		"sellbuffbuyskillPet",
		"sellbuffstart",
		"sellbuffstop",
	};
	
	private SellBuff()
	{
		if (Config.SELLBUFF_ENABLED)
		{
			BypassHandler.getInstance().registerHandler(this);
			VoicedCommandHandler.getInstance().registerHandler(this);
		}
	}
	
	@Override
	public boolean useBypass(String command, Player player, Creature target)
	{
		String cmd = "";
		final StringBuilder params = new StringBuilder();
		final StringTokenizer st = new StringTokenizer(command, " ");
		
		if (st.hasMoreTokens())
		{
			cmd = st.nextToken();
		}
		
		while (st.hasMoreTokens())
		{
			params.append(st.nextToken() + (st.hasMoreTokens() ? " " : ""));
		}
		
		if (cmd.isEmpty())
		{
			return false;
		}
		return useBypass(cmd, player, params.toString());
	}
	
	@Override
	public boolean useVoicedCommand(String command, Player player, String params)
	{
		switch (command)
		{
			case "sellbuff", "sellbuffs":
			{
				SellBuffsManager.getInstance().sendSellMenu(player);
				break;
			}
		}
		return true;
	}
	
	public boolean useBypass(String command, Player player, String params)
	{
		if (!Config.SELLBUFF_ENABLED)
		{
			return false;
		}
		
		switch (command)
		{
			case "sellbuffstart":
			{
				if (player.isSellingBuffs() || (params == null) || params.isEmpty())
				{
					return false;
				}
				else if (player.getSellingBuffs().isEmpty())
				{
					player.sendMessage("Your list of buffs is empty, please add some buffs first!");
					return false;
				}
				else
				{
					final StringBuilder title = new StringBuilder();
					title.append("BUFF SELL: ");
					final StringTokenizer st = new StringTokenizer(params, " ");
					while (st.hasMoreTokens())
					{
						title.append(st.nextToken() + " ");
					}
					
					if (title.length() > 40)
					{
						player.sendMessage("Your title cannot exceed 29 characters in length. Please try again.");
						return false;
					}
					
					SellBuffsManager.getInstance().startSellBuffs(player, title.toString());
				}
				break;
			}
			case "sellbuffstop":
			{
				if (player.isSellingBuffs())
				{
					SellBuffsManager.getInstance().stopSellBuffs(player);
				}
				break;
			}
			case "sellbuffadd":
			{
				if (!player.isSellingBuffs())
				{
					int index = 0;
					if ((params != null) && !params.isEmpty() && StringUtil.isNumeric(params))
					{
						index = Integer.parseInt(params);
					}
					
					SellBuffsManager.getInstance().sendBuffChoiceMenu(player, index);
				}
				break;
			}
			case "sellbuffedit":
			{
				if (!player.isSellingBuffs())
				{
					SellBuffsManager.getInstance().sendBuffEditMenu(player);
				}
				break;
			}
			case "sellbuffchangeprice":
			{
				if (!player.isSellingBuffs() && (params != null) && !params.isEmpty())
				{
					final StringTokenizer st = new StringTokenizer(params, " ");
					
					int skillId = -1;
					int price = -1;
					
					if (st.hasMoreTokens())
					{
						skillId = Integer.parseInt(st.nextToken());
					}
					
					if (st.hasMoreTokens())
					{
						try
						{
							price = Integer.parseInt(st.nextToken());
						}
						catch (NumberFormatException e)
						{
							player.sendMessage("Too big price! Maximum price is " + Config.SELLBUFF_MAX_PRICE);
							SellBuffsManager.getInstance().sendBuffEditMenu(player);
						}
					}
					
					if ((skillId == -1) || (price == -1))
					{
						return false;
					}
					
					final Skill skillToChange = player.getKnownSkill(skillId);
					if (skillToChange == null)
					{
						return false;
					}
					
					final SellBuffHolder holder = player.getSellingBuffs().stream().filter(h -> (h.getSkillId() == skillToChange.getId())).findFirst().orElse(null);
					if ((holder != null))
					{
						player.sendMessage("Price of " + player.getKnownSkill(holder.getSkillId()).getName() + " has been changed to " + price + "!");
						holder.setPrice(price);
						SellBuffsManager.getInstance().sendBuffEditMenu(player);
					}
				}
				break;
			}
			case "sellbuffremove":
			{
				if (!player.isSellingBuffs() && (params != null) && !params.isEmpty())
				{
					final StringTokenizer st = new StringTokenizer(params, " ");
					
					int skillId = -1;
					
					if (st.hasMoreTokens())
					{
						skillId = Integer.parseInt(st.nextToken());
					}
					
					if ((skillId == -1))
					{
						return false;
					}
					
					final Skill skillToRemove = player.getKnownSkill(skillId);
					if (skillToRemove == null)
					{
						return false;
					}
					
					final SellBuffHolder holder = player.getSellingBuffs().stream().filter(h -> (h.getSkillId() == skillToRemove.getId())).findFirst().orElse(null);
					if ((holder != null) && player.getSellingBuffs().remove(holder))
					{
						player.sendMessage("Skill " + player.getKnownSkill(holder.getSkillId()).getName() + " has been removed!");
						SellBuffsManager.getInstance().sendBuffEditMenu(player);
					}
				}
				break;
			}
			case "sellbuffaddskill":
			{
				if (!player.isSellingBuffs() && (params != null) && !params.isEmpty())
				{
					final StringTokenizer st = new StringTokenizer(params, " ");
					
					int skillId = -1;
					long price = -1;
					
					if (st.hasMoreTokens())
					{
						skillId = Integer.parseInt(st.nextToken());
					}
					
					if (st.hasMoreTokens())
					{
						try
						{
							price = Integer.parseInt(st.nextToken());
						}
						catch (NumberFormatException e)
						{
							player.sendMessage("Too big price! Maximum price is " + Config.SELLBUFF_MIN_PRICE);
							SellBuffsManager.getInstance().sendBuffEditMenu(player);
						}
					}
					
					if ((skillId == -1) || (price == -1))
					{
						return false;
					}
					
					final Skill skillToAdd = player.getKnownSkill(skillId);
					if (skillToAdd == null)
					{
						return false;
					}
					else if (price < Config.SELLBUFF_MIN_PRICE)
					{
						player.sendMessage("Too small price! Minimum price is " + Config.SELLBUFF_MIN_PRICE);
						return false;
					}
					else if (price > Config.SELLBUFF_MAX_PRICE)
					{
						player.sendMessage("Too big price! Maximum price is " + Config.SELLBUFF_MAX_PRICE);
						return false;
					}
					else if (player.getSellingBuffs().size() >= Config.SELLBUFF_MAX_BUFFS)
					{
						player.sendMessage("You already reached max count of buffs! Max buffs is: " + Config.SELLBUFF_MAX_BUFFS);
						return false;
					}
					else if (!SellBuffsManager.getInstance().isInSellList(player, skillToAdd))
					{
						player.getSellingBuffs().add(new SellBuffHolder(skillToAdd.getId(), price));
						player.sendMessage(skillToAdd.getName() + " has been added!");
						SellBuffsManager.getInstance().sendBuffChoiceMenu(player, 0);
					}
				}
				break;
			}
			case "sellbuffbuymenu":
			{
				if ((params != null) && !params.isEmpty())
				{
					final StringTokenizer st = new StringTokenizer(params, " ");
					
					int objId = -1;
					int index = 0;
					if (st.hasMoreTokens())
					{
						objId = Integer.parseInt(st.nextToken());
					}
					
					if (st.hasMoreTokens())
					{
						index = Integer.parseInt(st.nextToken());
					}
					
					final Player seller = World.getInstance().getPlayer(objId);
					if (seller != null)
					{
						if (!seller.isSellingBuffs() || !player.isInsideRadius3D(seller, Npc.INTERACTION_DISTANCE))
						{
							return false;
						}
						
						SellBuffsManager.getInstance().sendBuffMenu(player, seller, index);
					}
				}
				break;
			}
			case "sellbuffbuyskill":
			{
				if ((params != null) && !params.isEmpty())
				{
					final StringTokenizer st = new StringTokenizer(params, " ");
					int objId = -1;
					int skillId = -1;
					int index = 0;
					
					if (st.hasMoreTokens())
					{
						objId = Integer.parseInt(st.nextToken());
					}
					
					if (st.hasMoreTokens())
					{
						skillId = Integer.parseInt(st.nextToken());
					}
					
					if (st.hasMoreTokens())
					{
						index = Integer.parseInt(st.nextToken());
					}
					
					if ((skillId == -1) || (objId == -1))
					{
						return false;
					}
					
					final Player seller = World.getInstance().getPlayer(objId);
					if (seller == null)
					{
						return false;
					}
					
					final Skill skillToBuy = seller.getKnownSkill(skillId);
					if (!seller.isSellingBuffs() || !LocationUtil.checkIfInRange(Npc.INTERACTION_DISTANCE, player, seller, true) || (skillToBuy == null))
					{
						return false;
					}
					
					if (seller.getCurrentMp() < (skillToBuy.getMpConsume() * Config.SELLBUFF_MP_MULTIPLER))
					{
						player.sendMessage(seller.getName() + " has not enough mana for " + skillToBuy.getName() + "!");
						SellBuffsManager.getInstance().sendBuffMenu(player, seller, index);
						return false;
					}
					
					// Check if buff requires item to cast and seller has enough.
					final int itemConsumeId = skillToBuy.getItemConsumeId();
					final int itemConsumeCount = skillToBuy.getItemConsumeCount();
					if ((itemConsumeId > 0) && (itemConsumeCount > 0))
					{
						final long available = seller.getInventory().getInventoryItemCount(itemConsumeId, -1);
						if (available < itemConsumeCount)
						{
							final ItemTemplate requiredItem = ItemData.getInstance().getTemplate(itemConsumeId);
							final String itemName = (requiredItem != null) ? requiredItem.getName() : "required item";
							
							// Check if the seller is online before sending them the message.
							if (seller.isOnline())
							{
								seller.sendMessage(player.getName() + " tried to buy " + skillToBuy.getName() + " but you do not have enough " + itemName + "!");
							}
							player.sendMessage(seller.getName() + " doesn't have enough " + itemName + " to cast " + skillToBuy.getName() + "!");
							SellBuffsManager.getInstance().sendBuffMenu(player, seller, index);
							return false;
						}
					}
					
					final SellBuffHolder holder = seller.getSellingBuffs().stream().filter(h -> (h.getSkillId() == skillToBuy.getId())).findFirst().orElse(null);
					if (holder != null)
					{
						if (Quest.getQuestItemsCount(player, Config.SELLBUFF_PAYMENT_ID) >= holder.getPrice())
						{
							Quest.takeItems(player, Config.SELLBUFF_PAYMENT_ID, holder.getPrice());
							Quest.giveItems(seller, Config.SELLBUFF_PAYMENT_ID, holder.getPrice());
							seller.reduceCurrentMp(skillToBuy.getMpConsume() * Config.SELLBUFF_MP_MULTIPLER);
							
							// Consume item(s) required by the buff.
							if ((itemConsumeId > 0) && (itemConsumeCount > 0))
							{
								seller.destroyItemByItemId(ItemProcessType.FEE, itemConsumeId, itemConsumeCount, player, true);
							}
							
							// Cast buff.
							skillToBuy.activateSkill(seller, Collections.singletonList(player));
						}
						else
						{
							final ItemTemplate item = ItemData.getInstance().getTemplate(Config.SELLBUFF_PAYMENT_ID);
							if (item != null)
							{
								player.sendMessage("Not enough " + item.getName() + "!");
							}
							else
							{
								player.sendMessage("Not enough items!");
							}
						}
					}
					SellBuffsManager.getInstance().sendBuffMenu(player, seller, index);
				}
				break;
			}
			case "sellbuffbuyskillPet":
			{
				if ((params != null) && !params.isEmpty())
				{
					final StringTokenizer st = new StringTokenizer(params, " ");
					int objId = -1;
					int skillId = -1;
					int index = 0;
					
					if (st.hasMoreTokens())
					{
						objId = Integer.parseInt(st.nextToken());
					}
					
					if (st.hasMoreTokens())
					{
						skillId = Integer.parseInt(st.nextToken());
					}
					
					if (st.hasMoreTokens())
					{
						index = Integer.parseInt(st.nextToken());
					}
					
					if ((skillId == -1) || (objId == -1))
					{
						return false;
					}
					
					final Player seller = World.getInstance().getPlayer(objId);
					if (seller == null)
					{
						return false;
					}
					
					final Skill skillToBuy = seller.getKnownSkill(skillId);
					if (!seller.isSellingBuffs() || !LocationUtil.checkIfInRange(Npc.INTERACTION_DISTANCE, player, seller, true) || (skillToBuy == null))
					{
						return false;
					}
					
					if (seller.getCurrentMp() < (skillToBuy.getMpConsume() * Config.SELLBUFF_MP_MULTIPLER))
					{
						player.sendMessage(seller.getName() + " has not enough mana for " + skillToBuy.getName() + "!");
						SellBuffsManager.getInstance().sendBuffMenu(player, seller, index);
						return false;
					}
					
					// Check if buff requires item to cast and seller has enough.
					final int itemConsumeId = skillToBuy.getItemConsumeId();
					final int itemConsumeCount = skillToBuy.getItemConsumeCount();
					if ((itemConsumeId > 0) && (itemConsumeCount > 0))
					{
						final long available = seller.getInventory().getInventoryItemCount(itemConsumeId, -1);
						if (available < itemConsumeCount)
						{
							final ItemTemplate requiredItem = ItemData.getInstance().getTemplate(itemConsumeId);
							final String itemName = (requiredItem != null) ? requiredItem.getName() : "required item";
							
							// Check if the seller is online before sending them the message.
							if (seller.isOnline())
							{
								seller.sendMessage(player.getName() + " tried to buy " + skillToBuy.getName() + " but you do not have enough " + itemName + "!");
							}
							player.sendMessage(seller.getName() + " doesn't have enough " + itemName + " to cast " + skillToBuy.getName() + "!");
							SellBuffsManager.getInstance().sendBuffMenu(player, seller, index);
							return false;
						}
					}
					
					final SellBuffHolder holder = seller.getSellingBuffs().stream().filter(h -> (h.getSkillId() == skillToBuy.getId())).findFirst().orElse(null);
					if (holder != null)
					{
						if (Quest.getQuestItemsCount(player, Config.SELLBUFF_PAYMENT_ID) >= holder.getPrice())
						{
							if ((player.getSummon() == null) || player.getSummon().isDead())
							{
								player.sendMessage("Your pet must be summoned and alive to receive buffs.");
							}
							else
							{
								Quest.takeItems(player, Config.SELLBUFF_PAYMENT_ID, holder.getPrice());
								Quest.giveItems(seller, Config.SELLBUFF_PAYMENT_ID, holder.getPrice());
								seller.reduceCurrentMp(skillToBuy.getMpConsume() * Config.SELLBUFF_MP_MULTIPLER);
								
								// Consume item(s) required by the buff.
								if ((itemConsumeId > 0) && (itemConsumeCount > 0))
								{
									seller.destroyItemByItemId(ItemProcessType.FEE, itemConsumeId, itemConsumeCount, player, true);
								}
								
								// Cast buff.
								skillToBuy.activateSkill(seller, Collections.singletonList(player.getSummon()));
							}
						}
						else
						{
							final ItemTemplate item = ItemData.getInstance().getTemplate(Config.SELLBUFF_PAYMENT_ID);
							if (item != null)
							{
								player.sendMessage("Not enough " + item.getName() + "!");
							}
							else
							{
								player.sendMessage("Not enough items!");
							}
						}
					}
					SellBuffsManager.getInstance().sendBuffMenu(player, seller, index);
				}
				break;
			}
		}
		return true;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
	
	@Override
	public String[] getBypassList()
	{
		return BYPASS_COMMANDS;
	}
	
	public static void main(String[] args)
	{
		new SellBuff();
	}
}