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
 * Chamber of Delusion Square.
 * @author GKR
 */
public class ChamberOfDelusionSquare extends Chamber
{
	// NPCs
	private static final int ENTRANCE_GATEKEEPER = 32662;
	private static final int ROOM_GATEKEEPER_FIRST = 32684;
	private static final int ROOM_GATEKEEPER_LAST = 32692;
	private static final int AENKINEL = 25694;
	private static final int BOX = 18820;
	
	// Misc
	private static final Location[] ENTER_POINTS =
	{
		new Location(-122368, -153388, -6688),
		new Location(-122368, -152524, -6688),
		new Location(-120480, -155116, -6688),
		new Location(-120480, -154236, -6688),
		new Location(-121440, -151212, -6688),
		new Location(-120464, -152908, -6688),
		new Location(-122368, -154700, -6688),
		new Location(-121440, -152908, -6688),
		new Location(-121440, -154572, -6688), // Raid room
	};
	private static final int INSTANCEID = 131;
	
	private ChamberOfDelusionSquare()
	{
		super(INSTANCEID, ENTRANCE_GATEKEEPER, ROOM_GATEKEEPER_FIRST, ROOM_GATEKEEPER_LAST, AENKINEL, BOX);
		roomEnterPoints = ENTER_POINTS;
	}
	
	public static void main(String[] args)
	{
		new ChamberOfDelusionSquare();
	}
}