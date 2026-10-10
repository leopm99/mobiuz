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

import com.l2journey.commons.network.WritableBuffer;
import com.l2journey.gameserver.model.groups.Party;
import com.l2journey.gameserver.network.GameClient;
import com.l2journey.gameserver.network.ServerPackets;

/**
 * @author chris_00
 */
public class ExMPCCPartyInfoUpdate extends ServerPacket
{
	private final int _mode;
	private final int _leaderOID;
	private final int _memberCount;
	private final String _name;
	
	/**
	 * @param party
	 * @param mode 0 = Remove, 1 = Add
	 */
	public ExMPCCPartyInfoUpdate(Party party, int mode)
	{
		_name = party.getLeader().getName();
		_leaderOID = party.getLeaderObjectId();
		_memberCount = party.getMemberCount();
		_mode = mode;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_MPCC_PARTY_INFO_UPDATE.writeId(this, buffer);
		buffer.writeString(_name);
		buffer.writeInt(_leaderOID);
		buffer.writeInt(_memberCount);
		buffer.writeInt(_mode); // mode 0 = Remove Party, 1 = AddParty, maybe more...
	}
}
