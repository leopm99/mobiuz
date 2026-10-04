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
package com.l2journey.gameserver.model.variables;

import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Player;

/**
 * @author KingHanker
 */
public class Statistics
{
	private Statistics()
	{
	}

	public static int getRealOnline()
	{
		int counter = 0;
		for (final Player onlinePlayer : World.getInstance().getPlayers())
		{
			if (onlinePlayer.isOnline() && ((onlinePlayer.getClient() != null) && !onlinePlayer.getClient().isDetached()))
			{
				counter++;
			}
		}
		final int realOnline = counter;
		return realOnline;
	}
	
	public static int getOffShops()
	{
		final int counter = getRealOnline();
		final int OffShop = World.getInstance().getAllPlayersCount() - counter;
		return OffShop;
	}
	
	public static String getTotalPlayersOn()
	{
		final String PlayersOn = String.valueOf(World.getInstance().getPlayers().size());
		return PlayersOn;
	}
}