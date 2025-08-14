--V002__Add_Constraints_Customers

ALTER TABLE customers
    ADD CONSTRAINT uq_customers_email UNIQUE (email),
    ADD CONSTRAINT uq_customers_code UNIQUE (customer_code),
    ADD CONSTRAINT ck_email_format CHECK ( email ~* '^[a-zA-Z0-9._%+-]+@[a-zA-Z]+\.+[a-zA-Z]$' );