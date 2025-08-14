--V013__Create_Product_Recommendations_Table

CREATE TABLE product_recommendations(
    customer_id INT REFERENCES customers(customer_id) NOT NULL ,
    product_id INT REFERENCES products(product_id) NOT NULL ,
    PRIMARY KEY (customer_id, product_id),
    score DECIMAL(10,2) NOT NULL CHECK ( score >= 0 AND score <= 1 ) ,
    created_at TIMESTAMP DEFAULT NOW()
);