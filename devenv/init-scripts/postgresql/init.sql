-- PostgreSQL Test Database Initialization Script
-- Copyright 2025 agwlvssainokuni

-- Create sample tables for testing
-- Sample users table
CREATE TABLE IF NOT EXISTS sample_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

-- Sample products table
CREATE TABLE IF NOT EXISTS sample_products (
    id BIGSERIAL PRIMARY KEY,
    product_code VARCHAR(20) NOT NULL UNIQUE,
    product_name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    category VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Sample orders table
CREATE TABLE IF NOT EXISTS sample_orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    FOREIGN KEY (user_id) REFERENCES sample_users(id)
);

-- Create function for updating updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for updated_at
DROP TRIGGER IF EXISTS update_sample_users_updated_at ON sample_users;
CREATE TRIGGER update_sample_users_updated_at
    BEFORE UPDATE ON sample_users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_sample_products_updated_at ON sample_products;
CREATE TRIGGER update_sample_products_updated_at
    BEFORE UPDATE ON sample_products
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Insert sample data
INSERT INTO sample_users (username, email, full_name) VALUES
('john_doe', 'john@example.com', 'John Doe'),
('jane_smith', 'jane@example.com', 'Jane Smith'),
('bob_wilson', 'bob@example.com', 'Bob Wilson')
ON CONFLICT (username) DO NOTHING;

INSERT INTO sample_products (product_code, product_name, description, price, category) VALUES
('PRD001', 'Laptop Computer', 'High-performance laptop for business use', 1299.99, 'Electronics'),
('PRD002', 'Office Chair', 'Ergonomic office chair with lumbar support', 299.99, 'Furniture'),
('PRD003', 'Wireless Mouse', 'Bluetooth wireless mouse', 49.99, 'Electronics')
ON CONFLICT (product_code) DO NOTHING;

INSERT INTO sample_orders (user_id, total_amount, status) VALUES
(1, 1299.99, 'COMPLETED'),
(2, 349.98, 'PENDING'),
(1, 49.99, 'SHIPPED');