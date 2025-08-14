--V018__Alter_Email_Constraints

ALTER TABLE customers
    DROP CONSTRAINT IF EXISTS ck_email_format,
    ADD CONSTRAINT ck_email_format CHECK (email ~* '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$'
    );