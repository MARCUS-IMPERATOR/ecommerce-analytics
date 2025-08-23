--V012__Create_Customer_Segments_Tables

CREATE TABLE customer_segments(
    customer_id INT REFERENCES customers(customer_id) PRIMARY KEY ,
    segment_label VARCHAR(30) CHECK ( segment_label IN ('CHAMPION','LOYAL''AT_RISK','NEW')),
    version INT DEFAULT 0 NOT NULL,
    recency INT NOT NULL ,
    frequency DECIMAL(10,2) NOT NULL ,
    monetary DECIMAL(10,2) NOT NULL ,
    segment_score INT,
    last_calculated TIMESTAMP DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);