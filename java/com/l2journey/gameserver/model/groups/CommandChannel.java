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
package com.l2journey.gameserver.model.groups;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.l2journey.Config;
import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.ExCloseMPCC;
import com.l2journey.gameserver.network.serverpackets.ExMPCCPartyInfoUpdate;
import com.l2journey.gameserver.network.serverpackets.ExOpenMPCC;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;

/**
 * This class serves as a container for command channels.
 * @author chris_00
 */
public class CommandChannel extends AbstractPlayerGroup
{
	private final Collection<Party> _parties = ConcurrentHashMap.newKeySet();
	private Player _commandLeader;
	private int _channelLvl;
	
	/**
	 * Create a new command channel and add the leader's party to it.
	 * @param leader the leader of this command channel
	 */
	public CommandChannel(Player leader)
	{
		_commandLeader = leader;
		final Party party = leader.getParty();
		_parties.add(party);
		_channelLvl = party.getLevel();
		party.setCommandChannel(this);
		party.broadcastMessage(SystemMessageId.THE_COMMAND_CHANNEL_HAS_BEEN_FORMED);
		party.broadcastPacket(ExOpenMPCC.STATIC_PACKET);
	}
	
	/**
	 * Add a party to this command channel.
	 * @param party the party to add
	 */
	public void addParty(Party party)
	{
		if (party == null)
		{
			return;
		}
		// Update the CCinfo for existing players
		broadcastPacket(new ExMPCCPartyInfoUpdate(party, 1));
		_parties.add(party);
		if (party.getLevel() > _channelLvl)
		{
			_channelLvl = party.getLevel();
		}
		party.setCommandChannel(this);
		party.broadcastPacket(new SystemMessage(SystemMessageId.YOU_HAVE_JOINED_THE_COMMAND_CHANNEL));
		party.broadcastPacket(ExOpenMPCC.STATIC_PACKET);
	}
	
	/**
	 * Remove a party from this command channel.
	 * @param party the party to remove
	 */
	public void removeParty(Party party)
	{
		if (party == null)
		{
			return;
		}
		
		_parties.remove(party);
		_channelLvl = 0;
		for (Party pty : _parties)
		{
			if (pty.getLevel() > _channelLvl)
			{
				_channelLvl = pty.getLevel();
			}
		}
		party.setCommandChannel(null);
		party.broadcastPacket(new ExCloseMPCC());
		if (_parties.size() < 2)
		{
			broadcastPacket(new SystemMessage(SystemMessageId.THE_COMMAND_CHANNEL_HAS_BEEN_DISBANDED));
			disbandChannel();
		}
		else
		{
			// Update the CCinfo for existing players
			broadcastPacket(new ExMPCCPartyInfoUpdate(party, 0));
		}
	}
	
	/**
	 * Disband this command channel.
	 */
	public void disbandChannel()
	{
		if (_parties != null)
		{
			for (Party party : _parties)
			{
				if (party != null)
				{
					removeParty(party);
				}
			}
			_parties.clear();
		}
	}
	
	/**
	 * @return the total count of all members of this command channel
	 */
	@Override
	public int getMemberCount()
	{
		int count = 0;
		for (Party party : _parties)
		{
			if (party != null)
			{
				count += party.getMemberCount();
			}
		}
		return count;
	}
	
	/**
	 * @return a list of all parties in this command channel
	 */
	public Collection<Party> getParties()
	{
		return _parties;
	}
	
	/**
	 * @return a list of all members in this command channel
	 */
	@Override
	public List<Player> getMembers()
	{
		final List<Player> members = new LinkedList<>();
		for (Party party : _parties)
		{
			members.addAll(party.getMembers());
		}
		return members;
	}
	
	/**
	 * @return the level of this command channel (equals the level of the highest-leveled character in this command channel)
	 */
	@Override
	public int getLevel()
	{
		return _channelLvl;
	}
	
	@Override
	public void setLeader(Player leader)
	{
		_commandLeader = leader;
		if (leader.getLevel() > _channelLvl)
		{
			_channelLvl = leader.getLevel();
		}
	}
	
	/**
	 * @param obj
	 * @return true if proper condition for RaidWar
	 */
	public boolean meetRaidWarCondition(WorldObject obj)
	{
		if (!(obj.isCreature() && obj.asCreature().isRaid()))
		{
			return false;
		}
		return (getMemberCount() >= Config.LOOT_RAIDS_PRIVILEGE_CC_SIZE);
	}
	
	/**
	 * @return the leader of this command channel
	 */
	@Override
	public Player getLeader()
	{
		return _commandLeader;
	}
	
	/**
	 * Check if a given player is in this command channel.
	 * @param player the player to check
	 * @return {@code true} if he does, {@code false} otherwise
	 */
	@Override
	public boolean containsPlayer(Player player)
	{
		if ((_parties != null) && !_parties.isEmpty())
		{
			for (Party party : _parties)
			{
				if (party.containsPlayer(player))
				{
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Iterates over all command channel members without the need to allocate a new list
	 * @see com.l2journey.gameserver.model.groups.AbstractPlayerGroup#forEachMember(Function)
	 */
	@Override
	public boolean forEachMember(Function<Player, Boolean> function)
	{
		if ((_parties != null) && !_parties.isEmpty())
		{
			for (Party party : _parties)
			{
				if (!party.forEachMember(function))
				{
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * Check whether this command channel's leader object id matches the other channel's leader object id.<br>
	 * A missing leader has object id {@code 0}, so two leaderless channels match.<br>
	 * Do not replace this with {@link Object#equals(Object)}: parties are hash keys in a command channel, and the leader can change.
	 * @param other the other command channel to check against
	 * @return {@code true} if both channels have the same leader object id, {@code false} otherwise
	 */
	public boolean hasSameLeader(CommandChannel other)
	{
		return (other != null) && (getLeaderObjectId() == other.getLeaderObjectId());
	}
}
