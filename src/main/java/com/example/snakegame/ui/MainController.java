package com.example.snakegame.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class MainController {
    @FXML
    private Button exitBtn;

    @FXML
    private Button findGameBtn;

    @FXML
    private Button startGameBtn;

    @FXML
    void exit(ActionEvent event) {
        Stage stage = (Stage) exitBtn.getScene().getWindow();
        stage.close();
    }

    @FXML
    void findGame(ActionEvent event) throws IOException, InterruptedException {
        FXMLLoader loader = new FXMLLoader();
        Parent root = loader.load(getClass().getResource("findGame.fxml").openStream());
        FindController controller = loader.getController();
        controller.start();
        Scene scene = new Scene(root);
        Stage primaryStae = new Stage();
        primaryStae.setScene(scene);
        primaryStae.show();
        Stage stage = (Stage) exitBtn.getScene().getWindow();
        stage.close();
    }

    @FXML
    void startGame(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("startGame.fxml"));
        Scene scene = new Scene(root);
        Stage primaryStage = new Stage();
        primaryStage.setTitle("Create game");
        primaryStage.setScene(scene);
        primaryStage.initModality(Modality.NONE);
        primaryStage.show();
    }
}