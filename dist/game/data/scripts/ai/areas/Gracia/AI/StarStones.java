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
package ai.areas.Gracia.AI;

import java.util.List;

import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.network.SystemMessageId;

import ai.AbstractNpcAI;

/**
 * Star Stones AI.
 * @author Gigiikun
 */
public class StarStones extends AbstractNpcAI
{
	// @formatter:off
	private static final int[] MOBS =
	{
		18684, 18685, 18686, 18687, 18688, 18689, 18690, 18691, 18692
	};
	// @formatter:on
	
	private static final int COLLECTION_RATE = 1;
	
	public StarStones()
	{
		addSkillSeeId(MOBS);
	}
	
	@Override
	public void onSkillSee(Npc npc, Player caster, Skill skill, List<WorldObject> targets, boolean isSummon)
	{
		if (skill.getId() == 932)
		{
			int itemId = 0;
			
			switch (npc.getId())
			{
				case 18684, 18685, 18686:
				{
					// give Red item
					itemId = 14009;
					break;
				}
				case 18687, 18688, 18689:
				{
					// give Blue item
					itemId = 14010;
					break;
				}
				case 18690, 18691, 18692:
				{
					// give Green item
					itemId = 14011;
					break;
				}
				default:
				{
					// unknown npc!
					return;
				}
			}
			if (getRandom(100) < 33)
			{
				caster.sendPacket(SystemMessageId.YOUR_COLLECTION_HAS_SUCCEEDED);
				caster.addItem(ItemProcessType.REWARD, itemId, getRandom(COLLECTION_RATE + 1, 2 * COLLECTION_RATE), null, true);
			}
			else if (((skill.getLevel() == 1) && (getRandom(100) < 15)) || ((skill.getLevel() == 2) && (getRandom(100) < 50)) || ((skill.getLevel() == 3) && (getRandom(100) < 75)))
			{
				caster.sendPacket(SystemMessageId.YOUR_COLLECTION_HAS_SUCCEEDED);
				caster.addItem(ItemProcessType.REWARD, itemId, getRandom(1, COLLECTION_RATE), null, true);
			}
			else
			{
				caster.sendPacket(SystemMessageId.THE_COLLECTION_HAS_FAILED);
			}
			npc.deleteMe();
		}
	}
}
