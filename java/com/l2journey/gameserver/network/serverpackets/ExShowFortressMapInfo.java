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
package com.l2journey.gameserver.network.serverpackets;

import java.util.List;

import com.l2journey.commons.network.WritableBuffer;
import com.l2journey.gameserver.managers.FortSiegeManager;
import com.l2journey.gameserver.model.FortSiegeSpawn;
import com.l2journey.gameserver.model.Spawn;
import com.l2journey.gameserver.model.siege.Fort;
import com.l2journey.gameserver.model.siege.FortSiege;
import com.l2journey.gameserver.network.GameClient;
import com.l2journey.gameserver.network.ServerPackets;

/**
 * @author KenM
 */
public class ExShowFortressMapInfo extends ServerPacket
{
	private final Fort _fortress;
	private final FortSiege _siege;
	private final List<FortSiegeSpawn> _commanders;
	
	public ExShowFortressMapInfo(Fort fortress)
	{
		_fortress = fortress;
		_siege = fortress.getSiege();
		_commanders = FortSiegeManager.getInstance().getCommanderSpawnList(fortress.getResidenceId());
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_SHOW_FORTRESS_MAP_INFO.writeId(this, buffer);
		buffer.writeInt(_fortress.getResidenceId());
		buffer.writeInt(_siege.isInProgress()); // fortress siege status
		buffer.writeInt(_fortress.getFortSize()); // barracks count
		if ((_commanders != null) && !_commanders.isEmpty() && _siege.isInProgress())
		{
			switch (_commanders.size())
			{
				case 3:
				{
					for (FortSiegeSpawn spawn : _commanders)
					{
						if (isSpawned(spawn.getId()))
						{
							buffer.writeInt(0);
						}
						else
						{
							buffer.writeInt(1);
						}
					}
					break;
				}
				case 4: // TODO: change 4 to 5 once control room supported
				{
					int count = 0;
					for (FortSiegeSpawn spawn : _commanders)
					{
						count++;
						if (count == 4)
						{
							buffer.writeInt(1); // TODO: control room emulated
						}
						if (isSpawned(spawn.getId()))
						{
							buffer.writeInt(0);
						}
						else
						{
							buffer.writeInt(1);
						}
					}
					break;
				}
				default:
				{
					break;
				}
			}
		}
		else
		{
			for (int i = 0; i < _fortress.getFortSize(); i++)
			{
				buffer.writeInt(0);
			}
		}
	}
	
	private boolean isSpawned(int npcId)
	{
		boolean ret = false;
		for (Spawn spawn : _siege.getCommanders())
		{
			if (spawn.getId() == npcId)
			{
				ret = true;
				break;
			}
		}
		return ret;
	}
}
