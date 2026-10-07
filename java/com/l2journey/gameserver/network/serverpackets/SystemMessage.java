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

import java.util.Arrays;

import com.l2journey.Config;
import com.l2journey.commons.network.WritableBuffer;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.Summon;
import com.l2journey.gameserver.model.actor.templates.NpcTemplate;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.network.GameClient;
import com.l2journey.gameserver.network.PacketLogger;
import com.l2journey.gameserver.network.ServerPackets;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.SystemMessageId.SMLocalisation;

/**
 * @author Forsaiken
 */
public class SystemMessage extends ServerPacket
{
	private static final SMParam[] EMPTY_PARAM_ARRAY = new SMParam[0];
	
	public class SMParam
	{
		private final byte _type;
		private final Object _value;
		
		public SMParam(byte type, Object value)
		{
			_type = type;
			_value = value;
		}
		
		public byte getType()
		{
			return _type;
		}
		
		public Object getValue()
		{
			return _value;
		}
		
		public String getStringValue()
		{
			return (String) _value;
		}
		
		public int getIntValue()
		{
			return ((Integer) _value).intValue();
		}
		
		public long getLongValue()
		{
			return ((Long) _value).longValue();
		}
		
		public int[] getIntArrayValue()
		{
			return (int[]) _value;
		}
	}
	
	// TODO: UnAfraid: Check/Implement id's: 14,15.
	// 15 exists in goddess of destruction but also may works in h5 needs to be verified!
	// id 14 unknown
	public static final byte TYPE_SYSTEM_STRING = 13;
	public static final byte TYPE_PLAYER_NAME = 12;
	public static final byte TYPE_DOOR_NAME = 11;
	public static final byte TYPE_INSTANCE_NAME = 10;
	public static final byte TYPE_ELEMENT_NAME = 9;
	// id 8 - same as 3
	public static final byte TYPE_ZONE_NAME = 7;
	public static final byte TYPE_LONG_NUMBER = 6;
	public static final byte TYPE_CASTLE_NAME = 5;
	public static final byte TYPE_SKILL_NAME = 4;
	public static final byte TYPE_ITEM_NAME = 3;
	public static final byte TYPE_NPC_NAME = 2;
	public static final byte TYPE_INT_NUMBER = 1;
	public static final byte TYPE_TEXT = 0;
	
	private SMParam[] _params;
	private final SystemMessageId _smId;
	private int _paramIndex;
	
	public SystemMessage(int id)
	{
		_smId = SystemMessageId.getSystemMessageId(id);
		_params = _smId.getParamCount() > 0 ? new SMParam[_smId.getParamCount()] : EMPTY_PARAM_ARRAY;
	}
	
	public SystemMessage(SystemMessageId smId)
	{
		if (smId == null)
		{
			throw new NullPointerException("SystemMessageId cannot be null!");
		}
		_smId = smId;
		_params = smId.getParamCount() > 0 ? new SMParam[smId.getParamCount()] : EMPTY_PARAM_ARRAY;
	}
	
	public SystemMessage(String text)
	{
		if (text == null)
		{
			throw new NullPointerException();
		}
		_smId = SystemMessageId.getSystemMessageId(SystemMessageId.S1_2.getId());
		_params = new SMParam[1];
		addString(text);
	}
	
	public int getId()
	{
		return _smId.getId();
	}
	
	public SystemMessageId getSystemMessageId()
	{
		return _smId;
	}
	
	private void append(SMParam param)
	{
		if (_paramIndex >= _params.length)
		{
			_params = Arrays.copyOf(_params, _paramIndex + 1);
			_smId.setParamCount(_paramIndex + 1);
			PacketLogger.info("Wrong parameter count '" + (_paramIndex + 1) + "' for SystemMessageId: " + _smId);
		}
		_params[_paramIndex++] = param;
	}
	
	public SystemMessage addString(String text)
	{
		append(new SMParam(TYPE_TEXT, text));
		return this;
	}
	
	/**
	 * Appends a Castle name parameter type, the name will be read from CastleName-e.dat.<br>
	 * <ul>
	 * <li>1-9 Castle names</li>
	 * <li>21 Fortress of Resistance</li>
	 * <li>22-33 Clan Hall names</li>
	 * <li>34 Devastated Castle</li>
	 * <li>35 Bandit Stronghold</li>
	 * <li>36-61 Clan Hall names</li>
	 * <li>62 Rainbow Springs</li>
	 * <li>63 Wild Beast Reserve</li>
	 * <li>64 Fortress of the Dead</li>
	 * <li>81-89 Territory names</li>
	 * <li>90-100 null</li>
	 * <li>101-121 Fortress names</li>
	 * </ul>
	 * @param number the conquerable entity
	 * @return the system message with the proper parameter
	 */
	public SystemMessage addCastleId(int number)
	{
		append(new SMParam(TYPE_CASTLE_NAME, number));
		return this;
	}
	
	public SystemMessage addInt(int number)
	{
		append(new SMParam(TYPE_INT_NUMBER, number));
		return this;
	}
	
	public SystemMessage addLong(long number)
	{
		append(new SMParam(TYPE_LONG_NUMBER, number));
		return this;
	}
	
	public SystemMessage addPcName(Player pc)
	{
		append(new SMParam(TYPE_PLAYER_NAME, pc.getAppearance().getVisibleName()));
		return this;
	}
	
	/**
	 * ID from doorData.xml
	 * @param doorId
	 * @return
	 */
	public SystemMessage addDoorName(int doorId)
	{
		append(new SMParam(TYPE_DOOR_NAME, doorId));
		return this;
	}
	
	public SystemMessage addNpcName(Npc npc)
	{
		return addNpcName(npc.getTemplate());
	}
	
	public SystemMessage addNpcName(Summon npc)
	{
		return addNpcName(npc.getId());
	}
	
	public SystemMessage addNpcName(NpcTemplate template)
	{
		if (template.isUsingServerSideName())
		{
			return addString(template.getName());
		}
		return addNpcName(template.getId());
	}
	
	public SystemMessage addNpcName(int id)
	{
		append(new SMParam(TYPE_NPC_NAME, 1000000 + id));
		return this;
	}
	
	public SystemMessage addItemName(Item item)
	{
		return addItemName(item.getId());
	}
	
	public SystemMessage addItemName(ItemTemplate item)
	{
		return addItemName(item.getId());
	}
	
	public SystemMessage addItemName(int id)
	{
		final ItemTemplate item = ItemData.getInstance().getTemplate(id);
		if (item.getDisplayId() != id)
		{
			return addString(item.getName());
		}
		append(new SMParam(TYPE_ITEM_NAME, id));
		return this;
	}
	
	public SystemMessage addZoneName(int x, int y, int z)
	{
		append(new SMParam(TYPE_ZONE_NAME, new int[]
		{
			x,
			y,
			z
		}));
		return this;
	}
	
	public SystemMessage addSkillName(Skill skill)
	{
		if (skill.getId() != skill.getDisplayId())
		{
			return addString(skill.getName());
		}
		return addSkillName(skill.getId(), skill.getLevel());
	}
	
	public SystemMessage addSkillName(int id)
	{
		return addSkillName(id, 1);
	}
	
	public SystemMessage addSkillName(int id, int lvl)
	{
		append(new SMParam(TYPE_SKILL_NAME, new int[]
		{
			id,
			lvl
		}));
		return this;
	}
	
	/**
	 * Elemental name - 0(Fire) ...
	 * @param type
	 * @return
	 */
	public SystemMessage addElemental(int type)
	{
		append(new SMParam(TYPE_ELEMENT_NAME, type));
		return this;
	}
	
	/**
	 * ID from sysstring-e.dat
	 * @param type
	 * @return
	 */
	public SystemMessage addSystemString(int type)
	{
		append(new SMParam(TYPE_SYSTEM_STRING, type));
		return this;
	}
	
	/**
	 * Instance name from instantzonedata-e.dat
	 * @param type id of instance
	 * @return
	 */
	public SystemMessage addInstanceName(int type)
	{
		append(new SMParam(TYPE_INSTANCE_NAME, type));
		return this;
	}
	
	public SMParam[] getParams()
	{
		return _params;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.SYSTEM_MESSAGE.writeId(this, buffer);
		
		// Localisation related.
		if (Config.MULTILANG_ENABLE)
		{
			final Player player = client.getPlayer();
			if (player != null)
			{
				final String lang = player.getLang();
				if ((lang != null) && !lang.equals("en"))
				{
					final SMLocalisation sml = _smId.getLocalisation(lang);
					if (sml != null)
					{
						final Object[] params = new Object[_paramIndex];
						for (int i = 0; i < _paramIndex; i++)
						{
							params[i] = _params[i].getValue();
						}
						buffer.writeInt(SystemMessageId.S1_2.getId());
						buffer.writeInt(1);
						buffer.writeInt(TYPE_TEXT);
						buffer.writeString(sml.getLocalisation(params));
						return;
					}
				}
			}
		}
		
		buffer.writeInt(_smId.getId());
		buffer.writeInt(_params.length);
		for (SMParam param : _params)
		{
			if (param == null)
			{
				PacketLogger.warning("Found null parameter for SystemMessageId " + _smId);
				continue;
			}
			buffer.writeInt(param.getType());
			switch (param.getType())
			{
				case TYPE_TEXT, TYPE_PLAYER_NAME:
				{
					buffer.writeString(param.getStringValue());
					break;
				}
				case TYPE_LONG_NUMBER:
				{
					buffer.writeLong(param.getLongValue());
					break;
				}
				case TYPE_ITEM_NAME, TYPE_CASTLE_NAME, TYPE_INT_NUMBER, TYPE_NPC_NAME, TYPE_ELEMENT_NAME, TYPE_SYSTEM_STRING, TYPE_INSTANCE_NAME, TYPE_DOOR_NAME:
				{
					buffer.writeInt(param.getIntValue());
					break;
				}
				case TYPE_SKILL_NAME:
				{
					final int[] array = param.getIntArrayValue();
					buffer.writeInt(array[0]); // SkillId
					buffer.writeInt(array[1]); // SkillLevel
					break;
				}
				case TYPE_ZONE_NAME:
				{
					final int[] array = param.getIntArrayValue();
					buffer.writeInt(array[0]); // x
					buffer.writeInt(array[1]); // y
					buffer.writeInt(array[2]); // z
					break;
				}
			}
		}
	}
}