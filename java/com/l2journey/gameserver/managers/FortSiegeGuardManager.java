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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.model.Spawn;
import com.l2journey.gameserver.model.siege.Fort;

public class FortSiegeGuardManager
{
	private static final Logger LOGGER = Logger.getLogger(FortSiegeGuardManager.class.getName());
	
	private final Fort _fort;
	private final Map<Integer, List<Spawn>> _siegeGuards = new HashMap<>();
	
	public FortSiegeGuardManager(Fort fort)
	{
		_fort = fort;
	}
	
	/**
	 * Spawn guards.
	 */
	public void spawnSiegeGuard()
	{
		try
		{
			final List<Spawn> monsterList = _siegeGuards.get(_fort.getResidenceId());
			if (monsterList != null)
			{
				for (Spawn spawnDat : monsterList)
				{
					spawnDat.doSpawn(false);
					if (spawnDat.getRespawnDelay() == 0)
					{
						spawnDat.stopRespawn();
					}
					else
					{
						spawnDat.startRespawn();
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e, () -> "Error spawning siege guards for fort " + _fort.getName() + ":" + e.getMessage());
		}
	}
	
	/**
	 * Unspawn guards.
	 */
	public void unspawnSiegeGuard()
	{
		try
		{
			final List<Spawn> monsterList = _siegeGuards.get(_fort.getResidenceId());
			if (monsterList != null)
			{
				for (Spawn spawnDat : monsterList)
				{
					spawnDat.stopRespawn();
					if (spawnDat.getLastSpawn() != null)
					{
						spawnDat.getLastSpawn().doDie(spawnDat.getLastSpawn());
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e, () -> "Error unspawning siege guards for fort " + _fort.getName() + ":" + e.getMessage());
		}
	}
	
	/**
	 * Load guards.
	 */
	void loadSiegeGuard()
	{
		_siegeGuards.clear();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT npcId, x, y, z, heading, respawnDelay FROM fort_siege_guards WHERE fortId = ?"))
		{
			final int fortId = _fort.getResidenceId();
			ps.setInt(1, fortId);
			try (ResultSet rs = ps.executeQuery())
			{
				final List<Spawn> siegeGuardSpawns = new ArrayList<>();
				while (rs.next())
				{
					final Spawn spawn = new Spawn(rs.getInt("npcId"));
					spawn.setAmount(1);
					spawn.setXYZ(rs.getInt("x"), rs.getInt("y"), rs.getInt("z"));
					spawn.setHeading(rs.getInt("heading"));
					spawn.setRespawnDelay(rs.getInt("respawnDelay"));
					spawn.setLocationId(0);
					
					siegeGuardSpawns.add(spawn);
				}
				_siegeGuards.put(fortId, siegeGuardSpawns);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e, () -> "Error loading siege guard for fort " + _fort.getName() + ": " + e.getMessage());
		}
	}
	
	public Fort getFort()
	{
		return _fort;
	}
	
	public Map<Integer, List<Spawn>> getSiegeGuardSpawn()
	{
		return _siegeGuards;
	}
}
