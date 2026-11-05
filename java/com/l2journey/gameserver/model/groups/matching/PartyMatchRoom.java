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
package com.l2journey.gameserver.model.groups.matching;

import java.util.ArrayList;
import java.util.List;

import com.l2journey.gameserver.managers.MapRegionManager;
import com.l2journey.gameserver.model.MapRegion;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.ExManagePartyRoomMember;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;

/**
 * @author Gnacik
 */
public class PartyMatchRoom
{
	private final int _id;
	private String _title;
	private int _loot;
	private int _minLevel;
	private int _maxLevel;
	private int _maxmem;
	private final List<Player> _members = new ArrayList<>();
	
	public PartyMatchRoom(int id, String title, int loot, int minLevel, int maxLevel, int maxmem, Player owner)
	{
		_id = id;
		_title = title;
		_loot = loot;
		_minLevel = minLevel;
		_maxLevel = maxLevel;
		_maxmem = maxmem;
		_members.add(owner);
	}
	
	public List<Player> getPartyMembers()
	{
		return _members;
	}
	
	public void addMember(Player player)
	{
		_members.add(player);
	}
	
	public void deleteMember(Player player)
	{
		if (player != getOwner())
		{
			_members.remove(player);
			notifyMembersAboutExit(player);
		}
		else if (_members.size() == 1)
		{
			PartyMatchRoomList.getInstance().deleteRoom(_id);
		}
		else
		{
			changeLeader(_members.get(1));
			deleteMember(player);
		}
	}
	
	public void notifyMembersAboutExit(Player player)
	{
		for (Player _member : _members)
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_LEFT_THE_PARTY_ROOM);
			sm.addString(player.getName());
			_member.sendPacket(sm);
			_member.sendPacket(new ExManagePartyRoomMember(player, this, 2));
		}
	}
	
	public void changeLeader(Player newLeader)
	{
		// Get current leader
		final Player oldLeader = _members.get(0);
		// Remove new leader
		_members.remove(newLeader);
		// Move him to first position
		_members.set(0, newLeader);
		// Add old leader as normal member
		_members.add(oldLeader);
		// Broadcast change
		for (Player member : _members)
		{
			member.sendPacket(new ExManagePartyRoomMember(newLeader, this, 1));
			member.sendPacket(new ExManagePartyRoomMember(oldLeader, this, 1));
			member.sendPacket(SystemMessageId.THE_LEADER_OF_THE_PARTY_ROOM_HAS_CHANGED);
		}
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getLootType()
	{
		return _loot;
	}
	
	public int getMinLevel()
	{
		return _minLevel;
	}
	
	public int getMaxLevel()
	{
		return _maxLevel;
	}
	
	/**
	 * <ul>
	 * <li>1 : Talking Island</li>
	 * <li>2 : Gludio</li>
	 * <li>3 : Dark Elven Ter.</li>
	 * <li>4 : Elven Territory</li>
	 * <li>5 : Dion</li>
	 * <li>6 : Giran</li>
	 * <li>7 : Neutral Zone</li>
	 * <li>8 : Lyonn</li>
	 * <li>9 : Schuttgart</li>
	 * <li>10 : Oren</li>
	 * <li>11 : Hunters Village</li>
	 * <li>12 : Innadril</li>
	 * <li>13 : Aden</li>
	 * <li>14 : Rune</li>
	 * <li>15 : Goddard</li>
	 * </ul>
	 * @return the id
	 */
	public int getLocation()
	{
		final MapRegion region = MapRegionManager.getInstance().getMapRegion(_members.get(0));
		return region == null ? 0 : region.getBbs();
	}
	
	public int getMembers()
	{
		return _members.size();
	}
	
	public int getMaxMembers()
	{
		return _maxmem;
	}
	
	public String getTitle()
	{
		return _title;
	}
	
	public Player getOwner()
	{
		return _members.get(0);
	}
	
	/* SET */
	
	public void setMinLevel(int minLevel)
	{
		_minLevel = minLevel;
	}
	
	public void setMaxLevel(int maxLevel)
	{
		_maxLevel = maxLevel;
	}
	
	public void setLootType(int loot)
	{
		_loot = loot;
	}
	
	public void setMaxMembers(int maxmem)
	{
		_maxmem = maxmem;
	}
	
	public void setTitle(String title)
	{
		_title = title;
	}
}