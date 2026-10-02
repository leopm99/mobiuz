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
package com.l2journey.gameserver.network.clientpackets;

import com.l2journey.gameserver.model.CharSelectInfoPackage;
import com.l2journey.gameserver.model.events.EventDispatcher;
import com.l2journey.gameserver.model.events.EventType;
import com.l2journey.gameserver.model.events.holders.actor.player.OnPlayerRestore;
import com.l2journey.gameserver.network.GameClient;
import com.l2journey.gameserver.network.serverpackets.CharSelectionInfo;

/**
 * @version $Revision: 1.4.2.1.2.2 $ $Date: 2005/03/27 15:29:29 $
 */
public class CharacterRestore extends ClientPacket
{
	// cd
	private int _charSlot;
	
	@Override
	protected void readImpl()
	{
		_charSlot = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final GameClient client = getClient();
		if (!client.getFloodProtectors().canSelectCharacter())
		{
			return;
		}
		
		client.restore(_charSlot);
		final CharSelectionInfo cl = new CharSelectionInfo(client.getAccountName(), client.getSessionId().getPlayOkID1(), 0);
		client.sendPacket(cl);
		client.setCharSelection(cl.getCharInfo());
		
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_RESTORE))
		{
			final CharSelectInfoPackage charInfo = client.getCharSelection(_charSlot);
			EventDispatcher.getInstance().notifyEvent(new OnPlayerRestore(charInfo.getObjectId(), charInfo.getName(), client));
		}
	}
}
