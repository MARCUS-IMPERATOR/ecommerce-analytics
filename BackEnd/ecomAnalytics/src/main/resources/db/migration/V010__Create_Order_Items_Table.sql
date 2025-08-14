--V010__Create_Order_Items_Table

CREATE TABLE order_items(
    order_id INT NOT NULL ,
    product_id INT NOT NULL ,
    quantity INT NOT NULL CHECK ( quantity > 0 ) ,
    unit_price DECIMAL(10,2) NOT NULL CHECK ( unit_price >= 0 ),
    PRIMARY KEY (order_id, product_id) ,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE ,
    FOREIGN KEY (product_id) REFERENCES products(product_id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);