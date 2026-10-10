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
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.holders.player.Shortcut;
import com.l2journey.gameserver.network.GameClient;
import com.l2journey.gameserver.network.ServerPackets;

/**
 * @author KingHanker
 */
public class ShortcutInit extends ServerPacket
{
	private Collection<Shortcut> _shortcuts;
	
	public ShortcutInit(Player player)
	{
		if (player == null)
		{
			return;
		}
		_shortcuts = player.getAllShortcuts();
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.SHORT_CUT_INIT.writeId(this, buffer);
		buffer.writeInt(_shortcuts.size());
		for (Shortcut sc : _shortcuts)
		{
			buffer.writeInt(sc.getType().ordinal());
			buffer.writeInt(sc.getSlot() + (sc.getPage() * 12));
			switch (sc.getType())
			{
				case ITEM:
				{
					buffer.writeInt(sc.getId());
					buffer.writeInt(1);
					buffer.writeInt(sc.getSharedReuseGroup());
					buffer.writeInt(0);
					buffer.writeInt(0);
					buffer.writeShort(0);
					buffer.writeShort(0);
					break;
				}
				case SKILL:
				{
					buffer.writeInt(sc.getId());
					buffer.writeInt(sc.getLevel());
					buffer.writeByte(0); // C5
					buffer.writeInt(sc.getCharacterType()); // C6 - 1 for player, 2 for pet
					break;
				}
				case ACTION, MACRO, RECIPE, BOOKMARK:
				{
					buffer.writeInt(sc.getId());
					buffer.writeInt(sc.getCharacterType()); // C6 - 1 for player, 2 for pet
				}
				default:
				{
					break;
				}
			}
		}
	}
}
