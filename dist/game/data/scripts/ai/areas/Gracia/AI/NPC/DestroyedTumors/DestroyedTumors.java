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
package ai.areas.Gracia.AI.NPC.DestroyedTumors;

import com.l2journey.gameserver.managers.InstanceManager;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.instancezone.InstanceWorld;
import com.l2journey.gameserver.model.quest.QuestState;

import ai.AbstractNpcAI;

/**
 * @author LordWinter, Mobius
 */
public class DestroyedTumors extends AbstractNpcAI
{
	private static final String EXAMINE_TUMOR = "examine_tumor";
	private static final String SHOWCHECKPAGE = "showcheckpage";
	private static final String HTML_N_32535_2 = "32535-2.htm";
	private static final String HTML_N_32535_6 = "32535-6.htm";
	private static final String HTML_N_32535_4 = "32535-4.htm";
	private static final String HTML_N_32535_3 = "32535-3.htm";

	private final long warpTimer = System.currentTimeMillis();
	
	public DestroyedTumors()
	{
		addStartNpc(32535);
		addFirstTalkId(32535);
		addTalkId(32535);
		
		addStartNpc(32536);
		addFirstTalkId(32536);
		addTalkId(32536);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			st = newQuestState(player);
		}
		
		InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
		
		if ((world != null) && (world.getTemplateId() == 119))
		{
			if (event.equalsIgnoreCase(EXAMINE_TUMOR))
			{
				if ((player.getParty() == null) || (player.getParty().getLeader() != player))
				{
					htmltext = HTML_N_32535_2;
				}
				else
				{
					htmltext = "32535-1.htm";
				}
			}
			else if (event.equalsIgnoreCase(SHOWCHECKPAGE))
			{
				if (player.getInventory().getItemByItemId(13797) == null)
				{
					htmltext = HTML_N_32535_6;
				}
				else if ((warpTimer + 60000) > System.currentTimeMillis())
				{
					htmltext = HTML_N_32535_4;
				}
				else if (world.getParameters().getInt("tag", 0) <= 0)
				{
					htmltext = HTML_N_32535_3;
				}
				else
				{
					htmltext = "32535-5a.htm";
				}
			}
		}
		else if ((world != null) && (world.getTemplateId() == 120))
		{
			if (event.equalsIgnoreCase(EXAMINE_TUMOR))
			{
				if ((player.getParty() == null) || (player.getParty().getLeader() != player))
				{
					htmltext = HTML_N_32535_2;
				}
				else
				{
					htmltext = "32535-1.htm";
				}
			}
			else if (event.equalsIgnoreCase(SHOWCHECKPAGE))
			{
				if (player.getInventory().getItemByItemId(13797) == null)
				{
					htmltext = HTML_N_32535_6;
				}
				else if ((warpTimer + 60000) > System.currentTimeMillis())
				{
					htmltext = HTML_N_32535_4;
				}
				else if (world.getParameters().getInt("tag", 0) <= 0)
				{
					htmltext = HTML_N_32535_3;
				}
				else
				{
					htmltext = "32535-5b.htm";
				}
			}
		}
		else if ((world != null) && (world.getTemplateId() == 121))
		{
			if (event.equalsIgnoreCase(EXAMINE_TUMOR))
			{
				if (npc.getId() == 32536)
				{
					if ((player.getParty() == null) || (player.getParty().getLeader() != player))
					{
						htmltext = "32536-2.htm";
					}
					else
					{
						htmltext = "32536-1.htm";
					}
				}
				if (npc.getId() == 32535)
				{
					if ((player.getParty() == null) || (player.getParty().getLeader() != player))
					{
						htmltext = HTML_N_32535_2;
					}
					else
					{
						htmltext = "32535-7.htm";
					}
				}
			}
			else if (event.equalsIgnoreCase(SHOWCHECKPAGE))
			{
				if (player.getInventory().getItemByItemId(13797) == null)
				{
					htmltext = HTML_N_32535_6;
				}
				else if ((warpTimer + 60000) > System.currentTimeMillis())
				{
					htmltext = HTML_N_32535_4;
				}
				else if (world.getParameters().getInt("tag", 0) <= 0)
				{
					htmltext = HTML_N_32535_3;
				}
				else
				{
					htmltext = "32535-5.htm";
				}
			}
			else if (event.equalsIgnoreCase("reenter"))
			{
				if ((player.getInventory().getItemByItemId(13797) == null) || (player.getInventory().getItemByItemId(13797).getCount() < 3))
				{
					htmltext = HTML_N_32535_6;
				}
				else
				{
					htmltext = "32535-8.htm";
				}
			}
		}
		else if (((world != null) && (world.getTemplateId() == 122)) && ((event.equalsIgnoreCase(EXAMINE_TUMOR)) && (npc.getId() == 32535)))
		{
			htmltext = HTML_N_32535_4;
		}
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			st = newQuestState(player);
		}
		
		if (npc.getId() == 32535)
		{
			return "32535.htm";
		}
		
		if (npc.getId() == 32536)
		{
			return "32536.htm";
		}
		
		return "";
	}
}
