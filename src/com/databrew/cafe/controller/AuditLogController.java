package com.databrew.cafe.controller;

import com.databrew.cafe.dao.AuditLogDao;
import com.databrew.cafe.model.AuditLog;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AuditLogController {

    @FXML
    private TableView<AuditLog> auditTable;
    @FXML
    private TableColumn<AuditLog, Number> colId;
    @FXML
    private TableColumn<AuditLog, String> colTimestamp;
    @FXML
    private TableColumn<AuditLog, String> colUser;
    @FXML
    private TableColumn<AuditLog, String> colAction;
    @FXML
    private TableColumn<AuditLog, String> colEntity;
    @FXML
    private TableColumn<AuditLog, Number> colEntityId;
    @FXML
    private TableColumn<AuditLog, String> colDetails;

    @FXML
    private TextField searchField;
    @FXML
    private DatePicker fromDate;
    @FXML
    private DatePicker toDate;

    private final AuditLogDao auditLogDao = new AuditLogDao();
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new SimpleLongProperty(c.getValue().getId()));
        colTimestamp.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCreatedAt() != null ? c.getValue().getCreatedAt().format(DT_FMT) : ""));
        colUser.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getUsername() != null ? c.getValue().getUsername() : "System"));
        colAction.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAction()));
        colEntity.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEntity()));
        colEntityId.setCellValueFactory(c -> {
            Long eid = c.getValue().getEntityId();
            return eid != null ? new SimpleLongProperty(eid) : new SimpleLongProperty(0);
        });
        colDetails.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDetails()));
        refresh();
    }

    private void refresh() {
        try {
            auditTable.setItems(FXCollections.observableArrayList(auditLogDao.findAll()));
        } catch (SQLException e) {
            showError("Load failed: " + e.getMessage());
        }
    }

    @FXML
    private void onFilter() {
        try {
            List<AuditLog> results;
            String keyword = searchField.getText();
            if (keyword != null && !keyword.isBlank()) {
                results = auditLogDao.search(keyword.trim());
            } else if (fromDate.getValue() != null && toDate.getValue() != null) {
                results = auditLogDao.findByDateRange(fromDate.getValue(), toDate.getValue());
            } else if (fromDate.getValue() != null) {
                results = auditLogDao.findByDateRange(fromDate.getValue(), fromDate.getValue());
            } else {
                results = auditLogDao.findAll();
            }
            auditTable.setItems(FXCollections.observableArrayList(results));
        } catch (SQLException e) {
            showError("Filter failed: " + e.getMessage());
        }
    }

    @FXML
    private void onReset() {
        searchField.clear();
        fromDate.setValue(null);
        toDate.setValue(null);
        refresh();
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}
