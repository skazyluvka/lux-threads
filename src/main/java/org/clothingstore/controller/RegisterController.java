package org.clothingstore.controller;

import org.clothingstore.service.AuthService;
import org.clothingstore.util.NavigationManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class RegisterController {

    @FXML private TextField     emailField;
    @FXML private PasswordField passField;
    @FXML private PasswordField confirmField;
    @FXML private Label         msgLabel;
    @FXML private Button        registerBtn;

    private final AuthService authService = new AuthService();

    @FXML
    public void handleRegister() {
        String email   = emailField.getText().trim();
        String pass    = passField.getText();
        String confirm = confirmField.getText();

        if (email.isEmpty() || pass.isEmpty()) { showError("Заполните все поля"); return; }
        if (!pass.equals(confirm))             { showError("Пароли не совпадают"); return; }
        if (pass.length() < 6)                 { showError("Пароль минимум 6 символов"); return; }

        registerBtn.setDisable(true);
        registerBtn.setText("Регистрация...");
        new Thread(() -> {
            try {
                boolean ok = authService.register(email, pass);
                Platform.runLater(() -> {
                    if (ok) NavigationManager.navigateTo("main");
                    else    showError("Email уже используется");
                    registerBtn.setDisable(false);
                    registerBtn.setText("Создать аккаунт");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Ошибка регистрации: " + e.getMessage());
                    registerBtn.setDisable(false);
                    registerBtn.setText("Создать аккаунт");
                });
            }
        }).start();
    }

    @FXML public void handleBack() { NavigationManager.navigateTo("login"); }

    private void showError(String msg) {
        msgLabel.setText(msg);
        msgLabel.getStyleClass().setAll("error-label");
        msgLabel.setVisible(true);
    }
}
