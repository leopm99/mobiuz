package com.l2journey.gameserver.votesystem.model;

/**
 * @author l2.topgameserver.net
 */
public class GlobalVote
{
	private int _voteSite;
	private int _votesLastReward;
	private int _currentVotes;
	
	public GlobalVote()
	{
		
	}
	
	public GlobalVote(int voteSite, int votesLastReward)
	{
		_voteSite = voteSite;
		_votesLastReward = votesLastReward;
	}
	
	public void setVoteSite(int voteSite)
	{
		_voteSite = voteSite;
	}
	
	public void setVotesLastReward(int votesLastReward)
	{
		_votesLastReward = votesLastReward;
	}
	
	public void setCurrentVotes(int currentVotes)
	{
		_currentVotes = currentVotes;
	}
	
	public int getVoyeSite()
	{
		return _voteSite;
	}
	
	public int getVotesLastReward()
	{
		return _votesLastReward;
	}
	
	public int getCurrentVotes()
	{
		return _currentVotes;
	}
	
}
