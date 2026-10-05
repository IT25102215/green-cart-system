-- ============================================================
-- GREEN CART - MYSQL WORKBENCH DATABASE SETUP
-- Runtime schema used by the Spring Boot application.
-- Database: grocery_ordering
-- ============================================================

CREATE DATABASE IF NOT EXISTS grocery_ordering
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE grocery_ordering;

CREATE TABLE IF NOT EXISTS categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    image VARCHAR(255) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_categories_name (name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(10) NULL,
    address VARCHAR(255) NULL,
    profile_image VARCHAR(255) NULL,
    role VARCHAR(30) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    reset_token VARCHAR(255) NULL,
    reset_token_expiry DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    KEY idx_users_role (role)
 ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS delivery_staff (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    vehicle_number VARCHAR(30) NOT NULL,
    availability_status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    max_active_orders INT NOT NULL DEFAULT 5,
    PRIMARY KEY (id),
    UNIQUE KEY uk_delivery_staff_user (user_id),
    UNIQUE KEY uk_delivery_staff_vehicle (vehicle_number),
    KEY idx_delivery_staff_availability (availability_status),
    CONSTRAINT fk_delivery_staff_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT chk_delivery_staff_capacity CHECK (max_active_orders BETWEEN 1 AND 20)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS suppliers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NULL,
    phone VARCHAR(10) NULL,
    address VARCHAR(255) NULL,
    company VARCHAR(255) NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    KEY idx_suppliers_active (active),
    KEY idx_suppliers_name (name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS products (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(2000) NULL,
    category_id BIGINT NULL,
    price DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    discount DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    stock INT NOT NULL DEFAULT 0,
    image VARCHAR(255) NULL,
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    KEY idx_products_category (category_id),
    KEY idx_products_name (name),
    KEY idx_products_deleted_at (deleted_at),
    CONSTRAINT fk_products_category
        FOREIGN KEY (category_id) REFERENCES categories(id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT chk_products_price CHECK (price >= 0),
    CONSTRAINT chk_products_discount CHECK (discount >= 0 AND discount <= 100),
    CONSTRAINT chk_products_stock CHECK (stock >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS cart_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cart_user_product (user_id, product_id),
    KEY idx_cart_product (product_id),
    CONSTRAINT fk_cart_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_cart_product
        FOREIGN KEY (product_id) REFERENCES products(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT chk_cart_quantity CHECK (quantity > 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NULL,
    total DECIMAL(12,2) NOT NULL,
    shipping_fee DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(30) NOT NULL,
    address VARCHAR(255) NULL,
    phone VARCHAR(10) NULL,
    delivery_notes VARCHAR(500) NULL,
    cancellation_reason VARCHAR(500) NULL,
    cancelled_at DATETIME(6) NULL,
    payment_method VARCHAR(50) NULL,
    payment_status VARCHAR(50) NULL,
    payment_id VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_orders_user (user_id),
    KEY idx_orders_status (status),
    KEY idx_orders_created_at (created_at),
    KEY idx_orders_status_created (status, created_at),
    CONSTRAINT fk_orders_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT chk_orders_total CHECK (total >= 0),
    CONSTRAINT chk_orders_shipping CHECK (shipping_fee >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NULL,
    quantity INT NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    original_price DECIMAL(12,2) NULL,
    discount_percent DECIMAL(5,2) NULL,
    PRIMARY KEY (id),
    KEY idx_order_items_order (order_id),
    KEY idx_order_items_product (product_id),
    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product
        FOREIGN KEY (product_id) REFERENCES products(id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT chk_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT chk_order_items_price CHECK (price >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS payments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    method VARCHAR(50) NULL,
    status VARCHAR(50) NULL,
    amount DECIMAL(12,2) NULL,
    transaction_id VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_payments_order (order_id),
    UNIQUE KEY uk_payments_transaction (transaction_id),
    CONSTRAINT fk_payments_order
        FOREIGN KEY (order_id) REFERENCES orders(id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS deliveries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    delivery_staff_id BIGINT NULL,
    status VARCHAR(30) NOT NULL,
    problem VARCHAR(500) NULL,
    rescheduled_for DATETIME(6) NULL,
    assigned_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_deliveries_order (order_id),
    KEY idx_deliveries_staff (delivery_staff_id),
    KEY idx_deliveries_status (status),
    KEY idx_deliveries_staff_status (delivery_staff_id, status),
    CONSTRAINT fk_deliveries_order
        FOREIGN KEY (order_id) REFERENCES orders(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_deliveries_staff
        FOREIGN KEY (delivery_staff_id) REFERENCES delivery_staff(id)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS feedback (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    order_id BIGINT NULL,
    subject VARCHAR(120) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    response VARCHAR(2000) NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_feedback_order (order_id),
    KEY idx_feedback_user (user_id),
    KEY idx_feedback_status (status),
    CONSTRAINT fk_feedback_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_feedback_order
        FOREIGN KEY (order_id) REFERENCES orders(id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS complaints (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    order_id BIGINT NULL,
    complaint_type VARCHAR(80) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    response VARCHAR(2000) NULL,
    status VARCHAR(30) NOT NULL,
    assigned_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_complaints_user (user_id),
    KEY idx_complaints_order (order_id),
    KEY idx_complaints_status (status),
    CONSTRAINT fk_complaints_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_complaints_order
        FOREIGN KEY (order_id) REFERENCES orders(id)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS support_inquiries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    subject VARCHAR(150) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    response_details VARCHAR(2000) NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_support_inquiries_user (user_id),
    KEY idx_support_inquiries_status (status),
    CONSTRAINT fk_support_inquiries_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS reviews (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NULL,
    order_id BIGINT NULL,
    rating INT NOT NULL,
    comment VARCHAR(1000) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_reviews_order (order_id),
    KEY idx_reviews_user (user_id),
    CONSTRAINT fk_reviews_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_reviews_order
        FOREIGN KEY (order_id) REFERENCES orders(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS site_reviews (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    rating INT NOT NULL,
    comment VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    moderated_at DATETIME(6) NULL,
    moderated_by VARCHAR(255) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_site_reviews_user (user_id),
    KEY idx_site_reviews_status (status),
    CONSTRAINT fk_site_reviews_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT chk_site_reviews_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS supplier_products (
    id BIGINT NOT NULL AUTO_INCREMENT,
    supplier_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    supplier_price DECIMAL(12,2) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    assigned_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_supplier_product (supplier_id, product_id),
    KEY idx_supplier_products_supplier (supplier_id),
    KEY idx_supplier_products_product (product_id),
    CONSTRAINT fk_supplier_products_supplier
        FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_supplier_products_product
        FOREIGN KEY (product_id) REFERENCES products(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT chk_supplier_product_price CHECK (supplier_price IS NULL OR supplier_price >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS purchase_orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    supplier_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    expected_delivery_date DATE NULL,
    received_quantity INT NOT NULL DEFAULT 0,
    received_at DATETIME(6) NULL,
    delivery_note VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_purchase_orders_supplier (supplier_id),
    KEY idx_purchase_orders_product (product_id),
    KEY idx_purchase_orders_status (status),
    CONSTRAINT fk_purchase_orders_supplier
        FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_purchase_orders_product
        FOREIGN KEY (product_id) REFERENCES products(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_purchase_orders_quantity CHECK (quantity > 0),
    CONSTRAINT chk_purchase_orders_price CHECK (unit_price >= 0)
) ENGINE=InnoDB;

-- Order audit table used by runtime MySQL triggers created from data.sql
CREATE TABLE IF NOT EXISTS order_audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    message VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_order_audit_order (order_id)
) ENGINE=InnoDB;


CREATE TABLE IF NOT EXISTS contact_messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone VARCHAR(10) NOT NULL,
    subject VARCHAR(150) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    response VARCHAR(2000) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'NEW',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    responded_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    KEY idx_contact_messages_status (status),
    KEY idx_contact_messages_created_at (created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS system_audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    module VARCHAR(40) NOT NULL,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(60) NOT NULL,
    entity_id BIGINT NULL,
    old_value VARCHAR(500) NULL,
    new_value VARCHAR(500) NULL,
    description VARCHAR(1000) NOT NULL,
    actor_email VARCHAR(254) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_system_audit_module (module),
    KEY idx_system_audit_created_at (created_at),
    KEY idx_system_audit_actor (actor_email)
) ENGINE=InnoDB;

SELECT 'Green Cart MySQL schema is ready.' AS setup_status;
