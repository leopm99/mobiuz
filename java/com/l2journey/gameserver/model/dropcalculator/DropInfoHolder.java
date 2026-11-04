/*
 * Copyright (C) 2004-2015 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2journey.gameserver.model.dropcalculator;

/**
 * @author vGodFather
 */
public class DropInfoHolder
{
private final int _npcId;
	private final String _npcName;
	private final byte _npcLevel;
	private final long _minDrop;
	private final long _maxDrop;
	private final double _chance;
	private final boolean _isSweep;
	
	public DropInfoHolder(int npcId, String npcName, byte level, long min, long max, double chance, boolean sweep)
	{
		_npcId = npcId;
		_npcName = npcName;
		_npcLevel = level;
		_minDrop = min;
		_maxDrop = max;
		_chance = chance;
		_isSweep = sweep;
	}
	
	public int getNpcId()
	{
		return _npcId;
	}
	
public String getName()
	{
		return _npcName;
	}
	
public byte getLevel()
	{
		return _npcLevel;
	}
	
public boolean isSweep()
	{
		return _isSweep;
	}
	
/**
	 * Returns the minimum quantity of items dropped
	 * @return int
	 */
	public long getMin()
	{
		return _minDrop;
	}
	
	/**
	 * Returns the maximum quantity of items dropped
	 * @return int
	 */
	public long getMax()
	{
		return _maxDrop;
	}
	
	/**
	 * Returns the chance of having a drop
	 * @return int
	 */
	public double getChance()
	{
		return _chance;
	}
	
/**
	 * Sets the value for maximal quantity of dopped items
	 * @param maxdrop : int designating the quantity of dropped items
	 */
/**
	 * Sets the chance of having the item for a drop
	 * @param chance : int designating the chance
	 */
}