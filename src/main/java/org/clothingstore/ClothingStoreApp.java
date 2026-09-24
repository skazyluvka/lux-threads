package org.clothingstore;

import org.clothingstore.util.NavigationManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class ClothingStoreApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        NavigationManager.init(primaryStage);
        NavigationManager.navigateTo("login");
        primaryStage.setTitle("LUXE THREADS — Дизайнерская Одежда");
        primaryStage.setMinWidth(1200);
        primaryStage.setMinHeight(780);
        primaryStage.setWidth(1280);
        primaryStage.setHeight(800);
        primaryStage.show();
    }


    public static void main(String[] args) { launch(args); }
}
