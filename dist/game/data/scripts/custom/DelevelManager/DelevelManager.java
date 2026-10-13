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
package custom.DelevelManager;

import com.l2journey.Config;
import com.l2journey.gameserver.data.xml.ExperienceData;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;

import ai.AbstractNpcAI;

/**
 * @author KingHanker
 */
public class DelevelManager extends AbstractNpcAI
{
	private static final String HTML_N_36604 = "36604.htm";

	private DelevelManager()
	{
		addStartNpc(Config.DELEVEL_MANAGER_NPCID);
		addTalkId(Config.DELEVEL_MANAGER_NPCID);
		addFirstTalkId(Config.DELEVEL_MANAGER_NPCID);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		
		// Verificar se o sistema esta habilitado
		if (!Config.DELEVEL_MANAGER_ENABLED)
		{
			return "36604-disabled.htm";
		}
		
		switch (event)
		{
			case "delevel":
			{
				// Verificar se esta em combate
				if (player.isInCombat())
				{
					return "36604-3.htm";
				}
				
				// Verificar nivel minimo
				if (player.getLevel() <= Config.DELEVEL_MANAGER_MINIMUM_DELEVEL)
				{
					return "36604-2.htm";
				}
				
				// Verifica level da Subclasse
				if (player.isSubClassActive() && (player.getLevel() == Config.BASE_SUBCLASS_LEVEL))
				{
					return "36604-4.htm";
				}
				
				// Verificar itens necessarios
				if (getQuestItemsCount(player, Config.DELEVEL_MANAGER_ITEMID) < Config.DELEVEL_MANAGER_ITEMCOUNT)
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.S2_UNIT_S_OF_THE_ITEM_S1_IS_ARE_REQUIRED);
					sm.addItemName(Config.DELEVEL_MANAGER_ITEMID);
					sm.addLong(Config.DELEVEL_MANAGER_ITEMCOUNT);
					player.sendPacket(sm);
					return "36604-1.htm";
				}
				
				// Mostrar informacoes antes de confirmar
				final int targetLevel = player.getLevel() - 1;
				final long currentExp = player.getExp();
				final long targetExp = ExperienceData.getInstance().getExpForLevel(targetLevel);
				
				String preview = getHtm(player, "36604-preview.htm");
				preview = preview.replace("%currentLevel%", String.valueOf(player.getLevel()));
				preview = preview.replace("%currentExp%", String.format("%,d", currentExp));
				preview = preview.replace("%targetLevel%", String.valueOf(targetLevel));
				preview = preview.replace("%targetExp%", String.format("%,d", targetExp));
				
				return preview;
			}
			case "confirm_delevel":
			{
				// Revalidar condicoes basicas antes de executar
				if (player.isInCombat())
				{
					return HTML_N_36604;
				}
				
				if (player.getLevel() <= Config.DELEVEL_MANAGER_MINIMUM_DELEVEL)
				{
					return "36604-2.htm";
				}
				
				if (player.isSubClassActive() && (player.getLevel() == Config.BASE_SUBCLASS_LEVEL))
				{
					return "36604-4.htm";
				}
				
				if (getQuestItemsCount(player, Config.DELEVEL_MANAGER_ITEMID) < Config.DELEVEL_MANAGER_ITEMCOUNT)
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.S2_UNIT_S_OF_THE_ITEM_S1_IS_ARE_REQUIRED);
					sm.addItemName(Config.DELEVEL_MANAGER_ITEMID);
					sm.addLong(Config.DELEVEL_MANAGER_ITEMCOUNT);
					player.sendPacket(sm);
					return "36604-1.htm";
				}
				
				// Remover itens
				takeItems(player, Config.DELEVEL_MANAGER_ITEMID, Config.DELEVEL_MANAGER_ITEMCOUNT);
				
				// Calcular e remover experiencia
				final long expToRemove = player.getExp() - ExperienceData.getInstance().getExpForLevel(player.getLevel() - 1);
				player.getStat().removeExpAndSp(expToRemove, 0);
				
				// Atualizar informacoes do jogador
				player.broadcastUserInfo();
				
				return HTML_N_36604;
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
		return HTML_N_36604;
	}
	
	public static void main(String[] args)
	{
		new DelevelManager();
	}
}
