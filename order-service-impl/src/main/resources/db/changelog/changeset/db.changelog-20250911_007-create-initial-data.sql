--liquibase formatted sql

--changeset Vlad:20251109_007_2012

INSERT INTO app.items (id, name, price, created_at, updated_at, version)
VALUES
  (1, 'Macbook', 2100.00, NOW(), NOW(), 0),
  (2, 'Iphone', 1100.00, NOW(), NOW(), 0),
  (3, 'Ipad', 700.00, NOW(), NOW(), 0),
  (4, 'Airpods', 250.00, NOW(), NOW(), 0),
  (5, 'Apple charging cable', 20.00, NOW(), NOW(), 0);

SELECT SETVAL('items_id_seq', (SELECT MAX(id) FROM app.items));

-- user_id = 1
INSERT INTO app.orders (id, user_id, status, creation_date, created_at, updated_at, version)
VALUES
  ('00000000-0000-0000-0000-000000000001', 1, 'FINISHED', NOW(), NOW(), NOW(), 0),
  ('00000000-0000-0000-0000-000000000002', 1, 'FINISHED', NOW(), NOW(), NOW(), 0),
  ('00000000-0000-0000-0000-000000000003', 1, 'PENDING', NOW(), NOW(), NOW(), 0);

-- user_id = 2
INSERT INTO app.orders (id, user_id, status, creation_date, created_at, updated_at, version)
VALUES
  ('00000000-0000-0000-0000-000000000004', 2, 'FINISHED', NOW(), NOW(), NOW(), 0),
  ('00000000-0000-0000-0000-000000000005', 2, 'FINISHED', NOW(), NOW(), NOW(), 0);

-- Order 1 (user 1, FINISHED) — Macbook + Iphone
INSERT INTO app.order_items (id, order_id, item_id, item_name, item_price, quantity, created_at, updated_at, version)
VALUES
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 1, 'Macbook', 2100.00, 1, NOW(), NOW(), 0),
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 2, 'Iphone', 1100.00, 2, NOW(), NOW(), 0);

-- Order 2 (user 1, FINISHED) — Ipad + Airpods
INSERT INTO app.order_items (id, order_id, item_id, item_name, item_price, quantity, created_at, updated_at, version)
VALUES
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000002', 3, 'Ipad', 700.00, 1, NOW(), NOW(), 0),
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000002', 4, 'Airpods', 250.00, 2, NOW(), NOW(), 0);

-- Order 3 (user 1, PENDING) — Charging Cable + Iphone + Ipad
INSERT INTO app.order_items (id, order_id, item_id, item_name, item_price, quantity, created_at, updated_at, version)
VALUES
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000003', 5, 'Apple charging cable', 20.00, 2, NOW(), NOW(), 0),
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000003', 2, 'Iphone', 1100.00, 1, NOW(), NOW(), 0),
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000003', 3, 'Ipad', 700.00, 1, NOW(), NOW(), 0);

-- Order 4 (user 2, FINISHED) — Ноутбук + Airpods
INSERT INTO app.order_items (id, order_id, item_id, item_name, item_price, quantity, created_at, updated_at, version)
VALUES
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000004', 1, 'Ноутбук', 2100.00, 1, NOW(), NOW(), 0),
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000004', 4, 'Airpods', 250.00, 1, NOW(), NOW(), 0);

-- Order 5 (user 2, FINISHED) — Ipad + Apple charging cable
INSERT INTO app.order_items (id, order_id, item_id, item_name, item_price, quantity, created_at, updated_at, version)
VALUES
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000005', 3, 'Ipad', 700.00, 1, NOW(), NOW(), 0),
  (gen_random_uuid(), '00000000-0000-0000-0000-000000000005', 5, 'Apple charging cable', 20.00, 1, NOW(), NOW(), 0);