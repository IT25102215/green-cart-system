INSERT INTO categories (name) SELECT 'Fruits' WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name='Fruits');
INSERT INTO categories (name) SELECT 'Vegetables' WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name='Vegetables');
INSERT INTO categories (name) SELECT 'Dairy' WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name='Dairy');
INSERT INTO categories (name) SELECT 'Bakery' WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name='Bakery');
INSERT INTO categories (name) SELECT 'Meat & Fish' WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name='Meat & Fish');
INSERT INTO categories (name) SELECT 'Grains' WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name='Grains');
INSERT INTO categories (name) SELECT 'Household' WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name='Household');
INSERT INTO categories (name) SELECT 'Snacks' WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name='Snacks');
INSERT INTO categories (name) SELECT 'Beverages' WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name='Beverages');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Apples', 'per 500g', 1000.00, 10, 50, 'https://images.unsplash.com/photo-1568702846914-96b305d2aaeb?w=400',
       (SELECT id FROM categories WHERE name='Fruits')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Apples');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Pineapple', 'per 1.25kg', 500.00, 0, 5, 'https://www.thehopsack.ie/wp-content/uploads/2024/08/pineapple.jpg',
       (SELECT id FROM categories WHERE name='Fruits')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Pineapple');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Banana-Kolikuttu', 'per 500g', 270.00, 0, 10, 'https://lakpura.com/cdn/shop/files/LSZ0074722-01-E.jpg?v=1721144622',
       (SELECT id FROM categories WHERE name='Fruits')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Banana-Kolikuttu');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Guava', 'per 1kg', 420.00, 0, 20, 'https://www.crystalvalleyfoods.com/wp-content/uploads/2025/02/Cut-Thai-Guava-Fruit-scaled-e1740674216536.jpeg',
       (SELECT id FROM categories WHERE name='Fruits')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Guava');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Imported red Grapes', 'per 500g', 1680.00, 15, 10, 'https://objectstorage.ap-mumbai-1.oraclecloud.com/n/softlogicbicloud/b/cdn/o/products/320012--01--1549602291.jpeg',
       (SELECT id FROM categories WHERE name='Fruits')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Imported red Grapes');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Green Apple', 'per 700g', 1340.00, 0, 25, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTima2MWlDbJsd7dns1vR2ehZdIetKWRKR69Q&s',
       (SELECT id FROM categories WHERE name='Fruits')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Green Apple');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Imported Pomegranate', 'per 500g', 2180.00, 0, 3, 'https://objectstorage.ap-mumbai-1.oraclecloud.com/n/softlogicbicloud/b/cdn/o/products/600-600/320028--1--1560487777.jpeg',
       (SELECT id FROM categories WHERE name='Fruits')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Imported Pomegranate');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Veralu', 'per 200g', 170.00, 0, 100, 'https://upload.wikimedia.org/wikipedia/commons/thumb/6/61/CeylonOliveVeralu.jpg/250px-CeylonOliveVeralu.jpg',
       (SELECT id FROM categories WHERE name='Fruits')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Veralu');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Imported Green Grapes', 'per 500g', 1670.00, 0, 45, 'https://ican-mall.oss-ap-southeast-1.aliyuncs.com/2025/09/98eb11df11634bc395298a1e99f6887b.webp',
       (SELECT id FROM categories WHERE name='Fruits')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Imported Green Grapes');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Wood Apple', 'per 1kg', 670.00, 0, 60, 'https://lakpura.com/cdn/shop/files/LSZ0076022-01-E.jpg?v=1719309428&width=1946',
       (SELECT id FROM categories WHERE name='Fruits')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Wood Apple');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Big Onion', 'per 500g', 170.00, 0, 100, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRUVdBJODgbcjIQsT4VsaVDXjUPwc1NOHfhJA&s',
       (SELECT id FROM categories WHERE name='Vegetables')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Big Onion');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Tomato', 'per 500g', 80.00, 0, 50, 'https://www.melissas.com/cdn/shop/files/image-of-organic-cherry-tomatoes-fruit-1125637785_600x600.jpg?v=1738742330',
       (SELECT id FROM categories WHERE name='Vegetables')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Tomato');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Green Chillies', 'per 100g', 30.00, 0, 200, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQEI0CZ-3U4wCcZOzO-gRuSAgFmriylkgBY_A&s',
       (SELECT id FROM categories WHERE name='Vegetables')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Green Chillies');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Potatoes', 'per 500g', 340.00, 0, 150, 'https://cdn.mos.cms.futurecdn.net/iC7HBvohbJqExqvbKcV3pP.jpg',
       (SELECT id FROM categories WHERE name='Vegetables')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Potatoes');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Carrot', 'per 500g', 200.00, 5, 100, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQn8BZZm3cCzgO0AiGVH7wtUPDBNqprzHQA_g&s',
       (SELECT id FROM categories WHERE name='Vegetables')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Carrot');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Pumpkin', 'per 500g', 30.00, 0, 20, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQTNQMEfK0ONn4zMZ3AGVCZqwCoSmtzJeR65w&s',
       (SELECT id FROM categories WHERE name='Vegetables')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Pumpkin');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Leeks', 'per 250g', 80.00, 0, 50, 'https://www.runningtothekitchen.com/wp-content/uploads/2023/05/how-to-cut-leeks-1.jpg',
       (SELECT id FROM categories WHERE name='Vegetables')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Leeks');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Brinjal', 'per 350g', 270.00, 0, 100, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQq99xYnKGpDyUDgbAg4yhaxItY7v176_8ljA&s',
       (SELECT id FROM categories WHERE name='Vegetables')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Brinjal');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Ladies Fingers', 'per 250g', 30.00, 0, 70, 'https://img.drz.lazcdn.com/static/lk/p/4fcd269054439e10f2cb5bb745e5a66a.jpg_720x720q80.jpg',
       (SELECT id FROM categories WHERE name='Vegetables')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Ladies Fingers');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Gotukola', '100g', 130.00, 0, 10, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTDhd0mnJ8ZkRFzMGo1uAHp5GMFTC41rAl5fg&s',
       (SELECT id FROM categories WHERE name='Vegetables')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Gotukola');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Raththi 400g', '400g', 1340.00, 8, 79, 'https://lk-live-21.slatic.net/kf/Sd9325065a2fb4649b2e28376eabcb490o.jpg',
       (SELECT id FROM categories WHERE name='Dairy')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Raththi 400g');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Milo 400g', '400g', 1000.00, 0, 79, 'https://objectstorage.ap-mumbai-1.oraclecloud.com/n/softlogicbicloud/b/cdn/o/products/600-600/115799--1--1597915996.jpeg',
       (SELECT id FROM categories WHERE name='Dairy')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Milo 400g');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Bio Clean Toilet Cleaner', '500ml', 500.00, 0, 96, 'https://static-01.daraz.lk/p/914d3ff400abb8e7f77cb3a5ec19081e.jpg',
       (SELECT id FROM categories WHERE name='Household')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Bio Clean Toilet Cleaner');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Diva Fresh', '1kg', 370.00, 0, 106, 'https://img.drz.lazcdn.com/g/kf/S76f6f951a6e346e58532626d73d31dc7l.jpg_720x720q80.jpg',
       (SELECT id FROM categories WHERE name='Household')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Diva Fresh');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Anchor Mixed Berry Yoghurt', '100g', 230.00, 0, 153, 'https://essstr.blob.core.windows.net/essimg/ItemAsset/Pic93287.jpg',
       (SELECT id FROM categories WHERE name='Dairy')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Anchor Mixed Berry Yoghurt');


INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Tiara Chocolate Cake', '310g', 950.00, 10, 82, 'https://objectstorage.ap-mumbai-1.oraclecloud.com/n/softlogicbicloud/b/cdn/o/products/600-600/114241--1--1638985016.jpeg',
       (SELECT id FROM categories WHERE name='Snacks')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Tiara Chocolate Cake');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Sandwich Bread', '500g', 550.00, 0, 21, 'https://essstr.blob.core.windows.net/essimg/ItemAsset/Pic10743.jpg',
       (SELECT id FROM categories WHERE name='Bakery')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Sandwich Bread');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Crescent Mini Kieves', '240g', 820.00, 0, 42, 'https://essstr.blob.core.windows.net/essimg/ItemAsset/Pic6732.jpg',
       (SELECT id FROM categories WHERE name='Meat & Fish')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Crescent Mini Kieves');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Munchee Super Cream Cracker', '490g', 540.00, 0, 63, 'https://objectstorage.ap-mumbai-1.oraclecloud.com/n/softlogicbicloud/b/cdn/o/products/114839--01--1623926509.jpeg',
       (SELECT id FROM categories WHERE name='Snacks')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Munchee Super Cream Cracker');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Chicken Sausages', '500g', 1360.00, 12, 34, 'https://objectstorage.ap-mumbai-1.oraclecloud.com/n/softlogicbicloud/b/cdn/o/products/600-600/123580--01--1702306073.jpeg',
       (SELECT id FROM categories WHERE name='Meat & Fish')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Chicken Sausages');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Rani Sandalwood Soap', '90g', 230.00, 0, 78, 'https://www.ceylonsupermart.com/cdn/shop/products/Rani-Sandalwood-Soap-90g-1-Bar-Soap-Ceylon-Supermart.jpg?v=1641249057',
       (SELECT id FROM categories WHERE name='Household')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Rani Sandalwood Soap');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Sunlight Soap', '110g', 170.00, 0, 62, 'https://essstr.blob.core.windows.net/essimg/ItemAsset/Pic118809.jpg',
       (SELECT id FROM categories WHERE name='Household')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Sunlight Soap');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Astra Fat Spread', '500g', 1220.00, 0, 132, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQOL-rR9ikniuWEodPQZbDqY4bYoK2RpTafaA&s',
       (SELECT id FROM categories WHERE name='Dairy')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Astra Fat Spread');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Kotmale Mozzarella Cheese', '500g', 2010.00, 15, 15,
       'https://objectstorage.ap-mumbai-1.oraclecloud.com/n/softlogicbicloud/b/cdn/o/products/600-600/117615--01--1615287640.jpeg',
       (SELECT id FROM categories WHERE name='Dairy')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Kotmale Mozzarella Cheese');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Kotmale Processed Cheese Wedges', '120g', 670.00, 0, 10,
       'https://objectstorage.ap-mumbai-1.oraclecloud.com/n/softlogicbicloud/b/cdn/o/products/600-600/121511--01--1646230966.jpeg',
       (SELECT id FROM categories WHERE name='Dairy')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Kotmale Processed Cheese Wedges');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Kotmale Curd', '900g', 600.00, 0, 8,
       'https://objectstorage.ap-mumbai-1.oraclecloud.com/n/softlogicbicloud/b/cdn/o/products/600-600/123247--01--1683902920.jpeg',
       (SELECT id FROM categories WHERE name='Dairy')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Kotmale Curd');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Richlife Butter Salted', '200g', 1000.00, 0, 25,
       'https://i0.wp.com/onlinekade.lk/wp-content/uploads/2022/06/4797777007191-1.jpg',
       (SELECT id FROM categories WHERE name='Dairy')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Richlife Butter Salted');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Maliban Full Cream Milk Powder', '400g', 1340.00, 0, 20,
       'https://essstr.blob.core.windows.net/essimg/ItemAsset/Pic25417_20250704094357.jpg',
       (SELECT id FROM categories WHERE name='Dairy')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Maliban Full Cream Milk Powder');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Anchor Non-Fat Milk Powder', '400g', 1340.00, 0, 15,
       'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQKwRSbZd_yq1hCKWLwo5MhyrAPhUgQjklz7Q&s',
       (SELECT id FROM categories WHERE name='Dairy')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Anchor Non-Fat Milk Powder');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Magic Vanila & Chocolate Ice Cream', '1 Liters', 1340.00, 0, 50,
       'https://i0.wp.com/onlinekade.lk/wp-content/uploads/2023/09/4792085000292.jpg',
       (SELECT id FROM categories WHERE name='Dairy')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Magic Vanila & Chocolate Ice Cream');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Kotmale Drinking Yoghut Vanila', '180ml', 150.00, 0, 30,
       'https://objectstorage.ap-mumbai-1.oraclecloud.com/n/softlogicbicloud/b/cdn/o/products/600-600/123244--01--1683902919.jpeg',
       (SELECT id FROM categories WHERE name='Dairy')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Kotmale Drinking Yoghut Vanila');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Kotmale Drinking Yoghut Strawberry', '180ml', 150.00, 0, 20,
       'https://objectstorage.ap-mumbai-1.oraclecloud.com/n/softlogicbicloud/b/cdn/o/products/600-600/123245--01--1683902919.jpeg',
       (SELECT id FROM categories WHERE name='Dairy')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Kotmale Drinking Yoghut Strawberry');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Magic Cone Chocolate', '120ml', 150.00, 0, 50,
       'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTOyR7hCALORqjQBH8QsfaVvwNWBUIcPnHFnQ&s',
       (SELECT id FROM categories WHERE name='Dairy')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Magic Cone Chocolate');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Pepsi', '2L', 830.00, 5, 10,
       'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSJq1eFYcuTWax144MI9McRDUKSuY4Tb4nr5A&s',
       (SELECT id FROM categories WHERE name='Beverages')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Pepsi');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Ride Glow Apple Energy Drink', '250ml', 400.00, 0, 20,
       'https://objectstorage.ap-mumbai-1.oraclecloud.com/n/softlogicbicloud/b/cdn/o/products/600-600/125619--01--1763103146.jpeg',
       (SELECT id FROM categories WHERE name='Beverages')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Ride Glow Apple Energy Drink');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Kist Mango Nectar', '1L', 840.00, 0, 30,
       'https://spar2u.lk/cdn/shop/files/3000233-01.jpg?v=1748382624',
       (SELECT id FROM categories WHERE name='Beverages')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Kist Mango Nectar');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'MD Orange Cordial', '400ml', 1170.00, 0, 45,
       'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQdSVV6lTqCZglFdGe-UyF0IZ-UQ06MA7bTIw&s',
       (SELECT id FROM categories WHERE name='Beverages')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='MD Orange Cordial');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Kist Woodapple Drink', '500ml', 340.00, 0, 45,
       'https://onlinekade.lk/wp-content/uploads/2021/11/4792143134310-1.jpg',
       (SELECT id FROM categories WHERE name='Beverages')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Kist Woodapple Drink');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Maliban Lemon Puff', '200g', 330.00, 10, 70,
       'https://objectstorage.ap-mumbai-1.oraclecloud.com/n/softlogicbicloud/b/cdn/o/products/600-600/116165--01--1555692002.jpeg',
       (SELECT id FROM categories WHERE name='Snacks')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Maliban Lemon Puff');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Maliban Chocolate Cream Biscuit', '400g', 670.00, 0, 60,
       'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQGGs7ejMRUkpEYKgmbmZKmmyH-beJJjuo8Bg&s',
       (SELECT id FROM categories WHERE name='Snacks')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Maliban Chocolate Cream Biscuit');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Maliban Gold Marie', '350g', 330.00, 0, 70,
       'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTgzgfPJz2EErsJv-nRc6Ke5UCEhFU0Pb3W4A&s',
       (SELECT id FROM categories WHERE name='Snacks')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Maliban Gold Marie');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Scan Jumbo Peanuts', '150g', 1000.00, 0, 40,
       'https://lakpura.com/cdn/shop/files/LSZ0075512-01-E.jpg?v=1706602453&width=1946',
       (SELECT id FROM categories WHERE name='Snacks')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Scan Jumbo Peanuts');

INSERT INTO products (name, description, price, discount, stock, image, category_id)
SELECT 'Magic Choko', '160g', 1000.00, 20, 50,
       'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTg-MM3zXVeS2bQeaVgV785wjmt3VV9Ab9GJg&s',
       (SELECT id FROM categories WHERE name='Snacks')
    WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Magic Choko');

UPDATE orders SET shipping_fee = 0 WHERE shipping_fee IS NULL;


-- GREEN CART SELECTED PRODUCT DISCOUNTS - ONE-TIME MIGRATION
-- Fixes databases that previously received category-wide discounts.
-- This executes only once, so discounts later edited by an administrator persist across restarts.
CREATE TABLE IF NOT EXISTS app_migrations (
    migration_key VARCHAR(100) PRIMARY KEY,
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

UPDATE products
SET discount = 0
WHERE NOT EXISTS (
    SELECT 1 FROM app_migrations WHERE migration_key = 'selected_discounts_v2'
);

UPDATE products SET discount = 10 WHERE name = 'Apples'
  AND NOT EXISTS (SELECT 1 FROM app_migrations WHERE migration_key = 'selected_discounts_v2');
UPDATE products SET discount = 15 WHERE name = 'Imported red Grapes'
  AND NOT EXISTS (SELECT 1 FROM app_migrations WHERE migration_key = 'selected_discounts_v2');
UPDATE products SET discount = 5 WHERE name = 'Carrot'
  AND NOT EXISTS (SELECT 1 FROM app_migrations WHERE migration_key = 'selected_discounts_v2');
UPDATE products SET discount = 8 WHERE name = 'Raththi 400g'
  AND NOT EXISTS (SELECT 1 FROM app_migrations WHERE migration_key = 'selected_discounts_v2');
UPDATE products SET discount = 10 WHERE name = 'Tiara Chocolate Cake'
  AND NOT EXISTS (SELECT 1 FROM app_migrations WHERE migration_key = 'selected_discounts_v2');
UPDATE products SET discount = 12 WHERE name = 'Chicken Sausages'
  AND NOT EXISTS (SELECT 1 FROM app_migrations WHERE migration_key = 'selected_discounts_v2');
UPDATE products SET discount = 15 WHERE name = 'Kotmale Mozzarella Cheese'
  AND NOT EXISTS (SELECT 1 FROM app_migrations WHERE migration_key = 'selected_discounts_v2');
UPDATE products SET discount = 5 WHERE name = 'Pepsi'
  AND NOT EXISTS (SELECT 1 FROM app_migrations WHERE migration_key = 'selected_discounts_v2');
UPDATE products SET discount = 10 WHERE name = 'Maliban Lemon Puff'
  AND NOT EXISTS (SELECT 1 FROM app_migrations WHERE migration_key = 'selected_discounts_v2');
UPDATE products SET discount = 20 WHERE name = 'Magic Choko'
  AND NOT EXISTS (SELECT 1 FROM app_migrations WHERE migration_key = 'selected_discounts_v2');

INSERT IGNORE INTO app_migrations (migration_key)
VALUES ('selected_discounts_v2');

-- ============================================================
-- ORDER AUDIT TRIGGERS (IT2140 Part F / Order Management viva)
-- MySQL stores audit messages instead of using SQL Server PRINT.
-- ============================================================
DROP TRIGGER IF EXISTS trg_order_insert;
CREATE TRIGGER trg_order_insert
AFTER INSERT ON orders
FOR EACH ROW
INSERT INTO order_audit_log(order_id, action, message)
VALUES (NEW.id, 'INSERT', CONCAT('New order #', NEW.id, ' created with status ', NEW.status));

DROP TRIGGER IF EXISTS trg_order_status_update;
CREATE TRIGGER trg_order_status_update
AFTER UPDATE ON orders
FOR EACH ROW
INSERT INTO order_audit_log(order_id, action, message)
SELECT NEW.id,
       'STATUS_CHANGE',
       CONCAT('Order #', NEW.id, ' status changed from ', OLD.status, ' to ', NEW.status)
WHERE NOT (OLD.status <=> NEW.status);

DROP TRIGGER IF EXISTS trg_order_delete;
CREATE TRIGGER trg_order_delete
BEFORE DELETE ON orders
FOR EACH ROW
INSERT INTO order_audit_log(order_id, action, message)
VALUES (OLD.id, 'DELETE', CONCAT('Order #', OLD.id, ' deleted'));
