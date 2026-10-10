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
package ai.others.WyvernManager;

import java.util.HashMap;
import java.util.Map;

import com.l2journey.Config;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.sevensigns.SevenSigns;
import com.l2journey.gameserver.model.siege.Fort;
import com.l2journey.gameserver.model.siege.clanhalls.SiegableHall;
import com.l2journey.gameserver.util.ArrayUtil;

import ai.AbstractNpcAI;

/**
 * Wyvern Manager
 * @author xban1x
 */
public class WyvernManager extends AbstractNpcAI
{
	private enum ManagerType
	{
		CASTLE,
		CLAN_HALL,
		FORT,
	}
	
	// Misc
	private static final int CRYSTAL_B_GRADE = 1460;
	private static final int WYVERN = 12621;
	private static final int WYVERN_FEE = 25;
	private static final int STRIDER_LEVEL = 55;
	private static final int[] STRIDERS =
	{
		12526,
		12527,
		12528,
		16038,
		16039,
		16040,
		16068,
		13197
	};
	// NPCS
	private static final Map<Integer, ManagerType> MANAGERS = new HashMap<>();
	static
	{
		MANAGERS.put(35101, ManagerType.CASTLE);
		MANAGERS.put(35143, ManagerType.CASTLE);
		MANAGERS.put(35185, ManagerType.CASTLE);
		MANAGERS.put(35227, ManagerType.CASTLE);
		MANAGERS.put(35275, ManagerType.CASTLE);
		MANAGERS.put(35317, ManagerType.CASTLE);
		MANAGERS.put(35364, ManagerType.CASTLE);
		MANAGERS.put(35510, ManagerType.CASTLE);
		MANAGERS.put(35536, ManagerType.CASTLE);
		MANAGERS.put(35556, ManagerType.CASTLE);
		MANAGERS.put(35419, ManagerType.CLAN_HALL);
		MANAGERS.put(35638, ManagerType.CLAN_HALL);
		MANAGERS.put(36457, ManagerType.FORT);
		MANAGERS.put(36458, ManagerType.FORT);
		MANAGERS.put(36459, ManagerType.FORT);
		MANAGERS.put(36460, ManagerType.FORT);
		MANAGERS.put(36461, ManagerType.FORT);
		MANAGERS.put(36462, ManagerType.FORT);
		MANAGERS.put(36463, ManagerType.FORT);
		MANAGERS.put(36464, ManagerType.FORT);
		MANAGERS.put(36465, ManagerType.FORT);
		MANAGERS.put(36466, ManagerType.FORT);
		MANAGERS.put(36467, ManagerType.FORT);
		MANAGERS.put(36468, ManagerType.FORT);
		MANAGERS.put(36469, ManagerType.FORT);
		MANAGERS.put(36470, ManagerType.FORT);
		MANAGERS.put(36471, ManagerType.FORT);
		MANAGERS.put(36472, ManagerType.FORT);
		MANAGERS.put(36473, ManagerType.FORT);
		MANAGERS.put(36474, ManagerType.FORT);
		MANAGERS.put(36475, ManagerType.FORT);
		MANAGERS.put(36476, ManagerType.FORT);
		MANAGERS.put(36477, ManagerType.FORT);
	}
	
	private WyvernManager()
	{
		addStartNpc(MANAGERS.keySet());
		addTalkId(MANAGERS.keySet());
		addFirstTalkId(MANAGERS.keySet());
	}
	
	private String mountWyvern(Npc npc, Player player)
	{
		if (player.isMounted() && (player.getMountLevel() >= STRIDER_LEVEL) && ArrayUtil.contains(STRIDERS, player.getMountNpcId()))
		{
			if (isOwnerClan(npc, player) && (getQuestItemsCount(player, CRYSTAL_B_GRADE) >= WYVERN_FEE))
			{
				takeItems(player, CRYSTAL_B_GRADE, WYVERN_FEE);
				player.dismount();
				player.mount(WYVERN, 0, true);
				return "wyvernmanager-04.html";
			}
			return replacePart(player, "wyvernmanager-06.html");
		}
		return replacePart(player, "wyvernmanager-05.html");
	}
	
	private boolean isOwnerClan(Npc npc, Player player)
	{
		if (!player.isClanLeader())
		{
			return false;
		}
		final ManagerType type = MANAGERS.get(npc.getId());
		switch (type)
		{
			case CASTLE:
			{
				if (npc.getCastle() != null)
				{
					return player.getClanId() == npc.getCastle().getOwnerId();
				}
				return false;
			}
			case CLAN_HALL:
			{
				if (npc.getConquerableHall() != null)
				{
					return player.getClanId() == npc.getConquerableHall().getOwnerId();
				}
				return false;
			}
			case FORT:
			{
				final Fort fort = npc.getFort();
				if ((fort != null) && (fort.getOwnerClan() != null))
				{
					return player.getClanId() == npc.getFort().getOwnerClan().getId();
				}
				return false;
			}
			default:
			{
				return false;
			}
		}
	}
	
	private boolean isInSiege(Npc npc)
	{
		final ManagerType type = MANAGERS.get(npc.getId());
		switch (type)
		{
			case CASTLE:
			{
				return npc.getCastle().getZone().isActive();
			}
			case CLAN_HALL:
			{
				final SiegableHall hall = npc.getConquerableHall();
				return (hall != null) ? hall.isInSiege() : npc.getCastle().getSiege().isInProgress();
			}
			case FORT:
			{
				return npc.getFort().getZone().isActive();
			}
			default:
			{
				return false;
			}
		}
	}
	
	private String getResidenceName(Npc npc)
	{
		final ManagerType type = MANAGERS.get(npc.getId());
		switch (type)
		{
			case CASTLE:
			{
				return npc.getCastle().getName();
			}
			case CLAN_HALL:
			{
				return npc.getConquerableHall().getName();
			}
			case FORT:
			{
				return npc.getFort().getName();
			}
			default:
			{
				return null;
			}
		}
	}
	
	private String replaceAll(Npc npc, Player player)
	{
		return replacePart(player, "wyvernmanager-01.html").replace("%residence_name%", getResidenceName(npc));
	}
	
	private String replacePart(Player player, String htmlFile)
	{
		return getHtm(player, htmlFile).replace("%wyvern_fee%", String.valueOf(WYVERN_FEE)).replace("%strider_level%", String.valueOf(STRIDER_LEVEL));
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "Return":
			{
				if (!isOwnerClan(npc, player))
				{
					htmltext = "wyvernmanager-02.html";
				}
				else if (Config.ALLOW_WYVERN_ALWAYS)
				{
					htmltext = replaceAll(npc, player);
				}
				else
				{
					final ManagerType type = MANAGERS.get(npc.getId());
					if ((type == ManagerType.CASTLE) && SevenSigns.getInstance().isSealValidationPeriod() && (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DUSK))
					{
						htmltext = "wyvernmanager-dusk.html";
					}
					else
					{
						htmltext = replaceAll(npc, player);
					}
				}
				break;
			}
			case "Help":
			{
				final ManagerType type = MANAGERS.get(npc.getId());
				htmltext = replacePart(player, (type == ManagerType.CASTLE) ? "wyvernmanager-03.html" : "wyvernmanager-03b.html");
				break;
			}
			case "RideWyvern":
			{
				if (!Config.ALLOW_WYVERN_ALWAYS)
				{
					if (!Config.ALLOW_WYVERN_DURING_SIEGE && (isInSiege(npc) || player.isInSiege()))
					{
						player.sendMessage("You cannot summon wyvern while in siege.");
						return null;
					}
					final ManagerType type = MANAGERS.get(npc.getId());
					if ((type == ManagerType.CASTLE) && SevenSigns.getInstance().isSealValidationPeriod() && ((SevenSigns.getInstance()).getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DUSK))
					{
						htmltext = "wyvernmanager-dusk.html";
					}
					else
					{
						htmltext = mountWyvern(npc, player);
					}
				}
				else
				{
					htmltext = mountWyvern(npc, player);
				}
				break;
			}
			default:
			{
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		String htmltext = null;
		if (!isOwnerClan(npc, player))
		{
			htmltext = "wyvernmanager-02.html";
		}
		else
		{
			if (Config.ALLOW_WYVERN_ALWAYS)
			{
				htmltext = replaceAll(npc, player);
			}
			else
			{
				final ManagerType type = MANAGERS.get(npc.getId());
				if ((type == ManagerType.CASTLE) && SevenSigns.getInstance().isSealValidationPeriod() && (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DUSK))
				{
					htmltext = "wyvernmanager-dusk.html";
				}
				else
				{
					htmltext = replaceAll(npc, player);
				}
			}
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new WyvernManager();
	}
}
