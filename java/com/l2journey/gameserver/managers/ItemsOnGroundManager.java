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
package com.l2journey.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.Config;
import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.taskmanagers.ItemsAutoDestroyTaskManager;

/**
 * This class manage all items on ground.
 * @author Enforcer
 */
public class ItemsOnGroundManager implements Runnable
{
	private static final Logger LOGGER = Logger.getLogger(ItemsOnGroundManager.class.getName());
	
	private final Collection<Item> _items = ConcurrentHashMap.newKeySet();
	
	protected ItemsOnGroundManager()
	{
		if (Config.SAVE_DROPPED_ITEM_INTERVAL > 0)
		{
			ThreadPool.scheduleAtFixedRate(this, Config.SAVE_DROPPED_ITEM_INTERVAL, Config.SAVE_DROPPED_ITEM_INTERVAL);
		}
		load();
	}
	
	private void load()
	{
		// If SaveDroppedItem is false, may want to delete all items previously stored to avoid add old items on reactivate
		if (!Config.SAVE_DROPPED_ITEM && Config.CLEAR_DROPPED_ITEM_TABLE)
		{
			emptyTable();
		}
		
		if (!Config.SAVE_DROPPED_ITEM)
		{
			return;
		}
		
		// if DestroyPlayerDroppedItem was previously false, items currently protected will be added to ItemsAutoDestroy
		if (Config.DESTROY_DROPPED_PLAYER_ITEM)
		{
			String str = null;
			if (!Config.DESTROY_EQUIPABLE_PLAYER_ITEM)
			{
				// Recycle misc. items only
				str = "UPDATE itemsonground SET drop_time = ? WHERE drop_time = -1 AND equipable = 0";
			}
			else if (Config.DESTROY_EQUIPABLE_PLAYER_ITEM)
			{
				// Recycle all items including equip-able
				str = "UPDATE itemsonground SET drop_time = ? WHERE drop_time = -1";
			}
			
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement(str))
			{
				ps.setLong(1, System.currentTimeMillis());
				ps.execute();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, e, () -> getClass().getSimpleName() + ": Error while updating table ItemsOnGround " + e.getMessage());
			}
		}
		
		// Add items to world
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT object_id,item_id,count,enchant_level,x,y,z,drop_time,equipable FROM itemsonground"))
		{
			int count = 0;
			try (ResultSet rs = ps.executeQuery())
			{
				Item item;
				while (rs.next())
				{
					item = new Item(rs.getInt(1), rs.getInt(2));
					World.getInstance().addObject(item);
					// this check and..
					if (item.isStackable() && (rs.getInt(3) > 1))
					{
						item.setCount(rs.getInt(3));
					}
					// this, are really necessary?
					if (rs.getInt(4) > 0)
					{
						item.setEnchantLevel(rs.getInt(4));
					}
					item.setXYZ(rs.getInt(5), rs.getInt(6), rs.getInt(7));
					item.setWorldRegion(World.getInstance().getRegion(item));
					item.getWorldRegion().addVisibleObject(item);
					final long dropTime = rs.getLong(8);
					item.setDropTime(dropTime);
					item.setProtected(dropTime == -1);
					item.setSpawned(true);
					World.getInstance().addVisibleObject(item, item.getWorldRegion());
					_items.add(item);
					count++;
					// add to ItemsAutoDestroy only items not protected
					if (!Config.LIST_PROTECTED_ITEMS.contains(item.getId()) && (dropTime > -1) && (((Config.AUTODESTROY_ITEM_AFTER > 0) && !item.getTemplate().hasExImmediateEffect()) || ((Config.HERB_AUTO_DESTROY_TIME > 0) && item.getTemplate().hasExImmediateEffect())))
					{
						ItemsAutoDestroyTaskManager.getInstance().addItem(item);
					}
				}
			}
			final int loggedCount = count;
			LOGGER.info(() -> getClass().getSimpleName() + ": Loaded " + loggedCount + " items.");
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, e, () -> getClass().getSimpleName() + ": Error while loading ItemsOnGround " + e.getMessage());
		}
		
		if (Config.EMPTY_DROPPED_ITEM_TABLE_AFTER_LOAD)
		{
			emptyTable();
		}
	}
	
	public void save(Item item)
	{
		if (!Config.SAVE_DROPPED_ITEM)
		{
			return;
		}
		_items.add(item);
	}
	
	public void removeObject(Item item)
	{
		if (Config.SAVE_DROPPED_ITEM)
		{
			_items.remove(item);
		}
	}
	
	public void saveInDb()
	{
		run();
	}
	
	public void cleanUp()
	{
		_items.clear();
	}
	
	public void emptyTable()
	{
		try (Connection con = DatabaseFactory.getConnection();
			Statement s = con.createStatement())
		{
			s.executeUpdate("DELETE FROM itemsonground");
		}
		catch (Exception e1)
		{
			LOGGER.log(Level.SEVERE, e1, () -> getClass().getSimpleName() + ": Error while cleaning table ItemsOnGround " + e1.getMessage());
		}
	}
	
	@Override
	public synchronized void run()
	{
		if (!Config.SAVE_DROPPED_ITEM)
		{
			return;
		}
		
		emptyTable();
		
		if (_items.isEmpty())
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO itemsonground(object_id,item_id,count,enchant_level,x,y,z,drop_time,equipable) VALUES(?,?,?,?,?,?,?,?,?)"))
		{
			for (Item item : _items)
			{
				if ((item == null) || CursedWeaponsManager.getInstance().isCursed(item.getId()))
				{
					continue; // Cursed Items not saved to ground, prevent double save
				}
				
				try
				{
					ps.setInt(1, item.getObjectId());
					ps.setInt(2, item.getId());
					ps.setLong(3, item.getCount());
					ps.setInt(4, item.getEnchantLevel());
					ps.setInt(5, item.getX());
					ps.setInt(6, item.getY());
					ps.setInt(7, item.getZ());
					ps.setLong(8, item.isProtected() ? -1 : item.getDropTime()); // item is protected or AutoDestroyed
					ps.setLong(9, item.isEquipable() ? 1 : 0); // set equip-able
					ps.execute();
					ps.clearParameters();
				}
				catch (Exception e)
				{
					LOGGER.log(Level.SEVERE, e, () -> getClass().getSimpleName() + ": Error while inserting into table ItemsOnGround: " + e.getMessage());
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, e, () -> getClass().getSimpleName() + ": SQL error while storing items on ground: " + e.getMessage());
		}
	}
	
	/**
	 * Gets the single instance of {@code ItemsOnGroundManager}.
	 * @return single instance of {@code ItemsOnGroundManager}
	 */
	public static ItemsOnGroundManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ItemsOnGroundManager INSTANCE = new ItemsOnGroundManager();
	}
}
