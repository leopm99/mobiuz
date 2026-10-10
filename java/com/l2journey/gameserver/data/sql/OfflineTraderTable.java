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
package com.l2journey.gameserver.data.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.Config;
import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.data.holders.SellBuffHolder;
import com.l2journey.gameserver.managers.AntiFeedManager;
import com.l2journey.gameserver.model.ManufactureItem;
import com.l2journey.gameserver.model.TradeItem;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.Summon;
import com.l2journey.gameserver.model.actor.enums.player.PrivateStoreType;
import com.l2journey.gameserver.model.olympiad.OlympiadManager;
import com.l2journey.gameserver.model.zone.ZoneId;
import com.l2journey.gameserver.network.Disconnection;
import com.l2journey.gameserver.network.GameClient;
import com.l2journey.gameserver.network.serverpackets.LeaveWorld;
import com.l2journey.gameserver.network.serverpackets.ServerClose;

public class OfflineTraderTable
{
	private static final Logger LOGGER = Logger.getLogger(OfflineTraderTable.class.getName());
	private static final Logger LOGGER_ACCOUNTING = Logger.getLogger("accounting");
	
	// SQL DEFINITIONS
	private static final String SAVE_OFFLINE_STATUS = "INSERT INTO character_offline_trade (`charId`,`time`,`type`,`title`) VALUES (?,?,?,?)";
	private static final String SAVE_ITEMS = "INSERT INTO character_offline_trade_items (`charId`,`item`,`count`,`price`) VALUES (?,?,?,?)";
	private static final String CLEAR_OFFLINE_TABLE = "DELETE FROM character_offline_trade";
	private static final String CLEAR_OFFLINE_TABLE_PLAYER = "DELETE FROM character_offline_trade WHERE `charId`=?";
	private static final String CLEAR_OFFLINE_TABLE_ITEMS = "DELETE FROM character_offline_trade_items";
	private static final String CLEAR_OFFLINE_TABLE_ITEMS_PLAYER = "DELETE FROM character_offline_trade_items WHERE `charId`=?";
	private static final String LOAD_OFFLINE_STATUS = "SELECT `charId`, `time`, `type`, `title` FROM character_offline_trade";
	private static final String LOAD_OFFLINE_ITEMS = "SELECT `charId`, `item`, `count`, `price` FROM character_offline_trade_items WHERE `charId`=?";
	
	protected OfflineTraderTable()
	{
	}
	
	public void storeOffliners()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement stm1 = con.prepareStatement(CLEAR_OFFLINE_TABLE);
			PreparedStatement stm2 = con.prepareStatement(CLEAR_OFFLINE_TABLE_ITEMS);
			PreparedStatement stm3 = con.prepareStatement(SAVE_OFFLINE_STATUS);
			PreparedStatement stmItems = con.prepareStatement(SAVE_ITEMS))
		{
			stm1.execute();
			stm2.execute();
			for (Player pc : World.getInstance().getPlayers())
			{
				try
				{
					if (pc.isInStoreMode() && ((pc.getClient() == null) || pc.getClient().isDetached()))
					{
						stm3.setInt(1, pc.getObjectId()); // Char Id
						stm3.setLong(2, pc.getOfflineStartTime());
						stm3.setInt(3, pc.isSellingBuffs() ? PrivateStoreType.SELL_BUFFS.getId() : pc.getPrivateStoreType().getId()); // store type
						String title = null;
						
						switch (pc.getPrivateStoreType())
						{
							case BUY:
							{
								if (!Config.OFFLINE_TRADE_ENABLE)
								{
									continue;
								}
								
								title = pc.getBuyList().getTitle();
								for (TradeItem i : pc.getBuyList().getItems())
								{
									stmItems.setInt(1, pc.getObjectId());
									stmItems.setInt(2, i.getItem().getId());
									stmItems.setLong(3, i.getCount());
									stmItems.setLong(4, i.getPrice());
									stmItems.addBatch();
									stmItems.clearParameters();
								}
								stmItems.executeBatch();
								break;
							}
							case SELL, PACKAGE_SELL:
							{
								if (!Config.OFFLINE_TRADE_ENABLE)
								{
									continue;
								}
								
								title = pc.getSellList().getTitle();
								if (pc.isSellingBuffs())
								{
									stmItems.setLong(3, 0);
									for (SellBuffHolder holder : pc.getSellingBuffs())
									{
										stmItems.setInt(1, pc.getObjectId());
										stmItems.setInt(2, holder.getSkillId());
										stmItems.setLong(4, holder.getPrice());
										stmItems.addBatch();
									}
									stmItems.executeBatch();
								}
								else
								{
									for (TradeItem i : pc.getSellList().getItems())
									{
										stmItems.setInt(1, pc.getObjectId());
										stmItems.setInt(2, i.getObjectId());
										stmItems.setLong(3, i.getCount());
										stmItems.setLong(4, i.getPrice());
										stmItems.addBatch();
										stmItems.clearParameters();
									}
									stmItems.executeBatch();
								}
								break;
							}
							case MANUFACTURE:
							{
								if (!Config.OFFLINE_CRAFT_ENABLE)
								{
									continue;
								}
								
								title = pc.getStoreName();
								stmItems.setLong(3, 0);
								for (ManufactureItem i : pc.getManufactureItems().values())
								{
									stmItems.setInt(1, pc.getObjectId());
									stmItems.setInt(2, i.getRecipeId());
									stmItems.setLong(4, i.getCost());
									stmItems.addBatch();
								}
								stmItems.executeBatch();
								break;
							}
							default:
							{
								break;
							}
						}
						
						stm3.setString(4, title);
						stm3.executeUpdate();
						stm3.clearParameters();
						// No need to call con.commit() as HikariCP autocommit is true.
					}
				}
				catch (Exception e)
				{
					LOGGER.log(Level.WARNING, e, () -> getClass().getSimpleName() + ": Error while saving offline trader: " + pc.getObjectId() + " " + e);
				}
			}
			
			LOGGER.info(getClass().getSimpleName() + ": Offline traders stored.");
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e, () -> getClass().getSimpleName() + ": Error while saving offline traders: " + e);
		}
	}
	
	public void restoreOfflineTraders()
	{
		LOGGER.info(getClass().getSimpleName() + ": Loading offline traders...");
		int nTraders = 0;
		try (Connection con = DatabaseFactory.getConnection();
			Statement stm = con.createStatement();
			ResultSet rs = stm.executeQuery(LOAD_OFFLINE_STATUS);
			PreparedStatement stmItems = con.prepareStatement(LOAD_OFFLINE_ITEMS))
		{
			while (rs.next())
			{
				final long time = rs.getLong("time");
				if (Config.OFFLINE_MAX_DAYS > 0)
				{
					final Calendar cal = Calendar.getInstance();
					cal.setTimeInMillis(time);
					cal.add(Calendar.DAY_OF_YEAR, Config.OFFLINE_MAX_DAYS);
					if (cal.getTimeInMillis() <= System.currentTimeMillis())
					{
						continue;
					}
				}
				
				final int typeId = rs.getInt("type");
				boolean isSellBuff = false;
				if (typeId == PrivateStoreType.SELL_BUFFS.getId())
				{
					isSellBuff = true;
				}
				
				final PrivateStoreType type = isSellBuff ? PrivateStoreType.PACKAGE_SELL : PrivateStoreType.findById(typeId);
				if (type == null)
				{
					final String logMessage = getClass().getSimpleName() + ": PrivateStoreType with id " + rs.getInt("type") + " could not be found.";
					LOGGER.warning(() -> logMessage);
					continue;
				}
				
				if (type == PrivateStoreType.NONE)
				{
					continue;
				}
				
				Player player = null;
				
				try
				{
					player = Player.load(rs.getInt("charId"));
					player.setOnlineStatus(true, false);
					player.setOfflineStartTime(time);
					
					if (isSellBuff)
					{
						player.setSellingBuffs(true);
					}
					
					player.spawnMe(player.getX(), player.getY(), player.getZ());
					stmItems.setInt(1, player.getObjectId());
					try (ResultSet items = stmItems.executeQuery())
					{
						switch (type)
						{
							case BUY:
							{
								while (items.next())
								{
									if (player.getBuyList().addItemByItemId(items.getInt("item"), items.getLong("count"), items.getLong("price")) == null)
									{
									}
								}
								
								player.getBuyList().setTitle(rs.getString("title"));
								break;
							}
							case SELL, PACKAGE_SELL:
							{
								if (player.isSellingBuffs())
								{
									while (items.next())
									{
										player.getSellingBuffs().add(new SellBuffHolder(items.getInt("item"), items.getLong("price")));
									}
								}
								else
								{
									while (items.next())
									{
										if (player.getSellList().addItem(items.getInt("item"), items.getLong("count"), items.getLong("price")) == null)
										{
										}
									}
								}
								
								player.getSellList().setTitle(rs.getString("title"));
								player.getSellList().setPackaged(type == PrivateStoreType.PACKAGE_SELL);
								break;
							}
							case MANUFACTURE:
							{
								while (items.next())
								{
									player.getManufactureItems().put(items.getInt("item"), new ManufactureItem(items.getInt("item"), items.getLong("price")));
								}
								
								player.setStoreName(rs.getString("title"));
								break;
							}
							default:
							{
								break;
							}
						}
					}
					stmItems.clearParameters();
					
					player.sitDown();
					if (Config.OFFLINE_SET_NAME_COLOR)
					{
						player.getAppearance().setNameColor(Config.OFFLINE_NAME_COLOR);
					}
					
					player.setPrivateStoreType(type);
					player.setOnlineStatus(true, true);
					player.restoreEffects();
					if (!Config.OFFLINE_ABNORMAL_EFFECTS.isEmpty())
					{
						player.startAbnormalVisualEffect(false, Config.OFFLINE_ABNORMAL_EFFECTS.get(Rnd.get(Config.OFFLINE_ABNORMAL_EFFECTS.size())));
					}
					
					player.broadcastUserInfo();
					nTraders++;
				}
				catch (Exception e)
				{
					final Player failedPlayer = player;
					LOGGER.log(Level.WARNING, e, () -> getClass().getSimpleName() + ": Error loading trader: " + failedPlayer);
					if (player != null)
					{
						Disconnection.of(player).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
					}
				}
			}
			
			World.OFFLINE_TRADE_COUNT = nTraders;
			final int loggedNTraders = nTraders;
			LOGGER.info(() -> getClass().getSimpleName() + ": Loaded " + loggedNTraders + " offline traders.");
			
			if (!Config.STORE_OFFLINE_TRADE_IN_REALTIME)
			{
				try (Statement stm1 = con.createStatement())
				{
					stm1.execute(CLEAR_OFFLINE_TABLE);
					stm1.execute(CLEAR_OFFLINE_TABLE_ITEMS);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e, () -> getClass().getSimpleName() + ": Error while loading offline traders: ");
		}
	}
	
	public synchronized void onTransaction(Player trader, boolean finished, boolean firstCall)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement stm1 = con.prepareStatement(CLEAR_OFFLINE_TABLE_ITEMS_PLAYER);
			PreparedStatement stm2 = con.prepareStatement(CLEAR_OFFLINE_TABLE_PLAYER);
			PreparedStatement stm3 = con.prepareStatement(SAVE_ITEMS);
			PreparedStatement stm4 = con.prepareStatement(SAVE_OFFLINE_STATUS))
		{
			String title = null;
			stm1.setInt(1, trader.getObjectId()); // Char Id
			stm1.execute();
			
			// Trade is done - clear info
			if (finished)
			{
				stm2.setInt(1, trader.getObjectId()); // Char Id
				stm2.execute();
			}
			else
			{
				try
				{
					if ((trader.getClient() == null) || trader.getClient().isDetached())
					{
						switch (trader.getPrivateStoreType())
						{
							case BUY:
							{
								if (firstCall)
								{
									title = trader.getBuyList().getTitle();
								}
								
								for (TradeItem i : trader.getBuyList().getItems())
								{
									stm3.setInt(1, trader.getObjectId());
									stm3.setInt(2, i.getItem().getId());
									stm3.setLong(3, i.getCount());
									stm3.setLong(4, i.getPrice());
									stm3.addBatch();
									stm3.clearParameters();
								}
								stm3.executeBatch();
								break;
							}
							case SELL, PACKAGE_SELL:
							{
								if (firstCall)
								{
									title = trader.getSellList().getTitle();
								}
								
								if (trader.isSellingBuffs())
								{
									stm3.setLong(3, 0);
									for (SellBuffHolder holder : trader.getSellingBuffs())
									{
										stm3.setInt(1, trader.getObjectId());
										stm3.setInt(2, holder.getSkillId());
										stm3.setLong(4, holder.getPrice());
										stm3.addBatch();
									}
									stm3.executeBatch();
								}
								else
								{
									for (TradeItem i : trader.getSellList().getItems())
									{
										stm3.setInt(1, trader.getObjectId());
										stm3.setInt(2, i.getObjectId());
										stm3.setLong(3, i.getCount());
										stm3.setLong(4, i.getPrice());
										stm3.addBatch();
										stm3.clearParameters();
									}
									stm3.executeBatch();
								}
								break;
							}
							case MANUFACTURE:
							{
								if (firstCall)
								{
									title = trader.getStoreName();
								}
								
								stm3.setLong(3, 0);
								for (ManufactureItem i : trader.getManufactureItems().values())
								{
									stm3.setInt(1, trader.getObjectId());
									stm3.setInt(2, i.getRecipeId());
									stm3.setLong(4, i.getCost());
									stm3.addBatch();
								}
								stm3.executeBatch();
								break;
							}
							default:
							{
								break;
							}
						}
						
						if (firstCall)
						{
							stm4.setInt(1, trader.getObjectId()); // Char Id
							stm4.setLong(2, trader.getOfflineStartTime());
							stm4.setInt(3, trader.isSellingBuffs() ? PrivateStoreType.SELL_BUFFS.getId() : trader.getPrivateStoreType().getId()); // store type
							stm4.setString(4, title);
							stm4.executeUpdate();
							stm4.clearParameters();
						}
					}
				}
				catch (Exception e)
				{
					LOGGER.log(Level.WARNING, e, () -> getClass().getSimpleName() + ": Error while saving offline trader: " + trader.getObjectId() + " " + e);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e, () -> getClass().getSimpleName() + ": Error while saving offline traders: " + e);
		}
	}
	
	public synchronized void removeTrader(int traderObjId)
	{
		World.OFFLINE_TRADE_COUNT--;
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement stm1 = con.prepareStatement(CLEAR_OFFLINE_TABLE_ITEMS_PLAYER);
			PreparedStatement stm2 = con.prepareStatement(CLEAR_OFFLINE_TABLE_PLAYER))
		{
			stm1.setInt(1, traderObjId);
			stm1.execute();
			
			stm2.setInt(1, traderObjId);
			stm2.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e, () -> getClass().getSimpleName() + ": Error while removing offline trader: " + traderObjId + " " + e);
		}
	}
	
	/**
	 * Check whether player is able to enter offline mode.
	 * @param player the player to be check.
	 * @return {@code true} if the player is allowed to remain as off-line shop.
	 */
	private boolean offlineMode(Player player)
	{
		if ((player == null) || player.isInOlympiadMode() || player.isRegisteredOnEvent() || player.isJailed() || (player.getVehicle() != null))
		{
			return false;
		}
		
		boolean canSetShop = false;
		switch (player.getPrivateStoreType())
		{
			case SELL, PACKAGE_SELL, BUY:
			{
				canSetShop = Config.OFFLINE_TRADE_ENABLE;
				break;
			}
			case MANUFACTURE:
			{
				canSetShop = Config.OFFLINE_TRADE_ENABLE;
				break;
			}
			default:
			{
				canSetShop = Config.OFFLINE_CRAFT_ENABLE && player.isCrafting();
				break;
			}
		}
		
		if (Config.OFFLINE_MODE_IN_PEACE_ZONE && !player.isInsideZone(ZoneId.PEACE))
		{
			canSetShop = false;
		}
		
		// Check whether client is null or player is already in offline mode.
		final GameClient client = player.getClient();
		if ((client == null) || client.isDetached())
		{
			return false;
		}
		
		return canSetShop;
	}
	
	/**
	 * Manages the disconnection process of offline traders.
	 * @param player
	 * @return {@code true} when player entered offline mode, otherwise {@code false}
	 */
	public boolean enteredOfflineMode(Player player)
	{
		if (!offlineMode(player))
		{
			return false;
		}
		
		World.OFFLINE_TRADE_COUNT++;
		
		final GameClient client = player.getClient();
		client.close(ServerClose.STATIC_PACKET);
		if (!Config.DUALBOX_COUNT_OFFLINE_TRADERS)
		{
			AntiFeedManager.getInstance().onDisconnect(client);
		}
		
		client.setDetached(true);
		
		player.leaveParty();
		OlympiadManager.getInstance().unRegisterNoble(player);
		
		// If the Player has Pet, unsummon it
		Summon pet = player.getSummon();
		if (pet != null)
		{
			pet.setRestoreSummon(true);
			pet.unSummon(player);
			pet = player.getSummon();
			
			// Dead pet wasn't unsummoned, broadcast npcinfo changes (pet will be without owner name - means owner offline)
			if (pet != null)
			{
				pet.broadcastNpcInfo(0);
			}
		}
		
		if (Config.OFFLINE_SET_NAME_COLOR)
		{
			player.getAppearance().setNameColor(Config.OFFLINE_NAME_COLOR);
			player.broadcastUserInfo();
		}
		
		if (player.getOfflineStartTime() == 0)
		{
			player.setOfflineStartTime(System.currentTimeMillis());
		}
		
		// Store trade on exit, if realtime saving is enabled.
		if (Config.STORE_OFFLINE_TRADE_IN_REALTIME)
		{
			onTransaction(player, false, true);
		}
		
		player.storeMe();
		LOGGER_ACCOUNTING.info(() -> "Entering offline mode, " + client);
		
		if (!Config.OFFLINE_ABNORMAL_EFFECTS.isEmpty())
		{
			player.startAbnormalVisualEffect(true, Config.OFFLINE_ABNORMAL_EFFECTS.get(Rnd.get(Config.OFFLINE_ABNORMAL_EFFECTS.size())));
		}
		
		return true;
	}
	
	/**
	 * Gets the single instance of OfflineTradersTable.
	 * @return single instance of OfflineTradersTable
	 */
	public static OfflineTraderTable getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final OfflineTraderTable INSTANCE = new OfflineTraderTable();
	}
}
