--liquibase formatted sql

--changeset Vlad:20251109_006_1812

CREATE INDEX order_items_order_id_idx ON app.order_items(order_id);
CREATE INDEX order_items_item_id_idx ON app.order_items(item_id);