package com.databrew.cafe.controller;

import com.databrew.cafe.dao.CategoryDao;
import com.databrew.cafe.dao.DiscountDao;
import com.databrew.cafe.dao.MenuDao;
import com.databrew.cafe.dao.TaxDao;
import com.databrew.cafe.model.Category;
import com.databrew.cafe.model.Discount;
import com.databrew.cafe.model.MenuItem;
import com.databrew.cafe.model.Order;
import com.databrew.cafe.model.OrderItem;
import com.databrew.cafe.model.Tax;
import com.databrew.cafe.service.OrderService;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PosController {
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> categoryCombo;
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
    private TableColumn<OrderItem, Number> cartPriceCol;
    @FXML
    private TableColumn<OrderItem, Number> cartTotalCol;
    @FXML
    private TableColumn<OrderItem, String> cartActionsCol;

    @FXML
    private Label subtotalLabel;
    @FXML
    private Label discountLabel;
    @FXML
    private Label taxLabel;
    @FXML
    private Label totalLabel;
    @FXML
    private TextField customerField;
    @FXML
    private ComboBox<String> customerTypeCombo;
    @FXML
    private ComboBox<String> discountCombo;
    @FXML
    private ComboBox<String> taxCombo;
    @FXML
    private ComboBox<String> methodCombo;

    private final MenuDao menuDao = new MenuDao();
    private final CategoryDao categoryDao = new CategoryDao();
    private final DiscountDao discountDao = new DiscountDao();
    private final TaxDao taxDao = new TaxDao();
    private final OrderService orderService = new OrderService();

    private final ObservableList<MenuItem> allMenuItems = FXCollections.observableArrayList();
    private final ObservableList<MenuItem> filteredMenuItems = FXCollections.observableArrayList();
    private final ObservableList<OrderItem> cartItems = FXCollections.observableArrayList();
    private final Map<Long, MenuItem> menuIndex = new HashMap<>();
    private final Map<Long, String> categoryNames = new HashMap<>();

    private List<Discount> discounts;
    private List<Tax> taxes;

    @FXML
    public void initialize() {
        wireMenuTable();
        wireCartTable();
        loadMenuItems();
        loadCategories();
        loadDiscountsAndTaxes();

        customerTypeCombo.setItems(FXCollections.observableArrayList("GENERAL", "STUDENT", "STAFF", "LOYAL"));
        customerTypeCombo.getSelectionModel().selectFirst();
        methodCombo.getSelectionModel().selectFirst();
        cartTable.setItems(cartItems);

        // Recalculate totals when discount or tax selection changes
        discountCombo.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> updateTotals());
        taxCombo.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> updateTotals());

        // When customer type changes, reload applicable discounts
        customerTypeCombo.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
            if (nv != null)
                loadDiscountsForType(nv);
        });

        // Combined filter from category + search
        categoryCombo.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> applyFilter());
        searchField.textProperty().addListener((o, ov, nv) -> applyFilter());
    }

    private void wireMenuTable() {
        menuNameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        menuPriceCol.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getPrice()));
        menuCategoryCol.setCellValueFactory(c -> {
            String cat = categoryNames.getOrDefault(c.getValue().getCategoryId(),
                    "Cat #" + c.getValue().getCategoryId());
            return new SimpleStringProperty(cat);
        });
        menuTable.setItems(filteredMenuItems);
    }

    private void wireCartTable() {
        cartItemCol.setCellValueFactory(c -> {
            MenuItem mi = menuIndex.get(c.getValue().getMenuItemId());
            return new SimpleStringProperty(mi != null ? mi.getName() : "Item #" + c.getValue().getMenuItemId());
        });
        cartQtyCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getQuantity()));
        cartPriceCol.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getUnitPrice()));
        cartTotalCol.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getLineTotal()));
    }

    private void loadMenuItems() {
        try {
            List<MenuItem> items = menuDao.findActive();
            allMenuItems.setAll(items);
            filteredMenuItems.setAll(items);
            menuIndex.clear();
            for (MenuItem item : items)
                menuIndex.put(item.getId(), item);
        } catch (SQLException e) {
            alert(Alert.AlertType.ERROR, "Menu load failed: " + e.getMessage());
        }
    }

    private void loadCategories() {
        try {
            List<Category> cats = categoryDao.findAll();
            ObservableList<String> names = FXCollections.observableArrayList("All Categories");
            for (Category c : cats) {
                categoryNames.put(c.getId(), c.getName());
                names.add(c.getName());
            }
            categoryCombo.setItems(names);
            categoryCombo.getSelectionModel().selectFirst();
        } catch (SQLException e) {
            alert(Alert.AlertType.ERROR, "Category load failed: " + e.getMessage());
        }
    }

    private void loadDiscountsAndTaxes() {
        try {
            discounts = discountDao.findAll();
            taxes = taxDao.findAll();

            loadDiscountsForType("GENERAL");

            ObservableList<String> taxNames = FXCollections.observableArrayList("No Tax");
            for (Tax t : taxes)
                taxNames.add(t.getName() + " (" + t.getRate() + "%)");
            taxCombo.setItems(taxNames);
            taxCombo.getSelectionModel().selectFirst();
        } catch (SQLException e) {
            alert(Alert.AlertType.ERROR, "Load discounts/taxes failed: " + e.getMessage());
        }
    }

    private void loadDiscountsForType(String customerType) {
        try {
            List<Discount> applicable = discountDao.findByCustomerType(customerType);
            ObservableList<String> names = FXCollections.observableArrayList("No Discount");
            for (Discount d : applicable) {
                String label = d.getName() + " ("
                        + (d.getType().equals("PERCENT") ? d.getValue() + "%" : "$" + d.getValue()) + ")";
                names.add(label);
            }
            discountCombo.setItems(names);
            discountCombo.getSelectionModel().selectFirst();
            // Update the discounts reference for calculation
            discounts = applicable;
        } catch (SQLException e) {
            alert(Alert.AlertType.ERROR, "Load discounts failed: " + e.getMessage());
        }
    }

    private void applyFilter() {
        String catSel = categoryCombo.getValue();
        String term = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();

        filteredMenuItems.setAll(allMenuItems.filtered(item -> {
            boolean matchCat = (catSel == null || catSel.equals("All Categories") ||
                    categoryNames.getOrDefault(item.getCategoryId(), "").equals(catSel));
            boolean matchSearch = term.isEmpty() || item.getName().toLowerCase().contains(term) ||
                    (item.getDescription() != null && item.getDescription().toLowerCase().contains(term));
            return matchCat && matchSearch;
        }));
    }

    @FXML
    public void onAdd() {
        MenuItem selected = menuTable.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        // Check if already in cart - increment quantity
        for (OrderItem oi : cartItems) {
            if (oi.getMenuItemId() == selected.getId()) {
                oi.setQuantity(oi.getQuantity() + 1);
                oi.setLineTotal(oi.getQuantity() * oi.getUnitPrice());
                cartTable.refresh();
                updateTotals();
                return;
            }
        }

        OrderItem oi = new OrderItem();
        oi.setMenuItemId(selected.getId());
        oi.setQuantity(1);
        oi.setUnitPrice(selected.getPrice());
        oi.setLineTotal(selected.getPrice());
        cartItems.add(oi);
        updateTotals();
    }

    @FXML
    public void onIncreaseQty() {
        OrderItem sel = cartTable.getSelectionModel().getSelectedItem();
        if (sel == null)
            return;
        sel.setQuantity(sel.getQuantity() + 1);
        sel.setLineTotal(sel.getQuantity() * sel.getUnitPrice());
        cartTable.refresh();
        updateTotals();
    }

    @FXML
    public void onDecreaseQty() {
        OrderItem sel = cartTable.getSelectionModel().getSelectedItem();
        if (sel == null)
            return;
        if (sel.getQuantity() <= 1) {
            cartItems.remove(sel);
        } else {
            sel.setQuantity(sel.getQuantity() - 1);
            sel.setLineTotal(sel.getQuantity() * sel.getUnitPrice());
            cartTable.refresh();
        }
        updateTotals();
    }

    @FXML
    public void onRemoveItem() {
        OrderItem sel = cartTable.getSelectionModel().getSelectedItem();
        if (sel != null) {
            cartItems.remove(sel);
            updateTotals();
        }
    }

    @FXML
    public void onClearCart() {
        cartItems.clear();
        updateTotals();
    }

    @FXML
    public void onCheckout() {
        if (cartItems.isEmpty()) {
            alert(Alert.AlertType.WARNING, "Cart is empty");
            return;
        }

        double subtotal = cartItems.stream().mapToDouble(OrderItem::getLineTotal).sum();

        // Resolve discount
        Discount selectedDiscount = getSelectedDiscount();
        double discountAmt = 0;
        Long discountId = null;
        if (selectedDiscount != null) {
            discountId = selectedDiscount.getId();
            discountAmt = selectedDiscount.getType().equals("PERCENT")
                    ? subtotal * selectedDiscount.getValue() / 100.0
                    : selectedDiscount.getValue();
        }

        // Resolve tax
        Tax selectedTax = getSelectedTax();
        double afterDiscount = subtotal - discountAmt;
        double taxAmt = 0;
        Long taxId = null;
        if (selectedTax != null) {
            taxId = selectedTax.getId();
            taxAmt = afterDiscount * selectedTax.getRate() / 100.0;
        }

        double total = afterDiscount + taxAmt;

        Order order = new Order();
        order.setCustomerName(customerField.getText());
        order.setCustomerType(customerTypeCombo.getValue());
        order.setItems(cartItems);
        order.setSubtotal(subtotal);
        order.setDiscountAmount(discountAmt);
        order.setDiscountId(discountId);
        order.setTaxAmount(taxAmt);
        order.setTaxId(taxId);
        order.setTotal(total);

        try {
            long orderId = orderService.createOrderWithItems(order, cartItems);
            orderService.recordPaymentAndInvoice(orderId, total, methodCombo.getValue());

            // Show receipt dialog
            showReceiptDialog(orderId, order, discountAmt, taxAmt, total,
                    selectedDiscount, selectedTax, methodCombo.getValue());

            cartItems.clear();
            updateTotals();
        } catch (SQLException e) {
            alert(Alert.AlertType.ERROR, "Checkout failed: " + e.getMessage());
        }
    }

    private void updateTotals() {
        double subtotal = cartItems.stream().mapToDouble(OrderItem::getLineTotal).sum();

        Discount d = getSelectedDiscount();
        double discountAmt = 0;
        if (d != null) {
            discountAmt = d.getType().equals("PERCENT") ? subtotal * d.getValue() / 100.0 : d.getValue();
        }

        double afterDiscount = subtotal - discountAmt;

        Tax t = getSelectedTax();
        double taxAmt = 0;
        if (t != null) {
            taxAmt = afterDiscount * t.getRate() / 100.0;
        }

        double total = afterDiscount + taxAmt;

        subtotalLabel.setText(String.format("$%.2f", subtotal));
        discountLabel.setText(String.format("-$%.2f", discountAmt));
        taxLabel.setText(String.format("+$%.2f", taxAmt));
        totalLabel.setText(String.format("$%.2f", total));
    }

    private Discount getSelectedDiscount() {
        int idx = discountCombo.getSelectionModel().getSelectedIndex();
        if (idx <= 0 || discounts == null || idx - 1 >= discounts.size())
            return null;
        return discounts.get(idx - 1);
    }

    private Tax getSelectedTax() {
        int idx = taxCombo.getSelectionModel().getSelectedIndex();
        if (idx <= 0 || taxes == null || idx - 1 >= taxes.size())
            return null;
        return taxes.get(idx - 1);
    }

    private void showReceiptDialog(long orderId, Order order, double discountAmt, double taxAmt,
            double total, Discount discount, Tax tax, String paymentMethod) {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════\n");
        sb.append("         DATABREW CAFE\n");
        sb.append("           RECEIPT\n");
        sb.append("═══════════════════════════════════\n");
        sb.append(String.format("Order #: %d\n", orderId));
        sb.append(String.format("Customer: %s (%s)\n",
                order.getCustomerName() != null ? order.getCustomerName() : "Walk-in",
                order.getCustomerType()));
        sb.append("───────────────────────────────────\n");

        for (OrderItem oi : cartItems.isEmpty() ? order.getItems() : cartItems) {
            MenuItem mi = menuIndex.get(oi.getMenuItemId());
            String name = mi != null ? mi.getName() : "Item";
            sb.append(String.format("%-20s x%d  $%.2f\n", name, oi.getQuantity(), oi.getLineTotal()));
        }

        sb.append("───────────────────────────────────\n");
        sb.append(String.format("Subtotal:              $%.2f\n", order.getSubtotal()));
        if (discount != null) {
            sb.append(String.format("Discount (%s):     -$%.2f\n", discount.getName(), discountAmt));
        }
        if (tax != null) {
            sb.append(String.format("Tax (%s %.1f%%):      +$%.2f\n", tax.getName(), tax.getRate(), taxAmt));
        }
        sb.append("───────────────────────────────────\n");
        sb.append(String.format("TOTAL:                 $%.2f\n", total));
        sb.append(String.format("Payment: %s\n", paymentMethod));
        sb.append("═══════════════════════════════════\n");
        sb.append("       Thank you! Come again!\n");

        Alert receiptAlert = new Alert(Alert.AlertType.INFORMATION);
        receiptAlert.setTitle("Order Receipt");
        receiptAlert.setHeaderText("Order #" + orderId + " Completed!");
        TextArea receiptText = new TextArea(sb.toString());
        receiptText.setEditable(false);
        receiptText.setWrapText(true);
        receiptText.setPrefWidth(400);
        receiptText.setPrefHeight(350);
        receiptText.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 13px;");
        receiptAlert.getDialogPane().setContent(receiptText);
        receiptAlert.getDialogPane().setPrefWidth(460);
        receiptAlert.showAndWait();
    }

    private void alert(Alert.AlertType type, String msg) {
        new Alert(type, msg).showAndWait();
    }
}
