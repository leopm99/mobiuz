/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2journey.gameserver.model.siege;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.Config;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.clan.Clan;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.itemcontainer.Inventory;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;

/**
 * Class managing periodical events with castle
 * @author Vice
 */
public class FortUpdater implements Runnable
{
	private static final Logger LOGGER = Logger.getLogger(FortUpdater.class.getName());
	private static final int CASTLE_SUPPLY_REPUTATION_COST = 2;
	
	private final Clan _clan;
	private final Fort _fort;
	private int _runCount;
	private final FortUpdaterType _updaterType;
	
	public FortUpdater(Fort fort, Clan clan, int runCount, FortUpdaterType ut)
	{
		_fort = fort;
		_clan = clan;
		_runCount = runCount;
		_updaterType = ut;
	}
	
	@Override
	public void run()
	{
		try
		{
			switch (_updaterType)
			{
				case PERIODIC_UPDATE:
				{
					_runCount++;
					if ((_fort.getOwnerClan() == null) || (_fort.getOwnerClan() != _clan))
					{
						return;
					}
					
					_fort.getOwnerClan().increaseBloodOathCount();
					
					if (_fort.getFortState() == 2)
					{
						if (_clan.getWarehouse().getAdena() >= Config.FS_FEE_FOR_CASTLE)
						{
							_clan.getWarehouse().destroyItemByItemId(ItemProcessType.FEE, Inventory.ADENA_ID, Config.FS_FEE_FOR_CASTLE, null, null);
							_fort.getContractedCastle().addToTreasuryNoTax(Config.FS_FEE_FOR_CASTLE);
							
							final Clan castleClan = _fort.getContractedCastle().getOwner();
							if ((castleClan != null) && (castleClan.getReputationScore() >= CASTLE_SUPPLY_REPUTATION_COST))
							{
								castleClan.takeReputationScore(CASTLE_SUPPLY_REPUTATION_COST);
								_fort.raiseSupplyLeveL();
							}
							else
							{
								final SystemMessage sm = new SystemMessage(SystemMessageId.THE_SUPPLY_ITEMS_HAVE_NOT_BEEN_PROVIDED_BECAUSE_THE_CASTLE_YOU_ARE_IN_CONTRACT_WITH_DOESN_T_HAVE_ENOUGH_CLAN_REPUTATION);
								final Player fortressLeader = _clan.getLeader().getPlayer();
								if (fortressLeader != null)
								{
									fortressLeader.sendPacket(sm);
								}
								if (castleClan != null)
								{
									final Player castleLeader = castleClan.getLeader().getPlayer();
									if (castleLeader != null)
									{
										castleLeader.sendPacket(sm);
									}
								}
							}
						}
						else
						{
							_fort.setFortState(1, 0);
						}
					}
					_fort.saveFortVariables();
					break;
				}
				case MAX_OWN_TIME:
				{
					if ((_fort.getOwnerClan() == null) || (_fort.getOwnerClan() != _clan))
					{
						return;
					}
					if (_fort.getOwnedTime() > (Config.FS_MAX_OWN_TIME * 3600))
					{
						_fort.removeOwner(true);
						_fort.setFortState(0, 0);
					}
					break;
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "", e);
		}
	}
	
	public int getRunCount()
	{
		return _runCount;
	}
}