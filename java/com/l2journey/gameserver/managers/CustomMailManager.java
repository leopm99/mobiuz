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
package com.l2journey.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.Config;
import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.commons.util.StringUtil;
import com.l2journey.gameserver.model.Message;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.item.holders.ItemEnchantHolder;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.itemcontainer.Mail;
import com.l2journey.gameserver.network.enums.MessageSenderType;

/**
 * @author Mobius
 */
public class CustomMailManager
{
	private static final Logger LOGGER = Logger.getLogger(CustomMailManager.class.getName());
	
	// SQL Statements
	private static final String READ_SQL = "SELECT receiver, items, subject, message, date FROM custom_mail";
	private static final String DELETE_SQL = "DELETE FROM custom_mail WHERE date=? AND receiver=?";
	
	protected CustomMailManager()
	{
		ThreadPool.scheduleAtFixedRate(() ->
		{
			try (Connection con = DatabaseFactory.getConnection();
				Statement ps = con.createStatement();
				ResultSet rs = ps.executeQuery(READ_SQL))
			{
				while (rs.next())
				{
					final int playerId = rs.getInt("receiver");
					final Player player = World.getInstance().getPlayer(playerId);
					if ((player != null) && player.isOnline())
					{
						// Create message.
						final String items = rs.getString("items");
						final Message msg = new Message(playerId, rs.getString("subject"), rs.getString("message"), MessageSenderType.PLAYER);
						final List<ItemEnchantHolder> itemHolders = new ArrayList<>();
						for (String str : items.split(";"))
						{
							if (str.contains(" "))
							{
								final String[] split = str.split(" ");
								final String itemId = split[0];
								final String itemCount = split[1];
								final String enchant = split.length > 2 ? split[2] : "0";
								if (StringUtil.isNumeric(itemId) && StringUtil.isNumeric(itemCount))
								{
									itemHolders.add(new ItemEnchantHolder(Integer.parseInt(itemId), Long.parseLong(itemCount), Integer.parseInt(enchant)));
								}
							}
							else if (StringUtil.isNumeric(str))
							{
								itemHolders.add(new ItemEnchantHolder(Integer.parseInt(str), 1));
							}
						}
						if (!itemHolders.isEmpty())
						{
							final Mail attachments = msg.createAttachments();
							for (ItemEnchantHolder itemHolder : itemHolders)
							{
								final Item item = attachments.addItem(ItemProcessType.TRANSFER, itemHolder.getId(), itemHolder.getCount(), null, null);
								if (itemHolder.getEnchantLevel() > 0)
								{
									item.setEnchantLevel(itemHolder.getEnchantLevel());
								}
							}
						}
						
						// Delete entry from database.
						try (PreparedStatement stmt = con.prepareStatement(DELETE_SQL))
						{
							stmt.setString(1, rs.getString("date"));
							stmt.setInt(2, playerId);
							stmt.execute();
						}
						catch (SQLException e)
						{
							LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Error deleting entry from database: ", e);
						}
						
						// Send message.
						MailManager.getInstance().sendMessage(msg);
						LOGGER.info(getClass().getSimpleName() + ": Message sent to " + player.getName() + ".");
					}
				}
			}
			catch (SQLException e)
			{
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Error reading from database: ", e);
			}
		}, Config.CUSTOM_MAIL_MANAGER_DELAY, Config.CUSTOM_MAIL_MANAGER_DELAY);
		
		LOGGER.info(getClass().getSimpleName() + ": Enabled.");
	}
	
	public static CustomMailManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CustomMailManager INSTANCE = new CustomMailManager();
	}
}
