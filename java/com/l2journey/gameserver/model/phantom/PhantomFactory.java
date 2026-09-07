package com.l2journey.gameserver.model.phantom;

import java.util.ArrayList;
import java.util.List;

import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.data.sql.CharInfoTable;
import com.l2journey.gameserver.data.xml.PlayerTemplateData;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.appearance.PlayerAppearance;
import com.l2journey.gameserver.model.actor.enums.creature.Race;
import com.l2journey.gameserver.model.actor.enums.player.PlayerClass;
import com.l2journey.gameserver.model.actor.templates.PlayerTemplate;

public class PhantomFactory
{
	private static final String ACCOUNT_NAME = "phantom_ai";

	// Separamos os guerreiros dos magos
	private static final PlayerClass[] FIGHTER_CLASSES =
	{
		PlayerClass.FIGHTER,
		PlayerClass.ELVEN_FIGHTER,
		PlayerClass.DARK_FIGHTER,
		PlayerClass.ORC_FIGHTER,
		PlayerClass.DWARVEN_FIGHTER,
		PlayerClass.TROOPER, 
		PlayerClass.WARDER   
	};

	private static final PlayerClass[] MAGE_CLASSES =
	{
		PlayerClass.MAGE,
		PlayerClass.ELVEN_MAGE,
		PlayerClass.DARK_MAGE,
		PlayerClass.ORC_MAGE
	};

	// Contador para controlar a proporcao exata de magos e guerreiros
	private static int _spawnCounter = 0;

	private static final String[] PREFIXES = {
		"MaxiLegg", "LucasLegg", "BenjaLegg", "Dark", "Light", "Blood", "Shadow", "Fire", "Ice", "Storm",
		"Night", "Silver", "Golden", "Red", "Black", "Blue", "White", "Silent", "Crazy", "Wild",
		"Deadly", "Rapid", "Brave", "Fatal", "Cold", "Ancient", "Royal", "Broken", "Burning", "Frozen",
		"Ghost", "Divine", "Infernal", "Sacred", "Bloody", "MadMax", "Shrek", "Lost", "Elite"
	};

	private static final String[] FIRST_NAMES = {
		"Adrian", "Bruno", "Camila", "Daniel", "Elena", "Fabian", "Gabriel", "Hector", "Ivan", "Julian",
		"Laura", "Lucas", "Marcos", "Matias", "Nicolas", "Paula", "Rafael", "Sofia", "Tomas", "Valeria",
		"Leon", "Altair", "Drake", "Kael", "Ragnar", "Lucian", "Orion", "Darian", "Kain", "Auron",
		"Cain", "Ezio", "Lautaro", "Thiago", "Enzo", "Dante", "Axel", "Mauro", "Renzo", "Gael",
		"Franco", "Agustin", "Leandro", "Damian", "Ulises", "Joaquin", "Benjamin", "Valentino", "Santino", "Bautista",
		"Alan", "Elias", "Aaron", "Naomi", "Kiara", "Mia", "Luna", "Abril", "Bianca", "Mora",
		"Alma", "Celeste", "Brisa", "Nadia", "Melina", "Ariana", "Zoe", "Noelia", "Tamara", "Karen",
		"Milena"
	};

	private static final String[] SUFFIXES = {
		"Blade", "Knight", "Hunter", "Slayer", "Master", "Arrow", "Rider", "Fury", "Strike", "Heart",
		"Storm", "Moon", "Flame", "Shadow", "Wolf", "Dragon", "Tiger", "Cross", "Steel", "Frost",
		"Skull", "Raven", "Claw", "Stone", "Sniper", "Reaper", "Ghost", "Walker", "Lord", "Queen",
		"King", "Soul", "Wind", "Breaker", "Crusher", "Destroyer", "Legend", "Hero", "Phoenix", "Demon",
		"Angel"
	};

	public static List<Player> createPhantoms(int count)
	{
		List<Player> created = new ArrayList<>();

		for (int i = 0; i < count; i++)
		{
			Player phantom = createOne();

			if (phantom != null)
			{
				created.add(phantom);
			}
		}

		return created;
	}

	private static Player createOne()
	{
		try
		{
			PlayerClass playerClass = getBalancedBaseClass();

			if (playerClass == null)
			{
				return null;
			}

			PlayerTemplate template = PlayerTemplateData.getInstance().getTemplate(playerClass);

			if (template == null)
			{
				return null;
			}

			String name = buildUniqueName();

			Race race = template.getRace();
			boolean female;

			if (race == Race.KAMAEL)
			{
				female = (playerClass == PlayerClass.WARDER);
			}
			else
			{
				female = Rnd.get(100) < 45;
			}

			byte hairStyle = getSafeHairStyle(race, female);
			byte hairColor = (byte) Rnd.get(4);
			byte face = (byte) Rnd.get(3);

			PlayerAppearance appearance =
				new PlayerAppearance(face, hairColor, hairStyle, female);

			Player phantom =
				Player.create(template, ACCOUNT_NAME, name, appearance);

			if (phantom == null)
			{
				return null;
			}

			phantom.setOnlineStatus(true, true);
			phantom.setRunning();

			phantom.setInvisible(false);
			phantom.setInvul(false);

			phantom.setCurrentHp(phantom.getMaxHp());
			phantom.setCurrentMp(phantom.getMaxMp());

			phantom.broadcastUserInfo();
			phantom.broadcastInfo();

			phantom.storeMe();

			PhantomManager.logToFile(
				name,
				"Creado automaticamente. Race=" + race + " Class=" + playerClass
			);

			return phantom;
		}
		catch (Exception e)
		{
			PhantomManager.logException(
				"AUTO_CREATE",
				"Error creando phantom",
				e
			);

			return null;
		}
	}

	private static byte getSafeHairStyle(Race race, boolean female)
	{
		switch (race)
		{
			case KAMAEL:
			{
				return (byte) Rnd.get(female ? 5 : 4);
			}

			case DWARF:
			{
				return (byte) Rnd.get(female ? 6 : 5);
			}

			default:
			{
				return (byte) Rnd.get(female ? 6 : 5);
			}
		}
	}

	// Novo metodo: 5 guerreiros para cada 1 mago.
	private static synchronized PlayerClass getBalancedBaseClass()
	{
		List<PlayerClass> available = new ArrayList<>();
		
		_spawnCounter++;
		
		boolean isMage = (_spawnCounter % 6 == 0);
		PlayerClass[] targetGroup = isMage ? MAGE_CLASSES : FIGHTER_CLASSES;

		// Evitamos que o contador cresca ao infinito
		if (_spawnCounter >= 60)
		{
			_spawnCounter = 0;
		}

		for (PlayerClass playerClass : targetGroup)
		{
			PlayerTemplate template = PlayerTemplateData.getInstance().getTemplate(playerClass);

			if ((template != null) && isPlayableRace(template.getRace()))
			{
				available.add(playerClass);
			}
		}

		return available.isEmpty() ? null : available.get(Rnd.get(available.size()));
	}

	private static boolean isPlayableRace(Race race)
	{
		return (race == Race.HUMAN)
			|| (race == Race.ELF)
			|| (race == Race.DARK_ELF)
			|| (race == Race.ORC)
			|| (race == Race.DWARF)
			|| (race == Race.KAMAEL);
	}

	private static String buildUniqueName()
	{
		for (int i = 0; i < 1000; i++)
		{
			String name;

			switch (Rnd.get(3))
			{
				case 0:
					name = PREFIXES[Rnd.get(PREFIXES.length)];
					break;

				case 1:
					name = FIRST_NAMES[Rnd.get(FIRST_NAMES.length)];
					break;

				default:
					name = SUFFIXES[Rnd.get(SUFFIXES.length)];
					break;
			}

			//if (Rnd.get(100) < 5)
			//{
				//name += Rnd.get(10, 999);
			//}

			if (name.length() > 16)
			{
				name = name.substring(0, 16);
			}

			if (!CharInfoTable.getInstance().doesCharNameExist(name))
			{
				return name;
			}
		}

		return "Player" + Rnd.get(1000, 99999);
	}
}