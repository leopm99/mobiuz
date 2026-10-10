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
package handlers.targethandlers;

import java.util.Collections;
import java.util.List;

import com.l2journey.gameserver.handler.ITargetTypeHandler;
import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.skill.targets.TargetType;
import com.l2journey.gameserver.network.SystemMessageId;

/**
 * @author UnAfraid
 */
public class PartyOther implements ITargetTypeHandler
{
	@Override
	public List<WorldObject> getTargetList(Skill skill, Creature creature, boolean onlyFirst, Creature target)
	{
		if ((target != null) && (target != creature) && creature.isInParty() && target.isInParty() && (creature.getParty().getLeaderObjectId() == target.getParty().getLeaderObjectId()))
		{
			if (!target.isDead())
			{
				if (target.isPlayer())
				{
					switch (skill.getId())
					{
						// FORCE BUFFS may cancel here but there should be a proper condition.
						case 426:
						{
							if (!target.asPlayer().isMageClass())
							{
								return Collections.singletonList(target);
							}
							return Collections.emptyList();
						}
						case 427:
						{
							if (target.asPlayer().isMageClass())
							{
								return Collections.singletonList(target);
							}
							return Collections.emptyList();
						}
						default:
						{
							break;
						}
					}
				}
				return Collections.singletonList(target);
			}
			return Collections.emptyList();
		}
		
		creature.sendPacket(SystemMessageId.THAT_IS_AN_INCORRECT_TARGET);
		return Collections.emptyList();
	}
	
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.PARTY_OTHER;
	}
}
