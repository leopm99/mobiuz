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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntFunction;

import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.data.xml.NpcData;
import com.l2journey.gameserver.model.actor.enums.npc.DropType;
import com.l2journey.gameserver.model.actor.holders.npc.DropGroupHolder;
import com.l2journey.gameserver.model.actor.holders.npc.DropHolder;
import com.l2journey.gameserver.model.actor.templates.NpcTemplate;
import com.l2journey.gameserver.model.item.ItemTemplate;

/** Immutable reverse and forward drop indexes, rebuilt after NPC data reloads. */
public class DropInfoHandler
{
	private record Snapshot(Map<Integer, List<DropInfoHolder>> items, Map<Integer, List<DropInfoHolder>> npcs, List<NpcTemplate> templates, long revision) { }
	private volatile Snapshot _snapshot;
	private final boolean _live;

	private DropInfoHandler()
	{
		_live = true;
	}

	public DropInfoHandler(Collection<NpcTemplate> templates, IntFunction<ItemTemplate> items)
	{
		_live = false;
		_snapshot = build(templates, items, 0);
	}

	private Snapshot snapshot()
	{
		if (_live)
		{
			final NpcData data = NpcData.getInstance();
			if ((_snapshot == null) || (_snapshot.revision() != data.getRevision()))
			{
				synchronized (data)
				{
					if ((_snapshot == null) || (_snapshot.revision() != data.getRevision()))
					{
						_snapshot = build(data.getTemplates(n -> true), ItemData.getInstance()::getTemplate, data.getRevision());
					}
				}
			}
		}
		return _snapshot;
	}

	private static Snapshot build(Collection<NpcTemplate> templates, IntFunction<ItemTemplate> items, long revision)
	{
		final Map<Integer, List<DropInfoHolder>> byItem = new HashMap<>();
		final Map<Integer, List<DropInfoHolder>> byNpc = new HashMap<>();
		for (NpcTemplate npc : templates)
		{
			final List<DropInfoHolder> drops = new ArrayList<>();
			if (npc.getDropGroups() != null)
			{
				int number = 0;
				for (DropGroupHolder group : npc.getDropGroups())
				{
					number++;
					for (DropHolder drop : group.getDropList())
					{
						add(drops, npc, drop, DropType.DROP, group.getChance() / 100d, number, items);
					}
				}
			}
			if (npc.getDropList() != null)
			{
				for (DropHolder drop : npc.getDropList()) { add(drops, npc, drop, DropType.DROP, 1, 0, items); }
			}
			if (npc.getSpoilList() != null)
			{
				for (DropHolder drop : npc.getSpoilList()) { add(drops, npc, drop, DropType.SPOIL, 1, 0, items); }
			}
			byNpc.put(npc.getId(), List.copyOf(drops));
			for (DropInfoHolder drop : drops) { byItem.computeIfAbsent(drop.itemId(), _ -> new ArrayList<>()).add(drop); }
		}
		byItem.replaceAll((_, drops) -> drops.stream().sorted(Comparator.comparingInt((DropInfoHolder d) -> d.npc().getLevel()).thenComparingInt(d -> d.npc().getId())).toList());
		return new Snapshot(Map.copyOf(byItem), Map.copyOf(byNpc), List.copyOf(templates), revision);
	}

	private static void add(List<DropInfoHolder> result, NpcTemplate npc, DropHolder drop, DropType type, double groupChance, int group, IntFunction<ItemTemplate> items)
	{
		final ItemTemplate item = items.apply(drop.getItemId());
		// Use the actual herb marker instead of hardcoded ID ranges from another chronicle.
		if ((item != null) && !item.hasExImmediateEffect())
		{
			result.add(new DropInfoHolder(npc, drop.getItemId(), type, drop.getMin(), drop.getMax(), drop.getChance() * groupChance, group));
		}
	}

	public List<DropInfoHolder> getDrop(int itemId)
	{
		return snapshot().items().getOrDefault(itemId, List.of());
	}

	public List<DropInfoHolder> getNpcDrops(int npcId)
	{
		return snapshot().npcs().getOrDefault(npcId, List.of());
	}

	public List<ItemTemplate> searchItems(String query, int sort)
	{
		final String needle = query.trim().toLowerCase(Locale.ROOT);
		final List<ItemTemplate> found = new ArrayList<>();
		for (int id : snapshot().items().keySet())
		{
			final ItemTemplate item = ItemData.getInstance().getTemplate(id);
			if ((item != null) && (Integer.toString(id).equals(needle) || item.getName().toLowerCase(Locale.ROOT).contains(needle))) { found.add(item); }
		}
		final Comparator<ItemTemplate> names = Comparator.comparing(ItemTemplate::getName, String.CASE_INSENSITIVE_ORDER).thenComparingInt(ItemTemplate::getId);
		final Comparator<ItemTemplate> order = switch (sort)
		{
			case 1 -> Comparator.comparingLong((ItemTemplate i) -> getDrop(i.getId()).stream().filter(d -> !d.isSpoil()).count()).reversed().thenComparing(names);
			case 2 -> Comparator.comparingLong((ItemTemplate i) -> getDrop(i.getId()).stream().filter(DropInfoHolder::isSpoil).count()).reversed().thenComparing(names);
			default -> names;
		};
		return found.stream().sorted(order).toList();
	}

	public List<NpcTemplate> searchNpcs(String query, int sort)
	{
		final String needle = query.trim().toLowerCase(Locale.ROOT);
		final Comparator<NpcTemplate> names = Comparator.comparing(NpcTemplate::getName, String.CASE_INSENSITIVE_ORDER).thenComparingInt(NpcTemplate::getId);
		final Comparator<NpcTemplate> order = switch (sort)
		{
			case 1 -> Comparator.comparingInt((NpcTemplate n) -> n.getLevel()).reversed().thenComparing(names);
			case 2 -> Comparator.comparingInt((NpcTemplate n) -> getNpcDrops(n.getId()).size()).reversed().thenComparing(names);
			default -> names;
		};
		return snapshot().templates().stream().filter(n -> Integer.toString(n.getId()).equals(needle) || n.getName().toLowerCase(Locale.ROOT).contains(needle)).sorted(order).toList();
	}

	public static DropInfoHandler getInstance() { return SingletonHolder.INSTANCE; }
	private static class SingletonHolder { private static final DropInfoHandler INSTANCE = new DropInfoHandler(); }
}
