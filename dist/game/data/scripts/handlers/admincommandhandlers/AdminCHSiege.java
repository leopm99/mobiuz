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
package handlers.admincommandhandlers;

import java.util.Calendar;
import java.util.logging.Logger;

import com.l2journey.Config;
import com.l2journey.gameserver.data.sql.ClanTable;
import com.l2journey.gameserver.handler.IAdminCommandHandler;
import com.l2journey.gameserver.managers.CHSiegeManager;
import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.clan.Clan;
import com.l2journey.gameserver.model.siege.clanhalls.ClanHallSiegeEngine;
import com.l2journey.gameserver.model.siege.clanhalls.SiegableHall;
import com.l2journey.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2journey.gameserver.network.serverpackets.SiegeInfo;

/**
 * @author BiggBoss
 */
public class AdminCHSiege implements IAdminCommandHandler
{
	private static final Logger LOGGER = Logger.getLogger(AdminCHSiege.class.getName());
	
	private static final String[] COMMANDS =
	{
		"admin_chsiege_siegablehall",
		"admin_chsiege_startSiege",
		"admin_chsiege_endsSiege",
		"admin_chsiege_setSiegeDate",
		"admin_chsiege_addAttacker",
		"admin_chsiege_removeAttacker",
		"admin_chsiege_clearAttackers",
		"admin_chsiege_listAttackers",
		"admin_chsiege_forwardSiege"
	};
	
	@Override
	public String[] getAdminCommandList()
	{
		return COMMANDS;
	}
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		final String[] split = command.split(" ");
		if (Config.NO_QUESTS)
		{
			activeChar.sendSysMessage("AltDevNoQuests = true; Clan Hall Sieges are disabled!");
		}
		else if (split.length < 2)
		{
			activeChar.sendSysMessage("You have to specify the hall id at least");
		}
		else
		{
			final SiegableHall hall = getHall(split[1], activeChar);
			if (hall == null)
			{
				activeChar.sendSysMessage("Could not find he desired siegable hall (" + split[1] + ")");
			}
			else if (hall.getSiege() == null)
			{
				activeChar.sendSysMessage("The given hall does not have any attached siege!");
			}
			else
			{
				// Opening the hall page is the default command. Error paths must not refresh it.
				boolean refreshHallPage = split[0].equals(COMMANDS[0]);
				if (split[0].equals(COMMANDS[1]))
				{
					if (hall.isInSiege())
					{
						activeChar.sendSysMessage("The requested clan hall is alredy in siege!");
					}
					else
					{
						final Clan owner = ClanTable.getInstance().getClan(hall.getOwnerId());
						if (owner != null)
						{
							hall.free();
							owner.setHideoutId(0);
							hall.addAttacker(owner);
						}
						hall.getSiege().startSiege();
						refreshHallPage = true;
					}
				}
				else if (split[0].equals(COMMANDS[2]))
				{
					if (!hall.isInSiege())
					{
						activeChar.sendSysMessage("The requested clan hall is not in siege!");
					}
					else
					{
						hall.getSiege().endSiege();
						refreshHallPage = true;
					}
				}
				else if (split[0].equals(COMMANDS[3]))
				{
					if (!hall.isRegistering())
					{
						activeChar.sendSysMessage("Cannot change siege date while hall is in siege");
					}
					else if (split.length < 3)
					{
						activeChar.sendSysMessage("The date format is incorrect. Try again.");
					}
					else
					{
						final String[] rawDate = split[2].split(";");
						if (rawDate.length < 2)
						{
							activeChar.sendSysMessage("You have to specify this format DD-MM-YYYY;HH:MM");
						}
						else
						{
							final String[] day = rawDate[0].split("-");
							final String[] hour = rawDate[1].split(":");
							if ((day.length < 3) || (hour.length < 2))
							{
								activeChar.sendSysMessage("Incomplete day, hour or both!");
							}
							else
							{
								final int d = parseInt(day[0]);
								final int month = parseInt(day[1]) - 1;
								final int year = parseInt(day[2]);
								final int h = parseInt(hour[0]);
								final int min = parseInt(hour[1]);
								if (((month == 2) && (d > 28)) || (d > 31) || (d <= 0) || (month <= 0) || (month > 12) || (year < Calendar.getInstance().get(Calendar.YEAR)))
								{
									activeChar.sendSysMessage("Wrong day/month/year gave!");
								}
								else if ((h <= 0) || (h > 24) || (min < 0) || (min >= 60))
								{
									activeChar.sendSysMessage("Wrong hour/minutes gave!");
								}
								else
								{
									final Calendar c = Calendar.getInstance();
									c.set(Calendar.YEAR, year);
									c.set(Calendar.MONTH, month);
									c.set(Calendar.DAY_OF_MONTH, d);
									c.set(Calendar.HOUR_OF_DAY, h);
									c.set(Calendar.MINUTE, min);
									c.set(Calendar.SECOND, 0);
									if (c.getTimeInMillis() > System.currentTimeMillis())
									{
										activeChar.sendMessage(hall.getName() + " siege: " + c.getTime());
										hall.setNextSiegeDate(c.getTimeInMillis());
										hall.getSiege().updateSiege();
										hall.updateDb();
										refreshHallPage = true;
									}
									else
									{
										activeChar.sendSysMessage("The given time is in the past!");
									}
								}
							}
						}
					}
				}
				else if (split[0].equals(COMMANDS[4]))
				{
					if (hall.isInSiege())
					{
						activeChar.sendSysMessage("The clan hall is in siege, cannot add attackers now.");
					}
					else
					{
						Clan attacker = null;
						if (split.length < 3)
						{
							final WorldObject rawTarget = activeChar.getTarget();
							Player target = null;
							if (rawTarget == null)
							{
								activeChar.sendSysMessage("You must target a clan member of the attacker!");
							}
							else if (!rawTarget.isPlayer())
							{
								activeChar.sendSysMessage("You must target a player with clan!");
							}
							else if ((target = rawTarget.asPlayer()).getClan() == null)
							{
								activeChar.sendSysMessage("Your target does not have any clan!");
							}
							else if (hall.getSiege().checkIsAttacker(target.getClan()))
							{
								activeChar.sendSysMessage("Your target's clan is alredy participating!");
							}
							else
							{
								attacker = target.getClan();
							}
						}
						else
						{
							final Clan rawClan = ClanTable.getInstance().getClanByName(split[2]);
							if (rawClan == null)
							{
								activeChar.sendSysMessage("The given clan does not exist!");
							}
							else if (hall.getSiege().checkIsAttacker(rawClan))
							{
								activeChar.sendSysMessage("The given clan is alredy participating!");
							}
							else
							{
								attacker = rawClan;
							}
						}
						
						if (attacker != null)
						{
							hall.addAttacker(attacker);
							refreshHallPage = true;
						}
					}
				}
				else if (split[0].equals(COMMANDS[5]))
				{
					if (hall.isInSiege())
					{
						activeChar.sendSysMessage("The clan hall is in siege, cannot remove attackers now.");
					}
					else if (split.length < 3)
					{
						final WorldObject rawTarget = activeChar.getTarget();
						Player target = null;
						if (rawTarget == null)
						{
							activeChar.sendSysMessage("You must target a clan member of the attacker!");
						}
						else if (!rawTarget.isPlayer())
						{
							activeChar.sendSysMessage("You must target a player with clan!");
						}
						else if ((target = rawTarget.asPlayer()).getClan() == null)
						{
							activeChar.sendSysMessage("Your target does not have any clan!");
						}
						else if (!hall.getSiege().checkIsAttacker(target.getClan()))
						{
							activeChar.sendSysMessage("Your target's clan is not participating!");
						}
						else
						{
							hall.removeAttacker(target.getClan());
							refreshHallPage = true;
						}
					}
					else
					{
						final Clan rawClan = ClanTable.getInstance().getClanByName(split[2]);
						if (rawClan == null)
						{
							activeChar.sendSysMessage("The given clan does not exist!");
						}
						else if (!hall.getSiege().checkIsAttacker(rawClan))
						{
							activeChar.sendSysMessage("The given clan is not participating!");
						}
						else
						{
							hall.removeAttacker(rawClan);
							refreshHallPage = true;
						}
					}
				}
				else if (split[0].equals(COMMANDS[6]))
				{
					if (hall.isInSiege())
					{
						activeChar.sendSysMessage("The requested hall is in siege right now, cannot clear attacker list!");
					}
					else
					{
						hall.getSiege().getAttackers().clear();
						refreshHallPage = true;
					}
				}
				else if (split[0].equals(COMMANDS[7]))
				{
					activeChar.sendPacket(new SiegeInfo(hall, activeChar));
					refreshHallPage = true;
				}
				else if (split[0].equals(COMMANDS[8]))
				{
					final ClanHallSiegeEngine siegable = hall.getSiege();
					siegable.cancelSiegeTask();
					switch (hall.getSiegeStatus())
					{
						case REGISTERING:
						{
							siegable.prepareOwner();
							break;
						}
						case WAITING_BATTLE:
						{
							siegable.startSiege();
							break;
						}
						case RUNNING:
						{
							siegable.endSiege();
							break;
						}
					}
					refreshHallPage = true;
				}
				
				if (refreshHallPage)
				{
					sendSiegableHallPage(activeChar, split[1], hall);
				}
			}
		}
		return false;
	}
	
	private SiegableHall getHall(String id, Player gm)
	{
		final int ch = parseInt(id);
		if (ch == 0)
		{
			gm.sendMessage("Wrong clan hall id, unparseable id!");
			return null;
		}
		
		final SiegableHall hall = CHSiegeManager.getInstance().getSiegableHall(ch);
		if (hall == null)
		{
			gm.sendMessage("Could not find the clan hall.");
		}
		
		return hall;
	}
	
	private int parseInt(String st)
	{
		int val = 0;
		try
		{
			val = Integer.parseInt(st);
		}
		catch (NumberFormatException e)
		{
			LOGGER.warning("Problem with AdminCHSiege: " + e.getMessage());
		}
		return val;
	}
	
	private void sendSiegableHallPage(Player activeChar, String hallId, SiegableHall hall)
	{
		final NpcHtmlMessage msg = new NpcHtmlMessage();
		msg.setFile(null, "data/html/admin/siegablehall.htm");
		msg.replace("%clanhallId%", hallId);
		msg.replace("%clanhallName%", hall.getName());
		if (hall.getOwnerId() > 0)
		{
			final Clan owner = ClanTable.getInstance().getClan(hall.getOwnerId());
			if (owner != null)
			{
				msg.replace("%clanhallOwner%", owner.getName());
			}
			else
			{
				msg.replace("%clanhallOwner%", "No Owner");
			}
		}
		else
		{
			msg.replace("%clanhallOwner%", "No Owner");
		}
		activeChar.sendPacket(msg);
	}
}
