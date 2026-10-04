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
package com.l2journey.gameserver.model.zone.type;

import com.l2journey.Config;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.player.PrivateStoreType;
import com.l2journey.gameserver.model.zone.ZoneId;
import com.l2journey.gameserver.model.zone.ZoneType;
import com.l2journey.gameserver.network.Disconnection;
import com.l2journey.gameserver.network.serverpackets.LeaveWorld;

/**
 * A Town zone
 * @author durgus
 */
public class TownZone extends ZoneType
{
	private int _townId;
	private int _taxById;
	private boolean _isTWZone = false;
	
	public TownZone(int id)
	{
		super(id);
		
		_taxById = 0;
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("townId"))
		{
			_townId = Integer.parseInt(value);
		}
		else if (name.equals("taxById"))
		{
			_taxById = Integer.parseInt(value);
		}
		else
		{
			super.setParameter(name, value);
		}
	}
	
	@Override
	protected void onEnter(Creature creature)
	{
		if (_isTWZone)
		{
			creature.setInTownWarEvent(true);
			creature.setInsideZone(ZoneId.PVP, true);
			creature.updatePvPFlag(1);
			creature.broadcastInfo();
			creature.sendMessage("You entered a Town War event zone.");
		}
		creature.setInsideZone(ZoneId.TOWN, true);
	}
	
	@Override
	protected void onExit(Creature creature)
	{
		if (_isTWZone)
		{
			creature.setInTownWarEvent(false);
			creature.setInsideZone(ZoneId.PVP, false);
			creature.updatePvPFlag(0);
			creature.broadcastInfo();
			creature.sendMessage("You left a Town War event zone.");
		}
		creature.setInsideZone(ZoneId.TOWN, false);
	}
	
	public void updateForCharactersInside()
	{
		for (Creature creature : getCharactersInside())
		{
			if (creature != null)
			{
				onUpdate(creature);
			}
		}
	}
	
	public void onUpdate(Creature creature)
	{
		if (_isTWZone)
		{
			if (creature instanceof Player)
			{
				Player player = (Player) creature;
				if (hasAnyShop(player))
				{
					disconnectPlayerWithShop(player);
					return;
				}
			}
			
			creature.setInTownWarEvent(true);
			creature.setInsideZone(ZoneId.PVP, true);
			creature.updatePvPFlag(1);
			creature.broadcastInfo();
			creature.sendMessage("You entered a Town War event zone.");
		}
		else
		{
			creature.setInTownWarEvent(false);
			creature.setInsideZone(ZoneId.PVP, false);
			creature.updatePvPFlag(0);
			creature.broadcastInfo();
			creature.sendMessage("You left a Town War event zone.");
		}
	}
	
	/**
	 * Returns this zones town id (if any)
	 * @return
	 */
	public int getTownId()
	{
		return _townId;
	}
	
	/**
	 * Returns this town zones castle id
	 * @return
	 */
	public int getTaxById()
	{
		return _taxById;
	}
	
	public final void setIsTWZone(boolean value)
	{
		_isTWZone = value;
	}
	
	@Override
	public void onReviveInside(Creature creature)
	{
		if (_isTWZone)
		{
			heal(creature);
		}
	}
	
	static void heal(Creature creature)
	{
		creature.setCurrentHp(creature.getMaxHp());
		creature.setCurrentCp(creature.getMaxCp());
		creature.setCurrentMp(creature.getMaxMp());
	}
	
	private boolean hasAnyShop(Player player)
	{
		return ((player.getPrivateStoreType() == PrivateStoreType.SELL) || (player.getPrivateStoreType() == PrivateStoreType.BUY)) || (Config.OFFLINE_CRAFT_ENABLE && (player.isCrafting() || (player.getPrivateStoreType() == PrivateStoreType.MANUFACTURE)));
	}
	
	private void disconnectPlayerWithShop(Player player)
	{
		Disconnection.of(player).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
		player.broadcastUserInfo();
	}
	
}
