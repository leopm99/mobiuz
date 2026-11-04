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
package com.l2journey.gameserver.network.serverpackets;

import java.util.Collection;

import com.l2journey.commons.network.WritableBuffer;
import com.l2journey.gameserver.model.Message;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.itemcontainer.ItemContainer;
import com.l2journey.gameserver.network.GameClient;
import com.l2journey.gameserver.network.PacketLogger;
import com.l2journey.gameserver.network.ServerPackets;

/**
 * @author Migi, DS
 */
public class ExReplyReceivedPost extends AbstractItemPacket
{
	private final Message _msg;
	private Collection<Item> _items = null;
	
	public ExReplyReceivedPost(Message msg)
	{
		_msg = msg;
		if (msg.hasAttachments())
		{
			final ItemContainer attachments = msg.getAttachments();
			if ((attachments != null) && (attachments.getSize() > 0))
			{
				_items = attachments.getItems();
			}
			else
			{
				PacketLogger.warning("Message " + msg.getId() + " has attachments but itemcontainer is empty.");
			}
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_REPLY_RECEIVED_POST.writeId(this, buffer);
		buffer.writeInt(_msg.getId());
		buffer.writeInt(_msg.isLocked());
		buffer.writeInt(0); // Unknown
		buffer.writeString(_msg.getSenderName());
		buffer.writeString(_msg.getSubject());
		buffer.writeString(_msg.getContent());
		if ((_items != null) && !_items.isEmpty())
		{
			buffer.writeInt(_items.size());
			for (Item item : _items)
			{
				writeItem(item, buffer);
				buffer.writeInt(item.getObjectId());
			}
		}
		else
		{
			buffer.writeInt(0);
		}
		buffer.writeLong(_msg.getReqAdena());
		buffer.writeInt(_msg.hasAttachments());
		buffer.writeInt(_msg.getClientSenderType());
	}
}
