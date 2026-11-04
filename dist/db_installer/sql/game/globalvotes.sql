CREATE TABLE IF NOT EXISTS `globalvotes` (
  `voteSite` TINYINT UNSIGNED NOT NULL,
  `lastRewardVotes` INT NOT NULL DEFAULT '0',
  PRIMARY KEY (`voteSite`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_general_ci;
