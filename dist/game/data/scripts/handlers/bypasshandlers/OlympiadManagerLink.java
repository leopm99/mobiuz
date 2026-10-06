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

import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import com.l2journey.EventsConfig;
import com.l2journey.commons.util.StringUtil;
import com.l2journey.gameserver.data.xml.MultisellData;
import com.l2journey.gameserver.handler.IBypassHandler;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.Summon;
import com.l2journey.gameserver.model.actor.instance.OlympiadManager;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.olympiad.CompetitionType;
import com.l2journey.gameserver.model.olympiad.Hero;
import com.l2journey.gameserver.model.olympiad.Olympiad;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.skill.holders.SkillHolder;
import com.l2journey.gameserver.network.serverpackets.ExHeroList;
import com.l2journey.gameserver.network.serverpackets.MagicSkillUse;
import com.l2journey.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author DS
 */
public class OlympiadManagerLink implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"olympiaddesc",
		"olympiadnoble",
		"olybuff",
		"olympiad"
	};
	
	private static final String FEWER_THAN = "Fewer than " + EventsConfig.OLYMPIAD_REG_DISPLAY;
	private static final String MORE_THAN = "More than " + EventsConfig.OLYMPIAD_REG_DISPLAY;
	private static final int GATE_PASS = EventsConfig.OLYMPIAD_COMP_RITEM;
	
	private static final SkillHolder[] ALLOWED_BUFFS =
	{
		new SkillHolder(4357, 2), // Haste Lv2
		new SkillHolder(4342, 2), // Wind Walk Lv2
		new SkillHolder(4356, 3), // Empower Lv3
		new SkillHolder(4355, 3), // Acumen Lv3
		new SkillHolder(4351, 6), // Concentration Lv6
		new SkillHolder(4345, 3), // Might Lv3
		new SkillHolder(4358, 3), // Guidance Lv3
		new SkillHolder(4359, 3), // Focus Lv3
		new SkillHolder(4360, 3), // Death Whisper Lv3
		new SkillHolder(4352, 2), // Berserker Spirit Lv2
	};
	
	@Override
	public boolean useBypass(String command, Player player, Creature target)
	{
		if (!(target instanceof OlympiadManager))
		{
			return false;
		}
		
		try
		{
			if (command.toLowerCase().startsWith("olympiaddesc"))
			{
				final int val = Integer.parseInt(command.substring(13, 14));
				final String suffix = command.substring(14);
				((OlympiadManager) target).showChatWindow(player, val, suffix);
			}
			else if (command.toLowerCase().startsWith("olympiadnoble"))
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(target.getObjectId());
				if (player.isCursedWeaponEquipped())
				{
					html.setFile(player, Olympiad.OLYMPIAD_HTML_PATH + "noble_cursed_weapon.htm");
					player.sendPacket(html);
					return false;
				}
				if (player.getClassIndex() != 0)
				{
					html.setFile(player, Olympiad.OLYMPIAD_HTML_PATH + "noble_sub.htm");
					html.replace("%objectId%", String.valueOf(target.getObjectId()));
					player.sendPacket(html);
					return false;
				}
				if (!player.isNoble() || (player.getPlayerClass().level() < 3))
				{
					html.setFile(player, Olympiad.OLYMPIAD_HTML_PATH + "noble_thirdclass.htm");
					html.replace("%objectId%", String.valueOf(target.getObjectId()));
					player.sendPacket(html);
					return false;
				}
				
				final int val = Integer.parseInt(command.substring(14));
				switch (val)
				{
					case 0: // H5 match selection
					{
						if (!com.l2journey.gameserver.model.olympiad.OlympiadManager.getInstance().isRegistered(player))
						{
							html.setFile(player, Olympiad.OLYMPIAD_HTML_PATH + "noble_desc2a.htm");
							html.replace("%objectId%", String.valueOf(target.getObjectId()));
							html.replace("%olympiad_period%", String.valueOf(Olympiad.getInstance().getPeriod()));
							html.replace("%olympiad_cycle%", String.valueOf(Olympiad.getInstance().getCurrentCycle()));
							html.replace("%olympiad_opponent%", String.valueOf(com.l2journey.gameserver.model.olympiad.OlympiadManager.getInstance().getCountOpponents()));
							player.sendPacket(html);
						}
						else
						{
							html.setFile(player, Olympiad.OLYMPIAD_HTML_PATH + "noble_unregister.htm");
							html.replace("%objectId%", String.valueOf(target.getObjectId()));
							player.sendPacket(html);
						}
						break;
					}
					case 1: // unregister
					{
						com.l2journey.gameserver.model.olympiad.OlympiadManager.getInstance().unRegisterNoble(player);
						break;
					}
					case 2: // show waiting list | TODO: cleanup (not used anymore)
					{
						final int nonClassed = com.l2journey.gameserver.model.olympiad.OlympiadManager.getInstance().getRegisteredNonClassBased().size();
						final int teams = com.l2journey.gameserver.model.olympiad.OlympiadManager.getInstance().getRegisteredTeamsBased().size();
						final Collection<List<Integer>> allClassed = com.l2journey.gameserver.model.olympiad.OlympiadManager.getInstance().getRegisteredClassBased().values();
						int classed = 0;
						if (!allClassed.isEmpty())
						{
							for (List<Integer> cls : allClassed)
							{
								if (cls != null)
								{
									classed += cls.size();
								}
							}
						}
						html.setFile(player, Olympiad.OLYMPIAD_HTML_PATH + "noble_registered.htm");
						if (EventsConfig.OLYMPIAD_REG_DISPLAY > 0)
						{
							html.replace("%listClassed%", classed < EventsConfig.OLYMPIAD_REG_DISPLAY ? FEWER_THAN : MORE_THAN);
							html.replace("%listNonClassedTeam%", teams < EventsConfig.OLYMPIAD_REG_DISPLAY ? FEWER_THAN : MORE_THAN);
							html.replace("%listNonClassed%", nonClassed < EventsConfig.OLYMPIAD_REG_DISPLAY ? FEWER_THAN : MORE_THAN);
						}
						else
						{
							html.replace("%listClassed%", String.valueOf(classed));
							html.replace("%listNonClassedTeam%", String.valueOf(teams));
							html.replace("%listNonClassed%", String.valueOf(nonClassed));
						}
						html.replace("%objectId%", String.valueOf(target.getObjectId()));
						player.sendPacket(html);
						break;
					}
					case 3: // There are %points% Grand Olympiad points granted for this event. | TODO: cleanup (not used anymore)
					{
						final int points = Olympiad.getInstance().getNoblePoints(player.getObjectId());
						html.setFile(player, Olympiad.OLYMPIAD_HTML_PATH + "noble_points1.htm");
						html.replace("%points%", String.valueOf(points));
						html.replace("%objectId%", String.valueOf(target.getObjectId()));
						player.sendPacket(html);
						break;
					}
					case 4: // register non classed
					{
						com.l2journey.gameserver.model.olympiad.OlympiadManager.getInstance().registerNoble(player, CompetitionType.NON_CLASSED);
						break;
					}
					case 5: // register classed
					{
						com.l2journey.gameserver.model.olympiad.OlympiadManager.getInstance().registerNoble(player, CompetitionType.CLASSED);
						break;
					}
					case 6: // request tokens reward
					{
						if (player.getVariables().getInt(Olympiad.UNCLAIMED_OLYMPIAD_PASSES_VAR, 0) > 0)
						{
							html.setFile(player, Olympiad.OLYMPIAD_HTML_PATH + "noble_settle.htm");
							html.replace("%objectId%", String.valueOf(target.getObjectId()));
							player.sendPacket(html);
						}
						else
						{
							html.setFile(player, Olympiad.OLYMPIAD_HTML_PATH + "noble_nopoints2.htm");
							html.replace("%objectId%", String.valueOf(target.getObjectId()));
							player.sendPacket(html);
						}
						break;
					}
					case 7: // Equipment Rewards
					{
						MultisellData.getInstance().separateAndSend(102, player, target.asNpc(), false);
						break;
					}
					case 8: // Misc. Rewards
					{
						MultisellData.getInstance().separateAndSend(103, player, target.asNpc(), false);
						break;
					}
					case 9: // Your Grand Olympiad Score from the previous period is %points% point(s) | TODO: cleanup (not used anymore)
					{
						final int point = Olympiad.getInstance().getLastNobleOlympiadPoints(player.getObjectId());
						html.setFile(player, Olympiad.OLYMPIAD_HTML_PATH + "noble_points2.htm");
						html.replace("%points%", String.valueOf(point));
						html.replace("%objectId%", String.valueOf(target.getObjectId()));
						player.sendPacket(html);
						break;
					}
					case 10: // give tokens to player
					{
						final int passes = player.getVariables().getInt(Olympiad.UNCLAIMED_OLYMPIAD_PASSES_VAR, 0);
						if (passes > 0)
						{
							player.getVariables().remove(Olympiad.UNCLAIMED_OLYMPIAD_PASSES_VAR);
							player.addItem(ItemProcessType.REWARD, GATE_PASS, passes * EventsConfig.OLYMPIAD_GP_PER_POINT, player, true);
						}
						break;
					}
					case 11: // register team
					{
						com.l2journey.gameserver.model.olympiad.OlympiadManager.getInstance().registerNoble(player, CompetitionType.TEAMS);
						break;
					}
					default:
					{
						LOGGER.warning("Olympiad System: Could not send packet for request " + val);
						break;
					}
				}
			}
			else if (command.toLowerCase().startsWith("olybuff"))
			{
				int buffCount = player.getOlympiadBuffCount();
				if (buffCount <= 0)
				{
					return false;
				}
				
				final NpcHtmlMessage html = new NpcHtmlMessage(target.getObjectId());
				final String[] params = command.split(" ");
				if (!StringUtil.isNumeric(params[1]))
				{
					LOGGER.warning("Olympiad Buffer Warning: npcId = " + target.getId() + " has invalid buffGroup set in the bypass for the buff selected: " + params[1]);
					return false;
				}
				
				final int index = Integer.parseInt(params[1]);
				if ((index < 0) || (index >= ALLOWED_BUFFS.length))
				{
					LOGGER.warning("Olympiad Buffer Warning: npcId = " + target.getId() + " has invalid index sent in the bypass: " + index);
					return false;
				}
				
				if (buffCount > 0)
				{
					final Skill skill = ALLOWED_BUFFS[index].getSkill();
					if (skill != null)
					{
						target.setTarget(player);
						
						player.setOlympiadBuffCount(--buffCount);
						
						target.broadcastPacket(new MagicSkillUse(target, player, skill.getId(), skill.getLevel(), 0, 0));
						skill.applyEffects(player, player);
						final Summon summon = player.getSummon();
						if (summon != null)
						{
							target.broadcastPacket(new MagicSkillUse(target, summon, skill.getId(), skill.getLevel(), 0, 0));
							skill.applyEffects(summon, summon);
						}
					}
				}
				
				if (buffCount > 0)
				{
					html.setFile(player, buffCount == EventsConfig.OLYMPIAD_MAX_BUFFS ? Olympiad.OLYMPIAD_HTML_PATH + "olympiad_buffs.htm" : Olympiad.OLYMPIAD_HTML_PATH + "olympiad_5buffs.htm");
					html.replace("%objectId%", String.valueOf(target.getObjectId()));
					player.sendPacket(html);
				}
				else
				{
					html.setFile(player, Olympiad.OLYMPIAD_HTML_PATH + "olympiad_nobuffs.htm");
					html.replace("%objectId%", String.valueOf(target.getObjectId()));
					player.sendPacket(html);
					target.decayMe();
				}
			}
			else if (command.toLowerCase().startsWith("olympiad"))
			{
				final int val = Integer.parseInt(command.substring(9, 10));
				final NpcHtmlMessage reply = new NpcHtmlMessage(target.getObjectId());
				
				switch (val)
				{
					case 2: // show rank for a specific class
					{
						// for example >> Olympiad 1_88
						final int classId = Integer.parseInt(command.substring(11));
						if (((classId >= 88) && (classId <= 118)) || ((classId >= 131) && (classId <= 134)) || (classId == 136))
						{
							final List<String> names = Olympiad.getInstance().getClassLeaderBoard(classId);
							reply.setFile(player, Olympiad.OLYMPIAD_HTML_PATH + "olympiad_ranking.htm");
							int index = 1;
							for (String name : names)
							{
								reply.replace("%place" + index + "%", String.valueOf(index));
								reply.replace("%rank" + index + "%", name);
								index++;
								if (index > 10)
								{
									break;
								}
							}
							for (; index <= 10; index++)
							{
								reply.replace("%place" + index + "%", "");
								reply.replace("%rank" + index + "%", "");
							}
							reply.replace("%objectId%", String.valueOf(target.getObjectId()));
							player.sendPacket(reply);
						}
						break;
					}
					case 4: // hero list
					{
						player.sendPacket(new ExHeroList());
						break;
					}
					case 5: // Hero Certification
					{
						if (Hero.getInstance().isUnclaimedHero(player.getObjectId()))
						{
							Hero.getInstance().claimHero(player);
							reply.setFile(player, Olympiad.OLYMPIAD_HTML_PATH + "hero_receive.htm");
						}
						else
						{
							reply.setFile(player, Olympiad.OLYMPIAD_HTML_PATH + "hero_notreceive.htm");
						}
						player.sendPacket(reply);
						break;
					}
					default:
					{
						LOGGER.warning("Olympiad System: Could not send packet for request " + val);
						break;
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e, () -> "Exception in " + getClass().getSimpleName());
		}
		
		return true;
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}
