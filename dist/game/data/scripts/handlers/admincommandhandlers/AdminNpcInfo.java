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
 */
package handlers.admincommandhandlers;

import java.util.StringTokenizer;

import com.l2journey.gameserver.handler.IAdminCommandHandler;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.network.SystemMessageId;

import handlers.actionshifthandlers.NpcActionShift;

/**
 * Reopens the GM NPC Info panel (same content as shift-click on an NPC).
 */
public class AdminNpcInfo implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_npcinfo"
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (!command.startsWith("admin_npcinfo"))
		{
			return false;
		}
		
		WorldObject target = null;
		final StringTokenizer st = new StringTokenizer(command, " ");
		st.nextToken();
		if (st.hasMoreTokens())
		{
			try
			{
				target = World.getInstance().findObject(Integer.parseInt(st.nextToken()));
			}
			catch (NumberFormatException _)
			{
				activeChar.sendSysMessage("Usage: //npcinfo [objectId]");
				return false;
			}
		}
		else if (activeChar.getTarget() != null)
		{
			target = activeChar.getTarget();
		}
		
		if ((target == null) || !target.isNpc())
		{
			activeChar.sendPacket(SystemMessageId.THAT_IS_AN_INCORRECT_TARGET);
			return false;
		}
		
		return new NpcActionShift().action(activeChar, target, false);
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
