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

import com.l2journey.gameserver.data.xml.AdminData;

/**
 * @author HorridoJoho, Mobius
 */
public class AdminCommandAccessRight
{
	private final String _command;
	private final int _level;
	private final boolean _confirmDlg;
	
	/**
	 * Initializes the access right for an admin command using a StatSet configuration.
	 * @param set the StatSet containing configuration for the admin command
	 */
	public AdminCommandAccessRight(StatSet set)
	{
		_command = "admin_" + set.getString("command");
		_confirmDlg = set.getBoolean("confirmDlg", false);
		_level = set.getInt("accessLevel", 100);
	}
	
	/**
	 * Initializes the access right for an admin command with specific values.
	 * @param command the command name (with "admin_" prefix)
	 * @param confirm whether the command requires confirmation before execution
	 * @param level the access level required to execute the command
	 */
	public AdminCommandAccessRight(String command, boolean confirm, int level)
	{
		_command = command;
		_confirmDlg = confirm;
		_level = level;
	}
	
	/**
	 * Gets the admin command associated with this access right.
	 * @return the full admin command string, prefixed with "admin_"
	 */
	public String getAdminCommand()
	{
		return _command;
	}
	
	/**
	 * Checks whether the admin command requires confirmation before execution.
	 * @return {@code true} if confirmation is required, {@code false} otherwise
	 */
	public boolean requireConfirm()
	{
		return _confirmDlg;
	}
	
	/**
	 * Checks whether the provided access level grants permission to use the admin command.
	 * @param playerAccessLevel the access level of the player attempting to use the command
	 * @return {@code true} if the character's access level matches or exceeds the required access level, {@code false} otherwise
	 */
	public boolean hasAccess(AccessLevel playerAccessLevel)
	{
		// Check if the required access level is null.
		final AccessLevel accessLevel = AdminData.getInstance().getAccessLevel(_level);
		if (accessLevel == null)
		{
			return false;
		}
		
		// Check if the character's access level matches the required access level.
		// Check if the character's access level has child access to the required access level.
		return (accessLevel.getLevel() == playerAccessLevel.getLevel()) || playerAccessLevel.hasChildAccess(accessLevel);
	}
}
