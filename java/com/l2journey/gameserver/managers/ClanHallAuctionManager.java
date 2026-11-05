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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.model.residences.ClanHallAuction;

public class ClanHallAuctionManager
{
	protected static final Logger LOGGER = Logger.getLogger(ClanHallAuctionManager.class.getName());
	private final List<ClanHallAuction> _auctions = new ArrayList<>();
	
	private static final int SELLER_ID = 0;
	private static final String SELLER_NAME = "NPC";
	private static final String SELLER_CLAN_NAME = "NPC Clan";
	private static final String ITEM_TYPE = "ClanHall";
	private static final int ITEM_OBJECT_ID = 0;
	private static final long ITEM_QUANTITY = 1;
	private static final long CURRENT_BID = 0;
	
	private static final String INSERT_AUCTION = "INSERT INTO auction (id, sellerId, sellerName, sellerClanName, itemType, itemId, itemObjectId, itemName, itemQuantity, startingBid, currentBid, endDate) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
	
	private static final AuctionSeed[] AUCTION_SEEDS =
	{
		new AuctionSeed(22, "Moonstone Hall", 20000000L, 1073037600000L),
		new AuctionSeed(23, "Onyx Hall", 20000000L, 1073037600000L),
		new AuctionSeed(24, "Topaz Hall", 20000000L, 1073037600000L),
		new AuctionSeed(25, "Ruby Hall", 20000000L, 1073037600000L),
		new AuctionSeed(26, "Crystal Hall", 20000000L, 1073037600000L),
		new AuctionSeed(27, "Onyx Hall", 20000000L, 1073037600000L),
		new AuctionSeed(28, "Sapphire Hall", 20000000L, 1073037600000L),
		new AuctionSeed(29, "Moonstone Hall", 20000000L, 1073037600000L),
		new AuctionSeed(30, "Emerald Hall", 20000000L, 1073037600000L),
		new AuctionSeed(31, "Atramental Barracks", 8000000L, 1073037600000L),
		new AuctionSeed(32, "Scarlet Barracks", 8000000L, 1073037600000L),
		new AuctionSeed(33, "Viridian Barracks", 8000000L, 1073037600000L),
		new AuctionSeed(36, "Golden Chamber", 50000000L, 1106827200000L),
		new AuctionSeed(37, "Silver Chamber", 50000000L, 1106827200000L),
		new AuctionSeed(38, "Mithril Chamber", 50000000L, 1106827200000L),
		new AuctionSeed(39, "Silver Manor", 50000000L, 1106827200000L),
		new AuctionSeed(40, "Gold Manor", 50000000L, 1106827200000L),
		new AuctionSeed(41, "Bronze Chamber", 50000000L, 1106827200000L),
		new AuctionSeed(42, "Golden Chamber", 50000000L, 1106827200000L),
		new AuctionSeed(43, "Silver Chamber", 50000000L, 1106827200000L),
		new AuctionSeed(44, "Mithril Chamber", 50000000L, 1106827200000L),
		new AuctionSeed(45, "Bronze Chamber", 50000000L, 1106827200000L),
		new AuctionSeed(46, "Silver Manor", 50000000L, 1106827200000L),
		new AuctionSeed(47, "Moonstone Hall", 50000000L, 1106827200000L),
		new AuctionSeed(48, "Onyx Hall", 50000000L, 1106827200000L),
		new AuctionSeed(49, "Emerald Hall", 50000000L, 1106827200000L),
		new AuctionSeed(50, "Sapphire Hall", 50000000L, 1106827200000L),
		new AuctionSeed(51, "Mont Chamber", 50000000L, 1106827200000L),
		new AuctionSeed(52, "Astaire Chamber", 50000000L, 1106827200000L),
		new AuctionSeed(53, "Aria Chamber", 50000000L, 1106827200000L),
		new AuctionSeed(54, "Yiana Chamber", 50000000L, 1106827200000L),
		new AuctionSeed(55, "Roien Chamber", 50000000L, 1106827200000L),
		new AuctionSeed(56, "Luna Chamber", 50000000L, 1106827200000L),
		new AuctionSeed(57, "Traban Chamber", 50000000L, 1106827200000L),
		new AuctionSeed(58, "Eisen Hall", 20000000L, 1106827200000L),
		new AuctionSeed(59, "Heavy Metal Hall", 20000000L, 1106827200000L),
		new AuctionSeed(60, "Molten Ore Hall", 20000000L, 1106827200000L),
		new AuctionSeed(61, "Titan Hall", 20000000L, 1106827200000L)
	};
	
	private static final class AuctionSeed
	{
		private final int _id;
		private final String _itemName;
		private final long _startingBid;
		private final long _endDate;
		
		private AuctionSeed(int id, String itemName, long startingBid, long endDate)
		{
			_id = id;
			_itemName = itemName;
			_startingBid = startingBid;
			_endDate = endDate;
		}
	}
	
	protected ClanHallAuctionManager()
	{
		load();
	}
	
	public void reload()
	{
		_auctions.clear();
		load();
	}
	
	private void load()
	{
		try (Connection con = DatabaseFactory.getConnection();
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT id FROM auction ORDER BY id"))
		{
			while (rs.next())
			{
				_auctions.add(new ClanHallAuction(rs.getInt("id")));
			}
			LOGGER.info(() -> getClass().getSimpleName() + ": Loaded " + _auctions.size() + " auction(s)");
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e, () -> getClass().getSimpleName() + ": Exception: AuctionManager.load(): " + e.getMessage());
		}
	}
	
	public ClanHallAuction getAuction(int auctionId)
	{
		final int index = getAuctionIndex(auctionId);
		if (index >= 0)
		{
			return _auctions.get(index);
		}
		return null;
	}
	
	public int getAuctionIndex(int auctionId)
	{
		ClanHallAuction auction;
		for (int i = 0; i < _auctions.size(); i++)
		{
			auction = _auctions.get(i);
			if ((auction != null) && (auction.getId() == auctionId))
			{
				return i;
			}
		}
		return -1;
	}
	
	public List<ClanHallAuction> getAuctions()
	{
		return _auctions;
	}
	
	/**
	 * Init Clan NPC aution
	 * @param id
	 */
	public void initNPC(int id)
	{
		AuctionSeed seed = null;
		for (AuctionSeed candidate : AUCTION_SEEDS)
		{
			if (candidate._id == id)
			{
				seed = candidate;
				break;
			}
		}
		if (seed == null)
		{
			LOGGER.warning(() -> getClass().getSimpleName() + ": Clan Hall auction not found for Id :" + id);
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(INSERT_AUCTION))
		{
			ps.setInt(1, seed._id);
			ps.setInt(2, SELLER_ID);
			ps.setString(3, SELLER_NAME);
			ps.setString(4, SELLER_CLAN_NAME);
			ps.setString(5, ITEM_TYPE);
			ps.setInt(6, seed._id);
			ps.setInt(7, ITEM_OBJECT_ID);
			ps.setString(8, seed._itemName);
			ps.setLong(9, ITEM_QUANTITY);
			ps.setLong(10, seed._startingBid);
			ps.setLong(11, CURRENT_BID);
			ps.setLong(12, seed._endDate);
			ps.executeUpdate();
			_auctions.add(new ClanHallAuction(id));
			LOGGER.info(() -> getClass().getSimpleName() + ": Created auction for ClanHall: " + id);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, e, () -> getClass().getSimpleName() + ": Exception: Auction.initNPC(): " + e.getMessage());
		}
	}
	
	public static ClanHallAuctionManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ClanHallAuctionManager INSTANCE = new ClanHallAuctionManager();
	}
}
