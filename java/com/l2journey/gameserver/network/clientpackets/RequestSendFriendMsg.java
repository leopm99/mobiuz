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

import java.util.logging.Logger;

import com.l2journey.Config;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.L2FriendSay;

/**
 * Recieve Private (Friend) Message - 0xCC Format: c SS S: Message S: Receiving Player
 * @author Tempy
 */
public class RequestSendFriendMsg extends ClientPacket
{
	private static Logger LOGGER_CHAT = Logger.getLogger("chat");
	
	private String _message;
	private String _reciever;
	
	@Override
	protected void readImpl()
	{
		_message = readString();
		_reciever = readString();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if ((player == null) || (_message == null) || _message.isEmpty() || (_message.length() > 300))
		{
			return;
		}
		
		final Player targetPlayer = World.getInstance().getPlayer(_reciever);
		if ((targetPlayer == null) || !targetPlayer.getFriendList().contains(player.getObjectId()))
		{
			player.sendPacket(SystemMessageId.THAT_PLAYER_IS_NOT_ONLINE);
			return;
		}
		
		if (Config.LOG_CHAT)
		{
			final StringBuilder sb = new StringBuilder();
			sb.append("PRIV_MSG [");
			sb.append(player);
			sb.append(" to ");
			sb.append(targetPlayer);
			sb.append("] ");
			sb.append(_message);
			LOGGER_CHAT.info(() -> sb.toString());
		}
		
		targetPlayer.sendPacket(new L2FriendSay(player.getName(), _reciever, _message));
	}
}