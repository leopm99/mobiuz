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

import com.l2journey.commons.network.WritableBuffer;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.network.GameClient;
import com.l2journey.gameserver.network.ServerPackets;

public class EquipUpdate extends ServerPacket
{
	private final Item _item;
	private final int _change;
	
	public EquipUpdate(Item item, int change)
	{
		_item = item;
		_change = change;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EQUIP_UPDATE.writeId(this, buffer);
		buffer.writeInt(_change);
		buffer.writeInt(_item.getObjectId());
		int bodypart = 0;
		switch (_item.getTemplate().getBodyPart())
		{
			case ItemTemplate.SLOT_L_EAR:
			{
				bodypart = 0x01;
				break;
			}
			case ItemTemplate.SLOT_R_EAR:
			{
				bodypart = 0x02;
				break;
			}
			case ItemTemplate.SLOT_NECK:
			{
				bodypart = 0x03;
				break;
			}
			case ItemTemplate.SLOT_R_FINGER:
			{
				bodypart = 0x04;
				break;
			}
			case ItemTemplate.SLOT_L_FINGER:
			{
				bodypart = 0x05;
				break;
			}
			case ItemTemplate.SLOT_HEAD:
			{
				bodypart = 0x06;
				break;
			}
			case ItemTemplate.SLOT_R_HAND:
			{
				bodypart = 0x07;
				break;
			}
			case ItemTemplate.SLOT_L_HAND:
			{
				bodypart = 0x08;
				break;
			}
			case ItemTemplate.SLOT_GLOVES:
			{
				bodypart = 0x09;
				break;
			}
			case ItemTemplate.SLOT_CHEST:
			{
				bodypart = 0x0a;
				break;
			}
			case ItemTemplate.SLOT_LEGS:
			{
				bodypart = 0x0b;
				break;
			}
			case ItemTemplate.SLOT_FEET:
			{
				bodypart = 0x0c;
				break;
			}
			case ItemTemplate.SLOT_BACK:
			{
				bodypart = 0x0d;
				break;
			}
			case ItemTemplate.SLOT_LR_HAND:
			{
				bodypart = 0x0e;
				break;
			}
			case ItemTemplate.SLOT_HAIR:
			{
				bodypart = 0x0f;
				break;
			}
			case ItemTemplate.SLOT_BELT:
			{
				bodypart = 0x10;
				break;
			}
			default:
			{
				break;
			}
		}
		buffer.writeInt(bodypart);
	}
}
