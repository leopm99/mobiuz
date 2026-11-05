package com.l2journey.gameserver.model.phantom;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.Config;
import com.l2journey.PhantomConfig;
import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.data.sql.ClanTable;
import com.l2journey.gameserver.data.sql.CrestTable;
import com.l2journey.gameserver.data.xml.SkillData;
import com.l2journey.gameserver.model.Crest;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.clan.Clan;
import com.l2journey.gameserver.model.clan.enums.CrestType;
import com.l2journey.gameserver.model.skill.Skill;

public class PhantomClan
{
	private static final Logger LOGGER = Logger.getLogger(PhantomClan.class.getName());
	private static final int MAX_PHANTOM_CLANS = 5;
	private static final int DDS_MAGIC = 0x20534444; // "DDS "
	private static final int DXT1_FOURCC = 0x31545844; // "DXT1"
	private static final int MIN_CREST_BYTES = 128;
	private static final int MAX_CREST_BYTES = 256;

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

	private PhantomClan()
	{
	}

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
		if (bot.getLevel() < 65)
		{
			return;
		}

		boolean puedeCrear = bot.getLevel() >= 76;
		boolean crearNuevo = puedeCrear && (Rnd.get(100) < 30) && canCreatePhantomClan();

		if (crearNuevo)
		{
			String nombreElegido = NOMBRES_DE_CLAN[Rnd.get(NOMBRES_DE_CLAN.length)];

			if (ClanTable.getInstance().getClanByName(nombreElegido) == null)
			{
				Clan nuevoClan = ClanTable.getInstance().createClan(bot, nombreElegido);

				if (nuevoClan != null)
				{
					bot.setClan(nuevoClan);
					nuevoClan.changeLevel(5);
					darSkillsDeClan(nuevoClan);
					assignRandomCrest(nuevoClan);

					bot.setTitle("Leader");
					nuevoClan.broadcastClanStatus();
					bot.broadcastUserInfo();
					bot.getAI().setIntention(Intention.ACTIVE);
					return;
				}
			}
		}

		Clan clanDestino = findPhantomClanToJoin();

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

	/**
	 * @return {@code true} when fewer than {@link #MAX_PHANTOM_CLANS} phantom-named clans exist
	 */
	static boolean canCreatePhantomClan()
	{
		return canCreatePhantomClan(countExistingPhantomClans());
	}

	/**
	 * @param existingCount how many phantom-named clans already exist
	 * @return {@code true} when another phantom clan may be created
	 */
	static boolean canCreatePhantomClan(int existingCount)
	{
		return existingCount < MAX_PHANTOM_CLANS;
	}

	/**
	 * @return how many {@link #NOMBRES_DE_CLAN} names already exist on the server
	 */
	static int countExistingPhantomClans()
	{
		int count = 0;
		for (String name : NOMBRES_DE_CLAN)
		{
			if (isPhantomClan(ClanTable.getInstance().getClanByName(name)))
			{
				count++;
			}
		}
		return count;
	}

	/**
	 * A reserved name used by a real player is not a phantom clan.
	 * @param clan a clan that uses one of {@link #NOMBRES_DE_CLAN}, or {@code null}
	 * @return {@code true} when the clan leader is a phantom character
	 */
	static boolean isPhantomClan(Clan clan)
	{
		return (clan != null) && PhantomConfig.PHANTOM_IDS.contains(clan.getLeaderId());
	}

	/**
	 * @param clan a candidate clan
	 * @return {@code true} when a phantom may join this clan
	 */
	static boolean isJoinablePhantomClan(Clan clan)
	{
		return isPhantomClan(clan) && (clan.getMembersCount() < 15);
	}

	/**
	 * Picks a phantom-led clan that still has room. Player clans that reused a reserved name are skipped.
	 * @return the clan to join, or {@code null}
	 */
	static Clan findPhantomClanToJoin()
	{
		Clan clanDestino = null;
		int intentos = 0;
		while ((clanDestino == null) && (intentos < 10))
		{
			int index = Rnd.get(NOMBRES_DE_CLAN.length);
			if ((index < 0) || (index >= NOMBRES_DE_CLAN.length))
			{
				intentos++;
				continue;
			}
			String nombreAleatorio = NOMBRES_DE_CLAN[index];
			Clan candidate = ClanTable.getInstance().getClanByName(nombreAleatorio);
			if (isJoinablePhantomClan(candidate))
			{
				clanDestino = candidate;
			}
			intentos++;
		}
		return clanDestino;
	}

	static void assignRandomCrest(Clan clan)
	{
		if (clan == null)
		{
			return;
		}

		File crestDir = new File(Config.DATAPACK_ROOT, "data/PhantomCrests");
		if (!crestDir.isDirectory())
		{
			return;
		}

		File[] files = crestDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".dds"));
		if ((files == null) || (files.length == 0))
		{
			return;
		}

		List<File> valid = new ArrayList<>();
		for (File file : files)
		{
			try
			{
				byte[] data = Files.readAllBytes(file.toPath());
				if (isValidPledgeCrest(data))
				{
					valid.add(file);
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.FINE, "Skipping crest file " + file.getName(), e);
			}
		}

		if (valid.isEmpty())
		{
			return;
		}

		try
		{
			byte[] data = Files.readAllBytes(valid.get(Rnd.get(valid.size())).toPath());
			Crest crest = CrestTable.getInstance().createCrest(data, CrestType.PLEDGE);
			if (crest != null)
			{
				clan.changeClanCrest(crest.getId());
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Failed to assign phantom clan crest", e);
		}
	}

	/**
	 * Accepts a DDS buffer with DXT1 compression and pledge-sized length.
	 * @param data raw crest bytes
	 * @return {@code true} when the header and size look valid
	 */
	static boolean isValidPledgeCrest(byte[] data)
	{
		if ((data == null) || (data.length < MIN_CREST_BYTES) || (data.length > MAX_CREST_BYTES) || (data.length < 128))
		{
			return false;
		}

		int magic = readIntLE(data, 0);
		if (magic != DDS_MAGIC)
		{
			return false;
		}

		// DDS_PIXELFORMAT.dwFourCC at offset 84
		int fourCC = readIntLE(data, 84);
		return fourCC == DXT1_FOURCC;
	}

	private static int readIntLE(byte[] data, int offset)
	{
		return (data[offset] & 0xFF)
			| ((data[offset + 1] & 0xFF) << 8)
			| ((data[offset + 2] & 0xFF) << 16)
			| ((data[offset + 3] & 0xFF) << 24);
	}

	public static void clearRuntimeState()
	{
		// Clan state is read from ClanTable
	}

	private static void darSkillsDeClan(Clan clan)
	{
		int clanLevel = clan.getLevel();

		if (clanLevel < 5)
		{
			return;
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
