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
package ai.areas.Hellbound.AI.Zones.BaseTower;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.l2journey.gameserver.data.xml.DoorData;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.player.PlayerClass;
import com.l2journey.gameserver.model.skill.enums.SkillFinishType;
import com.l2journey.gameserver.model.skill.holders.SkillHolder;

import ai.AbstractNpcAI;

/**
 * Base Tower.
 * @author GKR
 */
public class BaseTower extends AbstractNpcAI
{
	// NPCs
	private static final int GUZEN = 22362;
	private static final int KENDAL = 32301;
	private static final int BODY_DESTROYER = 22363;
	// Skills
	private static final SkillHolder DEATH_WORD = new SkillHolder(5256, 1);
	// Misc
	private static final Map<Integer, Player> BODY_DESTROYER_TARGET_LIST = new ConcurrentHashMap<>();
	
	public BaseTower()
	{
		addKillId(GUZEN);
		addKillId(BODY_DESTROYER);
		addFirstTalkId(KENDAL);
		addAggroRangeEnterId(BODY_DESTROYER);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final PlayerClass classId = player.getPlayerClass();
		if (classId.equalsOrChildOf(PlayerClass.HELL_KNIGHT) || classId.equalsOrChildOf(PlayerClass.SOULTAKER))
		{
			return "32301-02.htm";
		}
		return "32301-01.htm";
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("CLOSE"))
		{
			DoorData.getInstance().getDoor(20260004).closeMe();
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onAggroRangeEnter(Npc npc, Player player, boolean isSummon)
	{
		if (!BODY_DESTROYER_TARGET_LIST.containsKey(npc.getObjectId()))
		{
			BODY_DESTROYER_TARGET_LIST.put(npc.getObjectId(), player);
			npc.setTarget(player);
			npc.doSimultaneousCast(DEATH_WORD.getSkill());
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		switch (npc.getId())
		{
			case GUZEN:
			{
				// Should Kendal be despawned before Guzen's spawn? Or it will be crowd of Kendal's
				addSpawn(KENDAL, npc.getSpawn().getLocation(), false, npc.getSpawn().getRespawnDelay(), false);
				DoorData.getInstance().getDoor(20260003).openMe();
				DoorData.getInstance().getDoor(20260004).openMe();
				startQuestTimer("CLOSE", 60000, npc, null, false);
				break;
			}
			case BODY_DESTROYER:
			{
				if (BODY_DESTROYER_TARGET_LIST.containsKey(npc.getObjectId()))
				{
					final Player pl = BODY_DESTROYER_TARGET_LIST.get(npc.getObjectId());
					if ((pl != null) && pl.isOnline() && !pl.isDead())
					{
						pl.stopSkillEffects(SkillFinishType.REMOVED, DEATH_WORD.getSkillId());
					}
					BODY_DESTROYER_TARGET_LIST.remove(npc.getObjectId());
				}
				break;
			}
			default:
			{
				break;
			}
		}
	}
}