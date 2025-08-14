--V008__Create_Order_Status_Enum

CREATE TYPE order_status_enum AS ENUM (
    'pending',
    'delivered',
    'cancelled'
);