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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.l2journey.Config;
import com.l2journey.commons.database.DatabaseBackup;
import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.commons.enums.ServerMode;
import com.l2journey.commons.network.ConnectionManager;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.network.loginserverpackets.game.ServerStatus;
import com.l2journey.loginserver.network.LoginClient;
import com.l2journey.loginserver.network.LoginPacketHandler;

/**
 * @author KenM
 */
public class LoginServer
{
	public static final Logger LOGGER = Logger.getLogger(LoginServer.class.getName());
	
	public static final int PROTOCOL_REV = 0x0106;
	
	private static LoginServer instance;
	private GameServerListener _gameServerListener;
	private static int loginStatus = ServerStatus.STATUS_NORMAL;
	
	private LoginServer() throws Exception
	{
		// Create log folder.
		final File logFolder = new File(".", "log");
		logFolder.mkdir();
		
		// Create input stream for log file -- or store file data into memory.
		try (InputStream is = new FileInputStream(new File("./log.cfg")))
		{
			LogManager.getLogManager().readConfiguration(is);
		}
		catch (IOException e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": " + e.getMessage());
		}
		
		// Load Config.
		Config.load(ServerMode.LOGIN);
		
		// Prepare the database.
		DatabaseFactory.init();
		
		// Initialize ThreadPool.
		ThreadPool.init();
		
		try
		{
			LoginController.load();
		}
		catch (GeneralSecurityException e)
		{
			LOGGER.log(Level.SEVERE, e, () -> "FATAL: Failed initializing LoginController. Reason: " + e.getMessage());
			System.exit(1);
		}
		
		GameServerTable.getInstance();
		
		loadBanFile();
		
		if (Config.LOGIN_SERVER_SCHEDULE_RESTART)
		{
			LOGGER.info(() -> "Scheduled LS restart after " + Config.LOGIN_SERVER_SCHEDULE_RESTART_TIME + " hours.");
			ThreadPool.schedule(() -> shutdown(true), Config.LOGIN_SERVER_SCHEDULE_RESTART_TIME * 3600000);
		}
		
		try
		{
			_gameServerListener = new GameServerListener();
			_gameServerListener.start();
			LOGGER.info(() -> "Listening for GameServers on " + Config.GAME_SERVER_LOGIN_HOST + ":" + Config.GAME_SERVER_LOGIN_PORT);
		}
		catch (IOException e)
		{
			LOGGER.log(Level.SEVERE, e, () -> "FATAL: Failed to start the Game Server Listener. Reason: " + e.getMessage());
			System.exit(1);
		}
		
		new ConnectionManager<>(new InetSocketAddress(Config.LOGIN_BIND_ADDRESS, Config.PORT_LOGIN), LoginClient::new, new LoginPacketHandler());
		LOGGER.info(() -> getClass().getSimpleName() + ": is now listening on: " + Config.LOGIN_BIND_ADDRESS + ":" + Config.PORT_LOGIN);
	}
	
	public GameServerListener getGameServerListener()
	{
		return _gameServerListener;
	}
	
	public void loadBanFile()
	{
		final File bannedFile = new File("./banned_ip.cfg");
		if (bannedFile.exists() && bannedFile.isFile())
		{
			try (FileInputStream fis = new FileInputStream(bannedFile);
				InputStreamReader is = new InputStreamReader(fis);
				LineNumberReader lnr = new LineNumberReader(is))
			{
				lnr.lines().map(String::trim).filter(l -> !l.isEmpty() && (l.charAt(0) != '#')).forEach(lineValue ->
				{
					String line = lineValue;
					String[] parts = line.split("#", 2); // address[ duration][ # comments]
					line = parts[0];
					parts = line.split("\\s+"); // Durations might be aligned via multiple spaces.
					final String address = parts[0];
					long duration = 0;
					if (parts.length > 1)
					{
						try
						{
							duration = Long.parseLong(parts[1]);
						}
						catch (NumberFormatException _)
						{
							LOGGER.warning("Skipped: Incorrect ban duration (" + parts[1] + ") on (" + bannedFile.getName() + "). Line: " + lnr.getLineNumber());
							return;
						}
					}
					
					try
					{
						LoginController.getInstance().addBanForAddress(address, duration);
					}
					catch (Exception _)
					{
						LOGGER.warning("Skipped: Invalid address (" + address + ") on (" + bannedFile.getName() + "). Line: " + lnr.getLineNumber());
					}
				});
			}
			catch (IOException e)
			{
				LOGGER.log(Level.WARNING, e, () -> "Error while reading the bans file (" + bannedFile.getName() + "). Details: " + e.getMessage());
			}
			LOGGER.info(() -> "Loaded " + LoginController.getInstance().getBannedIps().size() + " IP Bans.");
		}
		else
		{
			LOGGER.warning("IP Bans file (" + bannedFile.getName() + ") is missing or is a directory, skipped.");
		}
	}
	
	public void shutdown(boolean restart)
	{
		if (Config.BACKUP_DATABASE)
		{
			DatabaseBackup.performBackup();
		}
		Runtime.getRuntime().exit(restart ? 2 : 0);
	}
	
	public int getStatus()
	{
		return loginStatus;
	}
	
	public void setStatus(int status)
	{
		loginStatus = status;
	}
	
	public static LoginServer getInstance()
	{
		return instance;
	}
	
	public static void main(String[] args) throws Exception
	{
		instance = new LoginServer();
	}
}
