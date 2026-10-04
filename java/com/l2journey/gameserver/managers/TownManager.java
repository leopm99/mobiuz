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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.l2journey.gameserver.model.siege.Castle;
import com.l2journey.gameserver.model.zone.ZoneType;
import com.l2journey.gameserver.model.zone.type.TownZone;

/**
 * @author Zoinha
 */
public class TownManager
{
	private static final Map<Integer, Integer> CASTLES = new ConcurrentHashMap<>();
	
	private TownManager()
	{
		CASTLES.put(912, 1);
		CASTLES.put(916, 2);
		CASTLES.put(918, 3);
		CASTLES.put(922, 4);
		CASTLES.put(924, 5);
		CASTLES.put(926, 6);
		CASTLES.put(1538, 7);
		CASTLES.put(1537, 8);
		CASTLES.put(1714, 9);
	}
	
	public static int getTownCastle(int townId)
	{
		return CASTLES.containsKey(townId) ? CASTLES.get(townId) : 0;
	}
	
	public static boolean townHasCastleInSiege(int townId)
	{
		final int castleIndex = getTownCastle(townId);
		if (castleIndex > 0)
		{
			final Castle castle = CastleManager.getInstance().getCastles().get(CastleManager.getInstance().getCastleIndex(castleIndex));
			if (castle != null)
			{
				return castle.getSiege().isInProgress();
			}
		}
		return false;
	}
	
	public static boolean townHasCastleInSiege(int x, int y)
	{
		return townHasCastleInSiege(MapRegionManager.getInstance().getMapRegionLocId(x, y));
	}
	
	public static TownZone getTown(int townId)
	{
		for (TownZone temp : ZoneManager.getInstance().getAllZones(TownZone.class))
		{
			if (temp.getTownId() == townId)
			{
				return temp;
			}
		}
		return null;
	}
	
	/**
	 * Returns the town at that position (if any)
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public static TownZone getTown(int x, int y, int z)
	{
		for (ZoneType temp : ZoneManager.getInstance().getZones(x, y, z))
		{
			if (temp instanceof TownZone townZone)
			{
				return townZone;
			}
		}
		return null;
	}
}
