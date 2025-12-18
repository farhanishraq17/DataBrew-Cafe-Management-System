package com.databrew.cafe.controller;

import com.databrew.cafe.dao.CategoryDao;
import com.databrew.cafe.dao.MenuDao;
import com.databrew.cafe.model.Category;
import com.databrew.cafe.model.MenuItem;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MenuController {
    @FXML
    private TableView<MenuItem> menuTable;
    @FXML
    private TableColumn<MenuItem, Number> colId;
    @FXML
    private TableColumn<MenuItem, String> colName;
    @FXML
    private TableColumn<MenuItem, String> colCategory;
    @FXML
    private TableColumn<MenuItem, Number> colPrice;
    @FXML
    private TableColumn<MenuItem, String> colStatus;

    @FXML
    private TextField searchField;
    @FXML
    private TextField itemNameField;
    @FXML
    private TextField priceField;
    @FXML
    private ComboBox<Category> categoryCombo;
    @FXML
    private ComboBox<String> statusCombo;
    @FXML
    private ImageView itemImageView;
    @FXML
    private Button addItemButton;
    @FXML
    private Button updateItemButton;

    private final MenuDao menuDao = new MenuDao();
    private final CategoryDao categoryDao = new CategoryDao();
    private ObservableList<MenuItem> items = FXCollections.observableArrayList();
    private ObservableList<MenuItem> filtered = FXCollections.observableArrayList();
    private List<Category> categories;

    private MenuItem editingSelection;

    @FXML
    public void initialize() {
        wireTable();
        wireStatusCombo();
        setupSearch();
        loadData();
        menuTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> populateForm(newSel));
        setFormMode(false);
    }

    private void wireTable() {
        colId.setCellValueFactory(c -> new SimpleLongProperty(c.getValue().getId()));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colCategory
                .setCellValueFactory(c -> new SimpleStringProperty(resolveCategoryName(c.getValue().getCategoryId())));
        colPrice.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getPrice()));
        colStatus
                .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isActive() ? "Available" : "Sold Out"));
        menuTable.setItems(filtered);
        menuTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void wireStatusCombo() {
        statusCombo.setItems(FXCollections.observableArrayList("Available", "Sold Out"));
        statusCombo.getSelectionModel().selectFirst();
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, ov, nv) -> applyFilter(nv));
    }

    private void applyFilter(String term) {
        String q = term == null ? "" : term.trim().toLowerCase();
        if (q.isEmpty()) {
            filtered.setAll(items);
            return;
        }
        filtered.setAll(items.stream()
                .filter(i -> i.getName().toLowerCase().contains(q) ||
                        resolveCategoryName(i.getCategoryId()).toLowerCase().contains(q))
                .collect(Collectors.toList()));
    }

    private void loadData() {
        try {
            categories = categoryDao.findAll();
            if (categoryCombo != null) {
                categoryCombo.setItems(FXCollections.observableArrayList(categories));
                categoryCombo.setConverter(new StringConverter<>() {
                    @Override
                    public String toString(Category c) {
                        return c == null ? "" : c.getName();
                    }

                    @Override
                    public Category fromString(String s) {
                        return null;
                    }
                });
                categoryCombo.setCellFactory(list -> new ListCell<>() {
                    @Override
                    protected void updateItem(Category item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null : item.getName());
                    }
                });
                if (!categories.isEmpty()) {
                    categoryCombo.getSelectionModel().selectFirst();
                }
            }
            items = FXCollections.observableArrayList(menuDao.findAll());
            filtered.setAll(items);
        } catch (SQLException e) {
            showError("Failed to load menu: " + e.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        loadData();
        applyFilter(searchField.getText());
        menuTable.getSelectionModel().clearSelection();
        setFormMode(false);
    }

    @FXML
    private void handleClearForm() {
        editingSelection = null;
        itemNameField.clear();
        priceField.clear();
        if (categoryCombo != null && !categoryCombo.getItems().isEmpty()) {
            categoryCombo.getSelectionModel().selectFirst();
        }
        if (statusCombo != null) {
            statusCombo.getSelectionModel().selectFirst();
        }
        menuTable.getSelectionModel().clearSelection();
        setFormMode(false);
    }

    @FXML
    private void handleAddItem() {
        try {
            MenuItem toSave = buildItemFromForm(null);
            if (toSave == null) {
                return;
            }
            menuDao.insert(toSave);
            handleRefresh();
            handleClearForm();
        } catch (NumberFormatException ex) {
            showError("Price must be numeric");
        } catch (SQLException e) {
            showError("Save failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleUpdateItem() {
        if (editingSelection == null) {
            showError("Select an item to update");
            return;
        }
        try {
            MenuItem toSave = buildItemFromForm(editingSelection);
            if (toSave == null) {
                return;
            }
            menuDao.update(toSave);
            handleRefresh();
            handleClearForm();
        } catch (NumberFormatException ex) {
            showError("Price must be numeric");
        } catch (SQLException e) {
            showError("Update failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        MenuItem selected = menuTable.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + selected.getName() + "?", ButtonType.OK,
                ButtonType.CANCEL);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                menuDao.delete(selected.getId());
                handleRefresh();
            } catch (SQLException e) {
                // If FK constraint prevents delete, fall back to marking inactive.
                String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
                if (msg.contains("foreign") || msg.contains("parent") || msg.contains("constraint")) {
                    try {
                        selected.setActive(false);
                        menuDao.update(selected);
                        handleRefresh();
                        showError("Item in use; marked as inactive instead of delete.");
                    } catch (SQLException ex) {
                        showError("Delete failed: " + ex.getMessage());
                    }
                } else {
                    showError("Delete failed: " + e.getMessage());
                }
            }
        }
    }

    @FXML
    private void handleChangeImage() {
        // Placeholder: wire image picker later. For now we just keep the view
        // consistent.
    }

    private void populateForm(MenuItem item) {
        editingSelection = item;
        if (item == null) {
            handleClearForm();
            return;
        }
        itemNameField.setText(item.getName());
        priceField.setText(String.valueOf(item.getPrice()));
        if (categoryCombo != null && categories != null) {
            Category match = categories.stream()
                    .filter(c -> c.getId() == item.getCategoryId())
                    .findFirst()
                    .orElse(null);
            categoryCombo.getSelectionModel().select(match);
        }
        statusCombo.getSelectionModel().select(item.isActive() ? "Available" : "Sold Out");
        setFormMode(true);
    }

    private String resolveCategoryName(long categoryId) {
        if (categories == null)
            return "";
        return categories.stream()
                .filter(c -> c.getId() == categoryId)
                .map(Category::getName)
                .findFirst()
                .orElse("Unknown");
    }

    private MenuItem buildItemFromForm(MenuItem base) {
        MenuItem target = base == null ? new MenuItem() : base;
        target.setName(itemNameField.getText());
        Category selectedCat = categoryCombo.getSelectionModel().getSelectedItem();
        if (selectedCat == null) {
            showError("Please select a category");
            return null;
        }
        target.setCategoryId(selectedCat.getId());
        target.setPrice(Double.parseDouble(priceField.getText()));
        String status = statusCombo.getSelectionModel().getSelectedItem();
        target.setActive(!"Sold Out".equalsIgnoreCase(status));
        return target;
    }

    private void setFormMode(boolean editing) {
        if (addItemButton != null && updateItemButton != null) {
            addItemButton.setVisible(!editing);
            addItemButton.setManaged(!editing);
            updateItemButton.setVisible(editing);
            updateItemButton.setManaged(editing);
        }
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}
