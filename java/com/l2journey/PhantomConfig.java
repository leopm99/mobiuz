package com.l2journey;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.model.phantom.PhantomManager;

import com.l2journey.gameserver.model.phantom.PhantomHuntingSpots;

public class PhantomConfig
{
	private static final String PHANTOM_XML_PATH = "config/npcs/phantomplayers.xml";
	public static final List<Integer> PHANTOM_IDS = new ArrayList<>();
	public static final List<FarmZone> ROUTES = new ArrayList<>();
	
	public static class FarmZone
	{
		public int minLvl, maxLvl, spotX, spotY, spotZ, townX, townY, townZ;
		public FarmZone(int min, int max, int sx, int sy, int sz, int tx, int ty, int tz)
		{
			this.minLvl = min; this.maxLvl = max; this.spotX = sx; this.spotY = sy; this.spotZ = sz;
			this.townX = tx; this.townY = ty; this.townZ = tz;
		}
	}
	
	public static void init()
	{
		ROUTES.clear();
		ROUTES.add(new FarmZone(1, 5, -81887, 246451, -3680, -75056, 252947, -3331)); 
		ROUTES.add(new FarmZone(1, 5, -123940, 45054, -893, -116727, 45877, -367)); 
		ROUTES.add(new FarmZone(1, 5, -42652, 42522, -3492, -46622, 49942, -3060)); 

		ROUTES.add(new FarmZone(5, 10, -92693, 240541, -3415, -75056, 252947, -3331));
		ROUTES.add(new FarmZone(5, 10, 53246, 51356, -3520, 46618, 49482, -3060));
		ROUTES.add(new FarmZone(5, 10, -121786, 59971, -2358, -115766, 46707, -501));
		ROUTES.add(new FarmZone(5, 10, -122503, 62967, -2586, -115766, 46707, -501));
		
		ROUTES.add(new FarmZone(10, 15, -103844, 226807, -3646, -75056, 252947, -3331));
		ROUTES.add(new FarmZone(10, 15, -89319, 55760, -2696, -116725, 45512, -367));
		ROUTES.add(new FarmZone(10, 15, -16319, 46334, -3781, 12638, 16559, -4585));

		ROUTES.add(new FarmZone(15, 25, -43846, 117922, -3556, -14225, 123540, -3121));
		ROUTES.add(new FarmZone(15, 25, -19133, 142179, -3897, 18795, 145167, -3129));
		ROUTES.add(new FarmZone(15, 25, 26432, 140341, -3003, 82599, 53720, -1496));
		ROUTES.add(new FarmZone(15, 25, -54682, 107983, -3752, 82599, 53720, -1496));
		ROUTES.add(new FarmZone(15, 25, -10456, 145362, -3812, -14225, 123540, -3121));
		
		ROUTES.add(new FarmZone(25, 35, 31336, 185804, -3515, 15670, 142983, -2705));
		ROUTES.add(new FarmZone(25, 35, 15374, 119521, -3754, 15670, 142983, -2705));
		ROUTES.add(new FarmZone(25, 35, 31336, 185804, -3515, 15670, 142983, -2705));
		ROUTES.add(new FarmZone(25, 35, 32426, 181568, -3334, 15670, 142983, -2705));
		ROUTES.add(new FarmZone(25, 35, 26601, 188034, -3475, 15670, 142983, -2705));
		ROUTES.add(new FarmZone(25, 35, 3357, 173462, -3103, 15670, 142983, -2705));

		ROUTES.add(new FarmZone(25, 35, 47848, 144586, -3675, 15670, 142983, -2705));
		ROUTES.add(new FarmZone(35, 45, 47907, 144690, -3634, 15670, 142983, -2705));
		ROUTES.add(new FarmZone(35, 45, 43477, 149480, -3670, 15670, 142983, -2705));
		ROUTES.add(new FarmZone(35, 45, 111313, 187881, -3701, 111386, 218682, -3466));
		ROUTES.add(new FarmZone(35, 45, 48149, 206863, -3521, 111386, 218682, -3466));
		
		ROUTES.add(new FarmZone(45, 55, 180683, 17184, -3173, 148010, 28037, -2269));
		ROUTES.add(new FarmZone(45, 55, 59860, 161722, -2931, 148010, 28037, -2269));
		ROUTES.add(new FarmZone(45, 55, 46400, 205964, -3626, 148010, 28037, -2269));
		ROUTES.add(new FarmZone(45, 55, 65559, 158814, -3604, 148010, 28037, -2269));

		ROUTES.add(new FarmZone(50, 55, 181150, 23956, -3200, 148010, 28037, -2269));
        ROUTES.add(new FarmZone(50, 55, 172213, 42065, -4943, 148010, 28037, -2269));
		ROUTES.add(new FarmZone(55, 65, 189383, 22761, -3726, 148010, 28037, -2269));
		ROUTES.add(new FarmZone(55, 65, 190100, 18558, -3731, 148010, 28037, -2269));
		ROUTES.add(new FarmZone(55, 65, 108256, 2626, -3389, 148010, 28037, -2269));

		ROUTES.add(new FarmZone(65, 75, 147033, -8049, -4482, 146142, 26715, -2200));
		ROUTES.add(new FarmZone(65, 75, 142985, -9890, -4593, 87334, -142845, -1317));
		ROUTES.add(new FarmZone(65, 75, 141493, -13987, -4450, 146142, 26715, -2200));
		ROUTES.add(new FarmZone(65, 75, 140055, -59413, -3361, 87334, -142845, -1317));
		ROUTES.add(new FarmZone(65, 75, 142153, -61101, -3196, 146142, 26715, -2200));
		ROUTES.add(new FarmZone(70, 75, 134472, -47798, -3163, 87334, -142845, -1317));

		ROUTES.add(new FarmZone(70, 75, 156995, -58258, -3290, 147723, -56154, -2781));
		ROUTES.add(new FarmZone(75, 80, 175150, 54496, -6032, 147723, -56154, -2781));
		ROUTES.add(new FarmZone(75, 80, 185314, -48832, -6033, 147723, -56154, -2781));
		ROUTES.add(new FarmZone(75, 80, 111421, -54813, -2861, 147723, -56154, -2781));


		ROUTES.add(new FarmZone(80, 85, 82882, 110560, -3816, 147723, -56154, -2781));
		ROUTES.add(new FarmZone(80, 85, 142086, -85778, -4992, 147723, -56154, -2781));
		ROUTES.add(new FarmZone(80, 85, 124752, 115040, -3718, 138550, 114391, -3725));
		ROUTES.add(new FarmZone(80, 85, 115950, -83486, -3397, 18028, 170281, -3505));

		loadXML();
		PhantomHuntingSpots.load();
	}
	
	public static void loadXML()
	{
		PHANTOM_IDS.clear();
		try {
			File file = new File(PHANTOM_XML_PATH);
			if (!file.exists()) return;
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
			NodeList list = doc.getElementsByTagName("phantom");
			for (int i = 0; i < list.getLength(); i++)
				PHANTOM_IDS.add(Integer.parseInt(list.item(i).getAttributes().getNamedItem("charId").getNodeValue()));
		} catch (Exception e) { System.out.println(">>> [PHANTOM] ERROR XML: " + e.getMessage()); }
	}
	
	public static synchronized boolean addPhantomId(int charId)
	{
		if (PHANTOM_IDS.contains(charId)) return false;
		PHANTOM_IDS.add(charId);
		return true;
	}
	
	public static synchronized void saveXML()
	{
		try {
			File file = new File(PHANTOM_XML_PATH);
			
			if (file.getParentFile() != null && !file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}

			Set<Integer> uniqueIds = new LinkedHashSet<>(PHANTOM_IDS);
			try (FileWriter writer = new FileWriter(file, false)) {
				writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<list>\n");
				for (int charId : uniqueIds) writer.write("\t<phantom charId=\"" + charId + "\" />\n");
				writer.write("</list>\n");
			}
		} catch (Exception e) { PhantomManager.logToFile("XML", "Error: " + e.getMessage()); }
	}
	
	public static FarmZone getIdealZone(int level)
	{
		List<FarmZone> zonasDisponibles = new ArrayList<>();
		
		for (FarmZone zone : ROUTES) {
			if ((level >= zone.minLvl) && (level <= zone.maxLvl)) {
				zonasDisponibles.add(zone);
			}
		}
		
		if (!zonasDisponibles.isEmpty()) {
			return zonasDisponibles.get(Rnd.get(zonasDisponibles.size()));
		}
		
		return ROUTES.get(ROUTES.size() - 1);
	}
}