CREATE TABLE customers
(
    id          VARCHAR(36) PRIMARY KEY REFERENCES users (user_id),
    joined_at   TIMESTAMP(6),
    total_spent NUMERIC(38, 2) DEFAULT 0,
    points      INTEGER        DEFAULT 0
);

CREATE TABLE employees
(
    id              VARCHAR(36) PRIMARY KEY REFERENCES users (user_id),
    hire_date       DATE,
    employee_status VARCHAR(20),
    CONSTRAINT ck_employees_status
        CHECK (employee_status IN ('ACTIVE', 'RESIGNED', 'ON_LEAVE', 'SUSPENDED'))
);