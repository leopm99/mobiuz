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
package com.l2journey.gameserver.model;

import com.l2journey.gameserver.model.actor.Creature;

/**
 * @author xban1x
 */
public class AggroInfo
{
	private static final long MAX_VALUE = 1000000000000000L;
	
	private final Creature _attacker;
	private long _hate = 0;
	private long _damage = 0;
	
	public AggroInfo(Creature pAttacker)
	{
		_attacker = pAttacker;
	}
	
	public Creature getAttacker()
	{
		return _attacker;
	}
	
	public long getHate()
	{
		return _hate;
	}
	
	public long checkHate(Creature owner)
	{
		if (_attacker.isAlikeDead() || !_attacker.isSpawned() || !owner.isInSurroundingRegion(_attacker))
		{
			_hate = 0;
		}
		return _hate;
	}
	
	public void addHate(long value)
	{
		_hate = Math.min(_hate + value, MAX_VALUE);
	}
	
	public void stopHate()
	{
		_hate = 0;
	}
	
	public long getDamage()
	{
		return _damage;
	}
	
	public void addDamage(long value)
	{
		_damage = Math.min(_damage + value, MAX_VALUE);
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		
		if (obj instanceof AggroInfo aggroInfo)
		{
			return (aggroInfo.getAttacker() == _attacker);
		}
		
		return false;
	}
	
	@Override
	public int hashCode()
	{
		return _attacker.getObjectId();
	}
	
	@Override
	public String toString()
	{
		return "AggroInfo [attacker=" + _attacker + ", hate=" + _hate + ", damage=" + _damage + "]";
	}
}
