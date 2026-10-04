package com.l2journey.gameserver.model.phantom;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.managers.ZoneManager;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.zone.type.PeaceZone;
import com.l2journey.gameserver.model.zone.type.TownZone;

public class PhantomHuntingSpots
{
	// Inicializamos o Logger oficial do console
	private static final Logger LOGGER = Logger.getLogger(PhantomHuntingSpots.class.getName());
	
	private static final Map<Integer, Integer> NPC_LEVELS = new HashMap<>();
	private static final Map<Integer, String> NPC_NAMES = new HashMap<>();
	private static final Map<Integer, List<Location>> SPOTS_BY_LEVEL = new HashMap<>();
	
	private static boolean _loaded = false;
	private PhantomHuntingSpots()
	{
	}

	public static synchronized void load()
	{
		if (_loaded)
		{
			return;
		}
		
		NPC_LEVELS.clear();
		NPC_NAMES.clear();
		SPOTS_BY_LEVEL.clear();
		
		loadNpcLevels(new File("data/stats/npcs"));
		loadSpawnSpots(new File("data/spawns"));
		
		_loaded = true;
		
		int total = 0;
		for (List<Location> spots : SPOTS_BY_LEVEL.values())
		{
			total += spots.size();
		}
		
		// Anuncio limpo que se adapta ao formato do console
		LOGGER.info("[Phantom Hunting Spots]: Loaded " + total + " coordinates across " + SPOTS_BY_LEVEL.size() + " levels.");
	}
	
	public static boolean relocateForLevelIfNeeded(Player bot)
	{
		int band = getLevelBand(bot.getLevel());
		int objectId = bot.getObjectId();
		
		if (System.currentTimeMillis() < PhantomState.NEXT_HUNT_TELEPORT.getOrDefault(objectId, 0L))
		{
			return false;
		}
		
		// CHEQUEO ANTI-ESTANCAMIENTO: Se o bot respawnou na cidade, seu alvo sera de nivel muito baixo
		// ou diretamente nao tera alvo. Se ja passou de faixa e nao esta no seu spot, forcamos.
		boolean forceRelocate = false;
		if ((bot.getTarget() != null) && bot.getTarget().isNpc())
		{
			int targetId = ((com.l2journey.gameserver.model.actor.Npc) bot.getTarget()).getId();
			Integer targetLevel = NPC_LEVELS.get(targetId);
			
			// Se o alvo existe, e a diferenca de nivel e absurda (ex: bot 15 batendo em um nivel 1)
			if ((targetLevel != null) && ((bot.getLevel() - targetLevel) >= 10))
			{
				forceRelocate = true;
			}
		}
		else if (bot.isInsideZone(com.l2journey.gameserver.model.zone.ZoneId.PEACE) || bot.isInsideZone(com.l2journey.gameserver.model.zone.ZoneId.TOWN))
		{
			// Se o bot esta dentro de uma cidade e nao esta morto, forcamos a realocacao.
			forceRelocate = true;
		}
		
		// Se NAO forcamos a realocacao e esta na sua faixa de nivel correta, ignoramos.
		if (!forceRelocate && (PhantomState.HUNT_LEVEL_BAND.getOrDefault(objectId, -1) == band))
		{
			return false;
		}
		
		Location spot = getRandomSpot(bot.getLevel());
		if (spot == null)
		{
			return false;
		}
		
		return travelToLevelSpot(bot, spot, "Viajando a spot de leveo L" + bot.getLevel() + (forceRelocate ? " (Rescate)" : ""));
	}
	
	public static boolean travelToLevelSpot(Player bot, String reason)
	{
		Location spot = getRandomSpot(bot.getLevel());
		if (spot == null)
		{
			return false;
		}
		
		return travelToLevelSpot(bot, spot, reason);
	}
	
	public static boolean travelToLevelSpot(Player bot, Location spot, String reason)
	{
		if (spot == null)
		{
			return false;
		}
		
		PhantomState.HUNT_LEVEL_BAND.put(bot.getObjectId(), getLevelBand(bot.getLevel()));
		
		Location safe = PhantomGeo.getNpcLikeSpawn(spot);
		if (safe == null)
		{
			safe = spot;
		}
		
		PhantomEngine.movePhantomToWithReturn(bot, safe, reason);
		PhantomManager.logToFile(bot.getName(), reason + ": " + safe.getX() + ", " + safe.getY() + ", " + safe.getZ());
		return true;
	}
	
	/**
	 * @param playerLevel phantom level
	 * @param maxAttempts number of {@link #getRandomSpot(int)} tries
	 * @return first spot outside peace and town zones, or {@code null}
	 */
	public static Location getRandomSpotOutsidePeaceOrTown(int playerLevel, int maxAttempts)
	{
		for (int attempt = 0; attempt < maxAttempts; attempt++)
		{
			Location spot = getRandomSpot(playerLevel);
			if ((spot != null) && !isInsidePeaceOrTown(spot.getX(), spot.getY(), spot.getZ()))
			{
				return spot;
			}
		}
		return null;
	}
	
	static boolean isInsidePeaceOrTown(int x, int y, int z)
	{
		return (ZoneManager.getInstance().getZone(x, y, z, PeaceZone.class) != null) || (ZoneManager.getInstance().getZone(x, y, z, TownZone.class) != null);
	}
	
	public static Location getRandomSpot(int playerLevel)
	{
		for (int range = 0; range <= 12; range++)
		{
			List<Location> candidates = new ArrayList<>();
			int minLevel = Math.max(1, playerLevel - 3 - range);
			int maxLevel = playerLevel + 5 + range;
			
			for (int level = minLevel; level <= maxLevel; level++)
			{
				List<Location> spots = SPOTS_BY_LEVEL.get(level);
				if (spots != null)
				{
					candidates.addAll(spots);
				}
			}
			
			if (!candidates.isEmpty())
			{
				return candidates.get(Rnd.get(candidates.size()));
			}
		}
		return null;
	}
	
	private static int getLevelBand(int level)
	{
		return Math.max(1, (level / 5) * 5);
	}
	
	private static void loadNpcLevels(File dir)
	{
		File[] files = dir.listFiles();
		if (files == null)
		{
			return;
		}
		
		for (File file : files)
		{
			if (file.isDirectory())
			{
				loadNpcLevels(file);
			}
			else if (file.getName().endsWith(".xml"))
			{
				parseNpcFile(file);
			}
		}
	}
	
	private static void parseNpcFile(File file)
	{
		try
		{
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
			NodeList npcNodes = doc.getElementsByTagName("npc");
			
			for (int i = 0; i < npcNodes.getLength(); i++)
			{
				Element npc = (Element) npcNodes.item(i);
				
				String type = npc.getAttribute("type");
				if ((type == null) || !type.equalsIgnoreCase("Monster"))
				{
					continue;
				}
				
				String levelText = npc.getAttribute("level");
				if ((levelText == null) || levelText.isEmpty())
				{
					continue;
				}
				
				String idText = npc.getAttribute("id");
				if ((idText == null) || idText.isEmpty())
				{
					continue;
				}
				
				int id = Integer.parseInt(idText);
				int level = Integer.parseInt(levelText);
				String name = npc.getAttribute("name");
				
				if (isForbiddenTargetName(name))
				{
					continue;
				}
				
				NPC_LEVELS.put(id, level);
				NPC_NAMES.put(id, name);
			}
		}
		catch (Exception e)
		{
			PhantomManager.logToFile("SPOT_LOADER", "Error reading NPCs " + file.getName() + ": " + e.getMessage());
		}
	}
	
	private static void loadSpawnSpots(File dir)
	{
		File[] files = dir.listFiles();
		if (files == null)
		{
			return;
		}
		
		for (File file : files)
		{
			if (file.isDirectory())
			{
				loadSpawnSpots(file);
			}
			else if (file.getName().endsWith(".xml") && !file.getName().toLowerCase().contains("raid"))
			{
				parseSpawnFile(file);
			}
		}
	}
	
	private static void parseSpawnFile(File file)
	{
		try
		{
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
			NodeList spawnNodes = doc.getElementsByTagName("spawn");
			
			for (int i = 0; i < spawnNodes.getLength(); i++)
			{
				Element spawn = (Element) spawnNodes.item(i);
				
				NodeList territoryList = spawn.getElementsByTagName("territory");
				if (territoryList.getLength() == 0)
				{
					continue;
				}
				
				Element territory = (Element) territoryList.item(0);
				
				String minZText = territory.getAttribute("minZ");
				String maxZText = territory.getAttribute("maxZ");
				if (minZText.isEmpty() || maxZText.isEmpty())
				{
					continue;
				}
				
				int minZ = Integer.parseInt(minZText);
				int maxZ = Integer.parseInt(maxZText);
				int middleZ = (minZ + maxZ) / 2;
				
				// --- NOVA LOGICA: Calcular o Centro da area ---
				long sumX = 0;
				long sumY = 0;
				int count = 0;
				NodeList nodeList = territory.getElementsByTagName("node");
				
				for (int n = 0; n < nodeList.getLength(); n++)
				{
					Element node = (Element) nodeList.item(n);
					String xText = node.getAttribute("x");
					String yText = node.getAttribute("y");
					
					if (!xText.isEmpty() && !yText.isEmpty())
					{
						sumX += Integer.parseInt(xText);
						sumY += Integer.parseInt(yText);
						count++;
					}
				}
				
				if (count == 0)
				{
					continue;
				}
				
				// Calculamos o centro exato
				int centerX = (int) (sumX / count);
				int centerY = (int) (sumY / count);
				// -------------------------------------------------
				
				NodeList npcList = spawn.getElementsByTagName("npc");
				for (int j = 0; j < npcList.getLength(); j++)
				{
					Element npc = (Element) npcList.item(j);
					
					String idText = npc.getAttribute("id");
					if (idText.isEmpty())
					{
						continue;
					}
					
					int id = Integer.parseInt(idText);
					Integer level = NPC_LEVELS.get(id);
					
					if (level == null)
					{
						continue;
					}
					
					String npcName = NPC_NAMES.get(id);
					if (isForbiddenTargetName(npcName))
					{
						continue;
					}
					
					List<Location> spots = SPOTS_BY_LEVEL.computeIfAbsent(level, k -> new ArrayList<>());
					
					// Adicionamos o centro (com uma pequena margem de aleatoriedade para nao se sobreporem pixel a pixel)
					if (spots.size() < 6000)
					{
						spots.add(new Location(centerX + Rnd.get(-30, 30), centerY + Rnd.get(-30, 30), middleZ));
					}
				}
			}
		}
		catch (Exception e)
		{
			PhantomManager.logToFile("SPOT_LOADER", "Error reading spawns " + file.getName() + ": " + e.getMessage());
		}
	}
	
	public static boolean isForbiddenTargetName(String name)
	{
		if (name == null)
		{
			return false;
		}
		
		String lowerName = name.toLowerCase();
		return lowerName.contains("training")
				|| lowerName.contains("dummy")
				|| lowerName.contains("practice")
				|| lowerName.contains("tutorial")
				|| lowerName.contains("event")
				|| lowerName.contains("chest")
				|| lowerName.contains("treasure chest");
	}
}
