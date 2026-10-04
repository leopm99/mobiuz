package events.LoversJubilee;

import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.events.EventType;
import com.l2journey.gameserver.model.events.ListenerRegisterType;
import com.l2journey.gameserver.model.events.annotations.RegisterEvent;
import com.l2journey.gameserver.model.events.annotations.RegisterType;
import com.l2journey.gameserver.model.events.holders.actor.player.OnPlayerLogin;
import com.l2journey.gameserver.model.itemcontainer.Inventory;
import com.l2journey.gameserver.model.quest.LongTimeEvent;
import com.l2journey.gameserver.network.serverpackets.ExBrBroadcastEventState;

public final class LoversJubilee extends LongTimeEvent
{
	private static final int ROSALIA = 4305;
	
	private static final int ONE_RED_ROSE_BUD = 20905;
	private static final int ONE_BLUE_ROSE_BUD = 20906;
	private static final int ONE_WHILE_ROSE_BUD = 20907;
	
	private static final int DESELOPH_ROSE_NECKLACE = 20908;
	private static final int HYUM_ROSE_NECKLACE = 20909;
	private static final int REKANG_ROSE_NECKLACE = 20910;
	private static final int LILIAS_ROSE_NECKLACE = 20911;
	private static final int LAPHAM_ROSE_NECKLACE = 20912;
	private static final int MAFUM_ROSE_NECKLACE = 20913;
	
	private static final int IMPROVED_ROSE_SPIRIT_EXCHANGE_TICKET = 20914;
	
	private static final int IMPROVED_DESELOPH_ROSE_NECKLACE = 20915;
	private static final int IMPROVED_HYUM_ROSE_NECKLACE = 20916;
	private static final int IMPROVED_REKANG_ROSE_NECKLACE = 20917;
	private static final int IMPROVED_LILIAS_ROSE_NECKLACE = 20918;
	private static final int IMPROVED_LAPHAM_ROSE_NECKLACE = 20919;
	private static final int IMPROVED_MAFUM_ROSE_NECKLACE = 20920;
	
	private static final int SPIRIT_TEST_REPORT = 20921;
	
	private static final int ONE_ROSE_PRICE = 500;
	private static final int TEN_ROSES_PRICE = 5000;
	
	public static final int LOVERS_JUBILEE = 20100214;
	
	public LoversJubilee()
	{
		addStartNpc(ROSALIA);
		addFirstTalkId(ROSALIA);
		addTalkId(ROSALIA);
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerLogin(OnPlayerLogin event)
	{
		if (isEventPeriod())
		{
			event.getPlayer().sendPacket(new ExBrBroadcastEventState(ExBrBroadcastEventState.LOVERS_JUBILEE, 1));
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		switch (event)
		{
			case "50020_1":
			{
				htmltext = hasQuestItems(player, SPIRIT_TEST_REPORT) ? "4305-010.html" : "4305-002.html";
				break;
			}
			case "50020_2", "50020_3", "50020_4":
			{
				if (getQuestItemsCount(player, Inventory.ADENA_ID) < ONE_ROSE_PRICE)
				{
				}
				else
				{
					takeItems(player, Inventory.ADENA_ID, ONE_ROSE_PRICE);
					switch (event)
					{
						case "50020_2":
						{
							giveItems(player, ONE_RED_ROSE_BUD, 1);
							break;
						}
						case "50020_3":
						{
							giveItems(player, ONE_BLUE_ROSE_BUD, 1);
							break;
						}
						case "50020_4":
						{
							giveItems(player, ONE_WHILE_ROSE_BUD, 1);
							break;
						}
					}
				}
			}
			case "50020_5", "50020_6", "50020_7":
			{
				if (getQuestItemsCount(player, Inventory.ADENA_ID) < TEN_ROSES_PRICE)
				{
				}
				else
				{
					takeItems(player, Inventory.ADENA_ID, TEN_ROSES_PRICE);
					switch (event)
					{
						case "50020_5":
						{
							giveItems(player, ONE_RED_ROSE_BUD, 10);
							break;
						}
						case "50020_6":
						{
							giveItems(player, ONE_BLUE_ROSE_BUD, 10);
							break;
						}
						case "50020_7":
						{
							giveItems(player, ONE_WHILE_ROSE_BUD, 10);
							break;
						}
					}
				}
			}
			case "50020_8":
			{
				if (hasQuestItems(player, IMPROVED_ROSE_SPIRIT_EXCHANGE_TICKET))
				{
					htmltext = "4305-007.html";
				}
				else
				{
					htmltext = "4305-008.html";
				}
				break;
			}
			case "50020_9", "50020_10", "50020_11", "50020_12", "50020_13", "50020_14":
			{
				giveItems(player, SPIRIT_TEST_REPORT, 1);
				switch (event)
				{
					case "50020_9":
					{
						giveItems(player, DESELOPH_ROSE_NECKLACE, 1);
						break;
					}
					case "50020_10":
					{
						giveItems(player, HYUM_ROSE_NECKLACE, 1);
						break;
					}
					case "50020_11":
					{
						giveItems(player, REKANG_ROSE_NECKLACE, 1);
						break;
					}
					case "50020_12":
					{
						giveItems(player, LILIAS_ROSE_NECKLACE, 1);
						break;
					}
					case "50020_13":
					{
						giveItems(player, LAPHAM_ROSE_NECKLACE, 1);
						break;
					}
					case "50020_14":
					{
						giveItems(player, MAFUM_ROSE_NECKLACE, 1);
						break;
					}
				}
			}
			case "50020_15", "50020_16", "50020_17", "50020_18", "50020_19", "50020_20":
			{
				takeItems(player, IMPROVED_ROSE_SPIRIT_EXCHANGE_TICKET, 1);
				switch (event)
				{
					case "50020_15":
					{
						giveItems(player, IMPROVED_DESELOPH_ROSE_NECKLACE, 1);
						break;
					}
					case "50020_16":
					{
						giveItems(player, IMPROVED_HYUM_ROSE_NECKLACE, 1);
						break;
					}
					case "50020_17":
					{
						giveItems(player, IMPROVED_REKANG_ROSE_NECKLACE, 1);
						break;
					}
					case "50020_18":
					{
						giveItems(player, IMPROVED_LILIAS_ROSE_NECKLACE, 1);
						break;
					}
					case "50020_19":
					{
						giveItems(player, IMPROVED_LAPHAM_ROSE_NECKLACE, 1);
						break;
					}
					case "50020_20":
					{
						giveItems(player, IMPROVED_MAFUM_ROSE_NECKLACE, 1);
						break;
					}
				}
				htmltext = "4305-026.html";
			}
		}
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "4305-001.html";
	}
	
	public static void main(String[] args)
	{
		new LoversJubilee();
	}
}
