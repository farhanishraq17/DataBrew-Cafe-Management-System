-- DataBrew Cafe - Extended DB objects: views, procedures, indexes
USE cafedb;

-- ============================================
-- VIEWS
-- ============================================

-- POS menu view: menu items joined with category names
CREATE OR REPLACE VIEW pos_menu_view AS
SELECT mi.id, mi.category_id, c.name AS category_name,
       mi.name, mi.description, mi.price, mi.is_active
FROM menu_items mi
JOIN categories c ON c.id = mi.category_id
WHERE mi.is_active = 1
ORDER BY c.name, mi.name;

-- Dashboard stats: today's quick metrics
CREATE OR REPLACE VIEW dashboard_stats_view AS
SELECT
    (SELECT COUNT(*) FROM orders WHERE DATE(created_at) = CURDATE() AND status <> 'CANCELLED') AS orders_today,
    (SELECT COALESCE(SUM(total), 0) FROM orders WHERE DATE(created_at) = CURDATE() AND status <> 'CANCELLED') AS revenue_today,
    (SELECT COALESCE(AVG(total), 0) FROM orders WHERE DATE(created_at) = CURDATE() AND status <> 'CANCELLED') AS avg_order_value,
    (SELECT COUNT(*) FROM orders WHERE status = 'PENDING') AS pending_orders,
    (SELECT COUNT(*) FROM employees WHERE status = 'Active') AS active_employees,
    (SELECT COUNT(*) FROM inventory inv JOIN ingredients ing ON ing.id = inv.ingredient_id WHERE inv.quantity <= ing.min_threshold) AS low_stock_items;

-- Top selling items ranked by quantity sold
CREATE OR REPLACE VIEW top_selling_items_view AS
SELECT mi.name AS item_name, c.name AS category_name,
       SUM(oi.quantity) AS total_qty_sold,
       SUM(oi.line_total) AS total_revenue,
       COUNT(DISTINCT oi.order_id) AS order_count
FROM order_items oi
JOIN menu_items mi ON mi.id = oi.menu_item_id
JOIN categories c ON c.id = mi.category_id
JOIN orders o ON o.id = oi.order_id
WHERE o.status <> 'CANCELLED'
GROUP BY mi.id, mi.name, c.name
ORDER BY total_qty_sold DESC;

-- Supplier purchase summary
CREATE OR REPLACE VIEW supplier_summary_view AS
SELECT s.id, s.name, s.contact, s.phone, s.email,
       COUNT(p.id) AS total_purchases,
       COALESCE(SUM(p.cost), 0) AS total_spent,
       MAX(p.purchased_at) AS last_purchase_date
FROM suppliers s
LEFT JOIN purchases p ON p.supplier_id = s.id
GROUP BY s.id, s.name, s.contact, s.phone, s.email
ORDER BY s.name;

-- Revenue by category
CREATE OR REPLACE VIEW revenue_by_category_view AS
SELECT c.name AS category_name,
       SUM(oi.quantity) AS items_sold,
       SUM(oi.line_total) AS revenue
FROM order_items oi
JOIN menu_items mi ON mi.id = oi.menu_item_id
JOIN categories c ON c.id = mi.category_id
JOIN orders o ON o.id = oi.order_id
WHERE o.status <> 'CANCELLED'
GROUP BY c.id, c.name
ORDER BY revenue DESC;

-- Payment method breakdown
CREATE OR REPLACE VIEW payment_method_breakdown_view AS
SELECT p.method,
       COUNT(*) AS payment_count,
       SUM(p.amount) AS total_amount
FROM payments p
JOIN orders o ON o.id = p.order_id
WHERE o.status <> 'CANCELLED'
GROUP BY p.method
ORDER BY total_amount DESC;

-- Order history with details
CREATE OR REPLACE VIEW order_history_view AS
SELECT o.id, o.customer_name, o.customer_type, o.status,
       o.subtotal, o.discount_amount, o.tax_amount, o.total,
       o.created_at, o.updated_at,
       d.name AS discount_name, t.name AS tax_name,
       p.method AS payment_method,
       inv.invoice_number
FROM orders o
LEFT JOIN discounts d ON d.id = o.discount_id
LEFT JOIN taxes t ON t.id = o.tax_id
LEFT JOIN payments p ON p.order_id = o.id
LEFT JOIN invoices inv ON inv.order_id = o.id
ORDER BY o.created_at DESC;

-- ============================================
-- STORED PROCEDURES
-- ============================================

-- Record purchase and adjust inventory
DROP PROCEDURE IF EXISTS record_purchase_procedure;
DELIMITER //
CREATE PROCEDURE record_purchase_procedure(
    IN p_supplier_id BIGINT UNSIGNED,
    IN p_ingredient_id BIGINT UNSIGNED,
    IN p_quantity DECIMAL(10,2),
    IN p_cost DECIMAL(10,2)
)
BEGIN
    INSERT INTO purchases (supplier_id, ingredient_id, quantity, cost)
    VALUES (p_supplier_id, p_ingredient_id, p_quantity, p_cost);

    UPDATE inventory SET quantity = quantity + p_quantity
    WHERE ingredient_id = p_ingredient_id;
END//
DELIMITER ;

-- Cancel order and reverse inventory
DROP PROCEDURE IF EXISTS cancel_order_procedure;
DELIMITER //
CREATE PROCEDURE cancel_order_procedure(IN p_order_id BIGINT UNSIGNED)
BEGIN
    DECLARE v_status VARCHAR(20);
    SELECT status INTO v_status FROM orders WHERE id = p_order_id;

    IF v_status = 'CANCELLED' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Order is already cancelled';
    END IF;

    -- Reverse inventory adjustments
    UPDATE inventory inv
    JOIN order_items oi ON oi.order_id = p_order_id
    JOIN menu_items mi ON mi.id = oi.menu_item_id
    JOIN ingredients ing ON ing.id = inv.ingredient_id
    SET inv.quantity = inv.quantity + oi.quantity
    WHERE mi.id = oi.menu_item_id;

    UPDATE orders SET status = 'CANCELLED' WHERE id = p_order_id;

    INSERT INTO audit_logs (user_id, action, entity, entity_id, details)
    VALUES (NULL, 'CANCEL_ORDER', 'orders', p_order_id, CONCAT('Order ', p_order_id, ' cancelled'));
END//
DELIMITER ;

-- Date-range sales report
DROP PROCEDURE IF EXISTS range_sales_report_procedure;
DELIMITER //
CREATE PROCEDURE range_sales_report_procedure(IN p_from DATE, IN p_to DATE)
BEGIN
    SELECT
        DATE(o.created_at) AS sale_date,
        COUNT(DISTINCT o.id) AS orders_count,
        COALESCE(SUM(oi.quantity), 0) AS items_sold,
        COALESCE(SUM(o.total), 0) AS revenue
    FROM orders o
    LEFT JOIN order_items oi ON oi.order_id = o.id
    WHERE DATE(o.created_at) BETWEEN p_from AND p_to
      AND o.status <> 'CANCELLED'
    GROUP BY DATE(o.created_at)
    ORDER BY sale_date;
END//
DELIMITER ;

-- Monthly summary procedure
DROP PROCEDURE IF EXISTS monthly_summary_procedure;
DELIMITER //
CREATE PROCEDURE monthly_summary_procedure(IN p_year INT, IN p_month INT)
BEGIN
    SELECT
        COUNT(DISTINCT o.id) AS total_orders,
        COALESCE(SUM(oi.quantity), 0) AS total_items_sold,
        COALESCE(SUM(o.total), 0) AS total_revenue,
        COALESCE(AVG(o.total), 0) AS avg_order_value,
        (SELECT COUNT(*) FROM orders WHERE YEAR(created_at) = p_year AND MONTH(created_at) = p_month AND status = 'CANCELLED') AS cancelled_orders
    FROM orders o
    LEFT JOIN order_items oi ON oi.order_id = o.id
    WHERE YEAR(o.created_at) = p_year AND MONTH(o.created_at) = p_month
      AND o.status <> 'CANCELLED';
END//
DELIMITER ;

-- ============================================
-- ADDITIONAL TRIGGERS
-- ============================================

-- Log inventory changes
DROP TRIGGER IF EXISTS trg_inventory_after_update;
DELIMITER //
CREATE TRIGGER trg_inventory_after_update
AFTER UPDATE ON inventory
FOR EACH ROW
BEGIN
    IF OLD.quantity <> NEW.quantity THEN
        INSERT INTO audit_logs (user_id, action, entity, entity_id, details)
        VALUES (NULL, 'STOCK_CHANGE', 'inventory', NEW.id,
                CONCAT('Ingredient ', NEW.ingredient_id, ': ', OLD.quantity, ' -> ', NEW.quantity));
    END IF;
END//
DELIMITER ;

-- ============================================
-- INDEXES for performance
-- ============================================
-- MySQL 8.0 doesn't support CREATE INDEX IF NOT EXISTS; use procedure workaround
DROP PROCEDURE IF EXISTS create_index_if_not_exists;
DELIMITER //
CREATE PROCEDURE create_index_if_not_exists()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'audit_logs' AND index_name = 'idx_audit_logs_created') THEN
        CREATE INDEX idx_audit_logs_created ON audit_logs(created_at);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'orders' AND index_name = 'idx_orders_created_status') THEN
        CREATE INDEX idx_orders_created_status ON orders(created_at, status);
    END IF;
END//
DELIMITER ;
CALL create_index_if_not_exists();
DROP PROCEDURE IF EXISTS create_index_if_not_exists;

-- ============================================
-- ADDITIONAL SEED DATA
-- ============================================
INSERT IGNORE INTO discounts (name, type, value, applies_to) VALUES
  ('Staff 15%', 'PERCENT', 15.00, 'STAFF'),
  ('Loyal Customer 20%', 'PERCENT', 20.00, 'LOYAL'),
  ('Flat $5 Off', 'FLAT', 5.00, 'GENERAL');
