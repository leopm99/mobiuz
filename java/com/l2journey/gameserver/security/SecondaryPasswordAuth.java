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
package com.l2journey.gameserver.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.commons.util.StringUtil;
import com.l2journey.gameserver.LoginServerThread;
import com.l2journey.gameserver.data.xml.SecondaryAuthData;
import com.l2journey.gameserver.network.Disconnection;
import com.l2journey.gameserver.network.GameClient;
import com.l2journey.gameserver.network.serverpackets.Ex2ndPasswordAck;
import com.l2journey.gameserver.network.serverpackets.Ex2ndPasswordCheck;
import com.l2journey.gameserver.network.serverpackets.Ex2ndPasswordVerify;
import com.l2journey.gameserver.network.serverpackets.LeaveWorld;

/**
 * @author mrTJO
 */
public class SecondaryPasswordAuth
{
	private static final Logger LOGGER = Logger.getLogger(SecondaryPasswordAuth.class.getName());
	private final GameClient _activeClient;
	
	private String _password;
	private int _wrongAttempts;
	private boolean _authed;
	
	private static final String VAR_PWD = "secauth_pwd";
	private static final String VAR_WTE = "secauth_wte";
	
	private static final String SELECT_PASSWORD = "SELECT var, value FROM account_gsdata WHERE account_name=? AND var LIKE 'secauth_%'";
	private static final String INSERT_PASSWORD = "INSERT INTO account_gsdata VALUES (?, ?, ?)";
	private static final String UPDATE_PASSWORD = "UPDATE account_gsdata SET value=? WHERE account_name=? AND var=?";
	
	private static final String INSERT_ATTEMPT = "INSERT INTO account_gsdata VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE value=?";
	
	/**
	 * @param activeClient
	 */
	public SecondaryPasswordAuth(GameClient activeClient)
	{
		_activeClient = activeClient;
		_password = null;
		_wrongAttempts = 0;
		_authed = false;
		loadPassword();
	}
	
	private void loadPassword()
	{
		String var = null;
		String value = null;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(SELECT_PASSWORD))
		{
			statement.setString(1, _activeClient.getAccountName());
			try (ResultSet rs = statement.executeQuery())
			{
				while (rs.next())
				{
					var = rs.getString("var");
					value = rs.getString("value");
					if (var.equals(VAR_PWD))
					{
						_password = value;
					}
					else if (var.equals(VAR_WTE))
					{
						_wrongAttempts = Integer.parseInt(value);
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error while reading password.", e);
		}
	}
	
	public boolean savePassword(String value)
	{
		if (passwordExist())
		{
			LOGGER.warning("[SecondaryPasswordAuth]" + _activeClient.getAccountName() + " forced savePassword");
			Disconnection.of(_activeClient).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
			return false;
		}
		
		if (!validatePassword(value))
		{
			_activeClient.sendPacket(new Ex2ndPasswordAck(Ex2ndPasswordAck.WRONG_PATTERN));
			return false;
		}
		
		final String password = cryptPassword(value);
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(INSERT_PASSWORD))
		{
			statement.setString(1, _activeClient.getAccountName());
			statement.setString(2, VAR_PWD);
			statement.setString(3, password);
			statement.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error while writing password.", e);
			return false;
		}
		_password = password;
		return true;
	}
	
	public boolean insertWrongAttempt(int attempts)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(INSERT_ATTEMPT))
		{
			statement.setString(1, _activeClient.getAccountName());
			statement.setString(2, VAR_WTE);
			statement.setString(3, Integer.toString(attempts));
			statement.setString(4, Integer.toString(attempts));
			statement.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error while writing wrong attempts.", e);
			return false;
		}
		return true;
	}
	
	public boolean changePassword(String oldPassword, String newPassword)
	{
		if (!passwordExist())
		{
			LOGGER.warning("[SecondaryPasswordAuth]" + _activeClient.getAccountName() + " forced changePassword");
			Disconnection.of(_activeClient).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
			return false;
		}
		
		if (!checkPassword(oldPassword, true))
		{
			return false;
		}
		
		if (!validatePassword(newPassword))
		{
			_activeClient.sendPacket(new Ex2ndPasswordAck(Ex2ndPasswordAck.WRONG_PATTERN));
			return false;
		}
		
		final String password = cryptPassword(newPassword);
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(UPDATE_PASSWORD))
		{
			statement.setString(1, password);
			statement.setString(2, _activeClient.getAccountName());
			statement.setString(3, VAR_PWD);
			statement.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error while reading password.", e);
			return false;
		}
		
		_password = password;
		_authed = false;
		return true;
	}
	
	public boolean checkPassword(String value, boolean skipAuth)
	{
		final String password = cryptPassword(value);
		if (!password.equals(_password))
		{
			_wrongAttempts++;
			if (_wrongAttempts < SecondaryAuthData.getInstance().getMaxAttempts())
			{
				_activeClient.sendPacket(new Ex2ndPasswordVerify(Ex2ndPasswordVerify.PASSWORD_WRONG, _wrongAttempts));
				insertWrongAttempt(_wrongAttempts);
			}
			else
			{
				LoginServerThread.getInstance().sendTempBan(_activeClient.getAccountName(), _activeClient.getIp(), SecondaryAuthData.getInstance().getBanTime());
				LoginServerThread.getInstance().sendMail(_activeClient.getAccountName(), "SATempBan", _activeClient.getIp(), Integer.toString(SecondaryAuthData.getInstance().getMaxAttempts()), Long.toString(SecondaryAuthData.getInstance().getBanTime()), SecondaryAuthData.getInstance().getRecoveryLink());
				LOGGER.warning(() -> _activeClient.getAccountName() + " - (" + _activeClient.getIp() + ") has inputted the wrong password " + _wrongAttempts + " times in row.");
				insertWrongAttempt(0);
				_activeClient.close(new Ex2ndPasswordVerify(Ex2ndPasswordVerify.PASSWORD_BAN, SecondaryAuthData.getInstance().getMaxAttempts()));
			}
			return false;
		}
		if (!skipAuth)
		{
			_authed = true;
			_activeClient.sendPacket(new Ex2ndPasswordVerify(Ex2ndPasswordVerify.PASSWORD_OK, _wrongAttempts));
		}
		insertWrongAttempt(0);
		return true;
	}
	
	public boolean passwordExist()
	{
		return _password != null;
	}
	
	public void openDialog()
	{
		if (passwordExist())
		{
			_activeClient.sendPacket(new Ex2ndPasswordCheck(Ex2ndPasswordCheck.PASSWORD_PROMPT));
		}
		else
		{
			_activeClient.sendPacket(new Ex2ndPasswordCheck(Ex2ndPasswordCheck.PASSWORD_NEW));
		}
	}
	
	public boolean isAuthed()
	{
		return _authed;
	}
	
	private String cryptPassword(String password)
	{
		try
		{
			final MessageDigest md = MessageDigest.getInstance("SHA");
			final byte[] raw = password.getBytes(StandardCharsets.UTF_8);
			final byte[] hash = md.digest(raw);
			return Base64.getEncoder().encodeToString(hash);
		}
		catch (NoSuchAlgorithmException _)
		{
			LOGGER.severe("[SecondaryPasswordAuth]Unsupported Algorythm");
		}
		return null;
	}
	
	private boolean validatePassword(String password)
	{
		if (!StringUtil.isNumeric(password) || (password.length() < 6) || (password.length() > 8))
		{
			return false;
		}
		
		return !SecondaryAuthData.getInstance().isForbiddenPassword(password);
	}
}