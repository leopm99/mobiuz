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
package com.l2journey.gameserver.network.clientpackets;

import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.groups.Party;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.ExAskJoinMPCC;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;

/**
 * Format: (ch) S<br>
 * D0 0D 00 5A 00 77 00 65 00 72 00 67 00 00 00
 * @author chris_00
 */
public class RequestExAskJoinMPCC extends ClientPacket
{
	private String _name;
	
	@Override
	protected void readImpl()
	{
		_name = readString();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final Player target = World.getInstance().getPlayer(_name);
		// invite yourself? ;)
		if ((target == null) || (player.isInParty() && target.isInParty() && player.getParty().hasSameLeader(target.getParty())))
		{
			return;
		}
		
		SystemMessage sm;
		// activeChar is in a Party?
		if (player.isInParty())
		{
			final Party activeParty = player.getParty();
			// activeChar is PartyLeader? && activeChars Party is already in a CommandChannel?
			if (activeParty.getLeader().equals(player))
			{
				// if activeChars Party is in CC, is activeChar CCLeader?
				if (activeParty.isInCommandChannel() && activeParty.getCommandChannel().getLeader().equals(player))
				{
					// in CC and the CCLeader
					// target in a party?
					if (target.isInParty())
					{
						// targets party already in a CChannel?
						if (target.getParty().isInCommandChannel())
						{
							sm = new SystemMessage(SystemMessageId.C1_S_PARTY_IS_ALREADY_A_MEMBER_OF_THE_COMMAND_CHANNEL);
							sm.addString(target.getName());
							player.sendPacket(sm);
						}
						else
						{
							// ready to open a new CC
							// send request to targets Party's PartyLeader
							askJoinMPCC(player, target);
						}
					}
					else
					{
						player.sendMessage(target.getName() + " doesn't have party and cannot be invited to Command Channel.");
					}
				}
				else if (activeParty.isInCommandChannel() && !activeParty.getCommandChannel().getLeader().equals(player))
				{
					// in CC, but not the CCLeader
					sm = new SystemMessage(SystemMessageId.YOU_DO_NOT_HAVE_AUTHORITY_TO_INVITE_SOMEONE_TO_THE_COMMAND_CHANNEL);
					player.sendPacket(sm);
				}
				else
				{
					// target in a party?
					if (target.isInParty())
					{
						// targets party already in a CChannel?
						if (target.getParty().isInCommandChannel())
						{
							sm = new SystemMessage(SystemMessageId.C1_S_PARTY_IS_ALREADY_A_MEMBER_OF_THE_COMMAND_CHANNEL);
							sm.addString(target.getName());
							player.sendPacket(sm);
						}
						else
						{
							// ready to open a new CC
							// send request to targets Party's PartyLeader
							askJoinMPCC(player, target);
						}
					}
					else
					{
						player.sendMessage(target.getName() + " doesn't have party and cannot be invited to Command Channel.");
					}
				}
			}
			else
			{
				player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_AUTHORITY_TO_INVITE_SOMEONE_TO_THE_COMMAND_CHANNEL);
			}
		}
	}
	
	private void askJoinMPCC(Player requestor, Player target)
	{
		boolean hasRight = false;
		if (requestor.isClanLeader() && (requestor.getClan().getLevel() >= 5))
		{
			// Clan leader of level5 Clan or higher.
			hasRight = true;
		}
		else if (requestor.getInventory().getItemByItemId(8871) != null)
		{
			// 8871 Strategy Guide.
			// TODO: Should destroyed after successful invite?
			hasRight = true;
		}
		else if ((requestor.getPledgeClass() >= 5) && (requestor.getKnownSkill(391) != null))
		{
			// At least Baron or higher and the skill Clan Imperium
			hasRight = true;
		}
		
		if (!hasRight)
		{
			requestor.sendPacket(SystemMessageId.COMMAND_CHANNELS_CAN_ONLY_BE_FORMED_BY_A_PARTY_LEADER_WHO_IS_ALSO_THE_LEADER_OF_A_LEVEL_5_CLAN);
			return;
		}
		
		// Get the target's party leader, and do whole actions on him.
		final Player targetLeader = target.getParty().getLeader();
		SystemMessage sm;
		if (!targetLeader.isProcessingRequest())
		{
			requestor.onTransactionRequest(targetLeader);
			sm = new SystemMessage(SystemMessageId.C1_IS_INVITING_YOU_TO_A_COMMAND_CHANNEL_DO_YOU_ACCEPT);
			sm.addString(requestor.getName());
			targetLeader.sendPacket(sm);
			targetLeader.sendPacket(new ExAskJoinMPCC(requestor.getName()));
			requestor.sendMessage("You invited " + targetLeader.getName() + " to your Command Channel.");
		}
		else
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_ON_ANOTHER_TASK_PLEASE_TRY_AGAIN_LATER);
			sm.addString(targetLeader.getName());
			requestor.sendPacket(sm);
		}
	}
}
