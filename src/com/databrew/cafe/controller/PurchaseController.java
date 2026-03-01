package com.databrew.cafe.controller;

import com.databrew.cafe.dao.PurchaseDao;
import com.databrew.cafe.dao.SupplierDao;
import com.databrew.cafe.service.InventoryService;
import com.databrew.cafe.model.InventoryItem;
import com.databrew.cafe.model.Purchase;
import com.databrew.cafe.model.Supplier;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PurchaseController {

    @FXML
    private TableView<Purchase> purchaseTable;
    @FXML
    private TableColumn<Purchase, Number> colId;
    @FXML
    private TableColumn<Purchase, String> colSupplier;
    @FXML
    private TableColumn<Purchase, String> colIngredient;
    @FXML
    private TableColumn<Purchase, Number> colQty;
    @FXML
    private TableColumn<Purchase, Number> colCost;
    @FXML
    private TableColumn<Purchase, String> colDate;

    @FXML
    private ComboBox<Supplier> supplierCombo;
    @FXML
    private ComboBox<InventoryItem> ingredientCombo;
    @FXML
    private TextField qtyField;
    @FXML
    private TextField costField;

    private final PurchaseDao purchaseDao = new PurchaseDao();
    private final SupplierDao supplierDao = new SupplierDao();
    private final InventoryService inventoryService = new InventoryService();
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        wireTable();
        loadCombos();
        refresh();
    }

    private void wireTable() {
        colId.setCellValueFactory(c -> new SimpleLongProperty(c.getValue().getId()));
        colSupplier.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSupplierName()));
        colIngredient.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getIngredientName()));
        colQty.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getQuantity()));
        colCost.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getCost()));
        colDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getPurchasedAt() != null ? c.getValue().getPurchasedAt().format(DT_FMT) : ""));
    }

    private void loadCombos() {
        try {
            List<Supplier> suppliers = supplierDao.findAll();
            supplierCombo.setItems(FXCollections.observableArrayList(suppliers));
            supplierCombo.setConverter(new StringConverter<>() {
                @Override
                public String toString(Supplier s) {
                    return s == null ? "" : s.getName();
                }

                @Override
                public Supplier fromString(String s) {
                    return null;
                }
            });

            List<InventoryItem> items = inventoryService.listInventory();
            ingredientCombo.setItems(FXCollections.observableArrayList(items));
            ingredientCombo.setConverter(new StringConverter<>() {
                @Override
                public String toString(InventoryItem i) {
                    return i == null ? "" : i.getIngredientName() + " (" + i.getUnit() + ")";
                }

                @Override
                public InventoryItem fromString(String s) {
                    return null;
                }
            });
        } catch (SQLException e) {
            showError("Failed to load combos: " + e.getMessage());
        }
    }

    private void refresh() {
        try {
            purchaseTable.setItems(FXCollections.observableArrayList(purchaseDao.findAll()));
        } catch (SQLException e) {
            showError("Load failed: " + e.getMessage());
        }
    }

    @FXML
    private void onSave() {
        Supplier supplier = supplierCombo.getValue();
        InventoryItem ingredient = ingredientCombo.getValue();
        if (supplier == null || ingredient == null) {
            showError("Select a supplier and ingredient.");
            return;
        }
        try {
            double qty = Double.parseDouble(qtyField.getText().trim());
            double cost = Double.parseDouble(costField.getText().trim());
            if (qty <= 0 || cost < 0) {
                showError("Quantity must be > 0 and cost >= 0.");
                return;
            }
            Purchase p = new Purchase();
            p.setSupplierId(supplier.getId());
            p.setIngredientId(ingredient.getIngredientId());
            p.setQuantity(qty);
            p.setCost(cost);
            purchaseDao.insertAndAdjustStock(p);
            onClear();
            refresh();
            new Alert(Alert.AlertType.INFORMATION, "Purchase recorded. Inventory updated.").showAndWait();
        } catch (NumberFormatException e) {
            showError("Quantity and cost must be numbers.");
        } catch (SQLException e) {
            showError("Record failed: " + e.getMessage());
        }
    }

    @FXML
    private void onUpdatePurchase() {
        showError("Purchases cannot be edited. Record a new purchase instead.");
    }

    @FXML
    private void onDelete() {
        Purchase sel = purchaseTable.getSelectionModel().getSelectedItem();
        if (sel == null)
            return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete this purchase record?", ButtonType.OK,
                ButtonType.CANCEL);
        confirm.showAndWait().filter(btn -> btn == ButtonType.OK).ifPresent(btn -> {
            try {
                purchaseDao.delete(sel.getId());
                refresh();
            } catch (SQLException e) {
                showError("Delete failed: " + e.getMessage());
            }
        });
    }

    @FXML
    private void onClear() {
        supplierCombo.getSelectionModel().clearSelection();
        ingredientCombo.getSelectionModel().clearSelection();
        qtyField.clear();
        costField.clear();
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}
