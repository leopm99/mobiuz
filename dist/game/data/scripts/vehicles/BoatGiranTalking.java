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
public class BoatGiranTalking implements Runnable
{
	private static final Logger LOGGER = Logger.getLogger(BoatGiranTalking.class.getName());
	
	// Time: 868s
	private static final VehiclePathPoint[] GIRAN_TO_TALKING =
	{
		new VehiclePathPoint(51914, 189023, -3610, 150, 800),
		new VehiclePathPoint(60567, 189789, -3610, 150, 800),
		new VehiclePathPoint(63732, 197457, -3610, 200, 800),
		new VehiclePathPoint(63732, 219946, -3610, 250, 800),
		new VehiclePathPoint(62008, 222240, -3610, 250, 1200),
		new VehiclePathPoint(56115, 226791, -3610, 250, 1200),
		new VehiclePathPoint(40384, 226432, -3610, 300, 800),
		new VehiclePathPoint(37760, 226432, -3610, 300, 800),
		new VehiclePathPoint(27153, 226791, -3610, 300, 800),
		new VehiclePathPoint(12672, 227535, -3610, 300, 800),
		new VehiclePathPoint(-1808, 228280, -3610, 300, 800),
		new VehiclePathPoint(-22165, 230542, -3610, 300, 800),
		new VehiclePathPoint(-42523, 235205, -3610, 300, 800),
		new VehiclePathPoint(-68451, 259560, -3610, 250, 800),
		new VehiclePathPoint(-70848, 261696, -3610, 200, 800),
		new VehiclePathPoint(-83344, 261610, -3610, 200, 800),
		new VehiclePathPoint(-88344, 261660, -3610, 180, 800),
		new VehiclePathPoint(-92344, 261660, -3610, 180, 800),
		new VehiclePathPoint(-94242, 261659, -3610, 150, 800)
	};
	
	private static final VehiclePathPoint[] TALKING_DOCK =
	{
		new VehiclePathPoint(-96622, 261660, -3610, 150, 800)
	};
	
	// Time: 1398s
	private static final VehiclePathPoint[] TALKING_TO_GIRAN =
	{
		new VehiclePathPoint(-113925, 261660, -3610, 150, 800),
		new VehiclePathPoint(-126107, 249116, -3610, 180, 800),
		new VehiclePathPoint(-126107, 234499, -3610, 180, 800),
		new VehiclePathPoint(-126107, 219882, -3610, 180, 800),
		new VehiclePathPoint(-109414, 204914, -3610, 180, 800),
		new VehiclePathPoint(-92807, 204914, -3610, 180, 800),
		new VehiclePathPoint(-80425, 216450, -3610, 250, 800),
		new VehiclePathPoint(-68043, 227987, -3610, 250, 800),
		new VehiclePathPoint(-63744, 231168, -3610, 250, 800),
		new VehiclePathPoint(-60844, 231369, -3610, 250, 1800),
		new VehiclePathPoint(-44915, 231369, -3610, 200, 800),
		new VehiclePathPoint(-28986, 231369, -3610, 200, 800),
		new VehiclePathPoint(8233, 207624, -3610, 200, 800),
		new VehiclePathPoint(21470, 201503, -3610, 180, 800),
		new VehiclePathPoint(40058, 195383, -3610, 180, 800),
		new VehiclePathPoint(43022, 193793, -3610, 150, 800),
		new VehiclePathPoint(45986, 192203, -3610, 150, 800),
		new VehiclePathPoint(48950, 190613, -3610, 150, 800)
	};
	
	private static final VehiclePathPoint GIRAN_DOCK = TALKING_TO_GIRAN[TALKING_TO_GIRAN.length - 1];
	
	private final Boat _boat;
	private int _cycle = 0;
	private int _shoutCount = 0;
	
	private final CreatureSay arrivedAtGiran;
	private final CreatureSay arrivedAtGiran2;
	private final CreatureSay leaveGiran5;
	private final CreatureSay leaveGiran1;
	private final CreatureSay leaveGiran0;
	private final CreatureSay leavingGiran;
	private final CreatureSay arrivedAtTalking;
	private final CreatureSay arrivedAtTalking2;
	private final CreatureSay leaveTalking5;
	private final CreatureSay leaveTalking1;
	private final CreatureSay leaveTalking0;
	private final CreatureSay leavingTalking;
	private final CreatureSay busyTalking;
	
	private final CreatureSay arrivalTalking15;
	private final CreatureSay arrivalTalking10;
	private final CreatureSay arrivalTalking5;
	private final CreatureSay arrivalTalking1;
	private final CreatureSay arrivalGiran20;
	private final CreatureSay arrivalGiran15;
	private final CreatureSay arrivalGiran10;
	private final CreatureSay arrivalGiran5;
	private final CreatureSay arrivalGiran1;
	
	private final PlaySound giranSound;
	private final PlaySound talkingSound;
	
	public BoatGiranTalking(Boat boat)
	{
		_boat = boat;
		
		arrivedAtGiran = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_HAS_ARRIVED_AT_GIRAN_HARBOR);
		arrivedAtGiran2 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_WILL_LEAVE_FOR_TALKING_ISLAND_HARBOR_AFTER_ANCHORING_FOR_TEN_MINUTES);
		leaveGiran5 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_WILL_LEAVE_FOR_TALKING_ISLAND_HARBOR_IN_FIVE_MINUTES);
		leaveGiran1 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_WILL_LEAVE_FOR_TALKING_ISLAND_HARBOR_IN_ONE_MINUTE);
		leaveGiran0 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_WILL_BE_LEAVING_SOON_FOR_TALKING_ISLAND_HARBOR);
		leavingGiran = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_IS_LEAVING_FOR_TALKING_ISLAND_HARBOR);
		arrivedAtTalking = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_HAS_ARRIVED_AT_TALKING_ISLAND_HARBOR);
		arrivedAtTalking2 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_WILL_LEAVE_FOR_GIRAN_HARBOR_AFTER_ANCHORING_FOR_TEN_MINUTES);
		leaveTalking5 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_WILL_LEAVE_FOR_GIRAN_HARBOR_IN_FIVE_MINUTES);
		leaveTalking1 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_WILL_LEAVE_FOR_GIRAN_HARBOR_IN_ONE_MINUTE);
		leaveTalking0 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_WILL_BE_LEAVING_SOON_FOR_GIRAN_HARBOR);
		leavingTalking = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_IS_LEAVING_FOR_GIRAN_HARBOR);
		busyTalking = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_FROM_GIRAN_HARBOR_TO_TALKING_ISLAND_HAS_BEEN_DELAYED);
		
		arrivalTalking15 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_FROM_GIRAN_HARBOR_WILL_BE_ARRIVING_AT_TALKING_ISLAND_IN_APPROXIMATELY_15_MINUTES);
		arrivalTalking10 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_FROM_GIRAN_HARBOR_WILL_BE_ARRIVING_AT_TALKING_ISLAND_IN_APPROXIMATELY_10_MINUTES);
		arrivalTalking5 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_FROM_GIRAN_HARBOR_WILL_BE_ARRIVING_AT_TALKING_ISLAND_IN_APPROXIMATELY_5_MINUTES);
		arrivalTalking1 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_FROM_GIRAN_HARBOR_WILL_BE_ARRIVING_AT_TALKING_ISLAND_IN_APPROXIMATELY_1_MINUTE);
		arrivalGiran20 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_FROM_TALKING_ISLAND_WILL_BE_ARRIVING_AT_GIRAN_HARBOR_IN_APPROXIMATELY_20_MINUTES);
		arrivalGiran15 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_FROM_TALKING_ISLAND_WILL_BE_ARRIVING_AT_GIRAN_HARBOR_IN_APPROXIMATELY_15_MINUTES);
		arrivalGiran10 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_FROM_TALKING_ISLAND_WILL_BE_ARRIVING_AT_GIRAN_HARBOR_IN_APPROXIMATELY_10_MINUTES);
		arrivalGiran5 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_FROM_TALKING_ISLAND_WILL_BE_ARRIVING_AT_GIRAN_HARBOR_IN_APPROXIMATELY_5_MINUTES);
		arrivalGiran1 = new CreatureSay(ChatType.BOAT, 801, SystemMessageId.THE_FERRY_FROM_TALKING_ISLAND_WILL_BE_ARRIVING_AT_GIRAN_HARBOR_IN_APPROXIMATELY_1_MINUTE);
		
		giranSound = new PlaySound(0, "itemsound.ship_arrival_departure", 1, _boat.getObjectId(), GIRAN_DOCK.getX(), GIRAN_DOCK.getY(), GIRAN_DOCK.getZ());
		talkingSound = new PlaySound(0, "itemsound.ship_arrival_departure", 1, _boat.getObjectId(), TALKING_DOCK[0].getX(), TALKING_DOCK[0].getY(), TALKING_DOCK[0].getZ());
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
					BoatManager.getInstance().broadcastPacket(GIRAN_DOCK, TALKING_DOCK[0], leaveGiran5);
					ThreadPool.schedule(this, 240000);
					break;
				}
				case 1:
				{
					BoatManager.getInstance().broadcastPacket(GIRAN_DOCK, TALKING_DOCK[0], leaveGiran1);
					ThreadPool.schedule(this, 40000);
					break;
				}
				case 2:
				{
					BoatManager.getInstance().broadcastPacket(GIRAN_DOCK, TALKING_DOCK[0], leaveGiran0);
					ThreadPool.schedule(this, 20000);
					break;
				}
				case 3:
				{
					BoatManager.getInstance().broadcastPackets(GIRAN_DOCK, TALKING_DOCK[0], leavingGiran, arrivalTalking15);
					_boat.broadcastPacket(giranSound);
					_boat.payForRide(3946, 1, 46763, 187041, -3451);
					_boat.executePath(GIRAN_TO_TALKING);
					ThreadPool.schedule(this, 250000);
					break;
				}
				case 4:
				{
					BoatManager.getInstance().broadcastPacket(TALKING_DOCK[0], GIRAN_DOCK, arrivalTalking10);
					ThreadPool.schedule(this, 300000);
					break;
				}
				case 5:
				{
					BoatManager.getInstance().broadcastPacket(TALKING_DOCK[0], GIRAN_DOCK, arrivalTalking5);
					ThreadPool.schedule(this, 240000);
					break;
				}
				case 6:
				{
					BoatManager.getInstance().broadcastPacket(TALKING_DOCK[0], GIRAN_DOCK, arrivalTalking1);
					break;
				}
				case 7:
				{
					if (BoatManager.getInstance().dockBusy(BoatManager.TALKING_ISLAND))
					{
						if (_shoutCount == 0)
						{
							BoatManager.getInstance().broadcastPacket(TALKING_DOCK[0], GIRAN_DOCK, busyTalking);
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
				case 8:
				{
					BoatManager.getInstance().dockShip(BoatManager.TALKING_ISLAND, true);
					BoatManager.getInstance().broadcastPackets(TALKING_DOCK[0], GIRAN_DOCK, arrivedAtTalking, arrivedAtTalking2);
					_boat.broadcastPacket(talkingSound);
					ThreadPool.schedule(this, 300000);
					break;
				}
				case 9:
				{
					BoatManager.getInstance().broadcastPacket(TALKING_DOCK[0], GIRAN_DOCK, leaveTalking5);
					ThreadPool.schedule(this, 240000);
					break;
				}
				case 10:
				{
					BoatManager.getInstance().broadcastPacket(TALKING_DOCK[0], GIRAN_DOCK, leaveTalking1);
					ThreadPool.schedule(this, 40000);
					break;
				}
				case 11:
				{
					BoatManager.getInstance().broadcastPacket(TALKING_DOCK[0], GIRAN_DOCK, leaveTalking0);
					ThreadPool.schedule(this, 20000);
					break;
				}
				case 12:
				{
					BoatManager.getInstance().dockShip(BoatManager.TALKING_ISLAND, false);
					BoatManager.getInstance().broadcastPackets(TALKING_DOCK[0], GIRAN_DOCK, leavingTalking);
					_boat.broadcastPacket(talkingSound);
					_boat.payForRide(3945, 1, -96777, 258970, -3623);
					_boat.executePath(TALKING_TO_GIRAN);
					ThreadPool.schedule(this, 200000);
					break;
				}
				case 13:
				{
					BoatManager.getInstance().broadcastPacket(GIRAN_DOCK, TALKING_DOCK[0], arrivalGiran20);
					ThreadPool.schedule(this, 300000);
					break;
				}
				case 14:
				{
					BoatManager.getInstance().broadcastPacket(GIRAN_DOCK, TALKING_DOCK[0], arrivalGiran15);
					ThreadPool.schedule(this, 300000);
					break;
				}
				case 15:
				{
					BoatManager.getInstance().broadcastPacket(GIRAN_DOCK, TALKING_DOCK[0], arrivalGiran10);
					ThreadPool.schedule(this, 300000);
					break;
				}
				case 16:
				{
					BoatManager.getInstance().broadcastPacket(GIRAN_DOCK, TALKING_DOCK[0], arrivalGiran5);
					ThreadPool.schedule(this, 240000);
					break;
				}
				case 17:
				{
					BoatManager.getInstance().broadcastPacket(GIRAN_DOCK, TALKING_DOCK[0], arrivalGiran1);
					break;
				}
				case 18:
				{
					BoatManager.getInstance().broadcastPackets(GIRAN_DOCK, TALKING_DOCK[0], arrivedAtGiran, arrivedAtGiran2);
					_boat.broadcastPacket(giranSound);
					ThreadPool.schedule(this, 300000);
					break;
				}
			}
			_shoutCount = 0;
			_cycle++;
			if (_cycle > 18)
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
		final Boat boat = BoatManager.getInstance().getNewBoat(2, 48950, 190613, -3610, 60800);
		if (boat != null)
		{
			boat.registerEngine(new BoatGiranTalking(boat));
			boat.runEngine(180000);
		}
	}
}
