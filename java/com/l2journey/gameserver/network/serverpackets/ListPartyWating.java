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
package com.l2journey.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;

import com.l2journey.commons.network.WritableBuffer;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.groups.matching.PartyMatchRoom;
import com.l2journey.gameserver.model.groups.matching.PartyMatchRoomList;
import com.l2journey.gameserver.network.GameClient;
import com.l2journey.gameserver.network.ServerPackets;

/**
 * @author Gnacik
 */
public class ListPartyWating extends ServerPacket
{
	private final Player _player;
	private final int _loc;
	private final int _lim;
	private final List<PartyMatchRoom> _rooms;
	
	public ListPartyWating(Player player, int location, int limit)
	{
		_player = player;
		_loc = location;
		_lim = limit;
		_rooms = new ArrayList<>();
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		for (PartyMatchRoom room : PartyMatchRoomList.getInstance().getRooms())
		{
			if ((room.getMembers() < 1) || (room.getOwner() == null) || !room.getOwner().isOnline() || (room.getOwner().getPartyRoom() != room.getId()))
			{
				PartyMatchRoomList.getInstance().deleteRoom(room.getId());
				continue;
			}
			if ((_loc > 0) && (_loc != room.getLocation()))
			{
				continue;
			}
			if ((_lim == 0) && ((_player.getLevel() < room.getMinLevel()) || (_player.getLevel() > room.getMaxLevel())))
			{
				continue;
			}
			_rooms.add(room);
		}
		final int size = _rooms.size();
		ServerPackets.LIST_PARTY_WAITING.writeId(this, buffer);
		if (size > 0)
		{
			buffer.writeInt(1);
		}
		else
		{
			buffer.writeInt(0);
		}
		buffer.writeInt(_rooms.size());
		for (PartyMatchRoom room : _rooms)
		{
			buffer.writeInt(room.getId());
			buffer.writeString(room.getTitle());
			buffer.writeInt(room.getLocation());
			buffer.writeInt(room.getMinLevel());
			buffer.writeInt(room.getMaxLevel());
			buffer.writeInt(room.getMaxMembers());
			buffer.writeString(room.getOwner().getName());
			buffer.writeInt(room.getMembers());
			for (Player member : room.getPartyMembers())
			{
				if (member != null)
				{
					buffer.writeInt(member.getPlayerClass().getId());
					buffer.writeString(member.getName());
				}
				else
				{
					buffer.writeInt(0);
					buffer.writeString("Not Found");
				}
			}
		}
	}
}
