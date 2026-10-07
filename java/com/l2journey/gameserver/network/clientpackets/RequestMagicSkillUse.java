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
package com.l2journey.gameserver.network.clientpackets;

import com.l2journey.Config;
import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.effects.EffectType;
import com.l2journey.gameserver.model.skill.CommonSkill;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.skill.targets.TargetType;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.ActionFailed;

public class RequestMagicSkillUse extends ClientPacket
{
	private int _magicId;
	private boolean _ctrlPressed;
	private boolean _shiftPressed;
	
	@Override
	protected void readImpl()
	{
		_magicId = readInt(); // Identifier of the used skill
		_ctrlPressed = readInt() != 0; // True if it's a ForceAttack : Ctrl pressed
		_shiftPressed = readByte() != 0; // True if Shift pressed
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (player.isDead())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isFakeDeath())
		{
			if (_magicId != CommonSkill.FAKE_DEATH.getId())
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_MOVE_WHILE_SITTING);
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			else if (player.isSkillDisabled(CommonSkill.FAKE_DEATH.getSkill()))
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}
		
		// Get the level of the used skill
		Skill skill = player.getKnownSkill(_magicId);
		if (skill == null)
		{
			// Player doesn't know this skill, maybe it's the display Id.
			skill = player.getCustomSkill(_magicId);
			if (skill == null)
			{
				skill = player.getTransformSkill(_magicId);
				if (skill == null)
				{
					player.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
		}
		
		// Avoid Use of Skills in AirShip.
		if (player.isPlayable() && player.isInAirShip())
		{
			player.sendPacket(SystemMessageId.THIS_ACTION_IS_PROHIBITED_WHILE_MOUNTED_OR_ON_AN_AIRSHIP);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if ((player.isTransformed() || player.isInStance()) && !player.hasTransformSkill(skill.getId()))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// If Alternate rule Karma punishment is set to true, forbid skill Return to player with Karma
		if (!Config.ALT_GAME_KARMA_PLAYER_CAN_TELEPORT && (player.getKarma() > 0) && skill.hasEffectType(EffectType.TELEPORT))
		{
			return;
		}
		
		// players mounted on pets cannot use any toggle skills
		if (skill.isToggle() && player.isMounted())
		{
			return;
		}
		
		player.onActionRequest();
		
		// Stop if use self-buff (except if on AirShip or Boat).
		if ((skill.isContinuous() && !skill.isDebuff() && (skill.getTargetType() == TargetType.SELF)) && (!player.isInAirShip() || !player.isInBoat()))
		{
			player.getAI().setIntention(Intention.MOVE_TO, player.getLocation());
		}
		
		player.useMagic(skill, _ctrlPressed, _shiftPressed);
	}
}
