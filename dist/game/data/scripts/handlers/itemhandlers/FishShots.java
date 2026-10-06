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

import java.util.logging.Level;

import com.l2journey.gameserver.handler.IItemHandler;
import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Playable;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.Weapon;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.item.enums.ShotType;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.item.type.ActionType;
import com.l2journey.gameserver.model.item.type.WeaponType;
import com.l2journey.gameserver.model.skill.holders.SkillHolder;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.MagicSkillUse;
import com.l2journey.gameserver.util.Broadcast;

/**
 * @author -Nemesiss-
 */
public class FishShots implements IItemHandler
{
	@Override
	public boolean useItem(Playable playable, Item item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.YOUR_PET_CANNOT_CARRY_THIS_ITEM);
			return false;
		}
		
		final Player player = playable.asPlayer();
		final Item weaponInst = player.getActiveWeaponInstance();
		final Weapon weaponItem = player.getActiveWeaponItem();
		if ((weaponInst == null) || (weaponItem.getItemType() != WeaponType.FISHINGROD) || player.isChargedShot(ShotType.FISH_SOULSHOTS))
		{
			return false;
		}
		
		final long count = item.getCount();
		final SkillHolder[] skills = item.getTemplate().getSkills();
		if (skills == null)
		{
			LOGGER.log(Level.WARNING, () -> getClass().getSimpleName() + ": is missing skills!");
			return false;
		}
		
		final boolean gradeCheck = item.isEtcItem() && (item.getEtcItem().getDefaultAction() == ActionType.FISHINGSHOT) && (weaponInst.getTemplate().getCrystalTypePlus() == item.getTemplate().getCrystalTypePlus());
		if (!gradeCheck)
		{
			player.sendPacket(SystemMessageId.THAT_IS_THE_WRONG_GRADE_OF_SOULSHOT_FOR_THAT_FISHING_POLE);
			return false;
		}
		
		if (count < 1)
		{
			return false;
		}
		
		player.setChargedShot(ShotType.FISH_SOULSHOTS, true);
		player.destroyItem(ItemProcessType.NONE, item.getObjectId(), 1, null, false);
		final WorldObject oldTarget = player.getTarget();
		player.setTarget(player);
		
		Broadcast.toSelfAndKnownPlayers(player, new MagicSkillUse(player, skills[0].getSkillId(), skills[0].getSkillLevel(), 0, 0));
		player.setTarget(oldTarget);
		return true;
	}
}
