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
import java.util.Set;

import com.l2journey.commons.network.WritableBuffer;
import com.l2journey.gameserver.data.PetActionData;
import com.l2journey.gameserver.model.actor.Summon;
import com.l2journey.gameserver.network.GameClient;
import com.l2journey.gameserver.network.ServerPackets;

/**
 * @author KenM, KingHanker
 */
public class ExBasicActionList extends ServerPacket
{
	//@formatter:off
	public static final int[] ACTIONS_ON_TRANSFORM =
	{
		1, 2, 3, 4,
		5, 6, 7, 8,
		9, 11, 15, 16,
		17, 18, 19, 21,
		22, 23, 32, 36,
		39, 40, 41, 42,
		43, 44, 45, 46,
		47, 48, 50, 52,
		53, 54, 55, 56,
		57, 63, 64, 65,
		70, 1000, 1001, 1003,
		1004, 1005, 1006, 1007,
		1008, 1009, 1010, 1011,
		1012, 1013, 1014, 1015,
		1016, 1017, 1018, 1019,
		1020, 1021, 1022, 1023,
		1024, 1025, 1026, 1027,
		1028, 1029, 1030, 1031,
		1032, 1033, 1034, 1035,
		1036, 1037, 1038, 1039,
		1040, 1041, 1042, 1043,
		1044, 1045, 1046, 1047,
		1048, 1049, 1050, 1051,
		1052, 1053, 1054, 1055,
		1056, 1057, 1058, 1059,
		1060, 1061, 1062, 1063,
		1064, 1065, 1066, 1067,
		1068, 1069, 1070, 1071,
		1072, 1073, 1074, 1075,
		1076, 1077, 1078, 1079,
		1080, 1081, 1082, 1083,
		1084, 1089, 1090, 1091,
		1092, 1093, 1094, 1095,
		1096, 1097, 1098 
	};
	//@formatter:on
	
	public static final int[] DEFAULT_ACTION_LIST;
	static
	{
		final int count1 = 74; // 0 <-> (count1 - 1)
		final int count2 = 99; // 1000 <-> (1000 + count2 - 1)
		final int count3 = 16; // 5000 <-> (5000 + count3 - 1)
		DEFAULT_ACTION_LIST = new int[count1 + count2 + count3];
		int i;
		for (i = count1; i-- > 0;)
		{
			DEFAULT_ACTION_LIST[i] = i;
		}
		for (i = count2; i-- > 0;)
		{
			DEFAULT_ACTION_LIST[count1 + i] = 1000 + i;
		}
		for (i = count3; i-- > 0;)
		{
			DEFAULT_ACTION_LIST[count1 + count2 + i] = 5000 + i;
		}
	}
	
	// Action list without pet/servitor actions (basic actions and skill actions)
	// Basic pet: 15-19 (Attack, Stop, Unsummon, Move, Follow)
	// Basic servitor: 21-23 (Attack, Stop, Follow), 52-54 (Unsummon, Move, Move)
	// Skill actions: 1000-1099 and 5000-5015
	public static final int[] NO_PET_ACTION_LIST;
	static
	{
		NO_PET_ACTION_LIST = Arrays.stream(DEFAULT_ACTION_LIST).filter(id ->
		{
			// Filter out basic pet actions (15-19)
			// Filter out basic servitor actions (21-23)
			if (((id >= 15) && (id <= 19)) || ((id >= 21) && (id <= 23)))
			{
				return false;
			}
			// Filter out additional servitor actions (52-54)
			if ((id >= 52) && (id <= 54))
			{
				return false;
			}
			// Filter out pet skill actions (1000-1099)
			if ((id >= 1000) && (id < 1100))
			{
				return false;
			}
			// Filter out baby pet actions (5000-5015)
			return !((id >= 5000) && (id < 5016));
		}).toArray();
	}
	
	public static final ExBasicActionList STATIC_PACKET = new ExBasicActionList(DEFAULT_ACTION_LIST);
	public static final ExBasicActionList NO_PET_PACKET = new ExBasicActionList(NO_PET_ACTION_LIST);
	
	/**
	 * Creates a dynamic action list that excludes all pet/servitor actions. Used when pet is unsummoned - blocks basic pet actions and all skill actions.
	 * @return ExBasicActionList without any pet actions
	 */
	public static ExBasicActionList getPacketWithoutPlayerPetActions()
	{
		// Simply return the NO_PET_PACKET which excludes all pet/servitor actions
		return NO_PET_PACKET;
	}
	
	/**
	 * Creates a dynamic action list based on the summon's available skills. Player actions (0-73) are always included. Pet/Servitor actions are filtered based on summon's NPC ID and skills.
	 * @param summon the summon to check skills for
	 * @return ExBasicActionList with only available actions
	 */
	public static ExBasicActionList getPacketForSummon(Summon summon)
	{
		if (summon == null)
		{
			return NO_PET_PACKET;
		}
		
		// Get available pet actions based on summon's NPC ID and skills
		final Set<Integer> petActions = PetActionData.getAvailableActions(summon);
		
		// Build action list: player actions (0-73) + pet actions
		final int[] actionList = Arrays.stream(DEFAULT_ACTION_LIST).filter(id ->
		{
			// Always include player actions
			if (id < 74)
			{
				return true;
			}
			// Only include if summon has this specific action
			return petActions.contains(id);
		}).toArray();
		
		return new ExBasicActionList(actionList);
	}
	
	private final int[] _actionIds;
	
	public ExBasicActionList(int[] actionIds)
	{
		_actionIds = actionIds;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_BASIC_ACTION_LIST.writeId(this, buffer);
		buffer.writeInt(_actionIds.length);
		for (int actionId : _actionIds)
		{
			buffer.writeInt(actionId);
		}
	}
}
