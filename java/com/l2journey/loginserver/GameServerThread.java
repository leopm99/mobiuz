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
package com.l2journey.loginserver;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.l2journey.Config;
import com.l2journey.commons.crypt.NewCrypt;
import com.l2journey.commons.network.base.BaseWritablePacket;
import com.l2journey.commons.util.TraceUtil;
import com.l2journey.loginserver.GameServerTable.GameServerInfo;
import com.l2journey.loginserver.network.GameServerPacketHandler;
import com.l2journey.loginserver.network.GameServerPacketHandler.GameServerState;
import com.l2journey.loginserver.network.ScrambledKeyPair;
import com.l2journey.loginserver.network.loginserverpackets.ChangePasswordResponse;
import com.l2journey.loginserver.network.loginserverpackets.InitLS;
import com.l2journey.loginserver.network.loginserverpackets.KickPlayer;
import com.l2journey.loginserver.network.loginserverpackets.LoginServerFail;
import com.l2journey.loginserver.network.loginserverpackets.RequestCharacters;

/**
 * @author -Wooden-
 * @author KenM
 */
public class GameServerThread extends Thread
{
	protected static final Logger LOGGER = Logger.getLogger(GameServerThread.class.getName());
	
	/** Authed Clients on GameServer */
	private final Set<String> _accountsOnGameServer = ConcurrentHashMap.newKeySet();
	
	private final Socket _socket;
	private InputStream _in;
	private OutputStream _out;
	private final RSAPublicKey _publicKey;
	private final RSAPrivateKey _privateKey;
	private NewCrypt _blowfish;
	private GameServerState _loginConnectionState = GameServerState.CONNECTED;
	private final String _connectionIp;
	private String _connectionIPAddress;
	private GameServerInfo _gsi;
	
	@Override
	public void run()
	{
		_connectionIPAddress = _socket.getInetAddress().getHostAddress();
		if (isBannedGameserverIP())
		{
			LOGGER.info(() -> "GameServerRegistration: IP Address " + _connectionIPAddress + " is on Banned IP list.");
			forceClose(LoginServerFail.REASON_IP_BANNED);
			// Ensure no further processing for this connection.
			return;
		}
		if (!isAllowedGameserverIP(_connectionIPAddress))
		{
			// GameServerAllowedHosts is an intentional, admin-maintained list (loginserver.ini).
			// No automatic ban is applied here - just a silent reject of this connection.
			LOGGER.fine(() -> "GameServerRegistration: IP Address " + _connectionIPAddress + " is not in the allowed GameServer host list, closing connection.");
			forceClose(LoginServerFail.REASON_IP_BANNED);
			return;
		}
		
		final InitLS startPacket = new InitLS(_publicKey.getModulus().toByteArray());
		try
		{
			sendPacket(startPacket);
			
			int lengthHi = 0;
			int lengthLo = 0;
			int length = 0;
			boolean checksumOk = false;
			for (;;)
			{
				lengthLo = _in.read();
				lengthHi = _in.read();
				length = (lengthHi * 256) + lengthLo;
				if ((lengthHi < 0) || _socket.isClosed())
				{
					LOGGER.finer("LoginServerThread: Login terminated the connection.");
					break;
				}
				
				final int dataLength = length - 2;
				// Body must hold at least one full Blowfish block, otherwise decrypt() reads out of bounds.
				if ((dataLength < 8) || ((dataLength % 8) != 0))
				{
					final int loggedLength = length;
					LOGGER.warning(() -> "Invalid packet loggedLength (" + loggedLength + ") sent to the server, closing connection.(LS)");
					break;
				}
				
				final byte[] data = new byte[dataLength];
				int receivedBytes = 0;
				int newBytes = 0;
				int left = dataLength;
				while ((newBytes != -1) && (receivedBytes < dataLength))
				{
					newBytes = _in.read(data, receivedBytes, left);
					receivedBytes += newBytes;
					left -= newBytes;
				}
				
				if (receivedBytes != dataLength)
				{
					LOGGER.warning("Incomplete Packet is sent to the server, closing connection.(LS)");
					break;
				}
				
				// Decrypt if we have a key.
				_blowfish.decrypt(data, 0, data.length);
				checksumOk = NewCrypt.verifyChecksum(data);
				if (!checksumOk)
				{
					LOGGER.warning("Incorrect packet checksum, closing connection (LS)");
					return;
				}
				
				GameServerPacketHandler.handlePacket(data, this);
			}
		}
		catch (IOException e)
		{
			final String serverName = getServerId() != -1 ? "[" + getServerId() + "] " + GameServerTable.getInstance().getServerNameById(getServerId()) : "(" + _connectionIPAddress + ")";
			final String msg = "GameServer " + serverName + ": Connection lost: " + e.getMessage();
			LOGGER.info(msg);
		}
		catch (RuntimeException e)
		{
			// Guards against malformed/unexpected packets so the thread closes cleanly instead of dying uncaught.
			final String serverName = getServerId() != -1 ? "[" + getServerId() + "] " + GameServerTable.getInstance().getServerNameById(getServerId()) : "(" + _connectionIPAddress + ")";
			LOGGER.warning("GameServer " + serverName + ": Unexpected error while processing packet, closing connection: " + e);
		}
		finally
		{
			if (isAuthed())
			{
				if (_gsi != null)
				{
					_gsi.setDown();
				}
				LOGGER.info(() -> "Server [" + getServerId() + "] " + GameServerTable.getInstance().getServerNameById(getServerId()) + " is now set as disconnected.");
			}
			LoginServer.getInstance().getGameServerListener().removeGameServer(this);
			LoginServer.getInstance().getGameServerListener().removeFloodProtection(_connectionIp);
		}
	}
	
	public boolean hasAccountOnGameServer(String account)
	{
		return _accountsOnGameServer.contains(account);
	}
	
	public int getPlayerCount()
	{
		return _accountsOnGameServer.size();
	}
	
	/**
	 * Attachs a GameServerInfo to this Thread<br>
	 * <ul>
	 * <li>Updates the GameServerInfo values based on GameServerAuth packet</li>
	 * <li><b>Sets the GameServerInfo as Authed</b></li>
	 * </ul>
	 * @param gsi The GameServerInfo to be attached.
	 * @param port
	 * @param hosts
	 * @param maxPlayers
	 */
	public void attachGameServerInfo(GameServerInfo gsi, int port, String[] hosts, int maxPlayers)
	{
		setGameServerInfo(gsi);
		gsi.setGameServerThread(this);
		gsi.setPort(port);
		setGameHosts(hosts);
		gsi.setMaxPlayers(maxPlayers);
		gsi.setAuthed(true);
	}
	
	public void forceClose(int reason)
	{
		sendPacket(new LoginServerFail(reason));
		
		try
		{
			_socket.close();
		}
		catch (IOException _)
		{
			LOGGER.finer("GameServerThread: Failed disconnecting banned server, server already disconnected.");
		}
	}
	
	/**
	 * @return
	 */
	public static boolean isBannedGameserverIP()
	{
		return false;
	}
	
	/**
	 * Checks the configured GameServer host allowlist (loginserver.ini, GameServerAllowedHosts).<br>
	 * When the list is empty, every IP is allowed (default, backwards-compatible behavior).
	 * @param ipAddress
	 * @return {@code true} if the IP may proceed to the registration handshake.
	 */
	public static boolean isAllowedGameserverIP(String ipAddress)
	{
		return Config.GAME_SERVER_ALLOWED_HOSTS.isEmpty() || Config.GAME_SERVER_ALLOWED_HOSTS.contains(ipAddress);
	}
	
	public GameServerThread(Socket con)
	{
		_socket = con;
		_connectionIp = con.getInetAddress().getHostAddress();
		
		try
		{
			_in = _socket.getInputStream();
			_out = new BufferedOutputStream(_socket.getOutputStream());
		}
		catch (IOException e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": " + e.getMessage());
		}
		
		final ScrambledKeyPair pair = LoginController.getInstance().getScrambledRSAKeyPair();
		_privateKey = (RSAPrivateKey) pair.getPrivateKey();
		_publicKey = (RSAPublicKey) pair.getPublicKey();
		_blowfish = new NewCrypt("_;v.]05-31!|+-%xT!^[$\00");
		// Java 18
		// Java 19
		setName(getClass().getSimpleName() + "-" + threadId() + "@" + _connectionIp);
		
		start();
	}
	
	public void sendPacket(BaseWritablePacket packet)
	{
		if ((_blowfish == null) || (_socket == null) || _socket.isClosed())
		{
			return;
		}
		
		try
		{
			packet.write(); // Write initial data.
			packet.writeInt(0); // Reserved for checksum.
			int size = packet.getLength() - 2; // Size without header.
			final int padding = size % 8; // Padding of 8 bytes.
			if (padding != 0)
			{
				for (int i = padding; i < 8; i++)
				{
					packet.writeByte(0);
				}
			}
			
			// Size header + encrypted[data + checksum (int) + padding].
			final byte[] data = packet.getSendableBytes();
			
			// Encrypt.
			size = data.length - 2; // Data size without header.
			
			synchronized (_out)
			{
				NewCrypt.appendChecksum(data, 2, size);
				_blowfish.crypt(data, 2, size);
				
				_out.write(data);
				try
				{
					_out.flush();
				}
				catch (IOException _)
				{
					// GameServer might have terminated.
				}
			}
		}
		catch (IOException e)
		{
			LOGGER.severe("GameServerThread: IOException while sending packet " + packet.getClass().getSimpleName());
			LOGGER.severe(TraceUtil.getStackTrace(e));
		}
	}
	
	public void kickPlayer(String account)
	{
		sendPacket(new KickPlayer(account));
	}
	
	public void requestCharacters(String account)
	{
		sendPacket(new RequestCharacters(account));
	}
	
	public void changePasswordResponse(String characterName, String msgToSend)
	{
		sendPacket(new ChangePasswordResponse(characterName, msgToSend));
	}
	
	/**
	 * @param hosts The gameHost to set.
	 */
	public void setGameHosts(String[] hosts)
	{
		LOGGER.info(() -> "Updated Gameserver [" + getServerId() + "] " + GameServerTable.getInstance().getServerNameById(getServerId()) + " IP's (connected from " + _connectionIPAddress + "):");
		_gsi.clearServerAddresses();
		for (int i = 0; i < hosts.length; i += 2)
		{
			try
			{
				_gsi.addServerAddress(hosts[i], hosts[i + 1]);
			}
			catch (Exception e)
			{
				LOGGER.warning("Couldn't resolve hostname \"" + e + "\"");
			}
		}
		
		for (String s : _gsi.getServerAddresses())
		{
			LOGGER.info(s);
		}
	}
	
	/**
	 * @return Returns if game server is authed.
	 */
	public boolean isAuthed()
	{
		if (_gsi == null)
		{
			return false;
		}
		return _gsi.isAuthed();
	}
	
	public void setGameServerInfo(GameServerInfo gsi)
	{
		_gsi = gsi;
	}
	
	public GameServerInfo getGameServerInfo()
	{
		return _gsi;
	}
	
	/**
	 * @return Returns the connectionIpAddress.
	 */
	public String getConnectionIpAddress()
	{
		return _connectionIPAddress;
	}
	
	public int getServerId()
	{
		if (_gsi != null)
		{
			return _gsi.getId();
		}
		return -1;
	}
	
	public RSAPrivateKey getPrivateKey()
	{
		return _privateKey;
	}
	
	public void setBlowFish(NewCrypt blowfish)
	{
		_blowfish = blowfish;
	}
	
	public void addAccountOnGameServer(String account)
	{
		_accountsOnGameServer.add(account);
		LoginController.getInstance().removeAuthedLoginClient(account);
	}
	
	public void removeAccountOnGameServer(String account)
	{
		_accountsOnGameServer.remove(account);
		LoginController.getInstance().removeAuthedLoginClient(account);
	}
	
	public GameServerState getLoginConnectionState()
	{
		return _loginConnectionState;
	}
	
	public void setLoginConnectionState(GameServerState state)
	{
		_loginConnectionState = state;
	}
}
