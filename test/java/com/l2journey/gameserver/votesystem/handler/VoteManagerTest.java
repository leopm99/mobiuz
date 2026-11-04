package com.l2journey.gameserver.votesystem.handler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.Config;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.model.StatSet;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.network.GameClient;
import com.l2journey.gameserver.util.Broadcast;
import com.l2journey.gameserver.votesystem.Enum.VoteSite;
import com.l2journey.gameserver.votesystem.db.GlobalVoteDB;
import com.l2journey.gameserver.votesystem.db.IndividualVoteDB;
import com.l2journey.gameserver.votesystem.model.GlobalVote;
import com.l2journey.gameserver.votesystem.model.IndividualVote;
import com.l2journey.gameserver.votesystem.model.IndividualVoteResponse;
import com.l2journey.gameserver.votesystem.model.Reward;
import com.l2journey.gameserver.votesystem.util.VoteSiteData;

class VoteManagerTest
{
	private final Map<Field, Object> _config = new HashMap<>();
	private final List<MockedStatic<?>> _mocks = new ArrayList<>();
	private VoteManager _manager;
	private VoteSiteData _data;
	private IndividualVoteDB _individual;
	private GlobalVoteDB _global;
	private MockedStatic<VoteHandler> _provider;
	private MockedStatic<ThreadPool> _threads;
	private World _world;

	private <T> MockedStatic<T> staticMock(Class<T> type)
	{
		final var mocked = mockStatic(type);
		_mocks.add(mocked);
		return mocked;
	}

	@BeforeEach
	void setUp() throws Exception
	{
		for (String name : List.of("ENABLE_VOTE_SYSTEM", "ENABLE_GLOBAL_VOTE", "ENABLE_INDIVIDUAL_VOTE", "INTERVAL_TO_NEXT_VOTE", "GLOBAL_VOTES_AMOUNT_TO_NEXT_REWARD"))
		{
			final Field field = Config.class.getField(name);
			_config.put(field, field.get(null));
		}
		Config.ENABLE_VOTE_SYSTEM = false;
		final var constructor = VoteManager.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		_manager = spy(constructor.newInstance());
		Config.ENABLE_VOTE_SYSTEM = true;
		Config.ENABLE_GLOBAL_VOTE = true;
		Config.ENABLE_INDIVIDUAL_VOTE = true;
		Config.INTERVAL_TO_NEXT_VOTE = 43200000;
		Config.GLOBAL_VOTES_AMOUNT_TO_NEXT_REWARD = 50;
		_individual = mock(IndividualVoteDB.class);
		staticMock(IndividualVoteDB.class).when(IndividualVoteDB::getInstance).thenReturn(_individual);
		_global = mock(GlobalVoteDB.class);
		staticMock(GlobalVoteDB.class).when(GlobalVoteDB::getInstance).thenReturn(_global);
		when(_global.isLoaded()).thenReturn(true);
		final GlobalVote[] votes = new GlobalVote[VoteSite.slotCount()];
		for (int i = 0; i < votes.length; i++) { votes[i] = new GlobalVote(i, 0); }
		when(_global.getGlobalVotes()).thenReturn(votes);
		when(_global.saveGlobalVoteChecked(any())).thenReturn(true);
		_manager.loadGlobalVotes();
		final VoteSiteData data = mock(VoteSiteData.class);
		_data = data;
		staticMock(VoteSiteData.class).when(VoteSiteData::getInstance).thenReturn(data);
		final StatSet reward = new StatSet();
		reward.set("itemId", 57);
		reward.set("itemCount", 100);
		when(data.getRewards(anyInt())).thenReturn(List.of(new Reward(reward)));
		when(data.getSiteName(anyInt())).thenReturn("Test site");
		when(data.isIndividualEnabled(anyInt())).thenReturn(true);
		when(data.isGlobalEnabled(anyInt())).thenReturn(true);
		when(data.getSettings(anyInt())).thenReturn(new VoteSiteData.Settings(true, true, 0, 0, java.time.ZoneId.of("UTC")));
		when(data.getGlobalRewards(anyInt())).thenReturn(List.of(new Reward(reward)));
		final ItemData items = mock(ItemData.class);
		staticMock(ItemData.class).when(ItemData::getInstance).thenReturn(items);
		when(items.getTemplate(57)).thenReturn(mock(ItemTemplate.class));
		_provider = staticMock(VoteHandler.class);
		final IndividualVoteResponse response = new IndividualVoteResponse();
		response.setIsVoted(true);
		response.setVoteSiteTime(System.currentTimeMillis());
		_provider.when(() -> VoteHandler.getIndividualVoteResponse(anyInt(), anyString(), any())).thenReturn(response);
		_threads = staticMock(ThreadPool.class);
		_world = mock(World.class);
		staticMock(World.class).when(World::getInstance).thenReturn(_world);
		staticMock(Broadcast.class);
		doReturn(0).when(_manager).getGlobalVotesResponse(anyInt());
	}

	@AfterEach
	void tearDown() throws Exception
	{
		for (int i = _mocks.size() - 1; i >= 0; i--) { _mocks.get(i).close(); }
		for (var entry : _config.entrySet()) { entry.getKey().set(null, entry.getValue()); }
	}

	private Player player(int id, String ip)
	{
		final Player player = mock(Player.class);
		when(player.getObjectId()).thenReturn(id);
		when(player.isOnline()).thenReturn(true);
		final GameClient client = mock(GameClient.class);
		when(player.getClient()).thenReturn(client);
		when(client.getPlayer()).thenReturn(player);
		when(client.getHostAddress()).thenReturn(ip);
		when(player.addItem(ItemProcessType.REWARD, 57, 100, player, true)).thenReturn(mock(Item.class));
		return player;
	}

	@Test
	void durableIndividualClaimPrecedesDeliveryAndBlocksSameIpTwice()
	{
		final Player first = player(1, "192.0.2.1");
		final Player second = player(2, "192.0.2.1");
		when(_individual.claimVote(any())).thenReturn(true);
		_manager.getReward(first, 0);
		_manager.getReward(second, 0);
		final var order = inOrder(_individual, first);
		order.verify(_individual).claimVote(any());
		order.verify(first).addItem(ItemProcessType.REWARD, 57, 100, first, true);
		verify(_individual, times(1)).claimVote(any());
		verify(second, never()).addItem(any(), anyInt(), anyLong(), any(), anyBoolean());
	}

	@Test
	void databaseFailurePreventsItemDelivery()
	{
		final Player player = player(1, "192.0.2.1");
		_manager.getReward(player, 0);
		verify(player, never()).addItem(any(), anyInt(), anyLong(), any(), anyBoolean());
	}

	@Test
	void disconnectedPlayerAndInvalidSiteNeverCallProvider()
	{
		assertFalse(_manager.checkIndividualAvailableVote(mock(Player.class), 0));
		_manager.getReward(null, 0);
		_manager.getReward(player(1, "192.0.2.1"), 99);
		_provider.verifyNoInteractions();
	}

	@Test
	void providerClockOffsetIsAddedAndDurationsDoNotWrapAt24Hours()
	{
		final IndividualVote vote = new IndividualVote("ip", 3600000, System.currentTimeMillis() - 3600000, 0, true);
		assertEquals(Config.INTERVAL_TO_NEXT_VOTE, _manager.getTimeRemaining(vote), 1000);
		assertEquals("25H:01m:02s", _manager.CalculateTimeRemainingWithSampleDateFormat(90062000));
	}

	@Test
	void updatingGlobalTotalsDoesNotConsumeRewardThresholds()
	{
		doReturn(150).when(_manager).getGlobalVotesResponse(0);
		_manager.checkNewUpdate(0);
		verify(_global, never()).saveGlobalVoteChecked(any());
	}

	@Test
	void globalRoundRewardsOnePlayerPerIpAndDoesNotReplay()
	{
		final Player first = player(1, "192.0.2.1");
		final Player second = player(2, "192.0.2.1");
		when(_world.getPlayers()).thenReturn(List.of(first, second));
		doReturn(50).when(_manager).getGlobalVotesResponse(0);
		_manager.AutoGlobalVoteReward();
		_manager.AutoGlobalVoteReward();
		verify(first, times(1)).addItem(ItemProcessType.REWARD, 57, 100, first, true);
		verify(second, never()).addItem(any(), anyInt(), anyLong(), any(), anyBoolean());
		verify(_global, times(1)).saveGlobalVoteChecked(any());
	}

	@Test
	void noGlobalRecipientsOrFailedCheckpointCannotConsumeRound()
	{
		doReturn(50).when(_manager).getGlobalVotesResponse(0);
		when(_world.getPlayers()).thenReturn(List.of());
		_manager.AutoGlobalVoteReward();
		verify(_global, never()).saveGlobalVoteChecked(any());
		final Player player = player(1, "192.0.2.1");
		when(_world.getPlayers()).thenReturn(List.of(player));
		when(_global.saveGlobalVoteChecked(any())).thenReturn(false);
		_manager.AutoGlobalVoteReward();
		verify(player, never()).addItem(any(), anyInt(), anyLong(), any(), anyBoolean());
	}

	@Test
	void initialPollingIsSubmittedToManagedPoolAndShutdownCancelsTasks() throws Exception
	{
		_manager.checkAllResponseGlobalVotes();
		_threads.verify(() -> ThreadPool.execute(any(Runnable.class)));
		final ScheduledFuture<?> future = mock(ScheduledFuture.class);
		final Field task = VoteManager.class.getDeclaredField("_updateIndividualVotes");
		task.setAccessible(true);
		task.set(_manager, future);
		_manager.shutdown();
		verify(future).cancel(false);
		assertFalse(_manager.checkIndividualAvailableVote(player(1, "192.0.2.1"), 0));
	}

	@Test
	void disabledSitesNeverQueryProvidersOrDeliverRewards()
	{
		when(_data.isIndividualEnabled(0)).thenReturn(false);
		_manager.getReward(player(1, "192.0.2.1"), 0);
		_provider.verifyNoInteractions();
		when(_data.isGlobalEnabled(anyInt())).thenReturn(false);
		_manager.AutoGlobalVoteReward();
		verify(_manager, never()).getGlobalVotesResponse(anyInt());
	}

	@Test
	void perSiteGlobalStepReplacesTheSharedThreshold()
	{
		when(_data.getSettings(12)).thenReturn(new VoteSiteData.Settings(true, true, 20, 30000, java.time.ZoneId.of("UTC")));
		final Player player = player(1, "192.0.2.1");
		when(_world.getPlayers()).thenReturn(List.of(player));
		doReturn(20).when(_manager).getGlobalVotesResponse(12);
		_manager.AutoGlobalVoteReward();
		verify(player).addItem(ItemProcessType.REWARD, 57, 100, player, true);
		verify(_global).saveGlobalVoteChecked(argThat(vote -> vote.getVoyeSite() == 12 && vote.getVotesLastReward() == 20));
	}
}
