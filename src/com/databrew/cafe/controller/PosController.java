package com.databrew.cafe.controller;

import com.databrew.cafe.dao.MenuDao;
import com.databrew.cafe.model.MenuItem;
import com.databrew.cafe.model.Order;
import com.databrew.cafe.model.OrderItem;
import com.databrew.cafe.service.OrderService;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PosController {
    @FXML
    private TextField searchField;
    @FXML
    private TableView<MenuItem> menuTable;
    @FXML
    private TableColumn<MenuItem, String> menuNameCol;
    @FXML
    private TableColumn<MenuItem, Number> menuPriceCol;
    @FXML
    private TableColumn<MenuItem, String> menuCategoryCol;

    @FXML
    private TableView<OrderItem> cartTable;
    @FXML
    private TableColumn<OrderItem, String> cartItemCol;
    @FXML
    private TableColumn<OrderItem, Number> cartQtyCol;
    @FXML
    private TableColumn<OrderItem, Number> cartTotalCol;
    @FXML
    private Label totalLabel;
    @FXML
    private TextField customerField;
    @FXML
    private ComboBox<String> methodCombo;

    private final MenuDao menuDao = new MenuDao();
    private final OrderService orderService = new OrderService();
    private final ObservableList<MenuItem> allMenuItems = FXCollections.observableArrayList();
    private final ObservableList<MenuItem> filteredMenuItems = FXCollections.observableArrayList();
    private final ObservableList<OrderItem> cartItems = FXCollections.observableArrayList();
    private final Map<Long, MenuItem> menuIndex = new HashMap<>();

    @FXML
    public void initialize() {
        wireMenuTable();
        wireCartTable();
        loadMenuItems();
        methodCombo.setItems(FXCollections.observableArrayList("CASH", "CARD", "MFS"));
        methodCombo.getSelectionModel().selectFirst();
        cartTable.setItems(cartItems);
    }

    private void wireMenuTable() {
        menuNameCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        menuPriceCol.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getPrice()));
        // Category names are not available in the model, so we show the category id for
        // now.
        menuCategoryCol
                .setCellValueFactory(cell -> new SimpleStringProperty("Category " + cell.getValue().getCategoryId()));
        menuTable.setItems(filteredMenuItems);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter(newVal));
    }

    private void wireCartTable() {
        cartItemCol.setCellValueFactory(cell -> {
            MenuItem mi = menuIndex.get(cell.getValue().getMenuItemId());
            String name = (mi != null) ? mi.getName() : "Item #" + cell.getValue().getMenuItemId();
            return new SimpleStringProperty(name);
        });
        cartQtyCol.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getQuantity()));
        cartTotalCol.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getLineTotal()));
    }

    private void loadMenuItems() {
        try {
            List<MenuItem> items = menuDao.findActive();
            allMenuItems.setAll(items);
            filteredMenuItems.setAll(items);
            menuIndex.clear();
            for (MenuItem item : items) {
                menuIndex.put(item.getId(), item);
            }
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Menu load failed: " + e.getMessage()).showAndWait();
        }
    }

    private void applyFilter(String query) {
        String term = query == null ? "" : query.trim().toLowerCase();
        if (term.isEmpty()) {
            filteredMenuItems.setAll(allMenuItems);
            return;
        }
        // Simple name/description filter to keep the search responsive.
        filteredMenuItems.setAll(allMenuItems.filtered(item -> item.getName().toLowerCase().contains(term) ||
                (item.getDescription() != null && item.getDescription().toLowerCase().contains(term))));
    }

    @FXML
    public void onAdd() {
        MenuItem selected = menuTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        OrderItem oi = new OrderItem();
        oi.setMenuItemId(selected.getId());
        oi.setQuantity(1);
        oi.setUnitPrice(selected.getPrice());
        oi.setLineTotal(selected.getPrice());
        cartItems.add(oi);
        updateTotal();
    }

    @FXML
    public void onCheckout() {
        if (cartItems.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Cart is empty").showAndWait();
            return;
        }
        Order order = new Order();
        order.setCustomerName(customerField.getText());
        order.setCustomerType("GENERAL");
        order.setItems(cartItems);
        double subtotal = cartItems.stream().mapToDouble(OrderItem::getLineTotal).sum();
        order.setSubtotal(subtotal);
        order.setDiscountAmount(0);
        order.setTaxAmount(0);
        order.setTotal(subtotal);
        order.setTaxId(null);
        order.setDiscountId(null);

        try {
            long orderId = orderService.createOrderWithItems(order, cartItems);
            orderService.recordPaymentAndInvoice(orderId, order.getTotal(), methodCombo.getValue());
            new Alert(Alert.AlertType.INFORMATION, "Order completed").showAndWait();
            cartItems.clear();
            updateTotal();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Checkout failed: " + e.getMessage()).showAndWait();
        }
    }

    private void updateTotal() {
        double total = cartItems.stream().mapToDouble(OrderItem::getLineTotal).sum();
        totalLabel.setText(String.format("$%.2f", total));
    }
}
