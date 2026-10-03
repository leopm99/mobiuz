/*
 * Copyright (c) 2025 L2Journey Project
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 * ---
 * 
 * Portions of this software are derived from the L2JMobius Project, 
 * shared under the MIT License. The original license terms are preserved where 
 * applicable..
 * 
 */
package com.l2journey.gameserver.model.olympiad;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.l2journey.EventsConfig;
import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.events.EventDispatcher;
import com.l2journey.gameserver.model.events.EventType;
import com.l2journey.gameserver.model.events.holders.olympiad.OnOlympiadMatchResult;
import com.l2journey.gameserver.model.zone.type.OlympiadStadiumZone;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.ExOlympiadMatchResult;
import com.l2journey.gameserver.network.serverpackets.ExOlympiadUserInfo;
import com.l2journey.gameserver.network.serverpackets.ServerPacket;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;

/**
 * @author GodKratos, Pere, DS
 */
public abstract class OlympiadGameNormal extends AbstractOlympiadGame
{
	protected int _damageP1 = 0;
	protected int _damageP2 = 0;
	
	protected Participant _playerOne;
	protected Participant _playerTwo;
	
	protected OlympiadGameNormal(int id, Participant[] opponents)
	{
		super(id);
		
		_playerOne = opponents[0];
		_playerTwo = opponents[1];
		
		_playerOne.getPlayer().setOlympiadGameId(id);
		_playerTwo.getPlayer().setOlympiadGameId(id);
	}
	
	protected static Participant[] createListOfParticipants(List<Integer> list)
	{
		if ((list == null) || list.isEmpty() || (list.size() < 2))
		{
			return null;
		}
		
		int playerOneObjectId = 0;
		Player playerOne = null;
		Player playerTwo = null;
		
		while (list.size() > 1)
		{
			playerOneObjectId = list.remove(Rnd.get(list.size()));
			playerOne = World.getInstance().getPlayer(playerOneObjectId);
			if ((playerOne == null) || !playerOne.isOnline())
			{
				continue;
			}
			
			playerTwo = World.getInstance().getPlayer(list.remove(Rnd.get(list.size())));
			if ((playerTwo == null) || !playerTwo.isOnline())
			{
				list.add(playerOneObjectId);
				continue;
			}
			
			final Participant[] result = new Participant[2];
			result[0] = new Participant(playerOne, 1);
			result[1] = new Participant(playerTwo, 2);
			
			return result;
		}
		return null;
	}
	
	@Override
	public boolean containsParticipant(int playerId)
	{
		return ((_playerOne != null) && (_playerOne.getObjectId() == playerId)) || ((_playerTwo != null) && (_playerTwo.getObjectId() == playerId));
	}
	
	@Override
	public void sendOlympiadInfo(Creature creature)
	{
		creature.sendPacket(new ExOlympiadUserInfo(_playerOne));
		creature.sendPacket(new ExOlympiadUserInfo(_playerTwo));
	}
	
	@Override
	public void broadcastOlympiadInfo(OlympiadStadiumZone stadium)
	{
		stadium.broadcastPacket(new ExOlympiadUserInfo(_playerOne));
		stadium.broadcastPacket(new ExOlympiadUserInfo(_playerTwo));
	}
	
	@Override
	protected void broadcastPacket(ServerPacket packet)
	{
		if (_playerOne.updatePlayer())
		{
			_playerOne.getPlayer().sendPacket(packet);
		}
		
		if (_playerTwo.updatePlayer())
		{
			_playerTwo.getPlayer().sendPacket(packet);
		}
	}
	
	@Override
	protected final boolean portPlayersToArena(List<Location> spawns)
	{
		boolean result = true;
		try
		{
			result &= portPlayerToArena(_playerOne, spawns.get(0), _stadiumID);
			result &= portPlayerToArena(_playerTwo, spawns.get(spawns.size() / 2), _stadiumID);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "", e);
			return false;
		}
		return result;
	}
	
	@Override
	protected boolean needBuffers()
	{
		return true;
	}
	
	@Override
	protected void removals()
	{
		if (_aborted)
		{
			return;
		}
		
		removals(_playerOne.getPlayer(), true);
		removals(_playerTwo.getPlayer(), true);
	}
	
	@Override
	protected final boolean makeCompetitionStart()
	{
		if (!super.makeCompetitionStart() || (_playerOne.getPlayer() == null) || (_playerTwo.getPlayer() == null))
		{
			return false;
		}
		
		_playerOne.getPlayer().setOlympiadStart(true);
		_playerOne.getPlayer().updateEffectIcons();
		_playerTwo.getPlayer().setOlympiadStart(true);
		_playerTwo.getPlayer().updateEffectIcons();
		return true;
	}
	
	@Override
	protected void cleanEffects()
	{
		if ((_playerOne.getPlayer() != null) && !_playerOne.isDefaulted() && !_playerOne.isDisconnected() && (_playerOne.getPlayer().getOlympiadGameId() == _stadiumID))
		{
			cleanEffects(_playerOne.getPlayer());
		}
		
		if ((_playerTwo.getPlayer() != null) && !_playerTwo.isDefaulted() && !_playerTwo.isDisconnected() && (_playerTwo.getPlayer().getOlympiadGameId() == _stadiumID))
		{
			cleanEffects(_playerTwo.getPlayer());
		}
	}
	
	@Override
	protected void portPlayersBack()
	{
		if ((_playerOne.getPlayer() != null) && !_playerOne.isDefaulted() && !_playerOne.isDisconnected())
		{
			portPlayerBack(_playerOne.getPlayer());
		}
		if ((_playerTwo.getPlayer() != null) && !_playerTwo.isDefaulted() && !_playerTwo.isDisconnected())
		{
			portPlayerBack(_playerTwo.getPlayer());
		}
	}
	
	@Override
	protected void playersStatusBack()
	{
		if ((_playerOne.getPlayer() != null) && !_playerOne.isDefaulted() && !_playerOne.isDisconnected() && (_playerOne.getPlayer().getOlympiadGameId() == _stadiumID))
		{
			playerStatusBack(_playerOne.getPlayer());
		}
		
		if ((_playerTwo.getPlayer() != null) && !_playerTwo.isDefaulted() && !_playerTwo.isDisconnected() && (_playerTwo.getPlayer().getOlympiadGameId() == _stadiumID))
		{
			playerStatusBack(_playerTwo.getPlayer());
		}
	}
	
	@Override
	protected void clearPlayers()
	{
		_playerOne.setPlayer(null);
		_playerOne = null;
		_playerTwo.setPlayer(null);
		_playerTwo = null;
	}
	
	@Override
	protected void handleDisconnect(Player player)
	{
		if (player.getObjectId() == _playerOne.getObjectId())
		{
			_playerOne.setDisconnected(true);
		}
		else if (player.getObjectId() == _playerTwo.getObjectId())
		{
			_playerTwo.setDisconnected(true);
		}
	}
	
	@Override
	protected final boolean checkBattleStatus()
	{
		if (_aborted || (_playerOne.getPlayer() == null) || _playerOne.isDisconnected())
		{
			return false;
		}
		
		if ((_playerTwo.getPlayer() == null) || _playerTwo.isDisconnected())
		{
			return false;
		}
		
		return true;
	}
	
	@Override
	protected final boolean haveWinner()
	{
		if (!checkBattleStatus())
		{
			return true;
		}
		
		boolean playerOneLost = true;
		try
		{
			if (_playerOne.getPlayer().getOlympiadGameId() == _stadiumID)
			{
				playerOneLost = _playerOne.getPlayer().isDead();
			}
		}
		catch (Exception _)
		{
			playerOneLost = true;
		}
		
		boolean playerTwoLost = true;
		try
		{
			if (_playerTwo.getPlayer().getOlympiadGameId() == _stadiumID)
			{
				playerTwoLost = _playerTwo.getPlayer().isDead();
			}
		}
		catch (Exception _)
		{
			playerTwoLost = true;
		}
		
		return playerOneLost || playerTwoLost;
	}
	
	@Override
	protected void validateWinner(OlympiadStadiumZone stadium)
	{
		if (_aborted)
		{
			return;
		}
		
		final int nobodyWon = 0;
		final int teamOneWon = 1;
		final int teamTwoWon = 2;
		
		final List<OlympiadInfo> winnersList = new ArrayList<>(1);
		final List<OlympiadInfo> losersList = new ArrayList<>(1);
		
		final int playerOnePoints = _playerOne.getStats().getInt(POINTS);
		final int playerTwoPoints = _playerTwo.getStats().getInt(POINTS);
		
		// Create results for when the players defaulted before the battle started.
		if (_playerOne.isDefaulted() || _playerTwo.isDefaulted())
		{
			try
			{
				int winningSide = nobodyWon;
				final boolean isTie = _playerOne.isDefaulted() && _playerTwo.isDefaulted();
				
				if (_playerOne.isDefaulted())
				{
					try
					{
						final int points = Math.min(playerOnePoints / 3, EventsConfig.OLYMPIAD_MAX_POINTS);
						removePointsFromParticipant(_playerOne, points);
						
						if (!isTie)
						{
							winningSide = teamTwoWon;
							winnersList.add(new OlympiadInfo(_playerOne.getName(), _playerOne.getClanName(), _playerOne.getClanId(), _playerOne.getBaseClass(), _damageP1, playerOnePoints - points, -points));
						}
						
						if (EventsConfig.OLYMPIAD_LOG_FIGHTS)
						{
							LOGGER_OLYMPIAD.info(_playerOne.getName() + " default," + _playerOne + "," + _playerTwo + ",0,0,0,0," + points + "," + getType());
						}
					}
					catch (Exception e)
					{
						LOGGER.log(Level.WARNING, "Exception on validateWinner(): " + e.getMessage(), e);
					}
				}
				
				if (_playerTwo.isDefaulted())
				{
					try
					{
						final int points = Math.min(playerTwoPoints / 3, EventsConfig.OLYMPIAD_MAX_POINTS);
						removePointsFromParticipant(_playerTwo, points);
						
						if (!isTie)
						{
							winningSide = teamOneWon;
							winnersList.add(new OlympiadInfo(_playerTwo.getName(), _playerTwo.getClanName(), _playerTwo.getClanId(), _playerTwo.getBaseClass(), _damageP2, playerTwoPoints - points, -points));
						}
						
						if (EventsConfig.OLYMPIAD_LOG_FIGHTS)
						{
							LOGGER_OLYMPIAD.info(_playerTwo.getName() + " default," + _playerOne + "," + _playerTwo + ",0,0,0,0," + points + "," + getType());
						}
					}
					catch (Exception e)
					{
						LOGGER.log(Level.WARNING, "Exception on validateWinner(): " + e.getMessage(), e);
					}
				}
				
				final ExOlympiadMatchResult result = new ExOlympiadMatchResult(isTie, winningSide, winnersList, losersList);
				stadium.broadcastPacket(result);
				return;
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Exception on validateWinner(): " + e.getMessage(), e);
				return;
			}
		}
		
		final boolean hasPlayerOneCrashed = (_playerOne.getPlayer() == null) || _playerOne.isDisconnected();
		final boolean hasPlayerTwoCrashed = (_playerTwo.getPlayer() == null) || _playerTwo.isDisconnected();
		
		int pointDiff = Math.min(playerOnePoints, playerTwoPoints) / getDivider();
		if (pointDiff <= 0)
		{
			pointDiff = 1;
		}
		else if (pointDiff > EventsConfig.OLYMPIAD_MAX_POINTS)
		{
			pointDiff = EventsConfig.OLYMPIAD_MAX_POINTS;
		}
		
		// Create results for when the player's client's crashed.
		if (hasPlayerOneCrashed || hasPlayerTwoCrashed)
		{
			try
			{
				boolean isTie = false;
				int winningSide = nobodyWon;
				
				if (hasPlayerTwoCrashed && !hasPlayerOneCrashed)
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.CONGRATULATIONS_C1_YOU_WIN_THE_MATCH);
					sm.addString(_playerOne.getName());
					stadium.broadcastPacket(sm);
					
					_playerOne.updateStat(COMP_WON, 1);
					addPointsToParticipant(_playerOne, pointDiff);
					winnersList.add(new OlympiadInfo(_playerOne.getName(), _playerOne.getClanName(), _playerOne.getClanId(), _playerOne.getBaseClass(), _damageP1, playerOnePoints + pointDiff, pointDiff));
					
					_playerTwo.updateStat(COMP_LOST, 1);
					removePointsFromParticipant(_playerTwo, pointDiff);
					losersList.add(new OlympiadInfo(_playerTwo.getName(), _playerTwo.getClanName(), _playerTwo.getClanId(), _playerTwo.getBaseClass(), _damageP2, playerTwoPoints - pointDiff, -pointDiff));
					
					winningSide = teamOneWon;
					rewardParticipant(_playerOne.getPlayer(), getReward());
					
					if (EventsConfig.OLYMPIAD_LOG_FIGHTS)
					{
						LOGGER_OLYMPIAD.info(_playerTwo.getName() + " crash," + _playerOne + "," + _playerTwo + ",0,0,0,0," + pointDiff + "," + getType());
					}
					
					// Notify match result to event listener scripts.
					if (EventDispatcher.getInstance().hasListener(EventType.ON_OLYMPIAD_MATCH_RESULT, Olympiad.getInstance()))
					{
						EventDispatcher.getInstance().notifyEventAsync(new OnOlympiadMatchResult(_playerOne, _playerTwo, getType()), Olympiad.getInstance());
					}
				}
				else if (hasPlayerOneCrashed && !hasPlayerTwoCrashed)
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.CONGRATULATIONS_C1_YOU_WIN_THE_MATCH);
					sm.addString(_playerTwo.getName());
					stadium.broadcastPacket(sm);
					
					_playerTwo.updateStat(COMP_WON, 1);
					addPointsToParticipant(_playerTwo, pointDiff);
					winnersList.add(new OlympiadInfo(_playerTwo.getName(), _playerTwo.getClanName(), _playerTwo.getClanId(), _playerTwo.getBaseClass(), _damageP2, playerTwoPoints + pointDiff, pointDiff));
					
					_playerOne.updateStat(COMP_LOST, 1);
					removePointsFromParticipant(_playerOne, pointDiff);
					losersList.add(new OlympiadInfo(_playerOne.getName(), _playerOne.getClanName(), _playerOne.getClanId(), _playerOne.getBaseClass(), _damageP1, playerOnePoints - pointDiff, -pointDiff));
					
					winningSide = teamTwoWon;
					rewardParticipant(_playerTwo.getPlayer(), getReward());
					
					if (EventsConfig.OLYMPIAD_LOG_FIGHTS)
					{
						LOGGER_OLYMPIAD.info(_playerOne.getName() + " crash," + _playerOne + "," + _playerTwo + ",0,0,0,0," + pointDiff + "," + getType());
					}
					
					// Notify match result to event listener scripts.
					if (EventDispatcher.getInstance().hasListener(EventType.ON_OLYMPIAD_MATCH_RESULT, Olympiad.getInstance()))
					{
						EventDispatcher.getInstance().notifyEventAsync(new OnOlympiadMatchResult(_playerTwo, _playerOne, getType()), Olympiad.getInstance());
					}
				}
				else if (hasPlayerOneCrashed && hasPlayerTwoCrashed)
				{
					isTie = true;
					stadium.broadcastPacket(new SystemMessage(SystemMessageId.THERE_IS_NO_VICTOR_THE_MATCH_ENDS_IN_A_TIE));
					
					_playerOne.updateStat(COMP_LOST, 1);
					removePointsFromParticipant(_playerOne, pointDiff);
					losersList.add(new OlympiadInfo(_playerOne.getName(), _playerOne.getClanName(), _playerOne.getClanId(), _playerOne.getBaseClass(), _damageP1, playerOnePoints - pointDiff, -pointDiff));
					
					_playerTwo.updateStat(COMP_LOST, 1);
					removePointsFromParticipant(_playerTwo, pointDiff);
					losersList.add(new OlympiadInfo(_playerTwo.getName(), _playerTwo.getClanName(), _playerTwo.getClanId(), _playerTwo.getBaseClass(), _damageP2, playerTwoPoints - pointDiff, -pointDiff));
					
					if (EventsConfig.OLYMPIAD_LOG_FIGHTS)
					{
						LOGGER_OLYMPIAD.info("both crash," + _playerOne.getName() + "," + _playerOne + ",0,0,0,0," + _playerTwo + "," + pointDiff + "," + getType());
					}
				}
				
				_playerOne.updateStat(COMP_DONE, 1);
				_playerTwo.updateStat(COMP_DONE, 1);
				_playerOne.updateStat(COMP_DONE_WEEK, 1);
				_playerTwo.updateStat(COMP_DONE_WEEK, 1);
				_playerOne.updateStat(getWeeklyMatchType(), 1);
				_playerTwo.updateStat(getWeeklyMatchType(), 1);
				
				final ExOlympiadMatchResult result = new ExOlympiadMatchResult(isTie, winningSide, winnersList, losersList);
				stadium.broadcastPacket(result);
				
				// Notify match result to event listener scripts.
				if (EventDispatcher.getInstance().hasListener(EventType.ON_OLYMPIAD_MATCH_RESULT, Olympiad.getInstance()))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnOlympiadMatchResult(null, _playerOne, getType()), Olympiad.getInstance());
					EventDispatcher.getInstance().notifyEventAsync(new OnOlympiadMatchResult(null, _playerTwo, getType()), Olympiad.getInstance());
				}
				return;
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Exception on validateWinner(): " + e.getMessage(), e);
				return;
			}
		}
		
		// Create results for all other contexts.
		try
		{
			String winner = "draw";
			int winningSide = nobodyWon;
			boolean isTie = false;
			
			// Calculate Fight time.
			final long _fightTime = System.currentTimeMillis() - _startTime;
			
			double playerOneHp = 0;
			if ((_playerOne.getPlayer() != null) && !_playerOne.getPlayer().isDead())
			{
				playerOneHp = _playerOne.getPlayer().getCurrentHp() + _playerOne.getPlayer().getCurrentCp();
				if (playerOneHp < 0.5)
				{
					playerOneHp = 0;
				}
			}
			
			double playerTwoHp = 0;
			if ((_playerTwo.getPlayer() != null) && !_playerTwo.getPlayer().isDead())
			{
				playerTwoHp = _playerTwo.getPlayer().getCurrentHp() + _playerTwo.getPlayer().getCurrentCp();
				if (playerTwoHp < 0.5)
				{
					playerTwoHp = 0;
				}
			}
			
			// if players crashed, check if they've relogged.
			_playerOne.updatePlayer();
			_playerTwo.updatePlayer();
			
			final boolean isPlayerOneOffline = (_playerOne.getPlayer() == null) || !_playerOne.getPlayer().isOnline();
			final boolean isPlayerTwoOffline = (_playerTwo.getPlayer() == null) || !_playerTwo.getPlayer().isOnline();
			if (isPlayerOneOffline && isPlayerTwoOffline)
			{
				_playerOne.updateStat(COMP_DRAWN, 1);
				_playerTwo.updateStat(COMP_DRAWN, 1);
				
				losersList.add(new OlympiadInfo(_playerOne.getName(), _playerOne.getClanName(), _playerOne.getClanId(), _playerOne.getBaseClass(), _damageP1, playerOnePoints + pointDiff, pointDiff));
				losersList.add(new OlympiadInfo(_playerTwo.getName(), _playerTwo.getClanName(), _playerTwo.getClanId(), _playerTwo.getBaseClass(), _damageP2, playerTwoPoints - pointDiff, -pointDiff));
				isTie = true;
				
				final SystemMessage sm = new SystemMessage(SystemMessageId.THERE_IS_NO_VICTOR_THE_MATCH_ENDS_IN_A_TIE);
				stadium.broadcastPacket(sm);
			}
			else if (isPlayerTwoOffline || ((playerTwoHp == 0) && (playerOneHp != 0)) || ((_damageP1 > _damageP2) && (playerTwoHp != 0) && (playerOneHp != 0)))
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.CONGRATULATIONS_C1_YOU_WIN_THE_MATCH);
				sm.addString(_playerOne.getName());
				stadium.broadcastPacket(sm);
				
				_playerOne.updateStat(COMP_WON, 1);
				_playerTwo.updateStat(COMP_LOST, 1);
				
				addPointsToParticipant(_playerOne, pointDiff);
				winnersList.add(new OlympiadInfo(_playerOne.getName(), _playerOne.getClanName(), _playerOne.getClanId(), _playerOne.getBaseClass(), _damageP1, playerOnePoints + pointDiff, pointDiff));
				
				removePointsFromParticipant(_playerTwo, pointDiff);
				losersList.add(new OlympiadInfo(_playerTwo.getName(), _playerTwo.getClanName(), _playerTwo.getClanId(), _playerTwo.getBaseClass(), _damageP2, playerTwoPoints - pointDiff, -pointDiff));
				winner = _playerOne.getName() + " won";
				
				winningSide = teamOneWon;
				
				// Save Fight Result.
				saveResults(_playerOne, _playerTwo, 1, _startTime, _fightTime, getType());
				rewardParticipant(_playerOne.getPlayer(), getReward());
				
				// Notify match result to event listener scripts.
				if (EventDispatcher.getInstance().hasListener(EventType.ON_OLYMPIAD_MATCH_RESULT, Olympiad.getInstance()))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnOlympiadMatchResult(_playerOne, _playerTwo, getType()), Olympiad.getInstance());
				}
			}
			else if (isPlayerOneOffline || ((playerOneHp == 0) && (playerTwoHp != 0)) || ((_damageP2 > _damageP1) && (playerOneHp != 0) && (playerTwoHp != 0)))
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.CONGRATULATIONS_C1_YOU_WIN_THE_MATCH);
				sm.addString(_playerTwo.getName());
				stadium.broadcastPacket(sm);
				
				_playerTwo.updateStat(COMP_WON, 1);
				_playerOne.updateStat(COMP_LOST, 1);
				
				addPointsToParticipant(_playerTwo, pointDiff);
				winnersList.add(new OlympiadInfo(_playerTwo.getName(), _playerTwo.getClanName(), _playerTwo.getClanId(), _playerTwo.getBaseClass(), _damageP2, playerTwoPoints + pointDiff, pointDiff));
				
				removePointsFromParticipant(_playerOne, pointDiff);
				losersList.add(new OlympiadInfo(_playerOne.getName(), _playerOne.getClanName(), _playerOne.getClanId(), _playerOne.getBaseClass(), _damageP1, playerOnePoints - pointDiff, -pointDiff));
				
				winner = _playerTwo.getName() + " won";
				winningSide = teamTwoWon;
				
				// Save Fight Result.
				saveResults(_playerOne, _playerTwo, 2, _startTime, _fightTime, getType());
				rewardParticipant(_playerTwo.getPlayer(), getReward());
				
				// Notify match result to event listener scripts.
				if (EventDispatcher.getInstance().hasListener(EventType.ON_OLYMPIAD_MATCH_RESULT, Olympiad.getInstance()))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnOlympiadMatchResult(_playerTwo, _playerOne, getType()), Olympiad.getInstance());
				}
			}
			else
			{
				// Save Fight Result.
				saveResults(_playerOne, _playerTwo, 0, _startTime, _fightTime, getType());
				
				final SystemMessage sm = new SystemMessage(SystemMessageId.THERE_IS_NO_VICTOR_THE_MATCH_ENDS_IN_A_TIE);
				stadium.broadcastPacket(sm);
				isTie = true;
				
				final int p1PointsToRemove = Math.min(playerOnePoints / getDivider(), EventsConfig.OLYMPIAD_MAX_POINTS);
				removePointsFromParticipant(_playerOne, p1PointsToRemove);
				losersList.add(new OlympiadInfo(_playerOne.getName(), _playerOne.getClanName(), _playerOne.getClanId(), _playerOne.getBaseClass(), _damageP1, playerOnePoints - p1PointsToRemove, -p1PointsToRemove));
				
				final int p2PointsToRemove = Math.min(playerTwoPoints / getDivider(), EventsConfig.OLYMPIAD_MAX_POINTS);
				removePointsFromParticipant(_playerTwo, p2PointsToRemove);
				losersList.add(new OlympiadInfo(_playerTwo.getName(), _playerTwo.getClanName(), _playerTwo.getClanId(), _playerTwo.getBaseClass(), _damageP2, playerTwoPoints - p2PointsToRemove, -p2PointsToRemove));
			}
			
			_playerOne.updateStat(COMP_DONE, 1);
			_playerTwo.updateStat(COMP_DONE, 1);
			_playerOne.updateStat(COMP_DONE_WEEK, 1);
			_playerTwo.updateStat(COMP_DONE_WEEK, 1);
			_playerOne.updateStat(getWeeklyMatchType(), 1);
			_playerTwo.updateStat(getWeeklyMatchType(), 1);
			
			final ExOlympiadMatchResult result = new ExOlympiadMatchResult(isTie, winningSide, winnersList, losersList);
			stadium.broadcastPacket(result);
			
			if (EventsConfig.OLYMPIAD_LOG_FIGHTS)
			{
				LOGGER_OLYMPIAD.info(winner + "," + _playerOne.getName() + "," + _playerOne + "," + _playerTwo + "," + playerOneHp + "," + playerTwoHp + "," + _damageP1 + "," + _damageP2 + "," + pointDiff + "," + getType());
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception on validateWinner(): " + e.getMessage(), e);
		}
	}
	
	@Override
	protected void addDamage(Player player, int damage)
	{
		final Player player1 = _playerOne.getPlayer();
		final Player player2 = _playerTwo.getPlayer();
		if ((player1 == null) || (player2 == null))
		{
			return;
		}
		
		if (player == player1)
		{
			if (!player2.isInvul())
			{
				_damageP1 += damage;
			}
		}
		else if (player == player2)
		{
			if (!player1.isInvul())
			{
				_damageP2 += damage;
			}
		}
	}
	
	@Override
	public String[] getPlayerNames()
	{
		return new String[]
		{
			_playerOne.getName(),
			_playerTwo.getName()
		};
	}
	
	@Override
	public boolean checkDefaulted()
	{
		SystemMessage reason;
		_playerOne.updatePlayer();
		_playerTwo.updatePlayer();
		
		reason = checkDefaulted(_playerOne.getPlayer());
		if (reason != null)
		{
			_playerOne.setDefaulted(true);
			if (_playerTwo.getPlayer() != null)
			{
				_playerTwo.getPlayer().sendPacket(reason);
			}
		}
		
		reason = checkDefaulted(_playerTwo.getPlayer());
		if (reason != null)
		{
			_playerTwo.setDefaulted(true);
			if (_playerOne.getPlayer() != null)
			{
				_playerOne.getPlayer().sendPacket(reason);
			}
		}
		
		return _playerOne.isDefaulted() || _playerTwo.isDefaulted();
	}
	
	@Override
	public void resetDamage()
	{
		_damageP1 = 0;
		_damageP2 = 0;
	}
	
	protected void saveResults(Participant one, Participant two, int winner, long startTime, long fightTime, CompetitionType type)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO olympiad_fights (charOneId, charTwoId, charOneClass, charTwoClass, winner, start, time, classed) values(?,?,?,?,?,?,?,?)"))
		{
			ps.setInt(1, one.getObjectId());
			ps.setInt(2, two.getObjectId());
			ps.setInt(3, one.getBaseClass());
			ps.setInt(4, two.getBaseClass());
			ps.setInt(5, winner);
			ps.setLong(6, startTime);
			ps.setLong(7, fightTime);
			ps.setInt(8, type == CompetitionType.CLASSED ? 1 : 0);
			ps.execute();
		}
		catch (SQLException e)
		{
			if (LOGGER.isLoggable(Level.SEVERE))
			{
				LOGGER.log(Level.SEVERE, "SQL exception while saving olympiad fight.", e);
			}
		}
	}
	
	@Override
	public void makePlayersInvul()
	{
		if (_playerOne.getPlayer() != null)
		{
			_playerOne.getPlayer().setInvul(true);
		}
		if (_playerTwo.getPlayer() != null)
		{
			_playerTwo.getPlayer().setInvul(true);
		}
	}
	
	@Override
	public void removePlayersInvul()
	{
		if (_playerOne.getPlayer() != null)
		{
			_playerOne.getPlayer().setInvul(false);
		}
		if (_playerTwo.getPlayer() != null)
		{
			_playerTwo.getPlayer().setInvul(false);
		}
	}
}
