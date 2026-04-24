CREATE TABLE users
(
    username        VARCHAR(255) PRIMARY KEY,
    display_name    VARCHAR(255) NOT NULL,
    hashed_password VARCHAR(255) NOT NULL,
    balance         DOUBLE PRECISION
);
