package com.example.snakegame.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import me.ippolitov.fit.snakes.SnakesProto;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class StartController implements Initializable {

    @FXML
    private TextField dead_food_prob;

    @FXML
    private TextField food_per_player;

    @FXML
    private TextField food_static;

    @FXML
    private TextField height;

    @FXML
    private TextField nameField;

    @FXML
    private TextField note_timeout_ms;

    @FXML
    private TextField ping_delay_ms;

    @FXML
    private Button startBtn;

    @FXML
    private TextField state_delay_ms;


    @FXML
    private TextField witdth;

    @FXML
    void startGame(ActionEvent event) throws IOException {
        SnakesProto.GameConfig gameConfig = SnakesProto.GameConfig.newBuilder()
                .setDeadFoodProb(Float.parseFloat(dead_food_prob.getText()))
                .setFoodPerPlayer(Float.parseFloat(food_per_player.getText()))
                .setFoodStatic(Integer.parseInt(food_static.getText()))
                .setHeight(Integer.parseInt(height.getText()))
                .setNodeTimeoutMs(Integer.parseInt(note_timeout_ms.getText()))
                .setPingDelayMs(Integer.parseInt(ping_delay_ms.getText()))
                .setStateDelayMs(Integer.parseInt(state_delay_ms.getText()))
                .setWidth(Integer.parseInt(witdth.getText()))
                .build();
        String name = nameField.getText();
        FXMLLoader loader = new FXMLLoader();
        Parent root = loader.load(getClass().getResource("game.fxml").openStream());
        GameController gameController = loader.getController();
        gameController.startMasterGame(gameConfig,name,root);
        Stage stage = (Stage) startBtn.getScene().getWindow();
        stage.close();

    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
    }
}
