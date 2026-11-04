package com.l2journey.gameserver.votesystem.model;

/**
 * @author l2.topgameserver.net
 */
public class IndividualVoteResponse
{
	private boolean _isVoted;
	private long _diffTime;
	private long _voteSiteTime;
	
	public IndividualVoteResponse()
	{
		
	}
	
	public void setIsVoted(boolean isVoted)
	{
		_isVoted = isVoted;
	}
	
	public void setDiffTime(long diffTime)
	{
		_diffTime = diffTime;
	}
	
	public void setVoteSiteTime(long voteSiteTime)
	{
		_voteSiteTime = voteSiteTime;
	}
	
	public boolean getIsVoted()
	{
		return _isVoted;
	}
	
	public long getDiffTime()
	{
		return _diffTime;
	}
	
	public long getVoteSiteTime()
	{
		return _voteSiteTime;
	}
}
