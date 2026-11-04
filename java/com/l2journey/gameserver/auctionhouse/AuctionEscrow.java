package com.l2journey.gameserver.auctionhouse;

import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.enums.ItemLocation;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.itemcontainer.ItemContainer;

/** One container per listing: stackable listings must never merge with each other. */
final class AuctionEscrow extends ItemContainer
{
	private final int ownerId;
	private final int listingId;

	AuctionEscrow(int ownerId, int listingId)
	{
		this.ownerId = ownerId;
		this.listingId = listingId;
	}

	@Override public String getName() { return "AuctionHouse"; }
	@Override public Player getOwner() { return null; }
	@Override public int getOwnerId() { return ownerId; }
	@Override public ItemLocation getBaseLocation() { return ItemLocation.AUCTION; }

	@Override
	protected void addItem(Item item)
	{
		super.addItem(item);
		item.setItemLocation(ItemLocation.AUCTION, listingId);
	}

	void restoreItem(Item item)
	{
		// Preserve object ID, count, enchant, elements and augmentation; do not merge stacks.
		super.addItem(item);
	}
}
