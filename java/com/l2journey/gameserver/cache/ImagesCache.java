package com.l2journey.gameserver.cache;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.Locale;
import java.util.logging.Level;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.l2journey.commons.database.DatabaseFactory;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.l2journey.Config;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.managers.GlobalVariablesManager;
import com.l2journey.gameserver.network.serverpackets.PledgeCrest;
import com.l2journey.gameserver.util.DDSConverter;

public class ImagesCache
{
	private static final Logger LOGGER = Logger.getLogger(ImagesCache.class.getName());
	
	private final Map<Integer, byte[]> _images = new ConcurrentHashMap<>();
	
	private final Map<String, Integer> _imagesIdByName = new ConcurrentHashMap<>();
	
	public static final Pattern HTML_PATTERN = Pattern.compile("%image:(.*?)%", Pattern.CASE_INSENSITIVE);
	
	protected ImagesCache()
	{
		load();
	}
	
	public synchronized void load()
	{
		_images.clear();
		_imagesIdByName.clear();
		
		final File imagesDir = new File(Config.DATAPACK_ROOT, "data/images/id_by_name/");
		if (!imagesDir.exists() || !imagesDir.isDirectory())
		{
			LOGGER.info(getClass().getSimpleName() + ": Pasta de imagens nao encontrada em 'data/images/id_by_name/'. Carregamento abortado.");
			return;
		}
		
		final File[] imageFiles = imagesDir.listFiles();
		if (imageFiles == null)
		{
			return;
		}
		
		final GlobalVariablesManager variables = GlobalVariablesManager.getInstance();
		final boolean hasNamedImages = java.util.Arrays.stream(imageFiles).anyMatch(file -> file.isFile() && (file.getName().lastIndexOf('.') > 0) && !file.getName().substring(0, file.getName().lastIndexOf('.')).matches("[0-9]+"));
		if (hasNamedImages && !migrateImageIds(variables)) { return; }
		final Set<Integer> reservedIds = new HashSet<>();
		variables.getSet().forEach((name, value) ->
		{
			if (name.startsWith("ImageCache-"))
			{
				try { reservedIds.add(Integer.parseInt(String.valueOf(value))); }
				catch (NumberFormatException e) { LOGGER.warning("ImagesCache: Invalid persisted image ID for " + name); }
			}
		});
		// Reserve numeric image IDs before allocating named images, regardless of directory order.
		for (File file : imageFiles)
		{
			final int extension = file.getName().lastIndexOf('.');
			if (file.isFile() && (extension > 0))
			{
				try { reservedIds.add(Integer.parseInt(file.getName().substring(0, extension))); }
				catch (NumberFormatException e) { /* Named image. */ }
			}
		}
		int nextImageId = 1000000000;
		int loadedCount = 0;
		for (File file : imageFiles)
		{
			if (file.isDirectory())
			{
				continue;
			}
			
			int imageId;
			final String fileName = file.getName();
			final int extension = fileName.lastIndexOf('.');
			if (extension <= 0) { continue; }
			final ByteBuffer dds;
			try { dds = DDSConverter.convertToDDS(file); }
			catch (RuntimeException e)
			{
				LOGGER.log(Level.WARNING, "ImagesCache: Could not load " + fileName, e);
				continue;
			}
			if (dds == null) { continue; }
			final String baseName = fileName.substring(0, extension);
			try
			{
				imageId = Integer.parseInt(baseName);
			}
			catch (NumberFormatException e)
			{
				
				imageId = variables.getInt("ImageCache-" + fileName, 0);
				if (imageId == 0)
				{
					
					while (reservedIds.contains(nextImageId))
					{
						if (nextImageId == Integer.MAX_VALUE) { throw new IllegalStateException("Image ID space exhausted"); }
						nextImageId++;
					}
					imageId = nextImageId;
					variables.set("ImageCache-" + fileName, imageId);
					if (!saveImageIds(variables, Set.of("ImageCache-" + fileName)))
					{
						variables.remove("ImageCache-" + fileName);
						LOGGER.warning("ImagesCache: Image skipped because its ID could not be persisted: " + fileName);
						continue;
					}
					reservedIds.add(imageId);
				}
			}
			
			if ((imageId <= 0) || _images.containsKey(imageId))
			{
				LOGGER.warning("ImagesCache: Invalid or duplicate image ID " + imageId + " for " + fileName);
				continue;
			}
			_images.put(imageId, dds.array());
			_imagesIdByName.put(fileName.toLowerCase(Locale.ROOT), imageId);
			loadedCount++;
		}
		LOGGER.info(getClass().getSimpleName() + ": Carregadas " + loadedCount + " imagens customizadas.");
	}
	
	/** Copy legacy image IDs once, preserving every existing global mapping. */
	private static boolean migrateImageIds(GlobalVariablesManager variables)
	{
		final Set<String> migrated = new HashSet<>();
		try (Connection con = DatabaseFactory.getConnection())
		{
			boolean exists = false;
			try (ResultSet tables = con.getMetaData().getTables(con.getCatalog(), null, "server_variables", new String[] { "TABLE" }))
			{
				while (tables.next())
				{
					if ("server_variables".equalsIgnoreCase(tables.getString("TABLE_NAME"))) { exists = true; break; }
				}
			}
			if (exists)
			{
				try (PreparedStatement statement = con.prepareStatement("SELECT name, value FROM server_variables WHERE name LIKE 'ImageCache-%'");
					ResultSet rows = statement.executeQuery())
				{
					while (rows.next())
					{
						final String name = rows.getString("name");
						if (!variables.getSet().containsKey(name))
						{
							variables.set(name, rows.getString("value"));
							migrated.add(name);
						}
					}
				}
			}
		}
		catch (SQLException e)
		{
			migrated.forEach(variables::remove);
			LOGGER.log(Level.WARNING, "ImagesCache: Could not verify legacy image IDs; loading aborted to preserve mappings.", e);
			return false;
		}
		if (migrated.isEmpty() || saveImageIds(variables, migrated)) { return true; }
		migrated.forEach(variables::remove);
		return false;
	}

	/** Persist only image keys; never rewrite unrelated global variables. */
	private static boolean saveImageIds(GlobalVariablesManager variables, Set<String> names)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			con.setAutoCommit(false);
			try (PreparedStatement statement = con.prepareStatement("REPLACE INTO global_variables (var, value) VALUES (?, ?)"))
			{
				for (String name : names)
				{
					statement.setString(1, name);
					statement.setString(2, String.valueOf(variables.getSet().get(name)));
					statement.addBatch();
				}
				statement.executeBatch();
				con.commit();
			}
			catch (SQLException e)
			{
				con.rollback();
				throw e;
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "ImagesCache: Could not persist image IDs.", e);
			return false;
		}
		return true;
	}

	public String sendUsedImages(String html, Player player)
	{
		if ((html == null) || html.isEmpty() || (player == null))
		{
			return html;
		}
		
		String processedHtml = html;
		
		final Matcher m = HTML_PATTERN.matcher(processedHtml);
		while (m.find())
		{
			final String imageName = m.group(1).toLowerCase(Locale.ROOT);
			final Integer imageId = _imagesIdByName.get(imageName);
			if (imageId != null)
			{
				
				final String crestSyntax = "Crest.crest_" + Config.SERVER_ID + "_" + imageId;
				processedHtml = processedHtml.replace(m.group(0), crestSyntax);
			}
		}
		
		final String crestPrefix = "Crest.crest_" + Config.SERVER_ID + "_";
		int index = processedHtml.indexOf(crestPrefix);
		while (index != -1)
		{
			final int startIndex = index + crestPrefix.length();
			int endIndex = startIndex;
			while ((endIndex < processedHtml.length()) && Character.isDigit(processedHtml.charAt(endIndex)))
			{
				endIndex++;
			}
			
			if (endIndex > startIndex)
			{
				try
				{
					final int imageId = Integer.parseInt(processedHtml.substring(startIndex, endIndex));
					sendImageToPlayer(player, imageId);
				}
				catch (NumberFormatException e)
				{
					
				}
			}
			index = processedHtml.indexOf(crestPrefix, endIndex);
		}
		
		return processedHtml;
	}
	
	public void sendImageToPlayer(Player player, int imageId)
	{
		
		final byte[] data = _images.get(imageId);
		if (data != null)
		{
			player.sendPacket(new PledgeCrest(imageId, data));
			
		}
	}
	
	public int getImageId(String imageName)
	{
		return _imagesIdByName.getOrDefault(imageName.toLowerCase(Locale.ROOT), 0);
	}
	
	public byte[] getImage(int imageId)
	{
		return _images.get(imageId);
	}
	
	public static ImagesCache getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ImagesCache INSTANCE = new ImagesCache();
	}
}
