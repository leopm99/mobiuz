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

import java.util.Arrays;
import java.util.logging.Logger;

import com.l2journey.commons.network.base.BaseReadablePacket;
import com.l2journey.loginserver.GameServerTable;
import com.l2journey.loginserver.GameServerTable.GameServerInfo;
import com.l2journey.loginserver.GameServerThread;
import com.l2journey.loginserver.network.GameServerPacketHandler.GameServerState;
import com.l2journey.loginserver.network.loginserverpackets.AuthResponse;
import com.l2journey.loginserver.network.loginserverpackets.LoginServerFail;

/**
 * <pre>
 * Format: cccddb
 * c ID desejado
 * c aceita ID alternativo
 * c reserva host
 * s ExternalHostName
 * s InetranlHostName
 * d maximo de jogadores
 * d tamanho do hexid
 * b hexid
 * </pre>
 * 
 * @author -Wooden-
 */
public class GameServerAuth extends BaseReadablePacket
{
	protected static final Logger LOGGER = Logger.getLogger(GameServerAuth.class.getName());
	
	GameServerThread _server;
	private final byte[] _hexId;
	private final int _desiredId;
	private final int _maxPlayers;
	private final int _port;
	private final String[] _hosts;
	
	public GameServerAuth(byte[] decrypt, GameServerThread server)
	{
		super(decrypt);
		readByte(); // Packet id, it is already processed.
		
		_server = server;
		_desiredId = readByte();
		readByte(); // acceptAlternativeId (discarded)
		readByte(); // hostReserved (discarded)
		_port = readShort();
		_maxPlayers = readInt();
		int size = readInt();
		_hexId = readBytes(size);
		size = 2 * readInt();
		_hosts = new String[size];
		for (int i = 0; i < size; i++)
		{
			_hosts[i] = readString();
		}
		
		if (handleRegProcess())
		{
			final AuthResponse ar = new AuthResponse(server.getGameServerInfo().getId());
			server.sendPacket(ar);
			server.setLoginConnectionState(GameServerState.AUTHED);
		}
	}
	
	private boolean handleRegProcess()
	{
		final GameServerTable gameServerTable = GameServerTable.getInstance();
		final int id = _desiredId;
		final byte[] hexId = _hexId;
		
		// Existe um gameserver registrado com este id?
		GameServerInfo gsi = gameServerTable.getRegisteredGameServerById(id);
		if ((gsi != null) && Arrays.equals(gsi.getHexId(), hexId))
		{
			return attachRegisteredGameServer(gsi);
		}
		
		final GameServerInfo registeredByHexId = gameServerTable.getRegisteredGameServerByHexId(hexId);
		if (registeredByHexId != null)
		{
			if (registeredByHexId.getId() != id)
			{
				LOGGER.warning("GameServer requested server id " + id + " but hexid is already registered on server id " + registeredByHexId.getId() + ". Reusing the existing registration.");
			}
			return attachRegisteredGameServer(registeredByHexId);
		}
		
		LOGGER.warning("GameServer attempted to register with unknown hexid (requested server id " + id + "). Rejecting.");
		_server.forceClose(LoginServerFail.REASON_WRONG_HEXID);
		return false;
	}
	
	private boolean attachRegisteredGameServer(GameServerInfo gsi)
	{
		synchronized (gsi)
		{
			if (gsi.isAuthed())
			{
				_server.forceClose(LoginServerFail.REASON_ALREADY_LOGGED8IN);
				return false;
			}
			_server.attachGameServerInfo(gsi, _port, _hosts, _maxPlayers);
		}
		return true;
	}
}
