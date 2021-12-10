package com.example.snakegame.ui;

import com.example.snakegame.Model;
import com.example.snakegame.MoveDirection;
import com.example.snakegame.SnakeException;
import com.example.snakegame.net.AbstractNode;
import com.example.snakegame.net.MasterNode;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import me.ippolitov.fit.snakes.SnakesProto;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class GameController implements Initializable {
    @FXML
    private GridPane gridPane;
    @FXML
    private TextArea gamesArea;
    SnakesProto.GameConfig config;
    private Rectangle[][] rectangles;
    private AbstractNode node;

    public void startMasterGame(SnakesProto.GameConfig config, String name, Parent root) throws IOException {
        try {
            rectangles = new Rectangle[config.getWidth()][config.getHeight()];
            this.config = config;
            showScene(root);
            MasterNode masterNode = new MasterNode(this, name, config);
            masterNode.start();
            node = masterNode;

        } catch (SnakeException e) {
            e.printStackTrace();
        }
    }

    public void startGame(SnakesProto.GameConfig config, Parent root, AbstractNode node) {
        this.config = config;
        this.node = node;
        rectangles = new Rectangle[config.getWidth()][config.getHeight()];
        showScene(root);
    }

    private void showScene(Parent root) {
        Scene scene = new Scene(root);
        root.requestFocus();
        root.setFocusTraversable(false);
        scene.setOnKeyPressed(keyEvent -> {
            System.out.println(keyEvent.getCode());
            switch (keyEvent.getCode()) {
                case UP -> node.setTurn(MoveDirection.UP);
                case DOWN -> node.setTurn(MoveDirection.DOWN);
                case RIGHT -> node.setTurn(MoveDirection.RIGHT);
                case LEFT -> node.setTurn(MoveDirection.LEFT);
            }
        });
        Stage primaryStage = new Stage();
        primaryStage.setTitle("game");
        primaryStage.setScene(scene);
        primaryStage.initModality(Modality.NONE);
        primaryStage.show();
        gridPane.setGridLinesVisible(false);
        initFields();
    }

    private void initFields() {
        for (int rows = 0; rows < config.getWidth(); rows++) {
            for (int columns = 0; columns < config.getHeight(); columns++) {
                Rectangle rectangle = new Rectangle(gridPane.getMaxWidth() / config.getWidth(), gridPane.getMaxHeight() / config.getHeight());
                rectangle.setFill(Color.WHITE);
                rectangle.setStroke(Color.BLACK);
                rectangle.setStrokeWidth(0.05);
                rectangles[rows][columns] = rectangle;
                gridPane.add(rectangle, rows, columns);   // столбец=1 строка=0
            }
        }
    }

    public void update(SnakesProto.GameState state) {
        List<SnakesProto.GameState.Snake> snakes = state.getSnakesList();
        List<SnakesProto.GameState.Coord> food = state.getFoodsList();
        freeField();
        drawSnakes(snakes);
        drawFood(food);
    }

    private void freeField() {
        for (Rectangle[] rectangleArray : rectangles) {
            for (Rectangle rectangle : rectangleArray) {
                rectangle.setFill(Color.WHITE);
            }
        }
    }

    private void drawSnakes(List<SnakesProto.GameState.Snake> snakes) {
        for (SnakesProto.GameState.Snake snake : snakes) {
            ;
            List<SnakesProto.GameState.Coord> coords = snake.getPointsList();
            SnakesProto.GameState.Coord prevCoord = SnakesProto.GameState.Coord.newBuilder().setX(0).setY(0).build();
            for (SnakesProto.GameState.Coord coord : coords) {
                rectangles[Model.divideModulo(coord.getX() + prevCoord.getX(), config.getWidth())]
                        [Model.divideModulo(coord.getY() + prevCoord.getY(), config.getHeight())].setFill(Color.BLACK);
                prevCoord = SnakesProto.GameState.Coord.newBuilder()
                        .setX(Model.divideModulo(coord.getX() + prevCoord.getX(), config.getWidth()))
                        .setY(Model.divideModulo(coord.getY() + prevCoord.getY(), config.getHeight()))
                        .build();
            }
        }
    }

    public void setNode(AbstractNode node) {
        this.node = node;
    }

    private void drawFood(List<SnakesProto.GameState.Coord> coords) {
        for (SnakesProto.GameState.Coord coord : coords) {
            rectangles[coord.getX()][coord.getY()].setFill(Color.YELLOW);
        }
    }


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }
}
