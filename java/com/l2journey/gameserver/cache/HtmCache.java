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
package com.l2journey.gameserver.cache;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.Config;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.network.enums.ChatType;
import com.l2journey.gameserver.network.serverpackets.CreatureSay;

/**
 * @author Layane, Mobius
 */
public class HtmCache
{
	private static final Logger LOGGER = Logger.getLogger(HtmCache.class.getName());
	
	private static final Map<String, String> HTML_CACHE = Config.HTM_CACHE ? new HashMap<>() : new ConcurrentHashMap<>();
	
	private int _loadedFiles;
	private long _bytesBuffLen;
	
	protected HtmCache()
	{
		reload();
	}
	
	public void reload()
	{
		reload(Config.DATAPACK_ROOT);
	}
	
	public void reload(File file)
	{
		if (Config.HTM_CACHE)
		{
			LOGGER.info("Html cache start...");
			parseDir(file);
			LOGGER.info(() -> "Cache[HTML]: " + String.format("%.3f", getMemoryUsage()) + " megabytes on " + _loadedFiles + " files loaded.");
		}
		else
		{
			HTML_CACHE.clear();
			_loadedFiles = 0;
			_bytesBuffLen = 0;
			LOGGER.info("Cache[HTML]: Running lazy cache.");
		}
	}
	
	public void reloadPath(File file)
	{
		parseDir(file);
		LOGGER.info("Cache[HTML]: Reloaded specified path.");
	}
	
	public double getMemoryUsage()
	{
		return (float) _bytesBuffLen / 1048576;
	}
	
	public int getLoadedFiles()
	{
		return _loadedFiles;
	}
	
	private void parseDir(File dir)
	{
		final File[] files = dir.listFiles();
		if (files != null)
		{
			for (File file : files)
			{
				if (!file.isDirectory())
				{
					loadFile(file);
				}
				else
				{
					parseDir(file);
				}
			}
		}
	}
	
	public String loadFile(File file)
	{
		if ((file == null) || !file.isFile())
		{
			return null;
		}
		
		final String lowerCaseName = file.getName().toLowerCase();
		if (!(lowerCaseName.endsWith(".htm") || lowerCaseName.endsWith(".html")))
		{
			return null;
		}
		
		String filePath = null;
		String content = null;
		try (FileInputStream fis = new FileInputStream(file);
			BufferedInputStream bis = new BufferedInputStream(fis))
		{
			final int bytes = bis.available();
			final byte[] raw = new byte[bytes];
			
			bis.read(raw);
			content = new String(raw, StandardCharsets.UTF_8);
			content = content.replaceAll("(?s)<!--.*?-->", ""); // Remove html comments.
			content = content.replaceAll("[\\t\\n]", ""); // Remove tabs and new lines.
			
			// Automatic removal of -h parameter from specific bypasses.
			if (Config.HIDE_BYPASS_REMOVAL)
			{
				content = content.replace("bypass -h npc_%objectId%_Chat ", "bypass npc_%objectId%_Chat ");
				content = content.replace("bypass -h npc_%objectId%_Quest", "bypass npc_%objectId%_Quest");
			}
			
			filePath = file.toURI().getPath().substring(Config.DATAPACK_ROOT.toURI().getPath().length());
			if (Config.CHECK_HTML_ENCODING && !filePath.startsWith("data/lang") && !StandardCharsets.US_ASCII.newEncoder().canEncode(content))
			{
				final String logMessage = "HTML encoding check: File " + filePath + " contains non ASCII content.";
				LOGGER.warning(() -> logMessage);
			}
			
			final String oldContent = HTML_CACHE.put(filePath, content);
			if (oldContent == null)
			{
				_bytesBuffLen += bytes;
				_loadedFiles++;
			}
			else
			{
				_bytesBuffLen = (_bytesBuffLen - oldContent.length()) + bytes;
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Problem with htm file:", e);
		}
		return content;
	}
	
	public String getHtm(Player player, String path)
	{
		final String prefix = player != null ? player.getHtmlPrefix() : "";
		String newPath = prefix + path;
		String content = HTML_CACHE.get(newPath);
		if (!Config.HTM_CACHE && (content == null))
		{
			content = loadFile(new File(Config.DATAPACK_ROOT, newPath));
			if (content == null)
			{
				content = loadFile(new File(Config.SCRIPT_ROOT, newPath));
			}
		}
		
		// In case localisation does not exist try the default path.
		if (Config.RESTORE_HTML_BY_DEFAULT && (content == null) && !prefix.contentEquals(""))
		{
			if (Config.HTM_CACHE)
			{
				content = HTML_CACHE.get(path);
			}
			else
			{
				content = loadFile(new File(Config.DATAPACK_ROOT, path));
				if (content == null)
				{
					content = loadFile(new File(Config.SCRIPT_ROOT, path));
				}
			}
			newPath = path;
		}
		
		if ((player != null) && player.isGM() && Config.GM_DEBUG_HTML_PATHS)
		{
			player.sendPacket(new CreatureSay(null, ChatType.GENERAL, "HTML", newPath.substring(5)));
		}
		
		return content;
	}
	
	public boolean contains(String path)
	{
		return HTML_CACHE.containsKey(path);
	}
	
	public static HtmCache getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final HtmCache INSTANCE = new HtmCache();
	}
}
