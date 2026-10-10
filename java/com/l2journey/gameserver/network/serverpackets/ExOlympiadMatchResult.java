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

import java.util.List;

import com.l2journey.commons.network.WritableBuffer;
import com.l2journey.gameserver.model.olympiad.OlympiadInfo;
import com.l2journey.gameserver.network.GameClient;
import com.l2journey.gameserver.network.ServerPackets;

/**
 * @author JIV
 */
public class ExOlympiadMatchResult extends ServerPacket
{
	private static final int MESSAGE_TYPE = 1; // 0 = Match List; 1 = Match Result.
	private final boolean _isTie; // 0 - win, 1 - tie.
	private final int _winTeam;
	private final List<OlympiadInfo> _winnerList;
	private final List<OlympiadInfo> _loserList;
	private final int _loseTeam;
	
	/**
	 * Olympiad match result packet implementation.
	 * @param tie if false, there's a winner; if true, then it's a tie.
	 * @param winTeam 0 == nobody won; 1 == team one won; 2 == team two won.
	 * @param winnerList list of winning players.
	 * @param loserList list of losing players.
	 */
	public ExOlympiadMatchResult(boolean tie, int winTeam, List<OlympiadInfo> winnerList, List<OlympiadInfo> loserList)
	{
		_isTie = tie;
		_winTeam = winTeam;
		_winnerList = winnerList;
		_loserList = loserList;
		_loseTeam = winTeam == 2 ? 1 : 2;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_RECEIVE_OLYMPIAD.writeId(this, buffer);
		buffer.writeInt(MESSAGE_TYPE);
		buffer.writeInt(_isTie);
		buffer.writeString(_isTie || _winnerList.isEmpty() ? "nobody" : _winnerList.get(0).getName());
		buffer.writeInt(_winTeam);
		buffer.writeInt(_winnerList.size());
		for (OlympiadInfo info : _winnerList)
		{
			buffer.writeString(info.getName());
			buffer.writeString(info.getClanName());
			buffer.writeInt(info.getClanId());
			buffer.writeInt(info.getClassId());
			buffer.writeInt(info.getDamage());
			buffer.writeInt(info.getCurrentPoints());
			buffer.writeInt(info.getDiffPoints());
		}
		buffer.writeInt(_loseTeam);
		buffer.writeInt(_loserList.size());
		for (OlympiadInfo info : _loserList)
		{
			buffer.writeString(info.getName());
			buffer.writeString(info.getClanName());
			buffer.writeInt(info.getClanId());
			buffer.writeInt(info.getClassId());
			buffer.writeInt(info.getDamage());
			buffer.writeInt(info.getCurrentPoints());
			buffer.writeInt(info.getDiffPoints());
		}
	}
}
