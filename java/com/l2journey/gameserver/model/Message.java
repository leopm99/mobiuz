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
package com.l2journey.gameserver.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ScheduledFuture;

import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.data.sql.CharInfoTable;
import com.l2journey.gameserver.managers.IdManager;
import com.l2journey.gameserver.managers.MailManager;
import com.l2journey.gameserver.model.itemcontainer.Mail;
import com.l2journey.gameserver.network.enums.MessageSenderType;

/**
 * @author Migi, DS
 */
public class Message
{
	private static final int EXPIRATION = 360; // 15 days
	private static final int COD_EXPIRATION = 12; // 12 hours
	
	private static final int UNLOAD_ATTACHMENTS_INTERVAL = 900000; // 15-30 mins
	
	// post state
	public static final int DELETED = 0;
	public static final int READED = 1;
	public static final int REJECTED = 2;
	
	private final int _messageId;
	private final int _senderId;
	private final int _receiverId;
	private final long _expiration;
	private String _senderName = null;
	private String _receiverName = null;
	private final String _subject;
	private final String _content;
	private boolean _unread;
	private boolean _returned;
	private int _sendBySystem;
	private boolean _deletedBySender;
	private boolean _deletedByReceiver;
	private final long _reqAdena;
	private boolean _hasAttachments;
	private Mail _attachments = null;
	private ScheduledFuture<?> _unloadTask = null;
	
	/*
	 * Constructor for restoring from DB.
	 */
	public Message(ResultSet rset) throws SQLException
	{
		_messageId = rset.getInt("messageId");
		_senderId = rset.getInt("senderId");
		_receiverId = rset.getInt("receiverId");
		_subject = rset.getString("subject");
		_content = rset.getString("content");
		_expiration = rset.getLong("expiration");
		_reqAdena = rset.getLong("reqAdena");
		_hasAttachments = Boolean.parseBoolean(rset.getString("hasAttachments"));
		_unread = Boolean.parseBoolean(rset.getString("isUnread"));
		_deletedBySender = Boolean.parseBoolean(rset.getString("isDeletedBySender"));
		_deletedByReceiver = Boolean.parseBoolean(rset.getString("isDeletedByReceiver"));
		_sendBySystem = rset.getInt("sendBySystem");
		_returned = Boolean.parseBoolean(rset.getString("isReturned"));
	}
	
	/*
	 * This constructor used for creating new message.
	 */
	public Message(int senderId, int receiverId, boolean isCod, String subject, String text, long reqAdena)
	{
		_messageId = IdManager.getInstance().getNextId();
		_senderId = senderId;
		_receiverId = receiverId;
		_subject = subject;
		_content = text;
		_expiration = isCod ? System.currentTimeMillis() + (COD_EXPIRATION * 3600000) : System.currentTimeMillis() + (EXPIRATION * 3600000);
		_hasAttachments = false;
		_unread = true;
		_deletedBySender = false;
		_deletedByReceiver = false;
		_reqAdena = reqAdena;
	}
	
	/*
	 * This constructor used for System Mails
	 */
	public Message(int receiverId, String subject, String content, MessageSenderType sendBySystem)
	{
		_messageId = IdManager.getInstance().getNextId();
		_senderId = -1;
		_receiverId = receiverId;
		_subject = subject;
		_content = content;
		_expiration = System.currentTimeMillis() + (EXPIRATION * 3600000);
		_reqAdena = 0;
		_hasAttachments = false;
		_unread = true;
		_deletedBySender = true;
		_deletedByReceiver = false;
		_sendBySystem = sendBySystem.ordinal();
		_returned = false;
	}
	
	/*
	 * This constructor used for auto-generation of the "return attachments" message
	 */
	public Message(Message msg)
	{
		_messageId = IdManager.getInstance().getNextId();
		_senderId = msg.getSenderId();
		_receiverId = msg.getSenderId();
		_subject = "";
		_content = "";
		_expiration = System.currentTimeMillis() + (EXPIRATION * 3600000);
		_unread = true;
		_deletedBySender = true;
		_deletedByReceiver = false;
		_sendBySystem = MessageSenderType.NONE.ordinal();
		_returned = true;
		_reqAdena = 0;
		_hasAttachments = true;
		_attachments = msg.getAttachments();
		msg.removeAttachments();
		_attachments.setNewMessageId(_messageId);
		_unloadTask = ThreadPool.schedule(new AttachmentsUnloadTask(this), (long) (UNLOAD_ATTACHMENTS_INTERVAL) + Rnd.get(UNLOAD_ATTACHMENTS_INTERVAL));
	}
	
	public static final String INSERT_SQL = "INSERT INTO messages (messageId, senderId, receiverId, subject, content, expiration, reqAdena, hasAttachments, isUnread, isDeletedBySender, isDeletedByReceiver, sendBySystem, isReturned) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	public static void applyInsertParameters(Message msg, PreparedStatement stmt) throws SQLException
	{
		stmt.setInt(1, msg._messageId);
		stmt.setInt(2, msg._senderId);
		stmt.setInt(3, msg._receiverId);
		stmt.setString(4, msg._subject);
		stmt.setString(5, msg._content);
		stmt.setLong(6, msg._expiration);
		stmt.setLong(7, msg._reqAdena);
		stmt.setString(8, String.valueOf(msg._hasAttachments));
		stmt.setString(9, String.valueOf(msg._unread));
		stmt.setString(10, String.valueOf(msg._deletedBySender));
		stmt.setString(11, String.valueOf(msg._deletedByReceiver));
		stmt.setString(12, String.valueOf(msg._sendBySystem));
		stmt.setString(13, String.valueOf(msg._returned));
	}
	
	public int getId()
	{
		return _messageId;
	}
	
	public int getSenderId()
	{
		return _senderId;
	}
	
	public int getReceiverId()
	{
		return _receiverId;
	}
	
	public String getSenderName()
	{
		if (_senderName == null)
		{
			if (_sendBySystem != 0)
			{
				return "****";
			}
			
			_senderName = CharInfoTable.getInstance().getNameById(_senderId);
			if (_senderName == null)
			{
				_senderName = "";
			}
		}
		return _senderName;
	}
	
	public String getReceiverName()
	{
		if (_receiverName == null)
		{
			_receiverName = CharInfoTable.getInstance().getNameById(_receiverId);
			if (_receiverName == null)
			{
				_receiverName = "";
			}
		}
		return _receiverName;
	}
	
	public String getSubject()
	{
		return _subject;
	}
	
	public String getContent()
	{
		return _content;
	}
	
	public boolean isLocked()
	{
		return _reqAdena > 0;
	}
	
	public long getExpiration()
	{
		return _expiration;
	}
	
	public int getExpirationSeconds()
	{
		return (int) (_expiration / 1000);
	}
	
	public boolean isUnread()
	{
		return _unread;
	}
	
	public void markAsRead()
	{
		if (!_unread)
		{
			return;
		}
		_unread = false;
		MailManager.getInstance().markAsReadInDb(_messageId);
	}
	
	public boolean isDeletedBySender()
	{
		return _deletedBySender;
	}
	
	public void setDeletedBySender()
	{
		if (_deletedBySender)
		{
			return;
		}
		_deletedBySender = true;
		if (_deletedByReceiver)
		{
			MailManager.getInstance().deleteMessageInDb(_messageId);
		}
		else
		{
			MailManager.getInstance().markAsDeletedBySenderInDb(_messageId);
		}
	}
	
	public boolean isDeletedByReceiver()
	{
		return _deletedByReceiver;
	}
	
	public void setDeletedByReceiver()
	{
		if (_deletedByReceiver)
		{
			return;
		}
		_deletedByReceiver = true;
		if (_deletedBySender)
		{
			MailManager.getInstance().deleteMessageInDb(_messageId);
		}
		else
		{
			MailManager.getInstance().markAsDeletedByReceiverInDb(_messageId);
		}
	}
	
	public int getSendBySystem()
	{
		return _sendBySystem;
	}
	
	public boolean isReturned()
	{
		return _returned;
	}
	
	public void setReturned(boolean value)
	{
		_returned = value;
	}
	
	public long getReqAdena()
	{
		return _reqAdena;
	}
	
	public synchronized Mail getAttachments()
	{
		if (!_hasAttachments)
		{
			return null;
		}
		
		if (_attachments == null)
		{
			_attachments = new Mail(_senderId, _messageId);
			_attachments.restore();
			_unloadTask = ThreadPool.schedule(new AttachmentsUnloadTask(this), (long) (UNLOAD_ATTACHMENTS_INTERVAL) + Rnd.get(UNLOAD_ATTACHMENTS_INTERVAL));
		}
		
		return _attachments;
	}
	
	public boolean hasAttachments()
	{
		return _hasAttachments;
	}
	
	public synchronized void removeAttachments()
	{
		if (_attachments == null)
		{
			return;
		}
		_attachments = null;
		_hasAttachments = false;
		MailManager.getInstance().removeAttachmentsInDb(_messageId);
		if (_unloadTask != null)
		{
			_unloadTask.cancel(false);
		}
	}
	
	public synchronized Mail createAttachments()
	{
		if (_hasAttachments || (_attachments != null))
		{
			return null;
		}
		
		_attachments = new Mail(_senderId, _messageId);
		_hasAttachments = true;
		_unloadTask = ThreadPool.schedule(new AttachmentsUnloadTask(this), (long) (UNLOAD_ATTACHMENTS_INTERVAL) + Rnd.get(UNLOAD_ATTACHMENTS_INTERVAL));
		return _attachments;
	}
	
	protected final synchronized void unloadAttachments()
	{
		if (_attachments == null)
		{
			return;
		}
		_attachments.deleteMe();
		_attachments = null;
	}
	
	private static class AttachmentsUnloadTask implements Runnable
	{
		private Message _msg;
		
		AttachmentsUnloadTask(Message msg)
		{
			_msg = msg;
		}
		
		@Override
		public void run()
		{
			if (_msg == null)
			{
				return;
			}
			_msg.unloadAttachments();
			_msg = null;
		}
	}
}
