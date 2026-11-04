-- Run on the game database before starting the updated server.
-- Preserves existing rebirth counts. Adds the journal columns only when missing.
CREATE TABLE IF NOT EXISTS `character_rebirths` (
  `playerId` INT UNSIGNED NOT NULL,
  `rebirthCount` INT UNSIGNED NOT NULL DEFAULT 0,
  `state` VARCHAR(20) NOT NULL DEFAULT 'COMPLETE',
  `resetLevel` INT UNSIGNED NOT NULL DEFAULT 0,
  `resetExp` BIGINT UNSIGNED NOT NULL DEFAULT 0,
  `rewardItemId` INT UNSIGNED NOT NULL DEFAULT 0,
  `rewardItemCount` BIGINT UNSIGNED NOT NULL DEFAULT 0,
  `preservedSkills` VARCHAR(2048) NOT NULL DEFAULT '',
  `skillResetRefunds` VARCHAR(2048) NOT NULL DEFAULT '',
  PRIMARY KEY (`playerId`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_general_ci;

SET @rebirth_sql = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='character_rebirths' AND COLUMN_NAME='state')=0, 'ALTER TABLE `character_rebirths` ADD COLUMN `state` VARCHAR(20) NOT NULL DEFAULT ''COMPLETE''', 'SELECT 1');
PREPARE rebirth_update FROM @rebirth_sql;
EXECUTE rebirth_update;
DEALLOCATE PREPARE rebirth_update;

SET @rebirth_sql = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='character_rebirths' AND COLUMN_NAME='resetLevel')=0, 'ALTER TABLE `character_rebirths` ADD COLUMN `resetLevel` INT UNSIGNED NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE rebirth_update FROM @rebirth_sql;
EXECUTE rebirth_update;
DEALLOCATE PREPARE rebirth_update;

SET @rebirth_sql = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='character_rebirths' AND COLUMN_NAME='resetExp')=0, 'ALTER TABLE `character_rebirths` ADD COLUMN `resetExp` BIGINT UNSIGNED NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE rebirth_update FROM @rebirth_sql;
EXECUTE rebirth_update;
DEALLOCATE PREPARE rebirth_update;

SET @rebirth_sql = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='character_rebirths' AND COLUMN_NAME='rewardItemId')=0, 'ALTER TABLE `character_rebirths` ADD COLUMN `rewardItemId` INT UNSIGNED NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE rebirth_update FROM @rebirth_sql;
EXECUTE rebirth_update;
DEALLOCATE PREPARE rebirth_update;

SET @rebirth_sql = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='character_rebirths' AND COLUMN_NAME='rewardItemCount')=0, 'ALTER TABLE `character_rebirths` ADD COLUMN `rewardItemCount` BIGINT UNSIGNED NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE rebirth_update FROM @rebirth_sql;
EXECUTE rebirth_update;
DEALLOCATE PREPARE rebirth_update;

SET @rebirth_sql = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='character_rebirths' AND COLUMN_NAME='preservedSkills')=0, 'ALTER TABLE `character_rebirths` ADD COLUMN `preservedSkills` VARCHAR(2048) NOT NULL DEFAULT ''''', 'SELECT 1');
PREPARE rebirth_update FROM @rebirth_sql;
EXECUTE rebirth_update;
DEALLOCATE PREPARE rebirth_update;

SET @rebirth_sql = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='character_rebirths' AND COLUMN_NAME='skillResetRefunds')=0, 'ALTER TABLE `character_rebirths` ADD COLUMN `skillResetRefunds` VARCHAR(2048) NOT NULL DEFAULT ''''', 'SELECT 1');
PREPARE rebirth_update FROM @rebirth_sql;
EXECUTE rebirth_update;
DEALLOCATE PREPARE rebirth_update;

-- Only the rebirth journal is converted here. No character/item data is deleted.
ALTER TABLE `character_rebirths` ENGINE=InnoDB;

-- The runtime refuses resets unless these tables also use InnoDB.
-- If a legacy table uses another engine, back it up and migrate it separately.
SELECT TABLE_NAME,ENGINE FROM information_schema.TABLES
WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN
('characters','character_skills','character_shortcuts','character_rebirths');
