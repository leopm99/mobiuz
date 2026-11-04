CREATE TABLE IF NOT EXISTS `individualvotes` (
  `voterIp` VARCHAR(45) NOT NULL,
  `voteSite` TINYINT UNSIGNED NOT NULL,
  `diffTime` BIGINT NOT NULL DEFAULT '0',
  `votingTimeSite` BIGINT NOT NULL DEFAULT '0',
  `alreadyRewarded` TINYINT UNSIGNED NOT NULL DEFAULT '0',
  PRIMARY KEY (`voterIp`, `voteSite`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_general_ci;
