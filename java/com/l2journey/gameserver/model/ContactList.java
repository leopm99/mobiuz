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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.data.sql.CharInfoTable;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;

/**
 * TODO: System messages:<br>
 * ADD: 3223: The previous name is being registered. Please try again later.<br>
 * DEL 3219: $s1 was successfully deleted from your Contact List.<br>
 * DEL 3217: The name is not currently registered.
 * @author UnAfraid, mrTJO
 */
public class ContactList
{
	private static final Logger LOGGER = Logger.getLogger(ContactList.class.getName());
	
	private final Player _player;
	private final Set<String> _contacts = ConcurrentHashMap.newKeySet();
	
	private static final String QUERY_ADD = "REPLACE INTO character_contacts (charId, contactId) VALUES (?, ?)";
	private static final String QUERY_REMOVE = "DELETE FROM character_contacts WHERE charId = ? and contactId = ?";
	private static final String QUERY_LOAD = "SELECT contactId FROM character_contacts WHERE charId = ?";
	
	public ContactList(Player player)
	{
		_player = player;
		restore();
	}
	
	public void restore()
	{
		_contacts.clear();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(QUERY_LOAD))
		{
			statement.setInt(1, _player.getObjectId());
			try (ResultSet rset = statement.executeQuery())
			{
				int contactId;
				String contactName;
				while (rset.next())
				{
					contactId = rset.getInt(1);
					contactName = CharInfoTable.getInstance().getNameById(contactId);
					if ((contactName == null) || contactName.equals(_player.getName()) || (contactId == _player.getObjectId()))
					{
						continue;
					}
					
					_contacts.add(contactName);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e, () -> "Error found in " + _player.getName() + "'s ContactsList: " + e.getMessage());
		}
	}
	
	public boolean add(String name)
	{
		SystemMessage sm;
		
		final int contactId = CharInfoTable.getInstance().getIdByName(name);
		if (_contacts.contains(name))
		{
			_player.sendPacket(SystemMessageId.THE_NAME_ALREADY_EXISTS_ON_THE_ADDED_LIST);
			return false;
		}
		else if (_player.getName().equals(name))
		{
			_player.sendPacket(SystemMessageId.YOU_CANNOT_ADD_YOUR_OWN_NAME);
			return false;
		}
		else if (_contacts.size() >= 100)
		{
			_player.sendPacket(SystemMessageId.THE_MAXIMUM_NUMBER_OF_NAMES_100_HAS_BEEN_REACHED_YOU_CANNOT_REGISTER_ANY_MORE);
			return false;
		}
		else if (contactId < 1)
		{
			sm = new SystemMessage(SystemMessageId.THE_NAME_S1_DOESN_T_EXIST_PLEASE_TRY_ANOTHER_NAME);
			sm.addString(name);
			_player.sendPacket(sm);
			return false;
		}
		else
		{
			for (String contactName : _contacts)
			{
				if (contactName.equalsIgnoreCase(name))
				{
					_player.sendPacket(SystemMessageId.THE_NAME_ALREADY_EXISTS_ON_THE_ADDED_LIST);
					return false;
				}
			}
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(QUERY_ADD))
		{
			statement.setInt(1, _player.getObjectId());
			statement.setInt(2, contactId);
			statement.execute();
			
			_contacts.add(name);
			
			sm = new SystemMessage(SystemMessageId.S1_WAS_SUCCESSFULLY_ADDED_TO_YOUR_CONTACT_LIST);
			sm.addString(name);
			_player.sendPacket(sm);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e, () -> "Error found in " + _player.getName() + "'s ContactsList: " + e.getMessage());
		}
		return true;
	}
	
	public void remove(String name)
	{
		final int contactId = CharInfoTable.getInstance().getIdByName(name);
		if (!_contacts.contains(name))
		{
			_player.sendPacket(SystemMessageId.THE_NAME_IS_NOT_CURRENTLY_REGISTERED);
			return;
		}
		else if (contactId < 1)
		{
			// TODO: Message?
			return;
		}
		
		_contacts.remove(name);
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(QUERY_REMOVE))
		{
			statement.setInt(1, _player.getObjectId());
			statement.setInt(2, contactId);
			statement.execute();
			
			final SystemMessage sm = new SystemMessage(SystemMessageId.S1_WAS_SUCCESSFULLY_DELETED_FROM_YOUR_CONTACT_LIST);
			sm.addString(name);
			_player.sendPacket(sm);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e, () -> "Error found in " + _player.getName() + "'s ContactsList: " + e.getMessage());
		}
	}
	
	public Set<String> getAllContacts()
	{
		return _contacts;
	}
}
