--liquibase formatted sql

--changeset Vlad:20251109_003_1745

CREATE TABLE app.order_items
(
id UUID,
order_id UUID,
item_id BIGINT NOT NULL,
item_name VARCHAR(255) NOT NULL,
item_price NUMERIC(10,2) NOT NULL,
quantity INTEGER NOT NULL,
created_at TIMESTAMP(3) NOT NULL,
updated_at TIMESTAMP(3) NOT NULL,
version BIGINT NOT NULL,
CONSTRAINT order_items_id_pk PRIMARY KEY(id),
CONSTRAINT order_items_order_fk FOREIGN KEY (order_id) REFERENCES app.orders(id)
);
