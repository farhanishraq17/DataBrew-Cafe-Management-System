package com.databrew.cafe.controller;

import com.databrew.cafe.model.InventoryItem;
import com.databrew.cafe.service.InventoryService;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class InventoryController {

    @FXML
    private TableView<InventoryItem> inventoryTable;
    @FXML
    private TableColumn<InventoryItem, Number> colId;
    @FXML
    private TableColumn<InventoryItem, String> colName;
    @FXML
    private TableColumn<InventoryItem, Number> colQty;
    @FXML
    private TableColumn<InventoryItem, String> colUnit;
    @FXML
    private TableColumn<InventoryItem, Number> colMinThreshold;
    @FXML
    private TableColumn<InventoryItem, String> colStatus;
    @FXML
    private TableColumn<InventoryItem, String> colUpdated;

    @FXML
    private TextField nameField;
    @FXML
    private TextField unitField;
    @FXML
    private TextField qtyField;
    @FXML
    private TextField minThresholdField;
    @FXML
    private ToggleButton lowStockToggle;

    private final InventoryService inventoryService = new InventoryService();
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private boolean showLowStockOnly = false;

    @FXML
    public void initialize() {
        wireTable();
        refresh();
    }

    private void wireTable() {
        colId.setCellValueFactory(c -> new SimpleLongProperty(c.getValue().getId()));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getIngredientName()));
        colQty.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getQuantity()));
        colUnit.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUnit()));
        colMinThreshold.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getMinThreshold()));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStockStatus()));

        // Color-code the stock status
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("Low".equals(item)) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    }
                }
            }
        });

        colUpdated.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getLastUpdated() != null ? c.getValue().getLastUpdated().format(DT_FMT) : ""));

        inventoryTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> loadItem(n));
    }

    private void refresh() {
        try {
            List<InventoryItem> items;
            if (showLowStockOnly) {
                items = inventoryService.listLowStock();
            } else {
                items = inventoryService.listInventory();
            }
            inventoryTable.setItems(FXCollections.observableArrayList(items));
        } catch (SQLException e) {
            showError("Inventory load failed: " + e.getMessage());
        }
    }

    private void loadItem(InventoryItem item) {
        if (item == null)
            return;
        nameField.setText(item.getIngredientName());
        unitField.setText(item.getUnit());
        qtyField.setText(String.valueOf(item.getQuantity()));
        minThresholdField.setText(String.valueOf(item.getMinThreshold()));
    }

    @FXML
    private void onSave() {
        InventoryItem sel = inventoryTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            onAdd();
        } else {
            onUpdate(sel);
        }
    }

    @FXML
    private void onAdd() {
        inventoryTable.getSelectionModel().clearSelection();
        String name = nameField.getText();
        String unit = unitField.getText();
        if (name == null || name.isBlank() || unit == null || unit.isBlank()) {
            showError("Ingredient name and unit are required.");
            return;
        }
        try {
            double qty = parseDouble(qtyField.getText(), 0);
            double minTh = parseDouble(minThresholdField.getText(), 0);
            inventoryService.addItem(name.trim(), unit.trim(), minTh, qty);
            onClear();
            refresh();
        } catch (NumberFormatException e) {
            showError("Quantity and Min Threshold must be numbers.");
        } catch (SQLException e) {
            showError("Add failed: " + e.getMessage());
        }
    }

    @FXML
    private void onUpdateAction() {
        InventoryItem sel = inventoryTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showError("Select an item to update.");
            return;
        }
        onUpdate(sel);
    }

    private void onUpdate(InventoryItem sel) {
        String name = nameField.getText();
        String unit = unitField.getText();
        if (name == null || name.isBlank() || unit == null || unit.isBlank()) {
            showError("Ingredient name and unit are required.");
            return;
        }
        try {
            double qty = parseDouble(qtyField.getText(), 0);
            double minTh = parseDouble(minThresholdField.getText(), 0);
            inventoryService.updateItem(sel.getId(), sel.getIngredientId(), name.trim(), unit.trim(), minTh, qty);
            refresh();
        } catch (NumberFormatException e) {
            showError("Quantity and Min Threshold must be numbers.");
        } catch (SQLException e) {
            showError("Update failed: " + e.getMessage());
        }
    }

    @FXML
    private void onDelete() {
        InventoryItem sel = inventoryTable.getSelectionModel().getSelectedItem();
        if (sel == null)
            return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete ingredient \"" + sel.getIngredientName() + "\" and its stock?",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.showAndWait().filter(btn -> btn == ButtonType.OK).ifPresent(btn -> {
            try {
                inventoryService.deleteItem(sel.getId(), sel.getIngredientId());
                onClear();
                refresh();
            } catch (SQLException e) {
                showError("Delete failed: " + e.getMessage());
            }
        });
    }

    @FXML
    private void onClear() {
        inventoryTable.getSelectionModel().clearSelection();
        nameField.clear();
        unitField.clear();
        qtyField.clear();
        minThresholdField.clear();
    }

    @FXML
    private void onToggleLowStock() {
        showLowStockOnly = lowStockToggle.isSelected();
        refresh();
    }

    private double parseDouble(String text, double defaultVal) {
        if (text == null || text.isBlank())
            return defaultVal;
        return Double.parseDouble(text.trim());
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}
