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

import java.util.StringTokenizer;

import com.l2journey.Config;
import com.l2journey.gameserver.data.xml.ExperienceData;
import com.l2journey.gameserver.handler.IAdminCommandHandler;
import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.network.SystemMessageId;

public class AdminLevel implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_add_level",
		"admin_set_level"
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		final WorldObject targetChar = activeChar.getTarget();
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken(); // Get actual command
		String val = "";
		if (st.countTokens() >= 1)
		{
			val = st.nextToken();
		}
		
		if (actualCommand.equalsIgnoreCase("admin_add_level"))
		{
			try
			{
				if ((targetChar != null) && targetChar.isPlayable())
				{
					targetChar.asPlayable().getStat().addLevel(Byte.parseByte(val));
				}
			}
			catch (NumberFormatException _)
			{
				activeChar.sendSysMessage("Wrong Number Format");
			}
		}
		else if (actualCommand.equalsIgnoreCase("admin_set_level"))
		{
			if ((targetChar == null) || !targetChar.isPlayer())
			{
				activeChar.sendPacket(SystemMessageId.THAT_IS_AN_INCORRECT_TARGET); // incorrect target!
				return false;
			}
			
			final Player targetPlayer = targetChar.asPlayer();
			int maxLevel = ExperienceData.getInstance().getMaxLevel();
			if (targetPlayer.isSubClassActive() && (Config.MAX_SUBCLASS_LEVEL < maxLevel))
			{
				maxLevel = Config.MAX_SUBCLASS_LEVEL + 1; // +1 para deixar a barra de exp com 100%
			}
			
			try
			{
				final byte level = Byte.parseByte(val);
				if ((level >= 1) && (level <= maxLevel))
				{
					final long pXp = targetPlayer.getExp();
					final long tXp = ExperienceData.getInstance().getExpForLevel(level);
					if (pXp > tXp)
					{
						targetPlayer.getStat().setLevel(level);
						targetPlayer.removeExpAndSp(pXp - tXp, 0);
						activeChar.sendSysMessage("Removed " + (pXp - tXp) + " exp.");
					}
					else if (pXp < tXp)
					{
						targetPlayer.addExpAndSp(tXp - pXp, 0);
						activeChar.sendSysMessage("Added " + (tXp - pXp) + " exp.");
					}
					targetPlayer.setCurrentHpMp(targetPlayer.getMaxHp(), targetPlayer.getMaxMp());
					targetPlayer.setCurrentCp(targetPlayer.getMaxCp());
					targetPlayer.broadcastUserInfo();
				}
				else
				{
					activeChar.sendSysMessage("You must specify level between 1 and " + maxLevel + ".");
					return false;
				}
			}
			catch (NumberFormatException _)
			{
				activeChar.sendSysMessage("You must specify level between 1 and " + maxLevel + ".");
				return false;
			}
		}
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
