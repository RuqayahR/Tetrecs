package uk.ac.soton.comp1206.scene;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.soton.comp1206.App;
import uk.ac.soton.comp1206.network.Communicator;
import uk.ac.soton.comp1206.ui.GamePane;
import uk.ac.soton.comp1206.ui.GameWindow;
import uk.ac.soton.comp1206.utility.Multimedia;

/**
 * The main menu of the game. Provides a gateway to the rest of the game.
 */
public class MenuScene extends BaseScene {

    private static final Logger logger = LogManager.getLogger(MenuScene.class);

    private Communicator communicator;

    /**
     * Create a new menu scene
     * @param gameWindow the Game Window this will be displayed in
     */
    public MenuScene(GameWindow gameWindow) {
        super(gameWindow);
        logger.info("Creating Menu Scene");
        communicator = gameWindow.getCommunicator();
    }

    /**
     * Build the menu layout
     */
    @Override
    public void build() {
        logger.info("Building " + this.getClass().getName());

        root = new GamePane(gameWindow.getWidth(),gameWindow.getHeight());

        //set up background
        var menuPane = new StackPane();
        menuPane.setMaxWidth(gameWindow.getWidth());
        menuPane.setMaxHeight(gameWindow.getHeight());
        menuPane.getStyleClass().add("menu-background");
        root.getChildren().add(menuPane);

        var mainPane = new BorderPane();
        menuPane.getChildren().add(mainPane);

        //Create title using logo image
        var top = new HBox();
        var titleImage = new ImageView(new Image(this.getClass().getResource("/images/TetrECS.png").toExternalForm()));
        titleImage.setPreserveRatio(true);
        titleImage.setFitWidth(gameWindow.getWidth()/1.5);
        top.getChildren().add(titleImage);
        mainPane.setTop(top);
        top.setAlignment(Pos.TOP_CENTER);
        top.setPadding(new Insets(100,0,20,0));

        //add animations to title image
        createTitleAnimation(titleImage);

        //Add button for different options. Style as wanted
        var singlePlayer = new Button("Single Player");
        singlePlayer.getStyleClass().add("menuItem");
        var multiplayer = new Button ("Multiplayer");
        multiplayer.getStyleClass().add("menuItem");
        var instructions = new Button ("How to Play");
        instructions.getStyleClass().add("menuItem");
        var exit = new Button ("Exit");
        exit.getStyleClass().add("menuItem");
        var options = new VBox(0);
        options.getChildren().addAll(singlePlayer,multiplayer,instructions,exit);
        mainPane.setBottom(options);
        options.setAlignment(Pos.BOTTOM_CENTER);
        options.setPadding(new Insets(20,50,50,50));

        //Bind the button action to the startGame method in the menu
        singlePlayer.setOnAction(this::startGame);
        //Bind the button action to the showInstructions method in the menu
        instructions.setOnAction(this::showInstructions);
        //Bind the button action to the startMultiplayer method in the menu
        multiplayer.setOnAction(this::startMultiplayer);
        //Bind the button action to shut down the program to exit
        exit.setOnAction(event -> {
            communicator.send("QUIT");
            App.getInstance().shutdown();
        });

    }

    /**
     * Create animations for the title image
     * @param title the image to animate
     */
    private void createTitleAnimation(ImageView title) {
        //create rotation animation to rotate back and forth
        RotateTransition rotateTransition = new RotateTransition(Duration.millis(2000),title);
        rotateTransition.setFromAngle(-10);
        rotateTransition.setToAngle(10);
        rotateTransition.setInterpolator(Interpolator.LINEAR);
        rotateTransition.setAutoReverse(true);
        rotateTransition.setCycleCount(Timeline.INDEFINITE);

        //create scale rotation to grow and shrink
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(2000),title);
        scaleTransition.setFromX(0.8);
        scaleTransition.setToX(1.2);
        scaleTransition.setFromY(0.8);
        scaleTransition.setToY(1.2);
        scaleTransition.setInterpolator(Interpolator.LINEAR);
        scaleTransition.setAutoReverse(true);
        scaleTransition.setCycleCount(Timeline.INDEFINITE);

        //rotate and scale at the same time
        ParallelTransition p = new ParallelTransition(title,rotateTransition,scaleTransition);

        //fade in the image at start
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(2000),title);
        fadeTransition.setFromValue(0);
        fadeTransition.setToValue(1);
        //once faded in, rotate and shrink
        fadeTransition.setOnFinished(event -> p.play());

        //start the fade in animation
        fadeTransition.play();
    }


    /**
     * Initialise the menu
     */
    @Override
    public void initialise() {
        Multimedia.playMusic("menu.mp3");
    }

    /**
     * Handle when the Start Game button is pressed
     * @param event event
     */
    private void startGame(ActionEvent event) {
        gameWindow.startChallenge();
    }

    /**
     * Handle when the start Multiplayer game button is pressed
     * @param event event
     */
    private void startMultiplayer(ActionEvent event){gameWindow.loadScene(new LobbyScene(gameWindow));}

    /**
     * Handle when the instructions button is pressed
     * @param event event
     */
    private void showInstructions(ActionEvent event){
        gameWindow.loadScene(new InstructionsScene(gameWindow));
    }


}
