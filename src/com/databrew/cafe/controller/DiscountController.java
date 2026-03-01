package com.databrew.cafe.controller;

import com.databrew.cafe.dao.DiscountDao;
import com.databrew.cafe.model.Discount;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;

public class DiscountController {

    @FXML
    private TableView<Discount> discountTable;
    @FXML
    private TableColumn<Discount, Number> colId;
    @FXML
    private TableColumn<Discount, String> colName;
    @FXML
    private TableColumn<Discount, String> colType;
    @FXML
    private TableColumn<Discount, Number> colValue;
    @FXML
    private TableColumn<Discount, String> colApplies;

    @FXML
    private TextField nameField;
    @FXML
    private ComboBox<String> typeCombo;
    @FXML
    private TextField valueField;
    @FXML
    private ComboBox<String> appliesToCombo;

    private final DiscountDao discountDao = new DiscountDao();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new SimpleLongProperty(c.getValue().getId()));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType()));
        colValue.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getValue()));
        colApplies.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAppliesTo()));

        typeCombo.setItems(FXCollections.observableArrayList("PERCENT", "FLAT"));
        typeCombo.getSelectionModel().selectFirst();
        appliesToCombo.setItems(FXCollections.observableArrayList("GENERAL", "STUDENT", "STAFF", "LOYAL"));
        appliesToCombo.getSelectionModel().selectFirst();

        discountTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> loadItem(n));
        refresh();
    }

    private void refresh() {
        try {
            discountTable.setItems(FXCollections.observableArrayList(discountDao.findAll()));
        } catch (SQLException e) {
            showError("Load failed: " + e.getMessage());
        }
    }

    private void loadItem(Discount d) {
        if (d == null)
            return;
        nameField.setText(d.getName());
        typeCombo.getSelectionModel().select(d.getType());
        valueField.setText(String.valueOf(d.getValue()));
        appliesToCombo.getSelectionModel().select(d.getAppliesTo());
    }

    @FXML
    private void onSave() {
        Discount sel = discountTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            onAdd();
        } else {
            onUpdate(sel);
        }
    }

    @FXML
    private void onAdd() {
        discountTable.getSelectionModel().clearSelection();
        if (nameField.getText() == null || nameField.getText().isBlank()) {
            showError("Name is required.");
            return;
        }
        try {
            Discount d = buildFromForm(new Discount());
            discountDao.insert(d);
            onClear();
            refresh();
        } catch (NumberFormatException e) {
            showError("Value must be a number.");
        } catch (SQLException e) {
            showError("Add failed: " + e.getMessage());
        }
    }

    @FXML
    private void onUpdateAction() {
        Discount sel = discountTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showError("Select a discount to update.");
            return;
        }
        onUpdate(sel);
    }

    private void onUpdate(Discount sel) {
        if (nameField.getText() == null || nameField.getText().isBlank()) {
            showError("Name is required.");
            return;
        }
        try {
            buildFromForm(sel);
            discountDao.update(sel);
            refresh();
        } catch (NumberFormatException e) {
            showError("Value must be a number.");
        } catch (SQLException e) {
            showError("Update failed: " + e.getMessage());
        }
    }

    @FXML
    private void onDelete() {
        Discount sel = discountTable.getSelectionModel().getSelectedItem();
        if (sel == null)
            return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete discount \"" + sel.getName() + "\"?",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.showAndWait().filter(btn -> btn == ButtonType.OK).ifPresent(btn -> {
            try {
                discountDao.delete(sel.getId());
                onClear();
                refresh();
            } catch (SQLException e) {
                showError("Delete failed: " + e.getMessage());
            }
        });
    }

    @FXML
    private void onClear() {
        discountTable.getSelectionModel().clearSelection();
        nameField.clear();
        typeCombo.getSelectionModel().selectFirst();
        valueField.clear();
        appliesToCombo.getSelectionModel().selectFirst();
    }

    private Discount buildFromForm(Discount d) {
        d.setName(nameField.getText().trim());
        d.setType(typeCombo.getValue());
        d.setValue(Double.parseDouble(valueField.getText().trim()));
        d.setAppliesTo(appliesToCombo.getValue());
        return d;
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}
