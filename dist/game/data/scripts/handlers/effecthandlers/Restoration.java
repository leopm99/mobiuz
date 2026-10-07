/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package handlers.effecthandlers;

import com.l2journey.gameserver.model.StatSet;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.conditions.Condition;
import com.l2journey.gameserver.model.effects.AbstractEffect;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.PetItemList;

/**
 * Restoration effect implementation.
 * @author Zoey76
 */
public class Restoration extends AbstractEffect
{
	private final int _itemId;
	private final int _itemCount;
	
	public Restoration(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_itemId = params.getInt("itemId", 0);
		_itemCount = params.getInt("itemCount", 0);
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill)
	{
		if ((effected == null) || !effected.isPlayable())
		{
			return;
		}
		
		if ((_itemId <= 0) || (_itemCount <= 0))
		{
			effected.sendPacket(SystemMessageId.THERE_WAS_NOTHING_FOUND_INSIDE);
			LOGGER.warning(() -> Restoration.class.getSimpleName() + " effect with wrong item Id/count: " + _itemId + "/" + _itemCount + "!");
			return;
		}
		
		if (effected.isPlayer())
		{
			effected.asPlayer().addItem(ItemProcessType.REWARD, _itemId, _itemCount, effector, true);
		}
		else if (effected.isPet())
		{
			final Player player = effected.asPlayer();
			effected.getInventory().addItem(ItemProcessType.REWARD, _itemId, _itemCount, player, effector);
			player.sendPacket(new PetItemList(effected.getInventory().getItems()));
		}
	}
}
