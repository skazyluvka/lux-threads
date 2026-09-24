package org.clothingstore.controller;

import org.clothingstore.model.CartItem;
import org.clothingstore.model.PromoCode;
import org.clothingstore.service.CartService;
import org.clothingstore.util.NavigationManager;
import org.clothingstore.util.SessionManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class CartController implements Initializable {

    @FXML private VBox        cartItemsBox;
    @FXML private Label       emptyLabel;
    @FXML private Label       totalLabel;
    @FXML private Button      checkoutBtn;
    @FXML private TextField   promoField;
    @FXML private Label       promoStatusLabel;
    @FXML private Label       subtotalLabel;
    @FXML private Label       discountLabel;
    @FXML private VBox        summaryBox;

    private final CartService cartService = new CartService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadCart();
    }

    private void loadCart() {
        if (cartItemsBox == null) {
            System.err.println("ОШИБКА: cartItemsBox не найден!");
            return;
        }

        cartItemsBox.getChildren().clear();
        new Thread(() -> {
            try {
                List<CartItem> items = cartService.getCart();
                Platform.runLater(() -> {
                    if (cartItemsBox == null) return;

                    cartItemsBox.getChildren().clear();
                    if (items.isEmpty()) {
                        if (emptyLabel != null) {
                            emptyLabel.setVisible(true);
                            emptyLabel.setManaged(true);
                        }
                        updateTotal(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
                        if (checkoutBtn != null) checkoutBtn.setDisable(true);
                        return;
                    }

                    if (emptyLabel != null) {
                        emptyLabel.setVisible(false);
                        emptyLabel.setManaged(false);
                    }
                    if (checkoutBtn != null) checkoutBtn.setDisable(false);

                    for (CartItem item : items) {
                        cartItemsBox.getChildren().add(makeCartRow(item));
                    }
                    recalculate(items);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private HBox makeCartRow(CartItem item) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 16, 12, 16));
        row.getStyleClass().add("cart-row");

        // Изображение
        StackPane imgBox = new StackPane();
        imgBox.setPrefSize(70, 70);
        imgBox.setMinSize(70, 70);
        imgBox.setMaxSize(70, 70);
        imgBox.setStyle("-fx-background-color:#13131f; -fx-background-radius:10px;");

        if (item.getProduct() != null && item.getProduct().getImageUrl() != null && !item.getProduct().getImageUrl().isBlank()) {
            new Thread(() -> {
                try {
                    Image img = new Image(item.getProduct().getImageUrl(), 70, 70, true, true, true);
                    Platform.runLater(() -> {
                        ImageView iv = new ImageView(img);
                        iv.setFitWidth(70);
                        iv.setFitHeight(70);
                        Rectangle clip = new Rectangle(70, 70);
                        clip.setArcWidth(20);
                        clip.setArcHeight(20);
                        iv.setClip(clip);
                        imgBox.getChildren().setAll(iv);
                    });
                } catch (Exception ignored) {}
            }).start();
        }

        // Информация о товаре
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        String name = item.getProduct() != null ? item.getProduct().getName() : "Товар #" + item.getProductId();
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-text-fill:#e8e8f0; -fx-font-size:14px; -fx-font-weight:bold;");
        nameLabel.setWrapText(true);

        Label sizeLabel = new Label("Размер: " + (item.getSize() != null ? item.getSize() : "—"));
        sizeLabel.setStyle("-fx-text-fill:#8888aa; -fx-font-size:12px;");

        String priceStr = item.getProduct() != null ? item.getProduct().getPriceFormatted() : "—";
        Label priceLabel = new Label(priceStr);
        priceLabel.setStyle("-fx-text-fill:#c9a96e; -fx-font-size:13px;");

        info.getChildren().addAll(nameLabel, sizeLabel, priceLabel);

        // Управление количеством
        HBox qtyBox = new HBox(6);
        qtyBox.setAlignment(Pos.CENTER);

        Label qtyLabel = new Label(String.valueOf(item.getQuantity()));
        qtyLabel.setStyle("-fx-text-fill:#e8e8f0; -fx-font-size:14px; -fx-font-weight:bold; -fx-min-width:24px; -fx-alignment:center;");

        Button minusBtn = new Button("−");
        minusBtn.getStyleClass().add("qty-btn");
        minusBtn.setOnAction(e -> changeQty(item, item.getQuantity() - 1, qtyLabel));

        Button plusBtn = new Button("+");
        plusBtn.getStyleClass().add("qty-btn");
        plusBtn.setOnAction(e -> changeQty(item, item.getQuantity() + 1, qtyLabel));

        qtyBox.getChildren().addAll(minusBtn, qtyLabel, plusBtn);

        // ✅ НОВАЯ КНОПКА ПОЛНОГО УДАЛЕНИЯ
        Button deleteBtn = new Button("Удалить");
        deleteBtn.setStyle(
                "-fx-background-color: #f05050;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;" +
                        "-fx-background-radius: 6px;" +
                        "-fx-padding: 6 12 6 12;"
        );
        deleteBtn.setOnAction(e -> confirmAndRemoveItem(item));

        row.getChildren().addAll(imgBox, info, qtyBox, deleteBtn);
        return row;
    }

    private void changeQty(CartItem item, int newQty, Label qtyLabel) {
        if (newQty < 1) {
            // Если количество меньше 1, сразу удаляем товар с подтверждением
            confirmAndRemoveItem(item);
            return;
        }
        new Thread(() -> {
            try {
                cartService.updateQuantity(item.getId(), newQty);
                item.setQuantity(newQty);
                Platform.runLater(() -> {
                    if (qtyLabel != null) qtyLabel.setText(String.valueOf(newQty));
                    recalculateFromCart();
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert("Ошибка", e.getMessage()));
            }
        }).start();
    }

    // ✅ НОВЫЙ МЕТОД: Подтверждение и полное удаление
    private void confirmAndRemoveItem(CartItem item) {
        String itemName = item.getProduct() != null ? item.getProduct().getName() : "Этот товар";

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Удаление из корзины");
        alert.setHeaderText("Удалить \"" + itemName + "\" (размер " + item.getSize() + ")?");
        alert.setContentText("Товар будет полностью удалён из вашей корзины.");

        // Стилизация окна под темную тему (опционально)
        alert.getDialogPane().setStyle("-fx-background-color: #1a1a2e; -fx-text-fill: #e8e8f0;");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                removeItem(item);
            }
        });
    }

    private void removeItem(CartItem item) {
        new Thread(() -> {
            try {
                cartService.removeFromCart(item.getId());
                Platform.runLater(this::loadCart);
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert("Ошибка", "Не удалось удалить товар: " + e.getMessage()));
            }
        }).start();
    }

    private void recalculate(List<CartItem> items) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : items) {
            subtotal = subtotal.add(item.getTotalPrice());
        }

        BigDecimal discount = BigDecimal.ZERO;
        PromoCode promo = SessionManager.getAppliedPromo();
        if (promo != null) {
            discount = subtotal.multiply(BigDecimal.valueOf(promo.getDiscountPercent()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        BigDecimal total = subtotal.subtract(discount);
        updateTotal(subtotal, discount, total);
    }

    private void recalculateFromCart() {
        new Thread(() -> {
            try {
                List<CartItem> items = cartService.getCart();
                BigDecimal subtotal = BigDecimal.ZERO;
                for (CartItem item : items) {
                    subtotal = subtotal.add(item.getTotalPrice());
                }

                BigDecimal discount = BigDecimal.ZERO;
                PromoCode promo = SessionManager.getAppliedPromo();
                if (promo != null) {
                    discount = subtotal.multiply(BigDecimal.valueOf(promo.getDiscountPercent()))
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                }

                BigDecimal total = subtotal.subtract(discount);

                final BigDecimal fSubtotal = subtotal;
                final BigDecimal fDiscount = discount;
                final BigDecimal fTotal = total;

                Platform.runLater(() -> updateTotal(fSubtotal, fDiscount, fTotal));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void updateTotal(BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
        if (subtotalLabel != null) {
            subtotalLabel.setText(subtotal.intValue() + " ₽");
        }
        if (discountLabel != null) {
            discountLabel.setText(discount.compareTo(BigDecimal.ZERO) > 0 ? "−" + discount.intValue() + " ₽" : "—");
        }
        if (totalLabel != null) {
            totalLabel.setText(total.intValue() + " ₽");
        }
    }

    @FXML
    public void handleCheckout()  {
        NavigationManager.navigateTo("checkout");
    }

    @FXML
    public void handleBack() {
        NavigationManager.navigateTo("main");
    }

    @FXML
    public void handleClearCart() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Очистка корзины");
        alert.setHeaderText("Очистить всю корзину?");
        alert.setContentText("Все товары будут удалены. Это действие нельзя отменить.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        cartService.clearCart();
                        SessionManager.setAppliedPromo(null);
                        Platform.runLater(() -> {
                            if (promoStatusLabel != null) promoStatusLabel.setVisible(false);
                            loadCart();
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        });
    }

    @FXML
    public void handleApplyPromo() {
        String code = promoField != null ? promoField.getText().trim() : "";
        if (code.isEmpty()) {
            showAlert("Введите промокод", "Пожалуйста, введите промокод в поле.");
            return;
        }

        PromoCode promo = new PromoCode();
        promo.setCode(code);
        promo.setDiscountPercent(10); // Заглушка 10%

        SessionManager.setAppliedPromo(promo);

        if (promoStatusLabel != null) {
            promoStatusLabel.setText("✅ Промокод «" + code + "» применен (-10%)");
            promoStatusLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-weight: bold;");
            promoStatusLabel.setVisible(true);
        }

        recalculateFromCart();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}