package org.clothingstore.controller;

import org.clothingstore.model.Order;
import org.clothingstore.model.SellerProfile;
import org.clothingstore.model.UserProfile;
import org.clothingstore.service.AuthService;
import org.clothingstore.service.OrderService;
import org.clothingstore.service.SellerService;
import org.clothingstore.service.SupabaseClient;
import org.clothingstore.util.NavigationManager;
import org.clothingstore.util.SessionManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class ProfileController extends SupabaseClient implements Initializable {

    @FXML private Label emailLabel;
    @FXML private Label roleLabel;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField phoneField;
    @FXML private TextField addressField;
    @FXML private Label profileMsgLabel;

    @FXML private TextField resetEmailField;
    @FXML private Label resetMsgLabel;

    @FXML private VBox ordersBox;
    @FXML private Label ordersEmptyLabel;

    @FXML private VBox sellerBecomeCard;
    @FXML private TextField becomeSellerNameField;
    @FXML private TextArea becomeSellerBioArea;
    @FXML private Label becomeMsgLabel;

    @FXML private VBox sellerProfileCard;
    @FXML private Label sellerRatingLabel;
    @FXML private TextField editSellerNameField;
    @FXML private TextArea editSellerBioArea;
    @FXML private TextField editSellerAvatarField;
    @FXML private Label editSellerMsgLabel;

    private final AuthService authService = new AuthService();
    private final OrderService orderService = new OrderService();
    private final SellerService sellerService = new SellerService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        emailLabel.setText(SessionManager.getUserEmail());

        UserProfile p = SessionManager.getProfile();
        if (p != null) {
            roleLabel.setText("admin".equals(p.getRole())
                    ? "👑 Администратор" : "👤 Пользователь");
            firstNameField.setText(nvl(p.getFirstName()));
            lastNameField.setText(nvl(p.getLastName()));
            phoneField.setText(nvl(p.getPhone()));
            addressField.setText(nvl(p.getAddress()));
        }

        resetEmailField.setText(SessionManager.getUserEmail());

        phoneField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("[0-9+\\-() ]*")) {
                phoneField.setText(oldVal);
            }
        });

        phoneField.lengthProperty().addListener((obs, oldLen, newLen) -> {
            if (newLen.intValue() > 18) {
                phoneField.setText(phoneField.getText().substring(0, 18));
            }
        });

        loadOrders();
        loadSellerSection();
    }

    @FXML
    public void handleSaveProfile() {
        String uid = SessionManager.getUserId();

        Map<String, Object> body = new HashMap<>();
        body.put("first_name", firstNameField.getText().trim());
        body.put("last_name", lastNameField.getText().trim());
        body.put("phone", phoneField.getText().trim());
        body.put("address", addressField.getText().trim());

        new Thread(() -> {
            try {
                patch("/user_profiles?id=eq." + uid, body);

                UserProfile p = SessionManager.getProfile();
                if (p != null) {
                    p.setFirstName(firstNameField.getText().trim());
                    p.setLastName(lastNameField.getText().trim());
                    p.setPhone(phoneField.getText().trim());
                    p.setAddress(addressField.getText().trim());
                }

                Platform.runLater(() ->
                        showMsg(profileMsgLabel, "✓ Профиль успешно сохранён", true));
            } catch (Exception e) {
                Platform.runLater(() ->
                        showMsg(profileMsgLabel, "Ошибка: " + e.getMessage(), false));
            }
        }).start();
    }

    @FXML
    public void handleResetPassword() {
        String email = resetEmailField.getText().trim();
        if (email.isEmpty()) {
            showMsg(resetMsgLabel, "Введите email", false);
            return;
        }

        new Thread(() -> {
            try {
                authService.sendPasswordReset(email);
                Platform.runLater(() ->
                        showMsg(resetMsgLabel, "✓ Письмо отправлено на " + email, true));
            } catch (Exception e) {
                Platform.runLater(() ->
                        showMsg(resetMsgLabel, "Ошибка: " + e.getMessage(), false));
            }
        }).start();
    }

    private void loadSellerSection() {
        new Thread(() -> {
            try {
                SellerProfile sp = sellerService.getById(SessionManager.getUserId());

                Platform.runLater(() -> {
                    if (sp != null && sp.isSeller()) {
                        showSellerProfileCard(sp);
                    } else {
                        showBecomeSellerCard();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(this::showBecomeSellerCard);
            }
        }).start();
    }

    private void showBecomeSellerCard() {
        sellerBecomeCard.setVisible(true);
        sellerBecomeCard.setManaged(true);
        sellerProfileCard.setVisible(false);
        sellerProfileCard.setManaged(false);
    }

    private void showSellerProfileCard(SellerProfile sp) {
        sellerProfileCard.setVisible(true);
        sellerProfileCard.setManaged(true);
        sellerBecomeCard.setVisible(false);
        sellerBecomeCard.setManaged(false);

        editSellerNameField.setText(nvl(sp.getSellerName()));
        editSellerBioArea.setText(nvl(sp.getSellerBio()));
        editSellerAvatarField.setText(nvl(sp.getSellerAvatarUrl()));

        sellerRatingLabel.setText("Загрузка рейтинга...");

        new Thread(() -> {
            try {
                double rating = sellerService.getSellerRating(sp.getId());
                Platform.runLater(() ->
                        sellerRatingLabel.setText(rating > 0
                                ? String.format("⭐ %.1f — средний рейтинг", rating)
                                : "Оценок пока нет"));
            } catch (Exception ignored) {
                Platform.runLater(() -> sellerRatingLabel.setText("—"));
            }
        }).start();
    }

    @FXML
    public void handleBecomeSeller() {
        String name = becomeSellerNameField.getText().trim();
        String bio = becomeSellerBioArea.getText().trim();

        if (name.isBlank()) {
            showMsg(becomeMsgLabel, "Введите название магазина или ник", false);
            return;
        }

        new Thread(() -> {
            try {
                sellerService.becomeSeller(
                        SessionManager.getUserId(),
                        name,
                        bio,
                        null
                );

                SellerProfile sp = sellerService.getById(SessionManager.getUserId());

                Platform.runLater(() -> {
                    if (sp != null) {
                        showSellerProfileCard(sp);
                    } else {
                        showMsg(becomeMsgLabel, "✓ Вы стали продавцом! Обновите страницу.", true);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                        showMsg(becomeMsgLabel, "Ошибка: " + e.getMessage(), false));
            }
        }).start();
    }

    @FXML
    public void handleOpenSellerProfile() {
        NavigationManager.navigateToSeller(SessionManager.getUserId());
    }

    @FXML
    public void handleUpdateSellerProfile() {
        String name = editSellerNameField.getText().trim();
        String bio = editSellerBioArea.getText().trim();
        String avatar = editSellerAvatarField.getText().trim();

        if (name.isBlank()) {
            showMsg(editSellerMsgLabel, "Введите название магазина", false);
            return;
        }

        new Thread(() -> {
            try {
                sellerService.updateSellerProfile(
                        SessionManager.getUserId(),
                        name,
                        bio,
                        avatar.isBlank() ? null : avatar
                );

                Platform.runLater(() ->
                        showMsg(editSellerMsgLabel, "✓ Профиль продавца сохранён", true));
            } catch (Exception e) {
                Platform.runLater(() ->
                        showMsg(editSellerMsgLabel, "Ошибка: " + e.getMessage(), false));
            }
        }).start();
    }

    private void loadOrders() {
        new Thread(() -> {
            try {
                List<Order> orders = orderService.getUserOrders();

                Platform.runLater(() -> {
                    ordersBox.getChildren().clear();

                    if (orders.isEmpty()) {
                        ordersEmptyLabel.setVisible(true);
                        return;
                    }

                    ordersEmptyLabel.setVisible(false);

                    for (Order o : orders) {
                        ordersBox.getChildren().add(makeOrderCard(o));
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private VBox makeOrderCard(Order o) {
        VBox card = new VBox(6);
        card.getStyleClass().add("order-card");
        card.setPadding(new Insets(14, 16, 14, 16));

        HBox header = new HBox();
        header.setSpacing(12);

        Label idL = new Label("Заказ #" + o.getId());
        idL.setStyle("-fx-text-fill:#e8e8f0; -fx-font-weight:bold; -fx-font-size:14px;");
        HBox.setHgrow(idL, Priority.ALWAYS);

        Label statusL = new Label(translateStatus(o.getStatus()));
        statusL.getStyleClass().add("status-badge-" +
                (o.getStatus() != null ? o.getStatus() : "pending"));

        header.getChildren().addAll(idL, statusL);

        Label dateL = new Label("📅 " + (o.getCreatedAt() != null
                ? o.getCreatedAt().substring(0, 10) : "—"));
        dateL.setStyle("-fx-text-fill:#55556a; -fx-font-size:12px;");

        Label totalL = new Label("💳 " + o.getTotalPrice() + " ₽");
        totalL.setStyle("-fx-text-fill:#c9a96e; -fx-font-weight:bold;");

        Label addrL = new Label("📍 " + nvl(o.getDeliveryAddress()));
        addrL.setStyle("-fx-text-fill:#55556a; -fx-font-size:12px;");
        addrL.setWrapText(true);

        card.getChildren().addAll(header, dateL, totalL, addrL);
        return card;
    }

    private String translateStatus(String s) {
        return switch (s != null ? s : "") {
            case "pending" -> "⏳ Ожидает";
            case "processing" -> "⚙️ В обработке";
            case "shipped" -> "🚚 Отправлен";
            case "delivered" -> "✅ Доставлен";
            case "cancelled" -> "❌ Отменён";
            default -> s != null ? s : "—";
        };
    }

    @FXML
    public void handleBack() {
        NavigationManager.navigateTo("main");
    }

    @FXML
    public void handleLogout() {
        SessionManager.logout();
        NavigationManager.navigateTo("login");
    }

    private void showMsg(Label label, String msg, boolean ok) {
        label.setText(msg);
        label.getStyleClass().setAll(ok ? "success-label" : "error-label");
        label.setVisible(true);
    }

    private String nvl(String s) {
        return s != null ? s : "";
    }
}