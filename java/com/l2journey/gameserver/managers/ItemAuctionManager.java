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

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2journey.Config;
import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.model.itemauction.ItemAuctionInstance;

/**
 * @author Forsaiken
 */
public class ItemAuctionManager
{
	private static final Logger LOGGER = Logger.getLogger(ItemAuctionManager.class.getName());
	
	private final Map<Integer, ItemAuctionInstance> _managerInstances = new HashMap<>();
	private final AtomicInteger _auctionIds;
	
	protected ItemAuctionManager()
	{
		_auctionIds = new AtomicInteger(1);
		
		if (!Config.ALT_ITEM_AUCTION_ENABLED)
		{
			LOGGER.info(getClass().getSimpleName() + ": Disabled.");
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT auctionId FROM item_auction ORDER BY auctionId DESC LIMIT 0, 1"))
		{
			if (rs.next())
			{
				_auctionIds.set(rs.getInt(1) + 1);
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, e, () -> getClass().getSimpleName() + ": Failed loading auctions.");
		}
		
		final File file = new File(Config.DATAPACK_ROOT, "data/ItemAuctions.xml");
		if (!file.exists())
		{
			LOGGER.log(Level.WARNING, () -> getClass().getSimpleName() + ": Missing ItemAuctions.xml!");
			return;
		}
		
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		
		try
		{
			final Document document = factory.newDocumentBuilder().parse(file);
			for (Node na = document.getFirstChild(); na != null; na = na.getNextSibling())
			{
				if ("list".equalsIgnoreCase(na.getNodeName()))
				{
					for (Node nb = na.getFirstChild(); nb != null; nb = nb.getNextSibling())
					{
						if ("instance".equalsIgnoreCase(nb.getNodeName()))
						{
							final NamedNodeMap nab = nb.getAttributes();
							final int instanceId = Integer.parseInt(nab.getNamedItem("id").getNodeValue());
							
							if (_managerInstances.containsKey(instanceId))
							{
								throw new Exception("Dublicated instanceId " + instanceId);
							}
							
							_managerInstances.put(instanceId, new ItemAuctionInstance(instanceId, _auctionIds, nb));
						}
					}
				}
			}
			LOGGER.log(Level.INFO, () -> getClass().getSimpleName() + ": Loaded " + _managerInstances.size() + " instance(s).");
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, e, () -> getClass().getSimpleName() + ": Failed loading auctions from xml.");
		}
	}
	
	public void shutdown()
	{
		for (ItemAuctionInstance instance : _managerInstances.values())
		{
			instance.shutdown();
		}
	}
	
	public ItemAuctionInstance getManagerInstance(int instanceId)
	{
		return _managerInstances.get(instanceId);
	}
	
	public int getNextAuctionId()
	{
		return _auctionIds.getAndIncrement();
	}
	
	public static void deleteAuction(int auctionId)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM item_auction WHERE auctionId=?"))
			{
				ps.setInt(1, auctionId);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM item_auction_bid WHERE auctionId=?"))
			{
				ps.setInt(1, auctionId);
				ps.execute();
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, e, () -> "ItemAuctionManagerInstance: Failed deleting auction: " + auctionId);
		}
	}
	
	/**
	 * Gets the single instance of {@code ItemAuctionManager}.
	 * @return single instance of {@code ItemAuctionManager}
	 */
	public static ItemAuctionManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ItemAuctionManager INSTANCE = new ItemAuctionManager();
	}
}