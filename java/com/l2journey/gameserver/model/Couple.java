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
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.managers.IdManager;
import com.l2journey.gameserver.model.actor.Player;

/**
 * @author evill33t
 */
public class Couple
{
	private static final Logger LOGGER = Logger.getLogger(Couple.class.getName());
	
	private int _id = 0;
	private int _player1Id = 0;
	private int _player2Id = 0;
	private boolean _maried = false;
	private Calendar _affiancedDate;
	private Calendar _weddingDate;
	
	public Couple(int coupleId)
	{
		_id = coupleId;
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT player1Id, player2Id, married, affianceDate, weddingDate FROM mods_wedding WHERE id = ?"))
		{
			ps.setInt(1, _id);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					_player1Id = rs.getInt("player1Id");
					_player2Id = rs.getInt("player2Id");
					_maried = Boolean.parseBoolean(rs.getString("married"));
					_affiancedDate = Calendar.getInstance();
					_affiancedDate.setTimeInMillis(rs.getLong("affianceDate"));
					
					_weddingDate = Calendar.getInstance();
					_weddingDate.setTimeInMillis(rs.getLong("weddingDate"));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, e, () -> "Exception: Couple.load(): " + e.getMessage());
		}
	}
	
	public Couple(Player player1, Player player2)
	{
		final long currentTime = System.currentTimeMillis();
		_player1Id = player1.getObjectId();
		_player2Id = player2.getObjectId();
		
		_affiancedDate = Calendar.getInstance();
		_affiancedDate.setTimeInMillis(currentTime);
		
		_weddingDate = Calendar.getInstance();
		_weddingDate.setTimeInMillis(currentTime);
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO mods_wedding (id, player1Id, player2Id, married, affianceDate, weddingDate) VALUES (?, ?, ?, ?, ?, ?)"))
		{
			_id = IdManager.getInstance().getNextId();
			ps.setInt(1, _id);
			ps.setInt(2, _player1Id);
			ps.setInt(3, _player2Id);
			ps.setBoolean(4, false);
			ps.setLong(5, _affiancedDate.getTimeInMillis());
			ps.setLong(6, _weddingDate.getTimeInMillis());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, e, () -> "Could not create couple: " + e.getMessage());
		}
	}
	
	public void marry()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE mods_wedding set married = ?, weddingDate = ? where id = ?"))
		{
			ps.setBoolean(1, true);
			_weddingDate = Calendar.getInstance();
			ps.setLong(2, _weddingDate.getTimeInMillis());
			ps.setInt(3, _id);
			ps.execute();
			_maried = true;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, e, () -> "Could not marry: " + e.getMessage());
		}
	}
	
	public void divorce()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM mods_wedding WHERE id=?"))
		{
			ps.setInt(1, _id);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, e, () -> "Exception: Couple.divorce(): " + e.getMessage());
		}
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getPlayer1Id()
	{
		return _player1Id;
	}
	
	public int getPlayer2Id()
	{
		return _player2Id;
	}
	
	public boolean getMaried()
	{
		return _maried;
	}
	
	public Calendar getAffiancedDate()
	{
		return _affiancedDate;
	}
	
	public Calendar getWeddingDate()
	{
		return _weddingDate;
	}
}
