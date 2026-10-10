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
public class BoatTalkingGludin implements Runnable
{
	private static final Logger LOGGER = Logger.getLogger(BoatTalkingGludin.class.getName());
	
	// Time: 919s
	private static final VehiclePathPoint[] TALKING_TO_GLUDIN =
	{
		new VehiclePathPoint(-121385, 261660, -3610, 180, 800),
		new VehiclePathPoint(-127694, 253312, -3610, 200, 800),
		new VehiclePathPoint(-129274, 237060, -3610, 250, 800),
		new VehiclePathPoint(-114688, 139040, -3610, 200, 800),
		new VehiclePathPoint(-109663, 135704, -3610, 180, 800),
		new VehiclePathPoint(-102151, 135704, -3610, 180, 800),
		new VehiclePathPoint(-96686, 140595, -3610, 180, 800),
		new VehiclePathPoint(-95686, 147718, -3610, 180, 1800)
	};
	
	private static final VehiclePathPoint[] GLUDIN_DOCK =
	{
		new VehiclePathPoint(-95686, 150514, -3610, 150, 800)
	};
	
	// Time: 780s
	private static final VehiclePathPoint[] GLUDIN_TO_TALKING =
	{
		new VehiclePathPoint(-95686, 155514, -3610, 180, 800),
		new VehiclePathPoint(-95686, 185514, -3610, 250, 800),
		new VehiclePathPoint(-60136, 238816, -3610, 200, 800),
		new VehiclePathPoint(-60520, 259609, -3610, 180, 1800),
		new VehiclePathPoint(-65344, 261460, -3610, 180, 1800),
		new VehiclePathPoint(-83344, 261560, -3610, 180, 1800),
		new VehiclePathPoint(-88344, 261660, -3610, 180, 1800),
		new VehiclePathPoint(-92344, 261660, -3610, 150, 1800),
		new VehiclePathPoint(-94242, 261660, -3610, 150, 1800)
	};
	
	private static final VehiclePathPoint[] TALKING_DOCK =
	{
		new VehiclePathPoint(-96622, 261660, -3610, 150, 1800)
	};
	
	private final Boat _boat;
	private int _cycle = 0;
	private int _shoutCount = 0;
	
	private final CreatureSay arrivedAtTalking;
	private final CreatureSay arrivedAtTalking2;
	private final CreatureSay leaveTalking5;
	private final CreatureSay leaveTalking1;
	private final CreatureSay leaveTalking12;
	private final CreatureSay leaveTalking0;
	private final CreatureSay leavingTalking;
	private final CreatureSay arrivedAtGludin;
	private final CreatureSay arrivedAtGludin2;
	private final CreatureSay leaveGludin5;
	private final CreatureSay leaveGludin1;
	private final CreatureSay leaveGludin0;
	private final CreatureSay leavingGludin;
	private final CreatureSay busyTalking;
	private final CreatureSay busyGludin;
	
	private final CreatureSay arrivalGludin10;
	private final CreatureSay arrivalGludin5;
	private final CreatureSay arrivalGludin1;
	private final CreatureSay arrivalTalking10;
	private final CreatureSay arrivalTalking5;
	private final CreatureSay arrivalTalking1;
	
	private final PlaySound talkingSound;
	private final PlaySound gludinSound;
	
	public BoatTalkingGludin(Boat boat)
	{
		_boat = boat;
		_cycle = 0;
		
		arrivedAtTalking = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_HAS_ARRIVED_AT_TALKING_ISLAND_HARBOR);
		arrivedAtTalking2 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_WILL_LEAVE_FOR_GLUDIN_HARBOR_AFTER_ANCHORING_FOR_TEN_MINUTES);
		leaveTalking5 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_WILL_LEAVE_FOR_GLUDIN_HARBOR_IN_FIVE_MINUTES);
		leaveTalking1 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_WILL_LEAVE_FOR_GLUDIN_HARBOR_IN_ONE_MINUTE);
		leaveTalking12 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THOSE_WISHING_TO_RIDE_THE_FERRY_SHOULD_MAKE_HASTE_TO_GET_ON);
		leaveTalking0 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_WILL_BE_LEAVING_SOON_FOR_GLUDIN_HARBOR);
		leavingTalking = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_IS_LEAVING_FOR_GLUDIN_HARBOR);
		arrivedAtGludin = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_HAS_ARRIVED_AT_GLUDIN_HARBOR);
		arrivedAtGludin2 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_WILL_LEAVE_FOR_TALKING_ISLAND_HARBOR_AFTER_ANCHORING_FOR_TEN_MINUTES);
		leaveGludin5 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_WILL_LEAVE_FOR_TALKING_ISLAND_HARBOR_IN_FIVE_MINUTES);
		leaveGludin1 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_WILL_LEAVE_FOR_TALKING_ISLAND_HARBOR_IN_ONE_MINUTE);
		leaveGludin0 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_WILL_BE_LEAVING_SOON_FOR_TALKING_ISLAND_HARBOR);
		leavingGludin = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_IS_LEAVING_FOR_TALKING_ISLAND_HARBOR);
		busyTalking = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_FROM_GLUDIN_HARBOR_TO_TALKING_ISLAND_HAS_BEEN_DELAYED);
		busyGludin = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_FROM_TALKING_ISLAND_TO_GLUDIN_HARBOR_HAS_BEEN_DELAYED);
		
		arrivalGludin10 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_FROM_TALKING_ISLAND_WILL_ARRIVE_AT_GLUDIN_HARBOR_IN_APPROXIMATELY_10_MINUTES);
		arrivalGludin5 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_FROM_TALKING_ISLAND_WILL_BE_ARRIVING_AT_GLUDIN_HARBOR_IN_APPROXIMATELY_5_MINUTES);
		arrivalGludin1 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_FROM_TALKING_ISLAND_WILL_BE_ARRIVING_AT_GLUDIN_HARBOR_IN_APPROXIMATELY_1_MINUTE);
		arrivalTalking10 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_FROM_GLUDIN_HARBOR_WILL_BE_ARRIVING_AT_TALKING_ISLAND_IN_APPROXIMATELY_10_MINUTES);
		arrivalTalking5 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_FROM_GLUDIN_HARBOR_WILL_BE_ARRIVING_AT_TALKING_ISLAND_IN_APPROXIMATELY_5_MINUTES);
		arrivalTalking1 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_FROM_GLUDIN_HARBOR_WILL_BE_ARRIVING_AT_TALKING_ISLAND_IN_APPROXIMATELY_1_MINUTE);
		
		talkingSound = new PlaySound(0, "itemsound.ship_arrival_departure", 1, _boat.getObjectId(), TALKING_DOCK[0].getX(), TALKING_DOCK[0].getY(), TALKING_DOCK[0].getZ());
		gludinSound = new PlaySound(0, "itemsound.ship_arrival_departure", 1, _boat.getObjectId(), GLUDIN_DOCK[0].getX(), GLUDIN_DOCK[0].getY(), GLUDIN_DOCK[0].getZ());
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
					BoatManager.getInstance().broadcastPacket(TALKING_DOCK[0], GLUDIN_DOCK[0], leaveTalking5);
					ThreadPool.schedule(this, 240000);
					break;
				}
				case 1:
				{
					BoatManager.getInstance().broadcastPackets(TALKING_DOCK[0], GLUDIN_DOCK[0], leaveTalking1, leaveTalking12);
					ThreadPool.schedule(this, 40000);
					break;
				}
				case 2:
				{
					BoatManager.getInstance().broadcastPacket(TALKING_DOCK[0], GLUDIN_DOCK[0], leaveTalking0);
					ThreadPool.schedule(this, 20000);
					break;
				}
				case 3:
				{
					BoatManager.getInstance().dockShip(BoatManager.TALKING_ISLAND, false);
					BoatManager.getInstance().broadcastPackets(TALKING_DOCK[0], GLUDIN_DOCK[0], leavingTalking);
					_boat.broadcastPacket(talkingSound);
					_boat.payForRide(1074, 1, -96777, 258970, -3623);
					_boat.executePath(TALKING_TO_GLUDIN);
					ThreadPool.schedule(this, 300000);
					break;
				}
				case 4:
				{
					BoatManager.getInstance().broadcastPacket(GLUDIN_DOCK[0], TALKING_DOCK[0], arrivalGludin10);
					ThreadPool.schedule(this, 300000);
					break;
				}
				case 5:
				{
					BoatManager.getInstance().broadcastPacket(GLUDIN_DOCK[0], TALKING_DOCK[0], arrivalGludin5);
					ThreadPool.schedule(this, 240000);
					break;
				}
				case 6:
				{
					BoatManager.getInstance().broadcastPacket(GLUDIN_DOCK[0], TALKING_DOCK[0], arrivalGludin1);
					break;
				}
				case 7:
				{
					if (BoatManager.getInstance().dockBusy(BoatManager.GLUDIN_HARBOR))
					{
						if (_shoutCount == 0)
						{
							BoatManager.getInstance().broadcastPacket(GLUDIN_DOCK[0], TALKING_DOCK[0], busyGludin);
						}
						_shoutCount++;
						if (_shoutCount > 35)
						{
							_shoutCount = 0;
						}
						ThreadPool.schedule(this, 5000);
						return;
					}
					_boat.executePath(GLUDIN_DOCK);
					break;
				}
				case 8:
				{
					BoatManager.getInstance().dockShip(BoatManager.GLUDIN_HARBOR, true);
					BoatManager.getInstance().broadcastPackets(GLUDIN_DOCK[0], TALKING_DOCK[0], arrivedAtGludin, arrivedAtGludin2);
					_boat.broadcastPacket(gludinSound);
					ThreadPool.schedule(this, 300000);
					break;
				}
				case 9:
				{
					BoatManager.getInstance().broadcastPacket(TALKING_DOCK[0], GLUDIN_DOCK[0], leaveGludin5);
					ThreadPool.schedule(this, 240000);
					break;
				}
				case 10:
				{
					BoatManager.getInstance().broadcastPackets(TALKING_DOCK[0], GLUDIN_DOCK[0], leaveGludin1, leaveTalking12);
					ThreadPool.schedule(this, 40000);
					break;
				}
				case 11:
				{
					BoatManager.getInstance().broadcastPacket(TALKING_DOCK[0], GLUDIN_DOCK[0], leaveGludin0);
					ThreadPool.schedule(this, 20000);
					break;
				}
				case 12:
				{
					BoatManager.getInstance().dockShip(BoatManager.GLUDIN_HARBOR, false);
					BoatManager.getInstance().broadcastPackets(TALKING_DOCK[0], GLUDIN_DOCK[0], leavingGludin);
					_boat.broadcastPacket(gludinSound);
					_boat.payForRide(1075, 1, -90015, 150422, -3610);
					_boat.executePath(GLUDIN_TO_TALKING);
					ThreadPool.schedule(this, 150000);
					break;
				}
				case 13:
				{
					BoatManager.getInstance().broadcastPacket(TALKING_DOCK[0], GLUDIN_DOCK[0], arrivalTalking10);
					ThreadPool.schedule(this, 300000);
					break;
				}
				case 14:
				{
					BoatManager.getInstance().broadcastPacket(TALKING_DOCK[0], GLUDIN_DOCK[0], arrivalTalking5);
					ThreadPool.schedule(this, 240000);
					break;
				}
				case 15:
				{
					BoatManager.getInstance().broadcastPacket(TALKING_DOCK[0], GLUDIN_DOCK[0], arrivalTalking1);
					break;
				}
				case 16:
				{
					if (BoatManager.getInstance().dockBusy(BoatManager.TALKING_ISLAND))
					{
						if (_shoutCount == 0)
						{
							BoatManager.getInstance().broadcastPacket(TALKING_DOCK[0], GLUDIN_DOCK[0], busyTalking);
						}
						_shoutCount++;
						if (_shoutCount > 35)
						{
							_shoutCount = 0;
						}
						ThreadPool.schedule(this, 5000);
						return;
					}
					_boat.executePath(TALKING_DOCK);
					break;
				}
				case 17:
				{
					BoatManager.getInstance().dockShip(BoatManager.TALKING_ISLAND, true);
					BoatManager.getInstance().broadcastPackets(TALKING_DOCK[0], GLUDIN_DOCK[0], arrivedAtTalking, arrivedAtTalking2);
					_boat.broadcastPacket(talkingSound);
					ThreadPool.schedule(this, 300000);
					break;
				}
				default:
				{
					break;
				}
			}
			_shoutCount = 0;
			_cycle++;
			if (_cycle > 17)
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
		final Boat boat = BoatManager.getInstance().getNewBoat(1, -96622, 261660, -3610, 32768);
		if (boat != null)
		{
			boat.registerEngine(new BoatTalkingGludin(boat));
			boat.runEngine(180000);
			BoatManager.getInstance().dockShip(BoatManager.TALKING_ISLAND, true);
		}
	}
}
