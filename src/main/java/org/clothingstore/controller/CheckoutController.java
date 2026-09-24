package org.clothingstore.controller;

import org.clothingstore.model.CartItem;
import org.clothingstore.model.Order;
import org.clothingstore.model.PromoCode;
import org.clothingstore.service.CartService;
import org.clothingstore.service.OrderService;
import org.clothingstore.service.PromoCodeService;
import org.clothingstore.util.NavigationManager;
import org.clothingstore.util.SessionManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class CheckoutController implements Initializable {

    @FXML private TextField  nameField;
    @FXML private TextField  phoneField;
    @FXML private TextField  addressField;
    @FXML private TextArea   commentArea;
    @FXML private Label      subtotalLabel;
    @FXML private Label      discountLabel;
    @FXML private Label      totalLabel;
    @FXML private Label      promoLabel;
    @FXML private Button     placeOrderBtn;
    @FXML private VBox       orderSummaryBox;
    @FXML private Label      msgLabel;

    private final OrderService     orderService     = new OrderService();
    private final CartService      cartService      = new CartService();
    private final PromoCodeService promoCodeService = new PromoCodeService();

    // Данные будут загружены при инициализации
    private List<CartItem> currentCartItems;
    private PromoCode currentPromo;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Валидация телефона (только цифры, +, -, пробел, скобки)
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

        // Предзаполнение из профиля
        if (SessionManager.getProfile() != null) {
            var p = SessionManager.getProfile();
            nameField.setText(p.getFullName());
            phoneField.setText(p.getPhone() != null ? p.getPhone() : "");
            addressField.setText(p.getAddress() != null ? p.getAddress() : "");
        }

        // Загружаем данные корзины и промокод
        loadCheckoutData();
    }

    private void loadCheckoutData() {
        placeOrderBtn.setDisable(true);
        placeOrderBtn.setText("Загрузка данных...");

        new Thread(() -> {
            try {
                // 1. Загружаем корзину с сервера
                currentCartItems = cartService.getCart();
                // 2. Берем промокод из сессии (если он был применен в корзине)
                currentPromo = SessionManager.getAppliedPromo();

                Platform.runLater(() -> {
                    renderSummary();
                    placeOrderBtn.setDisable(false);
                    placeOrderBtn.setText("✓ Подтвердить заказ");
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showMsg("Ошибка загрузки корзины: " + e.getMessage(), false);
                    placeOrderBtn.setDisable(false);
                    placeOrderBtn.setText("✓ Подтвердить заказ");
                });
            }
        }).start();
    }

    private void renderSummary() {
        if (currentCartItems == null || currentCartItems.isEmpty()) {
            orderSummaryBox.getChildren().clear();
            orderSummaryBox.getChildren().add(new Label("Корзина пуста"));
            subtotalLabel.setText("0 ₽");
            discountLabel.setText("—");
            totalLabel.setText("0 ₽");
            placeOrderBtn.setDisable(true);
            return;
        }

        orderSummaryBox.getChildren().clear();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem item : currentCartItems) {
            String name = item.getProduct() != null
                    ? item.getProduct().getName() : "Товар #" + item.getProductId();

            Label l = new Label("• " + name + " × " + item.getQuantity()
                    + " [" + item.getSize() + "] — " + fmt(item.getTotalPrice()));
            l.setStyle("-fx-text-fill:#8888aa; -fx-font-size:13px;");
            l.setWrapText(true);
            orderSummaryBox.getChildren().add(l);

            subtotal = subtotal.add(item.getTotalPrice());
        }

        BigDecimal discount = BigDecimal.ZERO;
        if (currentPromo != null) {
            discount = subtotal
                    .multiply(BigDecimal.valueOf(currentPromo.getDiscountPercent()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            promoLabel.setText("✅ Промокод «" + currentPromo.getCode()
                    + "» −" + currentPromo.getDiscountPercent() + "%");
            promoLabel.setVisible(true);
        } else {
            promoLabel.setVisible(false);
        }

        subtotalLabel.setText(fmt(subtotal));
        discountLabel.setText(discount.compareTo(BigDecimal.ZERO) > 0 ? "−" + fmt(discount) : "—");
        totalLabel.setText(fmt(subtotal.subtract(discount)));
    }

    @FXML
    public void handlePlaceOrder() {
        String name    = nameField.getText().trim();
        String phone   = phoneField.getText().trim();
        String address = addressField.getText().trim();

        if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            showMsg("⚠ Заполните все обязательные поля (имя, телефон, адрес)", false);
            return;
        }
        if (phone.replaceAll("[^0-9]", "").length() < 10) {
            showMsg("⚠ Введите корректный номер телефона", false);
            return;
        }
        if (currentCartItems == null || currentCartItems.isEmpty()) {
            showMsg("⚠ Корзина пуста", false);
            return;
        }

        placeOrderBtn.setDisable(true);
        placeOrderBtn.setText("Оформление...");

        BigDecimal subtotal = currentCartItems.stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = BigDecimal.ZERO;
        if (currentPromo != null) {
            discount = subtotal
                    .multiply(BigDecimal.valueOf(currentPromo.getDiscountPercent()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        Order order = new Order();
        order.setUserId(SessionManager.getUserId());
        order.setTotalPrice(subtotal.subtract(discount));
        order.setDiscountAmount(discount);
        order.setPromoCode(currentPromo != null ? currentPromo.getCode() : null);
        order.setDeliveryAddress(address);
        order.setPhone(phone);
        order.setComment(commentArea.getText().trim());

        new Thread(() -> {
            try {
                Order created = orderService.createOrder(order, currentCartItems);

                if (currentPromo != null) {
                    try {
                        promoCodeService.incrementUses(currentPromo.getId());
                    } catch (Exception e) {
                        System.err.println("Не удалось обновить использование промокода: " + e.getMessage());
                    }
                }

                cartService.clearCart();
                SessionManager.setAppliedPromo(null); // Сбрасываем промокод после заказа

                Platform.runLater(() -> showSuccessDialog(created.getId(), fmt(order.getTotalPrice())));
            } catch (Exception e) {
                e.printStackTrace(); // <-- ЭТО ПОКАЖЕТ ОШИБКУ В КОНСОЛИ, ЕСЛИ ОНА БУДЕТ
                Platform.runLater(() -> {
                    showMsg("Ошибка оформления: " + e.getMessage(), false);
                    placeOrderBtn.setDisable(false);
                    placeOrderBtn.setText("✓ Подтвердить заказ");
                });
            }
        }).start();
    }

    private void showSuccessDialog(int orderId, String total) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Заказ оформлен!");
        alert.setHeaderText("🎉 Заказ №" + orderId + " успешно оформлен");
        alert.setContentText(
                "Сумма: " + total + "\n" +
                        "Статус: Ожидает обработки\n\n" +
                        "Мы свяжемся с вами в ближайшее время.\n" +
                        "(Это демо-режим — реальная оплата не производится)"
        );
        alert.showAndWait();

        NavigationManager.navigateTo("main");
    }

    @FXML
    public void handleBack() {
        NavigationManager.navigateTo("cart");
    }

    private void showMsg(String msg, boolean ok) {
        msgLabel.setText(msg);
        msgLabel.getStyleClass().setAll(ok ? "success-label" : "error-label");
        msgLabel.setVisible(true);
    }

    private String fmt(BigDecimal v) {
        return v.setScale(0, RoundingMode.HALF_UP).toPlainString() + " ₽";
    }
}