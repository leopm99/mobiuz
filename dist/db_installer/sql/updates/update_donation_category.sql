-- Stop the game server before running. Existing requests keep their original payload version.
SET @journey_category_sql = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='donations' AND COLUMN_NAME='mp_product_category')=0, 'ALTER TABLE `donations` ADD COLUMN `mp_product_category` VARCHAR(64) DEFAULT NULL', 'SELECT 1');
PREPARE journey_category_stmt FROM @journey_category_sql;
EXECUTE journey_category_stmt;
DEALLOCATE PREPARE journey_category_stmt;
