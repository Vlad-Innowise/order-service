--liquibase formatted sql

--changeset Vlad:20251109_003_1729

CREATE TABLE app.orders
(
id UUID,
user_id BIGINT NOT NULL,
status VARCHAR(32) NOT NULL,
creation_date TIMESTAMP(3) NOT NULL,
created_at TIMESTAMP(3) NOT NULL,
updated_at TIMESTAMP(3) NOT NULL,
version BIGINT NOT NULL,
CONSTRAINT orders_id_pk PRIMARY KEY(id)
);
