package handlers.voicedcommandhandlers;

import com.l2journey.Config;
import com.l2journey.gameserver.handler.IVoicedCommandHandler;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.quest.QuestState;
import com.l2journey.gameserver.network.serverpackets.NpcHtmlMessage;

public class SevenRB implements IVoicedCommandHandler
{
	private static final String QUEST_NAME = "Q00254_LegendaryTales";
	private static final String SERVER_NAME = "(NAME)";
	private static final String[] commands =
	{
		"7rb",
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String params)
	{
		if (!Config.ENABLE_SEVEN_RB_COMMAND)
		{
			activeChar.sendMessage("The .7rb voiced commands it's currently disabled.");
			return false;
		}
		
		if (command.equalsIgnoreCase("7rb"))
		{
			QuestState st = activeChar.getQuestState(QUEST_NAME);
			NpcHtmlMessage m = new NpcHtmlMessage();
			m.setHtml(buildHtml(st));
			activeChar.sendPacket(m);
		}
		return true;
	}
	
	private static final String buildHtml(QuestState st)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("<html><head>");
		sb.append("<title>" + SERVER_NAME + "</title>");
		sb.append("</head>");
		sb.append("<body><br>");
		sb.append("<br>7Rb Quest (Legendary Tales) status:<br>");
		if ((st == null) || st.isCreated())
		{
			sb.append("Quest is not started yet. Please visit Glimore in dragon valley in order to start it.");
			sb.append("<br>");
		}
		else
		{
			if (st.isCond(1))
			{
				for (Bosses boss : Bosses.class.getEnumConstants())
				{
					sb.append(boss.getName() + ": ");
					sb.append(checkMask(st, boss) ? "<font color=\"00FF00\">Killed.</font>" : "<font color=\"FF0000\">Not killed.</font>");
					sb.append("<br>");
				}
			}
			else if (st.isCond(2))
			{
				sb.append("All seven bosses defeated. Return to Gilmore to claim your reward.");
				sb.append("<br>");
			}
			else if (st.isCompleted())
			{
				sb.append("Legendary Tales quest is completed.");
				sb.append("<br>");
			}
		}
		sb.append("</body></html>");
		return sb.toString();
	}
	
	private static boolean checkMask(QuestState qs, Bosses boss)
	{
		int pos = boss.getMask();
		return ((qs.getInt("raids") & pos) == pos);
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return commands;
	}
	
	public static enum Bosses
	{
		EMERALD_HORN("Emerald Horn"),
		DUST_RIDER("Dust Rider"),
		BLEEDING_FLY("Bleeding Fly"),
		BLACK_DAGGER("Blackdagger Wing"),
		SHADOW_SUMMONER("Shadow Summoner"),
		SPIKE_SLASHER("Spike Slasher"),
		MUSCLE_BOMBER("Muscle Bomber");
		
		private final String name;
		private final int _mask;
		
		private Bosses(String name)
		{
			this.name = name;
			_mask = 1 << ordinal();
		}
		
		public int getMask()
		{
			return _mask;
		}
		
		public String getName()
		{
			return name;
		}
	}
}