--V007__Add_Index_products

CREATE INDEX idx_products_categ
    ON products(category);

CREATE INDEX idx_products_name_desc
    ON products
    USING GIN (to_tsvector('english',name || ' '|| description));