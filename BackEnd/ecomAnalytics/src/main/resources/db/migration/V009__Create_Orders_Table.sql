--V009__Create_Orders_Table

CREATE TABLE orders(
    order_id SERIAL PRIMARY KEY ,
    customer_id INT REFERENCES customers(customer_id) ON DELETE CASCADE NOT NULL ,
    status VARCHAR(30) NOT NULL CHECK ( status IN ('PENDING','DELIVERED','CANCELLED')),
    order_date TIMESTAMP DEFAULT NOW(),
    total_amount DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);