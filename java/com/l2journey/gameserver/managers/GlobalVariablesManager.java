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
package com.l2journey.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.model.variables.AbstractVariables;

public class GlobalVariablesManager extends AbstractVariables
{
	private static final Logger LOGGER = Logger.getLogger(GlobalVariablesManager.class.getName());
	
	// SQL Queries.
	private static final String SELECT_QUERY = "SELECT var, value FROM global_variables";
	private static final String DELETE_QUERY = "DELETE FROM global_variables";
	private static final String INSERT_QUERY = "REPLACE INTO global_variables (var, value) VALUES (?, ?)";
	
	protected GlobalVariablesManager()
	{
		restoreMe();
	}
	
	public boolean restoreMe()
	{
		// Restore previous variables.
		try (Connection con = DatabaseFactory.getConnection();
			Statement st = con.createStatement();
			ResultSet rset = st.executeQuery(SELECT_QUERY))
		{
			while (rset.next())
			{
				set(rset.getString("var"), rset.getString("value"));
			}
		}
		catch (SQLException _)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Couldn't restore global variables.");
			return false;
		}
		
		LOGGER.info(() -> getClass().getSimpleName() + ": Loaded " + getSet().size() + " variables.");
		return true;
	}
	
	public boolean storeMe()
	{
		try (Connection con = DatabaseFactory.getConnection();
			Statement del = con.createStatement();
			PreparedStatement st = con.prepareStatement(INSERT_QUERY))
		{
			// Clear previous entries.
			del.execute(DELETE_QUERY);
			
			// Insert all variables.
			for (Entry<String, Object> entry : getSet().entrySet())
			{
				st.setString(1, entry.getKey());
				st.setString(2, String.valueOf(entry.getValue()));
				st.addBatch();
			}
			st.executeBatch();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, e, () -> getClass().getSimpleName() + ": Couldn't save global variables to database.");
			return false;
		}
		
		LOGGER.info(() -> getClass().getSimpleName() + ": Stored " + getSet().size() + " variables.");
		return true;
	}
	
	public boolean deleteMe()
	{
		try (Connection con = DatabaseFactory.getConnection();
			Statement del = con.createStatement())
		{
			del.execute(DELETE_QUERY);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e, () -> getClass().getSimpleName() + ": Couldn't delete global variables to database.");
			return false;
		}
		return true;
	}
	
	/**
	 * Gets the single instance of {@code GlobalVariablesManager}.
	 * @return single instance of {@code GlobalVariablesManager}
	 */
	public static GlobalVariablesManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final GlobalVariablesManager INSTANCE = new GlobalVariablesManager();
	}
}
