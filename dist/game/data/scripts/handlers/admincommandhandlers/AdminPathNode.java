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
package handlers.admincommandhandlers;

import java.util.List;

import com.l2journey.Config;
import com.l2journey.gameserver.handler.IAdminCommandHandler;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.pathfinding.AbstractNodeLoc;
import com.l2journey.gameserver.pathfinding.PathFinding;

public class AdminPathNode implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_pn_info",
		"admin_show_path",
		"admin_path_debug",
		"admin_show_pn",
		"admin_find_path",
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (command.equals("admin_pn_info"))
		{
			final String[] info = PathFinding.getInstance().getStat();
			if (info == null)
			{
				activeChar.sendMessage("Not supported");
			}
			else
			{
				for (String msg : info)
				{
					activeChar.sendMessage(msg);
				}
			}
		}
		else if (command.equals("admin_show_path"))
		{
			
		}
		else if (command.equals("admin_path_debug"))
		{
			
		}
		else if (command.equals("admin_show_pn"))
		{
			
		}
		else if (command.equals("admin_find_path"))
		{
			if (Config.PATHFINDING == 0)
			{
				activeChar.sendMessage("PathFinding is disabled.");
			}
			else if (activeChar.getTarget() != null)
			{
				List<AbstractNodeLoc> path = PathFinding.getInstance().findPath(activeChar.getX(), activeChar.getY(), (short) activeChar.getZ(), activeChar.getTarget().getX(), activeChar.getTarget().getY(), (short) activeChar.getTarget().getZ(), activeChar.getInstanceId(), true);
				if (path == null)
				{
					activeChar.sendMessage("No Route!");
				}
				else
				{
					for (AbstractNodeLoc a : path)
					{
						activeChar.sendMessage("x:" + a.getX() + " y:" + a.getY() + " z:" + a.getZ());
					}
				}
			}
			else
			{
				activeChar.sendMessage("No Target!");
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
