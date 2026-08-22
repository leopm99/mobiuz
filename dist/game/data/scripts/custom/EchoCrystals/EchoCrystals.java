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
package custom.EchoCrystals;

import java.util.HashMap;
import java.util.Map;

import com.l2journey.commons.util.StringUtil;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;

import ai.AbstractNpcAI;

/**
 * Echo Crystals AI.
 * @author Plim
 */
public class EchoCrystals extends AbstractNpcAI
{
	private static final int[] NPCs =
	{
		31042,
		31043
	};
	
	private static final int ADENA = 57;
	private static final int COST = 200;
	
	private static final Map<Integer, ScoreData> SCORES = new HashMap<>();
	
	private class ScoreData
	{
		private final int crystalId;
		private final String okMsg;
		private final String noAdenaMsg;
		private final String noScoreMsg;
		
		public ScoreData(int crystalId, String okMsg, String noAdenaMsg, String noScoreMsg)
		{
			super();
			this.crystalId = crystalId;
			this.okMsg = okMsg;
			this.noAdenaMsg = noAdenaMsg;
			this.noScoreMsg = noScoreMsg;
		}
		
		public int getCrystalId()
		{
			return crystalId;
		}
		
		public String getOkMsg()
		{
			return okMsg;
		}
		
		public String getNoAdenaMsg()
		{
			return noAdenaMsg;
		}
		
		public String getNoScoreMsg()
		{
			return noScoreMsg;
		}
	}
	
	private EchoCrystals()
	{
		// Initialize Map
		SCORES.put(4410, new ScoreData(4411, "01", "02", "03"));
		SCORES.put(4409, new ScoreData(4412, "04", "05", "06"));
		SCORES.put(4408, new ScoreData(4413, "07", "08", "09"));
		SCORES.put(4420, new ScoreData(4414, "10", "11", "12"));
		SCORES.put(4421, new ScoreData(4415, "13", "14", "15"));
		SCORES.put(4419, new ScoreData(4417, "16", "05", "06"));
		SCORES.put(4418, new ScoreData(4416, "17", "05", "06"));
		
		for (int npc : NPCs)
		{
			addStartNpc(npc);
			addTalkId(npc);
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = "";
		if (StringUtil.isNumeric(event))
		{
			final int score = Integer.parseInt(event);
			if (SCORES.containsKey(score))
			{
				final int crystal = SCORES.get(score).getCrystalId();
				final String ok = SCORES.get(score).getOkMsg();
				final String noadena = SCORES.get(score).getNoAdenaMsg();
				final String noscore = SCORES.get(score).getNoScoreMsg();
				if (!hasQuestItems(player, score))
				{
					htmltext = npc.getId() + "-" + noscore + ".htm";
				}
				else if (getQuestItemsCount(player, ADENA) < COST)
				{
					htmltext = npc.getId() + "-" + noadena + ".htm";
				}
				else
				{
					takeItems(player, ADENA, COST);
					giveItems(player, crystal, 1);
					htmltext = npc.getId() + "-" + ok + ".htm";
				}
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		return "1.htm";
	}
	
	public static void main(String[] args)
	{
		new EchoCrystals();
	}
}
