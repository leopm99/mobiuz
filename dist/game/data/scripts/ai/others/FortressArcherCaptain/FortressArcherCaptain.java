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
package ai.others.FortressArcherCaptain;

import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;

import ai.AbstractNpcAI;

/**
 * Fortress Archer Captain AI.
 * @author St3eT
 */
public class FortressArcherCaptain extends AbstractNpcAI
{
	// NPCs
	private static final int[] ARCHER_CAPTAIN =
	{
		35661, // Shanty Fortress
		35692, // Southern Fortress
		35730, // Hive Fortress
		35761, // Valley Fortress
		35799, // Ivory Fortress
		35830, // Narsell Fortress
		35861, // Bayou Fortress
		35899, // White Sands Fortress
		35930, // Borderland Fortress
		35968, // Swamp Fortress
		36006, // Archaic Fortress
		36037, // Floran Fortress
		36075, // Cloud Mountain
		36113, // Tanor Fortress
		36144, // Dragonspine Fortress
		36175, // Antharas's Fortress
		36213, // Western Fortress
		36251, // Hunter's Fortress
		36289, // Aaru Fortress
		36320, // Demon Fortress
		36358, // Monastic Fortress
	};
	
	private FortressArcherCaptain()
	{
		addStartNpc(ARCHER_CAPTAIN);
		addFirstTalkId(ARCHER_CAPTAIN);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final int ownerClanId = npc.getFort().getOwnerClan() == null ? 0 : npc.getFort().getOwnerClan().getId();
		return ((player.getClan() != null) && (player.getClanId() == ownerClanId)) ? "FortressArcherCaptain.html" : "FortressArcherCaptain-01.html";
	}
	
	public static void main(String[] args)
	{
		new FortressArcherCaptain();
	}
}