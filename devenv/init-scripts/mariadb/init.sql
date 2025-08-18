-- MariaDB Test Database Initialization Script
-- Copyright 2025 agwlvssainokuni

-- Create additional test user
CREATE USER IF NOT EXISTS 'testuser2'@'%' IDENTIFIED BY 'testpass2';
GRANT ALL PRIVILEGES ON testdb.* TO 'testuser2'@'%';

-- Create sample tables for testing
USE testdb;

-- Sample users table
CREATE TABLE IF NOT EXISTS sample_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

-- Sample products table
CREATE TABLE IF NOT EXISTS sample_products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_code VARCHAR(20) NOT NULL UNIQUE,
    product_name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    category VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Sample orders table
CREATE TABLE IF NOT EXISTS sample_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    FOREIGN KEY (user_id) REFERENCES sample_users(id)
);

-- Insert sample data
INSERT IGNORE INTO sample_users (username, email, full_name) VALUES
('john_doe', 'john@example.com', 'John Doe'),
('jane_smith', 'jane@example.com', 'Jane Smith'),
('bob_wilson', 'bob@example.com', 'Bob Wilson');

INSERT IGNORE INTO sample_products (product_code, product_name, description, price, category) VALUES
('PRD001', 'Laptop Computer', 'High-performance laptop for business use', 1299.99, 'Electronics'),
('PRD002', 'Office Chair', 'Ergonomic office chair with lumbar support', 299.99, 'Furniture'),
('PRD003', 'Wireless Mouse', 'Bluetooth wireless mouse', 49.99, 'Electronics');

INSERT IGNORE INTO sample_orders (user_id, total_amount, status) VALUES
(1, 1299.99, 'COMPLETED'),
(2, 349.98, 'PENDING'),
(1, 49.99, 'SHIPPED');