--liquibase formatted sql

--changeset Vlad:20251109_004_1804

CREATE INDEX items_name_idx ON app.items(name);