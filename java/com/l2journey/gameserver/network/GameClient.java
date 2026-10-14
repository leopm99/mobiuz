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
package com.l2journey.gameserver.network;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.Config;
import com.l2journey.EventsConfig;
import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.commons.network.Buffer;
import com.l2journey.commons.network.Client;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.commons.util.TraceUtil;
import com.l2journey.gameserver.LoginServerThread;
import com.l2journey.gameserver.LoginServerThread.SessionKey;
import com.l2journey.gameserver.data.sql.CharInfoTable;
import com.l2journey.gameserver.data.sql.ClanTable;
import com.l2journey.gameserver.data.xml.SecondaryAuthData;
import com.l2journey.gameserver.model.CharSelectInfoPackage;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.clan.Clan;
import com.l2journey.gameserver.network.holders.ClientHardwareInfoHolder;
import com.l2journey.gameserver.network.serverpackets.LeaveWorld;
import com.l2journey.gameserver.network.serverpackets.ServerPacket;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;
import com.l2journey.gameserver.security.SecondaryPasswordAuth;
import com.l2journey.gameserver.util.FloodProtectors;

/**
 * Represents a client connected on GameServer.
 * @author KenM
 */
public class GameClient extends Client<com.l2journey.commons.network.Connection<GameClient>>
{
	private static final String DISCONNECTED = "disconnected";

	private static final Logger LOGGER = Logger.getLogger(GameClient.class.getName());
	private static final Logger LOGGER_ACCOUNTING = Logger.getLogger("accounting");
	
	private final FloodProtectors _floodProtectors = new FloodProtectors(this);
	private final ReentrantLock _playerLock = new ReentrantLock();
	private ConnectionState _connectionState = ConnectionState.CONNECTED;
	private Encryption _encryption = null;
	private String _ip = "N/A";
	private String _accountName;
	private SessionKey _sessionKey;
	private Player _player;
	private SecondaryPasswordAuth _secondaryAuth;
	private ClientHardwareInfoHolder _hardwareInfo;
	private List<CharSelectInfoPackage> _charSlotMapping = null;
	private volatile boolean _isDetached = false;
	private boolean _isAuthedGG;
	private boolean _protocolOk;
	private int _protocolVersion;
	private int[][] _trace;
	
	public GameClient(com.l2journey.commons.network.Connection<GameClient> connection)
	{
		super(connection);
		_ip = connection.getRemoteAddress();
	}
	
	@Override
	public void onConnected()
	{
		LOGGER_ACCOUNTING.finer(() -> "Client connected: " + _ip);
	}
	
	@Override
	public void onDisconnection()
	{
		LOGGER_ACCOUNTING.finer(() -> "Client disconnected: " + this);
		LoginServerThread.getInstance().sendLogout(_accountName);
		if ((_player == null) || !_player.isInOfflineMode())
		{
			Disconnection.of(this).onDisconnection();
		}
		_connectionState = ConnectionState.DISCONNECTED;
	}
	
	@Override
	public boolean encrypt(Buffer data, int offset, int size)
	{
		if (Config.PACKET_ENCRYPTION && (_encryption != null))
		{
			_encryption.encrypt(data, offset, size);
		}
		return true;
	}
	
	@Override
	public boolean decrypt(Buffer data, int offset, int size)
	{
		if (Config.PACKET_ENCRYPTION && (_encryption != null))
		{
			_encryption.decrypt(data, offset, size);
		}
		return true;
	}
	
	public void closeNow()
	{
		disconnect();
	}
	
	public void close(ServerPacket packet)
	{
		if (packet == null)
		{
			closeNow();
		}
		else
		{
			// Send the close packet.
			sendPacket(packet);
			
			// Wait for packet to be sent.
			ThreadPool.schedule(this::closeNow, 1000);
		}
	}
	
	public byte[] enableCrypt()
	{
		final byte[] key = BlowFishKeygen.getRandomKey();
		if (Config.PACKET_ENCRYPTION)
		{
			_encryption = new Encryption();
			_encryption.setKey(key);
		}
		return key;
	}
	
	public Player getPlayer()
	{
		return _player;
	}
	
	public void setPlayer(Player player)
	{
		_player = player;
	}
	
	public ReentrantLock getPlayerLock()
	{
		return _playerLock;
	}
	
	public FloodProtectors getFloodProtectors()
	{
		return _floodProtectors;
	}
	
	public void setGameGuardOk(boolean value)
	{
		_isAuthedGG = value;
	}
	
	public boolean isAuthedGG()
	{
		return _isAuthedGG;
	}
	
	public String getIp()
	{
		return _ip;
	}
	
	public void setAccountName(String accountName)
	{
		_accountName = accountName;
		if (SecondaryAuthData.getInstance().isEnabled())
		{
			_secondaryAuth = new SecondaryPasswordAuth(this);
		}
	}
	
	public String getAccountName()
	{
		return _accountName;
	}
	
	public void setSessionId(SessionKey sessionKey)
	{
		_sessionKey = sessionKey;
	}
	
	public SessionKey getSessionId()
	{
		return _sessionKey;
	}
	
	public void sendPacket(ServerPacket packet)
	{
		// Packet should never be null.
		if (packet == null)
		{
			LOGGER.warning(() -> TraceUtil.getStackTrace(new Exception()));
			return;
		}
		
		// Send the packet data.
		writePacket(packet);
		
		// Run packet implementation.
		packet.runImpl(_player);
	}
	
	public void sendPacket(SystemMessageId systemMessageId)
	{
		sendPacket(new SystemMessage(systemMessageId));
	}
	
	public boolean isDetached()
	{
		return _isDetached;
	}
	
	public void setDetached(boolean value)
	{
		_isDetached = value;
	}
	
	/**
	 * Method to handle character deletion
	 * @param characterSlot
	 * @return a byte:
	 *         <li>-1: Error: No char was found for such charslot, caught exception, etc...
	 *         <li>0: character is not member of any clan, proceed with deletion
	 *         <li>1: character is member of a clan, but not clan leader
	 *         <li>2: character is clan leader
	 */
	public byte markToDeleteChar(int characterSlot)
	{
		final int objectId = getObjectIdForSlot(characterSlot);
		if (objectId < 0)
		{
			return -1;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT clanId FROM characters WHERE charId=?"))
		{
			statement.setInt(1, objectId);
			byte answer = 0;
			try (ResultSet rs = statement.executeQuery())
			{
				final int clanId = rs.next() ? rs.getInt(1) : 0;
				if (clanId != 0)
				{
					final Clan clan = ClanTable.getInstance().getClan(clanId);
					if (clan == null)
					{
						answer = 0; // jeezes!
					}
					else if (clan.getLeaderId() == objectId)
					{
						answer = 2;
					}
					else
					{
						answer = 1;
					}
				}
				
				// Setting delete time
				if (answer == 0)
				{
					if (Config.DELETE_DAYS == 0)
					{
						deleteCharByObjId(objectId);
					}
					else
					{
						try (PreparedStatement ps2 = con.prepareStatement("UPDATE characters SET deletetime=? WHERE charId=?"))
						{
							ps2.setLong(1, System.currentTimeMillis() + (Config.DELETE_DAYS * 86400000)); // 24*60*60*1000 = 86400000
							ps2.setInt(2, objectId);
							ps2.execute();
						}
					}
					
					LOGGER_ACCOUNTING.info(() -> "Delete, " + objectId + ", " + this);
				}
			}
			return answer;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error updating delete time of character.", e);
			return -1;
		}
	}
	
	public void restore(int characterSlot)
	{
		final int objectId = getObjectIdForSlot(characterSlot);
		if (objectId < 0)
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET deletetime=0 WHERE charId=?"))
		{
			statement.setInt(1, objectId);
			statement.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error restoring character.", e);
		}
		
		LOGGER_ACCOUNTING.info(() -> "Restore, " + objectId + ", " + this);
	}
	
	public static void deleteCharByObjId(int objectId)
	{
		if (objectId < 0)
		{
			return;
		}
		
		CharInfoTable.getInstance().removeName(objectId);
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_contacts WHERE charId=? OR contactId=?"))
			{
				ps.setInt(1, objectId);
				ps.setInt(2, objectId);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_friends WHERE charId=? OR friendId=?"))
			{
				ps.setInt(1, objectId);
				ps.setInt(2, objectId);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_hennas WHERE charId=?"))
			{
				ps.setInt(1, objectId);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_macroses WHERE charId=?"))
			{
				ps.setInt(1, objectId);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_quests WHERE charId=?"))
			{
				ps.setInt(1, objectId);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_recipebook WHERE charId=?"))
			{
				ps.setInt(1, objectId);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_shortcuts WHERE charId=?"))
			{
				ps.setInt(1, objectId);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_skills WHERE charId=?"))
			{
				ps.setInt(1, objectId);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_skills_save WHERE charId=?"))
			{
				ps.setInt(1, objectId);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_subclasses WHERE charId=?"))
			{
				ps.setInt(1, objectId);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_summons WHERE ownerId=?"))
			{
				ps.setInt(1, objectId);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM heroes WHERE charId=?"))
			{
				ps.setInt(1, objectId);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM olympiad_nobles WHERE charId=?"))
			{
				ps.setInt(1, objectId);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM seven_signs WHERE charId=?"))
			{
				ps.setInt(1, objectId);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM pets WHERE item_obj_id IN (SELECT object_id FROM items WHERE items.owner_id=?)"))
			{
				ps.setInt(1, objectId);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM item_attributes WHERE itemId IN (SELECT object_id FROM items WHERE items.owner_id=?)"))
			{
				ps.setInt(1, objectId);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM items WHERE owner_id=?"))
			{
				ps.setInt(1, objectId);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM merchant_lease WHERE player_id=?"))
			{
				ps.setInt(1, objectId);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_raid_points WHERE charId=?"))
			{
				ps.setInt(1, objectId);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_reco_bonus WHERE charId=?"))
			{
				ps.setInt(1, objectId);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_instance_time WHERE charId=?"))
			{
				ps.setInt(1, objectId);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_variables WHERE charId=?"))
			{
				ps.setInt(1, objectId);
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM characters WHERE charId=?"))
			{
				ps.setInt(1, objectId);
				ps.execute();
			}
			
			if (EventsConfig.ALLOW_WEDDING)
			{
				try (PreparedStatement ps = con.prepareStatement("DELETE FROM mods_wedding WHERE player1Id = ? OR player2Id = ?"))
				{
					ps.setInt(1, objectId);
					ps.setInt(2, objectId);
					ps.execute();
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error deleting character.", e);
		}
	}
	
	public Player load(int characterSlot)
	{
		final int objectId = getObjectIdForSlot(characterSlot);
		if (objectId < 0)
		{
			return null;
		}
		
		Player player = World.getInstance().getPlayer(objectId);
		if (player != null)
		{
			// exploit prevention, should not happens in normal way
			if (player.isOnlineInt() == 1)
			{
				final Player loggedPlayer = player;
				LOGGER.severe(() -> "Attempt of double login: " + loggedPlayer.getName() + "(" + objectId + ") " + _accountName);
			}
			
			if (player.getClient() != null)
			{
				Disconnection.of(player).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
			}
			else
			{
				player.storeMe();
				player.deleteMe();
				
				// Rede de seguranca: se o player continuar registrado no mundo apos o
				// deleteMe() (ex.: deleteMe abortou antes do decayMe, ou ficou preso em
				// _allPlayers), forcamos a remocao para que o relogin nunca trave.
				if (World.getInstance().getPlayer(objectId) != null)
				{
					final Player loggedPlayer = player;
					LOGGER.warning(() -> "Ghost player still in world after deleteMe, forcing removal: " + loggedPlayer.getName() + "(" + objectId + ") " + _accountName);
					player.setTeleporting(false);
					player.decayMe();
				}
			}
			
			return null;
		}
		
		player = Player.load(objectId);
		if (player != null)
		{
			// prevent some values for each login
			player.setRunning(); // running is default
			player.standUp(); // standing is default
			player.refreshOverloaded();
			player.refreshExpertisePenalty();
		}
		else
		{
			LOGGER.severe(() -> "Could not restore in slot: " + characterSlot);
		}
		
		return player;
	}
	
	public void setCharSelection(List<CharSelectInfoPackage> characters)
	{
		_charSlotMapping = characters;
	}
	
	public CharSelectInfoPackage getCharSelection(int charslot)
	{
		if ((_charSlotMapping == null) || (charslot < 0) || (charslot >= _charSlotMapping.size()))
		{
			return null;
		}
		return _charSlotMapping.get(charslot);
	}
	
	public SecondaryPasswordAuth getSecondaryAuth()
	{
		return _secondaryAuth;
	}
	
	/**
	 * @param characterSlot
	 * @return
	 */
	private int getObjectIdForSlot(int characterSlot)
	{
		final CharSelectInfoPackage info = getCharSelection(characterSlot);
		if (info == null)
		{
			LOGGER.warning(() -> toString() + " tried to delete Character in slot " + characterSlot + " but no characters exits at that slot.");
			return -1;
		}
		return info.getObjectId();
	}
	
	public void setConnectionState(ConnectionState connectionState)
	{
		_connectionState = connectionState;
	}
	
	public ConnectionState getConnectionState()
	{
		return _connectionState;
	}
	
	public void setProtocolVersion(int version)
	{
		_protocolVersion = version;
	}
	
	public int getProtocolVersion()
	{
		return _protocolVersion;
	}
	
	public boolean isProtocolOk()
	{
		return _protocolOk;
	}
	
	public void setProtocolOk(boolean value)
	{
		_protocolOk = value;
	}
	
	public void setClientTracert(int[][] tracert)
	{
		_trace = tracert;
	}
	
	public int[][] getTrace()
	{
		return _trace;
	}
	
	public Encryption getEncryption()
	{
		return _encryption;
	}
	
	/**
	 * @return the hardwareInfo
	 */
	public ClientHardwareInfoHolder getHardwareInfo()
	{
		return _hardwareInfo;
	}
	
	/**
	 * @param hardwareInfo
	 */
	public void setHardwareInfo(ClientHardwareInfoHolder hardwareInfo)
	{
		_hardwareInfo = hardwareInfo;
	}
	
	/**
	 * Produces the best possible string representation of this client.
	 */
	@Override
	public String toString()
	{
		try
		{
			final String ip = getIp();
			final ConnectionState state = getConnectionState();
			switch (state)
			{
				case DISCONNECTED:
				{
					if (_accountName != null)
					{
						return "[Account: " + _accountName + " - IP: " + (ip == null ? DISCONNECTED : ip) + "]";
					}
					return "[IP: " + (ip == null ? DISCONNECTED : ip) + "]";
				}
				case CONNECTED:
				{
					return "[IP: " + (ip == null ? DISCONNECTED : ip) + "]";
				}
				case AUTHENTICATED:
				{
					return "[Account: " + _accountName + " - IP: " + (ip == null ? DISCONNECTED : ip) + "]";
				}
				case ENTERING, IN_GAME:
				{
					return "[Character: " + (_player == null ? DISCONNECTED : _player.getName() + "[" + _player.getObjectId() + "]") + " - Account: " + _accountName + " - IP: " + (ip == null ? DISCONNECTED : ip) + "]";
				}
				default:
				{
					throw new IllegalStateException("Missing state on switch.");
				}
			}
		}
		catch (NullPointerException _)
		{
			return "[Character read failed due to disconnect]";
		}
	}
}
