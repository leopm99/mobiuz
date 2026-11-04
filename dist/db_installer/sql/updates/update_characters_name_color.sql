-- Run against the game server database.
-- Preserve existing values if the column has already been added.
SET @name_color_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'characters'
    AND COLUMN_NAME = 'name_color'
);

SET @name_color_sql = IF(
  @name_color_exists = 0,
  'ALTER TABLE `characters` ADD COLUMN `name_color` MEDIUMINT UNSIGNED NOT NULL DEFAULT 16777215',
  'SELECT ''Column characters.name_color already exists'' AS message'
);

PREPARE name_color_statement FROM @name_color_sql;
EXECUTE name_color_statement;
DEALLOCATE PREPARE name_color_statement;
