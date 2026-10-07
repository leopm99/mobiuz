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
package com.l2journey.gameserver.model.zone.type;

import java.util.ArrayList;
import java.util.List;

import com.l2journey.gameserver.GeoData;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.zone.ZoneForm;

/**
 * Just dummy zone, needs only for geometry calculations
 * @author GKR, Mobius
 */
public class NpcSpawnTerritory
{
	private final String _name;
	private final ZoneForm _territory;
	private List<ZoneForm> _bannedTerritories;
	
	public NpcSpawnTerritory(String name, ZoneForm territory)
	{
		_name = name;
		_territory = territory;
	}
	
	public void addBannedTerritory(ZoneForm territory)
	{
		if (_bannedTerritories == null)
		{
			_bannedTerritories = new ArrayList<>(1);
		}
		_bannedTerritories.add(territory);
	}
	
	public String getName()
	{
		return _name;
	}
	
	/**
	 * GeoDriver rejects world coords outside its map; treat those as unsuitable for geo checks.
	 */
	private static boolean isInWorldBounds(int x, int y)
	{
		return (x >= World.WORLD_X_MIN) && (x <= World.WORLD_X_MAX) && (y >= World.WORLD_Y_MIN) && (y <= World.WORLD_Y_MAX);
	}
	
	/**
	 * @return {@code true} when height/LOS geo checks pass; {@code false} when the point is unsuitable or out of geo range
	 */
	private boolean passesGeoChecks(int randomX, int randomY, int randomZ, int centerX, int centerY, int centerZ)
	{
		if (!isInWorldBounds(randomX, randomY) || !isInWorldBounds(centerX, centerY))
		{
			return false;
		}
		
		try
		{
			if (GeoData.getInstance().getHeight(randomX, randomY, randomZ) > _territory.getHighZ())
			{
				return false;
			}
			return GeoData.getInstance().canSeeTarget(randomX, randomY, randomZ, centerX, centerY, centerZ, 0);
		}
		catch (IllegalArgumentException _)
		{
			// Out-of-range or unloaded geo tiles must not abort spawn loading.
			return false;
		}
	}
	
	public Location getRandomPoint()
	{
		int count = 0; // Prevent infinite loop.
		Location location;
		
		final Location centerPoint = _territory.getCenterPoint();
		final int centerX = centerPoint.getX();
		final int centerY = centerPoint.getY();
		final int centerZ = centerPoint.getZ();
		int randomX;
		int randomY;
		int randomZ;
		
		if (_bannedTerritories != null)
		{
			SEARCH: while (count++ < 100)
			{
				location = _territory.getRandomPoint();
				randomX = location.getX();
				randomY = location.getY();
				randomZ = location.getZ();
				
				for (ZoneForm territory : _bannedTerritories)
				{
					if (territory.isInsideZone(randomX, randomY, randomZ))
					{
						continue SEARCH;
					}
				}
				
				if (!passesGeoChecks(randomX, randomY, randomZ, centerX, centerY, centerZ))
				{
					continue;
				}
				
				return location;
			}
			
			count = 0;
			SEARCH_NO_GEO: while (count++ < 100)
			{
				location = _territory.getRandomPoint();
				randomX = location.getX();
				randomY = location.getY();
				randomZ = location.getZ();
				
				for (ZoneForm territory : _bannedTerritories)
				{
					if (territory.isInsideZone(randomX, randomY, randomZ))
					{
						continue SEARCH_NO_GEO;
					}
				}
				
				return location;
			}
		}
		
		count = 0;
		while (count++ < 100)
		{
			location = _territory.getRandomPoint();
			randomX = location.getX();
			randomY = location.getY();
			randomZ = location.getZ();
			
			if (!passesGeoChecks(randomX, randomY, randomZ, centerX, centerY, centerZ))
			{
				continue;
			}
			
			return location;
		}
		
		return _territory.getRandomPoint();
	}
	
	public boolean isInsideZone(int x, int y, int z)
	{
		return _territory.isInsideZone(x, y, z);
	}
	
	public int getHighZ()
	{
		return _territory.getHighZ();
	}
	
	public void visualizeZone(int z)
	{
		_territory.visualizeZone(z);
	}
}