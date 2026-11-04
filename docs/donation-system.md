# Donation system for L2Journey

Adapted from coldplay's DonationManager supplied in donation.zip (aCis 407), with the L2JMega patch supplied by the user as an integration reference. The original forum discussion and its automatic payment example informed the implementation. This is an adaptation, not a claim of authorship of the original system.

## Installation and first test

1. Compile the core with Java 25. Deploy the rebuilt GameServer.jar, the new libraries from dist/libs, the two donation scripts and the updated MasterHandler. Eclipse's .classpath includes the added dependencies; refresh the project if necessary.
2. Import dist/db_installer/sql/game/donations.sql into the game database. This script is for a fresh installation; it does not migrate an existing donation database. All four donation tables must use InnoDB. The server fails closed if the tables cannot be restored or the original DOUBLE price schema is detected. No SQL has been executed by this adaptation.
3. Deploy config/donation.ini and data/html/mods/donation. Enable only the provider to be tested, configure its credentials and receiving merchant id, select a valid stackable reward item, and then enable EnableDonationManager. Start with Mercado Pago PIX/BRL. Keep MercadoPagoSandbox=True for sandbox payments. PayPal has its own PayPalSandboxEnabled setting. Sandbox payments grant test items, so use a test server and test characters.
4. Restart and use .donate. No commandname-e.dat modification is required. The existing bypass and tutorial handlers route the commands, retaining HTML action validation. Check the initial page, terms, email, quantity, QR, history, manual verification, offline delivery and restart recovery in game before accepting real payments.

The original 21 HTML templates were copied without modification. QR encoding delegates DDS conversion to Journey's existing DDSConverter. The crest id is transient and allocated by Journey's IdManager, independently of the persistent purchase id; the close action still uses the purchase id. No existing CommunityBoard HTML was changed. The history renderer also closes its item links correctly; the original Java builder had an opening tag in place of the closing tag.

## Persistence and reconciliation

Purchase ids use a dedicated database sequence. Amounts use DECIMAL(24,8); reference, expiration, price, quantity and currency are stored per purchase. Provider payment ids are unique per method. No request is sent until its purchase and request marker have been saved. PIX retries use a stable idempotency key. Ambiguous creation attempts for other methods are not blindly repeated: a GM must reconcile the purchase rather than risk charging twice.

All purchases and provider metadata are retained for auditing, independently of character deletion. The upstream deletion options remain readable for compatibility, but the adaptation does not physically delete financial history. Do not rely on DeleteInactivePurchases, DeletePaymentData or DeleteOnCharDelete to erase records.

Bounded, rotating checks run every 30 seconds, with at most five purchases scheduled per cycle. Requested, unfinished purchases remain eligible for seven days, including canceled/expired purchases, to catch payments that complete late. This is polling, not a webhook implementation. Queueing and network timeouts bound load; large installations need provider-specific rate-limit tuning. Purchases beyond the seven-day automatic window require manual provider reconciliation.

Approved payments first become OFFLINE (paid, awaiting items). The same delivery path handles online characters and subsequent logins. The purchasable item must be stackable (the intended donation-token use case), so one inventory object can be checked durably. Inventory capacity and weight are checked before reserving delivery. New purchases are rate-limited even before email registration, and each character may have at most three active purchases. Payment references, exact amounts, currency and merchant identity are checked before approval; PayPal manual/external payments do not grant items. Mercado Pago's expected sandbox/live environment is also checked.

Delivery ledger states:

- PENDING: payment recorded, no item delivery reserved yet.
- RESERVED: a delivery was claimed before adding items. This state is never retried automatically.
- DELIVERED: item persistence was checked and completion committed.

The delivery ledger and the existing inventory subsystem do not share one database transaction. A crash between reservation and completion leaves RESERVED, which is logged for manual reconciliation. Inspect provider payment, purchase, inventory and item logs before deciding whether to complete the record or authorize a new delivery. Never bulk-reset RESERVED rows to PENDING. The design prevents blind redelivery but does not promise fully automatic exactly-once recovery across those two subsystems.

Refunds/chargebacks and removal of already-delivered items remain administrative operations. Financial records are retained. The upstream refund helper is not exposed as a player command.

## Libraries and API validation

New PIX purchases use the Orders API directly through JDK HttpClient. Mercado Pago SDK 2.1.24 is retained only for existing Payments purchases and the separate Checkout Pro/Preferences link integration. ZXing versions remain unchanged. The old MailerSend SDK was replaced with the documented REST endpoint using the JDK HttpClient, eliminating its SDK and missing Commons IO attachment dependency. PayPal and Binance also use JDK HttpClient. Existing Apache dependencies are still referenced by Mercado Pago and are not interchangeable with HttpClient 5 jars.

Official documentation consulted:

- https://github.com/mercadopago/sdk-java
- https://developer.paypal.com/api/invoicing/v2/schema.json
- https://developers.binance.com/en/docs/products/binance-pay-merchant/api-order-query-v2
- https://developers.mailersend.com/api/v1/email
- https://github.com/zxing/zxing

No authenticated gateway calls, real payments, database installation or in-game tests have been performed. Compilation and mocked tests do not certify production credentials, gateway account eligibility, current merchant restrictions, exchange-rate availability or client rendering. Configuration changes require a restart.

## Local validation

The full core and the donation handlers compile with Java 25. The donation suite contains 101 tests covering payment matching, manual PayPal payment rejection, delivery reservations, offline/full-inventory handling, persistence failures, purchase ownership, restoration of completed purchase metadata, actual Mercado Pago SDK date serialization/deserialization on Java 25 and QR/DDS generation. Gateway calls and database access are mocked where applicable; SDK serialization and QR generation execute locally.

## PIX migration to Orders

New PIX charges use POST /v1/orders. Manual and automatic verification use GET /v1/orders/{id}; unknown ids after a timeout are recovered with GET /v1/orders using external_reference and the required date range, followed by retrieval of the full order. Ambiguous references are held for manual reconciliation. Cancellation first checks payment status, uses POST /v1/orders/{id}/cancel with its own stable idempotency key when allowed, and retrieves the result again. No automatic refund or item withdrawal is added.

For an existing Journey installation, stop the server and import dist/db_installer/sql/updates/update_donation_orders.sql. It conditionally adds the protocol/account/environment snapshot columns and the transaction id column. It does not delete or convert existing purchases. For a new installation, use the updated dist/db_installer/sql/game/donations.sql instead. No SQL was executed during development.

The purchase stores its ORD identifier in payment_id and its nested PAY identifier in donations_payments.mp_transaction_id. mp_api distinguishes Orders from Payments. Unrequested PIX purchases can use Orders when first submitted. Previously requested Payments purchases are retrieved through their original API; the separate MP_LINK flow continues using Checkout Pro/Preferences. Do not downgrade to a build without Orders support once Orders purchases have been created.

Approval requires processed/accredited at both order and transaction levels, exact expected total and paid amounts, matching reference, merchant user_id and saved identifiers, a single PIX bank_transfer transaction, Brazil/BRL, and no refund or chargeback entries. Orders responses do not consistently provide live_mode or currency fields; when supplied they are checked, and country_code=BRA plus PIX establishes the BRL flow. The original account and sandbox flag are frozen before sending the request. Changing either blocks requests for existing Orders until the original settings are restored for reconciliation. HTTP 429/5xx responses activate backoff, honoring Retry-After up to one day, with a minimum of 30 seconds.

### Sandbox setup

Choose Checkout Transparente / API de Orders in the Mercado Pago application. Configure the private Access Token from the appropriate test environment and its receiving user id. Do not infer the environment from the token prefix: test credentials may also start with APP_USR. MercadoPagoSandbox must remain True for sandbox tests. It adds the test request header. When MercadoPagoSandboxAutoApprove=True, it also adds the documented payer.first_name=APRO simulation; it does not turn production credentials into test credentials. Test only on a dedicated game server with test characters.

The official PIX test scenario initially returns action_required and later simulates approval automatically. A real bank transfer is not required for this sandbox scenario. Preserve RequireTerms and the same UI flow when testing QR display, status refresh, background confirmation, offline delivery and restart recovery. Rendering and authenticated provider responses still require in-game/provider tests; the local suite uses simulated transports and does not contact Mercado Pago.

Official references used for the migration:

- https://www.mercadopago.com.br/developers/en/docs/checkout-api-orders/payment-integration/websites/pix
- https://www.mercadopago.com.br/developers/en/docs/checkout-api-orders/integration-test/pix
- https://www.mercadopago.com.br/developers/en/reference/online-payments/checkout-api/get-order/get
- https://www.mercadopago.com.br/developers/en/reference/online-payments/checkout-api/search-order/get
- https://www.mercadopago.com.br/developers/en/reference/online-payments/checkout-api/cancel-order/post

## Pending, cancellation and expiration sandbox tests

MercadoPagoSandboxAutoApprove controls only the sandbox APRO trigger. True sends payer.first_name=APRO; False omits first_name instead of inventing unsupported PIX rejection codes. Production never sends APRO. The default when the key is absent remains True for compatibility; the supplied config explicitly sets False for the next tests. No credentials were changed.

Before deploying this change on an existing installation, stop the gameserver and import dist/db_installer/sql/updates/update_donation_sandbox.sql. It adds mp_sandbox_auto_approve with default True so existing purchases retain their original APRO request. A new installation uses the updated donations.sql. This flag is captured before the first request and persisted; changing it only affects new purchases. Compile the core and restart after updating config.

Keep MercadoPagoSandbox=True, MercadoPagoSandboxAutoApprove=False and MercadoPagoPixExpirationTime=30. Create a NEW five-item purchase. Record inventory count and order id. Wait two minutes, query its status and inspect the same order in MP's Test panel. The expected unpaid path is action_required/waiting_transfer with no reward. The official PIX documentation explicitly documents APRO auto-approval; the behavior without that trigger must be confirmed against the actual sandbox response. If it still approves, stop this scenario and report its status; do not switch to production credentials to force a pending state.

Once a pending order is confirmed, test cancellation from the game's purchase page, including confirmation. Verify the same order becomes canceled in MP and grants no items. Relog and inspect the same purchase to check there is no delayed delivery.

For expiration, create a DIFFERENT pending order and leave it unpaid and uncanceled. Keep the gameserver running for at least the configured 30 minutes plus time for the provider's status update and the reconciliation task. Check the provider's terminal status and the local purchase; no reward may be granted. Do not shorten below MP's 30-minute minimum, manually rewrite purchase dates in the database, or change the system clock. If MP has not reached a terminal status yet, report the observed state rather than declaring expiration passed.

All tests remain on test credentials and test characters. This setting changes the request scenario, not local reward rules: rewards still require a verified accredited order and transaction.

## Provider expiration classification

Orders canceled or failed with status_detail=expired are recorded as EXPIRED. For a terminal canceled/failed order, the single payment's expired status/detail is also recognized. The local deadline alone never proves a provider expiration. Explicit API cancellation remains CANCELED, and an actual later accreditation can still be reconciled through the normal guarded delivery path. The generic canceled description no longer assumes that a player requested it.

Already-misclassified recent purchases are corrected by the regular reconciliation task when MP returns the expiration detail. No SQL rewrite or new schema migration is required. Keep the same credentials for those purchases. The status_others.htm footer uses wider cells and smaller padding for Voltar/Excluir; no bypasses or table structure were changed.

Reference: https://www.mercadopago.com.br/developers/pt/docs/checkout-api-orders/notifications (expired detail can accompany canceled/failed).


### Product data in Orders requests

Apply `dist/db_installer/sql/updates/update_donation_product.sql` with the server stopped before deploying this version (in addition to the previous Orders and sandbox updates). Fresh installations already include these columns. The update is repeatable and does not change existing purchases.

`PurchasableItemName` in `config/donation.ini` optionally overrides the item XML display name for new purchases. Leave it blank to use the XML name. Use up to 120 plain text characters, without HTML or control characters. The item ID still determines the actual reward. No HTML templates or layout were changed.

The product name is stored with each new purchase. New PIX Orders send `items.title`, `items.quantity`, `items.unit_price` and `items.external_code` (the reward item ID). Quantity times unit price must equal the order amount. Existing requested Orders retain request version 1 and their original body when retrying the same idempotency key. New Orders use version 2. Real buyer identity, device ID and webhooks require separate work; no invented buyer data is sent to improve the quality score.


## Integration quality: applicable practices and operational checks

The MP panel score is an advisory assessment of the provider's observed requests. Local compilation and tests do not guarantee points; create a new sandbox order after deployment and measure that order again. Previously sent orders intentionally retain their original payload.

Implemented in the native-game integration:

- Product title, quantity, exact unit price and external product code: versioned snapshots for new Orders. The product code is the reward item ID, not a personal identifier.
- Unique external reference: database-enforced purchase reference based on server and sequence IDs, without buyer email, account name, character name or tax ID. Preserve the database sequence when restoring backups; do not reuse an old sequence with live provider orders.
- Idempotency: a separate stable key per purchase/operation. Retry the SAME creation with the SAME key; a different purchase or cancellation gets a different key. Never generate a new key for each network retry.
- PIX and cancellation through MP's API, with a canonical lookup after cancellation to reconcile a concurrent approval.
- Background retrieval by order ID and reference recovery after ambiguous creation, independently of the manual check button. These are polling/recovery mechanisms; no webhook receiver is currently implemented.
- HTTPS outbound requests restricted to TLS 1.2/1.3, normal JDK trust validation and HTTPS hostname validation, no redirects, bounded timeouts and provider backoff. This does not install an HTTPS certificate for a public website.
- Logs of purchase creation, persisted status transitions and committed delivery, using local purchase IDs. Gateway failures identify the operation and exception type without dumping bodies, private tokens, buyer data, QR codes or exception messages. The normal Journey logging configuration controls log files and retention. Restrict access and back up purchase/delivery tables for reconciliation.
- Distinct pending, approved/delivered, failed, canceled and expired responses, plus explicit offline delivery handling. Existing game templates are retained.

Sandbox validation observed by the user: approval via the manual button, automatic approval detection, offline delivery, no redelivery after relog/restart, unpaid waiting, explicit cancellation and natural expiration. A declined payment has local mocked coverage but still needs an official supported PIX rejection scenario or provider evidence; do not invent a payer name to simulate it. For any future scenario record local purchase ID, ORD/PAY IDs, expected item count, provider state and actual inventory delta.

Do not fill payer first/last name, phone or identification with character/account names or invented values. Native game authentication is not proof of the buyer's legal identity. Account registration date, first online purchase and previous purchase date also need reliable account-level records and confirmation of the provider's field semantics before transmission. Optional buyer name, surname and CPF collection is now available as described below; no account/character data is repurposed as buyer identity.

Product category is configurable as described below; confirm its acceptance in a new sandbox order. Integrator ID applies only to registered/certified partners; no fabricated ID is sent. The current Orders client uses the REST API directly; the legacy Java SDK is not counted as Orders SDK integration. MercadoPago.js/device-session collection and official web logos require a separate web experience; the native game HTML cannot run those scripts.

A public webhook endpoint requires an HTTPS URL and trusted certificate, validated provider signatures, request limits, replay/idempotency checks and retrieval of the canonical order before delivery. Polling remains a fallback after any future webhook deployment. No endpoint was exposed by this change.

Refunds must currently be initiated and reconciled administratively in MP. A game-facing refund button would require policies for rewards already consumed/transferred, dispute handling and financial authorization. No automatic item withdrawal or refund was added. Releases/account-money reports are operational financial reconciliation: compare MP reports with saved ORD/PAY identifiers and donation delivery records, never infer spendable net proceeds from gross order totals. Direct Bacen integration is a separate payment-provider project, not a flag required by the existing MP PIX integration.

For deployment: back up the game database; stop the gameserver; apply the new product migration along with any older unapplied migrations; rebuild/deploy core; update donation.ini preserving private credentials; restart; create a NEW sandbox purchase; confirm title/quantity/unit price in the provider request and repeat the single-delivery check; then request a new quality measurement. Keep test and production records/credentials separated. The code changes here do not provision public HTTPS infrastructure, revoke leaked credentials, or certify production readiness.


### Configurable product category

For an existing database, stop the server and apply `dist/db_installer/sql/updates/update_donation_category.sql` before deploying the rebuilt core. This is a NEW update; the previous product/sandbox/Orders migrations still need to have been applied. Fresh installations include the new `mp_product_category` column in donations.sql. No SQL was executed during development.

`MercadoPagoProductCategory = games` selects the category for newly submitted PIX Orders. An empty value omits `category_id`. Values must be up to 64 ASCII letters, digits, hyphens or underscores. This validates local format, not membership in the provider's category catalog. The default `games` is documented by Mercado Pago for the games/online-platform sector in its Payments guidance; the Orders reference/schema defines `items.category_id` as a string without an enumerated list. Its acceptance and quality-score effect in this exact PIX Orders flow still require a new sandbox request. Do not promise an increase in points from local tests.

The category is frozen before the first network request and persisted with request version 3. A restart or config change does not change a requested purchase. Requests previously sent with version 1 (no items) or version 2 (items without category) retain their original body and idempotency key when retried. No automatic retry under a new key or fallback payload is performed if the provider rejects the category. Change the setting only for a new purchase after reconciling the original request.

Recompile/deploy the core, copy the updated config preserving private credentials, restart, create a NEW sandbox purchase, check the expected one-time reward and remeasure its ORD identifier. HTML files and builders were not changed by this category update.

Sources:
- https://www.mercadopago.com.br/developers/en/reference/online-payments/checkout-api/create-order/post
- https://raw.githubusercontent.com/mercadopago/openapi/main/schemas/orders.yaml
- https://www.mercadopago.com.br/developers/pt/docs/checkout-api-payments/how-tos/improve-payment-approval/industry-data/applications-online-platforms?scope=prod


### Optional explicit statement descriptor experiment

The account's invoice-name change did not satisfy the user's quality evaluation for a new PIX order. Do not assume delayed propagation or claim that this is proof of a local error. Orders documents `transactions.payments[].payment_method.statement_descriptor`; published examples use card payments. The field's acceptance and quality-score effect for this PIX integration are still unverified.

The new `MercadoPagoStatementDescriptor` config is blank by default, which omits the field and preserves the current PIX behavior. For a sandbox-only experiment, set `MercadoPagoStatementDescriptor = L2JOURNEY`, restart and create a NEW purchase. The optional value accepts up to 50 ASCII letters, digits or spaces (prefer a short merchant label). This field identifies the merchant on statements; it is separate from PurchasableItemName and does not change the reward, payment method or HTML.

Before deployment, stop the server and apply the NEW `dist/db_installer/sql/updates/update_donation_descriptor.sql` in addition to earlier unapplied migrations; rebuild and deploy the core and updated donation.ini preserving private credentials. The migration adds nullable `mp_statement_descriptor`; it is repeatable and does not rewrite existing requests. Fresh databases include it in donations.sql. No SQL or authenticated provider requests were executed during development.

Request version 4 saves the descriptor before the first network call. Existing requested versions 1-3 never gain the field on retry. Restarting or editing config does not change a requested purchase's payload. If the provider rejects the field, no automatic alternate-body retry or new idempotency key is used. Keep the original purchase for reconciliation, disable the setting for future purchases and report the rejection. Avoid paying against an uncertain request or changing an existing stored payload to force a retry.

For validation: use sandbox credentials; create a new order with the explicit descriptor; confirm whether MP accepts it and, if available, returns it in the canonical order/API Requests panel; run the approved single-delivery test; measure the SAME new ORD identifier. A successful API response and a higher quality score are separate checks. Do not claim the 13 points until the panel marks the criterion achieved. If rejected or ignored for PIX, restore the config to blank and raise the discrepancy with MP support; do not implement card payments just to obtain these points.

Official reference: https://www.mercadopago.com.br/developers/pt/docs/checkout-api-orders/resources/migrate-payments-to-orders


### Optional buyer name, surname and CPF

Apply the NEW `dist/db_installer/sql/updates/update_donation_buyer.sql` with the gameserver stopped and a database backup before deploying this version. It adds nullable `buyer_first_name`, `buyer_last_name` and `buyer_cpf` columns, preserving old purchases. Fresh installations include them in donations.sql. Earlier migrations must already be applied. No SQL or authenticated MP calls were executed during this work.

`MercadoPagoBuyerDetails = True` enables a new native form before each new PIX purchase. Deploy `data/html/mods/donation/buyer.htm` as well as the rebuilt core and updated config. The form explains that real buyer data will be sent to Mercado Pago. First name, surname and CPF are independently optional; leaving everything blank or choosing Pular continues the normal purchase flow. Setting the config to False restores the original purchase flow for new purchases. Existing email and terms screens remain intact, and other payment methods do not collect these fields.

Names support Unicode letters/combining marks, spaces, apostrophes and hyphens, up to 64 UTF-16 units. Control characters, markup and unsupported characters are rejected without echoing input. Compound names are preserved through explicit field delimiters with whitespace around each HTML variable. Blank native edit-box values of 0 are treated as absent. CPF accepts eleven digits or standard punctuation and validates check digits/repeated sequences; this is only a local format check, not verification of identity or ownership. No fake data or character nicknames are populated.

The entered details are saved per purchase, not in character/account profile variables. They are not echoed in history/status HTML or logs. A redacted toString avoids accidental exposure by the value object. The database/backups contain the supplied personal information and require access control. The optional data is retained with the existing purchase history for matching the original provider request; no automatic deletion policy was added.

Version 5 sends non-empty `payer.first_name`, `payer.last_name` and `payer.identification` (type CPF, normalized number). Existing request versions 1-4 do not gain fields on retry. Details cannot be edited after the request marker is set. The game HTML-action validation and purchase ownership checks remain enabled. Malformed input does not store or submit the purchase; the user may correct it, leave fields blank or skip. After names are accepted, a missing email still goes through the existing email screen, preserving the entered details in the pending draft.

Sandbox APRO remains the provider's explicit auto-approval trigger and overrides first_name only when MercadoPagoSandbox and MercadoPagoSandboxAutoApprove are both True. To measure the actual optional first name, keep sandbox enabled, set MercadoPagoSandboxAutoApprove=False, create a new test order, then simulate approval through the MP panel and verify a single reward. Use the provider's allowed test identity for sandbox, not arbitrary real persons' data. Measure the same new ORD identifier; local tests do not guarantee points.

The `accounts.created_time` column exists in the login database schema, but the current login/game communication does not supply it to the donation service. It was not replaced with a character's creation date or first purchase date. Registration-date transmission remains pending until reliable account history and an Orders-compatible field are verified.

Validation: 90 local tests passed, including optional blanks/skip, compound/accented names, CPF checks, malformed field boundaries, data restoration, immutable requested snapshots, single continuation, draft ownership and rejection of edits to already-requested purchases. Client rendering and MP evaluation remain in-game/provider tests.

Sources:
- https://www.mercadopago.com.br/developers/en/reference/online-payments/checkout-api/create-order/post
- https://www.mercadopago.com.ar/developers/en/docs/checkout-api-orders/payment-management/improve-payment-approval/industry-data


### Automatic reconciliation after switching sandbox/production

The user reported a successful real PIX purchase: scanned/paid QR, one reward delivery and COMPLETED. The repeated warnings on old purchases after switching to production were consistent with the saved account/environment guard rejecting those old sandbox orders. No authenticated requests or changes to the user's running server/database were performed during this fix.

Automatic reconciliation now filters Orders purchases by the saved receiving merchant ID and sandbox flag before dispatching work. The expiration task also removes mismatched-context purchases from its active queue without expiring/canceling them or contacting the provider. Reinstating the original account/environment allows eligible recent purchases to be reconciled again. Manual checks explain the context mismatch instead of reporting a generic network failure. The original snapshots are never rewritten to match new credentials.

This does not identify a different MP application/token within the same receiving account/environment: no token or token fingerprint is persisted. Such requests may still receive provider errors, handled with bounded retries. Application deletion must not be assumed to prove why any individual resource returns 404.

Failed Orders checks have transient per-purchase backoff: 404 starts at five minutes and doubles up to one hour; other failures start at thirty seconds and double up to fifteen minutes. A successful query clears the purchase's retry state. A manual check may retry before the automatic delay; existing provider-wide 429/5xx backoff is still enforced. One unavailable order does not delay unrelated purchases. Inactive retry records expire after seven days when failures are processed. These retry delays are in memory and reset on restart.

Gateway failures now produce one contextual warning rather than separate transport and purchase warnings. Repeated identical failures are logged at most once per five minutes per purchase; a changed failure category may log sooner. Only local ID, operation, exception class, HTTP status (when available), and retry delay are logged, not tokens, raw provider bodies, exception messages, names or CPF. Errors remain visible; they are not converted into successful payments, cancellations or expirations.

The original seven-day reconciliation window and recovery of late approvals for canceled/expired/failed purchases in the CURRENT context are preserved. Completed/OFFLINE/HIDDEN-state records remain excluded. Hiding a history entry with the separate hidden flag still does not delete it or discard financial reconciliation; in particular it is not a substitute for canceling an outstanding provider payment. Sandbox/production separation and retry management address the recurring warnings without deleting financial records.

Deployment requires rebuilding/deploying GameServer.jar and restarting. No new SQL migration, HTML update, credential change or config key is required by this reconciliation fix. If warnings remain, the new message identifies the purchase, HTTP status and retry delay for diagnosis.

Validation: 101 donation tests passed locally. Added checks cover environment/account selection, resuming in the original context, preserving late-payment reconciliation, explanation for manual mismatch, per-purchase 404/network backoff, isolation between orders, manual recovery and HTTP status propagation. Provider/network/database operations are mocked for these checks.


### Journey-native log formatting

Donation classes and handlers now use java.util.logging.Logger, matching the project's native logging setup. Messages are formatted by the existing ConsoleLogFormatter as `[dd/MM HH:mm:ss] ClassName: message`, using the server JVM time zone. No timestamps are inserted manually and no global logging formatter/configuration was changed. Warning/error levels use WARNING/SEVERE and exception-bearing records use JUL's log overload instead of SLF4J placeholder arguments. Parameter values are resolved in the message itself because Journey's console/file formatters read LogRecord.getMessage() directly.

This change also covers the executor, periodic jobs, currency tasks, MailerSend and legacy MP/PayPal/Binance handlers. No SLF4J APIs remain in the donation module; the project's SLF4J libraries remain installed for other dependencies. The Orders failure-message redaction and retry/log throttling remain intact. Financial validation, HTTP requests, saved purchase contexts and HTML are unchanged.

The donation source compiled in Java 25 and all 101 existing donation tests passed. A local probe confirmed the native console date/time prefix without starting the gameserver. Deploy the rebuilt GameServer.jar and restart; no SQL, HTML deployment or new logging config is required for this change.
