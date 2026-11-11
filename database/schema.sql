
-- MedTrack Database Schema
-- Database: medtrack
-- Compatible with MariaDB 12.3

CREATE DATABASE IF NOT EXISTS medtrack;
USE medtrack;

-- 1. Medicines
CREATE TABLE medicines (
    medicine_id INT NOT NULL AUTO_INCREMENT,
    medicine_name VARCHAR(150) NOT NULL,
    generic_name VARCHAR(150) DEFAULT NULL,
    category VARCHAR(100) DEFAULT NULL,
    manufacturer VARCHAR(150) DEFAULT NULL,
    unit VARCHAR(30) DEFAULT NULL,
    reorder_level INT DEFAULT 10,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (medicine_id),
    CONSTRAINT chk_med_reorder CHECK (reorder_level >= 0)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_uca1400_ai_ci;


-- 2. Medicine Batches
CREATE TABLE batches (
    batch_id INT NOT NULL AUTO_INCREMENT,
    medicine_id INT NOT NULL,
    batch_number VARCHAR(50) NOT NULL,
    manufacturing_date DATE DEFAULT NULL,
    expiry_date DATE NOT NULL,
    purchase_price DECIMAL(10,2) DEFAULT NULL,
    selling_price DECIMAL(10,2) DEFAULT NULL,
    quantity_received INT NOT NULL,
    available_quantity INT NOT NULL,

    PRIMARY KEY (batch_id),

    UNIQUE KEY uq_medicine_batch (medicine_id, batch_number),
    UNIQUE KEY uq_batch_medicine_pair (medicine_id, batch_id),

    CONSTRAINT fk_batch_medicine
        FOREIGN KEY (medicine_id)
        REFERENCES medicines (medicine_id),

    CONSTRAINT chk_batch_avail_qty
        CHECK (available_quantity >= 0),

    CONSTRAINT chk_batch_recv_qty
        CHECK (quantity_received >= 0),

    CONSTRAINT chk_batch_prices
        CHECK (purchase_price >= 0 AND selling_price >= 0),

    CONSTRAINT chk_available_not_exceed_received
        CHECK (available_quantity <= quantity_received)

) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_uca1400_ai_ci;


-- 3. Stock Allocations
CREATE TABLE stock_allocations (
    allocation_id INT NOT NULL AUTO_INCREMENT,
    medicine_id INT NOT NULL,
    batch_id INT NOT NULL,
    quantity_allocated INT NOT NULL,
    allocation_date TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (allocation_id),

    KEY idx_allocation_batch (batch_id),
    KEY idx_allocation_medicine_batch (medicine_id, batch_id),

    CONSTRAINT fk_allocation_medicine
        FOREIGN KEY (medicine_id)
        REFERENCES medicines (medicine_id),

    CONSTRAINT fk_allocation_batch
        FOREIGN KEY (batch_id)
        REFERENCES batches (batch_id),

    CONSTRAINT fk_allocation_medicine_batch
        FOREIGN KEY (medicine_id, batch_id)
        REFERENCES batches (medicine_id, batch_id),

    CONSTRAINT chk_alloc_qty
        CHECK (quantity_allocated > 0)

) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_uca1400_ai_ci;


-- 4. Stock Transactions
CREATE TABLE stock_transactions (
    transaction_id INT NOT NULL AUTO_INCREMENT,
    batch_id INT NOT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    quantity INT NOT NULL,
    reference_number VARCHAR(100) DEFAULT NULL,
    notes TEXT DEFAULT NULL,
    transaction_date TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (transaction_id),

    KEY idx_transaction_batch (batch_id),

    CONSTRAINT fk_transaction_batch
        FOREIGN KEY (batch_id)
        REFERENCES batches (batch_id),

    CONSTRAINT chk_trans_qty
        CHECK (quantity > 0),

    CONSTRAINT chk_transaction_type
        CHECK (transaction_type IN ('PURCHASE', 'SALE'))

) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_uca1400_ai_ci;
