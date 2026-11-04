package com.l2journey.gameserver.votesystem.util;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

import com.l2journey.commons.util.IXmlReader;
import com.l2journey.gameserver.model.StatSet;
import com.l2journey.gameserver.votesystem.model.Reward;
import com.l2journey.gameserver.votesystem.model.VoteSite;

/**
 * Loads vote sites and rewards using the shared datapack XML reader.
 * @author l2.topgameserver.net
 */
public class VoteSiteData implements IXmlReader
{
	public static final int GLOBAL_REWARD_SITE = 11;
	private static final Logger LOGGER = Logger.getLogger(VoteSiteData.class.getName());
	private volatile Map<Integer, VoteSite> _voteSites = Map.of();
	private volatile Map<Integer, Settings> _settings = Map.of();
	private volatile Map<Integer, java.util.List<Reward>> _globalRewards = Map.of();
	public record Settings(boolean individual, boolean global, int globalStep, long checkInterval, java.time.ZoneId zone) { }

	protected VoteSiteData()
	{
		load();
	}

	@Override
	public synchronized void load()
	{
		parseDatapackFile("data/votesystem.xml");
		LOGGER.info(() -> getClass().getSimpleName() + ": Loaded " + _voteSites.size() + " reward sites.");
	}

	@Override
	public synchronized void parseDocument(Document document, File file)
	{
		final Map<Integer, VoteSite> sites = new HashMap<>();
		final Map<Integer, Settings> settings = new HashMap<>();
		final Map<Integer, java.util.List<Reward>> globalRewards = new HashMap<>();
		forEach(document, "list", listNode -> forEach(listNode, "votesite", siteNode ->
		{
			try
			{
				final NamedNodeMap attrs = siteNode.getAttributes();
				final int ordinal = parseInteger(attrs, "ordinal", -1);
				final String name = parseString(attrs, "name", "").trim();
				if ((com.l2journey.gameserver.votesystem.Enum.VoteSite.fromId(ordinal) == null) && (ordinal != GLOBAL_REWARD_SITE) || name.isEmpty())
				{
					throw new IllegalArgumentException("Invalid site ordinal or empty name");
				}
				if (sites.containsKey(ordinal))
				{
					throw new IllegalArgumentException("Duplicate site ordinal " + ordinal);
				}
				final VoteSite site = new VoteSite();
				site.setSiteOrdinal(ordinal);
				site.setSiteName(name);
				forEach(siteNode, "items", itemsNode -> forEach(itemsNode, "item", itemNode ->
				{
					final Reward reward = new Reward(new StatSet(parseAttributes(itemNode)));
					if ((reward.getItemId() <= 0) || (reward.getItemCount() <= 0))
					{
						throw new IllegalArgumentException("Reward itemId and itemCount must be positive");
					}
					site.getRewardList().add(reward);
				}));
				final java.util.List<Reward> globalItems = new java.util.ArrayList<>();
				forEach(siteNode, "globalItems", itemsNode -> forEach(itemsNode, "item", itemNode ->
				{
					final Reward reward = new Reward(new StatSet(parseAttributes(itemNode)));
					if (reward.getItemId() <= 0 || reward.getItemCount() <= 0) { throw new IllegalArgumentException("Invalid global reward"); }
					globalItems.add(reward);
				}));
				final int step = parseInteger(attrs, "globalStep", 0);
				final int seconds = parseInteger(attrs, "checkInterval", 0);
				if ((step < 0) || (seconds < 0)) { throw new IllegalArgumentException("Negative step or check interval"); }
				final java.time.ZoneId zone = java.time.ZoneId.of(parseString(attrs, "timeZone", defaultZone(ordinal)));
				settings.put(ordinal, new Settings(enabled(attrs, "individualEnabled"), enabled(attrs, "globalEnabled"), step, seconds * 1000L, zone));
				if (!globalItems.isEmpty()) { globalRewards.put(ordinal, java.util.List.copyOf(globalItems)); }
				sites.put(ordinal, site);
			}
			catch (IllegalArgumentException | java.time.DateTimeException e)
			{
				LOGGER.log(Level.WARNING, "Invalid vote site skipped in " + file.getName(), e);
			}
		}));
		// Publish only a completed load; readers never observe a partially populated map.
		_globalRewards = Map.copyOf(globalRewards);
		_settings = Map.copyOf(settings);
		_voteSites = Map.copyOf(sites);
	}

	public synchronized String getSiteName(int ordinal)
	{
		final VoteSite site = _voteSites.get(ordinal);
		return site == null ? "Unknown vote site #" + ordinal : site.getSiteName();
	}

	public synchronized Collection<Reward> getRewards(int ordinal)
	{
		final VoteSite site = _voteSites.get(ordinal);
		return site == null ? List.of() : List.copyOf(site.getRewardList());
	}

	public synchronized Collection<Reward> getGlobalRewards(int site)
	{
		return _globalRewards.containsKey(site) ? _globalRewards.get(site) : getRewards(GLOBAL_REWARD_SITE);
	}

	private static boolean enabled(NamedNodeMap attrs, String key)
	{
		final var node = attrs.getNamedItem(key);
		if (node == null) { return true; }
		return switch (node.getNodeValue())
		{
			case "true", "1" -> true;
			case "false", "0" -> false;
			default -> throw new IllegalArgumentException("Invalid boolean attribute " + key);
		};
	}

	private static String defaultZone(int site)
	{
		return switch (site)
		{
			case 0 -> "Europe/Berlin";
			case 4 -> "America/Phoenix";
			case 7, 15 -> "America/Sao_Paulo";
			case 13, 14 -> "Europe/Athens";
			default -> "UTC";
		};
	}
	public synchronized boolean isIndividualEnabled(int site)
	{
		final Settings settings = _settings.get(site);
		return settings != null && settings.individual();
	}
	public synchronized boolean isGlobalEnabled(int site)
	{
		final Settings settings = _settings.get(site);
		return settings != null && settings.global();
	}
	public synchronized Settings getSettings(int site)
	{
		return _settings.getOrDefault(site, new Settings(false, false, 0, 0, java.time.ZoneId.of(defaultZone(site))));
	}

	public static VoteSiteData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final VoteSiteData INSTANCE = new VoteSiteData();
	}
}
