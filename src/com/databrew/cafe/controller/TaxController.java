package com.databrew.cafe.controller;

import com.databrew.cafe.dao.TaxDao;
import com.databrew.cafe.model.Tax;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;

public class TaxController {

    @FXML
    private TableView<Tax> taxTable;
    @FXML
    private TableColumn<Tax, Number> colId;
    @FXML
    private TableColumn<Tax, String> colName;
    @FXML
    private TableColumn<Tax, Number> colRate;

    @FXML
    private TextField nameField;
    @FXML
    private TextField rateField;

    private final TaxDao taxDao = new TaxDao();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new SimpleLongProperty(c.getValue().getId()));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colRate.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getRate()));
        taxTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> loadItem(n));
        refresh();
    }

    private void refresh() {
        try {
            taxTable.setItems(FXCollections.observableArrayList(taxDao.findAll()));
        } catch (SQLException e) {
            showError("Load failed: " + e.getMessage());
        }
    }

    private void loadItem(Tax t) {
        if (t == null)
            return;
        nameField.setText(t.getName());
        rateField.setText(String.valueOf(t.getRate()));
    }

    @FXML
    private void onSave() {
        Tax sel = taxTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            onAdd();
        } else {
            onUpdate(sel);
        }
    }

    @FXML
    private void onAdd() {
        taxTable.getSelectionModel().clearSelection();
        if (nameField.getText() == null || nameField.getText().isBlank()) {
            showError("Tax name is required.");
            return;
        }
        try {
            Tax t = new Tax();
            t.setName(nameField.getText().trim());
            t.setRate(Double.parseDouble(rateField.getText().trim()));
            taxDao.insert(t);
            onClear();
            refresh();
        } catch (NumberFormatException e) {
            showError("Rate must be a number.");
        } catch (SQLException e) {
            showError("Add failed: " + e.getMessage());
        }
    }

    @FXML
    private void onUpdateAction() {
        Tax sel = taxTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showError("Select a tax to update.");
            return;
        }
        onUpdate(sel);
    }

    private void onUpdate(Tax sel) {
        if (nameField.getText() == null || nameField.getText().isBlank()) {
            showError("Tax name is required.");
            return;
        }
        try {
            sel.setName(nameField.getText().trim());
            sel.setRate(Double.parseDouble(rateField.getText().trim()));
            taxDao.update(sel);
            refresh();
        } catch (NumberFormatException e) {
            showError("Rate must be a number.");
        } catch (SQLException e) {
            showError("Update failed: " + e.getMessage());
        }
    }

    @FXML
    private void onDelete() {
        Tax sel = taxTable.getSelectionModel().getSelectedItem();
        if (sel == null)
            return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete tax \"" + sel.getName() + "\"?", ButtonType.OK,
                ButtonType.CANCEL);
        confirm.showAndWait().filter(btn -> btn == ButtonType.OK).ifPresent(btn -> {
            try {
                taxDao.delete(sel.getId());
                onClear();
                refresh();
            } catch (SQLException e) {
                showError("Delete failed: " + e.getMessage());
            }
        });
    }

    @FXML
    private void onClear() {
        taxTable.getSelectionModel().clearSelection();
        nameField.clear();
        rateField.clear();
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}
