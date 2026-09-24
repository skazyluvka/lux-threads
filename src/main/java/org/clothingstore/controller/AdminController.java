package org.clothingstore.controller;

import org.clothingstore.model.*;
import org.clothingstore.service.*;
import org.clothingstore.util.ImageLoader;
import org.clothingstore.util.NavigationManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;

public class AdminController implements Initializable {

    // ── Товары — таблица
    @FXML private TableView<Product>            productsTable;
    @FXML private TableColumn<Product, Integer> colPId;
    @FXML private TableColumn<Product, String>  colPName;
    @FXML private TableColumn<Product, Double>  colPPrice;   // ← было BigDecimal
    @FXML private TableColumn<Product, Boolean> colPActive;

    // ── Товары — форма
    @FXML private TextField          pNameField;
    @FXML private TextArea           pDescField;
    @FXML private TextField          pPriceField;
    @FXML private ComboBox<Category> pCategoryCombo;
    @FXML private CheckBox           pActiveCheck;
    @FXML private Label              pMsgLabel;

    // ── Товары — фото
    @FXML private ImageView pImagePreview;
    @FXML private Label     pImageLabel;

    // ── Категории
    @FXML private TableView<Category>            categoriesTable;
    @FXML private TableColumn<Category, Integer> colCId;
    @FXML private TableColumn<Category, String>  colCName;
    @FXML private TableColumn<Category, String>  colCDesc;
    @FXML private TextField cNameField;
    @FXML private TextField cDescField;
    @FXML private Label     cMsgLabel;

    // ── Промокоды
    @FXML private TableView<PromoCode>            promoTable;
    @FXML private TableColumn<PromoCode, Integer> colPromoId;
    @FXML private TableColumn<PromoCode, String>  colPromoCode;
    @FXML private TableColumn<PromoCode, Integer> colPromoDiscount;
    @FXML private TableColumn<PromoCode, Boolean> colPromoActive;
    @FXML private TextField promoCodeField;
    @FXML private TextField promoDiscountField;
    @FXML private TextField promoMaxUsesField;
    @FXML private CheckBox  promoActiveCheck;
    @FXML private Label     promoMsgLabel;

    // ── Заказы
    @FXML private TableView<Order>               ordersTable;
    @FXML private TableColumn<Order, Integer>    colOId;
    @FXML private TableColumn<Order, String>     colOStatus;
    @FXML private TableColumn<Order, BigDecimal> colOTotal;
    @FXML private TableColumn<Order, String>     colODate;
    @FXML private ComboBox<String> statusCombo;
    @FXML private Label            oMsgLabel;

    private final ProductService   productService   = new ProductService();
    private final CategoryService  categoryService  = new CategoryService();
    private final PromoCodeService promoCodeService = new PromoCodeService();
    private final OrderService     orderService     = new OrderService();
    private final StorageService   storageService   = new StorageService();

    private Path   selectedImagePath = null;
    private String currentImageUrl   = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupProductsTab();
        setupCategoriesTab();
        setupPromosTab();
        setupOrdersTab();
        loadAll();
    }

    // ══════════════════════════════════════════════
    //  SETUP
    // ══════════════════════════════════════════════

    private void setupProductsTab() {
        colPId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colPName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colPActive.setCellValueFactory(new PropertyValueFactory<>("active"));
        pActiveCheck.setSelected(true);
        resetImageUI();

        productsTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, p) -> {
                    if (p == null) return;
                    pNameField.setText(p.getName());
                    pDescField.setText(nvl(p.getDescription()));
                    // ← исправлено: price теперь double, не BigDecimal
                    pPriceField.setText(String.valueOf(p.getPrice()));
                    // ← исправлено: isActive() теперь есть в Product
                    pActiveCheck.setSelected(p.isActive());
                    pCategoryCombo.getItems().stream()
                            .filter(c -> c.getId() == p.getCategoryId())
                            .findFirst().ifPresent(pCategoryCombo::setValue);

                    currentImageUrl   = p.getImageUrl();
                    selectedImagePath = null;

                    if (p.getImageUrl() != null && !p.getImageUrl().isBlank()) {
                        pImageLabel.setText("✓ Фото загружено");
                        pImageLabel.setStyle(
                                "-fx-text-fill:#50c878; -fx-font-size:12px;");
                        new Thread(() -> {
                            try {
                                Image img = ImageLoader.loadPreview(p.getImageUrl());
                                Platform.runLater(() -> {
                                    pImagePreview.setImage(img);
                                    pImagePreview.setVisible(true);
                                    pImagePreview.setManaged(true);
                                });
                            } catch (Exception ex) {
                                System.err.println("Preview error: " + ex.getMessage());
                            }
                        }).start();
                    } else {
                        resetImageUI();
                    }
                });
    }

    private void setupCategoriesTab() {
        colCId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        categoriesTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, c) -> {
                    if (c == null) return;
                    cNameField.setText(c.getName());
                    cDescField.setText(nvl(c.getDescription()));
                });
    }

    private void setupPromosTab() {
        colPromoId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colPromoCode.setCellValueFactory(new PropertyValueFactory<>("code"));
        colPromoDiscount.setCellValueFactory(
                new PropertyValueFactory<>("discountPercent"));
        colPromoActive.setCellValueFactory(
                new PropertyValueFactory<>("active"));
        promoActiveCheck.setSelected(true);
        promoTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, pc) -> {
                    if (pc == null) return;
                    promoCodeField.setText(pc.getCode());
                    promoDiscountField.setText(
                            String.valueOf(pc.getDiscountPercent()));
                    promoMaxUsesField.setText(
                            pc.getMaxUses() != null ? pc.getMaxUses().toString() : "");
                    promoActiveCheck.setSelected(pc.isActive());
                });
    }

    private void setupOrdersTab() {
        colOId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colOStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colOTotal.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));
        colODate.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        statusCombo.getItems().addAll(
                "pending", "processing", "shipped", "delivered", "cancelled");
    }

    // ══════════════════════════════════════════════
    //  LOAD
    // ══════════════════════════════════════════════

    private void loadAll() {
        loadProducts(); loadCategories(); loadPromos(); loadOrders();
    }

    private void loadProducts() {
        new Thread(() -> {
            try {
                List<Product>  list = productService.getAll();
                List<Category> cats = categoryService.getAll();
                Platform.runLater(() -> {
                    productsTable.getItems().setAll(list);
                    pCategoryCombo.getItems().setAll(cats);
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void loadCategories() {
        new Thread(() -> {
            try {
                List<Category> list = categoryService.getAll();
                Platform.runLater(() ->
                        categoriesTable.getItems().setAll(list));
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void loadPromos() {
        new Thread(() -> {
            try {
                List<PromoCode> list = promoCodeService.getAll();
                Platform.runLater(() ->
                        promoTable.getItems().setAll(list));
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void loadOrders() {
        new Thread(() -> {
            try {
                List<Order> list = orderService.getAllOrders();
                Platform.runLater(() ->
                        ordersTable.getItems().setAll(list));
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    // ══════════════════════════════════════════════
    //  ФОТО
    // ══════════════════════════════════════════════

    @FXML
    public void handleChooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Выберите изображение товара");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Изображения", "*.jpg", "*.jpeg", "*.png", "*.webp"));
        File file = chooser.showOpenDialog(NavigationManager.getStage());
        if (file == null) return;
        selectedImagePath = file.toPath();
        pImageLabel.setText("📎 " + file.getName());
        pImageLabel.setStyle("-fx-text-fill:#c9a96e; -fx-font-size:12px;");
        try {
            Image preview = new Image(file.toURI().toString(), 200, 120, true, true);
            pImagePreview.setImage(preview);
            pImagePreview.setVisible(true);
            pImagePreview.setManaged(true);
        } catch (Exception e) {
            pImagePreview.setVisible(false);
            pImagePreview.setManaged(false);
        }
    }

    @FXML
    public void handleRemoveImage() {
        selectedImagePath = null;
        currentImageUrl   = null;
        pImageLabel.setText("Фото удалено (сохрани товар)");
        pImageLabel.setStyle("-fx-text-fill:#f05050; -fx-font-size:12px;");
        pImagePreview.setVisible(false);
        pImagePreview.setManaged(false);
    }

    private String uploadImageIfSelected() throws Exception {
        if (selectedImagePath != null) {
            if (currentImageUrl != null && !currentImageUrl.isBlank()) {
                try { storageService.deleteImage(currentImageUrl); }
                catch (Exception ignored) {}
            }
            String uploadedUrl = storageService.uploadProductImage(selectedImagePath);
            selectedImagePath  = null;
            return uploadedUrl;
        }
        return currentImageUrl;
    }

    private void resetImageUI() {
        currentImageUrl   = null;
        selectedImagePath = null;
        pImageLabel.setText("Фото не выбрано");
        pImageLabel.setStyle("-fx-text-fill:#55556a; -fx-font-size:12px;");
        pImagePreview.setVisible(false);
        pImagePreview.setManaged(false);
    }

    // ══════════════════════════════════════════════
    //  PRODUCTS ACTIONS
    // ══════════════════════════════════════════════

    @FXML public void handleAddProduct()    { saveProduct(null); }

    @FXML public void handleUpdateProduct() {
        Product sel = productsTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showMsg(pMsgLabel, "Выберите товар в таблице", false); return;
        }
        saveProduct(sel);
    }

    private void saveProduct(Product existing) {
        String name     = pNameField.getText().trim();
        String priceStr = pPriceField.getText().trim();
        Category cat    = pCategoryCombo.getValue();

        if (name.isEmpty() || priceStr.isEmpty() || cat == null) {
            showMsg(pMsgLabel, "Заполните название, цену и категорию", false);
            return;
        }

        // ← исправлено: double вместо BigDecimal
        double priceVal;
        try {
            priceVal = Double.parseDouble(priceStr.replace(",", "."));
        } catch (NumberFormatException e) {
            showMsg(pMsgLabel, "Неверный формат цены", false);
            return;
        }

        pMsgLabel.setText("⏳ Сохранение...");
        pMsgLabel.setStyle("-fx-text-fill:#c9a96e;");
        pMsgLabel.setVisible(true);

        Product p = existing != null ? existing : new Product();
        p.setName(name);
        p.setDescription(pDescField.getText().trim());
        p.setPrice(priceVal);            // ← double
        p.setCategoryId(cat.getId());
        p.setActive(pActiveCheck.isSelected());  // ← теперь есть в Product

        new Thread(() -> {
            try {
                String imageUrl = uploadImageIfSelected();
                p.setImageUrl(imageUrl);
                if (existing == null) productService.create(p);
                else                  productService.update(p);
                Platform.runLater(() -> {
                    showMsg(pMsgLabel, "✓ Товар сохранён", true);
                    resetImageUI();
                    loadProducts();
                    clearProductForm();
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                        showMsg(pMsgLabel, "Ошибка: " + e.getMessage(), false));
            }
        }).start();
    }

    @FXML public void handleDeleteProduct() {
        Product sel = productsTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        if (!confirmDelete("товар «" + sel.getName() + "»")) return;
        new Thread(() -> {
            try {
                if (sel.getImageUrl() != null)
                    storageService.deleteImage(sel.getImageUrl());
                productService.delete(sel.getId());
                Platform.runLater(() -> {
                    showMsg(pMsgLabel, "✓ Товар удалён", true);
                    loadProducts();
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                        showMsg(pMsgLabel, "Ошибка: " + e.getMessage(), false));
            }
        }).start();
    }

    private void clearProductForm() {
        pNameField.clear();
        pDescField.clear();
        pPriceField.clear();
        pActiveCheck.setSelected(true);
        pCategoryCombo.setValue(null);
        productsTable.getSelectionModel().clearSelection();
    }

    // ══════════════════════════════════════════════
    //  CATEGORIES ACTIONS
    // ══════════════════════════════════════════════

    @FXML public void handleAddCategory()    { saveCategory(null); }

    @FXML public void handleUpdateCategory() {
        Category sel = categoriesTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showMsg(cMsgLabel, "Выберите категорию", false); return; }
        saveCategory(sel);
    }

    private void saveCategory(Category existing) {
        String name = cNameField.getText().trim();
        if (name.isEmpty()) { showMsg(cMsgLabel, "Введите название", false); return; }
        Category c = existing != null ? existing : new Category();
        c.setName(name);
        c.setDescription(cDescField.getText().trim());
        new Thread(() -> {
            try {
                if (existing == null) categoryService.create(c);
                else                  categoryService.update(c);
                Platform.runLater(() -> {
                    showMsg(cMsgLabel, "✓ Категория сохранена", true);
                    loadCategories();
                    cNameField.clear();
                    cDescField.clear();
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                        showMsg(cMsgLabel, "Ошибка: " + e.getMessage(), false));
            }
        }).start();
    }

    @FXML public void handleDeleteCategory() {
        Category sel = categoriesTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        if (!confirmDelete("категорию «" + sel.getName() + "»")) return;
        new Thread(() -> {
            try {
                categoryService.delete(sel.getId());
                Platform.runLater(() -> { showMsg(cMsgLabel, "✓ Удалена", true); loadCategories(); });
            } catch (Exception e) {
                Platform.runLater(() -> showMsg(cMsgLabel, "Ошибка: " + e.getMessage(), false));
            }
        }).start();
    }

    // ══════════════════════════════════════════════
    //  PROMO ACTIONS
    // ══════════════════════════════════════════════

    @FXML public void handleAddPromo() {
        String code    = promoCodeField.getText().trim().toUpperCase();
        String discStr = promoDiscountField.getText().trim();
        if (code.isEmpty() || discStr.isEmpty()) {
            showMsg(promoMsgLabel, "Введите код и скидку", false); return;
        }
        int disc;
        try { disc = Integer.parseInt(discStr); }
        catch (Exception e) {
            showMsg(promoMsgLabel, "Скидка должна быть числом 1–100", false); return;
        }
        PromoCode pc = new PromoCode();
        pc.setCode(code);
        pc.setDiscountPercent(disc);
        pc.setActive(promoActiveCheck.isSelected());
        String maxStr = promoMaxUsesField.getText().trim();
        if (!maxStr.isEmpty()) {
            try { pc.setMaxUses(Integer.parseInt(maxStr)); }
            catch (Exception ignored) {}
        }
        new Thread(() -> {
            try {
                promoCodeService.create(pc);
                Platform.runLater(() -> {
                    showMsg(promoMsgLabel, "✓ Промокод создан", true);
                    loadPromos();
                    promoCodeField.clear();
                    promoDiscountField.clear();
                    promoMaxUsesField.clear();
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                        showMsg(promoMsgLabel, "Ошибка: " + e.getMessage(), false));
            }
        }).start();
    }

    @FXML public void handleTogglePromo() {
        PromoCode sel = promoTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        new Thread(() -> {
            try {
                promoCodeService.toggleActive(sel.getId(), !sel.isActive());
                Platform.runLater(() -> { showMsg(promoMsgLabel, "✓ Статус изменён", true); loadPromos(); });
            } catch (Exception e) {
                Platform.runLater(() -> showMsg(promoMsgLabel, "Ошибка", false));
            }
        }).start();
    }

    @FXML public void handleDeletePromo() {
        PromoCode sel = promoTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        if (!confirmDelete("промокод «" + sel.getCode() + "»")) return;
        new Thread(() -> {
            try {
                promoCodeService.delete(sel.getId());
                Platform.runLater(() -> { showMsg(promoMsgLabel, "✓ Удалён", true); loadPromos(); });
            } catch (Exception e) {
                Platform.runLater(() -> showMsg(promoMsgLabel, "Ошибка", false));
            }
        }).start();
    }

    // ══════════════════════════════════════════════
    //  ORDERS ACTIONS
    // ══════════════════════════════════════════════

    @FXML public void handleUpdateOrderStatus() {
        Order  sel    = ordersTable.getSelectionModel().getSelectedItem();
        String status = statusCombo.getValue();
        if (sel == null || status == null) {
            showMsg(oMsgLabel, "Выберите заказ и новый статус", false); return;
        }
        new Thread(() -> {
            try {
                orderService.updateStatus(sel.getId(), status);
                Platform.runLater(() -> { showMsg(oMsgLabel, "✓ Статус обновлён", true); loadOrders(); });
            } catch (Exception e) {
                Platform.runLater(() ->
                        showMsg(oMsgLabel, "Ошибка: " + e.getMessage(), false));
            }
        }).start();
    }

    // ══════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════

    private boolean confirmDelete(String target) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "Удалить " + target + "?", ButtonType.YES, ButtonType.NO);
        a.setHeaderText(null);
        a.getDialogPane().getStylesheets().add(
                getClass().getResource(
                        "/org/clothingstore/css/styles.css").toExternalForm());
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private void showMsg(Label lbl, String msg, boolean ok) {
        lbl.setText(msg);
        lbl.getStyleClass().setAll(ok ? "success-label" : "error-label");
        lbl.setVisible(true);
    }

    private String nvl(String s) { return s != null ? s : ""; }

    @FXML public void handleBack() { NavigationManager.navigateTo("main"); }
}