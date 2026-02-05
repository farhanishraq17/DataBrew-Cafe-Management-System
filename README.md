# ☕ DataBrew Café Management System

A full-stack **JavaFX desktop application** backed by **MySQL 8.0+** for managing end-to-end café operations — from point-of-sale and inventory to employee scheduling, reporting, and audit logging.

> **CSE 4508 — Relational Database Management System Lab**  
> Islamic University of Technology

---

<div align="center">
  <table width="80%">
    <tr>
      <td style="vertical-align: top; padding-right: 20px;">
        <strong>Authors:</strong><br>
        Md. Farhan Ishraq (220041217)<br>
        Farhan Tahsin Khan (220041229)<br>
        Tashin Mustakim (220041239)
      </td>
      <td style="vertical-align: top; padding-left: 20px;">
        <strong>Supervisor:</strong><br>
        Mueeze Al Mushabbir<br>
        Lecturer, Dept. of CSE,<br>
        IUT
      </td>
    </tr>
  </table>
</div>

---

## 🖼️ ER Diagram

![ER Diagram](https://raw.githubusercontent.com/farhanishraq17/DataBrew-Cafe-Management-System/main/ERD.png)

---

## 📌 Overview

DataBrew Café Management System is a unified, database-driven solution that consolidates core café operations within a single application while maintaining data integrity, referential consistency, and security.

The system follows a layered **Model–View–Controller (MVC)** architecture with a dedicated **DAO** (Data Access Object) layer for all SQL interactions and a **Service** layer for business logic and transactional control.

### At a Glance

| Metric | Value |
|--------|-------|
| Database Tables | **20** (all 3NF) |
| User-Defined Functions | **3** |
| Stored Procedures | **8** |
| Triggers | **4** |
| Database Views | **7** |
| FXML Views | **15** |
| DAO Classes | **16** |

---

## 🛠️ Tech Stack

| Technology | Purpose |
|------------|---------|
| **Java (JDK 17+)** | Core application logic |
| **JavaFX 24 + FXML** | Desktop GUI with 15 views and CSS theming |
| **MySQL 8.0+** | RDBMS — `cafedb` database, InnoDB, utf8mb4 |
| **MySQL Connector/J 9.1.0** | JDBC driver |
| **JDBC** | SQL execution, stored procedure calls, transaction management |
| **SHA-256** (`java.security`) | Secure password hashing |
| **Git & GitHub** | Version control |

---

## ✨ Features

The application provides **17 integrated modules** that cover every aspect of daily café operations. Each feature is described in detail below, including its architecture, database interactions, and key implementation highlights.

---

### 1. User Authentication

Secure login is the gateway to the entire system. The `LoginController` accepts a username and password, delegates to `AuthService.authenticate()`, which queries the `users` table via `UserDao.findByUsername()`. The plain-text password is hashed with SHA-256 using `PasswordUtil` and compared against the stored 64-character hex digest.

**Security flow:**

1. User enters credentials in `LoginView.fxml`.
2. `AuthService` fetches the `User` record and checks `is_active = 1`.
3. `PasswordUtil.hash()` creates a SHA-256 digest of the input.
4. The digest is compared (case-insensitive) against `password_hash`.
5. On success, the `DashboardView.fxml` is loaded and the `User` object is passed to `DashboardController.setCurrentUser()`.

**PasswordUtil implementation:**

```java
public static String hash(String plain) {
    try {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(plain.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(digest);
    } catch (NoSuchAlgorithmException e) {
        throw new IllegalStateException("SHA-256 not available", e);
    }
}

public static boolean verify(String plain, String expectedHash) {
    return hash(plain).equalsIgnoreCase(expectedHash);
}
```

**Database columns involved:**

| Column | Type | Constraint |
|--------|------|------------|
| `username` | `VARCHAR(50)` | `NOT NULL UNIQUE` |
| `password_hash` | `CHAR(64)` | `NOT NULL` (SHA-256 hex) |
| `is_active` | `TINYINT(1)` | `CHECK (is_active IN (0,1))` |

A `BEFORE INSERT` trigger (`trg_users_before_insert`) validates that the password hash is at least 20 characters long, preventing accidental plain-text storage, and writes an audit log entry for every new user.

---

### 2. Role-Based Access Control (RBAC)

Access control is implemented through a many-to-many relationship between `users` and `roles` via the `user_roles` junction table.

**Supported roles:**

| Role | Description |
|------|-------------|
| `ADMIN` | Full system access, user management |
| `STAFF` | POS and basic operations |
| `CASHIER` | Point-of-sale only |
| `MANAGER` | Reports, employees, inventory |
| `INVENTORY_CLERK` | Inventory and supplier management |
| `VIEWER` | Read-only dashboard access |

**Schema:**

```sql
CREATE TABLE roles (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL UNIQUE,
  description VARCHAR(255)
);

CREATE TABLE user_roles (
  user_id BIGINT UNSIGNED NOT NULL,
  role_id BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);
```

In `DashboardController.setCurrentUser()`, the system checks the logged-in user's roles and hides the "User Management" sidebar button for non-admin users:

```java
public void setCurrentUser(User user) {
    this.currentUser = user;
    if (currentUserLabel != null && user != null) {
        currentUserLabel.setText("Current User: " + user.getUsername());
    }
    if (userMgmtBtn != null && user != null) {
        try {
            java.util.List<String> roles = new UserDao().getUserRoles(user.getId());
            boolean isAdmin = roles.stream().anyMatch(r -> r.toLowerCase().contains("admin"));
            userMgmtBtn.setVisible(isAdmin);
            userMgmtBtn.setManaged(isAdmin);
        } catch (Exception e) {
            System.err.println("Failed to check user roles: " + e.getMessage());
        }
    }
}
```

The `UserManagementController` dynamically creates role checkboxes from the database at runtime, allowing administrators to assign any combination of roles to any user.

---

### 3. Dashboard with KPIs

The dashboard is the main landing page after login and provides at-a-glance operational metrics fetched directly from the database.

**Metrics displayed:**

| Metric | SQL Source |
|--------|-----------|
| Today's Revenue | `SELECT COALESCE(SUM(total),0) FROM orders WHERE DATE(created_at)=CURDATE() AND status<>'CANCELLED'` |
| Today's Orders | `SELECT COUNT(*) FROM orders WHERE DATE(created_at)=CURDATE() AND status<>'CANCELLED'` |
| Pending Orders | `SELECT COUNT(*) FROM orders WHERE status='PENDING'` |
| Active Menu Items | `SELECT COUNT(*) FROM menu_items WHERE is_active=1` |

**Dashboard tables:**

| Table | Data Source | Columns Shown |
|-------|-------------|---------------|
| Top Sellers | `top_selling_items_view` (LIMIT 10) | Item Name, Qty Sold, Revenue |
| Low Stock | Custom query on `inventory JOIN ingredients` | Ingredient, Quantity, Min Threshold, Status |
| Recent Orders | `orders ORDER BY created_at DESC` (LIMIT 10) | ID, Customer, Total, Status, Date |

The low-stock table uses a color-coded status cell factory:

```java
private void colorStatusCol(TableColumn col, int idx) {
    col.setCellFactory(c -> new TableCell<ObservableList<String>, String>() {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) { setText(null); setStyle(""); return; }
            setText(item);
            if (item.contains("Low") || item.equals("CANCELLED")) {
                setStyle("-fx-text-fill: #dc2626; -fx-font-weight: 800;");
            } else if (item.equals("PAID") || item.equals("OK")) {
                setStyle("-fx-text-fill: #16a34a; -fx-font-weight: 800;");
            } else if (item.equals("PENDING")) {
                setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: 800;");
            } else {
                setStyle("-fx-font-weight: 700;");
            }
        }
    });
}
```

The dashboard also provides a database-backed view (`dashboard_stats_view`) that aggregates today's orders, revenue, average order value, pending orders, active employees, and low-stock item counts in a single SQL statement:

```sql
CREATE OR REPLACE VIEW dashboard_stats_view AS
SELECT
    (SELECT COUNT(*) FROM orders WHERE DATE(created_at) = CURDATE() AND status <> 'CANCELLED') AS orders_today,
    (SELECT COALESCE(SUM(total), 0) FROM orders WHERE DATE(created_at) = CURDATE() AND status <> 'CANCELLED') AS revenue_today,
    (SELECT COALESCE(AVG(total), 0) FROM orders WHERE DATE(created_at) = CURDATE() AND status <> 'CANCELLED') AS avg_order_value,
    (SELECT COUNT(*) FROM orders WHERE status = 'PENDING') AS pending_orders,
    (SELECT COUNT(*) FROM employees WHERE status = 'Active') AS active_employees,
    (SELECT COUNT(*) FROM inventory inv JOIN ingredients ing ON ing.id = inv.ingredient_id
     WHERE inv.quantity <= ing.min_threshold) AS low_stock_items;
```

**Navigation:** The sidebar provides 14 navigation buttons, each loading a different FXML view into the center pane of the `BorderPane` layout via `FXMLLoader`:

```java
private void loadView(String fxmlFile) {
    try {
        Parent view = FXMLLoader.load(getClass().getResource("/fxml/" + fxmlFile));
        mainLayout.setCenter(view);
    } catch (IOException e) {
        System.err.println("Could not load view: " + fxmlFile);
        e.printStackTrace();
    }
}
```

---

### 4. Point-of-Sale (POS)

The POS module is the core revenue-generating interface. It presents a visual card-based menu grid, a shopping cart, and a full checkout pipeline.

**Architecture overview:**

| Component | Role |
|-----------|------|
| `PosController` | FXML controller — UI logic, cart management, checkout |
| `OrderService` | Transactional orchestration — creates order, records payment, generates invoice |
| `OrderDao` | Raw SQL inserts for orders and order items |
| `PaymentDao` | Payment record insertion |
| `InvoiceDao` | Invoice storage |
| `MenuDao` | Fetches active menu items |
| `CategoryDao` | Fetches categories for filtering |
| `DiscountDao` | Loads applicable discounts by customer type |
| `TaxDao` | Loads available tax definitions |

**Menu card grid:**

Menu items are displayed as styled cards inside a `FlowPane` wrapped in a `ScrollPane`. Each card shows a category-appropriate emoji icon, the item name, category label, and price. Clicking a card adds the item to the cart.

```java
private VBox createMenuCard(MenuItem item) {
    VBox card = new VBox(6);
    card.getStyleClass().add("pos-menu-card");
    card.setAlignment(Pos.CENTER);

    StackPane imgHolder = new StackPane();
    imgHolder.getStyleClass().add("pos-card-image");
    Label icon = new Label(iconForCategory(item.getCategoryId()));
    icon.getStyleClass().add("pos-card-image-icon");
    imgHolder.getChildren().add(icon);

    Label name = new Label(item.getName());
    name.getStyleClass().add("pos-card-name");
    name.setWrapText(true);
    name.setMaxWidth(160);

    String catName = categoryNames.getOrDefault(item.getCategoryId(), "");
    Label catLabel = new Label(catName);
    catLabel.getStyleClass().add("pos-card-category");

    Label price = new Label(String.format("$%.2f", item.getPrice()));
    price.getStyleClass().add("pos-card-price");

    card.getChildren().addAll(imgHolder, name, catLabel, price);
    card.setOnMouseClicked(e -> addToCart(item));
    return card;
}
```

**Category icon mapping:**

```java
private static final Map<String, String> CATEGORY_ICONS = new HashMap<>();
static {
    CATEGORY_ICONS.put("coffee",    "☕");
    CATEGORY_ICONS.put("tea",       "🍵");
    CATEGORY_ICONS.put("juice",     "🧃");
    CATEGORY_ICONS.put("smoothie",  "🥤");
    CATEGORY_ICONS.put("pastry",    "🥐");
    CATEGORY_ICONS.put("cake",      "🍰");
    CATEGORY_ICONS.put("sandwich",  "🥪");
    CATEGORY_ICONS.put("dessert",   "🍩");
    CATEGORY_ICONS.put("snack",     "🍿");
    CATEGORY_ICONS.put("breakfast", "🥞");
    CATEGORY_ICONS.put("salad",     "🥗");
    CATEGORY_ICONS.put("beverage",  "🍹");
}
```

**Filtering:**

Items can be filtered simultaneously by category (dropdown) and search text (text field). Both listeners feed into `applyFilter()`:

```java
private void applyFilter() {
    String catSel = categoryCombo.getValue();
    String term = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();

    filteredMenuItems.setAll(allMenuItems.filtered(item -> {
        boolean matchCat = (catSel == null || catSel.equals("All Categories") ||
                categoryNames.getOrDefault(item.getCategoryId(), "").equals(catSel));
        boolean matchSearch = term.isEmpty() || item.getName().toLowerCase().contains(term) ||
                (item.getDescription() != null && item.getDescription().toLowerCase().contains(term));
        return matchCat && matchSearch;
    }));
    buildMenuCards();
}
```

**Cart management:**

The cart is an `ObservableList<OrderItem>` rendered in a `TableView`. Adding an item that is already in the cart increments its quantity instead of adding a duplicate row:

```java
private void addToCart(MenuItem selected) {
    for (OrderItem oi : cartItems) {
        if (oi.getMenuItemId() == selected.getId()) {
            oi.setQuantity(oi.getQuantity() + 1);
            oi.setLineTotal(oi.getQuantity() * oi.getUnitPrice());
            cartTable.refresh();
            updateTotals();
            return;
        }
    }
    OrderItem oi = new OrderItem();
    oi.setMenuItemId(selected.getId());
    oi.setQuantity(1);
    oi.setUnitPrice(selected.getPrice());
    oi.setLineTotal(selected.getPrice());
    cartItems.add(oi);
    updateTotals();
}
```

**Discount & tax computation (client-side preview):**

```java
private void updateTotals() {
    double subtotal = cartItems.stream().mapToDouble(OrderItem::getLineTotal).sum();
    Discount d = getSelectedDiscount();
    double discountAmt = 0;
    if (d != null) {
        discountAmt = d.getType().equals("PERCENT") ? subtotal * d.getValue() / 100.0 : d.getValue();
    }
    double afterDiscount = subtotal - discountAmt;
    Tax t = getSelectedTax();
    double taxAmt = 0;
    if (t != null) {
        taxAmt = afterDiscount * t.getRate() / 100.0;
    }
    double total = afterDiscount + taxAmt;

    subtotalLabel.setText(String.format("$%.2f", subtotal));
    discountLabel.setText(String.format("-$%.2f", discountAmt));
    taxLabel.setText(String.format("+$%.2f", taxAmt));
    totalLabel.setText(String.format("$%.2f", total));
}
```

**Checkout flow (transactional):**

The checkout delegates to `OrderService.createOrderWithItems()` which runs inside a JDBC transaction with manual commit/rollback:

```java
public long createOrderWithItems(Order order, List<OrderItem> items) throws SQLException {
    try (Connection conn = DBConnection.getConnection()) {
        conn.setAutoCommit(false);
        try {
            long orderId = orderDao.insertOrder(conn, order);
            orderDao.insertItems(conn, orderId, items);
            try (CallableStatement cs = conn.prepareCall("{call create_order_procedure(?,?,?,?)}")) {
                cs.setString(1, order.getCustomerName());
                cs.setString(2, order.getCustomerType());
                if (order.getTaxId() == null) cs.setNull(3, java.sql.Types.BIGINT);
                else cs.setLong(3, order.getTaxId());
                if (order.getDiscountId() == null) cs.setNull(4, java.sql.Types.BIGINT);
                else cs.setLong(4, order.getDiscountId());
                cs.execute();
            }
            conn.commit();
            return orderId;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }
}
```

After order creation, payment and invoice are recorded in a second transaction:

```java
public void recordPaymentAndInvoice(long orderId, double amount, String method) throws SQLException {
    try (Connection conn = DBConnection.getConnection()) {
        conn.setAutoCommit(false);
        try {
            Payment p = new Payment();
            p.setOrderId(orderId);
            p.setAmount(amount);
            p.setMethod(method);
            long paymentId = paymentDao.insert(conn, p);
            try (CallableStatement cs = conn.prepareCall("{call generate_invoice_procedure(?,?)}")) {
                cs.setLong(1, orderId);
                cs.setLong(2, paymentId);
                cs.execute();
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }
}
```

**Receipt dialog:**

On successful checkout, a monospaced ASCII-art receipt is displayed in a `TextArea` inside an `Alert` dialog:

```
═══════════════════════════════════
         DATABREW CAFE
           RECEIPT
═══════════════════════════════════
Order #: 42
Customer: John (STUDENT)
───────────────────────────────────
Espresso             x2  $7.00
Latte                x1  $4.50
───────────────────────────────────
Subtotal:              $11.50
Discount (Student 10):  -$1.15
Tax (VAT 15.0%):       +$1.55
───────────────────────────────────
TOTAL:                 $11.90
Payment: CASH
═══════════════════════════════════
       Thank you! Come again!
```

---

### 5. Menu Management

The `MenuController` provides full CRUD operations on `menu_items`. Items are linked to categories via `category_id` foreign key. Each item has a name, description, price, and active/inactive status.

**Schema:**

```sql
CREATE TABLE menu_items (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  category_id BIGINT UNSIGNED NOT NULL,
  name VARCHAR(120) NOT NULL,
  description VARCHAR(255),
  price DECIMAL(10,2) NOT NULL CHECK (price >= 0),
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  CONSTRAINT fk_menu_category FOREIGN KEY (category_id) REFERENCES categories(id),
  UNIQUE (category_id, name)
);
```

**Key behaviors:**

- **Soft-delete fallback:** When a hard `DELETE` fails due to FK constraints (order items referencing the menu item), the controller falls back to setting `is_active = 0`.
- **Unique constraint:** `UNIQUE (category_id, name)` prevents duplicate item names within the same category.
- **DAO pattern:** `MenuDao` provides `findActive()`, `findAll()`, `insert()`, `update()`, and `delete()` methods, each opening and closing its own JDBC connection.

```java
public void insert(MenuItem item) throws SQLException {
    String sql = "INSERT INTO menu_items (category_id, name, description, price, is_active) VALUES (?,?,?,?,?)";
    try (Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setLong(1, item.getCategoryId());
        ps.setString(2, item.getName());
        ps.setString(3, item.getDescription());
        ps.setDouble(4, item.getPrice());
        ps.setBoolean(5, item.isActive());
        ps.executeUpdate();
    }
}
```

---

### 6. Inventory & Ingredients

The inventory system tracks raw ingredients and their stock levels. Each ingredient has a minimum threshold that triggers a "Low" stock alert on the dashboard and in the inventory view.

**Schema (3 tables):**

```sql
CREATE TABLE ingredients (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(120) NOT NULL UNIQUE,
  unit VARCHAR(30) NOT NULL,
  min_threshold DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (min_threshold >= 0)
);

CREATE TABLE inventory (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  ingredient_id BIGINT UNSIGNED NOT NULL UNIQUE,
  quantity DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (quantity >= 0),
  last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_inventory_ing FOREIGN KEY (ingredient_id) REFERENCES ingredients(id)
);

CREATE TABLE menu_item_ingredients (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  menu_item_id BIGINT UNSIGNED NOT NULL,
  ingredient_id BIGINT UNSIGNED NOT NULL,
  qty_needed DECIMAL(10,2) NOT NULL DEFAULT 1.00,
  CONSTRAINT fk_mii_menu FOREIGN KEY (menu_item_id) REFERENCES menu_items(id),
  CONSTRAINT fk_mii_ing  FOREIGN KEY (ingredient_id) REFERENCES ingredients(id),
  UNIQUE KEY uq_menu_ing (menu_item_id, ingredient_id)
);
```

**Relationship chain:** `menu_items` ← `menu_item_ingredients` → `ingredients` ← `inventory` (1:1)

This junction table allows each menu item to map to multiple ingredients with specific quantities needed per unit sold. For example:

| Menu Item | Ingredient | Qty Needed |
|-----------|-----------|------------|
| Espresso | Coffee Beans | 18g |
| Latte | Coffee Beans | 18g |
| Latte | Milk | 200ml |
| Croissant | Butter | 30g |
| Croissant | Flour | 50g |
| Chocolate Cake | Chocolate Powder | 30g |
| Chocolate Cake | Cream | 50g |
| Chocolate Cake | Sugar | 40g |
| Chocolate Cake | Flour | 60g |

**Automatic stock deduction (trigger):**

When an order item is inserted, the `trg_order_items_after_insert` trigger automatically deducts the required ingredients from inventory:

```sql
CREATE TRIGGER trg_order_items_after_insert
AFTER INSERT ON order_items FOR EACH ROW
BEGIN
  UPDATE inventory inv
  JOIN menu_item_ingredients mii ON mii.ingredient_id = inv.ingredient_id
  SET inv.quantity = inv.quantity - (mii.qty_needed * NEW.quantity)
  WHERE mii.menu_item_id = NEW.menu_item_id
    AND inv.quantity >= (mii.qty_needed * NEW.quantity);
END;
```

**Inventory audit trail:**

Stock changes are automatically logged by the `trg_inventory_after_update` trigger:

```sql
CREATE TRIGGER trg_inventory_after_update
AFTER UPDATE ON inventory FOR EACH ROW
BEGIN
    IF OLD.quantity <> NEW.quantity THEN
        INSERT INTO audit_logs (user_id, action, entity, entity_id, details)
        VALUES (NULL, 'STOCK_CHANGE', 'inventory', NEW.id,
                CONCAT('Ingredient ', NEW.ingredient_id, ': ', OLD.quantity, ' -> ', NEW.quantity));
    END IF;
END;
```

**Service layer:**

`InventoryService` provides business operations: `listInventory()`, `listLowStock()`, `addItem()`, `updateItem()`, and `deleteItem()`. The `addItem()` method inserts both the ingredient and its inventory row in a single transaction:

```java
public long insertIngredientWithStock(String name, String unit, double minThreshold, double quantity)
        throws SQLException {
    try (Connection conn = DBConnection.getConnection()) {
        conn.setAutoCommit(false);
        try {
            String ingSql = "INSERT INTO ingredients (name, unit, min_threshold) VALUES (?,?,?)";
            long ingredientId;
            try (PreparedStatement ps = conn.prepareStatement(ingSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.setString(2, unit);
                ps.setDouble(3, minThreshold);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) ingredientId = rs.getLong(1);
                    else throw new SQLException("Failed to get ingredient ID");
                }
            }
            String invSql = "INSERT INTO inventory (ingredient_id, quantity) VALUES (?,?)";
            long invId;
            try (PreparedStatement ps = conn.prepareStatement(invSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, ingredientId);
                ps.setDouble(2, quantity);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) invId = rs.getLong(1);
                    else throw new SQLException("Failed to get inventory ID");
                }
            }
            conn.commit();
            return invId;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        }
    }
}
```

**Low-stock detection:** The inventory table view color-codes items as red ("Low") when `quantity < min_threshold` and green ("OK") otherwise.

---

### 7. Supplier Management

The `SupplierController` provides CRUD operations for supplier contacts. Suppliers are linked to purchase records.

**Schema:**

```sql
CREATE TABLE suppliers (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(120) NOT NULL UNIQUE,
  contact VARCHAR(120),
  phone VARCHAR(20),
  email VARCHAR(120)
);
```

**View support:** The `supplier_summary_view` aggregates purchase data per supplier:

```sql
CREATE OR REPLACE VIEW supplier_summary_view AS
SELECT s.id, s.name, s.contact, s.phone, s.email,
       COUNT(p.id) AS total_purchases,
       COALESCE(SUM(p.cost), 0) AS total_spent,
       MAX(p.purchased_at) AS last_purchase_date
FROM suppliers s
LEFT JOIN purchases p ON p.supplier_id = s.id
GROUP BY s.id, s.name, s.contact, s.phone, s.email
ORDER BY s.name;
```

---

### 8. Purchase Management

The `PurchaseController` records ingredient purchases from suppliers. Each purchase atomically inserts a purchase record and adjusts the inventory.

**Schema:**

```sql
CREATE TABLE purchases (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  supplier_id BIGINT UNSIGNED NOT NULL,
  ingredient_id BIGINT UNSIGNED NOT NULL,
  quantity DECIMAL(10,2) NOT NULL CHECK (quantity > 0),
  cost DECIMAL(10,2) NOT NULL CHECK (cost >= 0),
  purchased_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_purchase_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
  CONSTRAINT fk_purchase_ing FOREIGN KEY (ingredient_id) REFERENCES ingredients(id),
  INDEX (supplier_id), INDEX (ingredient_id)
);
```

**Stored procedure (atomic operation):**

```sql
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
END;
```

Purchase records are immutable — they cannot be edited or deleted after creation, providing a reliable audit trail for procurement.

---

### 9. Employee Management

The `EmployeeController` manages employee records with a tabbed detail pane offering three views: **Details**, **Shifts**, and **Accounts**.

**Schema:**

```sql
CREATE TABLE employees (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT UNSIGNED UNIQUE,
  position VARCHAR(80) NOT NULL,
  hire_date DATE NOT NULL,
  salary DECIMAL(10,2) NOT NULL,
  CONSTRAINT fk_employees_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);
```

**Key features:**

- Each employee can be optionally linked to a `user` account via `user_id` (nullable FK with `ON DELETE SET NULL`).
- The Details tab shows position, hire date, and salary.
- The Shifts tab lists assigned shifts and attendance records.
- The Accounts tab links employee records to user credentials.

---

### 10. Shift & Attendance

The shift management module defines named time-based shifts and tracks employee attendance.

**Schema:**

```sql
CREATE TABLE shifts (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL UNIQUE,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL
);

CREATE TABLE attendance (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  employee_id BIGINT UNSIGNED NOT NULL,
  shift_id BIGINT UNSIGNED NOT NULL,
  work_date DATE NOT NULL,
  check_in DATETIME,
  check_out DATETIME,
  status ENUM('PRESENT','ABSENT','LATE') NOT NULL DEFAULT 'PRESENT',
  CONSTRAINT fk_att_emp FOREIGN KEY (employee_id) REFERENCES employees(id),
  CONSTRAINT fk_att_shift FOREIGN KEY (shift_id) REFERENCES shifts(id),
  UNIQUE (employee_id, work_date)
);
```

**Default shift definitions:**

| Shift | Start | End |
|-------|-------|-----|
| Morning | 06:00 | 14:00 |
| Day | 14:00 | 22:00 |
| Night | 22:00 | 06:00 |

**Features:**

- Track check-in and check-out times.
- Mark employees as PRESENT, ABSENT, or LATE.
- Date-range filtering to view attendance over specific periods.
- Color-coded status display (green for PRESENT, red for ABSENT, amber for LATE).
- `UNIQUE (employee_id, work_date)` prevents duplicate attendance entries per day.

---

### 11. Order History

The `OrderHistoryController` provides a master-detail view of all orders with powerful filtering capabilities.

**Filtering options:**

| Filter | Type | Description |
|--------|------|-------------|
| Date Range | `DatePicker` pair | From and To dates |
| Status | `ComboBox` | ALL, PENDING, PAID, CANCELLED |
| Customer Name | `TextField` | Partial match via `LIKE '%term%'` |

**Master query (dynamic SQL construction):**

```java
StringBuilder sql = new StringBuilder(
    "SELECT o.id, o.customer_name, o.customer_type, o.status, o.subtotal, o.tax_amount, " +
    "o.discount_amount, o.total, o.created_at, " +
    "p.method AS payment_method, inv.invoice_number " +
    "FROM orders o " +
    "LEFT JOIN payments p ON p.order_id = o.id " +
    "LEFT JOIN invoices inv ON inv.order_id = o.id " +
    "WHERE 1=1 ");

List<Object> params = new ArrayList<>();
if (from != null) { sql.append("AND DATE(o.created_at) >= ? "); params.add(Date.valueOf(from)); }
if (to != null)   { sql.append("AND DATE(o.created_at) <= ? "); params.add(Date.valueOf(to)); }
if (status != null && !status.equals("ALL")) { sql.append("AND o.status = ? "); params.add(status); }
if (search != null && !search.isBlank()) { sql.append("AND o.customer_name LIKE ? "); params.add("%" + search.trim() + "%"); }
sql.append("ORDER BY o.created_at DESC LIMIT 500");
```

**Detail view:** Clicking an order in the master table loads its line items — menu item name, quantity, unit price, and line total — queried via a `JOIN` between `order_items` and `menu_items`.

**Order cancellation:** The Cancel button invokes the `cancel_order_procedure` stored procedure, which reverses all inventory adjustments, marks the order as CANCELLED, and writes an audit log entry:

```java
try (Connection conn = DBConnection.getConnection();
     CallableStatement cs = conn.prepareCall("{CALL cancel_order_procedure(?)}")) {
    cs.setLong(1, selected.getId());
    cs.execute();
}
```

---

### 12. Discount Management

The `DiscountController` provides CRUD for discount definitions. Discounts are scoped to specific customer types and come in two variants.

**Schema:**

```sql
CREATE TABLE discounts (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(80) NOT NULL UNIQUE,
  type ENUM('PERCENT','FLAT') NOT NULL,
  value DECIMAL(10,2) NOT NULL CHECK (value >= 0),
  applies_to ENUM('GENERAL','STUDENT','STAFF','LOYAL') NOT NULL
);
```

**Discount types:**

| Type | Behavior | Example |
|------|----------|---------|
| `PERCENT` | Deducts a percentage of the subtotal | "Student 10%" → 10% off |
| `FLAT` | Deducts a fixed dollar amount (capped at subtotal) | "Flat $5 Off" → $5 off |

**Seed data:**

```sql
INSERT IGNORE INTO discounts (name, type, value, applies_to) VALUES
  ('Student 10', 'PERCENT', 10.00, 'STUDENT'),
  ('Staff 15%', 'PERCENT', 15.00, 'STAFF'),
  ('Loyal Customer 20%', 'PERCENT', 20.00, 'LOYAL'),
  ('Flat $5 Off', 'FLAT', 5.00, 'GENERAL');
```

**POS integration:** When the customer type changes in the POS dropdown, `loadDiscountsForType()` queries the `discounts` table for applicable discounts and refreshes the discount combo box.

---

### 13. Tax Configuration

The `TaxController` provides CRUD for tax definitions. Taxes are percentage-based and applied after discounts.

**Schema:**

```sql
CREATE TABLE taxes (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(80) NOT NULL UNIQUE,
  rate DECIMAL(5,2) NOT NULL CHECK (rate >= 0)
);
```

**Tax computation formula:** `tax_amount = (subtotal - discount_amount) × rate / 100`

This is computed both client-side (for live preview in the POS) and server-side (by the `calculate_tax` database function during order finalization).

---

### 14. Reports Module

The `ReportsController` offers **6 report types**, each rendered dynamically using `ResultSetMetaData` — meaning the table columns are generated at runtime from whatever the query returns.

**Available reports:**

| Report | Data Source | Parameters |
|--------|-----------|------------|
| Daily Sales | `range_sales_report_procedure` | From date, To date |
| Monthly Summary | `monthly_summary_procedure` | Year, Month |
| Revenue by Category | `revenue_by_category_view` | None |
| Payment Breakdown | `payment_method_breakdown_view` | None |
| Top Sellers | `top_selling_items_view` | None |
| Supplier Summary | `supplier_summary_view` | None |

**Dynamic table rendering (ResultSetMetaData):**

```java
private void populateTable(ResultSet rs) throws SQLException {
    reportTable.getColumns().clear();
    reportTable.getItems().clear();

    ResultSetMetaData meta = rs.getMetaData();
    int colCount = meta.getColumnCount();

    for (int i = 1; i <= colCount; i++) {
        final int idx = i - 1;
        TableColumn<ObservableList<String>, String> col = new TableColumn<>(
                prettifyColumnName(meta.getColumnLabel(i)));
        col.setCellValueFactory(cell -> new SimpleStringProperty(
                idx < cell.getValue().size() ? cell.getValue().get(idx) : ""));
        col.setPrefWidth(140);
        reportTable.getColumns().add(col);
    }

    ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
    while (rs.next()) {
        ObservableList<String> row = FXCollections.observableArrayList();
        for (int i = 1; i <= colCount; i++) {
            String val = rs.getString(i);
            row.add(val != null ? val : "");
        }
        data.add(row);
    }
    reportTable.setItems(data);
}
```

**Column name prettification:** SQL column names like `total_qty_sold` are converted to `Total qty sold` for display:

```java
private String prettifyColumnName(String raw) {
    return raw.replace("_", " ").substring(0, 1).toUpperCase() + raw.replace("_", " ").substring(1);
}
```

**Monthly summary with KPI bar:** The monthly summary report also populates a summary bar above the table showing Total Orders, Items Sold, Revenue, and Average Order Value.

**Stored procedure calls (CallableStatement):**

```java
public void onDailySales() {
    String sql = "{CALL range_sales_report_procedure(?, ?)}";
    try (Connection conn = DBConnection.getConnection();
         CallableStatement cs = conn.prepareCall(sql)) {
        cs.setDate(1, Date.valueOf(from));
        cs.setDate(2, Date.valueOf(to));
        try (ResultSet rs = cs.executeQuery()) {
            populateTable(rs);
        }
    } catch (SQLException e) {
        showError(e);
    }
}
```

---

### 15. Invoice Generation

Invoices are automatically generated via the `generate_invoice_procedure` stored procedure when a payment is recorded during checkout.

**Schema:**

```sql
CREATE TABLE invoices (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT UNSIGNED NOT NULL UNIQUE,
  invoice_number VARCHAR(50) NOT NULL UNIQUE,
  payment_id BIGINT UNSIGNED,
  total DECIMAL(10,2) NOT NULL,
  issued_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_inv_order FOREIGN KEY (order_id) REFERENCES orders(id),
  CONSTRAINT fk_inv_payment FOREIGN KEY (payment_id) REFERENCES payments(id)
);
```

**Invoice number format:** `INV-YYYYMMDD-000XXX` where `XXX` is the zero-padded order ID.

```sql
CREATE PROCEDURE generate_invoice_procedure(IN p_order_id BIGINT UNSIGNED, IN p_payment_id BIGINT UNSIGNED)
BEGIN
  DECLARE v_invoice_no VARCHAR(50);
  DECLARE v_total DECIMAL(10,2);
  SELECT total INTO v_total FROM orders WHERE id = p_order_id;
  SET v_invoice_no = CONCAT('INV-', DATE_FORMAT(NOW(), '%Y%m%d'), '-', LPAD(p_order_id, 6, '0'));
  INSERT INTO invoices (order_id, invoice_number, payment_id, total)
  VALUES (p_order_id, v_invoice_no, p_payment_id, v_total);
END;
```

**Uniqueness:** `UNIQUE(order_id)` ensures one invoice per order. `UNIQUE(invoice_number)` prevents duplicate invoice numbers.

---

### 16. Audit Logging

The audit system maintains a comprehensive trail of all significant actions across the application, populated automatically by triggers and stored procedures.

**Schema:**

```sql
CREATE TABLE audit_logs (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT UNSIGNED,
  action VARCHAR(120) NOT NULL,
  entity VARCHAR(80),
  entity_id BIGINT,
  details TEXT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);
```

**Events that generate audit log entries:**

| Action | Source | Details |
|--------|--------|---------|
| `CREATE_USER` | `trg_users_before_insert` | "Created user {username}" |
| `PAYMENT` | `trg_payments_after_insert` | "Payment {id} recorded" |
| `STOCK_CHANGE` | `trg_inventory_after_update` | "Ingredient {id}: {old_qty} -> {new_qty}" |
| `CANCEL_ORDER` | `cancel_order_procedure` | "Order {id} cancelled" |

The `AuditLogController` provides a filterable viewer for the audit trail, allowing administrators to search by action type, entity, date range, and user.

**Performance index:**

```sql
CREATE INDEX idx_audit_logs_created ON audit_logs(created_at);
```

---

### 17. User Management (Admin Only)

The `UserManagementController` is accessible only to users with the `ADMIN` role. It provides complete user lifecycle management.

**Capabilities:**

- **Create users** — username, email, full name, phone, password (hashed with SHA-256 before storage)
- **Edit users** — update profile information, toggle active/inactive status
- **Delete users** — cascades to `user_roles` via `ON DELETE CASCADE`
- **Assign roles** — dynamic checkboxes loaded from the `roles` table at runtime
- **Reset passwords** — administrator can set a new password hash

**Users schema:**

```sql
CREATE TABLE users (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  email VARCHAR(120) NOT NULL UNIQUE,
  password_hash CHAR(64) NOT NULL,
  full_name VARCHAR(120) NOT NULL,
  phone VARCHAR(20),
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CHECK (is_active IN (0,1))
);
```

---

## 🧩 Database Design

The `cafedb` database uses the **InnoDB** storage engine with **utf8mb4** character set and **utf8mb4_unicode_ci** collation. All 20 tables are normalized to **Third Normal Form (3NF)**.

### Complete Table Reference

Below is the full schema for every table in the database, organized by functional module.

---

#### Module 1: Authentication & Access Control

**`roles`** — Defines system roles for access control.

```sql
CREATE TABLE roles (
  id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  name        VARCHAR(50)  NOT NULL UNIQUE,
  description VARCHAR(255)
);
```

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `BIGINT UNSIGNED` | `PK, AUTO_INCREMENT` | Unique role identifier |
| `name` | `VARCHAR(50)` | `NOT NULL, UNIQUE` | Role name (e.g., ADMIN) |
| `description` | `VARCHAR(255)` | — | Human-readable description |

---

**`users`** — Stores user credentials and profile information.

```sql
CREATE TABLE users (
  id            BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  username      VARCHAR(50)  NOT NULL UNIQUE,
  email         VARCHAR(120) NOT NULL UNIQUE,
  password_hash CHAR(64)     NOT NULL,
  full_name     VARCHAR(120) NOT NULL,
  phone         VARCHAR(20),
  is_active     TINYINT(1)   NOT NULL DEFAULT 1,
  created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CHECK (is_active IN (0,1))
);
```

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `BIGINT UNSIGNED` | `PK, AUTO_INCREMENT` | Unique user identifier |
| `username` | `VARCHAR(50)` | `NOT NULL, UNIQUE` | Login username |
| `email` | `VARCHAR(120)` | `NOT NULL, UNIQUE` | Email address |
| `password_hash` | `CHAR(64)` | `NOT NULL` | SHA-256 hex digest |
| `full_name` | `VARCHAR(120)` | `NOT NULL` | Display name |
| `phone` | `VARCHAR(20)` | Nullable | Contact number |
| `is_active` | `TINYINT(1)` | `DEFAULT 1, CHECK` | Account active flag |
| `created_at` | `TIMESTAMP` | `DEFAULT CURRENT_TIMESTAMP` | Registration time |

---

**`user_roles`** — Many-to-many junction between users and roles.

```sql
CREATE TABLE user_roles (
  user_id BIGINT UNSIGNED NOT NULL,
  role_id BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);
```

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `user_id` | `BIGINT UNSIGNED` | `PK, FK → users.id ON DELETE CASCADE` | User reference |
| `role_id` | `BIGINT UNSIGNED` | `PK, FK → roles.id ON DELETE CASCADE` | Role reference |

---

#### Module 2: Menu & Categories

**`categories`** — Menu item categories.

```sql
CREATE TABLE categories (
  id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  name        VARCHAR(80) NOT NULL UNIQUE,
  description VARCHAR(255)
);
```

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `BIGINT UNSIGNED` | `PK, AUTO_INCREMENT` | Category identifier |
| `name` | `VARCHAR(80)` | `NOT NULL, UNIQUE` | Category name |
| `description` | `VARCHAR(255)` | Nullable | Description |

---

**`menu_items`** — Products available for sale.

```sql
CREATE TABLE menu_items (
  id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  category_id BIGINT UNSIGNED NOT NULL,
  name        VARCHAR(120) NOT NULL,
  description VARCHAR(255),
  price       DECIMAL(10,2) NOT NULL CHECK (price >= 0),
  is_active   TINYINT(1) NOT NULL DEFAULT 1,
  CONSTRAINT fk_menu_category FOREIGN KEY (category_id) REFERENCES categories(id),
  UNIQUE (category_id, name)
);
```

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `BIGINT UNSIGNED` | `PK, AUTO_INCREMENT` | Item identifier |
| `category_id` | `BIGINT UNSIGNED` | `FK → categories.id` | Parent category |
| `name` | `VARCHAR(120)` | `NOT NULL` | Item name |
| `description` | `VARCHAR(255)` | Nullable | Item description |
| `price` | `DECIMAL(10,2)` | `NOT NULL, CHECK ≥ 0` | Unit price |
| `is_active` | `TINYINT(1)` | `DEFAULT 1` | Soft-delete flag |

---

#### Module 3: Inventory & Ingredients

**`ingredients`** — Raw materials used in menu items.

```sql
CREATE TABLE ingredients (
  id            BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  name          VARCHAR(120) NOT NULL UNIQUE,
  unit          VARCHAR(30) NOT NULL,
  min_threshold DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (min_threshold >= 0)
);
```

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `BIGINT UNSIGNED` | `PK, AUTO_INCREMENT` | Ingredient identifier |
| `name` | `VARCHAR(120)` | `NOT NULL, UNIQUE` | Ingredient name |
| `unit` | `VARCHAR(30)` | `NOT NULL` | Measurement unit (g, ml, pcs) |
| `min_threshold` | `DECIMAL(10,2)` | `DEFAULT 0, CHECK ≥ 0` | Low-stock threshold |

---

**`inventory`** — Current stock levels (1:1 with ingredients).

```sql
CREATE TABLE inventory (
  id            BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  ingredient_id BIGINT UNSIGNED NOT NULL UNIQUE,
  quantity      DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (quantity >= 0),
  last_updated  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_inventory_ing FOREIGN KEY (ingredient_id) REFERENCES ingredients(id)
);
```

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `BIGINT UNSIGNED` | `PK, AUTO_INCREMENT` | Stock record identifier |
| `ingredient_id` | `BIGINT UNSIGNED` | `FK, UNIQUE` | Linked ingredient |
| `quantity` | `DECIMAL(10,2)` | `DEFAULT 0, CHECK ≥ 0` | Current stock quantity |
| `last_updated` | `TIMESTAMP` | `ON UPDATE CURRENT_TIMESTAMP` | Last stock change time |

---

**`menu_item_ingredients`** — Junction table mapping menu items to ingredients with quantities needed per unit sold.

```sql
CREATE TABLE menu_item_ingredients (
  id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  menu_item_id  BIGINT UNSIGNED NOT NULL,
  ingredient_id BIGINT UNSIGNED NOT NULL,
  qty_needed    DECIMAL(10,2) NOT NULL DEFAULT 1.00,
  CONSTRAINT fk_mii_menu FOREIGN KEY (menu_item_id)  REFERENCES menu_items(id),
  CONSTRAINT fk_mii_ing  FOREIGN KEY (ingredient_id) REFERENCES ingredients(id),
  UNIQUE KEY uq_menu_ing (menu_item_id, ingredient_id)
);
```

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `BIGINT UNSIGNED` | `PK, AUTO_INCREMENT` | Row identifier |
| `menu_item_id` | `BIGINT UNSIGNED` | `FK → menu_items.id` | Menu item reference |
| `ingredient_id` | `BIGINT UNSIGNED` | `FK → ingredients.id` | Ingredient reference |
| `qty_needed` | `DECIMAL(10,2)` | `DEFAULT 1.00` | Quantity consumed per 1 unit sold |

---

#### Module 4: Order Processing

**`orders`** — Customer orders with financial breakdown.

```sql
CREATE TABLE orders (
  id              BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  customer_name   VARCHAR(120),
  customer_type   ENUM('GENERAL','STUDENT','STAFF','LOYAL') DEFAULT 'GENERAL',
  status          ENUM('PENDING','PAID','CANCELLED') NOT NULL DEFAULT 'PENDING',
  discount_id     BIGINT UNSIGNED,
  tax_id          BIGINT UNSIGNED,
  subtotal        DECIMAL(10,2) NOT NULL DEFAULT 0,
  tax_amount      DECIMAL(10,2) NOT NULL DEFAULT 0,
  discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
  total           DECIMAL(10,2) NOT NULL DEFAULT 0,
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_orders_discount FOREIGN KEY (discount_id) REFERENCES discounts(id),
  CONSTRAINT fk_orders_tax      FOREIGN KEY (tax_id)      REFERENCES taxes(id),
  INDEX (created_at)
);
```

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `BIGINT UNSIGNED` | `PK, AUTO_INCREMENT` | Order identifier |
| `customer_name` | `VARCHAR(120)` | Nullable | Customer name (Walk-in if null) |
| `customer_type` | `ENUM(...)` | `DEFAULT 'GENERAL'` | Customer classification |
| `status` | `ENUM(...)` | `DEFAULT 'PENDING'` | Order lifecycle state |
| `discount_id` | `BIGINT UNSIGNED` | `FK → discounts.id`, Nullable | Applied discount |
| `tax_id` | `BIGINT UNSIGNED` | `FK → taxes.id`, Nullable | Applied tax |
| `subtotal` | `DECIMAL(10,2)` | `DEFAULT 0` | Sum of line totals |
| `tax_amount` | `DECIMAL(10,2)` | `DEFAULT 0` | Computed tax |
| `discount_amount` | `DECIMAL(10,2)` | `DEFAULT 0` | Computed discount |
| `total` | `DECIMAL(10,2)` | `DEFAULT 0` | Final total |
| `created_at` | `DATETIME` | `DEFAULT CURRENT_TIMESTAMP` | Order creation time |
| `updated_at` | `DATETIME` | `ON UPDATE CURRENT_TIMESTAMP` | Last modification time |

---

**`order_items`** — Line items within an order.

```sql
CREATE TABLE order_items (
  id           BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  order_id     BIGINT UNSIGNED NOT NULL,
  menu_item_id BIGINT UNSIGNED NOT NULL,
  quantity     INT NOT NULL CHECK (quantity > 0),
  unit_price   DECIMAL(10,2) NOT NULL CHECK (unit_price >= 0),
  line_total   DECIMAL(10,2) NOT NULL CHECK (line_total >= 0),
  CONSTRAINT fk_oi_order FOREIGN KEY (order_id)     REFERENCES orders(id) ON DELETE CASCADE,
  CONSTRAINT fk_oi_menu  FOREIGN KEY (menu_item_id) REFERENCES menu_items(id),
  INDEX (order_id), INDEX (menu_item_id)
);
```

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `BIGINT UNSIGNED` | `PK, AUTO_INCREMENT` | Line item identifier |
| `order_id` | `BIGINT UNSIGNED` | `FK → orders.id ON DELETE CASCADE` | Parent order |
| `menu_item_id` | `BIGINT UNSIGNED` | `FK → menu_items.id` | Ordered product |
| `quantity` | `INT` | `CHECK > 0` | Quantity ordered |
| `unit_price` | `DECIMAL(10,2)` | `CHECK ≥ 0` | Snapshot of item price |
| `line_total` | `DECIMAL(10,2)` | `CHECK ≥ 0` | quantity × unit_price |

---

**`payments`** — Payment records for orders.

```sql
CREATE TABLE payments (
  id        BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  order_id  BIGINT UNSIGNED NOT NULL,
  amount    DECIMAL(10,2) NOT NULL CHECK (amount >= 0),
  method    ENUM('CASH','CARD','MFS') NOT NULL,
  paid_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  reference VARCHAR(120),
  CONSTRAINT fk_pay_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
  INDEX (order_id)
);
```

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `BIGINT UNSIGNED` | `PK, AUTO_INCREMENT` | Payment identifier |
| `order_id` | `BIGINT UNSIGNED` | `FK → orders.id ON DELETE CASCADE` | Associated order |
| `amount` | `DECIMAL(10,2)` | `CHECK ≥ 0` | Payment amount |
| `method` | `ENUM(...)` | — | CASH, CARD, or MFS |
| `paid_at` | `DATETIME` | `DEFAULT CURRENT_TIMESTAMP` | Payment time |
| `reference` | `VARCHAR(120)` | Nullable | External reference number |

---

**`invoices`** — Auto-generated invoice records.

```sql
CREATE TABLE invoices (
  id             BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  order_id       BIGINT UNSIGNED NOT NULL UNIQUE,
  invoice_number VARCHAR(50)     NOT NULL UNIQUE,
  payment_id     BIGINT UNSIGNED,
  total          DECIMAL(10,2)   NOT NULL,
  issued_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_inv_order   FOREIGN KEY (order_id)   REFERENCES orders(id),
  CONSTRAINT fk_inv_payment FOREIGN KEY (payment_id) REFERENCES payments(id)
);
```

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `BIGINT UNSIGNED` | `PK, AUTO_INCREMENT` | Invoice identifier |
| `order_id` | `BIGINT UNSIGNED` | `FK, UNIQUE` | One invoice per order |
| `invoice_number` | `VARCHAR(50)` | `UNIQUE` | Formatted: `INV-YYYYMMDD-000XXX` |
| `payment_id` | `BIGINT UNSIGNED` | `FK → payments.id`, Nullable | Associated payment |
| `total` | `DECIMAL(10,2)` | — | Invoice total amount |
| `issued_at` | `DATETIME` | `DEFAULT CURRENT_TIMESTAMP` | Issue timestamp |

---

#### Module 5: Supply Chain

**`suppliers`** — Supplier contact information.

```sql
CREATE TABLE suppliers (
  id      BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  name    VARCHAR(120) NOT NULL UNIQUE,
  contact VARCHAR(120),
  phone   VARCHAR(20),
  email   VARCHAR(120)
);
```

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `BIGINT UNSIGNED` | `PK, AUTO_INCREMENT` | Supplier identifier |
| `name` | `VARCHAR(120)` | `NOT NULL, UNIQUE` | Supplier name |
| `contact` | `VARCHAR(120)` | Nullable | Contact person |
| `phone` | `VARCHAR(20)` | Nullable | Phone number |
| `email` | `VARCHAR(120)` | Nullable | Email address |

---

**`purchases`** — Purchase records from suppliers.

```sql
CREATE TABLE purchases (
  id            BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  supplier_id   BIGINT UNSIGNED NOT NULL,
  ingredient_id BIGINT UNSIGNED NOT NULL,
  quantity      DECIMAL(10,2) NOT NULL CHECK (quantity > 0),
  cost          DECIMAL(10,2) NOT NULL CHECK (cost >= 0),
  purchased_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_purchase_supplier FOREIGN KEY (supplier_id)   REFERENCES suppliers(id),
  CONSTRAINT fk_purchase_ing      FOREIGN KEY (ingredient_id) REFERENCES ingredients(id),
  INDEX (supplier_id), INDEX (ingredient_id)
);
```

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `BIGINT UNSIGNED` | `PK, AUTO_INCREMENT` | Purchase identifier |
| `supplier_id` | `BIGINT UNSIGNED` | `FK → suppliers.id` | Supplier reference |
| `ingredient_id` | `BIGINT UNSIGNED` | `FK → ingredients.id` | Purchased ingredient |
| `quantity` | `DECIMAL(10,2)` | `CHECK > 0` | Quantity purchased |
| `cost` | `DECIMAL(10,2)` | `CHECK ≥ 0` | Total cost |
| `purchased_at` | `DATETIME` | `DEFAULT CURRENT_TIMESTAMP` | Purchase timestamp |

---

#### Module 6: Workforce Management

**`employees`** — Employee records optionally linked to user accounts.

```sql
CREATE TABLE employees (
  id        BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  user_id   BIGINT UNSIGNED UNIQUE,
  position  VARCHAR(80) NOT NULL,
  hire_date DATE NOT NULL,
  salary    DECIMAL(10,2) NOT NULL,
  CONSTRAINT fk_employees_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);
```

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `BIGINT UNSIGNED` | `PK, AUTO_INCREMENT` | Employee identifier |
| `user_id` | `BIGINT UNSIGNED` | `FK, UNIQUE, ON DELETE SET NULL` | Linked user account |
| `position` | `VARCHAR(80)` | `NOT NULL` | Job position |
| `hire_date` | `DATE` | `NOT NULL` | Date of hire |
| `salary` | `DECIMAL(10,2)` | `NOT NULL` | Monthly salary |

---

**`shifts`** — Named shift definitions.

```sql
CREATE TABLE shifts (
  id         BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  name       VARCHAR(50) NOT NULL UNIQUE,
  start_time TIME NOT NULL,
  end_time   TIME NOT NULL
);
```

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `BIGINT UNSIGNED` | `PK, AUTO_INCREMENT` | Shift identifier |
| `name` | `VARCHAR(50)` | `NOT NULL, UNIQUE` | Shift name |
| `start_time` | `TIME` | `NOT NULL` | Shift start |
| `end_time` | `TIME` | `NOT NULL` | Shift end |

---

**`attendance`** — Daily attendance records.

```sql
CREATE TABLE attendance (
  id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  employee_id BIGINT UNSIGNED NOT NULL,
  shift_id    BIGINT UNSIGNED NOT NULL,
  work_date   DATE NOT NULL,
  check_in    DATETIME,
  check_out   DATETIME,
  status      ENUM('PRESENT','ABSENT','LATE') NOT NULL DEFAULT 'PRESENT',
  CONSTRAINT fk_att_emp   FOREIGN KEY (employee_id) REFERENCES employees(id),
  CONSTRAINT fk_att_shift FOREIGN KEY (shift_id)    REFERENCES shifts(id),
  UNIQUE (employee_id, work_date)
);
```

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `BIGINT UNSIGNED` | `PK, AUTO_INCREMENT` | Attendance record identifier |
| `employee_id` | `BIGINT UNSIGNED` | `FK → employees.id` | Employee reference |
| `shift_id` | `BIGINT UNSIGNED` | `FK → shifts.id` | Assigned shift |
| `work_date` | `DATE` | `NOT NULL` | Working date |
| `check_in` | `DATETIME` | Nullable | Check-in timestamp |
| `check_out` | `DATETIME` | Nullable | Check-out timestamp |
| `status` | `ENUM(...)` | `DEFAULT 'PRESENT'` | Attendance status |

---

#### Module 7: Discounts & Taxes

**`discounts`** — Discount definitions scoped to customer types.

```sql
CREATE TABLE discounts (
  id         BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  name       VARCHAR(80)   NOT NULL UNIQUE,
  type       ENUM('PERCENT','FLAT') NOT NULL,
  value      DECIMAL(10,2) NOT NULL CHECK (value >= 0),
  applies_to ENUM('GENERAL','STUDENT','STAFF','LOYAL') NOT NULL
);
```

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `BIGINT UNSIGNED` | `PK, AUTO_INCREMENT` | Discount identifier |
| `name` | `VARCHAR(80)` | `NOT NULL, UNIQUE` | Discount name |
| `type` | `ENUM(...)` | — | PERCENT or FLAT |
| `value` | `DECIMAL(10,2)` | `CHECK ≥ 0` | Discount value |
| `applies_to` | `ENUM(...)` | — | Target customer type |

---

**`taxes`** — Tax definitions (percentage-based).

```sql
CREATE TABLE taxes (
  id   BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(80)  NOT NULL UNIQUE,
  rate DECIMAL(5,2) NOT NULL CHECK (rate >= 0)
);
```

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `BIGINT UNSIGNED` | `PK, AUTO_INCREMENT` | Tax identifier |
| `name` | `VARCHAR(80)` | `NOT NULL, UNIQUE` | Tax name |
| `rate` | `DECIMAL(5,2)` | `CHECK ≥ 0` | Tax percentage |

---

#### Module 8: Audit & Reporting

**`audit_logs`** — System-wide audit trail.

```sql
CREATE TABLE audit_logs (
  id         BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  user_id    BIGINT UNSIGNED,
  action     VARCHAR(120) NOT NULL,
  entity     VARCHAR(80),
  entity_id  BIGINT,
  details    TEXT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);
```

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `BIGINT UNSIGNED` | `PK, AUTO_INCREMENT` | Log entry identifier |
| `user_id` | `BIGINT UNSIGNED` | `FK → users.id ON DELETE SET NULL` | Acting user (nullable) |
| `action` | `VARCHAR(120)` | `NOT NULL` | Action type (CREATE_USER, PAYMENT, etc.) |
| `entity` | `VARCHAR(80)` | Nullable | Target entity type |
| `entity_id` | `BIGINT` | Nullable | Target entity identifier |
| `details` | `TEXT` | Nullable | Human-readable description |
| `created_at` | `DATETIME` | `DEFAULT CURRENT_TIMESTAMP` | Log timestamp |

---

### Foreign Key Relationship Map

```
roles ──(1:M)──> user_roles <──(M:1)── users
users ──(1:1)──> employees
employees ──(1:M)──> attendance <──(M:1)── shifts
categories ──(1:M)──> menu_items
menu_items ──(1:M)──> menu_item_ingredients <──(M:1)── ingredients
ingredients ──(1:1)──> inventory
suppliers ──(1:M)──> purchases <──(M:1)── ingredients
discounts ──(1:M)──> orders
taxes ──(1:M)──> orders
orders ──(1:M)──> order_items <──(M:1)── menu_items
orders ──(1:M)──> payments
orders ──(1:1)──> invoices <──(1:1)── payments
users ──(1:M)──> audit_logs
```

### Normalization

All tables are in **Third Normal Form (3NF)**:

- **1NF:** Every column contains atomic values. ENUMs are used for fixed domains (e.g., `ENUM('PENDING','PAID','CANCELLED')`).
- **2NF:** No partial dependencies — all non-key columns depend on the entire primary key. Junction tables (`user_roles`, `menu_item_ingredients`) use composite primary keys.
- **3NF:** No transitive dependencies — for example, `orders.discount_amount` is computed from `orders.subtotal` and `discounts.value` by a stored procedure, not stored redundantly without computation.

### Performance Indexes

```sql
CREATE INDEX idx_audit_logs_created    ON audit_logs(created_at);
CREATE INDEX idx_orders_created_status ON orders(created_at, status);
```

These are created conditionally via a disposable stored procedure to avoid errors if they already exist:

```sql
CREATE PROCEDURE create_index_if_not_exists()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = DATABASE() AND table_name = 'audit_logs'
                   AND index_name = 'idx_audit_logs_created') THEN
        CREATE INDEX idx_audit_logs_created ON audit_logs(created_at);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = DATABASE() AND table_name = 'orders'
                   AND index_name = 'idx_orders_created_status') THEN
        CREATE INDEX idx_orders_created_status ON orders(created_at, status);
    END IF;
END;
```

---

## 🗄️ Procedural SQL Summary

### Functions

The database uses **3 user-defined functions** for financial computations that are called from stored procedures and application code.

---

#### 1. `calculate_tax()`

Computes the tax amount given a subtotal and tax ID. Looks up the tax rate from the `taxes` table and returns `amount × rate / 100`.

```sql
CREATE FUNCTION calculate_tax(p_amount DECIMAL(10,2), p_tax_id BIGINT UNSIGNED)
RETURNS DECIMAL(10,2)
DETERMINISTIC
BEGIN
  DECLARE v_rate DECIMAL(5,2);
  SELECT rate INTO v_rate FROM taxes WHERE id = p_tax_id;
  RETURN IFNULL(p_amount * v_rate / 100, 0);
END;
```

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `p_amount` | `DECIMAL(10,2)` | Subtotal after discount |
| `p_tax_id` | `BIGINT UNSIGNED` | Tax definition ID |

**Returns:** `DECIMAL(10,2)` — computed tax amount, or `0` if tax ID is NULL.

**Called by:** `create_order_procedure`

---

#### 2. `calculate_discount()`

Applies a discount to a subtotal based on the customer type. Supports both PERCENT and FLAT discount types. FLAT discounts are capped at the subtotal amount using `LEAST()`.

```sql
CREATE FUNCTION calculate_discount(
  p_amount        DECIMAL(10,2),
  p_discount_id   BIGINT UNSIGNED,
  p_customer_type ENUM('GENERAL','STUDENT','STAFF','LOYAL')
)
RETURNS DECIMAL(10,2)
DETERMINISTIC
BEGIN
  DECLARE v_type  ENUM('PERCENT','FLAT');
  DECLARE v_value DECIMAL(10,2);
  DECLARE v_apply ENUM('GENERAL','STUDENT','STAFF','LOYAL');

  SELECT type, value, applies_to INTO v_type, v_value, v_apply
  FROM discounts WHERE id = p_discount_id;

  IF v_type IS NULL OR (v_apply <> p_customer_type AND v_apply <> 'GENERAL') THEN
    RETURN 0;
  END IF;

  RETURN IF(v_type = 'PERCENT', p_amount * v_value / 100, LEAST(v_value, p_amount));
END;
```

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `p_amount` | `DECIMAL(10,2)` | Order subtotal |
| `p_discount_id` | `BIGINT UNSIGNED` | Discount definition ID |
| `p_customer_type` | `ENUM(...)` | Customer classification |

**Returns:** `DECIMAL(10,2)` — computed discount amount, or `0` if discount is NULL or not applicable.

**Logic:**
1. Fetches the discount type, value, and target customer type.
2. If the discount doesn't exist or doesn't apply to this customer type (and isn't GENERAL), returns 0.
3. For PERCENT: returns `amount × value / 100`.
4. For FLAT: returns `MIN(value, amount)` to prevent negative totals.

---

#### 3. `get_total_order_price()`

Sums all line totals for a given order. Used by `create_order_procedure` to compute the subtotal.

```sql
CREATE FUNCTION get_total_order_price(p_order_id BIGINT UNSIGNED)
RETURNS DECIMAL(10,2)
DETERMINISTIC
BEGIN
  DECLARE v_total DECIMAL(10,2);
  SELECT SUM(line_total) INTO v_total FROM order_items WHERE order_id = p_order_id;
  RETURN IFNULL(v_total, 0);
END;
```

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `p_order_id` | `BIGINT UNSIGNED` | Order identifier |

**Returns:** `DECIMAL(10,2)` — sum of all `order_items.line_total` for the order, or `0` if no items exist.

---

### Stored Procedures

The database uses **8 stored procedures** for transactional operations, report generation, and utility tasks.

---

#### 1. `create_order_procedure`

Called after inserting order rows and order items. Computes the financial breakdown (subtotal, discount, tax, total) using the three functions and updates the order record.

```sql
CREATE PROCEDURE create_order_procedure(
  IN p_customer_name VARCHAR(120),
  IN p_customer_type ENUM('GENERAL','STUDENT','STAFF','LOYAL'),
  IN p_tax_id        BIGINT UNSIGNED,
  IN p_discount_id   BIGINT UNSIGNED
)
BEGIN
  DECLARE v_order_id BIGINT UNSIGNED;
  DECLARE v_subtotal DECIMAL(10,2);
  DECLARE v_discount DECIMAL(10,2);
  DECLARE v_tax      DECIMAL(10,2);
  DECLARE v_total    DECIMAL(10,2);

  SET v_order_id = LAST_INSERT_ID();
  SET v_subtotal = get_total_order_price(v_order_id);
  SET v_discount = calculate_discount(v_subtotal, p_discount_id, p_customer_type);
  SET v_tax      = calculate_tax(v_subtotal - v_discount, p_tax_id);
  SET v_total    = v_subtotal - v_discount + v_tax;

  UPDATE orders
    SET subtotal        = v_subtotal,
        discount_amount = v_discount,
        tax_amount      = v_tax,
        total           = v_total
  WHERE id = v_order_id;
END;
```

**Execution chain:**
1. Gets the order ID from `LAST_INSERT_ID()` (set by the preceding `INSERT` in the Java transaction).
2. Calls `get_total_order_price()` to sum line totals → `v_subtotal`.
3. Calls `calculate_discount()` to compute the discount → `v_discount`.
4. Calls `calculate_tax()` on `(subtotal - discount)` → `v_tax`.
5. Computes `v_total = v_subtotal - v_discount + v_tax`.
6. Updates the `orders` row with all computed values.

**Java invocation:**

```java
try (CallableStatement cs = conn.prepareCall("{call create_order_procedure(?,?,?,?)}")) {
    cs.setString(1, order.getCustomerName());
    cs.setString(2, order.getCustomerType());
    if (order.getTaxId() == null) cs.setNull(3, java.sql.Types.BIGINT);
    else cs.setLong(3, order.getTaxId());
    if (order.getDiscountId() == null) cs.setNull(4, java.sql.Types.BIGINT);
    else cs.setLong(4, order.getDiscountId());
    cs.execute();
}
```

---

#### 2. `generate_invoice_procedure`

Creates an invoice record with a formatted invoice number. Called after payment recording.

```sql
CREATE PROCEDURE generate_invoice_procedure(
  IN p_order_id   BIGINT UNSIGNED,
  IN p_payment_id BIGINT UNSIGNED
)
BEGIN
  DECLARE v_invoice_no VARCHAR(50);
  DECLARE v_total      DECIMAL(10,2);

  SELECT total INTO v_total FROM orders WHERE id = p_order_id;
  SET v_invoice_no = CONCAT('INV-', DATE_FORMAT(NOW(), '%Y%m%d'), '-', LPAD(p_order_id, 6, '0'));

  INSERT INTO invoices (order_id, invoice_number, payment_id, total)
  VALUES (p_order_id, v_invoice_no, p_payment_id, v_total);
END;
```

**Invoice number format:** `INV-20250709-000042` (for order #42 on July 9, 2025).

**Key functions used:**
- `DATE_FORMAT(NOW(), '%Y%m%d')` — formats current date as YYYYMMDD
- `LPAD(p_order_id, 6, '0')` — zero-pads the order ID to 6 digits

---

#### 3. `daily_sales_report_procedure`

Returns aggregated sales metrics for a single date.

```sql
CREATE PROCEDURE daily_sales_report_procedure(IN p_date DATE)
BEGIN
  SELECT 
    COUNT(DISTINCT o.id) AS orders_count,
    SUM(oi.quantity)      AS items_sold,
    SUM(o.total)          AS revenue
  FROM orders o
  JOIN order_items oi ON oi.order_id = o.id
  WHERE DATE(o.created_at) = p_date AND o.status <> 'CANCELLED';
END;
```

**Output columns:** `orders_count`, `items_sold`, `revenue`

---

#### 4. `record_purchase_procedure`

Atomically inserts a purchase record and adjusts inventory. Used by `PurchaseController`.

```sql
CREATE PROCEDURE record_purchase_procedure(
    IN p_supplier_id   BIGINT UNSIGNED,
    IN p_ingredient_id BIGINT UNSIGNED,
    IN p_quantity       DECIMAL(10,2),
    IN p_cost           DECIMAL(10,2)
)
BEGIN
    INSERT INTO purchases (supplier_id, ingredient_id, quantity, cost)
    VALUES (p_supplier_id, p_ingredient_id, p_quantity, p_cost);

    UPDATE inventory SET quantity = quantity + p_quantity
    WHERE ingredient_id = p_ingredient_id;
END;
```

**Side effect:** The `UPDATE inventory` statement fires `trg_inventory_after_update`, which writes an audit log entry for the stock change.

---

#### 5. `cancel_order_procedure`

Cancels an order, reverses all inventory deductions, and writes an audit log. This is the most complex procedure in the system.

```sql
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
    VALUES (NULL, 'CANCEL_ORDER', 'orders', p_order_id,
            CONCAT('Order ', p_order_id, ' cancelled'));
END;
```

**Operations performed:**
1. **Validation:** Checks if the order is already cancelled; raises `SQLSTATE '45000'` error if so.
2. **Inventory reversal:** Joins `order_items` → `menu_items` → `ingredients` → `inventory` and adds back the ordered quantities.
3. **Status update:** Sets `orders.status = 'CANCELLED'`.
4. **Audit logging:** Inserts a `CANCEL_ORDER` entry into `audit_logs`.

---

#### 6. `range_sales_report_procedure`

Returns a daily breakdown of sales over a date range. Used by the Reports module's "Daily Sales" report.

```sql
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
END;
```

**Output columns:** `sale_date`, `orders_count`, `items_sold`, `revenue`

---

#### 7. `monthly_summary_procedure`

Returns monthly aggregates including cancelled order count. Used by the "Monthly Summary" report.

```sql
CREATE PROCEDURE monthly_summary_procedure(IN p_year INT, IN p_month INT)
BEGIN
    SELECT
        COUNT(DISTINCT o.id) AS total_orders,
        COALESCE(SUM(oi.quantity), 0) AS total_items_sold,
        COALESCE(SUM(o.total), 0) AS total_revenue,
        COALESCE(AVG(o.total), 0) AS avg_order_value,
        (SELECT COUNT(*) FROM orders
         WHERE YEAR(created_at) = p_year AND MONTH(created_at) = p_month
         AND status = 'CANCELLED') AS cancelled_orders
    FROM orders o
    LEFT JOIN order_items oi ON oi.order_id = o.id
    WHERE YEAR(o.created_at) = p_year AND MONTH(o.created_at) = p_month
      AND o.status <> 'CANCELLED';
END;
```

**Output columns:** `total_orders`, `total_items_sold`, `total_revenue`, `avg_order_value`, `cancelled_orders`

---

#### 8. `create_index_if_not_exists`

A disposable utility procedure that creates performance indexes conditionally. It queries `information_schema.statistics` to check if an index already exists before creating it.

```sql
CREATE PROCEDURE create_index_if_not_exists()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = DATABASE() AND table_name = 'audit_logs'
                   AND index_name = 'idx_audit_logs_created') THEN
        CREATE INDEX idx_audit_logs_created ON audit_logs(created_at);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = DATABASE() AND table_name = 'orders'
                   AND index_name = 'idx_orders_created_status') THEN
        CREATE INDEX idx_orders_created_status ON orders(created_at, status);
    END IF;
END;
```

This procedure is called immediately after creation and then dropped:

```sql
CALL create_index_if_not_exists();
DROP PROCEDURE IF EXISTS create_index_if_not_exists;
```

---

### Triggers

The database uses **4 triggers** for automated side effects on data mutations.

---

#### 1. `trg_users_before_insert`

**Event:** `BEFORE INSERT` on `users`

**Purpose:** Validates the password hash length and writes an audit log entry for user creation.

```sql
CREATE TRIGGER trg_users_before_insert
BEFORE INSERT ON users
FOR EACH ROW
BEGIN
  IF CHAR_LENGTH(NEW.password_hash) < 20 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Password hash invalid';
  END IF;
  INSERT INTO audit_logs (user_id, action, entity, entity_id, details)
  VALUES (NEW.id, 'CREATE_USER', 'users', NULL, CONCAT('Created user ', NEW.username));
END;
```

**Behavior:**
- If `password_hash` is shorter than 20 characters, the insert is rejected with a `SQLSTATE 45000` error, preventing accidental plain-text password storage.
- On successful validation, an audit log entry is created with the action `CREATE_USER`.

---

#### 2. `trg_order_items_after_insert`

**Event:** `AFTER INSERT` on `order_items`

**Purpose:** Automatically deducts ingredient stock from inventory when an order item is created. Uses the `menu_item_ingredients` junction table to determine which ingredients and how much of each to deduct.

```sql
CREATE TRIGGER trg_order_items_after_insert
AFTER INSERT ON order_items FOR EACH ROW
BEGIN
  UPDATE inventory inv
  JOIN menu_item_ingredients mii ON mii.ingredient_id = inv.ingredient_id
  SET inv.quantity = inv.quantity - (mii.qty_needed * NEW.quantity)
  WHERE mii.menu_item_id = NEW.menu_item_id
    AND inv.quantity >= (mii.qty_needed * NEW.quantity);
END;
```

**Behavior:**
- Joins `inventory` with `menu_item_ingredients` to find all ingredients needed for the ordered menu item.
- Deducts `qty_needed × ordered_quantity` from each matching inventory row.
- The `WHERE inv.quantity >= (mii.qty_needed * NEW.quantity)` clause prevents negative stock levels.
- This trigger fires for every `INSERT` into `order_items`, which happens during the checkout batch insert via `OrderDao.insertItems()`.

**Example:** Ordering 2 Lattes triggers deductions of:
- Coffee Beans: 2 × 18g = 36g
- Milk: 2 × 200ml = 400ml

---

#### 3. `trg_payments_after_insert`

**Event:** `AFTER INSERT` on `payments`

**Purpose:** Automatically marks the associated order as PAID and writes an audit log entry.

```sql
CREATE TRIGGER trg_payments_after_insert
AFTER INSERT ON payments
FOR EACH ROW
BEGIN
  UPDATE orders SET status = 'PAID' WHERE id = NEW.order_id;
  INSERT INTO audit_logs (user_id, action, entity, entity_id, details)
  VALUES (NULL, 'PAYMENT', 'orders', NEW.order_id,
          CONCAT('Payment ', NEW.id, ' recorded'));
END;
```

**Behavior:**
- Changes the order status from `PENDING` to `PAID`.
- Inserts an audit log entry with the action `PAYMENT`.
- This trigger fires during `OrderService.recordPaymentAndInvoice()` when the `PaymentDao.insert()` call completes.

---

#### 4. `trg_inventory_after_update`

**Event:** `AFTER UPDATE` on `inventory`

**Purpose:** Logs all stock quantity changes to the audit trail for accountability.

```sql
CREATE TRIGGER trg_inventory_after_update
AFTER UPDATE ON inventory
FOR EACH ROW
BEGIN
    IF OLD.quantity <> NEW.quantity THEN
        INSERT INTO audit_logs (user_id, action, entity, entity_id, details)
        VALUES (NULL, 'STOCK_CHANGE', 'inventory', NEW.id,
                CONCAT('Ingredient ', NEW.ingredient_id, ': ', OLD.quantity, ' -> ', NEW.quantity));
    END IF;
END;
```

**Behavior:**
- Only fires when the `quantity` column actually changes (guard: `OLD.quantity <> NEW.quantity`).
- Records the old and new quantity values in the audit log details.
- This trigger fires on three occasions:
  1. When `trg_order_items_after_insert` deducts stock after an order.
  2. When `record_purchase_procedure` adds stock after a purchase.
  3. When `cancel_order_procedure` restores stock after order cancellation.

---

### Views

The database defines **7 views** for analytics, reporting, and data presentation layers.

---

#### 1. `pos_menu_view`

Active menu items joined with category names for the POS module.

```sql
CREATE OR REPLACE VIEW pos_menu_view AS
SELECT mi.id, mi.category_id, c.name AS category_name,
       mi.name, mi.description, mi.price, mi.is_active
FROM menu_items mi
JOIN categories c ON c.id = mi.category_id
WHERE mi.is_active = 1
ORDER BY c.name, mi.name;
```

**Columns returned:** `id`, `category_id`, `category_name`, `name`, `description`, `price`, `is_active`

---

#### 2. `dashboard_stats_view`

Today's quick KPI metrics aggregated from orders, employees, and inventory tables.

```sql
CREATE OR REPLACE VIEW dashboard_stats_view AS
SELECT
    (SELECT COUNT(*) FROM orders WHERE DATE(created_at) = CURDATE() AND status <> 'CANCELLED') AS orders_today,
    (SELECT COALESCE(SUM(total), 0) FROM orders WHERE DATE(created_at) = CURDATE() AND status <> 'CANCELLED') AS revenue_today,
    (SELECT COALESCE(AVG(total), 0) FROM orders WHERE DATE(created_at) = CURDATE() AND status <> 'CANCELLED') AS avg_order_value,
    (SELECT COUNT(*) FROM orders WHERE status = 'PENDING') AS pending_orders,
    (SELECT COUNT(*) FROM employees WHERE status = 'Active') AS active_employees,
    (SELECT COUNT(*) FROM inventory inv JOIN ingredients ing ON ing.id = inv.ingredient_id
     WHERE inv.quantity <= ing.min_threshold) AS low_stock_items;
```

**Columns returned:** `orders_today`, `revenue_today`, `avg_order_value`, `pending_orders`, `active_employees`, `low_stock_items`

---

#### 3. `top_selling_items_view`

Ranks menu items by total quantity sold across all non-cancelled orders.

```sql
CREATE OR REPLACE VIEW top_selling_items_view AS
SELECT mi.name AS item_name, c.name AS category_name,
       SUM(oi.quantity) AS total_qty_sold,
       SUM(oi.line_total) AS total_revenue,
       COUNT(DISTINCT oi.order_id) AS order_count
FROM order_items oi
JOIN menu_items mi ON mi.id = oi.menu_item_id
JOIN categories c  ON c.id = mi.category_id
JOIN orders o      ON o.id = oi.order_id
WHERE o.status <> 'CANCELLED'
GROUP BY mi.id, mi.name, c.name
ORDER BY total_qty_sold DESC;
```

**Columns returned:** `item_name`, `category_name`, `total_qty_sold`, `total_revenue`, `order_count`

---

#### 4. `supplier_summary_view`

Aggregates purchase history per supplier.

```sql
CREATE OR REPLACE VIEW supplier_summary_view AS
SELECT s.id, s.name, s.contact, s.phone, s.email,
       COUNT(p.id) AS total_purchases,
       COALESCE(SUM(p.cost), 0) AS total_spent,
       MAX(p.purchased_at) AS last_purchase_date
FROM suppliers s
LEFT JOIN purchases p ON p.supplier_id = s.id
GROUP BY s.id, s.name, s.contact, s.phone, s.email
ORDER BY s.name;
```

**Columns returned:** `id`, `name`, `contact`, `phone`, `email`, `total_purchases`, `total_spent`, `last_purchase_date`

---

#### 5. `revenue_by_category_view`

Revenue aggregated by menu category from non-cancelled orders.

```sql
CREATE OR REPLACE VIEW revenue_by_category_view AS
SELECT c.name AS category_name,
       SUM(oi.quantity) AS items_sold,
       SUM(oi.line_total) AS revenue
FROM order_items oi
JOIN menu_items mi ON mi.id = oi.menu_item_id
JOIN categories c  ON c.id = mi.category_id
JOIN orders o      ON o.id = oi.order_id
WHERE o.status <> 'CANCELLED'
GROUP BY c.id, c.name
ORDER BY revenue DESC;
```

**Columns returned:** `category_name`, `items_sold`, `revenue`

---

#### 6. `payment_method_breakdown_view`

Counts and sums payments by method (CASH, CARD, MFS) for non-cancelled orders.

```sql
CREATE OR REPLACE VIEW payment_method_breakdown_view AS
SELECT p.method,
       COUNT(*) AS payment_count,
       SUM(p.amount) AS total_amount
FROM payments p
JOIN orders o ON o.id = p.order_id
WHERE o.status <> 'CANCELLED'
GROUP BY p.method
ORDER BY total_amount DESC;
```

**Columns returned:** `method`, `payment_count`, `total_amount`

---

#### 7. `order_history_view`

Complete order history with discount names, tax names, payment methods, and invoice numbers joined from their respective tables.

```sql
CREATE OR REPLACE VIEW order_history_view AS
SELECT o.id, o.customer_name, o.customer_type, o.status,
       o.subtotal, o.discount_amount, o.tax_amount, o.total,
       o.created_at, o.updated_at,
       d.name AS discount_name, t.name AS tax_name,
       p.method AS payment_method,
       inv.invoice_number
FROM orders o
LEFT JOIN discounts d  ON d.id = o.discount_id
LEFT JOIN taxes t      ON t.id = o.tax_id
LEFT JOIN payments p   ON p.order_id = o.id
LEFT JOIN invoices inv ON inv.order_id = o.id
ORDER BY o.created_at DESC;
```

**Columns returned:** `id`, `customer_name`, `customer_type`, `status`, `subtotal`, `discount_amount`, `tax_amount`, `total`, `created_at`, `updated_at`, `discount_name`, `tax_name`, `payment_method`, `invoice_number`

---

## 📁 Project Structure

```
├── src/
│   ├── App.java                          # JavaFX entry point
│   └── com/databrew/cafe/
│       ├── controller/                   # 15 FXML controllers
│       │   ├── AuditLogController.java
│       │   ├── DashboardController.java
│       │   ├── DiscountController.java
│       │   ├── EmployeeController.java
│       │   ├── InventoryController.java
│       │   ├── LoginController.java
│       │   ├── MenuController.java
│       │   ├── OrderHistoryController.java
│       │   ├── PosController.java
│       │   ├── PurchaseController.java
│       │   ├── ReportsController.java
│       │   ├── ShiftManagementController.java
│       │   ├── SupplierController.java
│       │   ├── TaxController.java
│       │   └── UserManagementController.java
│       ├── dao/                          # 16 DAO classes
│       │   ├── AttendanceDao.java
│       │   ├── AuditLogDao.java
│       │   ├── CategoryDao.java
│       │   ├── DiscountDao.java
│       │   ├── EmployeeDao.java
│       │   ├── InventoryDao.java
│       │   ├── InvoiceDao.java
│       │   ├── MenuDao.java
│       │   ├── OrderDao.java
│       │   ├── PaymentDao.java
│       │   ├── PurchaseDao.java
│       │   ├── RoleDao.java
│       │   ├── ShiftDao.java
│       │   ├── SupplierDao.java
│       │   ├── TaxDao.java
│       │   └── UserDao.java
│       ├── model/                        # 18 model classes
│       │   ├── Attendance.java
│       │   ├── AuditLog.java
│       │   ├── Category.java
│       │   ├── Discount.java
│       │   ├── Employee.java
│       │   ├── Ingredient.java
│       │   ├── InventoryItem.java
│       │   ├── Invoice.java
│       │   ├── MenuItem.java
│       │   ├── Order.java
│       │   ├── OrderItem.java
│       │   ├── Payment.java
│       │   ├── Purchase.java
│       │   ├── Role.java
│       │   ├── Shift.java
│       │   ├── Supplier.java
│       │   ├── Tax.java
│       │   └── User.java
│       ├── service/                      # 3 Service classes
│       │   ├── AuthService.java          # Authentication logic
│       │   ├── InventoryService.java     # Stock operations
│       │   └── OrderService.java         # Order + payment transactions
│       └── util/                         # 2 Utility classes
│           ├── DBConnection.java         # JDBC connection factory
│           └── PasswordUtil.java         # SHA-256 hashing
├── resources/
│   ├── schema.sql                        # Core schema + functions + procedures + triggers
│   ├── extend_schema.sql                 # Views, additional procedures, indexes
│   ├── fix_trigger.sql                   # Fixed inventory trigger + junction table
│   ├── restore_trigger.sql               # Payment trigger restore
│   ├── seed_data.sql                     # Bulk seed data
│   ├── seed_users.sql / seed_remaining.sql
│   ├── css/theme.css                     # JavaFX stylesheet
│   └── fxml/                             # 15 FXML view files
│       ├── AuditLogView.fxml
│       ├── DashboardView.fxml
│       ├── DiscountView.fxml
│       ├── EmployeeView.fxml
│       ├── InventoryView.fxml
│       ├── LoginView.fxml
│       ├── MenuView.fxml
│       ├── OrderHistoryView.fxml
│       ├── PosView.fxml
│       ├── PurchaseView.fxml
│       ├── ReportsView.fxml
│       ├── ShiftManagementView.fxml
│       ├── SupplierView.fxml
│       ├── TaxView.fxml
│       └── UserManagementView.fxml
├── lib/
│   └── mysql-connector-j-9.1.0.jar
└── bin/                                  # Compiled classes
```

---

## 🏗️ Architecture

### Layered Design

```
┌──────────────────┐
│   FXML Views     │  ← 15 .fxml files + CSS theme
├──────────────────┤
│  Controllers     │  ← 15 JavaFX controllers (UI event handling)
├──────────────────┤
│   Services       │  ← 3 service classes (business logic, transactions)
├──────────────────┤
│     DAOs         │  ← 16 DAO classes (raw JDBC SQL operations)
├──────────────────┤
│    Utilities     │  ← DBConnection (JDBC), PasswordUtil (SHA-256)
├──────────────────┤
│  MySQL 8.0+      │  ← 20 tables, 3 functions, 8 procedures, 4 triggers, 7 views
└──────────────────┘
```

### Data Flow: Order Checkout

```
PosController.onCheckout()
  │
  ├─ OrderService.createOrderWithItems(order, items)
  │    │  conn.setAutoCommit(false)
  │    ├─ OrderDao.insertOrder(conn, order)         → INSERT INTO orders
  │    ├─ OrderDao.insertItems(conn, orderId, items) → INSERT INTO order_items (batch)
  │    │    └─ [TRIGGER] trg_order_items_after_insert → UPDATE inventory (deduct stock)
  │    │         └─ [TRIGGER] trg_inventory_after_update → INSERT INTO audit_logs
  │    ├─ CallableStatement: create_order_procedure  → Compute subtotal/discount/tax/total
  │    │    ├─ get_total_order_price()
  │    │    ├─ calculate_discount()
  │    │    └─ calculate_tax()
  │    └─ conn.commit()
  │
  ├─ OrderService.recordPaymentAndInvoice(orderId, total, method)
  │    │  conn.setAutoCommit(false)
  │    ├─ PaymentDao.insert(conn, payment)           → INSERT INTO payments
  │    │    └─ [TRIGGER] trg_payments_after_insert   → UPDATE orders SET status='PAID'
  │    │         └─ INSERT INTO audit_logs
  │    ├─ CallableStatement: generate_invoice_procedure → INSERT INTO invoices
  │    └─ conn.commit()
  │
  └─ showReceiptDialog()
```

### JDBC Connection Utility

```java
public final class DBConnection {
    private static final String URL  = "jdbc:mysql://localhost:3306/cafedb?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "MyNewPass";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("MySQL driver not found", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
```

---

## 🚀 Getting Started

### Prerequisites
- **Java JDK 17+**
- **JavaFX SDK 24**
- **MySQL 8.0+**

### Database Setup
```bash
mysql -u root -p < resources/schema.sql
mysql -u root -p cafedb < resources/fix_trigger.sql
mysql -u root -p cafedb < resources/extend_schema.sql
mysql -u root -p cafedb < resources/seed_users.sql
mysql -u root -p cafedb < resources/seed_data.sql
mysql -u root -p cafedb < resources/seed_remaining.sql
```

### Compile & Run
```powershell
# Compile
javac --module-path "C:\Java\javafx-sdk-24.0.2\lib" --add-modules javafx.controls,javafx.fxml -d bin -cp "lib\mysql-connector-j-9.1.0.jar" (Get-ChildItem -Path src -Recurse -Filter *.java | ForEach-Object { $_.FullName })

# Run
java --module-path "C:\Java\javafx-sdk-24.0.2\lib" --add-modules javafx.controls,javafx.fxml --enable-native-access=javafx.graphics -cp "$PWD\bin;$PWD\resources;$PWD\lib\mysql-connector-j-9.1.0.jar" com.databrew.cafe.App
```

> **Note:** Update the JavaFX SDK path to match your local installation.

---



