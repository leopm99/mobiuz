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
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.Config;
import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.model.TeleportLocation;

public class TeleportLocationTable
{
	private static final Logger LOGGER = Logger.getLogger(TeleportLocationTable.class.getName());
	
	private final Map<Integer, TeleportLocation> _teleports = new HashMap<>();
	
	protected TeleportLocationTable()
	{
		reloadAll();
	}
	
	public void reloadAll()
	{
		_teleports.clear();
		try (Connection con = DatabaseFactory.getConnection();
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT id, loc_x, loc_y, loc_z, price, fornoble, itemId FROM teleport"))
		{
			TeleportLocation teleport;
			while (rs.next())
			{
				teleport = new TeleportLocation();
				teleport.setTeleId(rs.getInt("id"));
				teleport.setLocX(rs.getInt("loc_x"));
				teleport.setLocY(rs.getInt("loc_y"));
				teleport.setLocZ(rs.getInt("loc_z"));
				teleport.setPrice(rs.getInt("price"));
				teleport.setForNoble(rs.getInt("fornoble") == 1);
				teleport.setItemId(rs.getInt("itemId"));
				
				_teleports.put(teleport.getTeleId(), teleport);
			}
			LOGGER.info(() -> getClass().getSimpleName() + ": Loaded " + _teleports.size() + " teleport location templates.");
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, e, () -> getClass().getSimpleName() + ": Error loading Teleport Table.");
		}
		
		if (!Config.CUSTOM_TELEPORT_TABLE)
		{
			return;
		}
		
		int cTeleCount = _teleports.size();
		try (Connection con = DatabaseFactory.getConnection();
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT id, loc_x, loc_y, loc_z, price, fornoble, itemId FROM custom_teleport"))
		{
			TeleportLocation teleport;
			while (rs.next())
			{
				teleport = new TeleportLocation();
				teleport.setTeleId(rs.getInt("id"));
				teleport.setLocX(rs.getInt("loc_x"));
				teleport.setLocY(rs.getInt("loc_y"));
				teleport.setLocZ(rs.getInt("loc_z"));
				teleport.setPrice(rs.getInt("price"));
				teleport.setForNoble(rs.getInt("fornoble") == 1);
				teleport.setItemId(rs.getInt("itemId"));
				
				_teleports.put(teleport.getTeleId(), teleport);
			}
			cTeleCount = _teleports.size() - cTeleCount;
			if (cTeleCount > 0)
			{
				final int loggedCTeleCount = cTeleCount;
				LOGGER.info(() -> getClass().getSimpleName() + ": Loaded " + loggedCTeleCount + " custom teleport location templates.");
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e, () -> getClass().getSimpleName() + ": Error while creating custom teleport table " + e.getMessage());
		}
	}
	
	/**
	 * @param id
	 * @return
	 */
	public TeleportLocation getTemplate(int id)
	{
		return _teleports.get(id);
	}
	
	public static TeleportLocationTable getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final TeleportLocationTable INSTANCE = new TeleportLocationTable();
	}
}
