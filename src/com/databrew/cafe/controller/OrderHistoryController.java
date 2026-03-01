package com.databrew.cafe.controller;

import com.databrew.cafe.dao.OrderDao;
import com.databrew.cafe.model.Order;
import com.databrew.cafe.model.OrderItem;
import com.databrew.cafe.util.DBConnection;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class OrderHistoryController {

    @FXML
    private TableView<Order> orderTable;
    @FXML
    private TableColumn<Order, Number> colId;
    @FXML
    private TableColumn<Order, String> colCustomer;
    @FXML
    private TableColumn<Order, String> colType;
    @FXML
    private TableColumn<Order, String> colStatus;
    @FXML
    private TableColumn<Order, Number> colSubtotal;
    @FXML
    private TableColumn<Order, Number> colDiscount;
    @FXML
    private TableColumn<Order, Number> colTax;
    @FXML
    private TableColumn<Order, Number> colTotal;
    @FXML
    private TableColumn<Order, String> colPayment;
    @FXML
    private TableColumn<Order, String> colInvoice;
    @FXML
    private TableColumn<Order, String> colDate;

    @FXML
    private TableView<OrderItem> itemsTable;
    @FXML
    private TableColumn<OrderItem, String> colItemName;
    @FXML
    private TableColumn<OrderItem, Number> colItemQty;
    @FXML
    private TableColumn<OrderItem, Number> colItemPrice;
    @FXML
    private TableColumn<OrderItem, Number> colItemTotal;

    @FXML
    private DatePicker fromDate;
    @FXML
    private DatePicker toDate;
    @FXML
    private ComboBox<String> statusCombo;
    @FXML
    private TextField searchField;

    private final ObservableList<Order> orders = FXCollections.observableArrayList();
    private final ObservableList<OrderItem> orderItems = FXCollections.observableArrayList();

    // Extra display fields stored in a parallel map via Order subclass approach
    // We'll use a simple inner helper to carry extra joined data
    private final java.util.Map<Long, String[]> orderExtras = new java.util.HashMap<>();

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        statusCombo.setItems(FXCollections.observableArrayList("ALL", "PENDING", "COMPLETED", "CANCELLED"));
        statusCombo.getSelectionModel().selectFirst();

        wireOrderTable();
        wireItemsTable();

        orderTable.setItems(orders);
        itemsTable.setItems(orderItems);

        orderTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null)
                loadOrderItems(newVal.getId());
            else
                orderItems.clear();
        });

        loadOrders(null, null, null, null);
    }

    private void wireOrderTable() {
        colId.setCellValueFactory(c -> new SimpleLongProperty(c.getValue().getId()));
        colCustomer.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCustomerName() != null ? c.getValue().getCustomerName() : ""));
        colType.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCustomerType() != null ? c.getValue().getCustomerType() : ""));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        colSubtotal.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getSubtotal()));
        colDiscount.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getDiscountAmount()));
        colTax.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getTaxAmount()));
        colTotal.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getTotal()));
        colPayment.setCellValueFactory(c -> {
            String[] extra = orderExtras.get(c.getValue().getId());
            return new SimpleStringProperty(extra != null ? extra[0] : "");
        });
        colInvoice.setCellValueFactory(c -> {
            String[] extra = orderExtras.get(c.getValue().getId());
            return new SimpleStringProperty(extra != null ? extra[1] : "");
        });
        colDate.setCellValueFactory(c -> {
            LocalDateTime dt = c.getValue().getCreatedAt();
            return new SimpleStringProperty(dt != null ? dt.format(DT_FMT) : "");
        });

        // Color-code status column
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                switch (item) {
                    case "COMPLETED":
                        setStyle("-fx-text-fill: #16a34a; -fx-font-weight: 800;");
                        break;
                    case "CANCELLED":
                        setStyle("-fx-text-fill: #dc2626; -fx-font-weight: 800;");
                        break;
                    case "PENDING":
                        setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: 800;");
                        break;
                    default:
                        setStyle("-fx-font-weight: 700;");
                        break;
                }
            }
        });
    }

    private void wireItemsTable() {
        colItemName.setCellValueFactory(c -> {
            // We'll store item name in a trick via OrderItem - for now lookup from joined
            // query
            String name = itemNameMap.getOrDefault(c.getValue().getMenuItemId(),
                    "Item #" + c.getValue().getMenuItemId());
            return new SimpleStringProperty(name);
        });
        colItemQty.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getQuantity()));
        colItemPrice.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getUnitPrice()));
        colItemTotal.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getLineTotal()));
    }

    private final java.util.Map<Long, String> itemNameMap = new java.util.HashMap<>();

    private void loadOrders(LocalDate from, LocalDate to, String status, String search) {
        orders.clear();
        orderExtras.clear();
        StringBuilder sql = new StringBuilder(
                "SELECT o.id, o.customer_name, o.customer_type, o.status, o.subtotal, o.tax_amount, " +
                        "o.discount_amount, o.total, o.created_at, " +
                        "p.method AS payment_method, inv.invoice_number " +
                        "FROM orders o " +
                        "LEFT JOIN payments p ON p.order_id = o.id " +
                        "LEFT JOIN invoices inv ON inv.order_id = o.id " +
                        "WHERE 1=1 ");

        List<Object> params = new ArrayList<>();
        if (from != null) {
            sql.append("AND DATE(o.created_at) >= ? ");
            params.add(Date.valueOf(from));
        }
        if (to != null) {
            sql.append("AND DATE(o.created_at) <= ? ");
            params.add(Date.valueOf(to));
        }
        if (status != null && !status.equals("ALL")) {
            sql.append("AND o.status = ? ");
            params.add(status);
        }
        if (search != null && !search.isBlank()) {
            sql.append("AND o.customer_name LIKE ? ");
            params.add("%" + search.trim() + "%");
        }
        sql.append("ORDER BY o.created_at DESC LIMIT 500");

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Order o = new Order();
                    o.setId(rs.getLong("id"));
                    o.setCustomerName(rs.getString("customer_name"));
                    o.setCustomerType(rs.getString("customer_type"));
                    o.setStatus(rs.getString("status"));
                    o.setSubtotal(rs.getDouble("subtotal"));
                    o.setTaxAmount(rs.getDouble("tax_amount"));
                    o.setDiscountAmount(rs.getDouble("discount_amount"));
                    o.setTotal(rs.getDouble("total"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null)
                        o.setCreatedAt(ts.toLocalDateTime());
                    orderExtras.put(o.getId(), new String[] {
                            rs.getString("payment_method"),
                            rs.getString("invoice_number")
                    });
                    orders.add(o);
                }
            }
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to load orders: " + e.getMessage()).showAndWait();
        }
    }

    private void loadOrderItems(long orderId) {
        orderItems.clear();
        itemNameMap.clear();
        String sql = "SELECT oi.id, oi.menu_item_id, oi.quantity, oi.unit_price, oi.line_total, mi.name AS item_name " +
                "FROM order_items oi JOIN menu_items mi ON mi.id = oi.menu_item_id " +
                "WHERE oi.order_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderItem oi = new OrderItem();
                    oi.setId(rs.getLong("id"));
                    oi.setMenuItemId(rs.getLong("menu_item_id"));
                    oi.setQuantity(rs.getInt("quantity"));
                    oi.setUnitPrice(rs.getDouble("unit_price"));
                    oi.setLineTotal(rs.getDouble("line_total"));
                    itemNameMap.put(oi.getMenuItemId(), rs.getString("item_name"));
                    orderItems.add(oi);
                }
            }
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to load order items: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    public void onFilter() {
        loadOrders(fromDate.getValue(), toDate.getValue(),
                statusCombo.getValue(), searchField.getText());
    }

    @FXML
    public void onReset() {
        fromDate.setValue(null);
        toDate.setValue(null);
        statusCombo.getSelectionModel().selectFirst();
        searchField.clear();
        loadOrders(null, null, null, null);
    }

    @FXML
    public void onCancel() {
        Order selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Select an order to cancel.").showAndWait();
            return;
        }
        if ("CANCELLED".equals(selected.getStatus())) {
            new Alert(Alert.AlertType.INFORMATION, "Order is already cancelled.").showAndWait();
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Cancel order #" + selected.getId() + "? This will reverse inventory adjustments.",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try (Connection conn = DBConnection.getConnection();
                        CallableStatement cs = conn.prepareCall("{CALL cancel_order_procedure(?)}")) {
                    cs.setLong(1, selected.getId());
                    cs.execute();
                    new Alert(Alert.AlertType.INFORMATION, "Order #" + selected.getId() + " cancelled.").showAndWait();
                    onFilter(); // refresh
                } catch (SQLException e) {
                    new Alert(Alert.AlertType.ERROR, "Cancel failed: " + e.getMessage()).showAndWait();
                }
            }
        });
    }
}
