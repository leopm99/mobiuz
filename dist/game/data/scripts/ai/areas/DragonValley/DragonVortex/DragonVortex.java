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
package ai.areas.DragonValley.DragonVortex;

import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;

import ai.AbstractNpcAI;

/**
 * Dragon Vortex AI.
 * @author Adry_85
 * @since 2.6.0.0
 */
public class DragonVortex extends AbstractNpcAI
{
	// NPC
	private static final int DRAGON_VORTEX = 32871;
	// Raids
	private static final int EMERALD_HORN = 25718;
	private static final int DUST_RIDER = 25719;
	private static final int BLEEDING_FLY = 25720;
	private static final int BLACKDAGGER_WING = 25721;
	private static final int SHADOW_SUMMONER = 25722;
	private static final int SPIKE_SLASHER = 25723;
	private static final int MUSCLE_BOMBER = 25724;
	// Item
	private static final int LARGE_DRAGON_BONE = 17248;
	// Variables
	private static final String I_QUEST0 = "I_QUEST0";
	// Locations
	private static final Location SPOT_1 = new Location(92744, 114045, -3072);
	private static final Location SPOT_2 = new Location(110112, 124976, -3624);
	private static final Location SPOT_3 = new Location(121637, 113657, -3792);
	private static final Location SPOT_4 = new Location(109346, 111849, -3040);
	
	private DragonVortex()
	{
		addStartNpc(DRAGON_VORTEX);
		addFirstTalkId(DRAGON_VORTEX);
		addTalkId(DRAGON_VORTEX);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "RAIDBOSS":
			{
				if (hasQuestItems(player, LARGE_DRAGON_BONE))
				{
					if (!npc.getVariables().getBoolean(I_QUEST0, false))
					{
						takeItems(player, LARGE_DRAGON_BONE, 1);
						final int random = getRandom(100);
						int raid = 0;
						if (random < 3)
						{
							raid = MUSCLE_BOMBER;
						}
						else if (random < 8)
						{
							raid = SHADOW_SUMMONER;
						}
						else if (random < 15)
						{
							raid = SPIKE_SLASHER;
						}
						else if (random < 25)
						{
							raid = BLACKDAGGER_WING;
						}
						else if (random < 45)
						{
							raid = BLEEDING_FLY;
						}
						else if (random < 67)
						{
							raid = DUST_RIDER;
						}
						else
						{
							raid = EMERALD_HORN;
						}
						
						Location loc = null;
						switch (npc.getX())
						{
							case 92225:
							{
								loc = SPOT_1;
								break;
							}
							case 110116:
							{
								loc = SPOT_2;
								break;
							}
							case 121172:
							{
								loc = SPOT_3;
								break;
							}
							case 108924:
							{
								loc = SPOT_4;
								break;
							}
							default:
							{
								break;
							}
						}
						
						npc.getVariables().set(I_QUEST0, true);
						addSpawn(raid, loc, false, 0, true);
						startQuestTimer("CANSPAWN", 60000, npc, null);
					}
					else
					{
						return "32871-02.html";
					}
				}
				else
				{
					return "32871-01.html";
				}
				break;
			}
			case "CANSPAWN":
			{
				npc.getVariables().set(I_QUEST0, false);
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
		new DragonVortex();
	}
}