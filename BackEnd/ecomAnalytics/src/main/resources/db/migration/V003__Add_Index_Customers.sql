--V003__Add_Index_Customers

CREATE INDEX idx_customers_email
    ON customers(email);

CREATE INDEX idx_customers_registration_date
    ON customers(registration_date);
