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
package ai.areas.DragonValley;

import com.l2journey.gameserver.model.StatSet;
import com.l2journey.gameserver.model.actor.Attackable;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.network.NpcStringId;
import com.l2journey.gameserver.network.enums.ChatType;
import com.l2journey.gameserver.network.serverpackets.ValidateLocation;

import ai.AbstractNpcAI;

/**
 * Lair of Antharas AI.
 * @author St3eT, UnAfraid
 */
public class LairOfAntharas extends AbstractNpcAI
{
	// NPC
	private static final int KNORIKS = 22857;
	private static final int DRAGON_KNIGHT = 22844;
	private static final int DRAGON_KNIGHT2 = 22845;
	private static final int ELITE_DRAGON_KNIGHT = 22846;
	
	private static final int DRAGON_GUARD = 22852;
	private static final int DRAGON_MAGE = 22853;
	// Misc
	private static final int KNIGHT_CHANCE = 30;
	private static final int KNORIKS_CHANCE = 60;
	private static final int KNORIKS_CHANCE2 = 50;
	
	private LairOfAntharas()
	{
		addKillId(DRAGON_KNIGHT, DRAGON_KNIGHT2, DRAGON_GUARD, DRAGON_MAGE);
		addSpawnId(DRAGON_KNIGHT, DRAGON_KNIGHT2, DRAGON_GUARD, DRAGON_MAGE);
		addMoveFinishedId(DRAGON_GUARD, DRAGON_MAGE);
		addAggroRangeEnterId(KNORIKS);
	}
	
	@Override
	public void onTimerEvent(String event, StatSet params, Npc npc, Player player)
	{
		if ((npc != null) && !npc.isDead() && npc.isSpawned())
		{
			if (!npc.isInCombat())
			{
				if (npc.calculateDistance2D(npc.getSpawn().getLocation()) > 10)
				{
					npc.asAttackable().returnHome();
				}
				else if (npc.getHeading() != npc.getSpawn().getHeading())
				{
					npc.setHeading(npc.getSpawn().getHeading());
					npc.broadcastPacket(new ValidateLocation(npc));
				}
			}
			getTimers().addTimer("CHECK_HOME_" + npc.getObjectId(), null, 10000, npc, null);
		}
	}
	
	@Override
	public void onAggroRangeEnter(Npc npc, Player player, boolean isSummon)
	{
		if (npc.isScriptValue(0) && (getRandom(100) < KNORIKS_CHANCE))
		{
			if (getRandom(100) < KNORIKS_CHANCE2)
			{
				npc.setScriptValue(1);
			}
			npc.broadcastSay(ChatType.NPC_SHOUT, NpcStringId.WHO_S_THERE_IF_YOU_DISTURB_THE_TEMPER_OF_THE_GREAT_LAND_DRAGON_ANTHARAS_I_WILL_NEVER_FORGIVE_YOU);
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		switch (npc.getId())
		{
			case DRAGON_KNIGHT:
			{
				if (getRandom(100) > KNIGHT_CHANCE)
				{
					final Npc newKnight = addSpawn(DRAGON_KNIGHT2, npc, false, 0, true);
					npc.deleteMe();
					newKnight.broadcastSay(ChatType.NPC_SHOUT, NpcStringId.THOSE_WHO_SET_FOOT_IN_THIS_PLACE_SHALL_NOT_LEAVE_ALIVE);
					addAttackDesire(newKnight, killer);
				}
				break;
			}
			case DRAGON_KNIGHT2:
			{
				if (getRandom(100) > KNIGHT_CHANCE)
				{
					final Npc eliteKnight = addSpawn(ELITE_DRAGON_KNIGHT, npc, false, 0, true);
					npc.deleteMe();
					eliteKnight.broadcastSay(ChatType.NPC_SHOUT, NpcStringId.IF_YOU_WISH_TO_SEE_HELL_I_WILL_GRANT_YOU_YOUR_WISH);
					addAttackDesire(eliteKnight, killer);
				}
				break;
			}
			case DRAGON_GUARD, DRAGON_MAGE:
			{
				cancelQuestTimer("CHECK_HOME", npc, null);
				break;
			}
		}
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		final Attackable mob = npc.asAttackable();
		mob.setOnKillDelay(0);
		if ((npc.getId() == DRAGON_GUARD) || (npc.getId() == DRAGON_MAGE))
		{
			mob.setRandomWalking(false);
			getTimers().addTimer("CHECK_HOME_" + npc.getObjectId(), null, 10000, npc, null);
		}
	}
	
	public static void main(String[] args)
	{
		new LairOfAntharas();
	}
}