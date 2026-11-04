package com.l2journey.gameserver.votesystem.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author l2.topgameserver.net
 */
public class VoteSite
{
	private int _siteOrdinal;
	private String _siteName;
	private final List<Reward> _rewards = new ArrayList<>();
	
	public VoteSite()
	{
		
	}
	
	public void setSiteOrdinal(int siteOrdinal)
	{
		_siteOrdinal = siteOrdinal;
	}
	
	public void setSiteName(String siteName)
	{
		_siteName = siteName;
	}
	
	public void setRewardList(List<Reward> rewards)
	{
		for (Reward r : rewards)
		{
			_rewards.add(r);
		}
	}
	
	public int getSiteOrdinal()
	{
		return _siteOrdinal;
	}
	
	public String getSiteName()
	{
		return _siteName;
	}
	
	public List<Reward> getRewardList()
	{
		return _rewards;
	}
	
}
