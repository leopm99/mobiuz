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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.data.xml.UIData;

/**
 * UI Keys Settings class.
 * @author mrTJO, Zoey76
 */
public class UIKeysSettings
{
	private static final Logger LOGGER = Logger.getLogger(UIKeysSettings.class.getName());
	
	private final int _playerObjId;
	private Map<Integer, List<ActionKey>> _storedKeys;
	private Map<Integer, List<Integer>> _storedCategories;
	private boolean _saved = true;
	
	public UIKeysSettings(int playerObjId)
	{
		_playerObjId = playerObjId;
		loadFromDB();
	}
	
	public void storeAll(Map<Integer, List<Integer>> catMap, Map<Integer, List<ActionKey>> keyMap)
	{
		_saved = false;
		_storedCategories = catMap;
		_storedKeys = keyMap;
	}
	
	public void storeCategories(Map<Integer, List<Integer>> catMap)
	{
		_saved = false;
		_storedCategories = catMap;
	}
	
	public Map<Integer, List<Integer>> getCategories()
	{
		return _storedCategories;
	}
	
	public void storeKeys(Map<Integer, List<ActionKey>> keyMap)
	{
		_saved = false;
		_storedKeys = keyMap;
	}
	
	public Map<Integer, List<ActionKey>> getKeys()
	{
		return _storedKeys;
	}
	
	public void loadFromDB()
	{
		getCatsFromDB();
		getKeysFromDB();
	}
	
	/**
	 * Save Categories and Mapped Keys into GameServer DataBase
	 */
	public void saveInDB()
	{
		if (_saved)
		{
			return;
		}
		
		if (hasCategoryRows())
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement statement = con.prepareStatement("REPLACE INTO character_ui_categories (`charId`, `catId`, `order`, `cmdId`) VALUES (?,?,?,?)"))
			{
				for (Map.Entry<Integer, List<Integer>> entry : _storedCategories.entrySet())
				{
					int category = entry.getKey();
					int order = 0;
					for (int key : entry.getValue())
					{
						statement.setInt(1, _playerObjId);
						statement.setInt(2, category);
						statement.setInt(3, order++);
						statement.setInt(4, key);
						statement.addBatch();
					}
				}
				statement.executeBatch();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, e, () -> "Exception: saveInDB(): " + e.getMessage());
			}
		}
		
		if (hasActionRows())
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement statement = con.prepareStatement("REPLACE INTO character_ui_actions (`charId`, `cat`, `order`, `cmd`, `key`, `tgKey1`, `tgKey2`, `show`) VALUES (?,?,?,?,?,?,?,?)"))
			{
				for (List<ActionKey> keyLst : _storedKeys.values())
				{
					int order = 0;
					for (ActionKey key : keyLst)
					{
						statement.setInt(1, _playerObjId);
						statement.setInt(2, key.getCategory());
						statement.setInt(3, order++);
						statement.setInt(4, key.getCommandId());
						statement.setInt(5, key.getKeyId());
						statement.setInt(6, key.getToogleKey1());
						statement.setInt(7, key.getToogleKey2());
						statement.setInt(8, key.getShowStatus());
						statement.addBatch();
					}
				}
				statement.executeBatch();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, e, () -> "Exception: saveInDB(): " + e.getMessage());
			}
		}
		_saved = true;
	}
	
	private boolean hasCategoryRows()
	{
		if (_storedCategories == null)
		{
			return false;
		}
		for (List<Integer> commands : _storedCategories.values())
		{
			if ((commands != null) && !commands.isEmpty())
			{
				return true;
			}
		}
		return false;
	}
	
	private boolean hasActionRows()
	{
		if (_storedKeys == null)
		{
			return false;
		}
		for (List<ActionKey> keys : _storedKeys.values())
		{
			if ((keys != null) && !keys.isEmpty())
			{
				return true;
			}
		}
		return false;
	}
	
	public void getCatsFromDB()
	{
		if (_storedCategories != null)
		{
			return;
		}
		
		_storedCategories = new HashMap<>();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT `catId`, `cmdId` FROM character_ui_categories WHERE `charId` = ? ORDER BY `catId`, `order`"))
		{
			ps.setInt(1, _playerObjId);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					UIData.addCategory(_storedCategories, rs.getInt("catId"), rs.getInt("cmdId"));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e, () -> "Exception: getCatsFromDB(): " + e.getMessage());
		}
		
		if (_storedCategories.isEmpty())
		{
			_storedCategories = UIData.getInstance().getCategories();
		}
	}
	
	public void getKeysFromDB()
	{
		if (_storedKeys != null)
		{
			return;
		}
		
		_storedKeys = new HashMap<>();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT `cat`, `cmd`, `key`, `tgKey1`, `tgKey2`, `show` FROM character_ui_actions WHERE `charId` = ? ORDER BY `cat`, `order`"))
		{
			ps.setInt(1, _playerObjId);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final int cat = rs.getInt("cat");
					UIData.addKey(_storedKeys, cat, new ActionKey(cat, rs.getInt("cmd"), rs.getInt("key"), rs.getInt("tgKey1"), rs.getInt("tgKey2"), rs.getInt("show")));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e, () -> "Exception: getKeysFromDB(): " + e.getMessage());
		}
		
		if (_storedKeys.isEmpty())
		{
			_storedKeys = UIData.getInstance().getKeys();
		}
	}
	
	public boolean isSaved()
	{
		return _saved;
	}
}
