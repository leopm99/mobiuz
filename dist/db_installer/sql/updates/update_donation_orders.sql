-- Stop the game server before applying this migration. Existing purchases remain on Payments.
SET @journey_orders_sql = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='donations' AND COLUMN_NAME='mp_api')=0, 'ALTER TABLE `donations` ADD COLUMN `mp_api` VARCHAR(16) NOT NULL DEFAULT ''PAYMENTS''', 'SELECT 1');
PREPARE journey_orders_stmt FROM @journey_orders_sql;
EXECUTE journey_orders_stmt;
DEALLOCATE PREPARE journey_orders_stmt;

SET @journey_orders_sql = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='donations' AND COLUMN_NAME='mp_sandbox')=0, 'ALTER TABLE `donations` ADD COLUMN `mp_sandbox` BOOLEAN NOT NULL DEFAULT FALSE', 'SELECT 1');
PREPARE journey_orders_stmt FROM @journey_orders_sql;
EXECUTE journey_orders_stmt;
DEALLOCATE PREPARE journey_orders_stmt;

SET @journey_orders_sql = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='donations' AND COLUMN_NAME='mp_merchant_id')=0, 'ALTER TABLE `donations` ADD COLUMN `mp_merchant_id` VARCHAR(32) DEFAULT NULL', 'SELECT 1');
PREPARE journey_orders_stmt FROM @journey_orders_sql;
EXECUTE journey_orders_stmt;
DEALLOCATE PREPARE journey_orders_stmt;

SET @journey_orders_sql = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='donations_payments' AND COLUMN_NAME='mp_transaction_id')=0, 'ALTER TABLE `donations_payments` ADD COLUMN `mp_transaction_id` VARCHAR(128) DEFAULT NULL', 'SELECT 1');
PREPARE journey_orders_stmt FROM @journey_orders_sql;
EXECUTE journey_orders_stmt;
DEALLOCATE PREPARE journey_orders_stmt;
