package org.clothingstore.controller;

import org.clothingstore.model.Post;
import org.clothingstore.model.PostComment;
import org.clothingstore.model.Product;
import org.clothingstore.model.ProductSize;
import org.clothingstore.model.SellerProfile;
import org.clothingstore.service.PostService;
import org.clothingstore.service.ProductService;
import org.clothingstore.service.SellerService;
import org.clothingstore.service.StorageService;
import org.clothingstore.util.ImageLoader;
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
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

public class SellerController implements Initializable {

    @FXML private Label sellerNameLabel;
    @FXML private Label ratingLabel;
    @FXML private Label bioLabel;
    @FXML private StackPane avatarBox;
    @FXML private VBox tabContent;
    @FXML private Button tabProducts;
    @FXML private Button tabPromoCodes;
    @FXML private Button tabPosts;

    private final SellerService sellerService = new SellerService();
    private final PostService postService = new PostService();
    private final ProductService productService = new ProductService();
    private final StorageService storageService = new StorageService();

    private SellerProfile seller;
    private String sellerId;
    private boolean initialized = false;

    public void setSellerId(String sellerId) {
        System.out.println("📥 SellerController получил sellerId: " + sellerId);
        this.sellerId = sellerId;
        if (initialized) {
            loadSeller();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initialized = true;
        sellerNameLabel.setText("Загрузка...");
        ratingLabel.setText("");
        bioLabel.setText("");
        bioLabel.setVisible(false);
        bioLabel.setManaged(false);

        if (sellerId != null && !sellerId.isBlank()) {
            loadSeller();
        }
    }

    private void loadSeller() {
        if (sellerId == null || sellerId.isBlank()) {
            sellerNameLabel.setText("Продавец");
            ratingLabel.setText("");
            bioLabel.setText("");
            bioLabel.setVisible(false);
            bioLabel.setManaged(false);
            handleTabProducts();
            return;
        }

        new Thread(() -> {
            try {
                System.out.println("🔄 Загрузка данных продавца из БД для ID: " + sellerId);
                seller = sellerService.getById(sellerId);

                Platform.runLater(() -> {
                    if (seller == null) {
                        System.out.println("⚠️ Продавец с таким ID не найден в БД");
                        sellerNameLabel.setText("Продавец");
                        ratingLabel.setText("");
                        bioLabel.setText("");
                        bioLabel.setVisible(false);
                        bioLabel.setManaged(false);
                        buildAvatarFallback("Продавец");
                        handleTabProducts();
                        return;
                    }

                    System.out.println("✅ Данные продавца загружены: " + seller.getFullName());

                    String displayName = seller.getDisplayName();
                    if (displayName == null || displayName.isBlank()) displayName = "Продавец";

                    sellerNameLabel.setText(displayName);
                    sellerNameLabel.setStyle("-fx-text-fill:#e8e8f0; -fx-font-size:22px; -fx-font-weight:bold;");

                    String bio = seller.getSellerBio();
                    if (bio != null && !bio.isBlank()) {
                        bioLabel.setText(bio);
                        bioLabel.setStyle("-fx-text-fill:#8888aa; -fx-font-size:13px;");
                        bioLabel.setWrapText(true);
                        bioLabel.setAlignment(Pos.CENTER);
                        bioLabel.setMaxWidth(Double.MAX_VALUE);
                        bioLabel.setVisible(true);
                        bioLabel.setManaged(true);
                    } else {
                        bioLabel.setText("");
                        bioLabel.setVisible(false);
                        bioLabel.setManaged(false);
                    }

                    ratingLabel.setText("Загрузка...");
                    ratingLabel.setStyle("-fx-text-fill:#c9a96e; -fx-font-size:14px;");

                    buildAvatar();
                    loadRating();
                    handleTabProducts();
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    sellerNameLabel.setText("Продавец");
                    ratingLabel.setText("");
                    bioLabel.setText("");
                    bioLabel.setVisible(false);
                    bioLabel.setManaged(false);
                    buildAvatarFallback("Продавец");
                    handleTabProducts();
                });
            }
        }).start();
    }

    private void buildAvatar() {
        String displayName = seller != null ? seller.getDisplayName() : "Продавец";
        if (displayName == null || displayName.isBlank()) displayName = "Продавец";
        buildAvatarFallback(displayName);

        if (seller != null && seller.getSellerAvatarUrl() != null && !seller.getSellerAvatarUrl().isBlank()) {
            new Thread(() -> {
                try {
                    Image img = new Image(seller.getSellerAvatarUrl(), 90, 90, true, true, true);
                    Platform.runLater(() -> {
                        ImageView iv = new ImageView(img);
                        iv.setFitWidth(90);
                        iv.setFitHeight(90);
                        Circle clip = new Circle(45, 45, 45);
                        iv.setClip(clip);
                        avatarBox.getChildren().setAll(iv);
                    });
                } catch (Exception ignored) {}
            }).start();
        }
    }

    private void buildAvatarFallback(String displayName) {
        Label initials = new Label(getInitials(displayName));
        initials.setStyle("-fx-text-fill:#c9a96e; -fx-font-size:32px; -fx-font-weight:bold;");
        StackPane circle = new StackPane(initials);
        circle.setPrefSize(90, 90);
        circle.setMaxSize(90, 90);
        circle.setStyle("-fx-background-color:#1a1a2e; -fx-background-radius:45px; -fx-border-color:#c9a96e; -fx-border-radius:45px; -fx-border-width:2px;");
        avatarBox.getChildren().setAll(circle);
    }

    private void loadRating() {
        new Thread(() -> {
            try {
                List<Product> products = sellerService.getProductsBySeller(sellerId);
                double rating = sellerService.getSellerRating(sellerId);
                int count = products.size();
                Platform.runLater(() -> {
                    String text;
                    if (count == 0) text = "Нет товаров";
                    else if (rating <= 0.0) text = count + " " + plural(count, "товар", "товара", "товаров");
                    else text = "★ " + String.format("%.1f", rating) + "  ·  " + count + " " + plural(count, "товар", "товара", "товаров");
                    ratingLabel.setText(text);
                    ratingLabel.setStyle("-fx-text-fill:#c9a96e; -fx-font-size:14px;");
                });
            } catch (Exception ignored) {
                Platform.runLater(() -> ratingLabel.setText(""));
            }
        }).start();
    }

    @FXML public void handleTabProducts() {
        tabProducts.getStyleClass().setAll("tab-btn-active");
        tabPosts.getStyleClass().setAll("tab-btn");
        if (tabPromoCodes != null) tabPromoCodes.getStyleClass().setAll("tab-btn");
        showProductsTab();
    }

    @FXML public void handleTabPosts() {
        tabPosts.getStyleClass().setAll("tab-btn-active");
        tabProducts.getStyleClass().setAll("tab-btn");
        if (tabPromoCodes != null) tabPromoCodes.getStyleClass().setAll("tab-btn");
        showPostsTab();
    }

    @FXML public void handleTabPromoCodes() {
        tabPromoCodes.getStyleClass().setAll("tab-btn-active");
        tabProducts.getStyleClass().setAll("tab-btn");
        tabPosts.getStyleClass().setAll("tab-btn");
        showPromoCodesTab();
    }

    private void showProductsTab() {
        tabContent.getChildren().clear();
        boolean isOwnProfile = sellerId != null && sellerId.equals(SessionManager.getUserId());
        if (isOwnProfile) {
            Button addProductBtn = new Button("+ Добавить товар");
            addProductBtn.getStyleClass().add("btn-primary");
            addProductBtn.setMaxWidth(Double.MAX_VALUE);
            addProductBtn.setOnAction(e -> showCreateProductForm());
            tabContent.getChildren().add(addProductBtn);
        }

        Label loading = new Label("Загрузка товаров...");
        loading.setStyle("-fx-text-fill:#8888aa;");
        tabContent.getChildren().add(loading);

        new Thread(() -> {
            try {
                List<Product> products = sellerService.getProductsBySeller(sellerId);
                Platform.runLater(() -> {
                    tabContent.getChildren().clear();
                    if (isOwnProfile) {
                        Button addProductBtn = new Button("+ Добавить товар");
                        addProductBtn.getStyleClass().add("btn-primary");
                        addProductBtn.setMaxWidth(Double.MAX_VALUE);
                        addProductBtn.setOnAction(e -> showCreateProductForm());
                        tabContent.getChildren().add(addProductBtn);
                    }
                    if (products == null || products.isEmpty()) {
                        Label empty = new Label("У продавца пока нет товаров");
                        empty.setStyle("-fx-text-fill:#8888aa; -fx-font-size:14px;");
                        tabContent.getChildren().add(empty);
                        return;
                    }
                    FlowPane grid = new FlowPane();
                    grid.setHgap(14);
                    grid.setVgap(14);
                    grid.setPrefWrapLength(1000);
                    for (Product p : products) {
                        grid.getChildren().add(makeProductCard(p));
                    }
                    tabContent.getChildren().add(grid);
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    tabContent.getChildren().clear();
                    Label err = new Label("Ошибка загрузки товаров");
                    err.setStyle("-fx-text-fill:#f05050;");
                    tabContent.getChildren().add(err);
                });
            }
        }).start();
    }

    private VBox makeProductCard(Product p) {
        VBox card = new VBox(0);
        card.setPrefWidth(200);
        card.setStyle("-fx-background-color:#1a1a2e; -fx-background-radius:14px; -fx-border-color:#2a2a3e; -fx-border-radius:14px;");

        StackPane imgBox = new StackPane();
        imgBox.setPrefHeight(140);
        imgBox.setStyle("-fx-background-color:#13131f; -fx-background-radius:14 14 0 0;");
        Label placeholder = new Label("👕");
        placeholder.setStyle("-fx-font-size:38px;");
        imgBox.getChildren().add(placeholder);

        if (p.getImageUrl() != null && !p.getImageUrl().isBlank()) {
            ImageView iv = new ImageView();
            iv.setFitWidth(200);
            iv.setFitHeight(140);
            iv.setPreserveRatio(false);
            iv.setSmooth(true);
            Rectangle clip = new Rectangle(200, 140);
            clip.setArcWidth(28);
            clip.setArcHeight(28);
            iv.setClip(clip);

            new Thread(() -> {
                try {
                    Image img = ImageLoader.loadCard(p.getImageUrl());
                    Platform.runLater(() -> {
                        iv.setImage(img);
                        imgBox.getChildren().setAll(iv);
                    });
                } catch (Exception ignored) {}
            }).start();
        }

        VBox info = new VBox(4);
        info.setPadding(new Insets(10, 12, 6, 12));
        Label name = new Label(p.getName());
        name.setWrapText(true);
        name.setMaxWidth(176);
        name.setStyle("-fx-text-fill:#e8e8f0; -fx-font-size:13px; -fx-font-weight:bold;");
        Label price = new Label(p.getPriceFormatted());
        price.setStyle("-fx-text-fill:#c9a96e; -fx-font-size:14px; -fx-font-weight:bold;");
        info.getChildren().addAll(name, price);

        VBox btnBox = new VBox(6);
        btnBox.setPadding(new Insets(4, 12, 12, 12));

        Button btn = new Button("Подробнее");
        btn.getStyleClass().add("btn-outline");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> NavigationManager.navigateToMainWithProduct(p.getId()));

        boolean isOwner = sellerId != null && sellerId.equals(SessionManager.getUserId());
        if (isOwner) {
            Button editBtn = new Button("Изменить");
            editBtn.getStyleClass().add("btn-primary");
            editBtn.setMaxWidth(Double.MAX_VALUE);
            editBtn.setOnAction(e -> showEditProductForm(p));

            Button deleteBtn = new Button("Удалить");
            deleteBtn.setStyle("-fx-background-color:#f05050; -fx-text-fill:white; -fx-font-size:12px;");
            deleteBtn.setMaxWidth(Double.MAX_VALUE);
            deleteBtn.setOnAction(e -> deleteProduct(p));

            btnBox.getChildren().addAll(btn, editBtn, deleteBtn);
        } else {
            btnBox.getChildren().add(btn);
        }

        card.getChildren().addAll(imgBox, info, btnBox);
        return card;
    }

    private void showEditProductForm(Product product) {
        tabContent.getChildren().clear();
        VBox form = new VBox(12);
        form.setPadding(new Insets(16));
        form.setStyle("-fx-background-color:#1a1a2e; -fx-background-radius:14px;");

        Label title = new Label("Редактировать товар");
        title.setStyle("-fx-text-fill:#e8e8f0; -fx-font-size:16px; -fx-font-weight:bold;");

        TextField nameField = new TextField(product.getName());
        nameField.setStyle("-fx-background-color:#13131f; -fx-text-fill:#e8e8f0;");

        TextArea descArea = new TextArea(product.getDescription());
        descArea.setPrefHeight(80);
        descArea.setStyle("-fx-control-inner-background:#13131f; -fx-text-fill:#e8e8f0;");

        Label categoryLabel = new Label("Категория:");
        categoryLabel.setStyle("-fx-text-fill:#e8e8f0; -fx-font-size:14px;");
        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll("Футболки", "Джинсы", "Куртки", "Свитера", "Аксессуары", "Обувь");
        int currentCatId = product.getCategoryId();
        String currentCatName = "Футболки";
        if (currentCatId == 2) currentCatName = "Джинсы";
        else if (currentCatId == 3) currentCatName = "Куртки";
        else if (currentCatId == 4) currentCatName = "Свитера";
        else if (currentCatId == 5) currentCatName = "Аксессуары";
        else if (currentCatId == 6) currentCatName = "Обувь";
        categoryCombo.setValue(currentCatName);
        categoryCombo.setStyle("-fx-background-color:#13131f; -fx-text-fill:#e8e8f0;");

        TextField priceField = new TextField(String.valueOf((int) product.getPrice()));
        priceField.setStyle("-fx-background-color:#13131f; -fx-text-fill:#e8e8f0;");
        priceField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.matches("\\d*")) priceField.setText(old);
        });

        Label sizesLabel = new Label("Размеры и количество:");
        sizesLabel.setStyle("-fx-text-fill:#e8e8f0; -fx-font-size:14px; -fx-font-weight:bold;");

        VBox sizesContainer = new VBox(8);
        sizesContainer.setPadding(new Insets(8, 0, 8, 0));

        Label loadingSizes = new Label("Загрузка размеров...");
        loadingSizes.setStyle("-fx-text-fill:#8888aa;");
        sizesContainer.getChildren().add(loadingSizes);

        String[] standardSizes = {"XS", "S", "M", "L", "XL", "XXL"};
        Map<String, TextField> sizeQuantityFields = new HashMap<>();

        new Thread(() -> {
            try {
                List<ProductSize> existingSizes = productService.getSizes(product.getId());
                Platform.runLater(() -> {
                    sizesContainer.getChildren().clear();
                    for (String size : standardSizes) {
                        HBox sizeRow = new HBox(12);
                        sizeRow.setAlignment(Pos.CENTER_LEFT);
                        Label sizeLabel = new Label(size + ":");
                        sizeLabel.setStyle("-fx-text-fill:#b8b8c8; -fx-min-width:40px;");

                        TextField qtyField = new TextField();
                        int currentQty = 0;
                        for (ProductSize ps : existingSizes) {
                            if (ps.getSize() != null && ps.getSize().equals(size)) {
                                currentQty = ps.getQuantity();
                                break;
                            }
                        }
                        qtyField.setText(String.valueOf(currentQty));
                        qtyField.setPrefColumnCount(5);
                        qtyField.setStyle("-fx-background-color:#13131f; -fx-text-fill:#e8e8f0;");
                        qtyField.textProperty().addListener((obs, old, newVal) -> {
                            if (!newVal.matches("\\d*")) qtyField.setText(old);
                        });

                        sizeQuantityFields.put(size, qtyField);
                        sizeRow.getChildren().addAll(sizeLabel, qtyField);
                        sizesContainer.getChildren().add(sizeRow);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    sizesContainer.getChildren().clear();
                    for (String size : standardSizes) {
                        HBox sizeRow = new HBox(12);
                        sizeRow.setAlignment(Pos.CENTER_LEFT);
                        Label sizeLabel = new Label(size + ":");
                        sizeLabel.setStyle("-fx-text-fill:#b8b8c8; -fx-min-width:40px;");
                        TextField qtyField = new TextField("0");
                        qtyField.setPrefColumnCount(5);
                        qtyField.setStyle("-fx-background-color:#13131f; -fx-text-fill:#e8e8f0;");
                        qtyField.textProperty().addListener((obs, old, newVal) -> {
                            if (!newVal.matches("\\d*")) qtyField.setText(old);
                        });
                        sizeQuantityFields.put(size, qtyField);
                        sizeRow.getChildren().addAll(sizeLabel, qtyField);
                        sizesContainer.getChildren().add(sizeRow);
                    }
                });
            }
        }).start();

        Label statusLabel = new Label();
        statusLabel.setVisible(false);

        Button saveBtn = new Button("Сохранить изменения");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setMaxWidth(Double.MAX_VALUE);

        Button cancelBtn = new Button("Отмена");
        cancelBtn.getStyleClass().add("btn-outline");
        cancelBtn.setMaxWidth(Double.MAX_VALUE);
        cancelBtn.setOnAction(e -> showProductsTab());

        saveBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            String desc = descArea.getText().trim();
            String priceStr = priceField.getText().trim();
            String selectedCategory = categoryCombo.getValue();

            if (name.isBlank() || desc.isBlank() || priceStr.isBlank() || selectedCategory == null) {
                statusLabel.setText("Заполните все поля");
                statusLabel.setStyle("-fx-text-fill:#f05050;");
                statusLabel.setVisible(true);
                return;
            }

            double price;
            try {
                price = Double.parseDouble(priceStr);
                if (price <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                statusLabel.setText("Некорректная цена");
                statusLabel.setStyle("-fx-text-fill:#f05050;");
                statusLabel.setVisible(true);
                return;
            }

            saveBtn.setDisable(true);
            saveBtn.setText("Сохранение...");

            new Thread(() -> {
                try {
                    product.setName(name);
                    product.setDescription(desc);
                    product.setPrice(price);

                    int categoryId = 1;
                    switch (selectedCategory) {
                        case "Джинсы": categoryId = 2; break;
                        case "Куртки": categoryId = 3; break;
                        case "Свитера": categoryId = 4; break;
                        case "Аксессуары": categoryId = 5; break;
                        case "Обувь": categoryId = 6; break;
                    }
                    product.setCategoryId(categoryId);

                    productService.update(product);

                    for (Map.Entry<String, TextField> entry : sizeQuantityFields.entrySet()) {
                        String qtyStr = entry.getValue().getText().trim();
                        int qty = qtyStr.isBlank() ? 0 : Integer.parseInt(qtyStr);
                        productService.setSizeQty(product.getId(), entry.getKey(), qty);
                    }

                    Platform.runLater(() -> {
                        showProductsTab();
                        showAlert("Успешно", "Товар обновлён");
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        statusLabel.setText("Ошибка: " + ex.getMessage());
                        statusLabel.setStyle("-fx-text-fill:#f05050;");
                        statusLabel.setVisible(true);
                        saveBtn.setDisable(false);
                        saveBtn.setText("Сохранить изменения");
                    });
                }
            }).start();
        });

        form.getChildren().addAll(title, nameField, descArea, categoryLabel, categoryCombo, priceField,
                new Separator(), sizesLabel, sizesContainer, statusLabel, saveBtn, cancelBtn);
        tabContent.getChildren().add(form);
    }

    private void deleteProduct(Product product) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Удаление товара");
        alert.setHeaderText("Удалить \"" + product.getName() + "\"?");
        alert.setContentText("Это действие нельзя отменить. Товар исчезнет из каталога.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        productService.delete(product.getId());
                        Platform.runLater(() -> {
                            showProductsTab();
                            showAlert("Удалено", "Товар успешно удалён");
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> showAlert("Ошибка", "Не удалось удалить: " + ex.getMessage()));
                    }
                }).start();
            }
        });
    }

    private void showPostsTab() {
        tabContent.getChildren().clear();
        Label loading = new Label("Загрузка публикаций...");
        loading.setStyle("-fx-text-fill:#8888aa;");
        tabContent.getChildren().add(loading);

        new Thread(() -> {
            try {
                List<Post> posts = postService.getByAuthor(sellerId);
                Platform.runLater(() -> {
                    tabContent.getChildren().clear();
                    boolean isOwnProfile = sellerId != null && sellerId.equals(SessionManager.getUserId());
                    if (isOwnProfile) {
                        Button createBtn = new Button("+ Новая публикация");
                        createBtn.getStyleClass().add("btn-outline");
                        createBtn.setMaxWidth(Double.MAX_VALUE);
                        createBtn.setOnAction(e -> showCreatePostForm());
                        tabContent.getChildren().add(createBtn);
                    }
                    if (posts == null || posts.isEmpty()) {
                        Label empty = new Label("Публикаций пока нет");
                        empty.setStyle("-fx-text-fill:#8888aa; -fx-font-size:14px;");
                        tabContent.getChildren().add(empty);
                        return;
                    }
                    for (Post post : posts) {
                        tabContent.getChildren().add(makePostCard(post, isOwnProfile));
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    tabContent.getChildren().clear();
                    Label err = new Label("Ошибка загрузки публикаций");
                    err.setStyle("-fx-text-fill:#f05050;");
                    tabContent.getChildren().add(err);
                });
            }
        }).start();
    }

    private VBox makePostCard(Post post, boolean isOwn) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color:#1a1a2e; -fx-background-radius:14px; -fx-border-color:#2a2a3e; -fx-border-radius:14px;");

        if (post.getTitle() != null && !post.getTitle().isBlank()) {
            Label title = new Label(post.getTitle());
            title.setStyle("-fx-text-fill:#e8e8f0; -fx-font-size:16px; -fx-font-weight:bold;");
            title.setWrapText(true);
            card.getChildren().add(title);
        }
        if (post.getImageUrl() != null && !post.getImageUrl().isBlank()) {
            StackPane imgBox = new StackPane();
            imgBox.setPrefHeight(220);
            imgBox.setStyle("-fx-background-color:#13131f; -fx-background-radius:10px;");
            Label loading = new Label("");
            loading.setStyle("-fx-font-size:30px;");
            imgBox.getChildren().add(loading);
            card.getChildren().add(imgBox);

            new Thread(() -> {
                try {
                    Image img = ImageLoader.loadDetail(post.getImageUrl());
                    Platform.runLater(() -> {
                        ImageView iv = new ImageView(img);
                        iv.setFitWidth(440);
                        iv.setFitHeight(220);
                        iv.setPreserveRatio(true);
                        iv.setSmooth(true);
                        Rectangle clip = new Rectangle(440, 220);
                        clip.setArcWidth(20);
                        clip.setArcHeight(20);
                        iv.setClip(clip);
                        imgBox.getChildren().setAll(iv);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        Label broken = new Label("Нет изображения");
                        broken.setStyle("-fx-text-fill:#666688; -fx-font-size:14px;");
                        imgBox.getChildren().setAll(broken);
                    });
                }
            }).start();
        }
        if (post.getContent() != null && !post.getContent().isBlank()) {
            Label content = new Label(post.getContent());
            content.setWrapText(true);
            content.setStyle("-fx-text-fill:#b8b8c8; -fx-font-size:13px;");
            card.getChildren().add(content);
        }

        VBox commentsBox = new VBox(8);
        Button showCommentsBtn = new Button("Показать комментарии");
        showCommentsBtn.getStyleClass().add("btn-ghost");
        showCommentsBtn.setOnAction(e -> loadComments(post, commentsBox, showCommentsBtn));
        card.getChildren().addAll(showCommentsBtn, commentsBox);

        if (isOwn) {
            Button deleteBtn = new Button("Удалить публикацию");
            deleteBtn.setStyle("-fx-background-color:transparent; -fx-text-fill:#f05050; -fx-font-size:12px; -fx-cursor:hand;");
            deleteBtn.setOnAction(e -> deletePost(post.getId()));
            card.getChildren().add(deleteBtn);
        }
        return card;
    }

    private void loadComments(Post post, VBox commentsBox, Button btn) {
        btn.setVisible(false);
        btn.setManaged(false);
        new Thread(() -> {
            try {
                List<PostComment> comments = postService.getComments(post.getId());
                Platform.runLater(() -> {
                    commentsBox.getChildren().clear();
                    for (PostComment c : comments) {
                        HBox row = new HBox(8);
                        row.setAlignment(Pos.TOP_LEFT);
                        String authorName = c.getAuthorName() != null && !c.getAuthorName().isBlank() ? c.getAuthorName() : "Пользователь";
                        Label author = new Label(authorName + ":");
                        author.setStyle("-fx-text-fill:#c9a96e; -fx-font-weight:bold;");
                        Label text = new Label(c.getContent());
                        text.setWrapText(true);
                        text.setStyle("-fx-text-fill:#b8b8c8;");
                        HBox.setHgrow(text, Priority.ALWAYS);
                        row.getChildren().addAll(author, text);
                        commentsBox.getChildren().add(row);
                    }
                    HBox inputRow = new HBox(8);
                    inputRow.setAlignment(Pos.CENTER_LEFT);
                    TextField input = new TextField();
                    input.setPromptText("Написать комментарий...");
                    input.setStyle("-fx-background-color:#13131f; -fx-text-fill:#e8e8f0; -fx-prompt-text-fill:#666688;");
                    HBox.setHgrow(input, Priority.ALWAYS);
                    Button sendBtn = new Button("→");
                    sendBtn.getStyleClass().add("btn-primary");
                    sendBtn.setOnAction(ev -> {
                        String text = input.getText().trim();
                        if (text.isBlank()) return;
                        sendBtn.setDisable(true);
                        new Thread(() -> {
                            try {
                                postService.addComment(post.getId(), text);
                                Platform.runLater(() -> {
                                    input.clear();
                                    sendBtn.setDisable(false);
                                    loadComments(post, commentsBox, new Button());
                                });
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                Platform.runLater(() -> sendBtn.setDisable(false));
                            }
                        }).start();
                    });
                    inputRow.getChildren().addAll(input, sendBtn);
                    commentsBox.getChildren().add(inputRow);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void showCreateProductForm() {
        tabContent.getChildren().clear();
        VBox form = new VBox(12);
        form.setPadding(new Insets(16));
        form.setStyle("-fx-background-color:#1a1a2e; -fx-background-radius:14px;");

        Label title = new Label("Добавить новый товар");
        title.setStyle("-fx-text-fill:#e8e8f0; -fx-font-size:16px; -fx-font-weight:bold;");

        TextField nameField = new TextField();
        nameField.setPromptText("Название товара *");
        nameField.setStyle("-fx-background-color:#13131f; -fx-text-fill:#e8e8f0;");

        TextArea descArea = new TextArea();
        descArea.setPromptText("Описание товара *");
        descArea.setWrapText(true);
        descArea.setPrefHeight(80);
        descArea.setStyle("-fx-control-inner-background:#13131f; -fx-text-fill:#e8e8f0;");

        Label categoryLabel = new Label("Категория:");
        categoryLabel.setStyle("-fx-text-fill:#e8e8f0; -fx-font-size:14px;");
        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll("Футболки", "Джинсы", "Куртки", "Свитера", "Аксессуары", "Обувь");
        categoryCombo.setValue("Футболки");
        categoryCombo.setStyle("-fx-background-color:#13131f; -fx-text-fill:#e8e8f0;");

        TextField priceField = new TextField();
        priceField.setPromptText("Цена (₽) *");
        priceField.setStyle("-fx-background-color:#13131f; -fx-text-fill:#e8e8f0;");
        priceField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.matches("\\d*")) priceField.setText(old);
        });

        Button uploadImageBtn = new Button("📷 Загрузить фото с ПК");
        uploadImageBtn.getStyleClass().add("btn-outline");
        uploadImageBtn.setMaxWidth(Double.MAX_VALUE);

        Label imageStatusLabel = new Label("Фото не выбрано");
        imageStatusLabel.setStyle("-fx-text-fill:#8888aa;");
        final Path[] selectedFilePath = {null};

        uploadImageBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Выберите изображение товара");
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Изображения", "*.png", "*.jpg", "*.jpeg", "*.webp"));
            File file = fileChooser.showOpenDialog(tabContent.getScene().getWindow());
            if (file != null) {
                selectedFilePath[0] = file.toPath();
                imageStatusLabel.setText("✅ Выбран файл: " + file.getName());
                imageStatusLabel.setStyle("-fx-text-fill:#4caf50;");
            }
        });

        TextField imageUrlField = new TextField();
        imageUrlField.setPromptText("Или вставьте ссылку на изображение");
        imageUrlField.setStyle("-fx-background-color:#13131f; -fx-text-fill:#e8e8f0;");

        Label sizesLabel = new Label("Размеры и количество:");
        sizesLabel.setStyle("-fx-text-fill:#e8e8f0; -fx-font-size:14px; -fx-font-weight:bold;");

        VBox sizesContainer = new VBox(8);
        sizesContainer.setPadding(new Insets(8, 0, 8, 0));

        String[] standardSizes = {"XS", "S", "M", "L", "XL", "XXL"};
        Map<String, TextField> sizeQuantityFields = new HashMap<>();

        for (String size : standardSizes) {
            HBox sizeRow = new HBox(12);
            sizeRow.setAlignment(Pos.CENTER_LEFT);
            Label sizeLabel = new Label(size + ":");
            sizeLabel.setStyle("-fx-text-fill:#b8b8c8; -fx-min-width:40px;");

            TextField qtyField = new TextField();
            qtyField.setPromptText("0");
            qtyField.setPrefColumnCount(5);
            qtyField.setStyle("-fx-background-color:#13131f; -fx-text-fill:#e8e8f0;");
            qtyField.textProperty().addListener((obs, old, newVal) -> {
                if (!newVal.matches("\\d*")) qtyField.setText(old);
            });

            sizeQuantityFields.put(size, qtyField);
            sizeRow.getChildren().addAll(sizeLabel, qtyField);
            sizesContainer.getChildren().add(sizeRow);
        }

        Label sizesHint = new Label("Укажите количество для каждого размера (0 если нет в наличии)");
        sizesHint.setStyle("-fx-text-fill:#666688; -fx-font-size:11px;");

        Label statusLabel = new Label();
        statusLabel.setVisible(false);

        Button publishBtn = new Button("Опубликовать товар");
        publishBtn.getStyleClass().add("btn-primary");
        publishBtn.setMaxWidth(Double.MAX_VALUE);

        Button cancelBtn = new Button("Отмена");
        cancelBtn.getStyleClass().add("btn-outline");
        cancelBtn.setMaxWidth(Double.MAX_VALUE);
        cancelBtn.setOnAction(e -> showProductsTab());

        publishBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            String desc = descArea.getText().trim();
            String priceStr = priceField.getText().trim();
            String selectedCategory = categoryCombo.getValue();

            if (name.isBlank() || desc.isBlank() || priceStr.isBlank() || selectedCategory == null) {
                statusLabel.setText("Заполните все обязательные поля (*)");
                statusLabel.setStyle("-fx-text-fill:#f05050;");
                statusLabel.setVisible(true);
                return;
            }

            double price;
            try {
                price = Double.parseDouble(priceStr);
                if (price <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                statusLabel.setText("Введите корректную цену (больше 0)");
                statusLabel.setStyle("-fx-text-fill:#f05050;");
                statusLabel.setVisible(true);
                return;
            }

            Map<String, Integer> sizes = new HashMap<>();
            int totalQuantity = 0;
            for (Map.Entry<String, TextField> entry : sizeQuantityFields.entrySet()) {
                String qtyStr = entry.getValue().getText().trim();
                int qty = qtyStr.isBlank() ? 0 : Integer.parseInt(qtyStr);
                sizes.put(entry.getKey(), qty);
                totalQuantity += qty;
            }

            if (totalQuantity == 0) {
                statusLabel.setText("Укажите количество хотя бы для одного размера");
                statusLabel.setStyle("-fx-text-fill:#f05050;");
                statusLabel.setVisible(true);
                return;
            }

            final Button finalPublishBtn = publishBtn;
            final Label finalStatusLabel = statusLabel;
            final Path[] finalSelectedFilePath = selectedFilePath;
            final TextField finalImageUrlField = imageUrlField;
            final String finalName = name;
            final String finalDesc = desc;
            final double finalPrice = price;
            final Map<String, Integer> finalSizes = sizes;
            final int finalTotalQuantity = totalQuantity;
            final String finalCategory = selectedCategory;

            finalPublishBtn.setDisable(true);
            finalPublishBtn.setText("Создание товара...");

            new Thread(() -> {
                try {
                    String finalImageUrl = finalImageUrlField.getText().trim();
                    if (finalSelectedFilePath[0] != null) {
                        Platform.runLater(() -> {
                            finalStatusLabel.setText("Загрузка фото...");
                            finalStatusLabel.setStyle("-fx-text-fill:#c9a96e;");
                            finalStatusLabel.setVisible(true);
                            finalPublishBtn.setText("Загрузка фото...");
                        });
                        finalImageUrl = storageService.uploadProductImage(finalSelectedFilePath[0]);
                    }

                    Product product = new Product();
                    product.setName(finalName);
                    product.setDescription(finalDesc);
                    product.setPrice(finalPrice);
                    product.setImageUrl(finalImageUrl);

                    int categoryId = 1;
                    switch (finalCategory) {
                        case "Джинсы": categoryId = 2; break;
                        case "Куртки": categoryId = 3; break;
                        case "Свитера": categoryId = 4; break;
                        case "Аксессуары": categoryId = 5; break;
                        case "Обувь": categoryId = 6; break;
                    }
                    product.setCategoryId(categoryId);

                    product.setSellerId(sellerId);
                    product.setActive(true);

                    Product created = productService.create(product);

                    for (Map.Entry<String, Integer> entry : finalSizes.entrySet()) {
                        if (entry.getValue() > 0) {
                            productService.setSizeQty(created.getId(), entry.getKey(), entry.getValue());
                        }
                    }

                    Platform.runLater(() -> {
                        showProductsTab();
                        showAlert("Товар добавлен", "Товар успешно создан с " + finalTotalQuantity + " ед. на складе");
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> {
                        finalStatusLabel.setText("Ошибка: " + ex.getMessage());
                        finalStatusLabel.setStyle("-fx-text-fill:#f05050;");
                        finalStatusLabel.setVisible(true);
                        finalPublishBtn.setDisable(false);
                        finalPublishBtn.setText("Опубликовать товар");
                    });
                }
            }).start();
        });

        form.getChildren().addAll(
                title, nameField, descArea,
                categoryLabel, categoryCombo,
                priceField, new Separator(),
                sizesLabel, sizesContainer, sizesHint, new Separator(),
                uploadImageBtn, imageStatusLabel, imageUrlField,
                statusLabel, publishBtn, cancelBtn
        );
        tabContent.getChildren().add(form);
    }

    private void showPromoCodesTab() {
        tabContent.getChildren().clear();
        Button createPromoBtn = new Button("+ Создать промокод");
        createPromoBtn.getStyleClass().add("btn-primary");
        createPromoBtn.setMaxWidth(Double.MAX_VALUE);
        createPromoBtn.setOnAction(e -> showCreatePromoCodeForm());
        tabContent.getChildren().add(createPromoBtn);

        Label info = new Label("ℹ️ Здесь будут ваши активные промокоды. Нажмите кнопку выше, чтобы создать новый.");
        info.setStyle("-fx-text-fill:#8888aa; -fx-font-size:14px; -fx-padding: 20 0 0 0;");
        tabContent.getChildren().add(info);
    }

    private void showCreatePromoCodeForm() {
        tabContent.getChildren().clear();
        VBox form = new VBox(12);
        form.setPadding(new Insets(16));
        form.setStyle("-fx-background-color:#1a1a2e; -fx-background-radius:14px;");

        Label title = new Label("Создать промокод");
        title.setStyle("-fx-text-fill:#e8e8f0; -fx-font-size:16px; -fx-font-weight:bold;");

        TextField codeField = new TextField();
        codeField.setPromptText("Код промокода (например, SALE2024)");
        codeField.setStyle("-fx-background-color:#13131f; -fx-text-fill:#e8e8f0;");

        TextField discountField = new TextField();
        discountField.setPromptText("Скидка в процентах (1-100)");
        discountField.setStyle("-fx-background-color:#13131f; -fx-text-fill:#e8e8f0;");
        discountField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.matches("\\d*")) discountField.setText(old);
        });

        DatePicker expiresDatePicker = new DatePicker();
        expiresDatePicker.setPromptText("Дата окончания");
        expiresDatePicker.setStyle("-fx-background-color:#13131f; -fx-text-fill:#e8e8f0;");

        ComboBox<String> timeCombo = new ComboBox<>();
        timeCombo.getItems().addAll("00:00", "09:00", "12:00", "18:00", "21:00", "23:59");
        timeCombo.setValue("21:00");
        timeCombo.setStyle("-fx-background-color:#13131f; -fx-text-fill:#e8e8f0;");

        TextField maxUsesField = new TextField();
        maxUsesField.setPromptText("Макс. использований (необязательно)");
        maxUsesField.setStyle("-fx-background-color:#13131f; -fx-text-fill:#e8e8f0;");
        maxUsesField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.matches("\\d*")) maxUsesField.setText(old);
        });

        Label statusLabel = new Label();
        statusLabel.setVisible(false);

        Button saveBtn = new Button("Создать промокод");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setMaxWidth(Double.MAX_VALUE);

        Button cancelBtn = new Button("Отмена");
        cancelBtn.getStyleClass().add("btn-outline");
        cancelBtn.setMaxWidth(Double.MAX_VALUE);
        cancelBtn.setOnAction(e -> showPromoCodesTab());

        saveBtn.setOnAction(e -> {
            String code = codeField.getText().trim().toUpperCase();
            String discountStr = discountField.getText().trim();
            LocalDate date = expiresDatePicker.getValue();
            String time = timeCombo.getValue();

            if (code.isBlank() || discountStr.isBlank() || date == null) {
                statusLabel.setText("Заполните код, скидку и дату окончания");
                statusLabel.setStyle("-fx-text-fill:#f05050;");
                statusLabel.setVisible(true);
                return;
            }

            int discount;
            try {
                discount = Integer.parseInt(discountStr);
                if (discount < 1 || discount > 100) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                statusLabel.setText("Скидка должна быть от 1 до 100");
                statusLabel.setStyle("-fx-text-fill:#f05050;");
                statusLabel.setVisible(true);
                return;
            }

            saveBtn.setDisable(true);
            saveBtn.setText("Создание...");

            new Thread(() -> {
                try {
                    Thread.sleep(500); // Имитация
                    Platform.runLater(() -> {
                        showPromoCodesTab();
                        showAlert("Успешно", "Промокод " + code + " успешно создан!");
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        statusLabel.setText("Ошибка: " + ex.getMessage());
                        statusLabel.setStyle("-fx-text-fill:#f05050;");
                        statusLabel.setVisible(true);
                        saveBtn.setDisable(false);
                        saveBtn.setText("Создать промокод");
                    });
                }
            }).start();
        });

        HBox dateBox = new HBox(12, expiresDatePicker, timeCombo);
        dateBox.setAlignment(Pos.CENTER_LEFT);

        form.getChildren().addAll(
                title, codeField, discountField,
                new Label("Дата и время окончания:"), dateBox, maxUsesField,
                statusLabel, saveBtn, cancelBtn
        );
        tabContent.getChildren().add(form);
    }

    private void showCreatePostForm() {
        tabContent.getChildren().clear();
        VBox form = new VBox(12);
        form.setPadding(new Insets(16));
        form.setStyle("-fx-background-color:#1a1a2e; -fx-background-radius:14px;");

        Label title = new Label("Новая публикация");
        title.setStyle("-fx-text-fill:#e8e8f0; -fx-font-size:16px; -fx-font-weight:bold;");

        TextField titleField = new TextField();
        titleField.setPromptText("Заголовок");
        titleField.setStyle("-fx-background-color:#13131f; -fx-text-fill:#e8e8f0;");

        TextArea contentArea = new TextArea();
        contentArea.setPromptText("Текст публикации");
        contentArea.setWrapText(true);
        contentArea.setPrefHeight(120);
        contentArea.setStyle("-fx-control-inner-background:#13131f; -fx-text-fill:#e8e8f0;");

        TextField imageField = new TextField();
        imageField.setPromptText("Ссылка на изображение (необязательно)");
        imageField.setStyle("-fx-background-color:#13131f; -fx-text-fill:#e8e8f0;");

        Label statusLabel = new Label();
        statusLabel.setVisible(false);

        Button publishBtn = new Button("Опубликовать");
        publishBtn.getStyleClass().add("btn-primary");
        publishBtn.setMaxWidth(Double.MAX_VALUE);

        Button cancelBtn = new Button("Отмена");
        cancelBtn.getStyleClass().add("btn-outline");
        cancelBtn.setMaxWidth(Double.MAX_VALUE);
        cancelBtn.setOnAction(e -> showPostsTab());

        publishBtn.setOnAction(e -> {
            String t = titleField.getText().trim();
            String c = contentArea.getText().trim();
            if (t.isBlank() && c.isBlank()) {
                statusLabel.setText("Заполните заголовок или текст");
                statusLabel.setStyle("-fx-text-fill:#f05050;");
                statusLabel.setVisible(true);
                return;
            }
            publishBtn.setDisable(true);
            publishBtn.setText("Публикация...");
            new Thread(() -> {
                try {
                    postService.createPost(t, c, imageField.getText().trim());
                    Platform.runLater(this::showPostsTab);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> {
                        statusLabel.setText("Ошибка: " + ex.getMessage());
                        statusLabel.setStyle("-fx-text-fill:#f05050;");
                        statusLabel.setVisible(true);
                        publishBtn.setDisable(false);
                        publishBtn.setText("Опубликовать");
                    });
                }
            }).start();
        });

        form.getChildren().addAll(title, titleField, contentArea, imageField, statusLabel, publishBtn, cancelBtn);
        tabContent.getChildren().add(form);
    }

    private void deletePost(long postId) {
        new Thread(() -> {
            try {
                postService.deletePost(postId);
                Platform.runLater(this::showPostsTab);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        return String.valueOf(parts[0].charAt(0)).toUpperCase();
    }

    private String plural(int n, String one, String few, String many) {
        int mod10 = n % 10;
        int mod100 = n % 100;
        if (mod100 >= 11 && mod100 <= 19) return many;
        if (mod10 == 1) return one;
        if (mod10 >= 2 && mod10 <= 4) return few;
        return many;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    public void handleBack() {
        NavigationManager.navigateTo("main");
    }
}