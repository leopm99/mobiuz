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
package com.l2journey.gameserver.managers;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2journey.Config;
import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.data.SpawnTable;
import com.l2journey.gameserver.model.DimensionalRift;
import com.l2journey.gameserver.model.DimensionalRiftRoom;
import com.l2journey.gameserver.model.Spawn;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.groups.Party;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Dimensional Rift manager.
 * @author kombat
 */
public class DimensionalRiftManager
{
	private static final Logger LOGGER = Logger.getLogger(DimensionalRiftManager.class.getName());
	private final Map<Byte, Map<Byte, DimensionalRiftRoom>> _rooms = HashMap.newHashMap(7);
	private static final int DIMENSIONAL_FRAGMENT_ITEM_ID = 7079;
	
	public static DimensionalRiftManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	protected DimensionalRiftManager()
	{
		loadRooms();
		loadSpawns();
	}
	
	public DimensionalRiftRoom getRoom(byte type, byte room)
	{
		return _rooms.get(type) == null ? null : _rooms.get(type).get(room);
	}
	
	private void loadRooms()
	{
		try (Connection con = DatabaseFactory.getConnection();
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT type, room_id, xMin, xMax, yMin, yMax, zMin, zMax, xT, yT, zT, boss FROM dimensional_rift"))
		{
			while (rs.next())
			{
				// 0 waiting room, 1 recruit, 2 soldier, 3 officer, 4 captain , 5 commander, 6 hero
				final byte type = rs.getByte("type");
				final byte room_id = rs.getByte("room_id");
				
				// coords related
				final int xMin = rs.getInt("xMin");
				final int xMax = rs.getInt("xMax");
				final int yMin = rs.getInt("yMin");
				final int yMax = rs.getInt("yMax");
				final int z1 = rs.getInt("zMin");
				final int z2 = rs.getInt("zMax");
				final int xT = rs.getInt("xT");
				final int yT = rs.getInt("yT");
				final int zT = rs.getInt("zT");
				final boolean isBossRoom = rs.getByte("boss") > 0;
				if (!_rooms.containsKey(type))
				{
					_rooms.put(type, HashMap.newHashMap(9));
				}
				
				_rooms.get(type).put(room_id, new DimensionalRiftRoom(type, room_id, xMin, xMax, yMin, yMax, z1, z2, xT, yT, zT, isBossRoom));
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e, () -> "Can't load Dimension Rift zones. " + e.getMessage());
		}
		
		final int typeSize = _rooms.keySet().size();
		int roomSize = 0;
		for (Map<Byte, DimensionalRiftRoom> room : _rooms.values())
		{
			roomSize += room.keySet().size();
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + typeSize + " room types with " + roomSize + " rooms.");
	}
	
	public void loadSpawns()
	{
		int countGood = 0;
		int countBad = 0;
		try
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			
			final File file = new File(Config.DATAPACK_ROOT, "data/DimensionalRift.xml");
			if (!file.exists())
			{
				LOGGER.log(Level.WARNING, () -> getClass().getSimpleName() + ": Couldn't find data/" + file.getName());
				return;
			}
			
			final Document document = factory.newDocumentBuilder().parse(file);
			NamedNodeMap attrs;
			byte type;
			byte roomId;
			int mobId;
			int x;
			int y;
			int z;
			int delay;
			int count;
			for (Node rift = document.getFirstChild(); rift != null; rift = rift.getNextSibling())
			{
				if ("rift".equalsIgnoreCase(rift.getNodeName()))
				{
					for (Node area = rift.getFirstChild(); area != null; area = area.getNextSibling())
					{
						if ("area".equalsIgnoreCase(area.getNodeName()))
						{
							attrs = area.getAttributes();
							type = Byte.parseByte(attrs.getNamedItem("type").getNodeValue());
							for (Node room = area.getFirstChild(); room != null; room = room.getNextSibling())
							{
								if ("room".equalsIgnoreCase(room.getNodeName()))
								{
									attrs = room.getAttributes();
									roomId = Byte.parseByte(attrs.getNamedItem("id").getNodeValue());
									for (Node spawn = room.getFirstChild(); spawn != null; spawn = spawn.getNextSibling())
									{
										if ("spawn".equalsIgnoreCase(spawn.getNodeName()))
										{
											attrs = spawn.getAttributes();
											mobId = Integer.parseInt(attrs.getNamedItem("mobId").getNodeValue());
											delay = Integer.parseInt(attrs.getNamedItem("delay").getNodeValue());
											count = Integer.parseInt(attrs.getNamedItem("count").getNodeValue());
											if (!_rooms.containsKey(type))
											{
												LOGGER.warning("Type " + type + " not found!");
											}
											else if (!_rooms.get(type).containsKey(roomId))
											{
												LOGGER.warning("Room " + roomId + " in Type " + type + " not found!");
											}
											
											for (int i = 0; i < count; i++)
											{
												final DimensionalRiftRoom riftRoom = _rooms.get(type).get(roomId);
												x = riftRoom.getRandomX();
												y = riftRoom.getRandomY();
												z = riftRoom.getTeleportCoorinates().getZ();
												if (_rooms.containsKey(type) && _rooms.get(type).containsKey(roomId))
												{
													final Spawn spawnDat = new Spawn(mobId);
													spawnDat.setAmount(1);
													spawnDat.setXYZ(x, y, z);
													spawnDat.setHeading(-1);
													spawnDat.setRespawnDelay(delay);
													SpawnTable.getInstance().addSpawn(spawnDat);
													_rooms.get(type).get(roomId).getSpawns().add(spawnDat);
													countGood++;
												}
												else
												{
													countBad++;
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e, () -> "Error on loading dimensional rift spawns: " + e.getMessage());
		}
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + countGood + " dimensional rift spawns, " + countBad + " errors.");
	}
	
	public void reload()
	{
		for (Map<Byte, DimensionalRiftRoom> rooms : _rooms.values())
		{
			for (DimensionalRiftRoom room : rooms.values())
			{
				room.getSpawns().clear();
			}
			rooms.clear();
		}
		_rooms.clear();
		loadRooms();
		loadSpawns();
	}
	
	public boolean checkIfInRiftZone(int x, int y, int z, boolean ignorePeaceZone)
	{
		if (ignorePeaceZone)
		{
			return _rooms.get((byte) 0).get((byte) 1).checkIfInZone(x, y, z);
		}
		return _rooms.get((byte) 0).get((byte) 1).checkIfInZone(x, y, z) && !_rooms.get((byte) 0).get((byte) 0).checkIfInZone(x, y, z);
	}
	
	public boolean checkIfInPeaceZone(int x, int y, int z)
	{
		return _rooms.get((byte) 0).get((byte) 0).checkIfInZone(x, y, z);
	}
	
	public void teleportToWaitingRoom(Player player)
	{
		player.teleToLocation(getRoom((byte) 0, (byte) 0).getTeleportCoorinates());
	}
	
	public synchronized void start(Player player, byte type, Npc npc)
	{
		boolean canPass = true;
		if (!player.isInParty())
		{
			showHtmlFile(player, "data/html/seven_signs/rift/NoParty.htm", npc);
			return;
		}
		
		final Party party = player.getParty();
		if (party.getLeaderObjectId() != player.getObjectId())
		{
			showHtmlFile(player, "data/html/seven_signs/rift/NotPartyLeader.htm", npc);
			return;
		}
		
		if (party.isInDimensionalRift())
		{
			handleCheat(player, npc);
			return;
		}
		
		if (party.getMemberCount() < Config.RIFT_MIN_PARTY_SIZE)
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			html.setFile(player, "data/html/seven_signs/rift/SmallParty.htm");
			html.replace("%npc_name%", npc.getName());
			html.replace("%count%", Integer.toString(Config.RIFT_MIN_PARTY_SIZE));
			player.sendPacket(html);
			return;
		}
		
		// max parties inside is rooms count - 1
		if (!isAllowedEnter(type))
		{
			player.sendMessage("Rift is full. Try later.");
			return;
		}
		
		for (Player p : party.getMembers())
		{
			if (!checkIfInPeaceZone(p.getX(), p.getY(), p.getZ()))
			{
				canPass = false;
				break;
			}
		}
		
		if (!canPass)
		{
			showHtmlFile(player, "data/html/seven_signs/rift/NotInWaitingRoom.htm", npc);
			return;
		}
		
		Item i;
		final int count = getNeededItems(type);
		for (Player p : party.getMembers())
		{
			i = p.getInventory().getItemByItemId(DIMENSIONAL_FRAGMENT_ITEM_ID);
			if (i == null)
			{
				canPass = false;
				break;
			}
			
			if (i.getCount() > 0)
			{
				if (i.getCount() < getNeededItems(type))
				{
					canPass = false;
					break;
				}
			}
			else
			{
				canPass = false;
				break;
			}
		}
		
		if (!canPass)
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			html.setFile(player, "data/html/seven_signs/rift/NoFragments.htm");
			html.replace("%npc_name%", npc.getName());
			html.replace("%count%", Integer.toString(count));
			player.sendPacket(html);
			return;
		}
		
		for (Player p : party.getMembers())
		{
			i = p.getInventory().getItemByItemId(DIMENSIONAL_FRAGMENT_ITEM_ID);
			if (!p.destroyItem(ItemProcessType.FEE, i, count, null, false))
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
				html.setFile(player, "data/html/seven_signs/rift/NoFragments.htm");
				html.replace("%npc_name%", npc.getName());
				html.replace("%count%", Integer.toString(count));
				player.sendPacket(html);
				return;
			}
		}
		
		byte room;
		List<Byte> emptyRooms;
		do
		{
			emptyRooms = getFreeRooms(type);
			room = emptyRooms.get(Rnd.get(1, emptyRooms.size()) - 1);
		}
		// find empty room
		while (_rooms.get(type).get(room).isPartyInside());
		new DimensionalRift(party, type, room);
	}
	
	public void killRift(DimensionalRift d)
	{
		if (d.getTeleportTimerTask() != null)
		{
			d.getTeleportTimerTask().cancel();
		}
		d.setTeleportTimerTask(null);
		
		if (d.getTeleportTimer() != null)
		{
			d.getTeleportTimer().cancel();
		}
		d.setTeleportTimer(null);
		
		if (d.getSpawnTimerTask() != null)
		{
			d.getSpawnTimerTask().cancel();
		}
		d.setSpawnTimerTask(null);
		
		if (d.getSpawnTimer() != null)
		{
			d.getSpawnTimer().cancel();
		}
		d.setSpawnTimer(null);
	}
	
	private int getNeededItems(byte type)
	{
		switch (type)
		{
			case 1:
			{
				return Config.RIFT_ENTER_COST_RECRUIT;
			}
			case 2:
			{
				return Config.RIFT_ENTER_COST_SOLDIER;
			}
			case 3:
			{
				return Config.RIFT_ENTER_COST_OFFICER;
			}
			case 4:
			{
				return Config.RIFT_ENTER_COST_CAPTAIN;
			}
			case 5:
			{
				return Config.RIFT_ENTER_COST_COMMANDER;
			}
			case 6:
			{
				return Config.RIFT_ENTER_COST_HERO;
			}
			default:
			{
				throw new IndexOutOfBoundsException();
			}
		}
	}
	
	public void showHtmlFile(Player player, String file, Npc npc)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, file);
		html.replace("%npc_name%", npc.getName());
		player.sendPacket(html);
	}
	
	public void handleCheat(Player player, Npc npc)
	{
		showHtmlFile(player, "data/html/seven_signs/rift/Cheater.htm", npc);
		if (!player.isGM())
		{
			LOGGER.warning(player + " was cheating in dimension rift area!");
			PunishmentManager.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " tried to cheat in dimensional rift.", Config.DEFAULT_PUNISH);
		}
	}
	
	public boolean isAllowedEnter(byte type)
	{
		int count = 0;
		for (DimensionalRiftRoom room : _rooms.get(type).values())
		{
			if (room.isPartyInside())
			{
				count++;
			}
		}
		return (count < (_rooms.get(type).size() - 1));
	}
	
	public List<Byte> getFreeRooms(byte type)
	{
		final List<Byte> list = new ArrayList<>();
		for (DimensionalRiftRoom room : _rooms.get(type).values())
		{
			if (!room.isPartyInside())
			{
				list.add(room.getRoom());
			}
		}
		return list;
	}
	
	private static class SingletonHolder
	{
		protected static final DimensionalRiftManager INSTANCE = new DimensionalRiftManager();
	}
}
