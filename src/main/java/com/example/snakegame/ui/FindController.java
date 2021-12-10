package com.example.snakegame.ui;

import com.example.snakegame.GameInfo;
import com.example.snakegame.net.NormalNode;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class FindController implements Initializable {
    private Button add;
    private Map<Button, GameInfo> games;
    @FXML
    private VBox gamesVbox;
    private NormalNode node;
    private Thread timerThread;

    @FXML
    void addButon(ActionEvent event) {
        Button button = new Button("WOWOWOWOW");
        gamesVbox.getChildren().add(button);

    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    public void start() throws IOException, InterruptedException {
        games = new HashMap<>();
        node = new NormalNode();
        node.startListenGames();
        Thread timerThread = new Thread() {
            @Override
            public void run() {
                while (!isInterrupted()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            addToList(node.getGames());
                        }
                    });
                }
            }
        };
        timerThread.start();

    }

    public void addToList(Map<SocketAddress, GameInfo> map) {
        gamesVbox.getChildren().clear();
        for (Map.Entry<SocketAddress, GameInfo> entry : map.entrySet()) {
            Button button = new Button(entry.getKey().toString());
            games.put(button, entry.getValue());
            button.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    FXMLLoader loader = new FXMLLoader();
                    Parent root = null;
                    try {
                        root = loader.load(getClass().getResource("game.fxml").openStream());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    GameController gameController = loader.getController();
                    node.setController(gameController);
                    gameController.startGame(games.get(button).getConfig(), root, node);
                    Stage stage = (Stage) gamesVbox.getScene().getWindow();
                    stage.close();
                    node.joinGame(games.get(button).getMasterAddress(), gameController);
                    node.start();
                }
            });
            gamesVbox.getChildren().add(button);
        }
    }

}
