package org.clothingstore.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.clothingstore.controller.SellerController;

import java.io.IOException;

public class NavigationManager {

    private static Stage primaryStage;
    private static int pendingProductId = -1;

    public static void init(Stage stage) { primaryStage = stage; }

    public static void navigateTo(String view) {
        try {
            String path = "/org/clothingstore/fxml/" + view + ".fxml";
            FXMLLoader loader = new FXMLLoader(
                    NavigationManager.class.getResource(path));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    NavigationManager.class.getResource(
                            "/org/clothingstore/css/styles.css").toExternalForm());
            primaryStage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void navigateToMainWithProduct(int productId) {
        pendingProductId = productId;
        navigateTo("main");
    }

    public static int consumePendingProduct() {
        int id = pendingProductId;
        pendingProductId = -1;
        return id;
    }

    public static void navigateToSeller(String sellerId) {
        try {
            String path = "/org/clothingstore/fxml/seller.fxml";
            FXMLLoader loader = new FXMLLoader(
                    NavigationManager.class.getResource(path));
            Parent root = loader.load();
            SellerController controller = loader.getController();
            controller.setSellerId(sellerId);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    NavigationManager.class.getResource(
                            "/org/clothingstore/css/styles.css").toExternalForm());
            primaryStage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Stage getStage() { return primaryStage; }
}