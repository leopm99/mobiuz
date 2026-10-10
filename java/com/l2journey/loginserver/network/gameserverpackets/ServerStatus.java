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
package com.l2journey.loginserver.network.gameserverpackets;

import java.util.logging.Logger;

import com.l2journey.commons.network.base.BaseReadablePacket;
import com.l2journey.loginserver.GameServerTable;
import com.l2journey.loginserver.GameServerTable.GameServerInfo;
import com.l2journey.loginserver.GameServerThread;

/**
 * @author -Wooden-
 */
public class ServerStatus extends BaseReadablePacket
{
	protected static final Logger LOGGER = Logger.getLogger(ServerStatus.class.getName());
	
	// Server list ids.
	public static final int SERVER_LIST_STATUS = 0x01;
	public static final int SERVER_TYPE = 0x02;
	public static final int SERVER_LIST_SQUARE_BRACKET = 0x03;
	public static final int MAX_PLAYERS = 0x04;
	public static final int TEST_SERVER = 0x05;
	public static final int SERVER_AGE = 0x06;
	
	// Server status.
	public static final int STATUS_AUTO = 0x00;
	public static final int STATUS_GOOD = 0x01;
	public static final int STATUS_NORMAL = 0x02;
	public static final int STATUS_FULL = 0x03;
	public static final int STATUS_DOWN = 0x04;
	public static final int STATUS_GM_ONLY = 0x05;
	
	// Server types.
	public static final int SERVER_NORMAL = 0x01;
	public static final int SERVER_RELAX = 0x02;
	public static final int SERVER_TEST = 0x04;
	public static final int SERVER_NOLABEL = 0x08;
	public static final int SERVER_CREATION_RESTRICTED = 0x10;
	public static final int SERVER_EVENT = 0x20;
	public static final int SERVER_FREE = 0x40;
	
	// Server ages.
	public static final int SERVER_AGE_ALL = 0x00;
	public static final int SERVER_AGE_15 = 0x0F;
	public static final int SERVER_AGE_18 = 0x12;
	
	public static final int ON = 0x01;
	public static final int OFF = 0x00;
	
	public ServerStatus(byte[] decrypt, GameServerThread server)
	{
		super(decrypt);
		readByte(); // Packet id, it is already processed.
		
		final GameServerInfo gsi = GameServerTable.getInstance().getRegisteredGameServerById(server.getServerId());
		if (gsi != null)
		{
			final int size = readInt();
			for (int i = 0; i < size; i++)
			{
				final int type = readInt();
				final int value = readInt();
				switch (type)
				{
					case SERVER_LIST_STATUS:
					{
						gsi.setStatus(value);
						break;
					}
					case SERVER_LIST_SQUARE_BRACKET:
					{
						gsi.setShowingBrackets(value == ON);
						break;
					}
					case MAX_PLAYERS:
					{
						gsi.setMaxPlayers(value);
						break;
					}
					case SERVER_TYPE:
					{
						gsi.setServerType(value);
						break;
					}
					case SERVER_AGE:
					{
						gsi.setAgeLimit(value);
						break;
					}
					default:
					{
						break;
					}
				}
			}
		}
	}
}
