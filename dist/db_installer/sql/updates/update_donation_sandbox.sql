-- Stop the game server before running. Keeps APRO for existing purchase snapshots.
SET @journey_sandbox_sql = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='donations' AND COLUMN_NAME='mp_sandbox_auto_approve')=0, 'ALTER TABLE `donations` ADD COLUMN `mp_sandbox_auto_approve` BOOLEAN NOT NULL DEFAULT TRUE', 'SELECT 1');
PREPARE journey_sandbox_stmt FROM @journey_sandbox_sql;
EXECUTE journey_sandbox_stmt;
DEALLOCATE PREPARE journey_sandbox_stmt;
