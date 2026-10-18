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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.model.Message;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.network.serverpackets.ExNoticePostArrived;
import com.l2journey.gameserver.taskmanagers.MessageDeletionTaskManager;

/**
 * @author Migi, DS
 */
public class MailManager
{
	private static final Logger LOGGER = Logger.getLogger(MailManager.class.getName());
	
	private final Map<Integer, Message> _messages = new ConcurrentHashMap<>();
	
	protected MailManager()
	{
		load();
	}
	
	private void load()
	{
		int count = 0;
		try (Connection con = DatabaseFactory.getConnection();
			Statement ps = con.createStatement();
			ResultSet rs = ps.executeQuery("SELECT `messageId`, `senderId`, `receiverId`, `subject`, `content`, `expiration`, `reqAdena`, `hasAttachments`, `isUnread`, `isDeletedBySender`, `isDeletedByReceiver`, `sendBySystem`, `isReturned` FROM messages ORDER BY expiration"))
		{
			while (rs.next())
			{
				count++;
				final Message msg = new Message(rs);
				final int msgId = msg.getId();
				_messages.put(msgId, msg);
				MessageDeletionTaskManager.getInstance().add(msgId, msg.getExpiration());
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, e, () -> getClass().getSimpleName() + ": Error loading from database:");
		}
		final int loggedCount = count;
		LOGGER.info(() -> getClass().getSimpleName() + ": Loaded " + loggedCount + " messages.");
	}
	
	public Message getMessage(int msgId)
	{
		return _messages.get(msgId);
	}
	
	public Collection<Message> getMessages()
	{
		return _messages.values();
	}
	
	public boolean hasUnreadPost(Player player)
	{
		final int objectId = player.getObjectId();
		for (Message msg : _messages.values())
		{
			if ((msg != null) && (msg.getReceiverId() == objectId) && msg.isUnread())
			{
				return true;
			}
		}
		return false;
	}
	
	public int getInboxSize(int objectId)
	{
		int size = 0;
		for (Message msg : _messages.values())
		{
			if ((msg != null) && (msg.getReceiverId() == objectId) && !msg.isDeletedByReceiver())
			{
				size++;
			}
		}
		return size;
	}
	
	public int getOutboxSize(int objectId)
	{
		int size = 0;
		for (Message msg : _messages.values())
		{
			if ((msg != null) && (msg.getSenderId() == objectId) && !msg.isDeletedBySender())
			{
				size++;
			}
		}
		return size;
	}
	
	public List<Message> getInbox(int objectId)
	{
		final List<Message> inbox = new LinkedList<>();
		for (Message msg : _messages.values())
		{
			if ((msg != null) && (msg.getReceiverId() == objectId) && !msg.isDeletedByReceiver())
			{
				inbox.add(msg);
			}
		}
		return inbox;
	}
	
	public List<Message> getOutbox(int objectId)
	{
		final List<Message> outbox = new LinkedList<>();
		for (Message msg : _messages.values())
		{
			if ((msg != null) && (msg.getSenderId() == objectId) && !msg.isDeletedBySender())
			{
				outbox.add(msg);
			}
		}
		return outbox;
	}
	
	public void sendMessage(Message msg)
	{
		_messages.put(msg.getId(), msg);
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(Message.INSERT_SQL))
		{
			Message.applyInsertParameters(msg, ps);
			ps.execute();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, e, () -> getClass().getSimpleName() + ": Error saving message:");
		}
		
		final Player receiver = World.getInstance().getPlayer(msg.getReceiverId());
		if (receiver != null)
		{
			receiver.sendPacket(ExNoticePostArrived.valueOf(true));
		}
		
		MessageDeletionTaskManager.getInstance().add(msg.getId(), msg.getExpiration());
	}
	
	public void markAsReadInDb(int msgId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE messages SET isUnread = 'false' WHERE messageId = ?"))
		{
			ps.setInt(1, msgId);
			ps.execute();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, e, () -> getClass().getSimpleName() + ": Error marking as read message:");
		}
	}
	
	public void markAsDeletedBySenderInDb(int msgId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE messages SET isDeletedBySender = 'true' WHERE messageId = ?"))
		{
			ps.setInt(1, msgId);
			ps.execute();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, e, () -> getClass().getSimpleName() + ": Error marking as deleted by sender message:");
		}
	}
	
	public void markAsDeletedByReceiverInDb(int msgId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE messages SET isDeletedByReceiver = 'true' WHERE messageId = ?"))
		{
			ps.setInt(1, msgId);
			ps.execute();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, e, () -> getClass().getSimpleName() + ": Error marking as deleted by receiver message:");
		}
	}
	
	public void removeAttachmentsInDb(int msgId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE messages SET hasAttachments = 'false' WHERE messageId = ?"))
		{
			ps.setInt(1, msgId);
			ps.execute();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, e, () -> getClass().getSimpleName() + ": Error removing attachments in message:");
		}
	}
	
	public void deleteMessageInDb(int msgId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM messages WHERE messageId = ?"))
		{
			ps.setInt(1, msgId);
			ps.execute();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, e, () -> getClass().getSimpleName() + ": Error deleting message:");
		}
		
		_messages.remove(msgId);
		IdManager.getInstance().releaseId(msgId);
	}
	
	/**
	 * Gets the single instance of {@code MailManager}.
	 * @return single instance of {@code MailManager}
	 */
	public static MailManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final MailManager INSTANCE = new MailManager();
	}
}