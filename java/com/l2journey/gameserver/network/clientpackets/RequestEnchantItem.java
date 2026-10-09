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
package com.l2journey.gameserver.network.clientpackets;

import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.Config;
import com.l2journey.gameserver.data.xml.EnchantItemData;
import com.l2journey.gameserver.managers.PunishmentManager;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.item.enchant.EnchantResultType;
import com.l2journey.gameserver.model.item.enchant.EnchantScroll;
import com.l2journey.gameserver.model.item.enchant.EnchantSupportItem;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.skill.CommonSkill;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.EnchantResult;
import com.l2journey.gameserver.network.serverpackets.InventoryUpdate;
import com.l2journey.gameserver.network.serverpackets.MagicSkillUse;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;

public class RequestEnchantItem extends ClientPacket
{
	protected static final Logger LOGGER_ENCHANT = Logger.getLogger("enchant.items");
	
	private int _objectId;
	private int _supportId;
	

	private static void logEnchant(Supplier<String> message)
	{
		if (LOGGER_ENCHANT.isLoggable(Level.INFO))
		{
			LOGGER_ENCHANT.info(message.get());
		}
	}

	private static void logEnchantResult(String outcome, Player player, Item item, Item scroll, Item support)
	{
		logEnchant(() ->
		{
			final StringBuilder sb = new StringBuilder();
			sb.append(outcome).append(", Character:").append(player.getName()).append(" [").append(player.getObjectId()).append("] Account:").append(player.getAccountName()).append(" IP:").append(player.getIPAddress()).append(", ");
			if (item.getEnchantLevel() > 0)
			{
				sb.append('+').append(item.getEnchantLevel()).append(' ');
			}
			sb.append(item.getName()).append('(').append(item.getCount()).append(") [").append(item.getObjectId()).append("], ").append(scroll.getName()).append('(').append(scroll.getCount()).append(") [").append(scroll.getObjectId()).append(']');
			if (support != null)
			{
				sb.append(", ").append(support.getName()).append('(').append(support.getCount()).append(") [").append(support.getObjectId()).append(']');
			}
			return sb.toString();
		});
	}

	@Override
	protected void readImpl()
	{
		_objectId = readInt();
		_supportId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if ((player == null) || (_objectId == 0))
		{
			return;
		}
		
		if (!player.isOnline() || getClient().isDetached())
		{
			player.setActiveEnchantItemId(Player.ID_NONE);
			return;
		}
		
		if (player.isProcessingTransaction() || player.isInStoreMode())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_ENCHANT_WHILE_OPERATING_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP);
			player.setActiveEnchantItemId(Player.ID_NONE);
			return;
		}
		
		final Item item = player.getInventory().getItemByObjectId(_objectId);
		Item scroll = player.getInventory().getItemByObjectId(player.getActiveEnchantItemId());
		Item support = player.getInventory().getItemByObjectId(player.getActiveEnchantSupportItemId());
		if ((item == null) || (scroll == null))
		{
			player.setActiveEnchantItemId(Player.ID_NONE);
			player.sendPacket(SystemMessageId.YOU_HAVE_CANCELLED_THE_ENCHANTING_PROCESS);
			player.sendPacket(new EnchantResult(2, 0, 0));
			return;
		}
		
		// template for scroll
		final EnchantScroll scrollTemplate = EnchantItemData.getInstance().getEnchantScroll(scroll);
		
		// scroll not found in list
		if (scrollTemplate == null)
		{
			return;
		}
		
		// template for support item, if exist
		EnchantSupportItem supportTemplate = null;
		if (support != null)
		{
			if (support.getObjectId() != _supportId)
			{
				player.setActiveEnchantItemId(Player.ID_NONE);
				return;
			}
			supportTemplate = EnchantItemData.getInstance().getSupportItem(support);
		}
		
		// first validation check - also over enchant check
		if (!scrollTemplate.isValid(item, supportTemplate) || (Config.DISABLE_OVER_ENCHANTING && (item.getEnchantLevel() == scrollTemplate.getMaxEnchantLevel())))
		{
			player.sendPacket(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITIONS);
			player.setActiveEnchantItemId(Player.ID_NONE);
			player.sendPacket(new EnchantResult(2, 0, 0));
			return;
		}
		
		// fast auto-enchant cheat check
		if ((player.getActiveEnchantTimestamp() == 0) || ((System.currentTimeMillis() - player.getActiveEnchantTimestamp()) < 2000))
		{
			PunishmentManager.handleIllegalPlayerAction(player, player + " use autoenchant program ", Config.DEFAULT_PUNISH);
			player.setActiveEnchantItemId(Player.ID_NONE);
			player.sendPacket(new EnchantResult(2, 0, 0));
			return;
		}
		
		// attempting to destroy scroll
		scroll = player.getInventory().destroyItem(ItemProcessType.FEE, scroll.getObjectId(), 1, player, item);
		if (scroll == null)
		{
			player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
			PunishmentManager.handleIllegalPlayerAction(player, player + " tried to enchant with a scroll he doesn't have", Config.DEFAULT_PUNISH);
			player.setActiveEnchantItemId(Player.ID_NONE);
			player.sendPacket(new EnchantResult(2, 0, 0));
			return;
		}
		
		// attempting to destroy support if exist
		if (support != null)
		{
			support = player.getInventory().destroyItem(ItemProcessType.FEE, support.getObjectId(), 1, player, item);
			if (support == null)
			{
				player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
				PunishmentManager.handleIllegalPlayerAction(player, player + " tried to enchant with a support item he doesn't have", Config.DEFAULT_PUNISH);
				player.setActiveEnchantItemId(Player.ID_NONE);
				player.sendPacket(new EnchantResult(2, 0, 0));
				return;
			}
		}
		
		final Item loggedScroll = scroll;
		
		final InventoryUpdate iu = new InventoryUpdate();
		synchronized (item)
		{
			// last validation check
			if ((item.getOwnerId() != player.getObjectId()) || !item.isEnchantable())
			{
				player.sendPacket(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITIONS);
				player.setActiveEnchantItemId(Player.ID_NONE);
				player.sendPacket(new EnchantResult(2, 0, 0));
				return;
			}
			
			final EnchantResultType resultType = scrollTemplate.calculateSuccess(player, item, supportTemplate);
			switch (resultType)
			{
				case ERROR:
				{
					player.sendPacket(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITIONS);
					player.setActiveEnchantItemId(Player.ID_NONE);
					player.sendPacket(new EnchantResult(2, 0, 0));
					break;
				}
				case SUCCESS:
				{
					Skill enchant4Skill = null;
					final ItemTemplate it = item.getTemplate();
					// Increase enchant level only if scroll's base template has chance, some armors can success over +20 but they shouldn't have increased.
					if (scrollTemplate.getChance(player, item) > 0)
					{
						item.setEnchantLevel(item.getEnchantLevel() + 1);
						item.updateDatabase();
					}
					player.sendPacket(new EnchantResult(0, 0, 0));
					showEnchantAnimation(player, item.getEnchantLevel());
					
					if (Config.LOG_ITEM_ENCHANTS)
					{
						logEnchantResult("Success", player, item, loggedScroll, support);
					}
					
					// announce the success
					final int minEnchantAnnounce = item.isArmor() ? 6 : 7;
					final int maxEnchantAnnounce = item.isArmor() ? 0 : 15;
					if ((item.getEnchantLevel() == minEnchantAnnounce) || (item.getEnchantLevel() == maxEnchantAnnounce))
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_SUCCESSFULLY_ENCHANTED_A_S2_S3);
						sm.addString(player.getName());
						sm.addInt(item.getEnchantLevel());
						sm.addItemName(item);
						player.broadcastPacket(sm);
						
						final Skill skill = CommonSkill.FIREWORK.getSkill();
						if (skill != null)
						{
							player.broadcastPacket(new MagicSkillUse(player, player, skill.getId(), skill.getLevel(), skill.getHitTime(), skill.getReuseDelay()));
						}
					}
					
					if ((item.isArmor()) && (item.getEnchantLevel() == 4) && item.isEquipped())
					{
						enchant4Skill = it.getEnchant4Skill();
						if (enchant4Skill != null)
						{
							// add skills bestowed from +4 armor
							player.addSkill(enchant4Skill, false);
							player.sendSkillList();
						}
					}
					
					player.sendItemList(false);
					break;
				}
				case FAILURE:
				{
					showEnchantAnimation(player, 0);
					if (scrollTemplate.isSafe())
					{
						// safe enchant - remain old value
						player.sendPacket(SystemMessageId.ENCHANT_FAILED_THE_ENCHANT_LEVEL_FOR_THE_CORRESPONDING_ITEM_WILL_BE_EXACTLY_RETAINED);
						player.sendPacket(new EnchantResult(5, 0, 0));
						if (Config.LOG_ITEM_ENCHANTS)
						{
							logEnchantResult("Safe Fail", player, item, loggedScroll, support);
						}
					}
					else
					{
						// unequip item on enchant failure to avoid item skills stack
						if (item.isEquipped())
						{
							if (item.getEnchantLevel() > 0)
							{
								final SystemMessage sm = new SystemMessage(SystemMessageId.THE_EQUIPMENT_S1_S2_HAS_BEEN_REMOVED);
								sm.addInt(item.getEnchantLevel());
								sm.addItemName(item);
								player.sendPacket(sm);
							}
							else
							{
								final SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_DISARMED);
								sm.addItemName(item);
								player.sendPacket(sm);
							}
							
							for (Item itm : player.getInventory().unEquipItemInSlotAndRecord(item.getLocationSlot()))
							{
								iu.addModifiedItem(itm);
							}
							player.sendInventoryUpdate(iu);
							player.broadcastUserInfo();
						}
						
						if (scrollTemplate.isBlessed())
						{
							// blessed enchant - clear enchant value
							player.sendPacket(SystemMessageId.THE_BLESSED_ENCHANT_FAILED_THE_ENCHANT_VALUE_OF_THE_ITEM_BECAME_0);
							
							item.setEnchantLevel(0);
							item.updateDatabase();
							player.sendPacket(new EnchantResult(3, 0, 0));
							if (Config.LOG_ITEM_ENCHANTS)
							{
								logEnchantResult("Blessed Fail", player, item, loggedScroll, support);
							}
						}
						else
						{
							// enchant failed, destroy item
							if (player.getInventory().destroyItem(ItemProcessType.DESTROY, item, player, null) == null)
							{
								// unable to destroy item, cheater ?
								PunishmentManager.handleIllegalPlayerAction(player, "Unable to delete item on enchant failure from " + player + ", possible cheater !", Config.DEFAULT_PUNISH);
								player.setActiveEnchantItemId(Player.ID_NONE);
								player.sendPacket(new EnchantResult(2, 0, 0));
								if (Config.LOG_ITEM_ENCHANTS)
								{
									logEnchantResult("Unable to destroy", player, item, loggedScroll, support);
								}
								return;
							}
							
							World.getInstance().removeObject(item);
							
							final int crystalId = item.getTemplate().getCrystalItemId();
							if ((crystalId != 0) && item.getTemplate().isCrystallizable())
							{
								int count = item.getCrystalCount() - ((item.getTemplate().getCrystalCount() + 1) / 2);
								count = count < 1 ? 1 : count;
								player.getInventory().addItem(ItemProcessType.COMPENSATE, crystalId, count, player, item);
								
								final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S2_S1_S);
								sm.addItemName(crystalId);
								sm.addLong(count);
								player.sendPacket(sm);
								player.sendPacket(new EnchantResult(1, crystalId, count));
							}
							else
							{
								player.sendPacket(new EnchantResult(4, 0, 0));
							}
							
							if (Config.LOG_ITEM_ENCHANTS)
							{
								logEnchantResult("Fail", player, item, loggedScroll, support);
							}
						}
					}
					player.sendItemList(true);
					break;
				}
			}
			player.broadcastUserInfo();
			player.setActiveEnchantItemId(Player.ID_NONE);
		}
	}
	
	public static void showEnchantAnimation(Player player, int enchantLevel)
	{
		if (player.getVarB("showEnchantAnime") && (enchantLevel <= 20))
		{
			final int skillId = 23096 + enchantLevel;
			final MagicSkillUse msu = new MagicSkillUse(player, player, skillId, 1, 1, 1);
			player.broadcastPacket(msu);
		}
	}
}
