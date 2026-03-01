package com.databrew.cafe.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.IOException;
import java.sql.*;
import com.databrew.cafe.model.User;
import com.databrew.cafe.util.DBConnection;

public class DashboardController {

    @FXML
    private BorderPane mainLayout;
    @FXML
    private Button fullscreenButton;
    @FXML
    private Label currentUserLabel;
    @FXML
    private VBox welcomePane;

    // Metric labels
    @FXML
    private Label metricRevenue;
    @FXML
    private Label metricOrders;
    @FXML
    private Label metricPending;
    @FXML
    private Label metricMenuItems;

    // Top sellers table
    @FXML
    private TableView<ObservableList<String>> topSellersTable;
    @FXML
    private TableColumn<ObservableList<String>, String> tsNameCol;
    @FXML
    private TableColumn<ObservableList<String>, String> tsQtyCol;
    @FXML
    private TableColumn<ObservableList<String>, String> tsRevenueCol;

    // Low stock table
    @FXML
    private TableView<ObservableList<String>> lowStockTable;
    @FXML
    private TableColumn<ObservableList<String>, String> lsNameCol;
    @FXML
    private TableColumn<ObservableList<String>, String> lsQtyCol;
    @FXML
    private TableColumn<ObservableList<String>, String> lsMinCol;
    @FXML
    private TableColumn<ObservableList<String>, String> lsStatusCol;

    // Recent orders table
    @FXML
    private TableView<ObservableList<String>> recentOrdersTable;
    @FXML
    private TableColumn<ObservableList<String>, String> roIdCol;
    @FXML
    private TableColumn<ObservableList<String>, String> roCustomerCol;
    @FXML
    private TableColumn<ObservableList<String>, String> roTotalCol;
    @FXML
    private TableColumn<ObservableList<String>, String> roStatusCol;
    @FXML
    private TableColumn<ObservableList<String>, String> roDateCol;

    private User currentUser;
    private Parent welcomeContent;

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (currentUserLabel != null && user != null) {
            currentUserLabel.setText("Current User: " + user.getUsername());
        }
    }

    @FXML
    public void initialize() {
        // Wire generic table columns
        wireCol(tsNameCol, 0);
        wireCol(tsQtyCol, 1);
        wireCol(tsRevenueCol, 2);
        wireCol(lsNameCol, 0);
        wireCol(lsQtyCol, 1);
        wireCol(lsMinCol, 2);
        wireCol(lsStatusCol, 3);
        wireCol(roIdCol, 0);
        wireCol(roCustomerCol, 1);
        wireCol(roTotalCol, 2);
        wireCol(roStatusCol, 3);
        wireCol(roDateCol, 4);

        // Color code status columns
        colorStatusCol(lsStatusCol, 3);
        colorStatusCol(roStatusCol, 3);

        // Save the welcome pane for later restoring
        welcomeContent = welcomePane;

        loadDashboardData();
    }

    @SuppressWarnings("unchecked")
    private void wireCol(TableColumn col, int idx) {
        col.setCellValueFactory(cell -> {
            ObservableList<String> row = (ObservableList<String>) ((TableColumn.CellDataFeatures) cell).getValue();
            return new SimpleStringProperty(idx < row.size() ? row.get(idx) : "");
        });
    }

    @SuppressWarnings("unchecked")
    private void colorStatusCol(TableColumn col, int idx) {
        col.setCellFactory(c -> new TableCell<ObservableList<String>, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                if (item.contains("Low") || item.equals("CANCELLED")) {
                    setStyle("-fx-text-fill: #dc2626; -fx-font-weight: 800;");
                } else if (item.equals("COMPLETED") || item.equals("OK")) {
                    setStyle("-fx-text-fill: #16a34a; -fx-font-weight: 800;");
                } else if (item.equals("PENDING")) {
                    setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: 800;");
                } else {
                    setStyle("-fx-font-weight: 700;");
                }
            }
        });
    }

    private void loadDashboardData() {
        loadMetrics();
        loadTopSellers();
        loadLowStock();
        loadRecentOrders();
    }

    private void loadMetrics() {
        try (Connection conn = DBConnection.getConnection()) {
            // Today's revenue
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(SUM(total),0) FROM orders WHERE DATE(created_at)=CURDATE() AND status<>'CANCELLED'");
                    ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    metricRevenue.setText(String.format("$%.2f", rs.getDouble(1)));
            }
            // Today's order count
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM orders WHERE DATE(created_at)=CURDATE() AND status<>'CANCELLED'");
                    ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    metricOrders.setText(String.valueOf(rs.getInt(1)));
            }
            // Pending orders
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM orders WHERE status='PENDING'");
                    ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    metricPending.setText(String.valueOf(rs.getInt(1)));
            }
            // Active menu items
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM menu_items WHERE is_active=1");
                    ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    metricMenuItems.setText(String.valueOf(rs.getInt(1)));
            }
        } catch (SQLException e) {
            System.err.println("Dashboard metrics error: " + e.getMessage());
        }
    }

    private void loadTopSellers() {
        ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM top_selling_items_view LIMIT 10");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ObservableList<String> row = FXCollections.observableArrayList(
                        rs.getString("item_name"),
                        String.valueOf(rs.getInt("total_qty_sold")),
                        String.format("$%.2f", rs.getDouble("total_revenue")));
                data.add(row);
            }
        } catch (SQLException e) {
            System.err.println("Top sellers error: " + e.getMessage());
        }
        topSellersTable.setItems(data);
    }

    private void loadLowStock() {
        ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT i.name AS ingredient, inv.quantity, i.min_threshold, " +
                                "CASE WHEN inv.quantity <= i.min_threshold THEN 'Low' ELSE 'OK' END AS status " +
                                "FROM inventory inv JOIN ingredients i ON i.id = inv.ingredient_id " +
                                "WHERE inv.quantity <= i.min_threshold ORDER BY inv.quantity ASC LIMIT 10");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ObservableList<String> row = FXCollections.observableArrayList(
                        rs.getString("ingredient"),
                        String.valueOf(rs.getDouble("quantity")),
                        String.valueOf(rs.getDouble("min_threshold")),
                        rs.getString("status"));
                data.add(row);
            }
        } catch (SQLException e) {
            System.err.println("Low stock error: " + e.getMessage());
        }
        lowStockTable.setItems(data);
    }

    private void loadRecentOrders() {
        ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT id, COALESCE(customer_name,'Walk-in') as customer, total, status, created_at " +
                                "FROM orders ORDER BY created_at DESC LIMIT 10");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ObservableList<String> row = FXCollections.observableArrayList(
                        String.valueOf(rs.getLong("id")),
                        rs.getString("customer"),
                        String.format("$%.2f", rs.getDouble("total")),
                        rs.getString("status"),
                        rs.getTimestamp("created_at") != null
                                ? rs.getTimestamp("created_at").toLocalDateTime().toString()
                                : "");
                data.add(row);
            }
        } catch (SQLException e) {
            System.err.println("Recent orders error: " + e.getMessage());
        }
        recentOrdersTable.setItems(data);
    }

    // ---- Sidebar navigation handlers ----

    @FXML
    private void handleGoToPOS(ActionEvent event) {
        loadView("PosView.fxml");
    }

    @FXML
    private void handleGoToMenu(ActionEvent event) {
        loadView("MenuView.fxml");
    }

    @FXML
    private void handleGoToEmployees(ActionEvent event) {
        loadView("EmployeeView.fxml");
    }

    @FXML
    private void handleGoToInventory(ActionEvent event) {
        loadView("InventoryView.fxml");
    }

    @FXML
    private void handleGoToSuppliers(ActionEvent event) {
        loadView("SupplierView.fxml");
    }

    @FXML
    private void handleGoToPurchases(ActionEvent event) {
        loadView("PurchaseView.fxml");
    }

    @FXML
    private void handleGoToDiscounts(ActionEvent event) {
        loadView("DiscountView.fxml");
    }

    @FXML
    private void handleGoToTaxes(ActionEvent event) {
        loadView("TaxView.fxml");
    }

    @FXML
    private void handleGoToOrderHistory(ActionEvent event) {
        loadView("OrderHistoryView.fxml");
    }

    @FXML
    private void handleGoToReports(ActionEvent event) {
        loadView("ReportsView.fxml");
    }

    @FXML
    private void handleGoToAuditLogs(ActionEvent event) {
        loadView("AuditLogView.fxml");
    }

    @FXML
    private void handleGoToUserManagement(ActionEvent event) {
        loadView("UserManagementView.fxml");
    }

    @FXML
    private void handleGoToShiftManagement(ActionEvent event) {
        loadView("ShiftManagementView.fxml");
    }

    @FXML
    private void handleGoToDashboard(ActionEvent event) {
        if (welcomeContent != null) {
            mainLayout.setCenter(welcomeContent);
            loadDashboardData(); // refresh data
        }
    }

    @FXML
    private void handleMinimize(ActionEvent event) {
        Stage stage = (Stage) mainLayout.getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    private void handleFullscreen(ActionEvent event) {
        Stage stage = (Stage) mainLayout.getScene().getWindow();
        boolean goingFullScreen = !stage.isFullScreen();
        stage.setFullScreen(goingFullScreen);
        if (fullscreenButton != null) {
            fullscreenButton.setText(goingFullScreen ? "Exit Fullscreen" : "Fullscreen");
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/LoginView.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Login - DataBrew Cafe");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadView(String fxmlFile) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource("/fxml/" + fxmlFile));
            mainLayout.setCenter(view);
        } catch (IOException e) {
            System.err.println("Could not load view: " + fxmlFile);
            e.printStackTrace();
        }
    }
}
