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

import java.util.List;
import java.util.Locale;

import com.l2journey.Config;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.itemcontainer.Inventory;

/** Shared calculator math, independent from HTML and board command routing. */
public final class DropInfoFunctions
{
	private DropInfoFunctions() { }

	public record Estimate(long min, long max, double chance) { }
	public record Page<T>(List<T> entries, int number, int total, int count) { }

	public static <T> Page<T> page(List<T> values, int requested, int size)
	{
		if (size <= 0) { throw new IllegalArgumentException("Page size must be positive"); }
		final int pages = values.isEmpty() ? 1 : ((values.size() - 1) / size) + 1;
		final int number = Math.clamp(requested, 1, pages);
		final int start = (number - 1) * size;
		return new Page<>(List.copyOf(values.subList(start, Math.min(values.size(), start + size))), number, pages, values.size());
	}

	/** Nominal per-roll rates for a normal NPC; not a simulation of kills or drop limits. */
	public static Estimate estimate(DropInfoHolder drop, Player player)
	{
		final boolean premium = Config.PREMIUM_SYSTEM_ENABLED && player.hasPremiumStatus();
		final boolean raid = "RaidBoss".equals(drop.npc().getType()) || "GrandBoss".equals(drop.npc().getType());
		double chance;
		double amount;
		if (drop.isSpoil())
		{
			chance = Config.RATE_SPOIL_DROP_CHANCE_MULTIPLIER * (premium ? Config.PREMIUM_RATE_SPOIL_CHANCE : 1) * player.getStat().getBonusSpoilRateMultiplier();
			amount = Config.RATE_SPOIL_DROP_AMOUNT_MULTIPLIER * (premium ? Config.PREMIUM_RATE_SPOIL_AMOUNT : 1);
		}
		else
		{
			final ItemTemplate item = ItemData.getInstance().getTemplate(drop.itemId());
			final boolean herb = (item != null) && item.hasExImmediateEffect();
			final Float byId = Config.RATE_DROP_CHANCE_BY_ID.get(drop.itemId());
			chance = byId != null ? byId : herb ? Config.RATE_HERB_DROP_CHANCE_MULTIPLIER : raid ? Config.RATE_RAID_DROP_CHANCE_MULTIPLIER : Config.RATE_DEATH_DROP_CHANCE_MULTIPLIER;
			if ((byId != null) && (drop.itemId() == Inventory.ADENA_ID)) { chance = Math.min(100, chance); }
			amount = Config.RATE_DROP_AMOUNT_BY_ID.getOrDefault(drop.itemId(), (float) (herb ? Config.RATE_HERB_DROP_AMOUNT_MULTIPLIER : raid ? Config.RATE_RAID_DROP_AMOUNT_MULTIPLIER : Config.RATE_DEATH_DROP_AMOUNT_MULTIPLIER));
			if (premium)
			{
				chance *= Config.PREMIUM_RATE_DROP_CHANCE_BY_ID.getOrDefault(drop.itemId(), (float) (herb || raid ? 1 : Config.PREMIUM_RATE_DROP_CHANCE));
				amount *= Config.PREMIUM_RATE_DROP_AMOUNT_BY_ID.getOrDefault(drop.itemId(), (float) (herb || raid ? 1 : Config.PREMIUM_RATE_DROP_AMOUNT));
			}
			chance *= player.getStat().getBonusDropRateMultiplier();
			amount *= player.getStat().getBonusDropAmountMultiplier();
			if (drop.itemId() == Inventory.ADENA_ID) { amount *= player.getStat().getBonusDropAdenaMultiplier(); }
		}
		return new Estimate((long) (drop.min() * Math.max(0, amount)), (long) (drop.max() * Math.max(0, amount)), Math.clamp(drop.chance() * chance, 0, 100));
	}

	public static String percent(double value)
	{
		if ((value > 0) && (value < 0.000001))
		{
			return java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString() + "%";
		}
		return String.format(Locale.ROOT, "%.6f", value).replaceAll("0+$", "").replaceAll("\\.$", "") + "%";
	}

	public static String escape(String value)
	{
		return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
	}
}
