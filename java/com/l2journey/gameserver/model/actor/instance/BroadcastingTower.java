/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2journey.gameserver.model.actor.instance;

import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.creature.InstanceType;
import com.l2journey.gameserver.model.actor.templates.NpcTemplate;
import com.l2journey.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author NightMarez
 * @version $Revision: 1.3.2.2.2.5 $ $Date: 2005/03/27 15:29:32 $
 */
public class BroadcastingTower extends Npc
{
	private static final String HTML_PATH = "data/html/observation/";

	/**
	 * Creates an observation.
	 * @param template the observation NPC template
	 */
	public BroadcastingTower(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.BroadcastingTower);
	}
	
	@Override
	public void showChatWindow(Player player, int value)
	{
		String filename = null;
		if (isInsideRadius2D(-79884, 86529, 0, 50) || isInsideRadius2D(-78858, 111358, 0, 50) || isInsideRadius2D(-76973, 87136, 0, 50) || isInsideRadius2D(-75850, 111968, 0, 50))
		{
			if (value == 0)
			{
				filename = HTML_PATH + getId() + "-Oracle.htm";
			}
			else
			{
				filename = HTML_PATH + getId() + "-Oracle-" + value + ".htm";
			}
		}
		else
		{
			if (value == 0)
			{
				filename = HTML_PATH + getId() + ".htm";
			}
			else
			{
				filename = HTML_PATH + getId() + "-" + value + ".htm";
			}
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}
}