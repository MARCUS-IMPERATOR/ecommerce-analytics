--V006__Add_Constraints_products

ALTER TABLE products
    ADD CONSTRAINT uq_product_sku UNIQUE (sku);