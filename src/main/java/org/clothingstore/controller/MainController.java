package org.clothingstore.controller;

import org.clothingstore.model.Product;
import org.clothingstore.model.Review;
import org.clothingstore.service.CartService;
import org.clothingstore.service.ProductService;
import org.clothingstore.service.ReviewService;
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
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToggleButton;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML private Button adminBtn;
    @FXML private Label cartBadge;
    @FXML private Label userLabel;
    @FXML private TextField searchField;
    @FXML private VBox categoryBar;
    @FXML private FlowPane productGrid;
    @FXML private VBox overlayBg;
    @FXML private VBox detailPanel;

    private final ProductService productService = new ProductService();
    private final CartService cartService = new CartService();
    private final ReviewService reviewService = new ReviewService();

    private ToggleGroup sizeToggleGroup;
    private Product currentProduct;
    private int currentQuantity = 1;
    private int selectedRating = 0;
    private Button[] starButtons = new Button[5];

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        System.out.println("🚀 Главный экран загружен");
        System.out.println("📧 Email: " + SessionManager.getUserEmail());
        if (SessionManager.getProfile() != null) {
            System.out.println("🔑 Роль: " + SessionManager.getProfile().getRole());
        }

        updateUIBasedOnRole();
        loadCategories();
        loadProducts();
    }

    private void updateUIBasedOnRole() {
        if (adminBtn != null) {
            boolean canSee = SessionManager.isAdmin();
            adminBtn.setVisible(canSee);
            adminBtn.setManaged(canSee);
        }
        if (userLabel != null && SessionManager.getProfile() != null) {
            userLabel.setText(SessionManager.getProfile().getFullName());
        }
    }

    private void loadCategories() {
        categoryBar.getChildren().clear();
        String[] categories = {"Все", "Футболки", "Джинсы", "Куртки", "Свитера", "Аксессуары", "Обувь"};
        for (String cat : categories) {
            Button btn = new Button(cat);
            btn.getStyleClass().add("cat-btn");
            if (cat.equals("Все")) btn.getStyleClass().add("cat-btn-active");
            final String selectedCat = cat;
            btn.setOnAction(e -> {
                for (var child : categoryBar.getChildren()) {
                    if (child instanceof Button) ((Button) child).getStyleClass().remove("cat-btn-active");
                }
                btn.getStyleClass().add("cat-btn-active");
            });
            categoryBar.getChildren().add(btn);
        }
    }

    private void loadProducts() {
        productGrid.getChildren().clear();
        Label loading = new Label("Загрузка товаров...");
        loading.setStyle("-fx-text-fill:#8888aa; -fx-font-size:16px;");
        productGrid.getChildren().add(loading);

        new Thread(() -> {
            try {
                List<Product> products = productService.getAll();
                Platform.runLater(() -> {
                    productGrid.getChildren().clear();
                    if (products == null || products.isEmpty()) {
                        Label empty = new Label("Товары не найдены");
                        empty.setStyle("-fx-text-fill:#55556a; -fx-font-size:16px;");
                        productGrid.getChildren().add(empty);
                        return;
                    }
                    for (Product p : products) {
                        if (p.isActive()) productGrid.getChildren().add(createProductCard(p));
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private VBox createProductCard(Product p) {
        VBox card = new VBox(0);
        card.setPrefWidth(220);
        card.getStyleClass().add("product-card");

        StackPane imgBox = new StackPane();
        imgBox.setPrefHeight(220);
        imgBox.setStyle("-fx-background-color:#1a1a2e; -fx-background-radius:16 16 0 0;");
        Label placeholder = new Label("👕");
        placeholder.setStyle("-fx-font-size:64px;");
        imgBox.getChildren().add(placeholder);

        if (p.getImageUrl() != null && !p.getImageUrl().isBlank()) {
            new Thread(() -> {
                try {
                    Image img = new Image(p.getImageUrl(), 220, 220, true, true, true);
                    Platform.runLater(() -> {
                        ImageView iv = new ImageView(img);
                        iv.setFitWidth(220);
                        iv.setFitHeight(220);
                        imgBox.getChildren().setAll(iv);
                    });
                } catch (Exception ignored) {}
            }).start();
        }

        VBox info = new VBox(8);
        info.setPadding(new Insets(12, 14, 14, 14));
        info.setAlignment(Pos.CENTER);
        Label name = new Label(p.getName());
        name.setStyle("-fx-text-fill:#e8e8f0; -fx-font-size:14px; -fx-font-weight:bold;");
        name.setWrapText(true);
        name.setMaxWidth(192);
        name.setAlignment(Pos.CENTER);
        Label price = new Label(p.getPriceFormatted());
        price.setStyle("-fx-text-fill:#c9a96e; -fx-font-size:18px; -fx-font-weight:bold;");
        info.getChildren().addAll(name, price);

        Button btn = new Button("Подробнее");
        btn.getStyleClass().add("btn-outline");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> showProductDetails(p));

        card.getChildren().addAll(imgBox, info, btn);
        card.setOnMouseClicked(e -> showProductDetails(p));
        return card;
    }

    private void showProductDetails(Product p) {
        currentProduct = p;
        currentQuantity = 1;
        selectedRating = 0;

        // ✅ ИСПРАВЛЕНО: правильно центрируем панель
        overlayBg.getChildren().clear();
        overlayBg.setAlignment(Pos.CENTER);
        overlayBg.setPadding(new Insets(20));
        overlayBg.setStyle("-fx-background-color:rgba(0,0,0,0.75);");

        // Создаём панель с фиксированной шириной
        VBox contentPanel = new VBox(0);
        contentPanel.setStyle("-fx-background-color:#13131f; -fx-background-radius:20px;");
        contentPanel.setPrefWidth(520);
        contentPanel.setMaxWidth(520);
        contentPanel.setPadding(new Insets(0));

        // 1. ЗАГОЛОВОК С КРЕСТИКОМ
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 20, 20, 24));
        header.setStyle("-fx-border-color:#2a2a3e; -fx-border-width:0 0 1 0;");

        Label titleLabel = new Label(p.getName());
        titleLabel.setStyle("-fx-text-fill:#e8e8f0; -fx-font-size:18px; -fx-font-weight:bold;");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(400);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color:transparent; -fx-text-fill:#8888aa; -fx-font-size:22px; -fx-cursor:hand; -fx-min-width:36px; -fx-min-height:36px; -fx-background-radius:18px;");
        closeBtn.setOnAction(e -> {
            overlayBg.setVisible(false);
            overlayBg.setManaged(false);
        });
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-background-color:#2a2a3e; -fx-text-fill:#e8e8f0; -fx-font-size:22px; -fx-cursor:hand; -fx-min-width:36px; -fx-min-height:36px; -fx-background-radius:18px;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-background-color:transparent; -fx-text-fill:#8888aa; -fx-font-size:22px; -fx-cursor:hand; -fx-min-width:36px; -fx-min-height:36px; -fx-background-radius:18px;"));

        header.getChildren().addAll(titleLabel, closeBtn);

        // 2. ИЗОБРАЖЕНИЕ
        StackPane imgContainer = new StackPane();
        imgContainer.setPrefHeight(300);
        imgContainer.setPadding(new Insets(0, 24, 0, 24));
        imgContainer.setStyle("-fx-background-color:#1a1a2e; -fx-background-radius:12px;");
        imgContainer.setMaxWidth(472);

        if (p.getImageUrl() != null && !p.getImageUrl().isBlank()) {
            new Thread(() -> {
                try {
                    Image img = new Image(p.getImageUrl(), 472, 300, true, true, true);
                    Platform.runLater(() -> {
                        ImageView iv = new ImageView(img);
                        iv.setFitWidth(472);
                        iv.setFitHeight(300);
                        iv.setPreserveRatio(true);
                        iv.setSmooth(true);
                        Rectangle clip = new Rectangle(472, 300);
                        clip.setArcWidth(12);
                        clip.setArcHeight(12);
                        iv.setClip(clip);
                        imgContainer.getChildren().setAll(iv);
                    });
                } catch (Exception ignored) {}
            }).start();
        }

        // 3. ЦЕНА
        VBox priceBox = new VBox(8);
        priceBox.setPadding(new Insets(24, 24, 16, 24));
        Label priceLabel = new Label(p.getPriceFormatted());
        priceLabel.setStyle("-fx-text-fill:#c9a96e; -fx-font-size:32px; -fx-font-weight:bold;");
        priceBox.getChildren().add(priceLabel);

        // 4. ОПИСАНИЕ
        Label descLabel = new Label(p.getDescription() != null ? p.getDescription() : "");
        descLabel.setStyle("-fx-text-fill:#8888aa; -fx-font-size:14px; -fx-line-spacing:1.4;");
        descLabel.setWrapText(true);
        descLabel.setPadding(new Insets(0, 24, 20, 24));

        // 5. ПРОДАВЕЦ
        VBox sellerBox = new VBox(10);
        sellerBox.setPadding(new Insets(16, 24, 28, 24));
        sellerBox.setStyle("-fx-background-color:#1a1a2e; -fx-background-radius:12px; -fx-border-color:#2a2a3e; -fx-border-radius:12px; -fx-border-width:1;");

        Label sellerTitle = new Label("Продавец");
        sellerTitle.setStyle("-fx-text-fill:#55556a; -fx-font-size:11px; -fx-font-weight:bold;");

        HBox sellerRow = new HBox(14);
        sellerRow.setAlignment(Pos.CENTER_LEFT);

        StackPane avatarBox = new StackPane();
        avatarBox.setPrefSize(36, 36);
        avatarBox.setStyle("-fx-background-color:#2a2a3e; -fx-background-radius:50%;");
        Label initial = new Label("S");
        initial.setStyle("-fx-text-fill:#c9a96e; -fx-font-weight:bold; -fx-font-size:14px;");
        avatarBox.getChildren().add(initial);

        VBox sellerInfo = new VBox(2);
        Label sellerName = new Label("swear");
        sellerName.setStyle("-fx-text-fill:#e8e8f0; -fx-font-size:14px; -fx-font-weight:bold;");
        sellerInfo.getChildren().add(sellerName);

        HBox.setHgrow(sellerInfo, Priority.ALWAYS);

        Button profileBtn = new Button("Профиль →");
        profileBtn.getStyleClass().add("btn-outline-sm");
        profileBtn.setStyle("-fx-font-size:12px; -fx-padding:6 12 6 12;");
        profileBtn.setOnAction(e -> {
            overlayBg.setVisible(false);
            overlayBg.setManaged(false);
            String targetSellerId = currentProduct.getSellerId();
            if (targetSellerId == null || targetSellerId.isBlank()) {
                targetSellerId = SessionManager.getUserId();
            }
            NavigationManager.navigateToSeller(targetSellerId);
        });

        sellerRow.getChildren().addAll(avatarBox, sellerInfo, profileBtn);
        sellerBox.getChildren().addAll(sellerTitle, sellerRow);

        // 6. РАЗМЕРЫ
        VBox sizeSection = new VBox(10);
        sizeSection.setPadding(new Insets(8, 24, 20, 24));

        Label sizeTitle = new Label("ВЫБЕРИТЕ РАЗМЕР");
        sizeTitle.setStyle("-fx-text-fill:#c9a96e; -fx-font-size:11px; -fx-font-weight:bold;");

        sizeToggleGroup = new ToggleGroup();
        HBox sizeBox = new HBox(10);
        sizeBox.setAlignment(Pos.CENTER_LEFT);
        String[] sizes = {"S", "M", "L", "XL"};
        for (String size : sizes) {
            ToggleButton sizeBtn = new ToggleButton(size);
            sizeBtn.setToggleGroup(sizeToggleGroup);
            sizeBtn.getStyleClass().add("size-toggle");
            sizeBtn.setMinWidth(60);
            sizeBtn.setMaxHeight(40);
            sizeBtn.setStyle("-fx-font-size:13px;");
            sizeBox.getChildren().add(sizeBtn);
        }
        sizeSection.getChildren().addAll(sizeTitle, sizeBox);

        // 7. КОЛИЧЕСТВО
        VBox qtySection = new VBox(10);
        qtySection.setPadding(new Insets(0, 24, 24, 24));

        Label qtyTitle = new Label("КОЛИЧЕСТВО");
        qtyTitle.setStyle("-fx-text-fill:#c9a96e; -fx-font-size:11px; -fx-font-weight:bold;");

        HBox qtyBox = new HBox(14);
        qtyBox.setAlignment(Pos.CENTER_LEFT);

        Button minusBtn = new Button("−");
        minusBtn.getStyleClass().add("qty-btn");
        minusBtn.setMinSize(40, 40);
        minusBtn.setStyle("-fx-font-size:18px;");

        Label qtyNumLabel = new Label("1");
        qtyNumLabel.setStyle("-fx-text-fill:#e8e8f0; -fx-font-size:20px; -fx-font-weight:bold;");
        qtyNumLabel.setMinWidth(50);
        qtyNumLabel.setAlignment(Pos.CENTER);

        Button plusBtn = new Button("+");
        plusBtn.getStyleClass().add("qty-btn");
        plusBtn.setMinSize(40, 40);
        plusBtn.setStyle("-fx-font-size:18px;");

        minusBtn.setOnAction(e -> { if (currentQuantity > 1) { currentQuantity--; qtyNumLabel.setText(String.valueOf(currentQuantity)); } });
        plusBtn.setOnAction(e -> { currentQuantity++; qtyNumLabel.setText(String.valueOf(currentQuantity)); });

        qtyBox.getChildren().addAll(minusBtn, qtyNumLabel, plusBtn);
        qtySection.getChildren().addAll(qtyTitle, qtyBox);

        // 8. КНОПКА В КОРЗИНУ
        Button addToCartBtn = new Button("Добавить в корзину");
        addToCartBtn.getStyleClass().add("btn-primary");
        addToCartBtn.setMaxWidth(Double.MAX_VALUE);
        addToCartBtn.setPadding(new Insets(14, 24, 14, 24));
        addToCartBtn.setMinHeight(52);
        addToCartBtn.setStyle("-fx-font-size:15px; -fx-font-weight:bold;");
        addToCartBtn.setOnAction(e -> addToCart(currentProduct, currentQuantity));

        // 9. ОТЗЫВЫ
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:#2a2a3e;");
        sep.setPadding(new Insets(0, 24, 0, 24));

        VBox reviewsSection = new VBox(12);
        reviewsSection.setPadding(new Insets(20, 24, 24, 24));

        Label reviewsTitle = new Label("ОТЗЫВЫ");
        reviewsTitle.setStyle("-fx-text-fill:#c9a96e; -fx-font-size:11px; -fx-font-weight:bold;");

        VBox reviewsContainer = new VBox(10);
        reviewsContainer.setPadding(new Insets(0, 0, 0, 0));

        Label reviewsStatus = new Label("Отзывов пока нет");
        reviewsStatus.setStyle("-fx-text-fill:#55556a; -fx-font-size:13px;");

        HBox ratingBox = new HBox(8);
        ratingBox.setPadding(new Insets(8, 0, 12, 0));
        for (int i = 1; i <= 5; i++) {
            Button star = new Button("★");
            star.setStyle("-fx-background-color:transparent; -fx-text-fill:#55556a; -fx-font-size:32px; -fx-cursor:hand; -fx-min-width:40px; -fx-min-height:40px; -fx-padding:0;");
            final int rating = i;
            star.setOnAction(e -> {
                selectedRating = rating;
                for (int j = 0; j < 5; j++) {
                    if (j < rating) {
                        starButtons[j].setStyle("-fx-background-color:transparent; -fx-text-fill:#c9a96e; -fx-font-size:32px; -fx-cursor:hand; -fx-min-width:40px; -fx-min-height:40px; -fx-padding:0;");
                    } else {
                        starButtons[j].setStyle("-fx-background-color:transparent; -fx-text-fill:#55556a; -fx-font-size:32px; -fx-cursor:hand; -fx-min-width:40px; -fx-min-height:40px; -fx-padding:0;");
                    }
                }
            });
            starButtons[i - 1] = star;
            ratingBox.getChildren().add(star);
        }

        TextField reviewField = new TextField();
        reviewField.setPromptText("Написать отзыв...");
        reviewField.setStyle("-fx-background-color:#1a1a2e; -fx-text-fill:#e8e8f0; -fx-border-color:#2a2a3e; -fx-border-radius:8px; -fx-padding:10px; -fx-font-size:13px;");
        HBox.setHgrow(reviewField, Priority.ALWAYS);

        Button submitReviewBtn = new Button("Отправить");
        submitReviewBtn.getStyleClass().add("btn-primary");
        submitReviewBtn.setStyle("-fx-font-size:13px; -fx-padding:10 16 10 16;");
        submitReviewBtn.setOnAction(e -> {
            String text = reviewField.getText().trim();
            if (selectedRating == 0) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Ошибка");
                alert.setHeaderText(null);
                alert.setContentText("Пожалуйста, выберите рейтинг (звёзды)");
                alert.showAndWait();
                return;
            }
            if (!text.isEmpty()) {
                new Thread(() -> {
                    try {
                        reviewService.addReview(currentProduct.getId(), selectedRating, text);
                        Platform.runLater(() -> {
                            reviewField.clear();
                            selectedRating = 0;
                            for (Button star : starButtons) {
                                star.setStyle("-fx-background-color:transparent; -fx-text-fill:#55556a; -fx-font-size:32px; -fx-cursor:hand; -fx-min-width:40px; -fx-min-height:40px; -fx-padding:0;");
                            }
                            reviewsStatus.setText("Спасибо за отзыв!");
                            loadReviews(currentProduct.getId(), reviewsContainer, reviewsStatus);
                        });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Ошибка");
                            alert.setHeaderText(null);
                            alert.setContentText("Не удалось сохранить отзыв: " + ex.getMessage());
                            alert.showAndWait();
                        });
                    }
                }).start();
            }
        });

        HBox reviewInput = new HBox(10);
        reviewInput.getChildren().addAll(reviewField, submitReviewBtn);
        reviewsContainer.getChildren().addAll(reviewsStatus, ratingBox, reviewInput);
        reviewsSection.getChildren().addAll(reviewsTitle, reviewsContainer);

        // СОБИРАЕМ ВСЁ В contentPanel
        contentPanel.getChildren().addAll(
                header,
                imgContainer,
                priceBox,
                descLabel,
                sellerBox,
                sizeSection,
                qtySection,
                addToCartBtn,
                sep,
                reviewsSection
        );

        // ✅ ИСПРАВЛЕНО: оборачиваем в HBox для центрирования
        ScrollPane scrollPane = new ScrollPane(contentPanel);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefWidth(540);
        scrollPane.setPrefHeight(700);
        scrollPane.setMaxHeight(700);
        scrollPane.setStyle("-fx-background-color:transparent; -fx-background:transparent; -fx-border-color:transparent;");
        scrollPane.setPadding(new Insets(0));
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        // ✅ Центрируем ScrollPane внутри HBox
        HBox centerWrapper = new HBox(scrollPane);
        centerWrapper.setAlignment(Pos.CENTER);
        centerWrapper.setPadding(new Insets(0));

        overlayBg.getChildren().add(centerWrapper);
        overlayBg.setVisible(true);
        overlayBg.setManaged(true);
    }

    private void loadReviews(int productId, VBox reviewsContainer, Label statusLabel) {
        new Thread(() -> {
            try {
                List<Review> reviews = reviewService.getByProduct(productId);
                Platform.runLater(() -> {
                    reviewsContainer.getChildren().removeIf(node ->
                            node instanceof VBox && ((VBox)node).getStyleClass().contains("review-item"));

                    if (reviews != null && !reviews.isEmpty()) {
                        statusLabel.setText("Отзывы (" + reviews.size() + "):");
                        for (Review r : reviews) {
                            VBox reviewItem = new VBox(4);
                            reviewItem.getStyleClass().add("review-item");
                            reviewItem.setPadding(new Insets(8, 0, 8, 0));
                            reviewItem.setStyle("-fx-border-color:#2a2a3e; -fx-border-radius:8px; -fx-padding:8px;");

                            Label starsLabel = new Label(r.getStars());
                            starsLabel.setStyle("-fx-text-fill:#c9a96e; -fx-font-size:18px;");

                            Label commentLabel = new Label(r.getComment());
                            commentLabel.setStyle("-fx-text-fill:#e8e8f0; -fx-font-size:13px;");
                            commentLabel.setWrapText(true);

                            reviewItem.getChildren().addAll(starsLabel, commentLabel);
                            reviewsContainer.getChildren().add(1, reviewItem);
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void addToCart(Product product, int quantity) {
        ToggleButton selectedSize = (ToggleButton) sizeToggleGroup.getSelectedToggle();
        if (selectedSize == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Выберите размер");
            alert.setHeaderText(null);
            alert.setContentText("Пожалуйста, выберите размер товара");
            alert.showAndWait();
            return;
        }
        String size = selectedSize.getText();
        new Thread(() -> {
            try {
                cartService.addToCart(product.getId(), size, quantity);
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Успешно");
                    alert.setHeaderText(null);
                    alert.setContentText(product.getName() + " (размер " + size + ") добавлен в корзину!");
                    alert.showAndWait();
                    overlayBg.setVisible(false);
                    overlayBg.setManaged(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Ошибка");
                    alert.setHeaderText(null);
                    alert.setContentText("Не удалось добавить в корзину: " + e.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
    }

    @FXML public void handleAdmin() { NavigationManager.navigateTo("admin"); }
    @FXML public void handleProfile() { NavigationManager.navigateTo("profile"); }
    @FXML public void handleCart() { NavigationManager.navigateTo("cart"); }
    @FXML public void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Выход");
        alert.setHeaderText("Вы действительно хотите выйти?");
        alert.setContentText("Все несохраненные данные будут потеряны.");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                SessionManager.logout();
                NavigationManager.navigateTo("login");
            }
        });
    }
}