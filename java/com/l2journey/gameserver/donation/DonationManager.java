package com.l2journey.gameserver.donation;

import java.util.logging.Level;

import com.google.zxing.WriterException;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.l2journey.gameserver.donation.util.Pagination;
import com.l2journey.gameserver.donation.util.DonationStrings;
import java.util.logging.Logger;

import java.util.Arrays;

import com.l2journey.gameserver.donation.paymenthandlers.MercadoPago;
import com.l2journey.gameserver.donation.paymenthandlers.binance.Binance;
import com.l2journey.gameserver.donation.paymenthandlers.paypal.PayPal;
import com.l2journey.gameserver.donation.purchase.Purchase;
import com.l2journey.gameserver.donation.purchase.PaymentMethod;
import com.l2journey.gameserver.donation.purchase.PurchaseStatus;
import com.l2journey.gameserver.donation.tasks.CryptoCurrencyTask;
import com.l2journey.gameserver.donation.tasks.DonationAutomaticPaymentTask;
import com.l2journey.gameserver.donation.tasks.DonationTaskManager;
import com.l2journey.gameserver.cache.HtmCache;

import com.l2journey.gameserver.data.xml.ItemData;

import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.network.serverpackets.ActionFailed;
import com.l2journey.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2journey.gameserver.network.serverpackets.PledgeCrest;
import com.l2journey.gameserver.network.serverpackets.ShowCalculator;
import com.l2journey.gameserver.network.serverpackets.TutorialCloseHtml;
import com.l2journey.gameserver.network.serverpackets.TutorialShowHtml;

public class DonationManager
{
	private static final Logger LOGGER = Logger.getLogger(DonationManager.class.getName());

	private static final int HTML_ID = 9999;
	private static final String HTML_PATH = "data/html/mods/donation/";
	
	private static final String HTML_EMPTY_TABLE = "<table><tr><td></td></tr><tr><td width=70>---</td><td width=115>---</td><td width=50>---</td><td width=75>---</td></tr><tr><td></td></tr></table>";
	private static final String HTML_LINE = "<img src=L2UI.SquareGray width=280 height=1>";
	public static final String HTML_PURCHASE_MESSAGE = "<br><table bgcolor=000000 cellpadding=4><tr><td align=center width=280><font color=ff0000>%s</font></td></tr><tr><td>%s</td></tr></table>";
	private static final String HTML_BANNER = "<tr><td></td><td><table bgcolor=ff0000><tr><td width=265 align=center>%s</td></tr></table></td></tr>";
	private static final String HTML_CHOICE_CHECKBOX = "<td fixwidth=1><button action=\"bypass donation %s_selector %s\" width=14 height=14 back=L2UI.control.checkBox_%s fore=L2UI.control.checkBox_%3$s></td><td align=left width=%d valign=top>%s</td>";
	private static final String HTML_CALCULATOR = "<tr><td align=center><a action=\"bypass donation calculator\">Calculadora</a></td></tr>";
	private static final String HTML_CHOICE_CLEAR = "<td align=center><button value=\"-\" action=\"bypass donation clear_selectors\" width=25 height=17 back=sek.cbui94 fore=sek.cbui92></td>";
	private static final String HTML_INPUT_BOX = "<edit var=\"Quantity\" type=number width=50 height=13 length=3>";
	private static final String HTML_SEARCH_CRYPTO = "<td width=14><button action=\"bypass donation search\" width=14 height=14 back=L2UI_ch3.QuestWndPlusBtn_over fore=L2UI_ch3.QuestWndPlusBtn></td>";
	private static final String HTML_QRCODE_BUTTON = "<td><button value=\"QR Code\" action=\"bypass donation qrcode %id%\" width=75 height=21 back=L2UI_ch3.Btn1_normalOn fore=L2UI_ch3.Btn1_normal></td>";
	private static final String HTML_LINK_BUTTON = "<td><button value=\"Link\" action=\"bypass donation link %id% false\" width=75 height=21 back=L2UI_ch3.Btn1_normalOn fore=L2UI_ch3.Btn1_normal></td>";
	private static final String HTML_CHECK_BUTTON = "<td><button value=\"Paguei\" action=\"bypass -h donation check %id%\" width=75 height=21 back=L2UI_ch3.Btn1_normalOn fore=L2UI_ch3.Btn1_normal></td>";
	private static final String HTML_REFRESH_BUTTON = "<td><table><tr><td><button action=\"bypass -h donation index\" width=14 height=14 back=L2UI_ch3.ShortcutWnd.shortcut_rotate_over fore=L2UI_ch3.ShortcutWnd.shortcut_rotate></td></tr></table></td>";

	private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@(.+)$";
	private static final String CURRENCY_CODE_PATERN = "^[a-zA-Z]{3,6}$";

	private final static Map<Integer, Purchase> _waitingPurchases = new ConcurrentHashMap<>();
	private final static Map<Integer, CheckoutChoice> _checkoutChoice = new ConcurrentHashMap<>();
	private final static Map<String, IPaymentHandler> _paymentHandlers = new HashMap<>();
	
	public DonationManager()
	{
        if (!DonationConfig.DONATION_ENABLED) return;
        registerPaymentHandler("MP", new MercadoPago());
        registerPaymentHandler("PAYPAL", new PayPal());
        registerPaymentHandler("BINANCE", new Binance());
        DonationData.getInstance().restore();
        if (DonationData.getInstance().isReady())
        {
            CurrencyManager.getInstance();
            DonationTaskManager.getInstance();
            DonationAutomaticPaymentTask.getInstance();
        }
	}
	
 private static String icon(int itemId)
 {
  final var item = ItemData.getInstance().getTemplate(itemId);
  return item == null ? "icon.etc_question_mark_i00" : item.getIcon();
 }
 private final Map<Integer, long[]> _requestTimes = new ConcurrentHashMap<>();
 private boolean allowRequest(int playerId, boolean buying)
 {
  final long[] times = _requestTimes.computeIfAbsent(playerId, k -> new long[2]);
  synchronized (times)
  {
   final int slot = buying ? 0 : 1;
   final long now = System.currentTimeMillis();
   if (now - times[slot] < (buying ? DonationConfig.DONATION_PAY_TIME : DonationConfig.DONATION_CHECK_TIME)) return false;
   times[slot] = now;
   return true;
  }
 }

	private static void registerPaymentHandler(String id, IPaymentHandler handler)
	{
		_paymentHandlers.put(id, handler);
	}
	
	public IPaymentHandler getPaymentHandler(String id)
	{
		return _paymentHandlers.get(id);
	}
	
	public void reload()
	{
		_paymentHandlers.values().forEach(IPaymentHandler::reload);
		_checkoutChoice.clear();
		CurrencyManager.getInstance().reload();
	}
	
	public boolean isEnabled()
	{
		if (!DonationData.getInstance().isReady() || !DonationConfig.DONATION_ENABLED || DonationConfig.DONATION_PURCHASABLE_ITEM == 0 || (ItemData.getInstance().getTemplate(DonationConfig.DONATION_PURCHASABLE_ITEM) == null || !ItemData.getInstance().getTemplate(DonationConfig.DONATION_PURCHASABLE_ITEM).isStackable()))
			return false;
		
		if ((DonationConfig.DONATION_MP_PIX || DonationConfig.DONATION_MP_LINK) && DonationConfig.DONATION_MP_TOKEN.isEmpty())
			return false;
		
		if (DonationConfig.DONATION_PAYPAL_LINK && (DonationConfig.DONATION_PAYPAL_CLIENT_ID.isEmpty() || DonationConfig.DONATION_PAYPAL_CLIENT_SECRET.isEmpty() || DonationConfig.DONATION_PAYPAL_ACCOUNT_EMAIL.isEmpty()))
			return false;
		
		if (DonationConfig.DONATION_BINANCE_PAY && (DonationConfig.DONATION_BINANCE_API_KEY.isEmpty() || DonationConfig.DONATION_BINANCE_SECRET_KEY.isEmpty()))
			return false;
		
		return true;
	}
	
	private static CheckoutChoice getChoice(int playerId)
	{
		return _checkoutChoice.computeIfAbsent(playerId, k -> new CheckoutChoice());
	}
	
	public void handleBypass(Player player, String command)
	{
        if (!DonationConfig.DONATION_ENABLED || !DonationData.getInstance().isReady() || player == null || !player.isOnline() || player.isJailed() || command == null || command.length() > 512) return;
        synchronized (player) {
        try { handleCommand(player, command); } catch (RuntimeException e) { LOGGER.log(Level.WARNING, "DonationManager: Donation request could not be processed for player " + player.getObjectId(), e); player.sendMessage("Unable to process your donation request. Please try again later."); }
        }
    }
    private void handleCommand(Player player, String command)
    {
		try
		{
			if (command.startsWith("buyer "))
			{
				handleBuyerDetails(player, command.substring(6));
				return;
			}
			final StringTokenizer st = new StringTokenizer(command, " ");
			final String action = st.nextToken();

			if (action.equals("htm"))
			{
				final String file = st.nextToken();
				if (file.equals("info.htm"))
					showCustomWindow(player, file);
			}
			else if (action.equals("email"))
			{
				final String next = st.nextToken();
				if (next.equals("view"))
				{
					if (_waitingPurchases.containsKey(player.getObjectId()))
						_waitingPurchases.remove(player.getObjectId());

					showEmailWindow(player, player.getVariables().getString("donation_email", null));
				}
				else if (next.equals("set"))
					setPlayerEmail(player, st.hasMoreTokens() ? st.nextToken() : null);
			}
			else if (action.equals("index"))
			{
				if (st.countTokens() == 2)
					showIndexWindow(player, Boolean.valueOf(st.nextToken()), Boolean.valueOf(st.nextToken()));
				else
					showIndexWindow(player);
			}
			else if (action.equals("gateway_selector"))
				getChoice(player.getObjectId()).changeGatewayChoice(player, st.nextToken());
			else if (action.equals("currency_selector"))
				getChoice(player.getObjectId()).changeCurrencyChoice(player, st.nextToken());
			else if (action.equals("custom_selector"))
				getChoice(player.getObjectId()).changeCustom(player, st.nextToken());
			else if (action.equals("clear_selectors"))
			{
				final CheckoutChoice cc = getChoice(player.getObjectId());
				cc.setCurrency(null);
				cc.setGateway(null);
				showIndexWindow(player);
			}
			else if (action.equals("clear_cryptos"))
			{
				getChoice(player.getObjectId()).getCustomCryptos().clear();
				showSearchWindow(player, null, null);
			}
			else if (action.equals("buy"))
				newPurchase(player, st.hasMoreTokens() ? Integer.valueOf(st.nextToken()) : 0);
			else if (action.equals("calculator"))
			{
				if (DonationConfig.DONATION_CALCULATOR)
					player.sendPacket(new ShowCalculator(4393));
			}
			else if (action.equals("search"))
			{
				if (st.hasMoreTokens())
					searchCriptoCurrency(player, st.nextToken().toUpperCase());
				else
					showSearchWindow(player, null, null);
			}
			else
			{
				final int id = Integer.valueOf(st.nextToken());
				if (action.equals("check"))
					checkPaymentStatus(player, id);
				else if (action.equals("status"))
					showPurchaseStatusWindow(player, id);
				else if (action.equals("hide"))
					hidePurchase(player, id);
				else if (action.equals("history"))
					showPurchaseHistoryWindow(player, id);
				else if (action.equals("cancel"))
					cancelPurchase(player, id);
				else if (action.equals("qrcode"))
					showQrCodeWindow(player, id);
				else if (action.equals("link"))
					showLinkWindow(player, id, st.nextToken().equals("true"));
				else if (action.equals("confirm"))
					showConfirmActionWindow(player, id, st.nextToken());
				else if (action.equals("closeqr"))
				{
					if (DonationConfig.DONATION_ENABLED)
					{
						showPurchaseStatusWindow(player, id);
						player.sendPacket(TutorialCloseHtml.STATIC_PACKET);
					}
				}
				else if (action.equals("terms"))
					handleTermsActions(player, id, st.nextToken());	
			}
		}
		catch (Exception e)
		{
			if (!player.isGM())
				player.sendMessage("Invalid donation request. Please reopen the donation window.");
			
			LOGGER.log(Level.WARNING, "DonationManager: Invalid donation request for player " + player.getObjectId(), e);
		}
	}
	
	/**
	 * Entrega a compra feita por um player que estava offline no momento em que os itens seriam entregues
	 * @param player
	 */
	public void offlinePlayer(Player player)
 {
  for (Purchase p : DonationData.getInstance().getPurchases(player.getObjectId()))
   if (p.getStatus() == PurchaseStatus.OFFLINE) deliver(player, p);
 }
 private void deliver(Player player, Purchase p)
 {
  synchronized (player)
  {
   if (!player.isOnline() || player.isInOfflineMode() || p.getStatus() != PurchaseStatus.OFFLINE) return;
   if (!player.getInventory().validateCapacityByItemId(p.getProductId(), p.getQuantity()) || !player.getInventory().validateWeightByItemId(p.getProductId(), p.getQuantity()))
   { player.sendMessage("Your donation items are pending. Free inventory space and reopen the donation window."); return; }
   if (!DonationData.getInstance().reserveDelivery(p)) return;
   try
   {
    final var item = player.addItem(com.l2journey.gameserver.model.item.enums.ItemProcessType.REWARD, p.getProductId(), p.getQuantity(), player, true);
    if (item == null) throw new IllegalStateException("Donation item was not created");
    final long expectedCount = item.getCount();
    item.updateDatabase(true);
    if (!DonationData.getInstance().verifyItem(p,item.getObjectId(),expectedCount)) throw new IllegalStateException("Donation item persistence could not be verified");
    DonationData.getInstance().finishDelivery(p);
    p.changeStatus(PurchaseStatus.COMPLETED);
    final NpcHtmlMessage html = new NpcHtmlMessage(0, HTML_ID);
    html.setFile(player, HTML_PATH + "thankyou.htm"); html.replace("%icon%", icon(p.getProductId())); html.replace("%resume%", p.resume()); player.sendPacket(html);
   }
   catch (RuntimeException e)
   { LOGGER.log(Level.SEVERE, "DonationManager: Donation " + p.getId() + " requires manual delivery reconciliation", e); player.sendMessage("Your payment is recorded. Please contact a GM to verify the item delivery."); }
  }
 }

	public Map<Integer, Purchase> getWaitingPurchases()
	{
		return _waitingPurchases;
	}

	public void showIndexWindow(Player player)
	{
        if (DonationConfig.DONATION_ENABLED) offlinePlayer(player);
		showIndexWindow(player, false, false);
	}
	
	public void showIndexWindow(Player player, boolean showGatewaySelector, boolean showCurrencySelector)
	{
		if (!isEnabled())
		{
			showCustomWindow(player, player.isGM() ? "index_disabled.htm" : "index_disabled_player.htm");
			return;
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(0, HTML_ID);
		html.setFile(player, HTML_PATH + "index_single.htm");
		
		final StringBuilder sb = new StringBuilder();
		final CheckoutChoice cc = getChoice(player.getObjectId());
		final Set<String> currencySet = new HashSet<>();
		String bannerMsg;
		
		if (showCurrencySelector || !showGatewaySelector && cc.getGateway() != null && cc.getCurrency() == null)
		{
			sb.append("<tr>");
			
			for (PaymentMethod pm : PaymentMethod.values())
			{
				if (!pm.isEnabled())
					continue;
				
				if (cc.getGateway() != null && !pm.name().equals(cc.getGateway()))
					continue;

				if (pm == PaymentMethod.BINANCE && !pm.name().equals(cc.getGateway()))
					continue;
				
				if (pm == PaymentMethod.BINANCE && cc.getCustomCryptos().isEmpty())
					cc.getCustomCryptos().addAll(CurrencyManager.DEFAULT_CRYPTO_CURRENCIES);
				
				for (String currency : pm == PaymentMethod.BINANCE ? cc.getCustomCryptos() : pm.getAllCurrencies())
				{
					if (!CurrencyManager.getInstance().isExchangeAvailable(pm, currency))
						continue;
					
					if (currencySet.contains(currency))
						continue;
					
					if (!currencySet.isEmpty() && currencySet.size() % (pm == PaymentMethod.BINANCE ? 4 : 5) == 0)
						sb.append("</tr><tr>");
					
					sb.append(String.format(HTML_CHOICE_CHECKBOX, "currency", currency, currency.equals(cc.getCurrency()) ? "checked" : "unable", 32, currency));
					currencySet.add(currency);
					
					if (DonationConfig.DONATION_BINANCE_PAY_CURRENCY[0].isEmpty() && "BINANCE".equals(cc.getGateway()) && cc.getCustomCryptos().size() == currencySet.size())
					{
						sb.append(HTML_SEARCH_CRYPTO);
					}
				}
			}
			
			if (currencySet.isEmpty())
			{
				sb.append("<td width=80 height=20 align=center>Indisponível</td>");
			}
			
			sb.append("</tr>");
			bannerMsg = "Escolha uma moeda de pagamento";
		}
		else if (showGatewaySelector || !showCurrencySelector && (cc.getGateway() == null || cc.getCurrency() == null))
		{
			sb.append("<tr>");
			
			for (PaymentMethod pm : PaymentMethod.values())
			{
				if (!pm.isEnabled())
					continue;
				
				if (cc.getCurrency() != null && !pm.getAllCurrencies().contains(cc.getCurrency()))
					continue;
				
				sb.append(String.format(HTML_CHOICE_CHECKBOX, "gateway", pm.name(), pm.name().equals(cc.getGateway()) ? "checked" : "unable", pm.getHtmlColumnWidth(), pm.getDisplayName()));
			}
			
			sb.append("</tr>");
			bannerMsg = "Escolha um método de pagamento";
		}
		else
			bannerMsg = "Escolha quanto deseja comprar";
		
		String price = "";
		String quantity = "";
		
		if (cc.getGateway() != null && cc.getCurrency() != null)
		{
			final PaymentMethod pm = PaymentMethod.valueOf(cc.getGateway());
			
			if (!showCurrencySelector && !showGatewaySelector)
			{
				sb.append("<tr>");
				sb.append(String.format(HTML_CHOICE_CHECKBOX, "gateway", cc.getGateway(), "checked", pm.getHtmlColumnWidth(), pm.getDisplayName()));
				
				sb.append(String.format(HTML_CHOICE_CHECKBOX, "currency", cc.getCurrency(), "checked", cc.getCurrency().length() > 3 ? 40 : 25, cc.getCurrency()));
				sb.append("</tr>");
			}
			
			if (pm == PaymentMethod.BINANCE)
			{
				if (DonationConfig.DONATION_DROPDOWN)
				{
					final StringBuilder sb2 = new StringBuilder();
					for (int qnt : pm.getDropdown())
						sb2.append(String.format("%d (%s);", qnt, pm.getPrice(cc.getCurrency(), qnt).toString()));
						
					quantity = sb2.toString();
				}
				
				price = "- " + cc.getGatewayPrice() + " (unidade)";
			}
			else
			{
				final NumberFormat nf = NumberFormat.getCurrencyInstance();
				nf.setCurrency(Currency.getInstance(cc.getCurrency()));
				
				if (DonationConfig.DONATION_DROPDOWN)
				{
					final StringBuilder sb2 = new StringBuilder();
					for (int qnt : pm.getDropdown())
						sb2.append(String.format("%d (%s);", qnt, pm.getPrice(cc.getCurrency(), qnt)));
					
					quantity = sb2.toString();
				}
				
				price = "- " + nf.format(cc.getGatewayPrice()) + " (unidade)";
			}
			
			html.replace("%gateway%", "Pagamento");
			html.replace("%currency%", "Moeda");
			html.replace("%refresh%", !pm.getMainCurrency().equals(cc.getCurrency()) ? HTML_REFRESH_BUTTON : "");
		}
		else
		{
			html.replace("%gateway%", cc.getGateway() == null ? "Pagamento" : cc.getGatewayName());
			html.replace("%currency%", cc.getCurrency() == null ? "Moeda" : cc.getCurrency());
			html.replace("%refresh%", "");
		}
		
		if (DonationConfig.DONATION_DROPDOWN)
			quantity = String.format("<combobox var=Quantity list=\"%s\" width=105>", quantity);
		else
			quantity = HTML_INPUT_BOX;
		
		html.replace("%icon%", icon(DonationConfig.DONATION_PURCHASABLE_ITEM));
		html.replace("%item_name%", Purchase.configuredProductName(DonationConfig.DONATION_PURCHASABLE_ITEM));
		html.replace("%price%", price);
		html.replace("%list%", sb.toString());
		html.replace("%banner%", String.format(HTML_BANNER, bannerMsg));
		html.replace("%gateway_selector%", !showGatewaySelector);
		html.replace("%currency_selector%", !showCurrencySelector);
		html.replace("%calculator%", DonationConfig.DONATION_CALCULATOR ? HTML_CALCULATOR : "");
		html.replace("%quantity%", quantity.isEmpty() ? "<combobox var=Quantity list=\"\" width=90>" : quantity);
		html.replace("%clear_selectors%", cc.getCurrency() != null || cc.getGateway() != null ? HTML_CHOICE_CLEAR : "");
		player.sendPacket(html);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	public void deleteOrExpire(Purchase purchase)
	{
		if (DonationConfig.DONATION_DELETE_INACTIVE)
			DonationData.getInstance().delete(purchase, true, true);
		else
		{
			purchase.changeStatus(PurchaseStatus.EXPIRED);
			DonationData.getInstance().delete(purchase, false, true); // true porque não precisamos salvar, já pagamento não foi feito
		}
	}
	
	private static Purchase getPurchase(int playerId, int purchaseId)
	{
		return DonationData.getInstance().getPurchases(playerId).stream().filter(p -> p.getId() == purchaseId).findFirst().orElse(null);
	}
	
	public void showCustomWindow(Player player, String file, String replace)
	{
		if (player == null || !player.isOnline())
			return;

		final NpcHtmlMessage html = new NpcHtmlMessage(0, HTML_ID);
		html.setFile(player, HTML_PATH + file);
		html.replace("%replace%", replace != null ? replace : "");
		player.sendPacket(html);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	public void showCustomWindow(Player player, String file)
	{
		showCustomWindow(player, file, null);
	}

	public void showCustomWindow(int playerId, String file)
	{
		showCustomWindow(World.getInstance().getPlayer(playerId), file, null);
	}

	private static void showFloodWindow(Player player, int purchaseId)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0, HTML_ID);
		html.setFile(player, HTML_PATH + "flood.htm");
		html.replace("%id%", purchaseId);
		html.replace("%bypass%", "bypass donation " + (purchaseId != 0 ? "status " + purchaseId : "index"));
		player.sendPacket(html);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	private void showConfirmActionWindow(Player player, int purchaseId, String action)
	{
		if (!isEnabled())
		{
			showIndexWindow(player);
			return;
		}
		
		if (!action.equals("cancel") && !action.equals("hide"))
			return;
		
		final Purchase p = getPurchase(player.getObjectId(), purchaseId);
		if (p == null)
			return;
		
		final NpcHtmlMessage html = new NpcHtmlMessage(0, HTML_ID);
		html.setFile(player, HTML_PATH + "action.htm");
		html.replace("%action%", action);
		html.replace("%purchase%", purchaseId);
		player.sendPacket(html);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	private static void showEmailWindow(Player player, String address)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0, HTML_ID);
		html.setFile(player, HTML_PATH + "email.htm");

		if (_waitingPurchases.containsKey(player.getObjectId()))
			html.replace("%address%", "<tr><td>Você será <font color=LEVEL>redirecionado ao pagamento</font> logo após concluir essa etapa.</td></tr>");
		else if (address == null)
			html.replace("%address%", "<tr><td align=center>Por favor, insira um endereço válido.</td></tr>");
		else
			html.replace("%address%", "<tr><td align=center>Endereço atual:</td></tr><tr><td align=center><font color=LEVEL>" + address + "</font></td></tr>");

		player.sendPacket(html);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	private void showPurchaseStatusWindow(Player player, int id)
	{
		final Purchase p = getPurchase(player.getObjectId(), id);
		if (p == null)
			return;

		showPurchaseStatusWindow(player, p);
	}

	public void showPurchaseStatusWindow(Player player, Purchase p)
	{
		if (player == null || !player.isOnline())
			return;

		final NpcHtmlMessage html = new NpcHtmlMessage(0, HTML_ID);
		final long expiration = TimeUnit.MILLISECONDS.toMinutes(p.getExpiration() - System.currentTimeMillis());
		
		if (p.getStatus() == PurchaseStatus.WAITING || p.getStatus() == PurchaseStatus.CREATED || p.getStatus() == PurchaseStatus.PENDING)
		{
			html.setFile(player, HTML_PATH + "status_waiting.htm");
			if (p.getMessage() != null)
			{
				html.replace("%msg%", p.getMessage());
				p.setMessage(null);
			}
			else
				html.replace("%msg%", "");
			
			if (p.getPaymentMethod().sendMail())
				html.replace("%link%", HTML_LINK_BUTTON);
			else
				html.replace("%link%", "");

			if (p.getStatus() == PurchaseStatus.WAITING)
				html.replace("%check%", HTML_CHECK_BUTTON);
			else
				html.replace("%check%", "");

			html.replace("%qrcode%", HTML_QRCODE_BUTTON);
		}
		else
		{
			html.setFile(player, HTML_PATH + "status_others.htm");
			html.replace("%hide%", DonationConfig.DONATION_HIDE_ENDED ? "<td width=80 align=center><a action=\"bypass donation confirm %id% hide\">Excluir</a></td>"  : "");
		}
		
		if (DonationConfig.DONATION_REQUIRE_TERMS)
		{
			if (!p.agreedTerms() && p.getStatus() != PurchaseStatus.PENDING)
				html.replace("%terms%", "");
			else
			{
				html.replace("%terms%", HtmCache.getInstance().getHtm(player, HTML_PATH + "table_terms.htm"));
				
				if (!p.agreedTerms())
				{
					html.replace("%bgcolor%", "bgcolor=ff0000");
					html.replace("%terms_cb%", "unable");
					html.replace("%bypass%", "action=\"bypass donation terms %id% checkbox\"");
				}
				else
				{
					html.replace("%terms_cb%", "checked");
					html.replace("%bgcolor%", "");
					html.replace("%bypass%", p.getStatus() == PurchaseStatus.PENDING ? "action=\"bypass donation terms %id% checkbox\"" : "");
				}
			}
		}
		else
			html.replace("%terms%", "");

		html.replace("%id%", p.getId());
		html.replace("%id_externo%", p.getPaymentId() == null ? "0" : p.getPaymentId());
		html.replace("%payment%", p.getPaymentMethod().getName());
		html.replace("%payment_type%", p.getPaymentMethod().getTypeName());
		html.replace("%currency%", p.getPaymentMethod() == PaymentMethod.MP_PIX ? "" : String.format("(%s)", p.getCurrency()));
		html.replace("%email%", p.getPlayerEmail());
		html.replace("%status%", p.getStatus().getDesc());
		html.replace("%date%", new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(p.getDate()));
		html.replace("%expiration%", + expiration > 1 ? expiration + " minutos" : " menos de 1 minuto");
		html.replace("%player%", player.getName());
		html.replace("%resume%", p.resume());
		player.sendPacket(html);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	private static void showPurchaseHistoryWindow(Player player, int page)
	{
		
		final NpcHtmlMessage html = new NpcHtmlMessage(0, HTML_ID);
		html.setFile(player, HTML_PATH + "history.htm");

		final Pagination<Purchase> pagination = new Pagination<>(DonationData.getInstance().getPurchases(player.getObjectId()).stream(), page, 9, p -> !p.isHidden(), Comparator.comparing(Purchase::getDate).reversed());
		for (Purchase p : pagination)
		{
			final String price = p.getFormatedPrice();
			pagination.append("<table><tr><td></td></tr><tr><td width=70>");
			pagination.append(new SimpleDateFormat("dd-MM-yyyy").format(p.getDate()));
			pagination.append("</td><td width=90>");
			pagination.append(DonationStrings.trimAndDress(p.getProductName(), 12));
			pagination.append("</td><td width=75>");
			pagination.append(price.length() > 10 ? "..." : price);
			pagination.append("</td><td width=75>");
			pagination.append(String.format("<a action=\"bypass donation status %d %d\">", p.getId(), page), p.getStatus().getName(), "</a>");
			pagination.append("</td></tr><tr><td></td></tr></table>");
			pagination.append(HTML_LINE);
		}

		if (pagination.isEmpty())
			pagination.append(HTML_EMPTY_TABLE);
		else if (pagination.getTotalEntries() > 9)
			pagination.generatePages("bypass donation history %page%");

		html.replace("%table%", pagination.getContent());
		player.sendPacket(html);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	public void showCheckoutWindow(Purchase purchase)
	{
		showCheckoutWindow(World.getInstance().getPlayer(purchase.getPlayerId()), purchase);
	}
	
	public void showCheckoutWindow(Player player, Purchase purchase)
	{
		if (player == null || !player.isOnline())
			return;
		
		if (!isEnabled())
		{
			showIndexWindow(player);
			return;
		}
		
		String msg = null;
		switch (purchase.getPaymentMethod())
		{
			case BINANCE:
				msg = "Use o aplicativo da Binance para escanear o código.<br1>\n"
					+ "Você também pode solicitar um e-mail contendo um link para à página de pagamento.";
				break;
			case MP_LINK:
			case PAYPAL:
				msg = "Use a câmera do seu celular para escanear o código e fazer o pagamento através do seu celular (no aplicativo ou navegador)<br1>"
					+ "Você também pode solicitar um e-mail contendo um link para à página de pagamento.";
				break;
			case MP_PIX:
				msg = "Use o aplicativo do seu banco para escanear o código do PIX.<br1>"
				+ "O MercadoPago irá lhe enviar um e-mail com o código \"cópia e cola\", você pode utilizá-lo também.";
				break;
		}

		final NpcHtmlMessage html = new NpcHtmlMessage(0, HTML_ID);
		html.setFile(player, HTML_PATH + (purchase.wantQrCode() ? "checkout_qrcode.htm" : "checkout_link.htm"));
		html.replace("%id%", purchase.getId());
		html.replace("%resume%", purchase.resume());
		html.replace("%msg%", msg);
		html.replace("%payment%", purchase.getPaymentMethod().getName());
		player.sendPacket(html);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	private void showQrCodeWindow(Player player, int purchaseId)
	{
		final Purchase p = getPurchase(player.getObjectId(), purchaseId);
		if (p == null)
			return;

		showQrCodeWindow(player, p);
	}

	public void showQrCodeWindow(Player player, Purchase p)
	{
		if (player == null || !player.isOnline())
			return;
		
		if (!isEnabled())
		{
			showIndexWindow(player);
			return;
		}
		
		if (!p.agreedTerms() || p.getStatus() != PurchaseStatus.WAITING && p.getStatus() != PurchaseStatus.CREATED && p.getStatus() != PurchaseStatus.PENDING)
		{
			showPurchaseStatusWindow(player, p);
			return;
		}

		if (p.getStatus() == PurchaseStatus.CREATED || p.getStatus() == PurchaseStatus.PENDING)
		{
			if (!isThreadSafe(player.getObjectId()))
				return;
			
			p.setWantQrCode(true);
			
			p.getPaymentHandler().sendPurchase(p);
			showCustomWindow(player, "requesting.htm");
			return;
		}
		
		if (p.getQrCode() == null)
			return;

		try
		{
			final byte[] qrCode = com.l2journey.gameserver.donation.util.DonationQrCode.create(p.getQrCode());
			final NpcHtmlMessage html = new NpcHtmlMessage(0, HTML_ID);
			html.setFile(player, HTML_PATH + "qrcode.htm");
			
			if (p.getPaymentMethod() == PaymentMethod.MP_PIX)
			{
				final StringBuilder sb = new StringBuilder();
				if (!DonationConfig.DONATION_MP_PIX_ACCOUNT_OWNER.isEmpty())
					sb.append("<tr><td align=center>TITULAR: " + DonationConfig.DONATION_MP_PIX_ACCOUNT_OWNER.toUpperCase() + "</td></tr>");
				
				if (!DonationConfig.DONATION_MP_PIX_ACCOUNT_CPF.isEmpty())
					sb.append("<tr><td align=center>CPF: " + DonationConfig.DONATION_MP_PIX_ACCOUNT_CPF + "</td></tr>");
				
				if (!DonationConfig.DONATION_MP_PIX_ACCOUNT_BANK.isEmpty())
					sb.append("<tr><td align=center>BANCO: " + DonationConfig.DONATION_MP_PIX_ACCOUNT_BANK.toUpperCase() + "</td></tr>");
				
				html.replace("%info%", sb.isEmpty() ? "" : "<br><br><table width=280 cellpadding=4>" + sb.toString() + "</table>");
			}
			else
				html.replace("%info%", "");
			
			html.replace("Crest.crest_%serverId%_%purchase%", "Crest.crest_" + com.l2journey.Config.SERVER_ID + "_" + p.getCrestId());
			html.replace("%purchase%", p.getId());
			html.replace("%serverId%", com.l2journey.Config.SERVER_ID);
			player.sendPacket(new PledgeCrest(p.getCrestId(), qrCode));
			player.sendPacket(new TutorialShowHtml(html.getHtml()));
		}
		catch (WriterException e)
		{
		}
	}

	private void showLinkWindow(Player player, int purchaseId, boolean sendMail)
	{
		final Purchase p = getPurchase(player.getObjectId(), purchaseId);
		if (p == null)
			return;
		
		if (!isEnabled())
		{
			showIndexWindow(player);
			return;
		}
		
		if (!isThreadSafe(player.getObjectId()))
			return;

		if (!p.agreedTerms() || p.getStatus() != PurchaseStatus.WAITING && p.getStatus() != PurchaseStatus.CREATED && p.getStatus() != PurchaseStatus.PENDING)
		{
			showPurchaseStatusWindow(player, p);
			return;
		}

		if (p.getStatus() == PurchaseStatus.CREATED || p.getStatus() == PurchaseStatus.PENDING)
		{
			if (p.getPaymentMethod().sendMail())
				p.setWantQrCode(false);
			
			showCustomWindow(player, "requesting.htm");
			p.getPaymentHandler().sendPurchase(p);
			return;
		}

		final NpcHtmlMessage html = new NpcHtmlMessage(0, HTML_ID);
		html.setFile(player, HTML_PATH + "link.htm");
		html.replace("%id%", p.getId());
		html.replace("%resend%", String.format("<td width=64><a action=\"bypass -h donation link %d true\">%s</a></td>", p.getId(), p.getEmailCoint() == 0 ? "Enviar" : "Reenviar"));

		if (sendMail)
		{
			final int maxEmails = p.getPaymentMethod() == PaymentMethod.PAYPAL ? 2 : DonationConfig.DONATION_MAXIMUM_NUMBER_EMAILS;
			
			if (p.getEmailCoint() >= maxEmails || maxEmails == 0)
				html.replace("%msg%", "Não é possível enviar um novo e-mail.");
			else
			{
				if (p.getExpiration() - System.currentTimeMillis() < TimeUnit.MINUTES.toMillis(1))
					html.replace("%msg%", "Não foi possível enviar um novo e-mail.");
				else if (p.getLastEmailTime() != 0 && p.getLastEmailTime() + TimeUnit.MINUTES.toMillis(2) > System.currentTimeMillis())
					html.replace("%msg%", "Aguarde antes de enviar um novo e-mail.");
				else
				{
					html.replace("%msg%", "E-mail reenviado!");
					p.getPaymentHandler().resendEmail(p);
				}
			}
			
			html.replace("%font%", "FF0000");
		}
		else
		{
			html.replace("%font%", "LEVEL");
			html.replace("%msg%", p.getEmailCoint() == 0 ? "Solicitar e-mail para pagamento" : "Já enviamos seu e-mail.");
		}

		player.sendPacket(html);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	public void showSearchWindow(Player player, String msg, String search)
	{
		
		final PaymentMethod pm = PaymentMethod.valueOf("BINANCE");
		if (!DonationConfig.DONATION_BINANCE_PAY && pm.getAltCurrencies() != null)
			return;
		
		final List<String> customs = getChoice(player.getObjectId()).getCustomCryptos();
		final StringBuilder sb1 = new StringBuilder();
		int count1 = 0;

		for (String currency : CurrencyManager.DEFAULT_CRYPTO_CURRENCIES)
		{
			final String column = String.format(HTML_CHOICE_CHECKBOX, "custom", currency, customs.contains(currency) ? "checked" : "unable", 115, String.format("%s (%s)", currency, CurrencyManager.getInstance().convert(pm, currency, 1)));
			sb1.append(count1 % 2 == 0 ? "<tr>" + column : column + "</tr>");
			count1++;
		}
		
		final StringBuilder sb2 = new StringBuilder();
		int count2 = 0;
		
		for (Entry<String, BigDecimal> entry : CurrencyManager.getInstance().getCryptoExchangeRates().entrySet())
		{
			if (CurrencyManager.DEFAULT_CRYPTO_CURRENCIES.contains(entry.getKey()))
				continue;
			
			final String column = String.format(HTML_CHOICE_CHECKBOX, "custom", entry.getKey(), customs.contains(entry.getKey()) ? "checked" : "unable", 125, String.format("<font color=%s> %s (%s)</font>", entry.getKey().equals(search) ? "ff0000" : "ffffff", entry.getKey(), entry.getValue().toString()));
			sb2.append(count2 % 2 == 0 ? "<tr>" + column : column + "</tr>");
			count2++;
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(0, HTML_ID);
		html.setFile(player, HTML_PATH + "search.htm");
		html.replace("%list%", sb1.toString());
		html.replace("%result%", sb2.toString());
		html.replace("%msg%", msg != null ? msg + "<br>" : "");
		html.replace("%clean%", customs.isEmpty() ? "" : "<td width=35><a action=\"bypass donation clear_cryptos\">Limpar</a></td>");
		player.sendPacket(html);
	}

	private void setPlayerEmail(Player player, String email)
	{
		if (email == null || email.equals("0") || email.length() > 254 || email.length() < 6)
		{
			showEmailWindow(player, null);
			return;
		}

		if (!DonationStrings.isValidString(email, EMAIL_PATTERN))
		{
			showEmailWindow(player, null);
			return;
		}

		final String domain = email.substring(email.indexOf("@") + 1).toLowerCase(java.util.Locale.ROOT);
		if (!Arrays.asList(DonationConfig.DONATION_ALLOWED_EMAILS).contains(domain))
		{
			showEmailWindow(player, null);
			return;
		}

		final Purchase p = _waitingPurchases.get(player.getObjectId());
		if (p != null)
		{
			p.setPlayerEmail(email);
			DonationData.getInstance().store(p);
			showPurchaseStatusWindow(player, p);
		}
		else
			showEmailWindow(player, email);

		player.getVariables().set("donation_email", email);
	}

	private void hidePurchase(Player player, int purchaseId)
 {
  final Purchase p = getPurchase(player.getObjectId(), purchaseId);
  if (p == null || !DonationConfig.DONATION_HIDE_ENDED || p.isBusy()) return;
  if (p.getStatus() == PurchaseStatus.WAITING || p.getStatus() == PurchaseStatus.PENDING || p.getStatus() == PurchaseStatus.CREATED || p.getStatus() == PurchaseStatus.FINISHING || p.getStatus() == PurchaseStatus.OFFLINE) return;
  p.hide();
  showPurchaseHistoryWindow(player,1);
 }

	private void cancelPurchase(Player player, int id)
	{
		if (!isEnabled())
		{
			showIndexWindow(player);
			return;
		}
		
		final Purchase p = getPurchase(player.getObjectId(), id);
		if (p == null)
			return;
		
		if (p.getStatus() == PurchaseStatus.HIDDEN)
		{
			showPurchaseHistoryWindow(player, 1);
			return;
		}
		
		if (p.isBusy())
		{
			showCustomWindow(player, "busy.htm");
			return;
		}

		if (p.getStatus() != PurchaseStatus.PENDING && p.getStatus() != PurchaseStatus.CREATED && p.getStatus() != PurchaseStatus.WAITING)
		{
			showPurchaseStatusWindow(player, p);
			return;
		}
		
		if (!isThreadSafe(player.getObjectId()))
			return;
		
		if (p.getQrCode() != null)
			player.sendPacket(TutorialCloseHtml.STATIC_PACKET);
			
		if (DonationConfig.DONATION_DELETE_INACTIVE)
		{
			DonationData.getInstance().delete(p, true, true);
			showIndexWindow(player);
		}
		else
		{
			p.changeStatus(PurchaseStatus.CANCELED);
			DonationData.getInstance().delete(p, false, true);
			showPurchaseStatusWindow(player, p);
		}
		
		DonationTaskManager.getInstance().remove(p);
		p.getPaymentHandler().closePurchase(p);
	}
	
	private void searchCriptoCurrency(Player player, String search)
	{
		
		if (!DonationConfig.DONATION_BINANCE_PAY && PaymentMethod.valueOf("BINANCE").getAltCurrencies() != null)
			return;
		
		if (!DonationStrings.isValidString(search, CURRENCY_CODE_PATERN))
		{
			showSearchWindow(player, "inválido", null);
			return;
		}
		
		if (CurrencyManager.getInstance().isCryptoAvailable(search))
		{
			showSearchWindow(player, null, search);
			return;
		}
		
		showCustomWindow(player, "requesting.htm");
		DonationExecutor.execute(() -> CryptoCurrencyTask.getInstance().search(player.getObjectId(), search));
	}
	
	private void handleTermsActions(Player player, int id, String target)
	{
		if (!isEnabled())
		{
			showIndexWindow(player);
			return;
		}
		
		final Purchase p = getPurchase(player.getObjectId(), id);
		if (p == null)
			return;
		
		if (target.equals("htm"))
		{
			showCustomWindow(player, "terms.htm", String.valueOf(id));
			return;
		}
		
		if (target.equals("checkbox"))
		{
			p.setTermsStatus(!p.agreedTerms());
            DonationData.getInstance().update(p);
			showPurchaseStatusWindow(player, p);
		}
	}

	private void handleBuyerDetails(Player player, String command)
	{
		final Purchase purchase = _waitingPurchases.get(player.getObjectId());
		if (purchase == null || purchase.getPlayerId() != player.getObjectId() || purchase.wasRequested() || purchase.getPaymentMethod() != PaymentMethod.MP_PIX)
		{
			return;
		}
		if (purchase.getExpiration() <= System.currentTimeMillis())
		{
			_waitingPurchases.remove(player.getObjectId(), purchase);
			player.sendMessage("Your purchase form expired. Please start a new purchase.");
			showIndexWindow(player);
			return;
		}
		final BuyerDetails details;
		try
		{
			details = BuyerDetails.parseForm(command);
		}
		catch (IllegalArgumentException e)
		{
			player.sendMessage("Invalid buyer details. Check the names and CPF, leave fields blank, or choose Skip.");
			showCustomWindow(player, "buyer.htm");
			return;
		}
		purchase.setBuyerDetails(details);
		finishNewPurchase(player, purchase);
	}

	private void finishNewPurchase(Player player, Purchase purchase)
	{
		if (purchase.getPlayerEmail() == null)
		{
			_waitingPurchases.put(player.getObjectId(), purchase);
			showEmailWindow(player, null);
		}
		else
		{
			DonationData.getInstance().store(purchase);
			showPurchaseStatusWindow(player, purchase);
		}
	}

	private void newPurchase(Player player, int quantity)
	{
		if (!isEnabled())
		{
			showIndexWindow(player);
			return;
		}
		
		if (!isThreadSafe(player.getObjectId()))
			return;
		
		if (quantity <= 0 || quantity > 999)
		{
			showIndexWindow(player);
			return;
		}
		
		if (DonationData.getInstance().getPurchases(player.getObjectId()).stream().filter(p -> p.getStatus() == PurchaseStatus.CREATED || p.getStatus() == PurchaseStatus.PENDING || p.getStatus() == PurchaseStatus.WAITING || p.getStatus() == PurchaseStatus.FINISHING).count() >= 3)
        { player.sendMessage("Please finish or cancel your pending purchases before creating another one."); return; }
		final CheckoutChoice cc = getChoice(player.getObjectId());
		if (cc.getGateway() == null || cc.getCurrency() == null)
		{
			showIndexWindow(player);
			return;
		}
		
		final PaymentMethod selected = PaymentMethod.valueOf(cc.getGateway());
        if (!selected.isEnabled() || !selected.getAllCurrencies().contains(cc.getCurrency()) || !CurrencyManager.getInstance().isExchangeAvailable(selected, cc.getCurrency())) return;
        if (DonationConfig.DONATION_DROPDOWN && java.util.Arrays.stream(selected.getDropdown()).noneMatch(q -> q == quantity)) return;
		final String email = player.getVariables().getString("donation_email", null);

		if (!allowRequest(player.getObjectId(), true))
		{
			showFloodWindow(player, 0);
			return;
		}

		final Purchase purchase = new Purchase(player.getObjectId(), quantity, email, player.getClient().getIp(), cc);
		if (selected == PaymentMethod.MP_PIX && DonationConfig.DONATION_MP_BUYER_DETAILS)
		{
			_waitingPurchases.put(player.getObjectId(), purchase);
			showCustomWindow(player, "buyer.htm");
		}
		else
		{
			finishNewPurchase(player, purchase);
		}
		
		player.sendPacket(TutorialCloseHtml.STATIC_PACKET);
	}
	
	/*
	 * Um player pesquisou por uma moeda na Binance, e a busca foi concluída, vamos exibir o resultado
	 */
	public void onSearchCrypto(int playerId, String search)
	{
		final Player player = World.getInstance().getPlayer(playerId);
		if (player == null || !player.isOnline())
			return;
		
		if (!isEnabled())
		{
			showIndexWindow(player);
			return;
		}
		
		if (!CurrencyManager.getInstance().isCryptoAvailable(search))
		{
			showSearchWindow(player, "Não foi possível encontrar a moeda " + search + ".", null);
			return;
		}
		
		showSearchWindow(player, null, search);
	}

	public void onCompletedPayment(Purchase p)
 {
  synchronized (p)
  {
   if (p.getPaymentId() == null || p.getStatus() == PurchaseStatus.COMPLETED || p.getStatus() == PurchaseStatus.HIDDEN || p.getStatus() == PurchaseStatus.OFFLINE) return;
   p.changeStatus(PurchaseStatus.OFFLINE);
  }
  final Player player = World.getInstance().getPlayer(p.getPlayerId());
  if (player != null) deliver(player, p);
 }

	private void checkPaymentStatus(Player player, int purchaseId)
	{
		
		final Purchase purchase = getPurchase(player.getObjectId(), purchaseId);
		if (purchase == null || purchase.getStatus() != PurchaseStatus.WAITING)
			return;
		
		if (!isThreadSafe(player.getObjectId()))
			return;
		
		if (purchase.getStatus() != PurchaseStatus.WAITING)
		{
			player.sendPacket(TutorialCloseHtml.STATIC_PACKET);
			showPurchaseStatusWindow(player, purchase);
			return;
		}

		if (!allowRequest(player.getObjectId(), false))
		{
			showFloodWindow(player, purchaseId);
			return;
		}

		purchase.getPaymentHandler().checkPurchase(purchase, true);
		showCustomWindow(player, "requesting.htm");
	}

	/**
	 * @param p
	 * @param clear : Caso não seja possível criar a no serviço de pagamento, não salvamos ela
	 */
	public void handleException(Purchase p, boolean clear)
 {
  p.setMessage("<br>Payment service temporarily unavailable. Please check again later.");
  showCustomWindow(p.getPlayerId(), "exception.htm");
 }

	/*
	 * Deleta ou expira uma purchase e a remove da task
	 */
	public void inactivePurchase(Purchase p)
	{
		DonationTaskManager.getInstance().remove(p);
		deleteOrExpire(p);
	}
	
	public void onPaymentNotFound(Purchase p)
	{
		final Player player = World.getInstance().getPlayer(p.getPlayerId());
		if (player == null || !player.isOnline())
			return;
			
		p.setMessagePaymentNotFound();
		DonationManager.getInstance().showPurchaseStatusWindow(player, p);
	}
	
	public void onPaymentFailed(Purchase p)
	{
		p.changeStatus(PurchaseStatus.FAILED);
		
		final Player player = World.getInstance().getPlayer(p.getPlayerId());
		if (player == null || !player.isOnline())
			return;
		
		player.sendPacket(TutorialCloseHtml.STATIC_PACKET);
		DonationManager.getInstance().showPurchaseStatusWindow(player, p);
	}
	
	/*
	 * Para evitar problemas de concorrência
	 * Seria melhor verificar apenas pela purchase atual ao invés de todas?
	 */
	public boolean isThreadSafe(int playerId)
	{
		if (DonationData.getInstance().getPurchases(playerId).stream().anyMatch(Purchase::isBusy))
		{
			showCustomWindow(playerId, "busy.htm");
			return false;
		}
		
		return true;
	}

	public static DonationManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final DonationManager INSTANCE = new DonationManager();
	}
}
