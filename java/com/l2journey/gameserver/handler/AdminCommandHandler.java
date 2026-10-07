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
package com.l2journey.gameserver.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.Config;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.commons.time.TimeUtil;
import com.l2journey.gameserver.data.xml.AdminData;
import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.player.PlayerAction;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.ConfirmDlg;
import com.l2journey.gameserver.util.GMAudit;

public class AdminCommandHandler implements IHandler<IAdminCommandHandler, String>
{
	private static final Logger LOGGER = Logger.getLogger(AdminCommandHandler.class.getName());
	
	private final Map<String, IAdminCommandHandler> _datatable;
	
	protected AdminCommandHandler()
	{
		_datatable = new HashMap<>();
	}
	
	@Override
	public void registerHandler(IAdminCommandHandler handler)
	{
		for (String id : handler.getAdminCommandList())
		{
			_datatable.put(id, handler);
		}
	}
	
	@Override
	public synchronized void removeHandler(IAdminCommandHandler handler)
	{
		for (String id : handler.getAdminCommandList())
		{
			_datatable.remove(id);
		}
	}
	
	/**
	 * WARNING: Please use {@link #useAdminCommand(Player, String, boolean)} instead.
	 */
	@Override
	public IAdminCommandHandler getHandler(String adminCommand)
	{
		String command = adminCommand;
		if (adminCommand.contains(" "))
		{
			command = adminCommand.substring(0, adminCommand.indexOf(' '));
		}
		return _datatable.get(command);
	}
	
	public void useAdminCommand(Player player, String fullCommand, boolean useConfirm)
	{
		if (!player.isGM())
		{
			return;
		}
		
		final String command = fullCommand.split(" ")[0];
		final String commandNoPrefix = command.substring(6);
		final IAdminCommandHandler handler = getHandler(command);
		if (handler == null)
		{
			player.sendMessage("The command '" + commandNoPrefix + "' does not exist!");
			LOGGER.warning(() -> "No handler registered for admin command '" + command + "'");
			return;
		}
		
		if (!AdminData.getInstance().hasAccess(command, player.getAccessLevel()))
		{
			player.sendMessage("You don't have the access rights to use this command!");
			LOGGER.warning(() -> player + " tried to use admin command '" + command + "', without proper access level!");
			return;
		}
		
		if (useConfirm && AdminData.getInstance().requireConfirm(command))
		{
			player.setAdminConfirmCmd(fullCommand);
			final ConfirmDlg dlg = new ConfirmDlg(SystemMessageId.S1_3);
			dlg.getSystemMessage().addString("Are you sure you want execute command '" + commandNoPrefix + "' ?");
			player.addAction(PlayerAction.ADMIN_COMMAND);
			player.sendPacket(dlg);
		}
		else
		{
			// Admin Commands must run through a long running task, otherwise a command that takes too much time will freeze the server, this way you'll feel only a minor spike.
			ThreadPool.execute(() ->
			{
				final long begin = System.currentTimeMillis();
				try
				{
					if (Config.GMAUDIT)
					{
						final WorldObject target = player.getTarget();
						GMAudit.logAction(player.getName() + " [" + player.getObjectId() + "]", fullCommand, (target != null ? target.getName() : "no-target"));
					}
					
					handler.useAdminCommand(fullCommand, player);
				}
				catch (RuntimeException e)
				{
					player.sendMessage("Exception during execution of  '" + fullCommand + "': " + e.toString());
					LOGGER.log(Level.WARNING, e, () -> "Exception during execution of " + fullCommand);
				}
				finally
				{
					final long runtime = System.currentTimeMillis() - begin;
					if (runtime > 5000)
					{
						player.sendMessage("The execution of '" + fullCommand + "' took " + TimeUtil.formatDuration(runtime) + ".");
					}
				}
			});
		}
	}
	
	@Override
	public int size()
	{
		return _datatable.size();
	}
	
	public static AdminCommandHandler getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AdminCommandHandler INSTANCE = new AdminCommandHandler();
	}
}
