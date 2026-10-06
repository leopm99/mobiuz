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
package com.l2journey.gameserver.data.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.l2journey.Config;
import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.data.xml.PetDataTable;

public class PetNameTable
{
	private static final Logger LOGGER = Logger.getLogger(PetNameTable.class.getName());
	
	public static PetNameTable getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	public boolean doesPetNameExist(String name, int petNpcId)
	{
		boolean result = true;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT name FROM pets p, items i WHERE p.item_obj_id = i.object_id AND name=? AND i.item_id IN (?)"))
		{
			ps.setString(1, name);
			final StringBuilder cond = new StringBuilder();
			if (!cond.toString().isEmpty())
			{
				cond.append(", ");
			}
			
			cond.append(PetDataTable.getInstance().getPetItemsByNpc(petNpcId));
			ps.setString(2, cond.toString());
			try (ResultSet rs = ps.executeQuery())
			{
				result = rs.next();
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, e, () -> getClass().getSimpleName() + ": Could not check existing petname:" + e.getMessage());
		}
		return result;
	}
	
	public boolean isValidPetName(String name)
	{
		if (Config.PET_NAME_TEMPLATE.equals(".*"))
		{
			return true;
		}
		
		if (!isAlphaNumeric(name))
		{
			return false;
		}
		
		Pattern pattern;
		try
		{
			pattern = Pattern.compile(Config.PET_NAME_TEMPLATE);
		}
		catch (PatternSyntaxException _)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Pet name pattern in config is invalid!");
			return false;
		}
		
		return pattern.matcher(name).matches();
	}
	
	private boolean isAlphaNumeric(String text)
	{
		if ((text == null) || text.isEmpty())
		{
			return false;
		}
		
		for (char aChar : text.toCharArray())
		{
			if (!Character.isLetterOrDigit(aChar))
			{
				return false;
			}
		}
		return true;
	}
	
	private static class SingletonHolder
	{
		protected static final PetNameTable INSTANCE = new PetNameTable();
	}
}
