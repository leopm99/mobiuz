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
package events.LoveYourGatekeeper;

import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.itemcontainer.Inventory;
import com.l2journey.gameserver.model.quest.LongTimeEvent;
import com.l2journey.gameserver.model.skill.holders.SkillHolder;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;

/**
 * Love Your Gatekeeper event.
 * @author Gladicek
 */
public class LoveYourGatekeeper extends LongTimeEvent
{
	// NPC
	private static final int GATEKEEPER = 32477;
	// Item
	private static final int GATEKEEPER_TRANSFORMATION_STICK = 12814;
	// Skills
	private static final SkillHolder TELEPORTER_TRANSFORM = new SkillHolder(5655, 1);
	// Misc
	private static final int HOURS = 24;
	private static final int PRICE = 10000;
	private static final String REUSE = LoveYourGatekeeper.class.getSimpleName() + "_reuse";
	
	private LoveYourGatekeeper()
	{
		addStartNpc(GATEKEEPER);
		addFirstTalkId(GATEKEEPER);
		addTalkId(GATEKEEPER);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "transform_stick":
			{
				if (player.getAdena() >= PRICE)
				{
					final long reuse = player.getVariables().getLong(REUSE, 0);
					if (reuse > System.currentTimeMillis())
					{
						final long remainingTime = (reuse - System.currentTimeMillis()) / 1000;
						final int hours = (int) (remainingTime / 3600);
						final int minutes = (int) ((remainingTime % 3600) / 60);
						final SystemMessage sm = new SystemMessage(SystemMessageId.S1_WILL_BE_AVAILABLE_FOR_RE_USE_AFTER_S2_HOUR_S_S3_MINUTE_S);
						sm.addItemName(GATEKEEPER_TRANSFORMATION_STICK);
						sm.addInt(hours);
						sm.addInt(minutes);
						player.sendPacket(sm);
					}
					else
					{
						takeItems(player, Inventory.ADENA_ID, PRICE);
						giveItems(player, GATEKEEPER_TRANSFORMATION_STICK, 1);
						player.getVariables().set(REUSE, System.currentTimeMillis() + (HOURS * 3600000));
					}
				}
				else
				{
					return "32477-3.htm";
				}
				return null;
			}
			case "transform":
			{
				if (!player.isTransformed())
				{
					player.doCast(TELEPORTER_TRANSFORM.getSkill());
				}
				return null;
			}
			default:
			{
				break;
			}
		}
		return event;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "32477.htm";
	}
	
	public static void main(String[] args)
	{
		new LoveYourGatekeeper();
	}
}