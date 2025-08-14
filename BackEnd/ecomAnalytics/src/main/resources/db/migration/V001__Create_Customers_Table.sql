--V001__Create_Customers_Table

CREATE EXTENSION IF NOT EXISTS citext;

CREATE TABLE customers(
    customer_id SERIAL PRIMARY KEY ,
    customer_code VARCHAR(30) NOT NULL ,
    first_name VARCHAR(50) NOT NULL ,
    last_name VARCHAR(50) NOT NULL ,
    age int NOT NULL ,
    country VARCHAR(100) NOT NULL ,
    email CITEXT NOT NULL ,
    phone VARCHAR(30),
    registration_date TIMESTAMP NOT NULL DEFAULT NOW(),
    total_spent DECIMAL(10,2) DEFAULT 0,
    order_count INT DEFAULT 0,
    last_order_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
