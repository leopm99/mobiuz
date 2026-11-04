package handlers.voicedcommandhandlers;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.Config;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.handler.IVoicedCommandHandler;
import com.l2journey.gameserver.managers.InstanceManager;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.olympiad.OlympiadManager;
import com.l2journey.gameserver.model.zone.ZoneId;
import com.l2journey.gameserver.network.serverpackets.NpcHtmlMessage;

/** Purchase removal of the character's reentry cooldown, without changing an instance world. */
public class InstanceReuseClearCmd implements IVoicedCommandHandler
{
	private static final Logger LOGGER = Logger.getLogger(InstanceReuseClearCmd.class.getName());
	private static final String[] COMMANDS = { "instancereuse", "clearinstance" };
	private static final int PAGE_SIZE = 8;
	private static final long CONFIRMATION_LIFETIME = 60000;
	// Keys do not retain disconnected player objects. Values contain no reference to the player.
	private final Map<Player, Confirmation> _confirmations = Collections.synchronizedMap(new WeakHashMap<>());

	private record Confirmation(String token, int instanceId, long reuseTime, int itemId, long count, long expiresAt) { }

	@Override
	public String[] getVoicedCommandList() { return COMMANDS; }

	@Override
	public boolean useVoicedCommand(String command, Player player, String params)
	{
		if ((player == null) || !("instancereuse".equalsIgnoreCase(command) || "clearinstance".equalsIgnoreCase(command))) { return false; }
		synchronized (player)
		{
			if (!Config.INSTANCE_REUSE_CLEAR_ENABLED)
			{
				_confirmations.remove(player);
				player.sendMessage("Instance reuse reset service is disabled.");
				return false;
			}
			if (!canPurchase(player)) { _confirmations.remove(player); return false; }
			if ((Config.INSTANCE_REUSE_CLEAR_ITEM_COUNT <= 0) || (ItemData.getInstance().getTemplate(Config.INSTANCE_REUSE_CLEAR_ITEM_ID) == null))
			{
				player.sendMessage("Instance reuse reset price is not configured correctly. Contact a GM.");
				return false;
			}
			final String[] args = params == null || params.isBlank() ? new String[0] : params.trim().split("\\s+");
			try
			{
				if (args.length == 0)
				{
					_confirmations.remove(player);
					display(player, 1);
					return true;
				}
				if ((args.length == 2) && args[0].equalsIgnoreCase("confirm")) { return confirm(player, args[1]); }
				if ((args.length == 2) && args[0].equalsIgnoreCase("page"))
				{
					final int page = Integer.parseInt(args[1]);
					if (page < 1) { throw new NumberFormatException(); }
					_confirmations.remove(player);
					display(player, page);
					return true;
				}
				if (args.length != 1) { throw new NumberFormatException(); }
				final int id = Integer.parseInt(args[0]);
				final InstanceManager manager = InstanceManager.getInstance();
				final Long reuse = manager.getAllInstanceTimes(player.getObjectId()).get(id);
				if (!eligible(manager, id, reuse))
				{
					player.sendMessage("This instance has no eligible active reuse to clear.");
					return false;
				}
				final Confirmation request = new Confirmation(UUID.randomUUID().toString(), id, reuse, Config.INSTANCE_REUSE_CLEAR_ITEM_ID, Config.INSTANCE_REUSE_CLEAR_ITEM_COUNT, System.currentTimeMillis() + CONFIRMATION_LIFETIME);
				_confirmations.put(player, request);
				final String html = "<html><body><center><br>Clear reuse for " + escape(manager.getInstanceIdName(id)) + "?<br>Cost: " + cost() + "<br>This purchases a new entry; normal entry requirements still apply.<br>"
					+ "<button value=\"Confirm\" action=\"bypass -h voice .clearinstance confirm " + request.token() + "\" width=100 height=24 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"><br>"
					+ "<button value=\"Cancel\" action=\"bypass -h voice .instancereuse\" width=100 height=24 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></center></body></html>";
				send(player, html);
				return true;
			}
			catch (NumberFormatException e)
			{
				player.sendMessage("Usage: .instancereuse or .clearinstance <instanceId>");
				return false;
			}
			catch (RuntimeException e)
			{
				LOGGER.log(Level.WARNING, "Could not process instance reuse purchase for " + player.getObjectId(), e);
				player.sendMessage("Could not complete the request. Contact a GM.");
				return false;
			}
		}
	}

	private static boolean canPurchase(Player player)
	{
		if (!player.isOnline() || (player.getInstanceId() != 0) || (InstanceManager.getInstance().getPlayerWorld(player) != null))
		{
			player.sendMessage("Leave your active instance and wait for its session to end before buying another entry.");
			return false;
		}
		if (player.isAlikeDead() || player.isInCombat() || player.isInDuel() || player.isCastingNow() || player.isCastingSimultaneouslyNow() || player.isTeleporting() || player.isInStoreMode() || player.isJailed() || player.inObserverMode() || player.isOnEvent() || player.isRegisteredOnEvent() || player.isOnSoloEvent() || player.isInOlympiadMode() || OlympiadManager.getInstance().isRegisteredInComp(player) || player.isInsideZone(ZoneId.SIEGE) || player.isInsideZone(ZoneId.PVP) || (player.getPvpFlag() > 0))
		{
			player.sendMessage("You cannot buy an instance entry in your current state.");
			return false;
		}
		return true;
	}

	private static boolean eligible(InstanceManager manager, int id, Long time)
	{
		return (id > 0) && (time != null) && (time > System.currentTimeMillis()) && !"UnknownInstance".equals(manager.getInstanceIdName(id)) && ((Config.INSTANCE_REUSE_CLEAR_ALLOWED_IDS == null) || Config.INSTANCE_REUSE_CLEAR_ALLOWED_IDS.isEmpty() || Config.INSTANCE_REUSE_CLEAR_ALLOWED_IDS.contains(id));
	}

	private boolean confirm(Player player, String token)
	{
		final Confirmation request = _confirmations.remove(player);
		final InstanceManager manager = InstanceManager.getInstance();
		final Long currentReuse = request == null ? null : manager.getAllInstanceTimes(player.getObjectId()).get(request.instanceId());
		if ((request == null) || (currentReuse == null) || !request.token().equals(token) || (request.expiresAt() <= System.currentTimeMillis()) || (request.itemId() != Config.INSTANCE_REUSE_CLEAR_ITEM_ID) || (request.count() != Config.INSTANCE_REUSE_CLEAR_ITEM_COUNT) || !eligible(manager, request.instanceId(), currentReuse) || (currentReuse.longValue() != request.reuseTime()))
		{
			player.sendMessage("Confirmation expired or the reuse/price changed. Open the service again.");
			return false;
		}
		if (!player.destroyItemByItemId(ItemProcessType.FEE, request.itemId(), request.count(), player, true))
		{
			player.sendMessage("Not enough currency to clear this instance reuse.");
			return false;
		}
		boolean cleared = false;
		try { cleared = manager.deleteInstanceTimeIfMatches(player.getObjectId(), request.instanceId(), request.reuseTime()); }
		finally
		{
			if (!cleared)
			{
				if (player.addItem(ItemProcessType.REFUND, request.itemId(), request.count(), player, true) == null)
				{
					LOGGER.severe("Instance reuse refund failed for character " + player.getObjectId() + ", item " + request.itemId() + ", count " + request.count());
					player.sendMessage("Reset and refund failed. Contact a GM.");
				}
				else { player.sendMessage("Reuse could not be cleared. Your payment was returned."); }
			}
		}
		if (!cleared) { return false; }
		LOGGER.info("Character " + player.getObjectId() + " purchased instance reuse reset " + request.instanceId() + " for " + request.count() + " of item " + request.itemId());
		player.sendMessage("Instance reuse cleared. You may enter again if all other requirements are met.");
		display(player, 1);
		return true;
	}

	private void display(Player player, int requestedPage)
	{
		final InstanceManager manager = InstanceManager.getInstance();
		final List<Map.Entry<Integer, Long>> entries = manager.getAllInstanceTimes(player.getObjectId()).entrySet().stream().filter(e -> eligible(manager, e.getKey(), e.getValue())).map(e -> Map.entry(e.getKey(), e.getValue())).sorted(Map.Entry.comparingByKey()).toList();
		final int pages = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
		final int page = Math.min(requestedPage, pages);
		final StringBuilder html = new StringBuilder("<html><body><center><br><font color=\"LEVEL\">Instance Reuse Reset</font><br>");
		html.append("Cost per reset: ").append(cost()).append("<br><table width=300><tr><td width=170>Name</td><td width=60>Time</td><td width=70>Action</td></tr>");
		final long now = System.currentTimeMillis();
		for (int i = (page - 1) * PAGE_SIZE; i < Math.min(entries.size(), page * PAGE_SIZE); i++)
		{
			final var entry = entries.get(i);
			final long seconds = Math.max(0, (entry.getValue() - now) / 1000);
			html.append("<tr><td>").append(escape(manager.getInstanceIdName(entry.getKey()))).append("</td><td>").append(String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 3600, (seconds % 3600) / 60)).append("</td><td><button value=\"Clear\" action=\"bypass -h voice .clearinstance ").append(entry.getKey()).append("\" width=60 height=21 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td></tr>");
		}
		html.append("</table><br>");
		if (entries.isEmpty()) { html.append("No active instance reuses eligible for reset.<br>"); }
		if (page > 1) { html.append("<a action=\"bypass -h voice .instancereuse page ").append(page - 1).append("\">Previous</a> "); }
		html.append(page).append(" / ").append(pages);
		if (page < pages) { html.append(" <a action=\"bypass -h voice .instancereuse page ").append(page + 1).append("\">Next</a>"); }
		html.append("<br><button value=\"Refresh\" action=\"bypass -h voice .instancereuse\" width=100 height=24 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></center></body></html>");
		send(player, html.toString());
	}

	private static String cost()
	{
		final ItemTemplate item = ItemData.getInstance().getTemplate(Config.INSTANCE_REUSE_CLEAR_ITEM_ID);
		return Config.INSTANCE_REUSE_CLEAR_ITEM_COUNT + " " + escape(item == null ? "Unknown item" : item.getName());
	}

	private static String escape(String value)
	{
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
	}

	private static void send(Player player, String html)
	{
		final NpcHtmlMessage packet = new NpcHtmlMessage();
		packet.setHtml(html);
		player.sendPacket(packet);
	}
}
