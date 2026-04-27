CREATE TABLE users
(
    username        VARCHAR(255) PRIMARY KEY,
    display_name    VARCHAR(255) NOT NULL,
    hashed_password VARCHAR(255) NOT NULL,
    balance         DOUBLE PRECISION
);

CREATE TABLE items
(
    item_id         INTEGER PRIMARY KEY AUTOINCREMENT,
    seller_username VARCHAR(255) NOT NULL,
    title           VARCHAR(255) NOT NULL,
    FOREIGN KEY (seller_username) REFERENCES users(username)
);

CREATE TABLE bids
(
    bid_id          INTEGER PRIMARY KEY AUTOINCREMENT,
    item_id         INTEGER NOT NULL,
    bidder_username VARCHAR(255) NOT NULL,
    bid_amount      DOUBLE PRECISION,
    bid_time        TEXT,
    FOREIGN KEY (item_id) REFERENCES items(item_id),
    FOREIGN KEY (bidder_username) REFERENCES users(username)
);

CREATE TABLE item_status
(
    item_id         INTEGER PRIMARY KEY,
    current_price   DOUBLE PRECISION,
    username        VARCHAR(255),
    start_time      TEXT,
    end_time        TEXT,
    item_status     INTEGER DEFAULT 0,
    starting_price  DOUBLE PRECISION,
    buy_it_now_price DOUBLE PRECISION,
    bid_increment   DOUBLE PRECISION,
    FOREIGN KEY (item_id) REFERENCES items(item_id)
);
