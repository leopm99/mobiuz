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
package ai.others.ArenaManager;

import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.itemcontainer.Inventory;
import com.l2journey.gameserver.model.skill.holders.SkillHolder;
import com.l2journey.gameserver.model.zone.ZoneId;
import com.l2journey.gameserver.network.SystemMessageId;

import ai.AbstractNpcAI;

/**
 * Arena Manager AI.
 * @author St3eT
 */
public class ArenaManager extends AbstractNpcAI
{
	// NPCs
	private static final int[] ARENA_MANAGER =
	{
		31226, // Arena Director (MDT)
		31225, // Arena Manager (Coliseum)
	};
	// Skill
	private static final SkillHolder[] BUFFS =
	{
		new SkillHolder(6805, 1), // Arena Empower
		new SkillHolder(6806, 1), // Arena Acumen
		new SkillHolder(6807, 1), // Arena Concentration
		new SkillHolder(6808, 1), // Arena Might
		new SkillHolder(6804, 1), // Arena Wind Walk
		new SkillHolder(6812, 1), // Arena Berserker Spirit
	};
	private static final SkillHolder CP_RECOVERY = new SkillHolder(4380, 1); // Arena: CP Recovery
	private static final SkillHolder HP_RECOVERY = new SkillHolder(6817, 1); // Arena HP Recovery
	// Misc
	private static final int CP_COST = 1000;
	private static final int HP_COST = 1000;
	private static final int BUFF_COST = 2000;
	
	private ArenaManager()
	{
		addStartNpc(ARENA_MANAGER);
		addTalkId(ARENA_MANAGER);
		addFirstTalkId(ARENA_MANAGER);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "CPrecovery":
			{
				if (player.getAdena() >= CP_COST)
				{
					takeItems(player, Inventory.ADENA_ID, CP_COST);
					startQuestTimer("CPrecovery_delay", 2000, npc, player);
				}
				else
				{
					player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
				}
				break;
			}
			case "CPrecovery_delay":
			{
				if ((player != null) && !player.isInsideZone(ZoneId.PVP))
				{
					npc.setTarget(player);
					npc.doCast(CP_RECOVERY.getSkill());
				}
				break;
			}
			case "HPrecovery":
			{
				if (player.getAdena() >= HP_COST)
				{
					takeItems(player, Inventory.ADENA_ID, HP_COST);
					startQuestTimer("HPrecovery_delay", 2000, npc, player);
				}
				else
				{
					player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
				}
				break;
			}
			case "HPrecovery_delay":
			{
				if ((player != null) && !player.isInsideZone(ZoneId.PVP))
				{
					npc.setTarget(player);
					npc.doCast(HP_RECOVERY.getSkill());
				}
				break;
			}
			case "Buff":
			{
				if (player.getAdena() >= BUFF_COST)
				{
					takeItems(player, Inventory.ADENA_ID, BUFF_COST);
					npc.setTarget(player);
					for (SkillHolder skill : BUFFS)
					{
						npc.doCast(skill.getSkill());
					}
				}
				else
				{
					player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
				}
				break;
			}
			default:
			{
				break;
			}
		}
		return super.onEvent(event, npc, player);
	}
	
	public static void main(String[] args)
	{
		new ArenaManager();
	}
}