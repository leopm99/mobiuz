package com.l2journey.gameserver.votesystem.model;

import com.l2journey.gameserver.model.StatSet;

/**
 * @author l2.topgameserver.net
 */
public class Reward
{
	private int _itemId;
	private int _itemCount;
	
	public Reward(StatSet set)
	{
		_itemId = set.getInt("itemId");
		_itemCount = set.getInt("itemCount");
	}
	
	public void setItemId(int itemId)
	{
		_itemId = itemId;
	}
	
	public void setItemCount(int itemCount)
	{
		_itemCount = itemCount;
	}
	
	public int getItemId()
	{
		return _itemId;
	}
	
	public int getItemCount()
	{
		return _itemCount;
	}
}
