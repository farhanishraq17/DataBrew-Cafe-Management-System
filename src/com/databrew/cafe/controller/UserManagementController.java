package com.databrew.cafe.controller;

import com.databrew.cafe.dao.RoleDao;
import com.databrew.cafe.dao.UserDao;
import com.databrew.cafe.model.Role;
import com.databrew.cafe.model.User;
import com.databrew.cafe.util.PasswordUtil;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class UserManagementController {

    @FXML
    private TableView<User> userTable;
    @FXML
    private TableColumn<User, Number> colId;
    @FXML
    private TableColumn<User, String> colUsername;
    @FXML
    private TableColumn<User, String> colFullName;
    @FXML
    private TableColumn<User, String> colEmail;
    @FXML
    private TableColumn<User, String> colRoles;
    @FXML
    private TableColumn<User, Boolean> colActive;

    @FXML
    private TextField searchField;
    @FXML
    private TextField usernameField;
    @FXML
    private TextField fullNameField;
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private CheckBox activeCheck;
    @FXML
    private VBox rolesBox;

    private final UserDao userDao = new UserDao();
    private final RoleDao roleDao = new RoleDao();
    private final ObservableList<User> userList = FXCollections.observableArrayList();
    private List<Role> allRoles = new ArrayList<>();
    private final Map<Long, CheckBox> roleCheckBoxes = new LinkedHashMap<>();
    // cache: userId -> list of role names (fetched via UserDao)
    private final Map<Long, List<String>> userRolesCache = new HashMap<>();

    private User selectedUser;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(cd -> new SimpleLongProperty(cd.getValue().getId()));
        colUsername.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getUsername()));
        colFullName.setCellValueFactory(
                cd -> new SimpleStringProperty(cd.getValue().getFullName() != null ? cd.getValue().getFullName() : ""));
        colEmail.setCellValueFactory(
                cd -> new SimpleStringProperty(cd.getValue().getEmail() != null ? cd.getValue().getEmail() : ""));
        colRoles.setCellValueFactory(cd -> {
            List<String> roles = userRolesCache.get(cd.getValue().getId());
            return new SimpleStringProperty(roles != null ? String.join(", ", roles) : "");
        });
        colActive.setCellValueFactory(cd -> new SimpleBooleanProperty(cd.getValue().isActive()));
        colActive.setCellFactory(col -> new TableCell<User, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item ? "Yes" : "No");
                setStyle(item ? "-fx-text-fill: #16a34a; -fx-font-weight: 800;"
                        : "-fx-text-fill: #dc2626; -fx-font-weight: 800;");
            }
        });

        userTable.setItems(userList);
        userTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> onSelected(n));

        loadRoles();
        loadUsers();
    }

    private void loadRoles() {
        try {
            allRoles = roleDao.findAll();
        } catch (SQLException e) {
            allRoles = new ArrayList<>();
        }
        rolesBox.getChildren().clear();
        roleCheckBoxes.clear();
        for (Role r : allRoles) {
            CheckBox cb = new CheckBox(r.getName());
            cb.setStyle("-fx-font-size: 13px;");
            roleCheckBoxes.put(r.getId(), cb);
            rolesBox.getChildren().add(cb);
        }
    }

    private void loadUsers() {
        try {
            List<User> users = userDao.findAll();
            userList.setAll(users);
            // cache roles per user
            userRolesCache.clear();
            for (User u : users) {
                userRolesCache.put(u.getId(), userDao.getUserRoles(u.getId()));
            }
            userTable.refresh();
        } catch (SQLException e) {
            showAlert("Error loading users: " + e.getMessage());
        }
    }

    private void onSelected(User u) {
        selectedUser = u;
        if (u == null)
            return;
        usernameField.setText(u.getUsername());
        fullNameField.setText(u.getFullName() != null ? u.getFullName() : "");
        emailField.setText(u.getEmail() != null ? u.getEmail() : "");
        passwordField.clear();
        activeCheck.setSelected(u.isActive());
        // check role boxes
        List<String> userRoles = userRolesCache.getOrDefault(u.getId(), Collections.emptyList());
        for (Map.Entry<Long, CheckBox> entry : roleCheckBoxes.entrySet()) {
            String roleName = entry.getValue().getText();
            entry.getValue().setSelected(userRoles.contains(roleName));
        }
    }

    @FXML
    private void onSearch() {
        String q = searchField.getText().trim().toLowerCase();
        if (q.isEmpty()) {
            loadUsers();
            return;
        }
        userList.setAll(userList.stream()
                .filter(u -> (u.getUsername() != null && u.getUsername().toLowerCase().contains(q))
                        || (u.getFullName() != null && u.getFullName().toLowerCase().contains(q))
                        || (u.getEmail() != null && u.getEmail().toLowerCase().contains(q)))
                .collect(Collectors.toList()));
    }

    @FXML
    private void onShowAll() {
        searchField.clear();
        loadUsers();
    }

    @FXML
    private void onAdd() {
        userTable.getSelectionModel().clearSelection();
        selectedUser = null;
        onSave();
    }

    @FXML
    private void onUpdate() {
        if (selectedUser == null) {
            showAlert("Select a user to update.");
            return;
        }
        onSave();
    }

    @FXML
    private void onSave() {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            showAlert("Username is required.");
            return;
        }

        try {
            if (selectedUser == null) {
                // Create new user
                String pw = passwordField.getText().trim();
                if (pw.isEmpty()) {
                    showAlert("Password is required for new users.");
                    return;
                }
                User u = new User();
                u.setUsername(username);
                u.setFullName(fullNameField.getText().trim());
                u.setEmail(emailField.getText().trim());
                u.setActive(activeCheck.isSelected());
                long newId = userDao.insert(u, PasswordUtil.hash(pw));
                // assign roles
                assignSelectedRoles(newId);
                showInfo("User created successfully.");
            } else {
                // Update existing
                selectedUser.setUsername(username);
                selectedUser.setFullName(fullNameField.getText().trim());
                selectedUser.setEmail(emailField.getText().trim());
                selectedUser.setActive(activeCheck.isSelected());
                userDao.update(selectedUser);
                // update roles
                userDao.removeAllRoles(selectedUser.getId());
                assignSelectedRoles(selectedUser.getId());
                // update password if provided
                String pw = passwordField.getText().trim();
                if (!pw.isEmpty()) {
                    userDao.updatePassword(selectedUser.getId(), PasswordUtil.hash(pw));
                }
                showInfo("User updated successfully.");
            }
            onClear();
            loadUsers();
        } catch (SQLException e) {
            showAlert("Save error: " + e.getMessage());
        }
    }

    @FXML
    private void onResetPassword() {
        if (selectedUser == null) {
            showAlert("Select a user first.");
            return;
        }
        String pw = passwordField.getText().trim();
        if (pw.isEmpty()) {
            showAlert("Enter a new password in the Password field.");
            return;
        }
        try {
            userDao.updatePassword(selectedUser.getId(), PasswordUtil.hash(pw));
            showInfo("Password reset for " + selectedUser.getUsername());
            passwordField.clear();
        } catch (SQLException e) {
            showAlert("Password reset error: " + e.getMessage());
        }
    }

    @FXML
    private void onDelete() {
        if (selectedUser == null) {
            showAlert("Select a user to delete.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete user '" + selectedUser.getUsername() + "'?",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    userDao.removeAllRoles(selectedUser.getId());
                    userDao.delete(selectedUser.getId());
                    showInfo("User deleted.");
                    onClear();
                    loadUsers();
                } catch (SQLException e) {
                    showAlert("Delete error: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void onClear() {
        selectedUser = null;
        userTable.getSelectionModel().clearSelection();
        usernameField.clear();
        fullNameField.clear();
        emailField.clear();
        passwordField.clear();
        activeCheck.setSelected(true);
        roleCheckBoxes.values().forEach(cb -> cb.setSelected(false));
    }

    private void assignSelectedRoles(long userId) throws SQLException {
        for (Map.Entry<Long, CheckBox> entry : roleCheckBoxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                userDao.assignRole(userId, entry.getKey());
            }
        }
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK).showAndWait();
    }

    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }
}
