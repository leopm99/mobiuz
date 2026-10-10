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
package vehicles;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.managers.BoatManager;
import com.l2journey.gameserver.model.VehiclePathPoint;
import com.l2journey.gameserver.model.actor.instance.Boat;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.enums.ChatType;
import com.l2journey.gameserver.network.serverpackets.CreatureSay;
import com.l2journey.gameserver.network.serverpackets.PlaySound;

/**
 * @author DS
 */
public class BoatRunePrimeval implements Runnable
{
	private static final Logger LOGGER = Logger.getLogger(BoatRunePrimeval.class.getName());
	
	// Time: 239s
	private static final VehiclePathPoint[] RUNE_TO_PRIMEVAL =
	{
		new VehiclePathPoint(32750, -39300, -3610, 180, 800),
		new VehiclePathPoint(27440, -39328, -3610, 250, 1000),
		new VehiclePathPoint(19616, -39360, -3610, 270, 1000),
		new VehiclePathPoint(3840, -38528, -3610, 270, 1000),
		new VehiclePathPoint(1664, -37120, -3610, 270, 1000),
		new VehiclePathPoint(896, -34560, -3610, 180, 1800),
		new VehiclePathPoint(832, -31104, -3610, 180, 180),
		new VehiclePathPoint(2240, -29132, -3610, 150, 1800),
		new VehiclePathPoint(4160, -27828, -3610, 150, 1800),
		new VehiclePathPoint(5888, -27279, -3610, 150, 1800),
		new VehiclePathPoint(7000, -27279, -3610, 150, 1800),
		new VehiclePathPoint(10342, -27279, -3610, 150, 1800)
	};
	
	// Time: 221s
	private static final VehiclePathPoint[] PRIMEVAL_TO_RUNE =
	{
		new VehiclePathPoint(15528, -27279, -3610, 180, 800),
		new VehiclePathPoint(22304, -29664, -3610, 290, 800),
		new VehiclePathPoint(33824, -26880, -3610, 290, 800),
		new VehiclePathPoint(38848, -21792, -3610, 240, 1200),
		new VehiclePathPoint(43424, -22080, -3610, 180, 1800),
		new VehiclePathPoint(44320, -25152, -3610, 180, 1800),
		new VehiclePathPoint(40576, -31616, -3610, 250, 800),
		new VehiclePathPoint(36819, -35315, -3610, 220, 800)
	};
	
	private static final VehiclePathPoint[] RUNE_DOCK =
	{
		new VehiclePathPoint(34381, -37680, -3610, 220, 800)
	};
	
	private static final VehiclePathPoint PRIMEVAL_DOCK = RUNE_TO_PRIMEVAL[RUNE_TO_PRIMEVAL.length - 1];
	
	private final Boat _boat;
	private int _cycle = 0;
	private int _shoutCount = 0;
	
	private final CreatureSay arrivedAtRune;
	private final CreatureSay arrivedAtRune2;
	private final CreatureSay leavingRune;
	private final CreatureSay arrivedAtPrimeval;
	private final CreatureSay arrivedAtPrimeval2;
	private final CreatureSay leavingPrimeval;
	private final CreatureSay busyRune;
	
	private final PlaySound runeSound;
	private final PlaySound primevalSound;
	
	public BoatRunePrimeval(Boat boat)
	{
		_boat = boat;
		
		arrivedAtRune = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.WELCOME_TO_RUNE_HARBOR);
		arrivedAtRune2 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_WILL_LEAVE_FOR_PRIMEVAL_ISLE_AFTER_ANCHORING_FOR_THREE_MINUTES);
		leavingRune = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_IS_NOW_DEPARTING_RUNE_HARBOR_FOR_PRIMEVAL_ISLE);
		arrivedAtPrimeval = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_HAS_ARRIVED_AT_PRIMEVAL_ISLE);
		arrivedAtPrimeval2 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_WILL_LEAVE_FOR_RUNE_HARBOR_AFTER_ANCHORING_FOR_THREE_MINUTES);
		leavingPrimeval = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_IS_NOW_DEPARTING_PRIMEVAL_ISLE_FOR_RUNE_HARBOR);
		busyRune = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_FROM_PRIMEVAL_ISLE_TO_RUNE_HARBOR_HAS_BEEN_DELAYED);
		
		runeSound = new PlaySound(0, "itemsound.ship_arrival_departure", 1, _boat.getObjectId(), RUNE_DOCK[0].getX(), RUNE_DOCK[0].getY(), RUNE_DOCK[0].getZ());
		primevalSound = new PlaySound(0, "itemsound.ship_arrival_departure", 1, _boat.getObjectId(), PRIMEVAL_DOCK.getX(), PRIMEVAL_DOCK.getY(), PRIMEVAL_DOCK.getZ());
	}
	
	@Override
	public void run()
	{
		try
		{
			switch (_cycle)
			{
				case 0:
				{
					BoatManager.getInstance().dockShip(BoatManager.RUNE_HARBOR, false);
					BoatManager.getInstance().broadcastPackets(RUNE_DOCK[0], PRIMEVAL_DOCK, leavingRune, runeSound);
					_boat.payForRide(8925, 1, 34513, -38009, -3640);
					_boat.executePath(RUNE_TO_PRIMEVAL);
					break;
				}
				case 1:
				{
					BoatManager.getInstance().broadcastPackets(PRIMEVAL_DOCK, RUNE_DOCK[0], arrivedAtPrimeval, arrivedAtPrimeval2, primevalSound);
					ThreadPool.schedule(this, 180000);
					break;
				}
				case 2:
				{
					BoatManager.getInstance().broadcastPackets(PRIMEVAL_DOCK, RUNE_DOCK[0], leavingPrimeval, primevalSound);
					_boat.payForRide(8924, 1, 10447, -24982, -3664);
					_boat.executePath(PRIMEVAL_TO_RUNE);
					break;
				}
				case 3:
				{
					if (BoatManager.getInstance().dockBusy(BoatManager.RUNE_HARBOR))
					{
						if (_shoutCount == 0)
						{
							BoatManager.getInstance().broadcastPacket(RUNE_DOCK[0], PRIMEVAL_DOCK, busyRune);
						}
						_shoutCount++;
						if (_shoutCount > 35)
						{
							_shoutCount = 0;
						}
						ThreadPool.schedule(this, 5000);
						return;
					}
					_boat.executePath(RUNE_DOCK);
					break;
				}
				case 4:
				{
					BoatManager.getInstance().dockShip(BoatManager.RUNE_HARBOR, true);
					BoatManager.getInstance().broadcastPackets(RUNE_DOCK[0], PRIMEVAL_DOCK, arrivedAtRune, arrivedAtRune2, runeSound);
					ThreadPool.schedule(this, 180000);
					break;
				}
			}
			_shoutCount = 0;
			_cycle++;
			if (_cycle > 4)
			{
				_cycle = 0;
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage());
		}
	}
	
	public static void main(String[] args)
	{
		final Boat boat = BoatManager.getInstance().getNewBoat(5, 34381, -37680, -3610, 40785);
		if (boat != null)
		{
			boat.registerEngine(new BoatRunePrimeval(boat));
			boat.runEngine(180000);
			BoatManager.getInstance().dockShip(BoatManager.RUNE_HARBOR, true);
		}
	}
}
