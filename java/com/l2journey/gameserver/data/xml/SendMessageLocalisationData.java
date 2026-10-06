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
package com.l2journey.gameserver.data.xml;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import com.l2journey.Config;
import com.l2journey.commons.util.IXmlReader;
import com.l2journey.gameserver.model.StatSet;
import com.l2journey.gameserver.model.actor.Player;

/**
 * @author Mobius
 */
public class SendMessageLocalisationData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(SendMessageLocalisationData.class.getName());
	
	private static final String SPLIT_STRING = "XXX";
	private static final Map<String, Map<String[], String[]>> SEND_MESSAGE_LOCALISATIONS = new ConcurrentHashMap<>();
	private static String _lang;
	
	protected SendMessageLocalisationData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		SEND_MESSAGE_LOCALISATIONS.clear();
		
		if (Config.MULTILANG_ENABLE)
		{
			for (String lang : Config.MULTILANG_ALLOWED)
			{
				final File file = new File("data/lang/" + lang + "/SendMessageLocalisation.xml");
				if (!file.isFile())
				{
					continue;
				}
				
				SEND_MESSAGE_LOCALISATIONS.put(lang, new ConcurrentHashMap<>());
				_lang = lang;
				parseDatapackFile("data/lang/" + lang + "/SendMessageLocalisation.xml");
				final int count = SEND_MESSAGE_LOCALISATIONS.get(lang).values().size();
				if (count == 0)
				{
					SEND_MESSAGE_LOCALISATIONS.remove(lang);
				}
				else
				{
					LOGGER.log(Level.INFO, () -> getClass().getSimpleName() + ": Loaded localisations for [" + lang + "].");
				}
			}
		}
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "localisation", localisationNode ->
		{
			final StatSet set = new StatSet(parseAttributes(localisationNode));
			SEND_MESSAGE_LOCALISATIONS.get(_lang).put(set.getString("message").split(SPLIT_STRING), set.getString("translation").split(SPLIT_STRING));
		}));
	}
	
	public static String getLocalisation(Player player, String message)
	{
		if (Config.MULTILANG_ENABLE && (player != null))
		{
			final String lang = (player.getLang() != null) ? player.getLang() : Config.MULTILANG_DEFAULT;
			
			final Map<String[], String[]> localisations = SEND_MESSAGE_LOCALISATIONS.get(lang);
			
			if (localisations != null)
			{
				// No pretty way of doing something like this.
				// Consider using proper SystemMessages where possible.
				String[] searchMessage;
				String[] replacementMessage;
				String localisation = message;
				boolean found;
				for (Entry<String[], String[]> entry : localisations.entrySet())
				{
					searchMessage = entry.getKey();
					replacementMessage = entry.getValue();
					
					// Exact match.
					if (searchMessage.length == 1)
					{
						if (searchMessage[0].equals(localisation))
						{
							return replacementMessage[0];
						}
					}
					else // Split match.
					{
						found = true;
						for (String part : searchMessage)
						{
							if (!localisation.contains(part))
							{
								found = false;
								break;
							}
						}
						if (found)
						{
							for (int i = 0; i < searchMessage.length; i++)
							{
								localisation = localisation.replace(searchMessage[i], replacementMessage[i]);
							}
							break;
						}
					}
				}
				return localisation;
			}
		}
		return message;
	}
	
	public static SendMessageLocalisationData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SendMessageLocalisationData INSTANCE = new SendMessageLocalisationData();
	}
}
