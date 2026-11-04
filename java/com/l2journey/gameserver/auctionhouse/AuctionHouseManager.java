package com.l2journey.gameserver.auctionhouse;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.Config;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.auctionhouse.AuctionHouseStore.Reservation;
import com.l2journey.gameserver.auctionhouse.AuctionHouseStore.Row;
import com.l2journey.gameserver.data.sql.CharInfoTable;
import com.l2journey.gameserver.managers.MailManager;
import com.l2journey.gameserver.model.Message;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.item.enums.ItemLocation;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.itemcontainer.Inventory;
import com.l2journey.gameserver.model.itemcontainer.Mail;
import com.l2journey.gameserver.model.zone.ZoneId;
import com.l2journey.gameserver.network.enums.MessageSenderType;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.ItemList;
import com.l2journey.gameserver.network.serverpackets.StatusUpdate;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;

/** Fixed-price player market. All listing mutations share this monitor, including expiration. */
public final class AuctionHouseManager
{
	private static final Logger LOGGER = Logger.getLogger(AuctionHouseManager.class.getName());
	private final AuctionHouseStore store;
	private final Map<Integer, Entry> entries = new HashMap<>();
	private boolean ready;
	private ScheduledFuture<?> expirationTask;

	private static final class RejectedOperation extends IllegalArgumentException
	{
		RejectedOperation(String message) { super(message); }
	}

	private static final class Entry
	{
		final int id;
		final int ownerId;
		final long price;
		final long expiresAt;
		final AuctionEscrow escrow;
		final Item item;
		long quantity;

		Entry(Row row, Item item)
		{
			id = row.id(); ownerId = row.ownerId(); price = row.price(); expiresAt = row.expiresAt(); quantity = row.quantity();
			this.item = item;
			escrow = new AuctionEscrow(ownerId, id);
			escrow.restoreItem(item);
		}

		Entry(int id, int ownerId, long price, long expiresAt, Item item, AuctionEscrow escrow)
		{
			this.id = id; this.ownerId = ownerId; this.price = price; this.expiresAt = expiresAt;
			this.item = item; this.escrow = escrow; quantity = item.getCount();
		}
	}

	AuctionHouseManager(AuctionHouseStore store) { this.store = store; }

	public synchronized void start()
	{
		if (!Config.COMMUNITY_AUCTION_ENABLED || ready || (expirationTask != null)) { return; }
		if (!Config.ALLOW_MAIL)
		{
			LOGGER.severe("AuctionHouseManager: Auction House requires AllowMail=True; service disabled.");
			return;
		}
		try
		{
			AuctionHouseRules.fee(1, 1, 1, Config.COMMUNITY_AUCTION_FEE_PERCENT, Config.COMMUNITY_AUCTION_MIN_FEE);
			if ((Config.COMMUNITY_AUCTION_MAX_LISTINGS < 1) || (Config.COMMUNITY_AUCTION_MAX_LISTINGS > 100))
			{
				throw new IllegalArgumentException("Auction listing limit must be between 1 and 100.");
			}
			store.validateSchema();
			for (Row row : store.load())
			{
				if (!"ACTIVE".equals(row.state()))
				{
					LOGGER.severe("AuctionHouseManager: Listing " + row.id() + " requires manual reconciliation; automatic processing is blocked.");
					continue;
				}
				try
				{
					AuctionHouseRules.total(row.quantity(), row.price());
					final Item item = store.restore(row);
					if ((item == null) || (World.getInstance().findObject(row.objectId()) != null))
					{
						throw new IllegalStateException("Missing or already loaded escrow item");
					}
					World.getInstance().addObject(item);
					entries.put(row.id(), new Entry(row, item));
				}
				catch (RuntimeException e)
				{
					store.review(row.id());
					LOGGER.log(Level.SEVERE, "AuctionHouseManager: Invalid escrow for listing " + row.id() + "; reconciliation required.", e);
				}
			}
			ready = true;
			expirationTask = ThreadPool.scheduleAtFixedRate(this::expire, 60_000, 60_000);
			LOGGER.info("AuctionHouseManager: Restored " + entries.size() + " active listings.");
		}
		catch (SQLException | RuntimeException e)
		{
			LOGGER.log(Level.SEVERE, "AuctionHouseManager: Service disabled; verify configuration and apply the community auction SQL.", e);
		}
	}

	public synchronized void shutdown()
	{
		ready = false;
		if (expirationTask != null) { expirationTask.cancel(false); }
	}

	public synchronized boolean isReady() { return ready && Config.COMMUNITY_AUCTION_ENABLED; }

	public void checkAccess(Player player)
	{
		if (!isReady()) { throw new IllegalArgumentException("The Auction House is currently unavailable."); }
		if ((player == null) || (player.getClient() == null) || player.isAlikeDead() || player.isJailed() || player.isInStoreMode()
			|| player.isProcessingTransaction() || player.getInventory().hasInventoryBlock() || (player.getActiveTradeList() != null) || player.isEnchanting()
			|| player.isCastingNow() || player.isCastingSimultaneouslyNow() || player.isAttackingNow() || player.isTeleporting()
			|| player.isInCombat() || player.isInDuel() || player.isInOlympiadMode() || player.isOnEvent()
			|| player.isInsideZone(ZoneId.SIEGE) || player.isInsideZone(ZoneId.PVP) || (player.getPvpFlag() > 0)
			|| (player.getKarma() > 0) || !player.getAccessLevel().allowTransaction())
		{
			throw new IllegalArgumentException("You cannot use the Auction House in your current state.");
		}
		if ((Config.COMMUNITY_AUCTION_PEACE_ONLY || Config.COMMUNITYBOARD_PEACE_ONLY) && !player.isInsideZone(ZoneId.PEACE))
		{
			throw new IllegalArgumentException("The Auction House can only be used in a peace zone.");
		}
	}

	public boolean canList(Player player, Item item)
	{
		return (item != null) && (item.getOwnerId() == player.getObjectId()) && (item.getItemLocation() == ItemLocation.INVENTORY)
			&& (item.getCount() > 0) && item.isAvailable(player, false, false) && item.isTradeable()
			&& !item.isAugmented() && !item.isQuestItem() && !item.isShadowItem() && !item.isTimeLimitedItem() && (item.getVisualItemId() == 0)
			&& !item.isEquipped() && (item.getId() != Inventory.ADENA_ID)
			&& (!item.isStackable() || ((item.getEnchantLevel() == 0) && (item.getCustomType1() == 0) && (item.getCustomType2() == 0)));
	}

	public synchronized List<AuctionListing> listings(int ownerId)
	{
		final long now = System.currentTimeMillis();
		return entries.values().stream().filter(e -> (ownerId == 0 || e.ownerId == ownerId) && e.expiresAt > now)
			.map(this::snapshot).toList();
	}

	public synchronized AuctionListing listing(int id)
	{
		final Entry entry = active(id);
		return snapshot(entry);
	}

	private AuctionListing snapshot(Entry e)
	{
		return describe(e.id, e.ownerId, e.item, e.quantity, e.price, e.expiresAt);
	}

	public static AuctionListing describe(int id, int ownerId, Item item, long quantity, long price, long expiresAt)
	{
		final ItemTemplate template = item.getTemplate();
		final boolean jewelry = (template.getBodyPart() & (ItemTemplate.SLOT_NECK | ItemTemplate.SLOT_L_EAR | ItemTemplate.SLOT_R_EAR | ItemTemplate.SLOT_L_FINGER | ItemTemplate.SLOT_R_FINGER)) != 0;
		final String category = item.isWeapon() ? "Weapons" : jewelry ? "Jewelry" : item.isArmor() ? "Armor" : "Other";
		return new AuctionListing(id, ownerId, item.getObjectId(), item.getId(), template.getName(), template.getIcon(), category,
			template.getCrystalType().name(), item.getEnchantLevel(), quantity, price, expiresAt);
	}

	public synchronized long listingFee(long quantity, long price, int days)
	{
		return AuctionHouseRules.fee(quantity, price, days, Config.COMMUNITY_AUCTION_FEE_PERCENT, Config.COMMUNITY_AUCTION_MIN_FEE);
	}

	public synchronized void create(Player player, int objectId, long quantity, long price, int days, long confirmedFee)
	{
		checkAccess(player);
		final long fee = listingFee(quantity, price, days);
		if (fee != confirmedFee) { throw new IllegalArgumentException("The listing fee changed. Please confirm again."); }
		if (entries.values().stream().filter(e -> e.ownerId == player.getObjectId()).count() >= Config.COMMUNITY_AUCTION_MAX_LISTINGS)
		{
			throw new IllegalArgumentException("You have reached the listing limit.");
		}
		final Item source = player.checkItemManipulation(objectId, quantity, "auction");
		if (!canList(player, source)) { throw new IllegalArgumentException("This item cannot be listed."); }
		final Item adena = player.getInventory().getItemByItemId(Inventory.ADENA_ID);
		if ((fee > 0) && ((adena == null) || (adena.getCount() < fee))) { throw new IllegalArgumentException("Not enough Adena for the listing fee."); }
		synchronized (adena == null ? player.getInventory() : adena)
		{
			synchronized (source)
			{
				checkAccess(player);
				if ((player.checkItemManipulation(objectId, quantity, "auction") != source) || !canList(player, source))
				{
					throw new IllegalArgumentException("The inventory item changed. Please try again.");
				}
				final long before = player.getAdena();
				final long sourceBefore = source.getCount();
				Reservation op = null;
				try
				{
					source.updateDatabase(true);
					verify(source);
					forceAdena(player);
					store.verifyAdena(player.getObjectId(), before);
					final long expiresAt = Math.addExact(System.currentTimeMillis(), days * 86_400_000L);
					op = store.create(player.getObjectId(), source, quantity, price, expiresAt, fee, before);
					if ((fee > 0) && !player.reduceAdena(ItemProcessType.FEE, fee, null, true))
					{
						store.verifyAdena(player.getObjectId(), before);
						store.finish(op, objectId, 0, "CANCELLED");
						throw new RejectedOperation("Not enough Adena for the listing fee.");
					}
					forceAdena(player);
					store.verifyAdena(player.getObjectId(), before - fee);
					store.checkpoint(op, "FEE_PAID", 0, 0);
					final AuctionEscrow escrow = new AuctionEscrow(player.getObjectId(), op.listingId());
					final Item held = player.getInventory().transferItem(ItemProcessType.TRANSFER, objectId, quantity, escrow, player, null);
					if (held == null)
					{
						store.verifyItem(objectId, player.getObjectId(), source.getId(), sourceBefore, ItemLocation.INVENTORY, source.getLocationSlot());
						refund(player, fee, before, op);
						store.finish(op, objectId, 0, "CANCELLED");
						throw new RejectedOperation("The item could not be transferred. The listing fee was returned.");
					}
					held.updateDatabase(true);
					verify(held);
					if ((held.getCount() != quantity) || (held.getId() != source.getId()) || (held.getOwnerId() != player.getObjectId())
						|| (held.getItemLocation() != ItemLocation.AUCTION) || (held.getLocationSlot() != op.listingId())) { throw new SQLException("Escrow identity or quantity mismatch"); }
					// A complete non-stackable transfer preserves the original object ID.
					if (held != source) { store.verifyItem(objectId, player.getObjectId(), source.getId(), sourceBefore - quantity, ItemLocation.INVENTORY, source.getLocationSlot()); }
					store.checkpoint(op, "ESCROW_STORED", held.getObjectId(), 0);
					store.finish(op, held.getObjectId(), quantity, "ACTIVE");
					entries.put(op.listingId(), new Entry(op.listingId(), player.getObjectId(), price, expiresAt, held, escrow));
					LOGGER.info("AuctionHouseManager: Listing " + op.listingId() + " created by " + player.getName() + ".");
				}
				catch (RejectedOperation e) { throw e; }
				catch (SQLException | RuntimeException e) { throw failure(op, e); }
				finally { refresh(player); }
			}
		}
	}

	public synchronized void buy(Player buyer, int listingId, long quantity, long confirmedUnitPrice)
	{
		checkAccess(buyer);
		final Entry entry = active(listingId);
		if ((entry.ownerId == buyer.getObjectId()) || (entry.price != confirmedUnitPrice)) { throw new IllegalArgumentException("This listing cannot be purchased."); }
		final long total = AuctionHouseRules.total(quantity, entry.price);
		if (total > Config.MAX_ADENA) { throw new IllegalArgumentException("Purchase total exceeds the Adena limit. Select a smaller quantity."); }
		if (quantity > entry.quantity) { throw new IllegalArgumentException("The available quantity changed. Please confirm again."); }
		final Item adena = buyer.getInventory().getItemByItemId(Inventory.ADENA_ID);
		if ((adena == null) || (adena.getCount() < total)) { throw new IllegalArgumentException("Not enough Adena."); }
		synchronized (adena)
		{
			synchronized (entry.item)
			{
				checkAccess(buyer);
				active(listingId);
				checkCapacity(buyer, entry.item, quantity);
				final long before = buyer.getAdena();
				final Item previous = entry.item.isStackable() ? buyer.getInventory().getItemByItemId(entry.item.getId()) : null;
				final long targetBefore = previous == null ? 0 : previous.getCount();
				Math.addExact(targetBefore, quantity);
				Reservation op = null;
				try
				{
					verify(entry.item);
					forceAdena(buyer);
					store.verifyAdena(buyer.getObjectId(), before);
					op = store.reserve(entry.id, "BUY", buyer.getObjectId(), entry.item, quantity, total, before);
					if (!buyer.reduceAdena(ItemProcessType.BUY, total, null, true))
					{
						store.verifyAdena(buyer.getObjectId(), before);
						store.finish(op, entry.item.getObjectId(), entry.quantity, "ACTIVE");
						throw new RejectedOperation("Not enough Adena.");
					}
					forceAdena(buyer);
					store.verifyAdena(buyer.getObjectId(), before - total);
					store.checkpoint(op, "CHARGED", 0, 0);
					final Item delivered = entry.escrow.transferItem(ItemProcessType.BUY, entry.item.getObjectId(), quantity, buyer.getInventory(), buyer, null);
					if (delivered == null)
					{
						verify(entry.item);
						if (entry.item.getCount() != entry.quantity) { throw new SQLException("Escrow changed during failed transfer"); }
						refund(buyer, total, before, op);
						store.finish(op, entry.item.getObjectId(), entry.quantity, "ACTIVE");
						throw new RejectedOperation("The item could not be transferred. Your Adena was returned.");
					}
					delivered.updateDatabase(true);
					if ((delivered.getCount() != Math.addExact(targetBefore, quantity)) || (delivered.getId() != entry.item.getId())
						|| (delivered.getOwnerId() != buyer.getObjectId()) || (delivered.getItemLocation() != ItemLocation.INVENTORY)) { throw new SQLException("Delivered item identity or quantity mismatch"); }
					verify(delivered);
					final long remaining = entry.quantity - quantity;
					if (delivered != entry.item) { store.verifyItem(entry.item.getObjectId(), entry.ownerId, entry.item.getId(), remaining, ItemLocation.AUCTION, entry.id); }
					store.checkpoint(op, "DELIVERED", delivered.getObjectId(), 0);
					paySeller(entry, quantity, total, op);
					store.finish(op, entry.item.getObjectId(), remaining, remaining == 0 ? "SOLD" : "ACTIVE");
					entry.quantity = remaining;
					if (remaining == 0) { entries.remove(entry.id); }
					LOGGER.info("AuctionHouseManager: Listing " + entry.id + " sold " + quantity + " from " + playerName(entry.ownerId) + " to " + buyer.getName() + "; operation " + op.operationId() + ".");
					notifyReceived(buyer, delivered.getId(), quantity);
				}
				catch (RejectedOperation e) { throw e; }
				catch (SQLException | RuntimeException e) { throw failure(op, e); }
				finally { refresh(buyer); }
			}
		}
	}

	public synchronized void cancel(Player player, int listingId)
	{
		checkAccess(player);
		final Entry entry = active(listingId);
		if (entry.ownerId != player.getObjectId()) { throw new IllegalArgumentException("You do not own this listing."); }
		returnItem(entry, "CANCELLED");
	}

	private void paySeller(Entry entry, long quantity, long amount, Reservation op) throws SQLException
	{
		final Message message = new Message(entry.ownerId, "Auction House sale", "Listing #" + entry.id + ": " + quantity + " " + entry.item.getTemplate().getName() + " sold for " + amount + " Adena. Operation: " + op.operationId(), MessageSenderType.AUCTION_MANAGER);
		store.checkpoint(op, "PAYMENT_RESERVED", 0, message.getId());
		final Mail mail = message.createAttachments();
		if (mail == null) { throw new IllegalStateException("Could not create payment attachments"); }
		final Item payment = mail.addItem(ItemProcessType.TRANSFER, Inventory.ADENA_ID, amount, null, null);
		if ((payment == null) || (payment.getCount() != amount)) { throw new IllegalStateException("Could not create payment item"); }
		payment.updateDatabase(true);
		verify(payment);
		MailManager.getInstance().sendMessage(message);
		store.verifyMessage(message.getId(), entry.ownerId);
		store.checkpoint(op, "SELLER_PAID", 0, message.getId());
	}

	private void returnItem(Entry entry, String reason)
	{
		synchronized (entry.item)
		{
			Reservation op = null;
			try
			{
				verify(entry.item);
				op = store.reserve(entry.id, reason, entry.ownerId, entry.item, entry.quantity, 0, 0);
				final Message message = new Message(entry.ownerId, "Auction House return", "Listing #" + entry.id + " was " + reason.toLowerCase(java.util.Locale.ROOT) + ". The unsold items are attached. Listing fees are not refunded. Operation: " + op.operationId(), MessageSenderType.AUCTION_MANAGER);
				store.checkpoint(op, "RETURN_RESERVED", 0, message.getId());
				final Mail mail = message.createAttachments();
				if (mail == null) { throw new IllegalStateException("Could not create return attachments"); }
				final Item returned = entry.escrow.transferItem(ItemProcessType.TRANSFER, entry.item.getObjectId(), entry.quantity, mail, null, null);
				if ((returned == null) || (returned.getCount() != entry.quantity) || (returned.getId() != entry.item.getId())
					|| (returned.getItemLocation() != ItemLocation.MAIL) || (returned.getLocationSlot() != message.getId())) { throw new IllegalStateException("Could not return escrow item"); }
				returned.updateDatabase(true);
				verify(returned);
				store.checkpoint(op, "RETURN_STORED", returned.getObjectId(), message.getId());
				MailManager.getInstance().sendMessage(message);
				store.verifyMessage(message.getId(), entry.ownerId);
				store.finish(op, returned.getObjectId(), 0, reason);
				entries.remove(entry.id);
				LOGGER.info("AuctionHouseManager: Listing " + entry.id + " " + reason.toLowerCase(java.util.Locale.ROOT) + "; items returned by mail to " + playerName(entry.ownerId) + ".");
			}
			catch (SQLException | RuntimeException e) { throw failure(op, e); }
		}
	}

	private synchronized void expire()
	{
		if (!ready) { return; }
		for (Entry entry : new ArrayList<>(entries.values()))
		{
			if (!ready) { break; }
			if (entry.expiresAt <= System.currentTimeMillis())
			{
				try { returnItem(entry, "EXPIRED"); }
				catch (RuntimeException e) { LOGGER.warning("AuctionHouseManager: Expiration of listing " + entry.id + " was interrupted; see reconciliation log."); }
			}
		}
	}

	private Entry active(int id)
	{
		final Entry entry = entries.get(id);
		if ((entry == null) || (entry.expiresAt <= System.currentTimeMillis())) { throw new IllegalArgumentException("This listing is no longer available."); }
		if ((entry.item.getCount() != entry.quantity) || (entry.item.getOwnerId() != entry.ownerId) || (entry.item.getItemLocation() != ItemLocation.AUCTION)
			|| (entry.item.getLocationSlot() != entry.id) || (entry.escrow.getItemByObjectId(entry.item.getObjectId()) != entry.item))
		{
			throw failure(new Reservation(id, "escrow-validation"), new IllegalStateException("Escrow identity changed"));
		}
		return entry;
	}

	private void checkCapacity(Player player, Item item, long quantity)
	{
		final long weight = Math.multiplyExact(quantity, item.getTemplate().getWeight());
		final int slots = item.isStackable() && player.getInventory().getItemByItemId(item.getId()) != null ? 0 : 1;
		if ((weight > Integer.MAX_VALUE) || !player.getInventory().validateWeight((int) weight) || !player.getInventory().validateCapacity(slots))
		{
			throw new IllegalArgumentException("Not enough inventory capacity or weight available.");
		}
	}

	private void refund(Player player, long amount, long expected, Reservation op) throws SQLException
	{
		store.checkpoint(op, "REFUND_RESERVED", 0, 0);
		if (amount > 0) { player.getInventory().addItem(ItemProcessType.REFUND, Inventory.ADENA_ID, amount, player, null); }
		forceAdena(player);
		store.verifyAdena(player.getObjectId(), expected);
		store.checkpoint(op, "REFUNDED", 0, 0);
	}

	private void verify(Item item) throws SQLException
	{
		store.verifyItem(item.getObjectId(), item.getOwnerId(), item.getId(), item.getCount(), item.getItemLocation(), item.getLocationSlot());
	}

	private static void forceAdena(Player player)
	{
		final Item adena = player.getInventory().getItemByItemId(Inventory.ADENA_ID);
		if (adena != null) { adena.updateDatabase(true); }
	}

	private IllegalArgumentException failure(Reservation op, Exception e)
	{
		if (op == null)
		{
			// A commit acknowledgement can fail even if the reservation was committed. Stop until restart.
			ready = false;
			LOGGER.log(Level.SEVERE, "AuctionHouseManager: Database operation failed; service stopped for reconciliation.", e);
		}
		else
		{
			entries.remove(op.listingId());
			try { store.review(op.listingId()); }
			catch (SQLException ex) { ready = false; LOGGER.log(Level.SEVERE, "AuctionHouseManager: Could not quarantine listing " + op.listingId(), ex); }
			LOGGER.log(Level.SEVERE, "AuctionHouseManager: Listing " + op.listingId() + ", operation " + op.operationId() + " requires reconciliation. Automatic retry is blocked.", e);
		}
		return new IllegalArgumentException("The operation needs administrator verification. Please report this listing; do not repeat the payment.");
	}

	private static String playerName(int objectId)
	{
		final String name = CharInfoTable.getInstance().getNameById(objectId);
		return name == null ? "Unknown" : name;
	}

	private static void notifyReceived(Player buyer, int itemId, long quantity)
	{
		try
		{
			final SystemMessage received = new SystemMessage(quantity > 1 ? SystemMessageId.YOU_HAVE_OBTAINED_S2_S1 : SystemMessageId.YOU_HAVE_OBTAINED_S1);
			received.addItemName(itemId);
			if (quantity > 1) { received.addLong(quantity); }
			buyer.sendPacket(received);
		}
		catch (RuntimeException e)
		{
			// A notification failure must not reopen or quarantine an already committed sale.
			LOGGER.log(Level.WARNING, "AuctionHouseManager: Could not notify " + buyer.getName() + " of the completed purchase.", e);
		}
	}

	private static void refresh(Player player)
	{
		player.sendPacket(new ItemList(player, false));
		final StatusUpdate update = new StatusUpdate(player);
		update.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(update);
	}

	public static AuctionHouseManager getInstance() { return SingletonHolder.INSTANCE; }
	private static class SingletonHolder
	{
		private static final AuctionHouseManager INSTANCE = new AuctionHouseManager(new AuctionHouseStore());
	}
}
