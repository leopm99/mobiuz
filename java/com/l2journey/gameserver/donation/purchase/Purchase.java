package com.l2journey.gameserver.donation.purchase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.l2journey.gameserver.donation.CheckoutChoice;
import com.l2journey.gameserver.donation.BuyerDetails;
import com.l2journey.gameserver.donation.DonationData;
import com.l2journey.gameserver.donation.DonationManager;
import com.l2journey.gameserver.donation.IPaymentHandler;
import com.l2journey.gameserver.donation.DonationConfig;
import com.l2journey.gameserver.data.sql.CharInfoTable;
import com.l2journey.gameserver.data.xml.ItemData;

public class Purchase
{
	private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(Purchase.class.getName());
	private int _crestId;
	public synchronized int getCrestId() { if (_crestId == 0) _crestId = com.l2journey.gameserver.managers.IdManager.getInstance().getNextId(); return _crestId; }
	private final int _id;
	private final int _player;
	private final int _product;
	private final int _quantity;
	private final BigDecimal _unitPrice;
	private long _date;
    private long _expiration;
    private String _reference;
    private boolean _requested;
    private boolean _hidden;
    public boolean isHidden() { return _hidden || _status == PurchaseStatus.HIDDEN; }
    public void hide() { _hidden = true; DonationData.getInstance().update(this); }
    public boolean wasRequested() { return _requested; }
    public void markRequested() { _requested = true; DonationData.getInstance().update(this); }
	private String _productName;
	private int _mpRequestVersion = 1;
	private String _mpProductCategory;
	private String _mpStatementDescriptor;
	private BuyerDetails _buyerDetails = BuyerDetails.EMPTY;
	public BuyerDetails getBuyerDetails() { return _buyerDetails; }
	public synchronized void setBuyerDetails(BuyerDetails details)
	{
		if (wasRequested()) throw new IllegalStateException("Cannot change payer details after requesting payment");
		_buyerDetails = java.util.Objects.requireNonNull(details);
	}
	public String getMpStatementDescriptor() { return _mpStatementDescriptor; }
	public String getMpProductCategory() { return _mpProductCategory; }
	public String getStoredProductName() { return _productName; }
	public int getMpRequestVersion() { return _mpRequestVersion; }
	private String _mpApi = "PAYMENTS";
	private boolean _mpSandbox;
	private boolean _mpSandboxAutoApprove = true;
	private String _mpMerchantId;
	private String _mpTransactionId;

	public String getMpApi() { return _mpApi; }
	public boolean usesMercadoPagoOrders() { return "ORDERS".equals(_mpApi); }
	public boolean isMpSandbox() { return _mpSandbox; }
	public boolean isMpSandboxAutoApprove() { return _mpSandboxAutoApprove; }
	public String getMpMerchantId() { return _mpMerchantId; }
	public String getMpTransactionId() { return _mpTransactionId; }
	public void setMpTransactionId(String value) { _mpTransactionId = value; }

	/** Freeze the protocol and account before the first request; never migrate an existing charge. */
	public synchronized void useMercadoPagoOrders()
	{
		if (wasRequested() || _paymentMethod != PaymentMethod.MP_PIX)
		{
			throw new IllegalStateException("An existing payment cannot change API");
		}
		_mpApi = "ORDERS";
		_mpRequestVersion = 5;
		_mpStatementDescriptor = DonationConfig.DONATION_MP_STATEMENT_DESCRIPTOR;
		_mpProductCategory = DonationConfig.DONATION_MP_PRODUCT_CATEGORY;
		if (_productName == null) _productName = configuredProductName(_product);
		_mpSandbox = DonationConfig.DONATION_MP_SANDBOX;
		_mpSandboxAutoApprove = DonationConfig.DONATION_MP_SANDBOX_AUTO_APPROVE;
		_mpMerchantId = DonationConfig.DONATION_MP_MERCHANT_ID;
	}

	private long _lastEmailTime;
	private String _paymentId;
	private String _mpPreferenceId;
	private String _paypalInvoiceId;
	private String _email;
	private String _message;
	private String _ipAddress;
	private String _qrCode;
	private String _link;
	private String _currency;
	private volatile boolean _busy;
	private boolean _wantQrCode;
	private boolean _terms;
	private int _emailCount;
	private volatile PurchaseStatus _status;
	private PaymentMethod _paymentMethod;

	public Purchase(ResultSet rs) throws SQLException
	{
		_paymentMethod = PaymentMethod.valueOf(rs.getString("payment_method"));
		_id = rs.getInt("purchase_id");
		_paymentId = rs.getString("payment_id");
		_player = rs.getInt("player_id");
		_product = rs.getInt("product_id");
		_quantity = rs.getInt("quantity");
		_unitPrice = rs.getBigDecimal("unit_price");
		_date = rs.getLong("date");
		_email = rs.getString("email");
		_status = PurchaseStatus.valueOf(rs.getString("status"));
		_currency = rs.getString("currency");
		_terms = rs.getBoolean("terms");
        _expiration = rs.getLong("expires_at");
        _reference = rs.getString("reference");
        _requested = rs.getBoolean("requested");
        _hidden = rs.getBoolean("hidden");
		_productName = rs.getString("product_name");
		_mpRequestVersion = rs.getInt("mp_request_version");
		_mpProductCategory = rs.getString("mp_product_category");
		_mpStatementDescriptor = rs.getString("mp_statement_descriptor");
		_buyerDetails = new BuyerDetails(rs.getString("buyer_first_name"), rs.getString("buyer_last_name"), rs.getString("buyer_cpf"));
		_mpApi = rs.getString("mp_api");
		_mpSandbox = rs.getBoolean("mp_sandbox");
		_mpSandboxAutoApprove = rs.getBoolean("mp_sandbox_auto_approve");
		_mpMerchantId = rs.getString("mp_merchant_id");
		_wantQrCode = _paymentMethod.isQrCode();
	}

	public Purchase(int player, int quantity, String email, String ipAddress, CheckoutChoice cc)
	{
		final PaymentMethod pm = PaymentMethod.valueOf(cc.getGateway());
		
		_id = DonationData.getInstance().allocateId();
		_player = player;
		_product = DonationConfig.DONATION_PURCHASABLE_ITEM;
		_productName = configuredProductName(_product);
		_quantity = quantity;
		_unitPrice = pm.getPrice(cc.getCurrency());
		_email = email;
		_ipAddress = ipAddress;
		_status = DonationConfig.DONATION_REQUIRE_TERMS ? PurchaseStatus.PENDING : PurchaseStatus.CREATED;
		_date = System.currentTimeMillis();
		_paymentMethod = pm;
		_currency = cc.getCurrency();
		_wantQrCode = pm.isQrCode();
        _expiration = calculateExpiration();
        _reference = "journey" + com.l2journey.Config.SERVER_ID + "p" + _id;
        if (_unitPrice == null || _unitPrice.signum() <= 0 || quantity <= 0 || quantity > 999) throw new IllegalArgumentException("Invalid purchase quantity or price");
	}

	public String getReference() { return _reference; }
	public final int getId()
	{
		return _id;
	}

	public final int getPlayerId()
	{
		return _player;
	}

	public final int getProductId()
	{
		return _product;
	}

	public final int getQuantity()
	{
		return _quantity;
	}

	public final BigDecimal getUnitPrice()
	{
		return _unitPrice;
	}

	public final BigDecimal getTotalPrice()
	{
		return _unitPrice.multiply(BigDecimal.valueOf(_quantity)).setScale(_paymentMethod == PaymentMethod.BINANCE ? 8 : 2, RoundingMode.HALF_UP);
	}

	public long getDate()
	{
		return _date;
	}

	public void updateDate(long value)
	{
		_date = value;
	}

	public String getPlayerEmail()
	{
		return _email;
	}
	
	public String getIpAddress()
	{
		return _ipAddress;
	}

	public void setPlayerEmail(String address)
	{
		_email = address;
		if (DonationData.getInstance().getPurchaseById(_id) != null) DonationData.getInstance().update(this);
	}

	public PurchaseStatus getStatus()
	{
		return _status;
	}

	public synchronized void changeStatus(PurchaseStatus newStatus)
	{
		if ((_status == PurchaseStatus.COMPLETED || _status == PurchaseStatus.OFFLINE || _status == PurchaseStatus.HIDDEN) && newStatus != PurchaseStatus.COMPLETED && newStatus != PurchaseStatus.HIDDEN) return;
		final PurchaseStatus previous = _status;
		_status = newStatus;
		
		if (newStatus == PurchaseStatus.FINISHING)
			return;
		
		try { DonationData.getInstance().update(this); } catch (RuntimeException e) { _status = previous; throw e; }
		if (previous != newStatus) LOGGER.info("Purchase: Donation purchase " + _id + " status " + previous + " -> " + newStatus);
	}
	
	public String getPlayerName()
	{
		return CharInfoTable.getInstance().getNameById(_player);
	}

	/**
	 * @return : ID da purchase no serviço de pagamento
	 */
	public String getPaymentId()
	{
		return _paymentId;
	}

	public void setPaymentId(String id)
	{
		_paymentId = id;
	}

	public String getMpPreferenceId()
	{
		return _mpPreferenceId;
	}

	public void setMpPreferenceId(String id)
	{
		_mpPreferenceId = id;
	}

	private String getMpPreferenceLink()
	{
		return _mpPreferenceId != null ? "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=" + _mpPreferenceId : null;
	}
	
	private String getPaypalInvoiceLink()
	{
		return _paypalInvoiceId != null ? (DonationConfig.DONATION_PAYPAL_SANDBOX_ENABLED ? "https://www.sandbox.paypal.com/invoice/p/#" : "https://www.paypal.com/invoice/p/#") + _paypalInvoiceId : null;
	}
	
	public void setLink(String url)
	{
		_link = url;
	}

	public String getLink()
	{
		return getLink(false);
	}
	
	public String getLink(boolean toStore)
	{
		switch (_paymentMethod)
		{
			case MP_LINK:
				return _link != null ? _link : (toStore ? null : getMpPreferenceLink());
			case PAYPAL:
				return toStore ? null : getPaypalInvoiceLink();
			default:
				return _link;
		}
	}

	public String getProductName()
	{
		if (_productName != null) return _productName;
		return itemName(_product);
	}

	public static String configuredProductName(int itemId)
	{
		final String name = DonationConfig.DONATION_PRODUCT_NAME;
		return name == null || name.isBlank() ? itemName(itemId) : name;
	}

	private static String itemName(int itemId)
	{
		final var item = ItemData.getInstance().getTemplate(itemId);
		return item == null ? "Item #" + itemId : item.getName();
	}

	public void setQrCode(String value)
	{
		_qrCode = value;
	}
	
	public String getQrCode()
	{
		return getQrCode(false);
	}
	
	public String getQrCode(boolean toStore)
	{
		switch (_paymentMethod)
		{
			case MP_LINK:
				return _link != null ? _link : (toStore ? null : getMpPreferenceLink());
			case PAYPAL:
				return toStore ? null : getPaypalInvoiceLink();
			default:
				return _qrCode;
		}
	}

	public long getExpiration() { return _expiration; }
	private long calculateExpiration()
	{
		switch (getPaymentMethod())
		{
			case MP_LINK:
				return getDate() + TimeUnit.MINUTES.toMillis(Math.max(DonationConfig.DONATION_MP_LINK_EXPIRATION_TIME, 1));
			case MP_PIX:
				return getDate() + TimeUnit.MINUTES.toMillis(Math.max(DonationConfig.DONATION_MP_PIX_EXPIRATION_TIME, 30));
			case PAYPAL:
				return getDate() + TimeUnit.MINUTES.toMillis(Math.max(DonationConfig.DONATION_PAYPAL_LINK_EXPIRATION_TIME, 1));
			case BINANCE:
				return getDate() + TimeUnit.MINUTES.toMillis(Math.max(DonationConfig.DONATION_BINANCE_EXPIRATION_TIME, 1));
			default:
				return 0;
		}
		
	}

	public boolean timeExpired()
	{
		return System.currentTimeMillis() > getExpiration();
	}

	public void setMessage(String value)
	{
		_message = value;
	}
	
	public void setMessagePaymentNotFound()
	{
		setMessage(String.format(DonationManager.HTML_PURCHASE_MESSAGE, "Pagamento não encontrado", "Se você já pagou, aguarde um pouco e verifique novamente."));
	}

	public String getMessage()
	{
		return _message;
	}

	public PaymentMethod getPaymentMethod()
	{
		return _paymentMethod;
	}

	public int getEmailCoint()
	{
		return _emailCount;
	}

	public void logEmailSending()
	{
		_lastEmailTime = System.currentTimeMillis();
		_emailCount++;
	}

	public synchronized void setBusy(Boolean status)
	{
		_busy = status;
	}

	public synchronized boolean tryStart() { if (_busy) return false; _busy = true; return true; }
	public boolean isBusy()
	{
		return _busy;
	}
	
	public String getFormatedPrice()
	{
		if (_paymentMethod == PaymentMethod.BINANCE)
			return getTotalPrice().toString();
		
		final NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.getDefault());
		nf.setCurrency(Currency.getInstance(_currency));
		return nf.format(getTotalPrice());
	}

	public String resume()
	{
		return String.format("%d %s - %s", getQuantity(), getProductName(), getFormatedPrice());
	}
	
	public void setPaypalInvoiceId(String id)
	{
		_paypalInvoiceId = id;
	}
	
	public String getPaypalInvoiceId()
	{
		return _paypalInvoiceId;
	}
	
	public String getCurrency()
	{
		return _currency;
	}
	
	public void setWantQrCode(boolean status)
	{
		_wantQrCode = status;
	}
	
	/*
	 * Alguns métodos de pagamento podem exibir o qrcode mesmo quando esse não é a forma de pagamento padrão
	 * Se o player optar por isso, então tratamos o checkout da compra de forma adequada a essa solicitação
	 */
	public boolean wantQrCode()
	{
		return _wantQrCode;
	}
	
	public void setTermsStatus(boolean value)
	{
		_terms = value;
	}
	
	public boolean agreedTerms()
	{
		return DonationConfig.DONATION_REQUIRE_TERMS ? _terms : true;
	}
	
	public long getLastEmailTime()
	{
		return _lastEmailTime;
	}
	
	public IPaymentHandler getPaymentHandler()
	{
		return DonationManager.getInstance().getPaymentHandler(_paymentMethod.getId());
	}
}
