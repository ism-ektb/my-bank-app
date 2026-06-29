CREATE SCHEMA IF NOT EXISTS account_service;

CREATE TABLE IF NOT EXISTS account_service.accounts
(
    id          BIGSERIAL PRIMARY KEY,
    login       VARCHAR NOT NULL UNIQUE,
    username    VARCHAR,
    birthdate   DATE,
    balance     BIGINT,
    CONSTRAINT not_negative_value CHECK ( balance >= 0 )
)