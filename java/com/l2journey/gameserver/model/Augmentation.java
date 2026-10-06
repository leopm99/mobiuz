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
package com.l2journey.gameserver.model;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.Config;
import com.l2journey.gameserver.data.xml.OptionData;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.options.Options;

/**
 * Used to store an augmentation and its bonuses.
 * @author durgus
 */
public class Augmentation
{
	private int _effectsId = 0;
	private AugmentationStatBonus _bonus = null;
	
	public Augmentation(int effects)
	{
		_effectsId = effects;
		_bonus = new AugmentationStatBonus(_effectsId);
	}
	
	public static class AugmentationStatBonus
	{
		private static final Logger LOGGER = Logger.getLogger(AugmentationStatBonus.class.getName());
		private final List<Options> _options = new ArrayList<>();
		private boolean _active;
		
		public AugmentationStatBonus(int augmentationId)
		{
			_active = false;
			final int[] stats = new int[2];
			stats[0] = 0x0000FFFF & augmentationId;
			stats[1] = (augmentationId >> 16);
			for (int stat : stats)
			{
				final Options op = OptionData.getInstance().getOptions(stat);
				if (op != null)
				{
					_options.add(op);
				}
				else
				{
					LOGGER.log(Level.WARNING, () -> getClass().getSimpleName() + ": Couldn't find option: " + stat);
				}
			}
		}
		
		public void applyBonus(Player player)
		{
			// make sure the bonuses are not applied twice..
			if (_active)
			{
				return;
			}
			
			for (Options op : _options)
			{
				op.apply(player);
			}
			
			_active = true;
		}
		
		public void removeBonus(Player player)
		{
			if (Config.BLOCK_AUGMENT_IN_OLY && player.isInOlympiadMode())
			{
				player.sendMessage("You can not use augment skills in Olympiad");
				return;
			}
			
			// make sure the bonuses are not removed twice
			if (!_active)
			{
				return;
			}
			
			for (Options op : _options)
			{
				op.remove(player);
			}
			
			_active = false;
		}
	}
	
	/**
	 * Get the augmentation "id" used in serverpackets.
	 * @return augmentationId
	 */
	public int getAugmentationId()
	{
		return _effectsId;
	}
	
	/**
	 * Applies the bonuses to the player.
	 * @param player
	 */
	public void applyBonus(Player player)
	{
		_bonus.applyBonus(player);
	}
	
	/**
	 * Removes the augmentation bonuses from the player.
	 * @param player
	 */
	public void removeBonus(Player player)
	{
		_bonus.removeBonus(player);
	}
}
