--liquibase formatted sql

--changeset Vlad:20251109_001_1714

CREATE TABLE app.items
(
id BIGSERIAL,
name VARCHAR(255) NOT NULL,
price NUMERIC(10,2) NOT NULL,
created_at TIMESTAMP(3) NOT NULL,
updated_at TIMESTAMP(3) NOT NULL,
version BIGINT NOT NULL,
CONSTRAINT items_id_pk PRIMARY KEY(id),
CONSTRAINT items_name_unq UNIQUE(name)
);
