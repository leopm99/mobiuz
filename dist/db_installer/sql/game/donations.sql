-- Journey donation service. Fresh installation only; do not import over legacy tables without migration.
CREATE TABLE IF NOT EXISTS donation_ids (
 id INT NOT NULL AUTO_INCREMENT PRIMARY KEY
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS donations (
 purchase_id INT NOT NULL PRIMARY KEY,
 player_id INT UNSIGNED NOT NULL,
 payment_id VARCHAR(128) DEFAULT NULL,
 payment_method VARCHAR(16) NOT NULL,
 email VARCHAR(254) DEFAULT NULL,
 product_id INT UNSIGNED NOT NULL,
 quantity INT UNSIGNED NOT NULL,
 unit_price DECIMAL(24,8) NOT NULL,
 currency VARCHAR(16) NOT NULL,
 date BIGINT NOT NULL,
 status VARCHAR(16) NOT NULL,
 terms BOOLEAN NOT NULL DEFAULT FALSE,
 expires_at BIGINT NOT NULL,
 reference VARCHAR(64) NOT NULL,
 requested BOOLEAN NOT NULL DEFAULT FALSE,
 hidden BOOLEAN NOT NULL DEFAULT FALSE,
 mp_api VARCHAR(16) NOT NULL DEFAULT 'PAYMENTS',
 mp_sandbox BOOLEAN NOT NULL DEFAULT FALSE,
 mp_sandbox_auto_approve BOOLEAN NOT NULL DEFAULT TRUE,
 mp_merchant_id VARCHAR(32) DEFAULT NULL,
 product_name VARCHAR(120) DEFAULT NULL,
 mp_request_version INT NOT NULL DEFAULT 1,
 mp_product_category VARCHAR(64) DEFAULT NULL,
 mp_statement_descriptor VARCHAR(50) DEFAULT NULL,
 buyer_first_name VARCHAR(64) DEFAULT NULL,
 buyer_last_name VARCHAR(64) DEFAULT NULL,
 buyer_cpf VARCHAR(11) DEFAULT NULL,
 UNIQUE KEY purchase_reference (reference),
 UNIQUE KEY provider_payment (payment_method,payment_id),
 KEY player_history (player_id,date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS donations_payments (
 purchase_id INT NOT NULL PRIMARY KEY,
 mp_preference_id VARCHAR(128),
 paypal_invoice_id VARCHAR(128),
 mp_transaction_id VARCHAR(128),
 qrcode TEXT,
 link TEXT,
 CONSTRAINT donation_payment_purchase FOREIGN KEY (purchase_id) REFERENCES donations(purchase_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS donation_deliveries (
 purchase_id INT NOT NULL PRIMARY KEY,
 state VARCHAR(16) NOT NULL,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT donation_delivery_purchase FOREIGN KEY (purchase_id) REFERENCES donations(purchase_id)
) ENGINE=InnoDB;
