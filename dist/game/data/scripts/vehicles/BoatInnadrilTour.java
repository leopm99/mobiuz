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
public class BoatInnadrilTour implements Runnable
{
	private static final Logger LOGGER = Logger.getLogger(BoatInnadrilTour.class.getName());
	
	// Time: 1867s
	private static final VehiclePathPoint[] TOUR =
	{
		new VehiclePathPoint(105129, 226240, -3610, 150, 800),
		new VehiclePathPoint(90604, 238797, -3610, 150, 800),
		new VehiclePathPoint(74853, 237943, -3610, 150, 800),
		new VehiclePathPoint(68207, 235399, -3610, 150, 800),
		new VehiclePathPoint(63226, 230487, -3610, 150, 800),
		new VehiclePathPoint(61843, 224797, -3610, 150, 800),
		new VehiclePathPoint(61822, 203066, -3610, 150, 800),
		new VehiclePathPoint(59051, 197685, -3610, 150, 800),
		new VehiclePathPoint(54048, 195298, -3610, 150, 800),
		new VehiclePathPoint(41609, 195687, -3610, 150, 800),
		new VehiclePathPoint(35821, 200284, -3610, 150, 800),
		new VehiclePathPoint(35567, 205265, -3610, 150, 800),
		new VehiclePathPoint(35617, 222471, -3610, 150, 800),
		new VehiclePathPoint(37932, 226588, -3610, 150, 800),
		new VehiclePathPoint(42932, 229394, -3610, 150, 800),
		new VehiclePathPoint(74324, 245231, -3610, 150, 800),
		new VehiclePathPoint(81872, 250314, -3610, 150, 800),
		new VehiclePathPoint(101692, 249882, -3610, 150, 800),
		new VehiclePathPoint(107907, 256073, -3610, 150, 800),
		new VehiclePathPoint(112317, 257133, -3610, 150, 800),
		new VehiclePathPoint(126273, 255313, -3610, 150, 800),
		new VehiclePathPoint(128067, 250961, -3610, 150, 800),
		new VehiclePathPoint(128520, 238249, -3610, 150, 800),
		new VehiclePathPoint(126428, 235072, -3610, 150, 800),
		new VehiclePathPoint(121843, 234656, -3610, 150, 800),
		new VehiclePathPoint(120096, 234268, -3610, 150, 800),
		new VehiclePathPoint(118572, 233046, -3610, 150, 800),
		new VehiclePathPoint(117671, 228951, -3610, 150, 800),
		new VehiclePathPoint(115936, 226540, -3610, 150, 800),
		new VehiclePathPoint(113628, 226240, -3610, 150, 800),
		new VehiclePathPoint(111300, 226240, -3610, 150, 800),
		new VehiclePathPoint(111264, 226240, -3610, 150, 800)
	};
	
	private static final VehiclePathPoint DOCK = TOUR[TOUR.length - 1];
	
	private final Boat _boat;
	private int _cycle = 0;
	
	private final CreatureSay arrivedAtInnadril;
	private final CreatureSay leaveInnadril5;
	private final CreatureSay leaveInnadril1;
	private final CreatureSay leaveInnadril0;
	private final CreatureSay leavingInnadril;
	
	private final CreatureSay arrival20;
	private final CreatureSay arrival15;
	private final CreatureSay arrival10;
	private final CreatureSay arrival5;
	private final CreatureSay arrival1;
	
	private final PlaySound innadrilSound;
	
	public BoatInnadrilTour(Boat boat)
	{
		_boat = boat;
		
		arrivedAtInnadril = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_INNADRIL_PLEASURE_BOAT_HAS_ARRIVED_IT_WILL_ANCHOR_FOR_TEN_MINUTES);
		leaveInnadril5 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_INNADRIL_PLEASURE_BOAT_WILL_LEAVE_IN_FIVE_MINUTES);
		leaveInnadril1 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_INNADRIL_PLEASURE_BOAT_WILL_LEAVE_IN_ONE_MINUTE);
		leaveInnadril0 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_INNADRIL_PLEASURE_BOAT_WILL_BE_LEAVING_SOON);
		leavingInnadril = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_INNADRIL_PLEASURE_BOAT_IS_LEAVING);
		
		arrival20 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_INNADRIL_PLEASURE_BOAT_WILL_ARRIVE_IN_APPROXIMATELY_20_MINUTES);
		arrival15 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_INNADRIL_PLEASURE_BOAT_WILL_ARRIVE_IN_APPROXIMATELY_15_MINUTES);
		arrival10 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_INNADRIL_PLEASURE_BOAT_WILL_ARRIVE_IN_APPROXIMATELY_10_MINUTES);
		arrival5 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_INNADRIL_PLEASURE_BOAT_WILL_ARRIVE_IN_APPROXIMATELY_5_MINUTES);
		arrival1 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_INNADRIL_PLEASURE_BOAT_WILL_ARRIVE_IN_APPROXIMATELY_1_MINUTE);
		
		innadrilSound = new PlaySound(0, "itemsound.ship_arrival_departure", 1, _boat.getObjectId(), DOCK.getX(), DOCK.getY(), DOCK.getZ());
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
					BoatManager.getInstance().broadcastPacket(DOCK, DOCK, leaveInnadril5);
					ThreadPool.schedule(this, 240000);
					break;
				}
				case 1:
				{
					BoatManager.getInstance().broadcastPacket(DOCK, DOCK, leaveInnadril1);
					ThreadPool.schedule(this, 40000);
					break;
				}
				case 2:
				{
					BoatManager.getInstance().broadcastPacket(DOCK, DOCK, leaveInnadril0);
					ThreadPool.schedule(this, 20000);
					break;
				}
				case 3:
				{
					BoatManager.getInstance().broadcastPackets(DOCK, DOCK, leavingInnadril, innadrilSound);
					_boat.payForRide(0, 1, 107092, 219098, -3952);
					_boat.executePath(TOUR);
					ThreadPool.schedule(this, 650000);
					break;
				}
				case 4:
				{
					BoatManager.getInstance().broadcastPacket(DOCK, DOCK, arrival20);
					ThreadPool.schedule(this, 300000);
					break;
				}
				case 5:
				{
					BoatManager.getInstance().broadcastPacket(DOCK, DOCK, arrival15);
					ThreadPool.schedule(this, 300000);
					break;
				}
				case 6:
				{
					BoatManager.getInstance().broadcastPacket(DOCK, DOCK, arrival10);
					ThreadPool.schedule(this, 300000);
					break;
				}
				case 7:
				{
					BoatManager.getInstance().broadcastPacket(DOCK, DOCK, arrival5);
					ThreadPool.schedule(this, 240000);
					break;
				}
				case 8:
				{
					BoatManager.getInstance().broadcastPacket(DOCK, DOCK, arrival1);
					break;
				}
				case 9:
				{
					BoatManager.getInstance().broadcastPackets(DOCK, DOCK, arrivedAtInnadril, innadrilSound);
					ThreadPool.schedule(this, 300000);
					break;
				}
			}
			_cycle++;
			if (_cycle > 9)
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
		final Boat boat = BoatManager.getInstance().getNewBoat(4, 111264, 226240, -3610, 32768);
		if (boat != null)
		{
			boat.registerEngine(new BoatInnadrilTour(boat));
			boat.runEngine(180000);
		}
	}
}
