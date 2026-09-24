package org.clothingstore.controller;

import org.clothingstore.service.AuthService;
import org.clothingstore.util.NavigationManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class LoginController {

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;
    @FXML private Button        loginBtn;
    @FXML private VBox          root;

    private final AuthService authService = new AuthService();

    @FXML
    public void handleLogin() {
        String email = emailField.getText().trim();
        String pass  = passwordField.getText();
        if (email.isEmpty() || pass.isEmpty()) {
            showError("Заполните все поля");
            return;
        }
        loginBtn.setDisable(true);
        loginBtn.setText("Вход...");
        new Thread(() -> {
            try {
                boolean ok = authService.login(email, pass);
                Platform.runLater(() -> {
                    if (ok) NavigationManager.navigateTo("main");
                    else    showError("Неверный email или пароль");
                    loginBtn.setDisable(false);
                    loginBtn.setText("Войти");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError(e.getClass().getSimpleName() + ": " + e.getMessage());
                    loginBtn.setDisable(false);
                    loginBtn.setText("Войти");
                });
            }
        }).start();
    }

    @FXML public void handleRegister() { NavigationManager.navigateTo("register"); }

    @FXML public void handleForgotPassword() {
        String email = emailField.getText().trim();
        if (email.isEmpty()) { showError("Введите email для сброса пароля"); return; }
        new Thread(() -> {
            try {
                authService.sendPasswordReset(email);
                Platform.runLater(() ->
                        showSuccess("Письмо для сброса пароля отправлено на " + email));
            } catch (Exception e) {
                Platform.runLater(() -> showError("Ошибка: " + e.getMessage()));
            }
        }).start();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.getStyleClass().removeAll("success-label");
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setVisible(true);
    }

    private void showSuccess(String msg) {
        errorLabel.setText(msg);
        errorLabel.getStyleClass().removeAll("error-label");
        errorLabel.getStyleClass().add("success-label");
        errorLabel.setVisible(true);
    }
}