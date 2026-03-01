package com.databrew.cafe.controller;

import com.databrew.cafe.dao.SupplierDao;
import com.databrew.cafe.model.Supplier;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;

public class SupplierController {

    @FXML
    private TableView<Supplier> supplierTable;
    @FXML
    private TableColumn<Supplier, Number> colId;
    @FXML
    private TableColumn<Supplier, String> colName;
    @FXML
    private TableColumn<Supplier, String> colContact;
    @FXML
    private TableColumn<Supplier, String> colPhone;
    @FXML
    private TableColumn<Supplier, String> colEmail;

    @FXML
    private TextField nameField;
    @FXML
    private TextField contactField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField emailField;

    private final SupplierDao supplierDao = new SupplierDao();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new SimpleLongProperty(c.getValue().getId()));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colContact.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getContact()));
        colPhone.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPhone()));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        supplierTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> loadItem(n));
        refresh();
    }

    private void refresh() {
        try {
            supplierTable.setItems(FXCollections.observableArrayList(supplierDao.findAll()));
        } catch (SQLException e) {
            showError("Load failed: " + e.getMessage());
        }
    }

    private void loadItem(Supplier s) {
        if (s == null)
            return;
        nameField.setText(s.getName());
        contactField.setText(s.getContact());
        phoneField.setText(s.getPhone());
        emailField.setText(s.getEmail());
    }

    @FXML
    private void onSave() {
        Supplier sel = supplierTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            onAdd();
        } else {
            onUpdate(sel);
        }
    }

    @FXML
    private void onAdd() {
        supplierTable.getSelectionModel().clearSelection();
        if (nameField.getText() == null || nameField.getText().isBlank()) {
            showError("Supplier name is required.");
            return;
        }
        try {
            Supplier s = new Supplier();
            s.setName(nameField.getText().trim());
            s.setContact(contactField.getText());
            s.setPhone(phoneField.getText());
            s.setEmail(emailField.getText());
            supplierDao.insert(s);
            onClear();
            refresh();
        } catch (SQLException e) {
            showError("Add failed: " + e.getMessage());
        }
    }

    @FXML
    private void onUpdateAction() {
        Supplier sel = supplierTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showError("Select a supplier to update.");
            return;
        }
        onUpdate(sel);
    }

    private void onUpdate(Supplier sel) {
        if (nameField.getText() == null || nameField.getText().isBlank()) {
            showError("Supplier name is required.");
            return;
        }
        try {
            sel.setName(nameField.getText().trim());
            sel.setContact(contactField.getText());
            sel.setPhone(phoneField.getText());
            sel.setEmail(emailField.getText());
            supplierDao.update(sel);
            refresh();
        } catch (SQLException e) {
            showError("Update failed: " + e.getMessage());
        }
    }

    @FXML
    private void onDelete() {
        Supplier sel = supplierTable.getSelectionModel().getSelectedItem();
        if (sel == null)
            return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete supplier \"" + sel.getName() + "\"?",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.showAndWait().filter(btn -> btn == ButtonType.OK).ifPresent(btn -> {
            try {
                supplierDao.delete(sel.getId());
                onClear();
                refresh();
            } catch (SQLException e) {
                showError("Delete failed: " + e.getMessage());
            }
        });
    }

    @FXML
    private void onClear() {
        supplierTable.getSelectionModel().clearSelection();
        nameField.clear();
        contactField.clear();
        phoneField.clear();
        emailField.clear();
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}
