package com.l2journey.gameserver.votesystem.Enum;

/** Stable IDs shared by the XML, bypasses and database. ID 11 is reserved for global rewards. */
public enum VoteSite
{
	L2TOPGAMESERVER(0), ITOPZ(1), L2TOPCO(2), L2VOTES(3), HOPZONE(4),
	L2NETWORK(5), L2TOPSERVERS(6), TOPL2JBRASIL(7), MMOTOP(8), TOPZONE(9), L2SERVERS(10),
	HOPZONE_EU(12), HOTSERVERS(13), L2RANKZONE(14), TOP4TEAMBR(15);

	private final int _id;
	VoteSite(int id) { _id = id; }
	public int getId() { return _id; }
	public static int slotCount() { return 16; }
	public static VoteSite fromId(int id)
	{
		for (VoteSite site : values()) { if (site._id == id) { return site; } }
		return null;
	}
}
