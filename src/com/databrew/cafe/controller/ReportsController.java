package com.databrew.cafe.controller;

import com.databrew.cafe.util.DBConnection;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.*;
import java.time.LocalDate;

public class ReportsController {

    @FXML
    private DatePicker fromDate;
    @FXML
    private DatePicker toDate;
    @FXML
    private Label reportTitle;
    @FXML
    private TableView<ObservableList<String>> reportTable;

    @FXML
    private javafx.scene.layout.HBox summaryBar;
    @FXML
    private Label summaryLabel1, summaryCaption1;
    @FXML
    private Label summaryLabel2, summaryCaption2;
    @FXML
    private Label summaryLabel3, summaryCaption3;
    @FXML
    private Label summaryLabel4, summaryCaption4;

    @FXML
    public void initialize() {
        fromDate.setValue(LocalDate.now().minusDays(30));
        toDate.setValue(LocalDate.now());
    }

    // ---- Report actions ----

    @FXML
    public void onDailySales() {
        LocalDate from = fromDate.getValue();
        LocalDate to = toDate.getValue();
        if (from == null || to == null) {
            showWarn("Select date range first.");
            return;
        }
        reportTitle.setText("Daily Sales Report (" + from + " to " + to + ")");
        hideSummary();
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

    @FXML
    public void onMonthlySummary() {
        LocalDate d = toDate.getValue() != null ? toDate.getValue() : LocalDate.now();
        reportTitle.setText("Monthly Summary for " + d.getMonth() + " " + d.getYear());
        String sql = "{CALL monthly_summary_procedure(?, ?)}";
        try (Connection conn = DBConnection.getConnection();
                CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, d.getYear());
            cs.setInt(2, d.getMonthValue());
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    showSummary(
                            String.valueOf(rs.getInt("total_orders")), "Total Orders",
                            String.valueOf(rs.getInt("total_items_sold")), "Items Sold",
                            String.format("$%.2f", rs.getDouble("total_revenue")), "Revenue",
                            String.format("$%.2f", rs.getDouble("avg_order_value")), "Avg Order");
                    // Also show in table
                    populateTableFromStart(rs);
                } else {
                    hideSummary();
                }
            }
        } catch (SQLException e) {
            showError(e);
        }
    }

    @FXML
    public void onRevenueByCategory() {
        reportTitle.setText("Revenue by Category");
        hideSummary();
        String sql = "SELECT * FROM revenue_by_category_view";
        runQuery(sql);
    }

    @FXML
    public void onPaymentBreakdown() {
        reportTitle.setText("Payment Method Breakdown");
        hideSummary();
        String sql = "SELECT * FROM payment_method_breakdown_view";
        runQuery(sql);
    }

    @FXML
    public void onTopSellers() {
        reportTitle.setText("Top Selling Items");
        hideSummary();
        String sql = "SELECT * FROM top_selling_items_view";
        runQuery(sql);
    }

    @FXML
    public void onSupplierSummary() {
        reportTitle.setText("Supplier Summary");
        hideSummary();
        String sql = "SELECT * FROM supplier_summary_view";
        runQuery(sql);
    }

    // ---- Helpers ----

    private void runQuery(String sql) {
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            populateTable(rs);
        } catch (SQLException e) {
            showError(e);
        }
    }

    /**
     * Dynamically builds columns and rows from any ResultSet.
     */
    @SuppressWarnings("unchecked")
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

    /**
     * For monthly summary: re-populate table from a ResultSet already past first
     * row.
     */
    @SuppressWarnings("unchecked")
    private void populateTableFromStart(ResultSet rs) throws SQLException {
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
            col.setPrefWidth(160);
            reportTable.getColumns().add(col);
        }

        // We already consumed the first row in the caller, so build row from current
        // cursor
        ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
        // Re-read current row (cursor is still on it after rs.next() returned true)
        ObservableList<String> row = FXCollections.observableArrayList();
        for (int i = 1; i <= colCount; i++) {
            String val = rs.getString(i);
            row.add(val != null ? val : "");
        }
        data.add(row);
        while (rs.next()) {
            ObservableList<String> r = FXCollections.observableArrayList();
            for (int i = 1; i <= colCount; i++) {
                String val = rs.getString(i);
                r.add(val != null ? val : "");
            }
            data.add(r);
        }
        reportTable.setItems(data);
    }

    private String prettifyColumnName(String raw) {
        return raw.replace("_", " ").substring(0, 1).toUpperCase() + raw.replace("_", " ").substring(1);
    }

    private void showSummary(String v1, String c1, String v2, String c2, String v3, String c3, String v4, String c4) {
        summaryLabel1.setText(v1);
        summaryCaption1.setText(c1);
        summaryLabel2.setText(v2);
        summaryCaption2.setText(c2);
        summaryLabel3.setText(v3);
        summaryCaption3.setText(c3);
        summaryLabel4.setText(v4);
        summaryCaption4.setText(c4);
        summaryBar.setVisible(true);
        summaryBar.setManaged(true);
    }

    private void hideSummary() {
        summaryBar.setVisible(false);
        summaryBar.setManaged(false);
    }

    private void showWarn(String msg) {
        new Alert(Alert.AlertType.WARNING, msg).showAndWait();
    }

    private void showError(SQLException e) {
        new Alert(Alert.AlertType.ERROR, "Report failed: " + e.getMessage()).showAndWait();
    }
}
