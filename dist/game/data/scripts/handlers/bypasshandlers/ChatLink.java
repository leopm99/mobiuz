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
package handlers.bypasshandlers;

import com.l2journey.gameserver.handler.IBypassHandler;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.events.EventDispatcher;
import com.l2journey.gameserver.model.events.EventType;
import com.l2journey.gameserver.model.events.holders.actor.npc.OnNpcFirstTalk;

public class ChatLink implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"Chat"
	};
	
	@Override
	public boolean useBypass(String command, Player player, Creature target)
	{
		if (!target.isNpc())
		{
			return false;
		}
		
		int val = 0;
		try
		{
			val = Integer.parseInt(command.substring(5));
		}
		catch (Exception _)
		{
			// Handled above.
		}
		
		final Npc npc = target.asNpc();
		if ((val == 0) && npc.hasListener(EventType.ON_NPC_FIRST_TALK))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnNpcFirstTalk(npc, player), npc);
		}
		else
		{
			npc.showChatWindow(player, val);
		}
		return false;
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}
