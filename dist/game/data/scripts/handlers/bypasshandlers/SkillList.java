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
package handlers.bypasshandlers;

import java.util.List;
import java.util.logging.Level;

import com.l2journey.Config;
import com.l2journey.gameserver.data.xml.SkillTreeData;
import com.l2journey.gameserver.handler.IBypassHandler;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.player.PlayerClass;
import com.l2journey.gameserver.model.actor.instance.Folk;
import com.l2journey.gameserver.network.serverpackets.ActionFailed;
import com.l2journey.gameserver.network.serverpackets.NpcHtmlMessage;

public class SkillList implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"SkillList"
	};
	
	@Override
	public boolean useBypass(String command, Player player, Creature target)
	{
		if ((target == null) || !target.isNpc())
		{
			return false;
		}
		
		if (Config.ALT_GAME_SKILL_LEARN)
		{
			try
			{
				final String id = command.substring(9).trim();
				if (!id.isEmpty())
				{
					Folk.showSkillList(player, target.asNpc(), PlayerClass.getPlayerClass(Integer.parseInt(id)));
				}
				else
				{
					boolean ownClass = false;
					
					final List<PlayerClass> classesToTeach = ((Folk) target).getClassesToTeach();
					for (PlayerClass cid : classesToTeach)
					{
						if (cid.equalsOrChildOf(player.getPlayerClass()))
						{
							ownClass = true;
							break;
						}
					}
					
					String text = "<html><body><center>Skill learning:</center><br>";
					if (!ownClass)
					{
						final String charType = player.getPlayerClass().isMage() ? "fighter" : "mage";
						text += "Skills of your class are the easiest to learn.<br>Skills of another class of your race are a little harder.<br>Skills for classes of another race are extremely difficult.<br>But the hardest of all to learn are the  " + charType + "skills!<br>";
					}
					
					// make a list of classes
					if (!classesToTeach.isEmpty())
					{
						int count = 0;
						PlayerClass classCheck = player.getPlayerClass();
						
						while ((count == 0) && (classCheck != null))
						{
							for (PlayerClass cid : classesToTeach)
							{
								if ((cid.level() > classCheck.level()) || SkillTreeData.getInstance().getAvailableSkills(player, cid, false, false).isEmpty())
								{
									continue;
								}
								
								text += "<a action=\"bypass -h npc_%objectId%_SkillList " + cid.getId() + "\">Learn " + cid + "'s class Skills</a><br>\n";
								count++;
							}
							classCheck = classCheck.getParent();
						}
					}
					else
					{
						text += "No Skills.<br>";
					}
					text += "</body></html>";
					
					final NpcHtmlMessage html = new NpcHtmlMessage(target.asNpc().getObjectId());
					html.setHtml(text);
					html.replace("%objectId%", String.valueOf(target.asNpc().getObjectId()));
					player.sendPacket(html);
					
					player.sendPacket(ActionFailed.STATIC_PACKET);
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Exception in " + getClass().getSimpleName(), e);
			}
		}
		else
		{
			Folk.showSkillList(player, target.asNpc(), player.getPlayerClass());
		}
		return true;
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}
