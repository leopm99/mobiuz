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
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.model.Couple;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Player;

/**
 * @author evill33t
 */
public class CoupleManager
{
	private static final Logger LOGGER = Logger.getLogger(CoupleManager.class.getName());
	
	private final List<Couple> _couples = new CopyOnWriteArrayList<>();
	
	protected CoupleManager()
	{
		load();
	}
	
	public void reload()
	{
		_couples.clear();
		load();
	}
	
	private void load()
	{
		try (Connection con = DatabaseFactory.getConnection();
			Statement ps = con.createStatement();
			ResultSet rs = ps.executeQuery("SELECT id FROM mods_wedding ORDER BY id"))
		{
			while (rs.next())
			{
				_couples.add(new Couple(rs.getInt("id")));
			}
			LOGGER.info(() -> getClass().getSimpleName() + ": Loaded " + _couples.size() + " couples(s)");
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, e, () -> "Exception: CoupleManager.load(): " + e.getMessage());
		}
	}
	
	public Couple getCouple(int coupleId)
	{
		final int index = getCoupleIndex(coupleId);
		return index >= 0 ? _couples.get(index) : null;
	}
	
	public void createCouple(Player player1, Player player2)
	{
		if ((player1 == null) || (player2 == null) || (player1.getPartnerId() != 0) || (player2.getPartnerId() != 0))
		{
			return;
		}
		
		final int player1id = player1.getObjectId();
		final int player2id = player2.getObjectId();
		final Couple couple = new Couple(player1, player2);
		_couples.add(couple);
		player1.setPartnerId(player2id);
		player2.setPartnerId(player1id);
		player1.setCoupleId(couple.getId());
		player2.setCoupleId(couple.getId());
	}
	
	public void deleteCouple(int coupleId)
	{
		final int index = getCoupleIndex(coupleId);
		final Couple couple = _couples.get(index);
		if (couple == null)
		{
			return;
		}
		final Player player1 = World.getInstance().getPlayer(couple.getPlayer1Id());
		final Player player2 = World.getInstance().getPlayer(couple.getPlayer2Id());
		if (player1 != null)
		{
			player1.setPartnerId(0);
			player1.setMarried(false);
			player1.setCoupleId(0);
		}
		if (player2 != null)
		{
			player2.setPartnerId(0);
			player2.setMarried(false);
			player2.setCoupleId(0);
		}
		couple.divorce();
		_couples.remove(index);
	}
	
	public int getCoupleIndex(int coupleId)
	{
		int i = 0;
		for (Couple temp : _couples)
		{
			if ((temp != null) && (temp.getId() == coupleId))
			{
				return i;
			}
			i++;
		}
		return -1;
	}
	
	public List<Couple> getCouples()
	{
		return _couples;
	}
	
	public static CoupleManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CoupleManager INSTANCE = new CoupleManager();
	}
}
