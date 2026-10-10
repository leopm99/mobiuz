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
import com.l2journey.gameserver.model.siege.Fort;
import com.l2journey.gameserver.network.GameClient;
import com.l2journey.gameserver.network.ServerPackets;

/**
 * @author KenM
 */
public class ExShowFortressSiegeInfo extends ServerPacket
{
	private final int _fortId;
	private final int _size;
	private final int _csize;
	private final int _csize2;
	
	/**
	 * @param fort
	 */
	public ExShowFortressSiegeInfo(Fort fort)
	{
		_fortId = fort.getResidenceId();
		_size = fort.getFortSize();
		final List<FortSiegeSpawn> commanders = FortSiegeManager.getInstance().getCommanderSpawnList(_fortId);
		_csize = ((commanders == null) ? 0 : commanders.size());
		_csize2 = fort.getSiege().getCommanders().size();
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_SHOW_FORTRESS_SIEGE_INFO.writeId(this, buffer);
		buffer.writeInt(_fortId); // Fortress Id
		buffer.writeInt(_size); // Total Barracks Count
		if (_csize > 0)
		{
			switch (_csize)
			{
				case 3:
				{
					switch (_csize2)
					{
						case 0:
						{
							buffer.writeInt(3);
							break;
						}
						case 1:
						{
							buffer.writeInt(2);
							break;
						}
						case 2:
						{
							buffer.writeInt(1);
							break;
						}
						case 3:
						{
							buffer.writeInt(0);
							break;
						}
						default:
						{
							break;
						}
					}
					break;
				}
				case 4: // TODO: change 4 to 5 once control room supported
				{
					switch (_csize2)
					{
						// TODO: once control room supported, update writeInt(0x0x) to support 5th room
						case 0:
						{
							buffer.writeInt(5);
							break;
						}
						case 1:
						{
							buffer.writeInt(4);
							break;
						}
						case 2:
						{
							buffer.writeInt(3);
							break;
						}
						case 3:
						{
							buffer.writeInt(2);
							break;
						}
						case 4:
						{
							buffer.writeInt(1);
							break;
						}
						default:
						{
							break;
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
			for (int i = 0; i < _size; i++)
			{
				buffer.writeInt(0);
			}
		}
	}
}
