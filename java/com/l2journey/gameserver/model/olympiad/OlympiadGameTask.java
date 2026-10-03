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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.EventsConfig;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.model.zone.type.OlympiadStadiumZone;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;

/**
 * @author DS
 */
public class OlympiadGameTask implements Runnable
{
	protected static final Logger LOGGER = Logger.getLogger(OlympiadGameTask.class.getName());
	protected static final long BATTLE_PERIOD = EventsConfig.OLYMPIAD_BATTLE; // 6 mins
	
	private static final int[] TELEPORT_TO_ARENA_TIMES =
	{
		120,
		60,
		30,
		15,
		10,
		5,
		4,
		3,
		2,
		1,
		0
	};
	private static final int[] BATTLE_START_TIME_FIRST =
	{
		60,
		50,
		40,
		30,
		20,
		10,
		0
	};
	private static final int[] BATTLE_START_TIME_SECOND =
	{
		10,
		5,
		4,
		3,
		2,
		1,
		0
	};
	private static final int[] TELEPORT_TO_TOWN_TIMES =
	{
		40,
		30,
		20,
		10,
		5,
		4,
		3,
		2,
		1,
		0
	};
	
	private final OlympiadStadiumZone _zone;
	private AbstractOlympiadGame _game;
	private OlympiadGameState _state = OlympiadGameState.IDLE;
	private boolean _needAnnounce = false;
	private int _countDown = 0;
	
	public OlympiadGameTask(OlympiadStadiumZone zone, int instanceId)
	{
		_zone = zone;
		_zone.setInstanceId(instanceId);
		zone.registerTask(this);
	}
	
	public boolean isRunning()
	{
		return _state != OlympiadGameState.IDLE;
	}
	
	public boolean isGameStarted()
	{
		return (_state.ordinal() >= OlympiadGameState.GAME_STARTED.ordinal()) && (_state.ordinal() <= OlympiadGameState.CLEANUP.ordinal());
	}
	
	public boolean isBattleStarted()
	{
		return _state == OlympiadGameState.BATTLE_IN_PROGRESS;
	}
	
	public boolean isBattleFinished()
	{
		return _state == OlympiadGameState.TELEPORT_TO_TOWN;
	}
	
	public boolean needAnnounce()
	{
		if (_needAnnounce)
		{
			_needAnnounce = false;
			return true;
		}
		return false;
	}
	
	public OlympiadStadiumZone getZone()
	{
		return _zone;
	}
	
	public AbstractOlympiadGame getGame()
	{
		return _game;
	}
	
	public void attachGame(AbstractOlympiadGame game)
	{
		if ((game != null) && (_state != OlympiadGameState.IDLE))
		{
			LOGGER.log(Level.WARNING, "Attempt to overwrite non-finished game in state " + _state);
			return;
		}
		
		_game = game;
		_state = OlympiadGameState.BEGIN;
		_needAnnounce = false;
		ThreadPool.execute(this);
	}
	
	@Override
	public void run()
	{
		try
		{
			int delay = 1; // schedule next call after 1s
			switch (_state)
			{
				// Game created
				case BEGIN:
				{
					_state = OlympiadGameState.TELEPORT_TO_ARENA;
					_countDown = EventsConfig.OLYMPIAD_WAIT_TIME;
					break;
				}
				// Teleport to arena countdown
				case TELEPORT_TO_ARENA:
				{
					if (_countDown > 0)
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_WILL_BE_MOVED_TO_THE_OLYMPIAD_STADIUM_IN_S1_SECOND_S);
						sm.addInt(_countDown);
						_game.broadcastPacket(sm);
					}
					
					delay = getDelay(TELEPORT_TO_ARENA_TIMES);
					if (_countDown <= 0)
					{
						_state = OlympiadGameState.GAME_STARTED;
					}
					break;
				}
				// Game start, port players to arena
				case GAME_STARTED:
				{
					if (!startGame())
					{
						_state = OlympiadGameState.GAME_CANCELLED;
						break;
					}
					
					_state = OlympiadGameState.BATTLE_COUNTDOWN_FIRST;
					_countDown = BATTLE_START_TIME_FIRST[0];
					delay = 5;
					break;
				}
				// Battle start countdown, first part (60-10)
				case BATTLE_COUNTDOWN_FIRST:
				{
					if (_countDown > 0)
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.THE_MATCH_WILL_START_IN_S1_SECOND_S);
						sm.addInt(_countDown);
						_zone.broadcastPacket(sm);
					}
					
					delay = getDelay(BATTLE_START_TIME_FIRST);
					if (_countDown <= 0)
					{
						_game.makePlayersInvul();
						openDoors();
						
						_state = OlympiadGameState.BATTLE_COUNTDOWN_SECOND;
						_countDown = BATTLE_START_TIME_SECOND[0];
						delay = getDelay(BATTLE_START_TIME_SECOND);
					}
					break;
				}
				// Battle start countdown, second part (10-0)
				case BATTLE_COUNTDOWN_SECOND:
				{
					if (_countDown > 0)
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.THE_MATCH_WILL_START_IN_S1_SECOND_S);
						sm.addInt(_countDown);
						_zone.broadcastPacket(sm);
					}
					
					delay = getDelay(BATTLE_START_TIME_SECOND);
					if (_countDown <= 0)
					{
						_state = OlympiadGameState.BATTLE_STARTED;
						_game.removePlayersInvul();
					}
					break;
				}
				// Beginning of the battle
				case BATTLE_STARTED:
				{
					_countDown = 0;
					_state = OlympiadGameState.BATTLE_IN_PROGRESS; // set state first, used in zone update
					if (!startBattle())
					{
						_state = OlympiadGameState.GAME_STOPPED;
					}
					break;
				}
				// Checks during battle
				case BATTLE_IN_PROGRESS:
				{
					_countDown += 1000;
					if (checkBattle() || (_countDown > EventsConfig.OLYMPIAD_BATTLE))
					{
						_state = OlympiadGameState.GAME_STOPPED;
					}
					break;
				}
				// Battle cancelled before teleport participants to the stadium
				case GAME_CANCELLED:
				{
					stopGame();
					_state = OlympiadGameState.CLEANUP;
					break;
				}
				// End of the battle
				case GAME_STOPPED:
				{
					_state = OlympiadGameState.TELEPORT_TO_TOWN;
					_countDown = TELEPORT_TO_TOWN_TIMES[0];
					stopGame();
					delay = getDelay(TELEPORT_TO_TOWN_TIMES);
					break;
				}
				// Teleport to town countdown
				case TELEPORT_TO_TOWN:
				{
					if (_countDown > 0)
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_WILL_BE_MOVED_BACK_TO_TOWN_IN_S1_SECOND_S);
						sm.addInt(_countDown);
						_game.broadcastPacket(sm);
					}
					
					delay = getDelay(TELEPORT_TO_TOWN_TIMES);
					if (_countDown <= 0)
					{
						_state = OlympiadGameState.CLEANUP;
					}
					break;
				}
				// Removals
				case CLEANUP:
				{
					cleanupGame();
					_state = OlympiadGameState.IDLE;
					_game = null;
					return;
				}
			}
			ThreadPool.schedule(this, delay * 1000);
		}
		catch (Exception e)
		{
			switch (_state)
			{
				case GAME_STOPPED, TELEPORT_TO_TOWN, CLEANUP, IDLE:
				{
					LOGGER.log(Level.WARNING, "Unable to return players back in town, exception: " + e.getMessage());
					_state = OlympiadGameState.IDLE;
					_game = null;
					return;
				}
			}
			
			LOGGER.log(Level.WARNING, "Exception in " + _state + ", trying to port players back: " + e.getMessage(), e);
			_state = OlympiadGameState.GAME_STOPPED;
			ThreadPool.schedule(this, 1000);
		}
	}
	
	private final int getDelay(int[] times)
	{
		int time;
		for (int i = 0; i < (times.length - 1); i++)
		{
			time = times[i];
			if (time >= _countDown)
			{
				continue;
			}
			
			final int delay = _countDown - time;
			_countDown = time;
			return delay;
		}
		// should not happens
		_countDown = -1;
		return 1;
	}
	
	/**
	 * Second stage: check for defaulted, port players to arena, announce game.
	 * @return true if no participants defaulted.
	 */
	private final boolean startGame()
	{
		try
		{
			// Checking for opponents and teleporting to arena
			if (_game.checkDefaulted())
			{
				return false;
			}
			
			_zone.closeDoors();
			if (_game.needBuffers())
			{
				_zone.spawnBuffers();
			}
			
			if (!_game.portPlayersToArena(_zone.getSpawns()))
			{
				return false;
			}
			
			_game.removals();
			_needAnnounce = true;
			OlympiadGameManager.getInstance().startBattle(); // inform manager
			return true;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		return false;
	}
	
	/**
	 * Third stage: open doors.
	 */
	private void openDoors()
	{
		try
		{
			_game.resetDamage();
			_zone.openDoors();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	/**
	 * Fourth stage: last checks, remove buffers, start competition itself.
	 * @return true if all participants online and ready on the stadium.
	 */
	private final boolean startBattle()
	{
		try
		{
			if (_game.needBuffers())
			{
				_zone.deleteBuffers();
			}
			
			if (_game.checkBattleStatus() && _game.makeCompetitionStart())
			{
				// game successfully started
				_game.broadcastOlympiadInfo(_zone);
				_zone.broadcastPacket(new SystemMessage(SystemMessageId.THE_MATCH_HAS_STARTED_FIGHT));
				_zone.updateZoneStatusForCharactersInside();
				return true;
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		return false;
	}
	
	/**
	 * Fifth stage: battle is running, returns true if winner found.
	 * @return
	 */
	private boolean checkBattle()
	{
		try
		{
			return _game.haveWinner();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		return true;
	}
	
	/**
	 * Sixth stage: winner's validations
	 */
	private void stopGame()
	{
		try
		{
			_game.validateWinner(_zone);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		
		try
		{
			_game.cleanEffects();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		
		try
		{
			_game.makePlayersInvul();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		
		try
		{
			_zone.updateZoneStatusForCharactersInside();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	/**
	 * Seventh stage: game cleanup (port players back, closing doors, etc)
	 */
	private void cleanupGame()
	{
		try
		{
			_game.removePlayersInvul();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		
		try
		{
			_game.playersStatusBack();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		
		try
		{
			_game.portPlayersBack();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		
		try
		{
			_game.clearPlayers();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		
		try
		{
			_zone.closeDoors();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
	}
}