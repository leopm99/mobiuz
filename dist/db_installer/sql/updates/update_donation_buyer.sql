-- Stop the server and back up the game database before applying. Adds optional payer snapshots; existing purchases remain unchanged.
SET @journey_buyer_sql = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='donations' AND COLUMN_NAME='buyer_first_name')=0, 'ALTER TABLE `donations` ADD COLUMN `buyer_first_name` VARCHAR(64) DEFAULT NULL', 'SELECT 1');
PREPARE journey_buyer_stmt FROM @journey_buyer_sql;
EXECUTE journey_buyer_stmt;
DEALLOCATE PREPARE journey_buyer_stmt;
SET @journey_buyer_sql = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='donations' AND COLUMN_NAME='buyer_last_name')=0, 'ALTER TABLE `donations` ADD COLUMN `buyer_last_name` VARCHAR(64) DEFAULT NULL', 'SELECT 1');
PREPARE journey_buyer_stmt FROM @journey_buyer_sql;
EXECUTE journey_buyer_stmt;
DEALLOCATE PREPARE journey_buyer_stmt;
SET @journey_buyer_sql = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='donations' AND COLUMN_NAME='buyer_cpf')=0, 'ALTER TABLE `donations` ADD COLUMN `buyer_cpf` VARCHAR(11) DEFAULT NULL', 'SELECT 1');
PREPARE journey_buyer_stmt FROM @journey_buyer_sql;
EXECUTE journey_buyer_stmt;
DEALLOCATE PREPARE journey_buyer_stmt;
