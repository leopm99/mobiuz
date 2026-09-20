package com.l2journey.gameserver.model.phantom;

import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.data.sql.ClanTable;
import com.l2journey.gameserver.data.xml.SkillData;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.clan.Clan;
import com.l2journey.gameserver.model.skill.Skill;

public class PhantomClan
{
	
	private static final String[] NOMBRES_DE_CLAN =
	{
		"IronWolves",
		"ShadowBane",
		"DragonSlayers",
		"BloodPact",
		"MysticArts",
		"LosLeggMan",
		"ValiantHearts",
		"NightWatch",
		"Eclipse",
		"DarkLegion",
		"ChaosLegion",
		"ApexPredators",
		"SilverVanguard",
		"CrimsonDawn",
		"BlackBears",
		"StormRiders",
		"SilentAssassins",
		"RoyalKnights",
		"FallenAngels",
		"Infinity",
		"Oblivion",
		"ImmortalArmy",
		"TitanGrasp",
		"SoulReapers",
		"DivineOrder",
		"PhoenixAshes",
		"Asgardians",
		"Spartans",
		"Revenants",
		"GhostDivision",
		"Nemesis",
		"ViperSquad",
		"WinterWolves",
		"IronFist"
	};
	
	private static final String[] TITULOS_CLAN =
	{
		"Fighter",
		"Veteran",
		"Guardian",
		"Elite",
		"Vanguard",
		"Knight",
		"KeloPartio",
		"ElFiolo",
		"Sniper",
		"LosLegg",
		"Hero",
		"Champion",
		"Slayer",
		"Wanderer",
		"Scout",
		"Captain",
		"General",
		"Commander",
		"ElJefe",
		"LaBestia",
		"Matador",
		"Ninja",
		"ElTanque",
		"Picante",
		"Tryhard",
		"Camper",
		"Mercenary",
		"Tirador",
		"Defender",
		"Support",
		"Striker",
		"Cazador",
		"Inmortal",
		"Leyenda",
		"Recluta",
		"Maestro",
		"Rebelde",
		"Renegado",
		"Soldado",
		"Centurion",
		"Kamikaze",
		"Intocable",
		"Vengador",
		"Rompehuesos",
		"Inquebrantable",
		"Maniaco",
		"Sicario",
		"Vagabundo",
		"Guerrilla",
		"Nomada"
	};
	
	public static void gestionarClanParaBot(Player bot)
	{
		if ((bot == null) || (bot.getClan() != null) || bot.isAttackingNow() || bot.isCastingNow())
		{
			return;
		}
		
		if (Rnd.get(100) < 2)
		{
			intentarCrearOReclutar(bot);
		}
	}
	
	private static void intentarCrearOReclutar(Player bot)
	{
		
		// 1. NIVEL MINIMO PARA ENTRAR: Se for menor que lvl 40, nao faz nada.
		if (bot.getLevel() < 65)
		{
			return;
		}
		
		// 2. REGRA DE CRIACAO: So os nivel 76+ tem chance de criar cla.
		boolean puedeCrear = bot.getLevel() >= 76;
		boolean crearNuevo = puedeCrear && (Rnd.get(100) < 30);
		
		if (crearNuevo)
		{
			String nombreElegido = NOMBRES_DE_CLAN[Rnd.get(NOMBRES_DE_CLAN.length)];
			
			// Verificamos se o cla nao existe no servidor
			if (ClanTable.getInstance().getClanByName(nombreElegido) == null)
			{
				Clan nuevoClan = ClanTable.getInstance().createClan(bot, nombreElegido);
				
				if (nuevoClan != null)
				{
					bot.setClan(nuevoClan);
					nuevoClan.changeLevel(5);
					darSkillsDeClan(nuevoClan);
					
					bot.setTitle("Leader");
					nuevoClan.broadcastClanStatus();
					bot.broadcastUserInfo();
					bot.getAI().setIntention(Intention.ACTIVE);
					return; // Terminou com sucesso, sai do metodo.
				}
			}
			// Se o nome ja estava ocupado, nao fazemos "return". Deixamos passar para tentar entrar.
		}
		
		// CODIGO PARA ENTRAR EM UM CLA EXISTENTE
		Clan clanDestino = null;
		int intentos = 0;
		
		// Buscamos um cla aleatorio diretamente na base do servidor
		while ((clanDestino == null) && (intentos < 10))
		{
			String nombreAleatorio = NOMBRES_DE_CLAN[Rnd.get(NOMBRES_DE_CLAN.length)];
			Clan c = ClanTable.getInstance().getClanByName(nombreAleatorio);
			
			if ((c != null) && (c.getMembersCount() < 15))
			{
				clanDestino = c;
			}
			intentos++;
		}
		
		if (clanDestino != null)
		{
			clanDestino.addClanMember(bot);
			bot.setClan(clanDestino);
			bot.storeMe();
			
			String tituloAleatorio = TITULOS_CLAN[Rnd.get(TITULOS_CLAN.length)];
			bot.setTitle(tituloAleatorio);
			
			bot.broadcastUserInfo();
			bot.getAI().setIntention(Intention.ACTIVE);
		}
	}
	
	public static void clearRuntimeState()
	{
		// Nao precisamos mais limpar listas de memoria porque lemos direto do ClanTable
	}
	
	private static void darSkillsDeClan(Clan clan)
	{
		int clanLevel = clan.getLevel();
		
		if (clanLevel < 5)
		{
			return; // No Lineage 2, as skills de cla comecam no nivel 5
		}
		
		int skillLevel = 1;
		if (clanLevel >= 9)
		{
			skillLevel = 3;
		}
		else if (clanLevel >= 7)
		{
			skillLevel = 2;
		}
		
		for (int skillId = 370; skillId <= 390; skillId++)
		{
			Skill skill = SkillData.getInstance().getSkill(skillId, skillLevel);
			if (skill != null)
			{
				clan.addNewSkill(skill);
			}
		}
		
		Skill imperium = SkillData.getInstance().getSkill(391, 1);
		if (imperium != null)
		{
			clan.addNewSkill(imperium);
		}
	}
}