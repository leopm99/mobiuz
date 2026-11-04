-- Journey Community Auction House. New tables; does not import legacy auction_house rows.
-- Run against the GAME database before enabling CommunityAuctionEnabled.
CREATE TABLE IF NOT EXISTS community_auction_listings (
  id INT NOT NULL AUTO_INCREMENT,
  owner_id INT NOT NULL,
  object_id INT NOT NULL,
  item_id INT NOT NULL,
  quantity BIGINT NOT NULL,
  unit_price BIGINT NOT NULL,
  expires_at BIGINT NOT NULL,
  state VARCHAR(16) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY owner_state (owner_id,state),
  KEY state_expiration (state,expires_at),
  CHECK (quantity >= 0),
  CHECK (unit_price > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS community_auction_operations (
  id CHAR(36) NOT NULL,
  listing_id INT NOT NULL,
  kind VARCHAR(16) NOT NULL,
  actor_id INT NOT NULL,
  source_object_id INT NOT NULL,
  source_count BIGINT NOT NULL,
  quantity BIGINT NOT NULL,
  amount BIGINT NOT NULL,
  adena_before BIGINT NOT NULL,
  target_object_id INT NOT NULL DEFAULT 0,
  message_id INT NOT NULL DEFAULT 0,
  stage VARCHAR(24) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY listing_stage (listing_id,stage),
  CONSTRAINT community_auction_operation_listing FOREIGN KEY (listing_id)
    REFERENCES community_auction_listings(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
