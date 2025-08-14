--V005__Create_Products_Table

CREATE TABLE products
(
    product_id  SERIAL PRIMARY KEY,
    sku         VARCHAR(50)        NOT NULL,
    name        VARCHAR(100)       NOT NULL,
    description TEXT,
    category VARCHAR(30) NOT NULL CHECK (category IN (
                                                      'LAPTOPS',
                                                      'SMARTPHONES',
                                                      'GAMING_CONSOLES',
                                                      'ACCESSORIES',
                                                      'TABLETS',
                                                      'OTHERS'
    )),
    brand VARCHAR(50) NOT NULL ,
    price       NUMERIC(10, 2)     NOT NULL CHECK ( price > 0 ),
    stock_quantity INTEGER            NOT NULL DEFAULT 0 CHECK ( stock_quantity  >= 0 ),
    created_at  TIMESTAMP          NOT NULL DEFAULT NOW()
);