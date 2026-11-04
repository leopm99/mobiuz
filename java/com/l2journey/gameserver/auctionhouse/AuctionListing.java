package com.l2journey.gameserver.auctionhouse;

/**
 * Immutable UI snapshot; no mutable inventory objects escape the manager.
 * @param id
 * @param ownerId
 * @param objectId
 * @param itemId
 * @param name
 * @param icon
 * @param category
 * @param grade
 * @param enchant
 * @param quantity
 * @param unitPrice
 * @param expiresAt
 */
public record AuctionListing(int id, int ownerId, int objectId, int itemId, String name, String icon, String category, String grade, int enchant, long quantity, long unitPrice, long expiresAt)
{
	public String displayName()
	{
		return (enchant > 0 ? "+" + enchant + " " : "") + name;
	}
}
