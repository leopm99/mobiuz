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

import java.util.ArrayList;
import java.util.List;

import com.l2journey.commons.network.WritableBuffer;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.skill.BuffInfo;
import com.l2journey.gameserver.network.GameClient;
import com.l2journey.gameserver.network.ServerPackets;

public class PartySpelled extends ServerPacket
{
	private final List<BuffInfo> _effects = new ArrayList<>();
	private final Creature _creature;
	
	public PartySpelled(Creature creature)
	{
		_creature = creature;
	}
	
	public void addSkill(BuffInfo info)
	{
		_effects.add(info);
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.PARTY_SPELLED.writeId(this, buffer);
		final int creatureType;
		if (_creature.isServitor())
		{
			creatureType = 2;
		}
		else if (_creature.isPet())
		{
			creatureType = 1;
		}
		else
		{
			creatureType = 0;
		}
		buffer.writeInt(creatureType);
		buffer.writeInt(_creature.getObjectId());
		buffer.writeInt(_effects.size());
		for (BuffInfo info : _effects)
		{
			if ((info != null) && info.isInUse())
			{
				buffer.writeInt(info.getSkill().getDisplayId());
				buffer.writeShort(info.getSkill().getDisplayLevel());
				buffer.writeInt(info.getTime());
			}
		}
	}
}
