module com.example.snakegame {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.google.protobuf;

    opens com.example.snakegame to javafx.fxml;
}