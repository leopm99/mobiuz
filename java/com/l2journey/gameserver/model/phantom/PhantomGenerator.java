package com.l2journey.gameserver.model.phantom;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.PhantomConfig;
import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.managers.IdManager;
import com.l2journey.gameserver.model.actor.Player;

public class PhantomGenerator
{
	private static final Logger LOGGER = Logger.getLogger(PhantomGenerator.class.getName());
	
	// --------------------------------------------------------------------------
	// CONFIGURACAO DE SPAWN E CLASSES (SOMENTE MAGOS/XAMAS)
	// --------------------------------------------------------------------------
	private static final int SPAWN_X = -84318;
	private static final int SPAWN_Y = 244579;
	private static final int SPAWN_Z = -3730;
	
	private static final int[] CLASSES =
	{
		10, // Human Mystic
		25, // Elven Mystic
		38, // Dark Mystic
		49 // Orc Mystic
	};
	
	private static final int[] RACES =
	{
		0, // Human
		1, // Elf
		2, // Dark Elf
		3 // Orc
	};
	
	// --------------------------------------------------------------------------
	// GERADOR DE NOMES
	// --------------------------------------------------------------------------
	private static final String[] SYL_1 =
	{
		"A",
		"Be",
		"Ca",
		"Da",
		"El",
		"Fa",
		"Ga",
		"He",
		"I",
		"Ka",
		"La",
		"Ma",
		"Na",
		"O",
		"Pa",
		"Ra",
		"Sa",
		"Ta",
		"Va",
		"Za"
	};
	
	private static final String[] SYL_2 =
	{
		"la",
		"me",
		"ni",
		"ro",
		"su",
		"da",
		"fe",
		"gi",
		"ko",
		"lu",
		"ma",
		"ne",
		"pi",
		"ra",
		"se",
		"to",
		"vu",
		"za"
	};
	
	private static final String[] SYL_3 =
	{
		"s",
		"n",
		"r",
		"l",
		"th",
		"x",
		"dor",
		"mar",
		"nis",
		"las",
		"mus",
		"ria",
		"tia",
		"nia",
		"wyn"
	};
	
	// --------------------------------------------------------------------------
	// LOGICA PRINCIPAL DE CRIACAO
	// --------------------------------------------------------------------------
	private PhantomGenerator()
	{
	}
	
	public static void createInGame(int cantidad, Player gm)
	{
		createInGame(cantidad, gm, Path.of("config", "PhantomPlayers.xml"), DatabaseFactory::getConnection, () -> IdManager.getInstance().getNextId());
	}
	
	static void createInGame(int cantidad, Player gm, Path xmlPath, Supplier<Connection> connections, IntSupplier nextCharId)
	{
		int creados = 0;
		String query = "INSERT IGNORE INTO `characters` (`charId`, `account_name`, `char_name`, `level`, `maxHp`, `curHp`, `maxCp`, `curCp`, `maxMp`, `curMp`, `face`, `hairStyle`, `hairColor`, `sex`, `x`, `y`, `z`, `race`, `classid`, `base_class`, `title`, `online`, `accesslevel`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		
		try (Connection con = connections.get();
			PreparedStatement ps = con.prepareStatement(query))
		{
			ps.setString(2, "PhantomAuto");
			ps.setInt(4, 1);
			ps.setInt(5, 1000);
			ps.setInt(6, 1000);
			ps.setInt(7, 500);
			ps.setInt(8, 500);
			ps.setInt(9, 1000);
			ps.setInt(10, 1000);
			ps.setInt(15, SPAWN_X);
			ps.setInt(16, SPAWN_Y);
			ps.setInt(17, SPAWN_Z);
			ps.setString(21, "");
			ps.setInt(22, 0);
			ps.setInt(23, 0);
			for (int i = 0; i < cantidad; i++)
			{
				int charId = nextCharId.getAsInt();
				PhantomRoll roll = rollPhantomAttributes();
				
				ps.setInt(1, charId);
				ps.setString(3, roll.name());
				ps.setInt(11, roll.face());
				ps.setInt(12, roll.hairStyle());
				ps.setInt(13, roll.hairColor());
				ps.setInt(14, roll.sex());
				ps.setInt(18, roll.race());
				ps.setInt(19, roll.classId());
				ps.setInt(20, roll.classId());
				
				ps.addBatch();
				PhantomConfig.PHANTOM_IDS.add(charId);
				creados++;
			}
			
			ps.executeBatch();
			saveXML(xmlPath);
			if (gm != null)
			{
				gm.sendMessage("¡Éxito! Se han creado " + creados + " Phantoms Magos/Chamanes nuevos.");
			}
		}
		catch (Exception e)
		{
			if (gm != null)
			{
				gm.sendMessage("Error creando phantoms: " + e.getMessage());
			}
			logCreateFailure(e);
		}
	}
	
	static PhantomRoll rollPhantomAttributes()
	{
		final String name = Rnd.pick(SYL_1) + Rnd.pick(SYL_2) + Rnd.pick(SYL_3);
		final int classId = Rnd.pick(CLASSES);
		final int race = RACES[switch (classId)
		{
			case 10 -> 0;
			case 25 -> 1;
			case 38 -> 2;
			default -> 3;
		}];
		return new PhantomRoll(name, classId, race, Rnd.get(2), Rnd.get(3), Rnd.get(4), Rnd.get(3));
	}
	
	record PhantomRoll(String name, int classId, int race, int sex, int face, int hairStyle, int hairColor)
	{
	}
	
	static void logCreateFailure(Exception e)
	{
		LOGGER.log(Level.SEVERE, e, () -> "Failed to create phantoms: " + e.getMessage());
	}
	
	// --------------------------------------------------------------------------
	// SALVAMENTO EM XML
	// --------------------------------------------------------------------------
	static void saveXML(Path target)
	{
		try (FileWriter fw = new FileWriter(target.toFile());
			PrintWriter pw = new PrintWriter(fw))
		{
			pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<list>");
			for (int id : PhantomConfig.PHANTOM_IDS)
			{
				pw.println("\t<phantom charId=\"" + id + "\" />");
			}
			pw.println("</list>");
		}
		catch (Exception e)
		{
			logSaveFailure(e);
		}
	}
	
	static void logSaveFailure(Exception e)
	{
		LOGGER.log(Level.SEVERE, e, () -> "Failed to save PhantomPlayers.xml: " + e.getMessage());
	}
}
