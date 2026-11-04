package com.l2journey.gameserver.donation;

import com.l2journey.commons.util.ConfigReader;

/** Donation settings. Restart the server after changing payment settings. */
public final class DonationConfig
{
    public static boolean DONATION_MP_SANDBOX = true;
    public static boolean DONATION_MP_SANDBOX_AUTO_APPROVE = true;
    public static String DONATION_MP_MERCHANT_ID = "";
    public static String DONATION_BINANCE_MERCHANT_ID = "";
	public static String[] DONATION_ALLOWED_EMAILS;
	public static String DONATION_MP_TOKEN;
	public static String DONATION_MP_CURRENCY;
	public static int[] DONATION_MP_LINK_DROPDOWN;
	public static String[] DONATION_MP_CURRENCIES;
	public static boolean DONATION_ENABLED;
	public static boolean DONATION_REQUIRE_TERMS;
	public static boolean DONATION_CLEAR_CHAR;
	public static boolean DONATION_PAYPAL_LINK;
	public static boolean DONATION_PAYPAL_SANDBOX_ENABLED;
	public static int DONATION_PAYPAL_LINK_EXPIRATION_TIME;
	public static double DONATION_PAYPAL_PRICE;
	public static String DONATION_PAYPAL_CLIENT_ID;
	public static String DONATION_PAYPAL_CLIENT_SECRET;
	public static String DONATION_PAYPAL_ACCOUNT_EMAIL;
	public static String DONATION_PAYPAL_CURRENCY;
	public static String[] DONATION_PAYPAL_CURRENCIES;
	public static int[] DONATION_PAYPAL_DROPDOWN;
	public static String DONATION_PAYPAL_WEBSITE;
	public static String DONATION_PAYPAL_NOTE_MSG;
	public static String DONATION_PAYPAL_LOGO_IMAGE;
	public static String DONATION_PAYPAL_PHONE_NUMBER;
	public static String DONATION_PAYPAL_PHONE_CODE;
	public static boolean DONATION_PAYPAL_MAIL;
	public static boolean DONATION_BINANCE_PAY;
	public static boolean DONATION_BINANCE_MAIL;
	public static double DONATION_BINANCE_PRICE;
	public static String DONATION_BINANCE_API_KEY;
	public static String DONATION_BINANCE_SECRET_KEY;
	public static String DONATION_BINANCE_FIAT_CURRENCY;
	public static String[] DONATION_BINANCE_PAY_CURRENCY;
	public static int[] DONATION_BINANCE_DROPDOWN;
	public static int DONATION_BINANCE_EXPIRATION_TIME;
	public static int DONATION_BINANCE_CURRENCY_TASK_INTERVAL;
	public static String DONATION_SERVER_NAME;
	public static String DONATION_MAILER_TOKEN;
	public static String DONATION_MAILER_ADDRESS;
	public static String DONATION_MAILER_TEMPLATE;
	public static String DONATION_MP_PIX_ACCOUNT_OWNER;
	public static String DONATION_MP_PIX_ACCOUNT_CPF;
	public static String DONATION_MP_PIX_ACCOUNT_BANK;
	public static int[] DONATION_MP_PIX_DROPDOWN;
	public static boolean DONATION_MP_PIX;
	public static boolean DONATION_MP_LINK;
	public static boolean DONATION_DELETE_INACTIVE;
	public static boolean DONATION_DELETE_PAYMENT_DATA;
	public static boolean DONATION_HIDE_ENDED;
	public static boolean DONATION_MP_LINK_MAIL;
	public static boolean DONATION_MP_PIX_MAIL;
	public static double DONATION_MP_PIX_PRICE;
	public static double DONATION_MP_LINK_PRICE;
	public static int DONATION_MP_PIX_EXPIRATION_TIME;
	public static int DONATION_MP_LINK_EXPIRATION_TIME;
	public static int DONATION_MAXIMUM_NUMBER_EMAILS;
	public static int DONATION_PAY_TIME;
	public static int DONATION_CHECK_TIME;
	public static int DONATION_PURCHASABLE_ITEM;
	public static String DONATION_PRODUCT_NAME = "";
	public static String DONATION_MP_PRODUCT_CATEGORY = "games";
	public static String DONATION_MP_STATEMENT_DESCRIPTOR = "";
	public static boolean DONATION_MP_BUYER_DETAILS = true;
	public static int DONATION_CURRENCY_TASK_INTERVAL;
	public static String DONATION_CURRENCY_CB_API_KEY;
	public static boolean DONATION_CURRENCY_AWESOMEAPI;
	public static boolean DONATION_CALCULATOR;
	public static boolean DONATION_DROPDOWN;
	static String normalizeStatementDescriptor(String value)
	{
		final String descriptor = value.trim();
		if (!descriptor.isEmpty() && !descriptor.matches("[A-Za-z0-9 ]{1,50}"))
		{
			throw new IllegalArgumentException("MercadoPagoStatementDescriptor must contain up to 50 ASCII letters, digits or spaces");
		}
		return descriptor;
	}

	public static void load()
	{
		DONATION_ENABLED = false;
        try {
        final ConfigReader donation = new ConfigReader("./config/donation.ini");
		DONATION_ENABLED = donation.getBoolean("EnableDonationManager", false);
		DONATION_DELETE_INACTIVE = donation.getBoolean("DeleteInactivePurchases", false);
		DONATION_DELETE_PAYMENT_DATA = donation.getBoolean("DeletePaymentData", true);
		DONATION_HIDE_ENDED = donation.getBoolean("HideEndedPurchases", true);
		DONATION_ALLOWED_EMAILS = java.util.Arrays.stream(donation.getString("AllowedEmailAddresses", "").split(",")).map(String::trim).map(v -> v.toLowerCase(java.util.Locale.ROOT)).toArray(String[]::new);
		DONATION_SERVER_NAME = donation.getString("ServerName", "");
		DONATION_PURCHASABLE_ITEM = donation.getInt("PurchasableItem", 0);
		DONATION_PRODUCT_NAME = donation.getString("PurchasableItemName", "").trim();
		if (DONATION_PRODUCT_NAME.length() > 120 || DONATION_PRODUCT_NAME.chars().anyMatch(c -> Character.isISOControl(c) || "<>&\"".indexOf(c) >= 0)) throw new IllegalArgumentException("Invalid PurchasableItemName: use up to 120 plain text characters without HTML");
		DONATION_CALCULATOR = donation.getBoolean("Calculator", true);
		DONATION_DROPDOWN = donation.getBoolean("ShowDropdown", false);
		DONATION_REQUIRE_TERMS = donation.getBoolean("RequireTerms", false);
		DONATION_CLEAR_CHAR = donation.getBoolean("DeleteOnCharDelete", false);
		DONATION_MP_PRODUCT_CATEGORY = donation.getString("MercadoPagoProductCategory", "games").trim();
		if (!DONATION_MP_PRODUCT_CATEGORY.isEmpty() && !DONATION_MP_PRODUCT_CATEGORY.matches("[A-Za-z0-9_-]{1,64}")) throw new IllegalArgumentException("Invalid MercadoPagoProductCategory");
		DONATION_MP_BUYER_DETAILS = donation.getBoolean("MercadoPagoBuyerDetails", true);
		DONATION_MP_STATEMENT_DESCRIPTOR = normalizeStatementDescriptor(donation.getString("MercadoPagoStatementDescriptor", ""));
		DONATION_MP_TOKEN = donation.getString("MercadoPagoApiToken", "");
		DONATION_MP_PIX = donation.getBoolean("MercadoPagoPix", false);
		DONATION_MP_PIX_PRICE = donation.getDouble("MercadoPagoPixPrice", 1.);
		DONATION_MP_PIX_EXPIRATION_TIME = donation.getInt("MercadoPagoPixExpirationTime", 30);
		DONATION_MP_PIX_ACCOUNT_OWNER = donation.getString("MercadoPagoPixAccountOwner", "");
		DONATION_MP_PIX_ACCOUNT_CPF = donation.getString("MercadoPagoPixAccountCpf", "");
		DONATION_MP_PIX_ACCOUNT_BANK = donation.getString("MercadoPagoPixAccountBank", "");
		DONATION_MP_PIX_DROPDOWN = java.util.Arrays.stream(donation.getString("MercadoPagoPixDropdown", "").split(",")).filter(v -> !v.isBlank()).map(String::trim).mapToInt(Integer::parseInt).toArray();
		DONATION_MP_PIX_MAIL = donation.getBoolean("MercadoPagoPixMail", false);
		DONATION_MP_LINK = donation.getBoolean("MercadoPagoLink", false);
		DONATION_MP_LINK_PRICE = donation.getDouble("MercadoPagoLinkPrice", 1.);
		DONATION_MP_CURRENCY = donation.getString("MercadoPagoCurrency", "");
		DONATION_MP_CURRENCIES = donation.getString("MercadoPagoCurrencies", "").split(",");
		DONATION_MP_LINK_EXPIRATION_TIME = donation.getInt("MercadoPagoLinkExpirationTime", 30);
		DONATION_MAXIMUM_NUMBER_EMAILS = donation.getInt("MaximumNumberEmails", 2);
		DONATION_MP_LINK_MAIL = donation.getBoolean("MercadoPagoLinkMail", true);
		DONATION_MP_LINK_DROPDOWN = java.util.Arrays.stream(donation.getString("MercadoPagoLinkDropdown", "").split(",")).filter(v -> !v.isBlank()).map(String::trim).mapToInt(Integer::parseInt).toArray();
		DONATION_PAYPAL_LINK = donation.getBoolean("PayPalLink", false);
		DONATION_PAYPAL_PRICE = donation.getDouble("PayPalPrice", 1.);
		DONATION_PAYPAL_CLIENT_ID = donation.getString("PayPalClientId", "");
		DONATION_PAYPAL_CLIENT_SECRET = donation.getString("PayPalClientSecret", "");
		DONATION_PAYPAL_ACCOUNT_EMAIL = donation.getString("PayPalAccountEmail", "");
		DONATION_PAYPAL_SANDBOX_ENABLED = donation.getBoolean("PayPalSandboxEnabled", true);
		DONATION_PAYPAL_LINK_EXPIRATION_TIME = donation.getInt("PayPalLinkExpirationTime", 30);
		DONATION_PAYPAL_CURRENCY = donation.getString("PayPalCurrency", "");
		DONATION_PAYPAL_CURRENCIES = donation.getString("PayPalCurrencies", "").split(",");
		DONATION_PAYPAL_WEBSITE = donation.getString("PayPalWebSite", "");
		DONATION_PAYPAL_NOTE_MSG = donation.getString("PayPalNoteMsg", "");
		DONATION_PAYPAL_LOGO_IMAGE = donation.getString("PayPalLogoImage", "");
		DONATION_PAYPAL_PHONE_NUMBER = donation.getString("PayPalPhoneNumber", "");
		DONATION_PAYPAL_PHONE_CODE = donation.getString("PayPalPhoneCountryCode", "");
		DONATION_PAYPAL_DROPDOWN = java.util.Arrays.stream(donation.getString("PayPalDropdown", "").split(",")).filter(v -> !v.isBlank()).map(String::trim).mapToInt(Integer::parseInt).toArray();
		DONATION_PAYPAL_MAIL = donation.getBoolean("PayPalMail", true);
		DONATION_BINANCE_PAY = donation.getBoolean("BinancePay", false);
		DONATION_BINANCE_PRICE = donation.getDouble("BinancePrice", 1.);
		DONATION_BINANCE_API_KEY = donation.getString("BinanceApiKey", "");
		DONATION_BINANCE_SECRET_KEY = donation.getString("BinanceSecretKey", "");
		DONATION_BINANCE_FIAT_CURRENCY = donation.getString("BinanceFiatCurrency", "");
		DONATION_BINANCE_PAY_CURRENCY = donation.getString("BinancePayCurrency", "").split(",");
		DONATION_BINANCE_MAIL = donation.getBoolean("BinanceMail", false);
		DONATION_BINANCE_EXPIRATION_TIME = donation.getInt("BinanceExpirationTime", 30);
		DONATION_BINANCE_DROPDOWN = java.util.Arrays.stream(donation.getString("BinanceDropdown", "").split(",")).filter(v -> !v.isBlank()).map(String::trim).mapToInt(Integer::parseInt).toArray();
		DONATION_BINANCE_CURRENCY_TASK_INTERVAL = donation.getInt("BinanceCurrencyTaskInterval", 5);
		DONATION_CURRENCY_CB_API_KEY = donation.getString("CurrencyBeaconApiKey", "");
		DONATION_CURRENCY_AWESOMEAPI = donation.getBoolean("AwesomeApi", false);
		DONATION_CURRENCY_TASK_INTERVAL = donation.getInt("FiatCurrencyTaskInterval", 60);
		DONATION_MAILER_TOKEN = donation.getString("MailerApiToken", "");
		DONATION_MAILER_ADDRESS = donation.getString("MailerAddress", "");
		DONATION_MAILER_TEMPLATE = donation.getString("MailerTemplateId", "");
		DONATION_PAY_TIME = donation.getInt("PayTime", 20000);
		DONATION_CHECK_TIME = donation.getInt("CheckTime", 10000);
        DONATION_MP_SANDBOX = donation.getBoolean("MercadoPagoSandbox", true);
        DONATION_MP_SANDBOX_AUTO_APPROVE = donation.getBoolean("MercadoPagoSandboxAutoApprove", true);
        DONATION_MP_MERCHANT_ID = donation.getString("MercadoPagoMerchantId", "").trim();
        DONATION_BINANCE_MERCHANT_ID = donation.getString("BinanceMerchantId", "").trim();
        if (DONATION_ENABLED)
        {
         if (DONATION_MP_PIX && (DONATION_MP_PIX_EXPIRATION_TIME < 30 || DONATION_MP_PIX_EXPIRATION_TIME > 43200)) throw new IllegalArgumentException("PIX expiration must be between 30 minutes and 30 days");
         if (DONATION_PURCHASABLE_ITEM <= 0 || DONATION_PAY_TIME < 1000 || DONATION_CHECK_TIME < 1000) throw new IllegalArgumentException("Invalid item or request intervals");
         if ((DONATION_MP_PIX || DONATION_MP_LINK) && (DONATION_MP_TOKEN.isBlank() || !DONATION_MP_MERCHANT_ID.matches("[1-9][0-9]*"))) throw new IllegalArgumentException("Mercado Pago token and merchant id are required");
         if (DONATION_PAYPAL_LINK && (DONATION_PAYPAL_CLIENT_ID.isBlank() || DONATION_PAYPAL_CLIENT_SECRET.isBlank() || DONATION_PAYPAL_ACCOUNT_EMAIL.isBlank())) throw new IllegalArgumentException("PayPal credentials are required");
         if (DONATION_BINANCE_PAY && (DONATION_BINANCE_API_KEY.isBlank() || DONATION_BINANCE_SECRET_KEY.isBlank() || !DONATION_BINANCE_MERCHANT_ID.matches("[1-9][0-9]*"))) throw new IllegalArgumentException("Binance credentials and merchant id are required");
         for (double price : new double[] { DONATION_MP_PIX_PRICE,DONATION_MP_LINK_PRICE,DONATION_PAYPAL_PRICE,DONATION_BINANCE_PRICE }) if (!Double.isFinite(price) || price <= 0) throw new IllegalArgumentException("Prices must be positive and finite");
         for (int[] dropdown : new int[][] { DONATION_MP_PIX_DROPDOWN,DONATION_MP_LINK_DROPDOWN,DONATION_PAYPAL_DROPDOWN,DONATION_BINANCE_DROPDOWN }) for (int quantity : dropdown) if (quantity <= 0 || quantity > 999) throw new IllegalArgumentException("Quantities must be between 1 and 999");
         for (String currency : new String[] { DONATION_MP_CURRENCY,DONATION_PAYPAL_CURRENCY,DONATION_BINANCE_FIAT_CURRENCY }) java.util.Currency.getInstance(currency);
        }
        }
        catch (RuntimeException e) { DONATION_ENABLED = false; java.util.logging.Logger.getLogger(DonationConfig.class.getName()).log(java.util.logging.Level.SEVERE,"Donation service disabled: invalid configuration",e); }

	}
}
