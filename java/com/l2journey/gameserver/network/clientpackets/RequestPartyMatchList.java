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
package com.l2journey.gameserver.network.clientpackets;

import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.groups.matching.PartyMatchRoom;
import com.l2journey.gameserver.model.groups.matching.PartyMatchRoomList;
import com.l2journey.gameserver.model.groups.matching.PartyMatchWaitingList;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.ExPartyRoomMember;
import com.l2journey.gameserver.network.serverpackets.PartyMatchDetail;

/**
 * @author Gnacik
 */
public class RequestPartyMatchList extends ClientPacket
{
	private int _roomid;
	private int _membersmax;
	private int _minLevel;
	private int _maxLevel;
	private int _loot;
	private String _roomtitle;
	
	@Override
	protected void readImpl()
	{
		_roomid = readInt();
		_membersmax = readInt();
		_minLevel = readInt();
		_maxLevel = readInt();
		_loot = readInt();
		_roomtitle = readString();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (_roomid > 0)
		{
			final PartyMatchRoom room = PartyMatchRoomList.getInstance().getRoom(_roomid);
			if (room != null)
			{
				room.setMaxMembers(_membersmax);
				room.setMinLevel(_minLevel);
				room.setMaxLevel(_maxLevel);
				room.setLootType(_loot);
				room.setTitle(_roomtitle);
				
				for (Player member : room.getPartyMembers())
				{
					if (member == null)
					{
						continue;
					}
					
					member.sendPacket(new PartyMatchDetail(room));
					member.sendPacket(SystemMessageId.THE_PARTY_ROOM_S_INFORMATION_HAS_BEEN_REVISED);
				}
			}
		}
		else
		{
			final int maxid = PartyMatchRoomList.getInstance().getMaxId();
			final PartyMatchRoom room = new PartyMatchRoom(maxid, _roomtitle, _loot, _minLevel, _maxLevel, _membersmax, player);
			
			// Remove from waiting list
			PartyMatchWaitingList.getInstance().removePlayer(player);
			
			PartyMatchRoomList.getInstance().addPartyMatchRoom(maxid, room);
			if (player.isInParty())
			{
				for (Player ptmember : player.getParty().getMembers())
				{
					if ((ptmember == null) || (ptmember == player))
					{
						continue;
					}
					
					ptmember.setPartyRoom(maxid);
					room.addMember(ptmember);
				}
			}
			player.sendPacket(new PartyMatchDetail(room));
			player.sendPacket(new ExPartyRoomMember(room, 1));
			player.sendPacket(SystemMessageId.YOU_HAVE_CREATED_A_PARTY_ROOM);
			
			player.setPartyRoom(maxid);
			player.broadcastUserInfo();
		}
	}
}