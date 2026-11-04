-- Stop the game server before running. Existing Orders keep their original request body.
SET @journey_product_sql = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='donations' AND COLUMN_NAME='product_name')=0, 'ALTER TABLE `donations` ADD COLUMN `product_name` VARCHAR(120) DEFAULT NULL', 'SELECT 1');
PREPARE journey_product_stmt FROM @journey_product_sql;
EXECUTE journey_product_stmt;
DEALLOCATE PREPARE journey_product_stmt;
SET @journey_product_sql = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='donations' AND COLUMN_NAME='mp_request_version')=0, 'ALTER TABLE `donations` ADD COLUMN `mp_request_version` INT NOT NULL DEFAULT 1', 'SELECT 1');
PREPARE journey_product_stmt FROM @journey_product_sql;
EXECUTE journey_product_stmt;
DEALLOCATE PREPARE journey_product_stmt;
