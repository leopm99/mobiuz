/*
 * Copyright (C) 2004-2015 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2journey.gameserver.model.dropcalculator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.l2journey.Config;
import com.l2journey.gameserver.data.xml.NpcData;
import com.l2journey.gameserver.model.actor.holders.npc.DropGroupHolder;
import com.l2journey.gameserver.model.actor.holders.npc.DropHolder;
import com.l2journey.gameserver.model.actor.templates.NpcTemplate;

/** Index of base drop percentages, rebuilt after NPC data reloads. */
public class DropInfoHandler
{
	private static final Logger LOGGER = Logger.getLogger(DropInfoHandler.class.getName());
	public static final Set<Integer> HERBS = Stream.of(IntStream.range(8154, 8158), IntStream.range(8600, 8615), IntStream.range(8952, 8954), IntStream.range(10655, 10658), IntStream.range(10432, 10434), IntStream.of(13028)).flatMapToInt(s -> s).boxed().collect(Collectors.toUnmodifiableSet());
	private volatile Snapshot _snapshot;
	
	private record Snapshot(long revision, List<Integer> restricted, Map<Integer, List<DropInfoHolder>> drops, Map<Integer, List<DropInfoHolder>> byNpc)
	{
	}
	
	public DropInfoHandler()
	{
		load();
	}
	
	public void load()
	{
		final NpcData data = NpcData.getInstance();
		synchronized (data)
		{
			final Map<Integer, List<DropInfoHolder>> drops = new HashMap<>();
			final List<Integer> restricted = restrictedIds();
			for (NpcTemplate npc : data.getTemplates(template -> !restricted.contains(template.getId())))
			{
				if (npc.getDropGroups() != null)
				{
					for (DropGroupHolder group : npc.getDropGroups())
					{
						for (DropHolder drop : group.getDropList())
						{
							add(drops, npc, drop, false, group.getChance() / 100d);
						}
					}
				}
				if (npc.getDropList() != null)
				{
					for (DropHolder drop : npc.getDropList())
					{
						add(drops, npc, drop, false, 1d);
					}
				}
				if (npc.getSpoilList() != null)
				{
					for (DropHolder drop : npc.getSpoilList())
					{
						add(drops, npc, drop, true, 1d);
					}
				}
			}
			drops.replaceAll((id, list) -> list.stream().sorted(Comparator.comparingInt(DropInfoHolder::getLevel)).toList());
			final Map<Integer, List<DropInfoHolder>> byNpc = new HashMap<>();
			drops.values().forEach(list -> list.forEach(drop -> byNpc.computeIfAbsent(drop.getNpcId(), id -> new ArrayList<>()).add(drop)));
			byNpc.replaceAll((id, list) -> List.copyOf(list));
			_snapshot = new Snapshot(data.getRevision(), restricted, Map.copyOf(drops), Map.copyOf(byNpc));
			LOGGER.info(() -> "DropInfoHandler: Loaded " + drops.size() + " drop data for calculator.");
		}
	}
	
	private static void add(Map<Integer, List<DropInfoHolder>> index, NpcTemplate npc, DropHolder drop, boolean spoil, double groupChance)
	{
		if (!HERBS.contains(drop.getItemId()))
		{
			index.computeIfAbsent(drop.getItemId(), id -> new ArrayList<>()).add(new DropInfoHolder(npc.getId(), npc.getName(), npc.getLevel(), drop.getMin(), drop.getMax(), drop.getChance() * groupChance, spoil));
		}
	}
	
	private static List<Integer> restrictedIds()
	{
		return Config.RESTRICTED_MOB_DROPLIST_IDS == null ? List.of() : List.copyOf(Config.RESTRICTED_MOB_DROPLIST_IDS);
	}
	
	private Snapshot snapshot()
	{
		final NpcData data = NpcData.getInstance();
		synchronized (data)
		{
			if ((_snapshot == null) || (_snapshot.revision() != data.getRevision()) || !_snapshot.restricted().equals(restrictedIds()))
			{
				load();
			}
			return _snapshot;
		}
	}
	
	/**
	 * Return a per-request copy; callers may sort it without changing the index.
	 * @param itemId
	 * @return
	 */
	public ArrayList<DropInfoHolder> getDrop(int itemId)
	{
		return new ArrayList<>(snapshot().drops().getOrDefault(itemId, List.of()));
	}
	
	public Map<Integer, ArrayList<DropInfoHolder>> getInfo()
	{
		final Map<Integer, ArrayList<DropInfoHolder>> result = new HashMap<>();
		snapshot().drops().forEach((id, drops) -> result.put(id, new ArrayList<>(drops)));
		return result;
	}
	
	public int getDropsCount(int npcId, boolean spoil)
	{
		return (int) snapshot().byNpc().getOrDefault(npcId, List.of()).stream().filter(drop -> drop.isSweep() == spoil).count();
	}
	
	public List<NpcTemplate> searchNpcs(String name)
	{
		final String query = name.toLowerCase(java.util.Locale.ROOT);
		final NpcData data = NpcData.getInstance();
		return snapshot().byNpc().keySet().stream().map(data::getTemplate).filter(java.util.Objects::nonNull).filter(npc -> npc.getName().toLowerCase(java.util.Locale.ROOT).contains(query)).collect(Collectors.toCollection(ArrayList::new));
	}
	
	public static DropInfoHandler getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final DropInfoHandler INSTANCE = new DropInfoHandler();
	}
}
