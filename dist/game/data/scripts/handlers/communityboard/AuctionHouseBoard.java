package handlers.communityboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.l2journey.gameserver.auctionhouse.AuctionHouseManager;
import com.l2journey.gameserver.auctionhouse.AuctionHouseRules;
import com.l2journey.gameserver.auctionhouse.AuctionListing;
import com.l2journey.gameserver.cache.HtmCache;
import com.l2journey.gameserver.data.sql.CharInfoTable;
import com.l2journey.gameserver.handler.CommunityBoardHandler;
import com.l2journey.gameserver.handler.IParseBoardHandler;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.instance.Item;

/** Native High Five board HTML. Each financial action requires an expiring, one-use server quote. */
public class AuctionHouseBoard implements IParseBoardHandler
{
	private static final int PAGE_SIZE = 7;
	private static final String ROOT = "_bbsauction";
	private static final List<String> CATEGORIES = List.of("All", "Weapons", "Armor", "Jewelry", "Other");
	private static final List<String> SORTS = List.of("Newest", "Price", "Name");
	private static final Map<Integer, Quote> QUOTES = new ConcurrentHashMap<>();
	private record Quote(String token, String kind, int id, long quantity, long price, int days, long fee, long deadline) { }

	@Override public String[] getCommunityBoardCommands() { return new String[] { ROOT }; }

	@Override
	public boolean parseCommunityBoardCommand(String command, Player player)
	{
		final AuctionHouseManager manager = AuctionHouseManager.getInstance();
		try
		{
			manager.checkAccess(player);
			if ((command == null) || (command.length() > 256)) { throw new IllegalArgumentException("Invalid Auction House request."); }
			if (!player.getClient().getFloodProtectors().canPerformTransaction()) { return true; }
			final String[] args = command.split(";", -1);
			for (int i = 0; i < args.length; i++) { args[i] = args[i].trim(); }
			if (!ROOT.equals(args[0])) { throw new IllegalArgumentException("Invalid Auction House request."); }
			final String page;
			if (args.length == 1)
			{
				page = market(manager, player, "market", "All", "", 0, "Newest");
			}
			else
			{
				page = switch (args[1])
				{
					case "list" ->
					{
						require(args, 7);
						yield market(manager, player, args[2], args[3], AuctionHouseRules.search(args[4]), number(args[5]), args[6]);
					}
					case "inventory" ->
					{
						require(args, 3);
						yield inventory(manager, player, number(args[2]));
					}
					case "sell" ->
					{
						require(args, 3);
						yield sell(manager, player, number(args[2]));
					}
					case "quote" ->
					{
						require(args, 6);
						final int id = number(args[2]);
						final long quantity = positive(args[3]);
						final long price = positive(args[4]);
						final int days = number(args[5]);
						final long fee = manager.listingFee(quantity, price, days);
						final Item item = player.checkItemManipulation(id, quantity, "auction");
						if (!manager.canList(player, item)) { throw new IllegalArgumentException("This item cannot be listed."); }
						yield confirm(player, "CREATE", id, quantity, price, days, fee, "List " + quantity + " " + escape(item.getTemplate().getName()) + " for " + amount(price) + " Adena each, for " + days + " day(s)?<br>Listing fee: <font color=LEVEL>" + amount(fee) + " Adena</font>.<br>This fee is not refunded on cancellation or expiration.");
					}
					case "view" ->
					{
						require(args, 3);
						yield detail(manager.listing(number(args[2])), player);
					}
					case "buy" ->
					{
						require(args, 4);
						final AuctionListing listing = manager.listing(number(args[2]));
						final long quantity = positive(args[3]);
						final long total = AuctionHouseRules.total(quantity, listing.unitPrice());
						if ((quantity > listing.quantity()) || (listing.ownerId() == player.getObjectId())) { throw new IllegalArgumentException("Invalid purchase quantity or owner."); }
						yield confirm(player, "BUY", listing.id(), quantity, listing.unitPrice(), 0, 0, "Buy " + quantity + " " + escape(listing.displayName()) + "?<br>Total: <font color=LEVEL>" + amount(total) + " Adena</font>.");
					}
					case "cancel" ->
					{
						require(args, 3);
						final AuctionListing listing = manager.listing(number(args[2]));
						if (listing.ownerId() != player.getObjectId()) { throw new IllegalArgumentException("You do not own this listing."); }
						yield confirm(player, "CANCEL", listing.id(), listing.quantity(), listing.unitPrice(), 0, 0, "Cancel listing #" + listing.id() + "?<br>Unsold items will be returned by mail.<br>The listing fee is not refunded.");
					}
					case "confirm" ->
					{
						require(args, 3);
						final Quote quote = QUOTES.get(player.getObjectId());
						if ((quote == null) || !quote.token().equals(args[2]) || (quote.deadline() < System.currentTimeMillis()) || !QUOTES.remove(player.getObjectId(), quote))
						{
							throw new IllegalArgumentException("This confirmation expired or was already used. Please open the listing again.");
						}
						switch (quote.kind())
						{
							case "CREATE" -> manager.create(player, quote.id(), quote.quantity(), quote.price(), quote.days(), quote.fee());
							case "BUY" -> manager.buy(player, quote.id(), quote.quantity(), quote.price());
							case "CANCEL" -> manager.cancel(player, quote.id());
							default -> throw new IllegalArgumentException("Invalid confirmation.");
						}
						yield message("Operation completed. Seller payments and returned items are sent by mail.");
					}
					default -> throw new IllegalArgumentException("Invalid Auction House request.");
				};
			}
			send(player, page);
		}
		catch (IllegalArgumentException | ArithmeticException e)
		{
			player.sendMessage(e.getMessage());
			send(player, message(escape(e.getMessage())));
		}
		return true;
	}

	private String market(AuctionHouseManager manager, Player player, String scope, String category, String query, int page, String sort)
	{
		if (!List.of("market", "mine").contains(scope) || !CATEGORIES.contains(category) || !SORTS.contains(sort)) { throw new IllegalArgumentException("Invalid filter."); }
		final String lower = query.toLowerCase(Locale.ROOT);
		final Comparator<AuctionListing> order = switch (sort)
		{
			case "Price" -> Comparator.comparingLong(AuctionListing::unitPrice).thenComparingInt(AuctionListing::id);
			case "Name" -> Comparator.comparing(AuctionListing::name).thenComparingInt(AuctionListing::id);
			default -> Comparator.comparingInt(AuctionListing::id).reversed();
		};
		final List<AuctionListing> listings = manager.listings("mine".equals(scope) ? player.getObjectId() : 0).stream()
			.filter(l -> ("All".equals(category) || l.category().equals(category)) && (lower.isEmpty() || l.name().toLowerCase(Locale.ROOT).contains(lower) || Integer.toString(l.itemId()).equals(query)))
			.sorted(order).toList();
		page = clampPage(page, listings.size());
		final StringBuilder html = new StringBuilder(6000);
		html.append("<table width=680 cellpadding=2 cellspacing=0><tr><td width=120><combobox var=category width=115 list=\"").append(options(CATEGORIES, category)).append("\"></td>")
			.append("<td width=210><edit var=query width=200 height=18 length=32></td><td width=120><combobox var=sort width=115 list=\"").append(options(SORTS, sort)).append("\"></td><td width=110>")
			.append(button("Search", ROOT + ";list;" + scope + "; $category ; $query ;0; $sort ", 100)).append("</td><td width=120>").append(button("Clear", ROOT + ";list;" + scope + ";All;;0;Newest", 100)).append("</td></tr></table><br1>")
			.append("<font color=AE9977>").append("mine".equals(scope) ? "My listings" : "Market").append(" - ").append(listings.size()).append(" listing(s)")
			.append(query.isEmpty() ? "" : " - Search: " + escape(query)).append("</font><br1>")
			.append("<table width=680 cellpadding=4 cellspacing=0 border=0><tr><td width=330>Item</td><td width=100>Available</td><td width=140>Unit price</td><td width=110>Action</td></tr>");
		for (AuctionListing listing : slice(listings, page))
		{
			html.append("<tr><td width=330 height=44 bgcolor=1A1A1A>").append(itemCell(listing)).append("</td><td width=100 bgcolor=1A1A1A>").append(amount(listing.quantity()))
				.append("</td><td width=140 bgcolor=1A1A1A><font color=LEVEL>").append(amount(listing.unitPrice())).append("</font></td><td width=110 bgcolor=1A1A1A>")
				.append(button("Details", ROOT + ";view;" + listing.id(), 100)).append("</td></tr>");
		}
		if (listings.isEmpty()) { html.append("<tr><td width=330 height=80>No listings match these filters.</td><td></td><td></td><td></td></tr>"); }
		html.append("</table><br>").append(pages(page, listings.size(), ROOT + ";list;" + scope + ";" + category + ";" + query + ";", ";" + sort));
		return html.toString();
	}

	private String inventory(AuctionHouseManager manager, Player player, int page)
	{
		final List<Item> items = new ArrayList<>(player.getInventory().getItems()).stream().filter(i -> manager.canList(player, i)).sorted(Comparator.comparingInt(Item::getObjectId)).toList();
		page = clampPage(page, items.size());
		final StringBuilder html = new StringBuilder("<font color=AE9977>Select an unequipped, tradeable item. Prices and fees are in Adena.</font><br><table width=680 cellpadding=4 cellspacing=0 border=0>");
		for (Item item : slice(items, page))
		{
			final AuctionListing listing = AuctionHouseManager.describe(0, player.getObjectId(), item, item.getCount(), 0, 0);
			html.append("<tr><td width=430 height=44 bgcolor=1A1A1A>").append(itemCell(listing)).append("</td><td width=130 bgcolor=1A1A1A>").append(amount(item.getCount()))
				.append("</td><td width=120 bgcolor=1A1A1A>").append(button("Sell", ROOT + ";sell;" + item.getObjectId(), 100)).append("</td></tr>");
		}
		if (items.isEmpty()) { html.append("<tr><td width=680 height=100>No eligible items in your inventory.</td></tr>"); }
		return html.append("</table><br>").append(pages(page, items.size(), ROOT + ";inventory;", "")).toString();
	}

	private String sell(AuctionHouseManager manager, Player player, int objectId)
	{
		final Item item = player.getInventory().getItemByObjectId(objectId);
		if (!manager.canList(player, item)) { throw new IllegalArgumentException("This item cannot be listed."); }
		return "<br>" + itemCell(AuctionHouseManager.describe(0, player.getObjectId(), item, item.getCount(), 0, 0))
			+ "<br><table width=400 cellpadding=5 cellspacing=0><tr><td width=200>Quantity (available: " + amount(item.getCount()) + ")</td><td width=200><edit var=quantity width=180 height=18 type=number length=7></td></tr>"
			+ "<tr><td width=200>Unit price in Adena</td><td width=200><edit var=price width=180 height=18 length=14></td></tr>"
			+ "<tr><td width=200>Duration in days</td><td width=200><combobox var=days width=180 list=\"1;2;3;4;5;6;7;8;9\"></td></tr></table><br>"
			+ button("Review listing", ROOT + ";quote;" + objectId + "; $quantity ; $price ; $days ", 160)
			+ "<br><font color=AE9977>The exact listing fee is shown before confirmation.</font>";
	}

	private String detail(AuctionListing listing, Player player)
	{
		final String seller = CharInfoTable.getInstance().getNameById(listing.ownerId());
		final long minutes = Math.max(1, (listing.expiresAt() - System.currentTimeMillis() + 59_999) / 60_000);
		final StringBuilder html = new StringBuilder("<br>").append(itemCell(listing)).append("<br><table width=450 cellpadding=5 cellspacing=0>")
			.append("<tr><td width=160>Listing / Item ID</td><td width=290>#").append(listing.id()).append(" / ").append(listing.itemId()).append("</td></tr>")
			.append("<tr><td>Seller</td><td>").append(escape(seller == null ? "Unknown" : seller)).append("</td></tr>")
			.append("<tr><td>Available</td><td>").append(amount(listing.quantity())).append("</td></tr><tr><td>Unit price</td><td><font color=LEVEL>")
			.append(amount(listing.unitPrice())).append(" Adena</font></td></tr><tr><td>Time remaining</td><td>").append(minutes).append(" minute(s)</td></tr></table><br>");
		if (listing.ownerId() == player.getObjectId())
		{
			html.append(button("Cancel listing", ROOT + ";cancel;" + listing.id(), 160));
		}
		else
		{
			html.append("<table width=360 cellpadding=4 cellspacing=0><tr><td width=180>Quantity to purchase</td><td width=180><edit var=quantity width=150 height=18 type=number length=7></td></tr></table><br>")
				.append(button("Review purchase", ROOT + ";buy;" + listing.id() + "; $quantity ", 160));
		}
		return html.toString();
	}

	private String confirm(Player player, String kind, int id, long quantity, long price, int days, long fee, String text)
	{
		final long now = System.currentTimeMillis();
		QUOTES.entrySet().removeIf(e -> e.getValue().deadline() < now);
		final Quote quote = new Quote(UUID.randomUUID().toString(), kind, id, quantity, price, days, fee, now + 120_000);
		QUOTES.put(player.getObjectId(), quote);
		return "<br><br><table width=580 cellpadding=8 cellspacing=0 bgcolor=1A1A1A><tr><td width=580 align=center>" + text + "</td></tr></table><br>"
			+ "<table width=340 cellpadding=4 cellspacing=0><tr><td width=170 align=center>" + button("Confirm", ROOT + ";confirm;" + quote.token(), 150)
			+ "</td><td width=170 align=center>" + button("Back", ROOT, 150) + "</td></tr></table>";
	}

	private static String itemCell(AuctionListing item)
	{
		final String icon = item.icon() != null && item.icon().matches("[A-Za-z0-9_.]+") ? item.icon() : "icon.etc_question_mark_i00";
		return "<table width=310 cellpadding=0 cellspacing=0 border=0><tr><td width=40><img src=\"" + icon + "\" width=32 height=32></td><td width=270>"
			+ "<font color=F2C75C>" + escape(shorten(item.displayName(), 34)) + "</font><br1><font color=AE9977>" + item.category() + " - " + escape(item.grade()) + "</font></td></tr></table>";
	}

	private static String pages(int page, int count, String prefix, String suffix)
	{
		final int total = Math.max(1, (count + PAGE_SIZE - 1) / PAGE_SIZE);
		return "<table width=340 cellpadding=2 cellspacing=0><tr><td width=100 align=center>" + (page > 0 ? button("Previous", prefix + (page - 1) + suffix, 95) : "")
			+ "</td><td width=140 align=center>Page " + (page + 1) + " / " + total + "</td><td width=100 align=center>" + (page + 1 < total ? button("Next", prefix + (page + 1) + suffix, 95) : "") + "</td></tr></table>";
	}

	private static int clampPage(int page, int count) { return Math.min(page, Math.max(0, (count - 1) / PAGE_SIZE)); }
	private static <T> List<T> slice(List<T> values, int page) { return values.subList(page * PAGE_SIZE, Math.min(values.size(), (page + 1) * PAGE_SIZE)); }
	private static String options(List<String> values, String selected) { return selected + ";" + String.join(";", values.stream().filter(v -> !v.equals(selected)).toList()); }
	private static void require(String[] values, int count) { if (values.length != count) { throw new IllegalArgumentException("Invalid Auction House request."); } }
	private static int number(String text)
	{
		final long value = positiveOrZero(text);
		if (value > Integer.MAX_VALUE) { throw new IllegalArgumentException("Invalid numeric value."); }
		return (int) value;
	}
	private static long positive(String text)
	{
		// High Five numeric edits can submit thousands separators (e.g. 20,000,000).
		// Validate the grouping before removing commas; malformed/decimal input stays invalid.
		String value = text.trim();
		if (value.contains(","))
		{
			if (!value.matches("[1-9][0-9]{0,2}(,[0-9]{3})+"))
			{
				throw new IllegalArgumentException("Enter a valid numeric value.");
			}
			value = value.replace(",", "");
		}
		final long n = positiveOrZero(value);
		if (n == 0) { throw new IllegalArgumentException("Enter a value greater than zero."); }
		return n;
	}
	private static long positiveOrZero(String text)
	{
		if (!text.trim().matches("[0-9]{1,18}")) { throw new IllegalArgumentException("Enter a valid numeric value."); }
		return Long.parseLong(text.trim());
	}
	private static String amount(long n) { return String.format(Locale.US, "%,d", n); }
	private static String shorten(String text, int max) { return text.length() > max ? text.substring(0, max - 3) + "..." : text; }
	private static String escape(String text) { return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;"); }
	private static String button(String label, String action, int width)
	{
		return "<button value=\"" + label + "\" action=\"bypass " + action + "\" width=" + width + " height=25 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">";
	}
	private static String message(String text) { return "<br><br><table width=580 cellpadding=8 cellspacing=0><tr><td width=580 align=center>" + text + "</td></tr></table>"; }
	private static void send(Player player, String content)
	{
		final String template = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/auction/main.html");
		final String html = template == null ? "<html><body><center>Auction House HTML is missing.</center></body></html>" : template.replace("%content%", content);
		CommunityBoardHandler.separateAndSend(html, player);
	}
}
