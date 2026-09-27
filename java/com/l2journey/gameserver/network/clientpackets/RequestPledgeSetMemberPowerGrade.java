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

import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.clan.Clan;
import com.l2journey.gameserver.model.clan.ClanAccess;
import com.l2journey.gameserver.model.clan.ClanMember;
import com.l2journey.gameserver.network.serverpackets.PledgeReceivePowerInfo;

/**
 * Format: (ch) Sd
 * @author -Wooden-
 */
public class RequestPledgeSetMemberPowerGrade extends ClientPacket
{
	private String _member;
	private int _powerGrade;
	
	@Override
	protected void readImpl()
	{
		_member = readString();
		_powerGrade = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final Clan clan = player.getClan();
		if ((clan == null) || !player.hasAccess(ClanAccess.MODIFY_RANKS))
		{
			return;
		}
		
		final ClanMember member = clan.getClanMember(_member);
		if ((member == null) || (member.getObjectId() == clan.getLeaderId()))
		{
			return;
		}
		
		if (member.getPledgeType() == Clan.SUBUNIT_ACADEMY)
		{
			// also checked from client side
			player.sendMessage("You cannot change academy member grade");
			return;
		}
		
		member.setPowerGrade(_powerGrade);
		
		// An online member keeps the privileges of the old rank until refreshed.
		final Player memberPlayer = member.getPlayer();
		if (memberPlayer != null)
		{
			memberPlayer.setClanPrivileges(clan.getRankPrivs(_powerGrade));
			memberPlayer.updateUserInfo();
		}
		
		clan.broadcastClanStatus();
		player.sendPacket(new PledgeReceivePowerInfo(member));
	}
}