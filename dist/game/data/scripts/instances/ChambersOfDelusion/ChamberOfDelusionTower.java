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
package instances.ChambersOfDelusion;

import com.l2journey.gameserver.model.Location;

/**
 * Chamber of Delusion Tower.
 * @author GKR
 */
public class ChamberOfDelusionTower extends Chamber
{
	// NPCs
	private static final int ENTRANCE_GATEKEEPER = 32663;
	private static final int ROOM_GATEKEEPER_FIRST = 32693;
	private static final int ROOM_GATEKEEPER_LAST = 32701;
	private static final int AENKINEL = 25695;
	private static final int BOX = 18823;
	
	// Misc
	private static final Location[] ENTER_POINTS =
	{
		new Location(-108976, -153372, -6688),
		new Location(-108960, -152524, -6688),
		new Location(-107088, -155052, -6688),
		new Location(-107104, -154236, -6688),
		new Location(-108048, -151244, -6688),
		new Location(-107088, -152956, -6688),
		new Location(-108992, -154604, -6688),
		new Location(-108032, -152892, -6688),
		new Location(-108048, -154572, -6688), // Raid room
	};
	private static final int INSTANCEID = 132; // this is the client number
	
	private ChamberOfDelusionTower()
	{
		super(INSTANCEID, ENTRANCE_GATEKEEPER, ROOM_GATEKEEPER_FIRST, ROOM_GATEKEEPER_LAST, AENKINEL, BOX);
		roomEnterPoints = ENTER_POINTS;
	}
	
	public static void main(String[] args)
	{
		new ChamberOfDelusionTower();
	}
}