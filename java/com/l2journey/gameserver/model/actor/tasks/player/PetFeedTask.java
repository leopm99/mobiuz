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
package com.l2journey.gameserver.model.actor.tasks.player;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.gameserver.data.xml.PetDataTable;
import com.l2journey.gameserver.handler.IItemHandler;
import com.l2journey.gameserver.handler.ItemHandler;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;

/**
 * Task dedicated for feeding player's pet.
 * @author UnAfraid
 */
public class PetFeedTask implements Runnable
{
	private static final Logger LOGGER = Logger.getLogger(PetFeedTask.class.getName());
	
	private final Player _player;
	
	public PetFeedTask(Player player)
	{
		_player = player;
	}
	
	@Override
	public void run()
	{
		try
		{
			if (!_player.isMounted() || (_player.getMountNpcId() == 0) || (PetDataTable.getInstance().getPetData(_player.getMountNpcId()) == null))
			{
				_player.stopFeed();
				return;
			}
			
			if (_player.getCurrentFeed() > _player.getFeedConsume())
			{
				// eat
				_player.setCurrentFeed(_player.getCurrentFeed() - _player.getFeedConsume());
			}
			else
			{
				// go back to pet control item, or simply said, unsummon it
				_player.setCurrentFeed(0);
				_player.stopFeed();
				_player.dismount();
				_player.sendPacket(SystemMessageId.YOU_ARE_OUT_OF_FEED_MOUNT_STATUS_CANCELED);
				return;
			}
			
			final Set<Integer> foodIds = PetDataTable.getInstance().getPetData(_player.getMountNpcId()).getFood();
			if (foodIds.isEmpty())
			{
				return;
			}
			
			Item food = null;
			for (int id : foodIds)
			{
				// TODO: possibly pet inv?
				food = _player.getInventory().getItemByItemId(id);
				if (food != null)
				{
					break;
				}
			}
			
			if ((food != null) && _player.isHungry())
			{
				final IItemHandler handler = ItemHandler.getInstance().getHandler(food.getEtcItem());
				if (handler != null)
				{
					handler.useItem(_player, food, false);
					final SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_PET_WAS_HUNGRY_SO_IT_ATE_S1);
					sm.addItemName(food.getId());
					_player.sendPacket(sm);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, e, () -> "Mounted Pet [NpcId: " + _player.getMountNpcId() + "] a feed task error has occurred");
		}
	}
}
