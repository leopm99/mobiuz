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
package ai.others.SymbolMaker;

import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.network.serverpackets.HennaEquipList;
import com.l2journey.gameserver.network.serverpackets.HennaRemoveList;

import ai.AbstractNpcAI;

/**
 * Symbol Maker AI.
 * @author Adry_85
 */
public class SymbolMaker extends AbstractNpcAI
{
	// NPCs
	private static final int[] NPCS =
	{
		31046, // Marsden
		31047, // Kell
		31048, // McDermott
		31049, // Pepper
		31050, // Thora
		31051, // Keach
		31052, // Heid
		31053, // Kidder
		31264, // Olsun
		31308, // Achim
		31953, // Rankar
	};
	
	private SymbolMaker()
	{
		addFirstTalkId(NPCS);
		addStartNpc(NPCS);
		addTalkId(NPCS);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "symbol_maker.htm", "symbol_maker-1.htm", "symbol_maker-2.htm", "symbol_maker-3.htm":
			{
				htmltext = event;
				break;
			}
			case "Draw":
			{
				player.sendPacket(new HennaEquipList(player));
				break;
			}
			case "Remove":
			{
				player.sendPacket(new HennaRemoveList(player));
				break;
			}
			default:
			{
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "symbol_maker.htm";
	}
	
	public static void main(String[] args)
	{
		new SymbolMaker();
	}
}