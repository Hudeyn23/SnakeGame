module com.example.snakegame {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.google.protobuf;
    exports com.example.snakegame;
    opens com.example.snakegame to javafx.fxml;
    exports com.example.snakegame.net;
    opens com.example.snakegame.net to javafx.fxml;
    exports com.example.snakegame.ui;
    opens com.example.snakegame.ui to javafx.fxml;
    opens me.ippolitov.fit.snakes to com.google.protobuf;
}