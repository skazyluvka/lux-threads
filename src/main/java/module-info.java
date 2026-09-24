module org.clothingstore {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires java.net.http;

    opens org.clothingstore            to javafx.fxml;
    opens org.clothingstore.controller to javafx.fxml;
    opens org.clothingstore.model      to com.google.gson;

    exports org.clothingstore;
    exports org.clothingstore.model;
    exports org.clothingstore.service;
    exports org.clothingstore.util;
}
