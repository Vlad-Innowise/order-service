--liquibase formatted sql

--changeset Vlad:20251109_005_1808

CREATE INDEX orders_user_id_idx ON app.orders(user_id);