package com.databrew.cafe.controller;

import com.databrew.cafe.model.User;
import com.databrew.cafe.service.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;

    private final AuthService authService = new AuthService();

    @FXML
    public void onLogin(ActionEvent event) {
        try {
            User user = authService.authenticate(usernameField.getText(), passwordField.getText());
            if (user == null) {
                errorLabel.setText("Invalid credentials");
                return;
            }
            Stage stage = (Stage) usernameField.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/DashboardView.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());
            com.databrew.cafe.controller.DashboardController controller = loader.getController();
            if (controller != null) {
                controller.setCurrentUser(user);
            }
            stage.setScene(scene);
            stage.setTitle("DataBrew Cafe Dashboard");
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Login failed: " + e.getMessage()).showAndWait();
        }
    }
}
