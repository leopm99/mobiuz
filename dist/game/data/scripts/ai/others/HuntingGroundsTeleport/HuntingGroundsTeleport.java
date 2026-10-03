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
package ai.others.HuntingGroundsTeleport;

import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.sevensigns.SevenSigns;
import com.l2journey.gameserver.util.ArrayUtil;

import ai.AbstractNpcAI;

/**
 * Hunting Grounds teleport AI.
 * @author Charus
 */
public class HuntingGroundsTeleport extends AbstractNpcAI
{
	// NPCs
	// @formatter:off
	private static final int[] PRIESTS =
	{
		31078, 31079, 31080, 31081, 31082, 31083, 31084, 31085, 31086, 31087,
		31088, 31089, 31090, 31091, 31168, 31169, 31692, 31693, 31694, 31695,
		31997, 31998
	};
	
	private static final int[] DAWN_NPCS =
	{
		31078, 31079, 31080, 31081, 31082, 31083, 31084, 31168, 31692, 31694,
		31997
	};
	// @formatter:on
	
	private HuntingGroundsTeleport()
	{
		addStartNpc(PRIESTS);
		addTalkId(PRIESTS);
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final SevenSigns ss = SevenSigns.getInstance();
		final int playerCabal = ss.getPlayerCabal(player.getObjectId());
		if (playerCabal == SevenSigns.CABAL_NULL)
		{
			return ArrayUtil.contains(DAWN_NPCS, npc.getId()) ? "dawn_tele-no.htm" : "dusk_tele-no.htm";
		}
		
		String htmltext = "";
		final boolean check = ss.isSealValidationPeriod() && (playerCabal == ss.getSealOwner(SevenSigns.SEAL_GNOSIS)) && (ss.getPlayerSeal(player.getObjectId()) == SevenSigns.SEAL_GNOSIS);
		switch (npc.getId())
		{
			case 31078, 31085:
			{
				htmltext = check ? "low_gludin.htm" : "hg_gludin.htm";
				break;
			}
			case 31079, 31086:
			{
				htmltext = check ? "low_gludio.htm" : "hg_gludio.htm";
				break;
			}
			case 31080, 31087:
			{
				htmltext = check ? "low_dion.htm" : "hg_dion.htm";
				break;
			}
			case 31081, 31088:
			{
				htmltext = check ? "low_giran.htm" : "hg_giran.htm";
				break;
			}
			case 31082, 31089:
			{
				htmltext = check ? "low_heine.htm" : "hg_heine.htm";
				break;
			}
			case 31083, 31090:
			{
				htmltext = check ? "low_oren.htm" : "hg_oren.htm";
				break;
			}
			case 31084, 31091:
			{
				htmltext = check ? "low_aden.htm" : "hg_aden.htm";
				break;
			}
			case 31168, 31169:
			{
				htmltext = check ? "low_hw.htm" : "hg_hw.htm";
				break;
			}
			case 31692, 31693:
			{
				htmltext = check ? "low_goddard.htm" : "hg_goddard.htm";
				break;
			}
			case 31694, 31695:
			{
				htmltext = check ? "low_rune.htm" : "hg_rune.htm";
				break;
			}
			case 31997, 31998:
			{
				htmltext = check ? "low_schuttgart.htm" : "hg_schuttgart.htm";
				break;
			}
			default:
			{
				break;
			}
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new HuntingGroundsTeleport();
	}
}
