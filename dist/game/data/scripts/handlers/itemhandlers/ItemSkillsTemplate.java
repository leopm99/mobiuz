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
package handlers.itemhandlers;

import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.handler.IItemHandler;
import com.l2journey.gameserver.model.actor.Playable;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.skill.holders.SkillHolder;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.ActionFailed;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;

/**
 * Template for item skills handler.
 * @author Zoey76
 */
public class ItemSkillsTemplate implements IItemHandler
{
	@Override
	public boolean useItem(Playable playable, Item item, boolean forceUse)
	{
		if (!playable.isPlayer() && !playable.isPet())
		{
			return false;
		}
		
		if (playable.isOnEvent())
		{
			playable.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Pets can use items only when they are tradable.
		if (playable.isPet() && !item.isTradeable())
		{
			playable.sendPacket(SystemMessageId.YOUR_PET_CANNOT_CARRY_THIS_ITEM);
			return false;
		}
		
		// Verify that item is not under reuse.
		if (!checkReuse(playable, null, item))
		{
			return false;
		}
		
		final SkillHolder[] skills = item.getEtcItem().getSkills();
		if (skills == null)
		{
			LOGGER.info(() -> "Item " + item + " does not have registered any skill for handler.");
			return false;
		}
		
		boolean hasConsumeSkill = false;
		for (SkillHolder skillInfo : skills)
		{
			if (skillInfo == null)
			{
				continue;
			}
			
			final Skill itemSkill = skillInfo.getSkill();
			if (itemSkill != null)
			{
				if (itemSkill.getItemConsumeId() > 0)
				{
					hasConsumeSkill = true;
				}
				
				// Verify that skill is not under reuse.
				if (!itemSkill.checkCondition(playable, playable.getTarget(), false) || playable.isSkillDisabled(itemSkill) || !checkReuse(playable, itemSkill, item))
				{
					return false;
				}
				
				if (!item.isPotion() && !item.isElixir() && !item.isScroll() && playable.isCastingNow())
				{
					return false;
				}
				
				if (itemSkill.isSimultaneousCast() || ((item.getTemplate().hasImmediateEffect() || item.getTemplate().hasExImmediateEffect()) && itemSkill.isStatic()))
				{
					playable.doSimultaneousCast(itemSkill);
					
					// Send message to the master only if skill was used successfully
					if (playable.isPet())
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_PET_USES_S1);
						sm.addSkillName(itemSkill);
						playable.sendPacket(sm);
					}
				}
				else
				{
					playable.getAI().setIntention(Intention.IDLE);
					if (!playable.useMagic(itemSkill, forceUse, false))
					{
						return false;
					}
					
					// Send message to the master only if skill was used successfully
					if (playable.isPet())
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_PET_USES_S1);
						sm.addSkillName(itemSkill);
						playable.sendPacket(sm);
					}
				}
				
				if (itemSkill.getReuseDelay() > 0)
				{
					playable.addTimeStamp(itemSkill, itemSkill.getReuseDelay());
				}
			}
		}
		
		if (checkConsume(item, hasConsumeSkill) && !playable.destroyItem(ItemProcessType.NONE, item.getObjectId(), 1, playable, false))
		{
			playable.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
			return false;
		}
		
		return true;
	}
	
	/**
	 * @param item the item being used
	 * @param hasConsumeSkill
	 * @return {@code true} check if item use consume item, {@code false} otherwise
	 */
	private boolean checkConsume(Item item, boolean hasConsumeSkill)
	{
		switch (item.getTemplate().getDefaultAction())
		{
			case CAPSULE, SKILL_REDUCE:
			{
				if (!hasConsumeSkill && item.getTemplate().hasImmediateEffect())
				{
					return true;
				}
			}
			default:
			{
				break;
			}
		}
		return false;
	}
	
	/**
	 * @param playable the character using the item or skill
	 * @param skill the skill being used, can be null
	 * @param item the item being used
	 * @return {@code true} if the the item or skill to check is available, {@code false} otherwise
	 */
	private boolean checkReuse(Playable playable, Skill skill, Item item)
	{
		final long remainingTime = (skill != null) ? playable.getSkillRemainingReuseTime(skill.getReuseHashCode()) : playable.getItemRemainingReuseTime(item.getObjectId());
		final boolean isAvailable = remainingTime <= 0;
		if (playable.isPlayer() && !isAvailable)
		{
			final int hours = (int) (remainingTime / 3600000);
			final int minutes = (int) (remainingTime % 3600000) / 60000;
			final int seconds = (int) ((remainingTime / 1000) % 60);
			SystemMessage sm = null;
			if (hours > 0)
			{
				sm = new SystemMessage(SystemMessageId.THERE_ARE_S2_HOUR_S_S3_MINUTE_S_AND_S4_SECOND_S_REMAINING_IN_S1_S_RE_USE_TIME);
				if ((skill == null) || skill.isStatic())
				{
					sm.addItemName(item);
				}
				else
				{
					sm.addSkillName(skill);
				}
				sm.addInt(hours);
				sm.addInt(minutes);
			}
			else if (minutes > 0)
			{
				sm = new SystemMessage(SystemMessageId.THERE_ARE_S2_MINUTE_S_S3_SECOND_S_REMAINING_IN_S1_S_RE_USE_TIME);
				if ((skill == null) || skill.isStatic())
				{
					sm.addItemName(item);
				}
				else
				{
					sm.addSkillName(skill);
				}
				sm.addInt(minutes);
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.THERE_ARE_S2_SECOND_S_REMAINING_IN_S1_S_RE_USE_TIME);
				if ((skill == null) || skill.isStatic())
				{
					sm.addItemName(item);
				}
				else
				{
					sm.addSkillName(skill);
				}
			}
			sm.addInt(seconds);
			playable.sendPacket(sm);
		}
		return isAvailable;
	}
}
