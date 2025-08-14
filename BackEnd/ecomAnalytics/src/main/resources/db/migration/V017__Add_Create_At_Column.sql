--V017__Add_Create_At_Column

ALTER TABLE customers
    ADD COLUMN updated_at TIMESTAMP NULL;

ALTER TABLE products
    ADD COLUMN updated_at TIMESTAMP NULL;

ALTER TABLE product_recommendations
    ADD COLUMN updated_at TIMESTAMP NULL;

ALTER TABLE customer_segments
    ADD COLUMN updated_at TIMESTAMP NULL;

ALTER TABLE order_items
    ADD COLUMN updated_at TIMESTAMP NULL;