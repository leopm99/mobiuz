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
package instances.NornilsGardenQuest;

import com.l2journey.gameserver.managers.InstanceManager;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.instancezone.InstanceWorld;
import com.l2journey.gameserver.model.quest.QuestState;

import instances.AbstractInstance;
import quests.Q00236_SeedsOfChaos.Q00236_SeedsOfChaos;

/**
 * Nornil's Garden Quest instant zone.
 * @author Zoey76
 */
public class NornilsGardenQuest extends AbstractInstance
{
	// NPCs
	private static final int RODENPICULA = 32237;
	private static final int MOTHER_NORNIL = 32239;
	// Location
	private static final Location ENTER_LOC = new Location(-119538, 87177, -12592);
	// Misc
	private static final int TEMPLATE_ID = 12;
	
	public NornilsGardenQuest()
	{
		addStartNpc(RODENPICULA, MOTHER_NORNIL);
		addTalkId(RODENPICULA, MOTHER_NORNIL);
		addFirstTalkId(RODENPICULA, MOTHER_NORNIL);
	}
	
	@Override
	protected boolean checkConditions(Player player)
	{
		final QuestState qs = player.getQuestState(Q00236_SeedsOfChaos.class.getSimpleName());
		return (qs != null) && (qs.getMemoState() >= 40) && (qs.getMemoState() <= 45);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		final QuestState q236 = player.getQuestState(Q00236_SeedsOfChaos.class.getSimpleName());
		switch (event)
		{
			case "enter":
			{
				if (checkConditions(player))
				{
					final Location originLoc = player.getLocation();
					enterInstance(player, TEMPLATE_ID);
					InstanceManager.getInstance().getPlayerWorld(player).setParameter("ORIGIN_LOC", originLoc);
					q236.setCond(16, true);
					htmltext = "32190-02.html";
				}
				else
				{
					htmltext = "32190-03.html";
				}
				break;
			}
			case "exit":
			{
				if ((q236 != null) && q236.isCompleted())
				{
					final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
					world.removeAllowed(player);
					finishInstance(world, 5000);
					player.setInstanceId(0);
					player.teleToLocation(world.getParameters().getLocation("ORIGIN_LOC"));
					htmltext = "32239-03.html";
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
	protected void onEnterInstance(Player player, InstanceWorld world, boolean firstEntrance)
	{
		if (firstEntrance)
		{
			world.addAllowed(player);
		}
		teleportPlayer(player, ENTER_LOC, world.getInstanceId(), false);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		String htmltext = null;
		final QuestState q236 = player.getQuestState(Q00236_SeedsOfChaos.class.getSimpleName());
		switch (npc.getId())
		{
			case RODENPICULA:
			{
				htmltext = (q236 != null) && (q236.isCompleted()) ? "32237-02.html" : "32237-01.html";
				break;
			}
			case MOTHER_NORNIL:
			{
				htmltext = (q236 != null) && (q236.isCompleted()) ? "32239-02.html" : "32239-01.html";
				break;
			}
			default:
			{
				break;
			}
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new NornilsGardenQuest();
	}
}
