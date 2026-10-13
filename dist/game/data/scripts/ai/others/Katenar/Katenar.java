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
package ai.others.Katenar;

import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.quest.QuestState;
import com.l2journey.gameserver.network.NpcStringId;
import com.l2journey.gameserver.network.enums.ChatType;

import ai.AbstractNpcAI;
import quests.Q00065_CertifiedSoulBreaker.Q00065_CertifiedSoulBreaker;

/**
 * Katenar AI for quests Certified Soul Breaker (65)
 * @author ivantotov
 */
public class Katenar extends AbstractNpcAI
{
	private static final String SPAWNED = "SPAWNED";

	// NPC
	private static final int KATENAR = 32242;
	// Item
	private static final int SEALED_DOCUMENT = 9803;
	
	private Katenar()
	{
		addStartNpc(KATENAR);
		addTalkId(KATENAR);
		addFirstTalkId(KATENAR);
		addSpawnId(KATENAR);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final Npc npc0 = npc.getVariables().getObject("npc0", Npc.class);
		final String htmltext = null;
		switch (event)
		{
			case "CREATED_50":
			{
				if ((npc0 != null) && !npc.getVariables().getBoolean(SPAWNED, false))
				{
					npc0.getVariables().set(SPAWNED, false);
				}
				npc.deleteMe();
				break;
			}
			case "GOOD_LUCK":
			{
				final QuestState qs = player.getQuestState(Q00065_CertifiedSoulBreaker.class.getSimpleName());
				if (qs.isMemoState(14))
				{
					if ((npc0 != null) && !npc.getVariables().getBoolean(SPAWNED, false))
					{
						npc0.getVariables().set(SPAWNED, false);
						npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.GOOD_LUCK);
					}
					npc.deleteMe();
				}
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
	public String onFirstTalk(Npc npc, Player talker)
	{
		final QuestState qs = talker.getQuestState(Q00065_CertifiedSoulBreaker.class.getSimpleName());
		String htmltext = getNoQuestMsg(talker);
		final int memoState = qs.getMemoState();
		if (memoState == 12)
		{
			htmltext = "32242-01.html";
		}
		else if (memoState == 13)
		{
			final Player player = npc.getVariables().getObject("player0", Player.class);
			if (player == talker)
			{
				qs.setMemoState(14);
				qs.setCond(13, true);
				htmltext = "32242-02.html";
			}
			else
			{
				qs.setMemoState(14);
				qs.setCond(13, true);
				htmltext = "32242-03.html";
			}
			if (!hasQuestItems(player, SEALED_DOCUMENT))
			{
				giveItems(player, SEALED_DOCUMENT, 1);
			}
		}
		else if (memoState == 14)
		{
			htmltext = "32242-04.html";
		}
		return htmltext;
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		startQuestTimer("CREATED_50", 50000, npc, null);
		final Player player = npc.getVariables().getObject("player0", Player.class);
		if (player != null)
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.I_AM_LATE);
		}
	}
	
	public static void main(String[] args)
	{
		new Katenar();
	}
}