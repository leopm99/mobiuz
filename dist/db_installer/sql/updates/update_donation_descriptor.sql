-- Stop the game server before running. Existing requests keep their original payload.
SET @journey_descriptor_sql = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='donations' AND COLUMN_NAME='mp_statement_descriptor')=0, 'ALTER TABLE `donations` ADD COLUMN `mp_statement_descriptor` VARCHAR(50) DEFAULT NULL', 'SELECT 1');
PREPARE journey_descriptor_stmt FROM @journey_descriptor_sql;
EXECUTE journey_descriptor_stmt;
DEALLOCATE PREPARE journey_descriptor_stmt;
