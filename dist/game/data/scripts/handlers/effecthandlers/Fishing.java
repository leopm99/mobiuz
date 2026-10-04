/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.effecthandlers;

import com.l2journey.Config;
import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.GeoData;
import com.l2journey.gameserver.managers.ZoneManager;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.StatSet;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.conditions.Condition;
import com.l2journey.gameserver.model.effects.AbstractEffect;
import com.l2journey.gameserver.model.effects.EffectType;
import com.l2journey.gameserver.model.item.Weapon;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.item.type.EtcItemType;
import com.l2journey.gameserver.model.item.type.WeaponType;
import com.l2journey.gameserver.model.itemcontainer.Inventory;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.zone.ZoneId;
import com.l2journey.gameserver.model.zone.ZoneType;
import com.l2journey.gameserver.model.zone.type.FishingZone;
import com.l2journey.gameserver.model.zone.type.WaterZone;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.util.LocationUtil;

/**
 * Fishing effect implementation.
 * @author UnAfraid
 */
public class Fishing extends AbstractEffect
{
	private static final int MIN_BAIT_DISTANCE = 90;
	private static final int MAX_BAIT_DISTANCE = 250;
	
	public Fishing(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.FISHING_START;
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill)
	{
		if (!effector.isPlayer())
		{
			return;
		}
		
		final Player player = effector.asPlayer();
		if (!Config.ALLOW_FISHING && !player.isGM())
		{
			player.sendMessage("Fishing is disabled!");
			return;
		}
		
		if (player.isFishing())
		{
			if (player.getFishCombat() != null)
			{
				player.getFishCombat().doDie(false);
			}
			else
			{
				player.endFishing(false);
			}
			
			player.sendPacket(SystemMessageId.YOUR_ATTEMPT_AT_FISHING_HAS_BEEN_CANCELLED);
			return;
		}
		
		// check for equipped fishing rod
		final Weapon equipedWeapon = player.getActiveWeaponItem();
		if (((equipedWeapon == null) || (equipedWeapon.getItemType() != WeaponType.FISHINGROD)))
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_A_FISHING_POLE_EQUIPPED);
			return;
		}
		
		// check for equipped lure
		final Item equipedLeftHand = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if ((equipedLeftHand == null) || (equipedLeftHand.getItemType() != EtcItemType.LURE))
		{
			player.sendPacket(SystemMessageId.YOU_MUST_PUT_BAIT_ON_YOUR_HOOK_BEFORE_YOU_CAN_FISH);
			return;
		}
		
		if (player.isInBoat())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_FISH_WHILE_RIDING_AS_A_PASSENGER_OF_A_BOAT_IT_S_AGAINST_THE_RULES);
			return;
		}
		
		if (player.isCrafting() || player.isInStoreMode())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_FISH_WHILE_USING_A_RECIPE_BOOK_PRIVATE_MANUFACTURE_OR_PRIVATE_STORE);
			return;
		}
		
		if (player.isInsideZone(ZoneId.WATER))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_FISH_WHILE_UNDER_WATER);
			return;
		}
		
		// calculate a position in front of the player with a random distance
		int distance = Rnd.get(MIN_BAIT_DISTANCE, MAX_BAIT_DISTANCE);
		final double angle = LocationUtil.convertHeadingToDegree(player.getHeading());
		final double radian = Math.toRadians(angle);
		final double sin = Math.sin(radian);
		final double cos = Math.cos(radian);
		int baitX = (int) (player.getX() + (cos * distance));
		int baitY = (int) (player.getY() + (sin * distance));
		
		// search for fishing and water zone
		FishingZone fishingZone = null;
		WaterZone waterZone = null;
		for (ZoneType zone : ZoneManager.getInstance().getZones(baitX, baitY))
		{
			if (zone instanceof FishingZone)
			{
				fishingZone = (FishingZone) zone;
			}
			else if (zone instanceof WaterZone)
			{
				waterZone = (WaterZone) zone;
			}
			
			if ((fishingZone != null) && (waterZone != null))
			{
				break;
			}
		}
		
		int baitZ = computeBaitZ(player, baitX, baitY, fishingZone, waterZone);
		if (baitZ == Integer.MIN_VALUE)
		{
			for (distance = MAX_BAIT_DISTANCE; distance >= MIN_BAIT_DISTANCE; --distance)
			{
				baitX = (int) (player.getX() + (cos * distance));
				baitY = (int) (player.getY() + (sin * distance));
				
				// search for fishing and water zone again
				fishingZone = null;
				waterZone = null;
				for (ZoneType zone : ZoneManager.getInstance().getZones(baitX, baitY))
				{
					if (zone instanceof FishingZone)
					{
						fishingZone = (FishingZone) zone;
					}
					else if (zone instanceof WaterZone)
					{
						waterZone = (WaterZone) zone;
					}
					
					if ((fishingZone != null) && (waterZone != null))
					{
						break;
					}
				}
				
				baitZ = computeBaitZ(player, baitX, baitY, fishingZone, waterZone);
				if (baitZ != Integer.MIN_VALUE)
				{
					break;
				}
			}
			
			if (baitZ == Integer.MIN_VALUE)
			{
				player.sendPacket(SystemMessageId.YOU_CAN_T_FISH_HERE);
				return;
			}
		}
		
		if (!player.destroyItem(ItemProcessType.NONE, equipedLeftHand, 1, null, false))
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_BAIT);
			return;
		}
		
		player.setLure(equipedLeftHand);
		player.startFishing(baitX, baitY, baitZ);
	}
	
	/**
	 * Computes the Z of the bait.
	 * @param player the player
	 * @param baitX the bait x
	 * @param baitY the bait y
	 * @param fishingZone the fishing zone
	 * @param waterZone the water zone
	 * @return the bait z or {@link Integer#MIN_VALUE} when you cannot fish here
	 */
	private static int computeBaitZ(Player player, int baitX, int baitY, FishingZone fishingZone, WaterZone waterZone)
	{
		if ((fishingZone == null) || (waterZone == null))
		{
			return Integer.MIN_VALUE;
		}
		
		// always use water zone, fishing zone high z is high in the air...
		final int baitZ = waterZone.getWaterZ();
		if (!GeoData.getInstance().canSeeTarget(player, new Location(baitX, baitY, baitZ)))
		{
			return Integer.MIN_VALUE;
		}
		
		if ((GeoData.getInstance().hasGeo(baitX, baitY)) && ((GeoData.getInstance().getHeight(baitX, baitY, baitZ) > baitZ) || (GeoData.getInstance().getHeight(baitX, baitY, player.getZ()) > baitZ)))
		{
			return Integer.MIN_VALUE;
		}
		
		return baitZ;
	}
}
