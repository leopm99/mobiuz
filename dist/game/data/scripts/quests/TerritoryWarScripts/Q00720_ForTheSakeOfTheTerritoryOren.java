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
package quests.TerritoryWarScripts;

import com.l2journey.gameserver.network.NpcStringId;

/**
 * For the Sake of the Territory - Oren (720)
 * @author Gigiikun
 */
public class Q00720_ForTheSakeOfTheTerritoryOren extends TerritoryWarSuperClass
{
	public Q00720_ForTheSakeOfTheTerritoryOren()
	{
		super(720);
		catapultId = 36502;
		territoryId = 84;
		leaderIds = new int[]
		{
			36526,
			36528,
			36531,
			36594
		};
		guardIds = new int[]
		{
			36527,
			36529,
			36530
		};
		npcString = new NpcStringId[]
		{
			NpcStringId.THE_CATAPULT_OF_OREN_HAS_BEEN_DESTROYED
		};
		registerKillIds();
	}
}
