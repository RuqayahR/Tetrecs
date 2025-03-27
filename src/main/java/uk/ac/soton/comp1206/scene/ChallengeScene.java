package uk.ac.soton.comp1206.scene;

import static javafx.scene.input.KeyCode.ESCAPE;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import javafx.animation.FillTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.soton.comp1206.component.GameBlock;
import uk.ac.soton.comp1206.component.GameBlockCoordinate;
import uk.ac.soton.comp1206.component.GameBoard;
import uk.ac.soton.comp1206.component.PieceBoard;
import uk.ac.soton.comp1206.game.Game;
import uk.ac.soton.comp1206.game.GamePiece;
import uk.ac.soton.comp1206.game.Grid;
import uk.ac.soton.comp1206.ui.GamePane;
import uk.ac.soton.comp1206.ui.GameWindow;
import uk.ac.soton.comp1206.utility.Multimedia;

/**
 * The Single Player challenge scene. Holds the UI for the single player game and handles any
 * listeners used to link the game model and state to the UI.
 */
public class ChallengeScene extends BaseScene {

    private static final Logger logger = LogManager.getLogger(ChallengeScene.class);

    /**
     * The game being played
     */
    protected Game game;

    /**
     * Displays the current piece to play
     */
    protected PieceBoard nextPieceBoard;

    /**
     * The UI for the game to be played on
     */
    protected GameBoard board;

    /**
     * Displays the following piece
     */
    protected PieceBoard followingPieceBoard;

    /**
     * Keeps track of the block currently being aimed at. Initialised to the middle block.
     */
    protected GameBlockCoordinate aimedBlock = new GameBlockCoordinate(2,2);

    /**
     * Keeps track of the previously aimed block
     */
    protected GameBlockCoordinate previouslyAimedBlock;

    /**
     * UI text representing level - binds to game property
     */
    protected Text level;

    /**
     * UI text representing the score - binds to game property
     */
    protected Text score;

    /**
     * UI text representing the high score - binds to game property
     */
    protected Text highscore;

    /**
     * UI text representing lives - binds to game property
     */
    protected Text lives;

    /**
     * UI text representing the multiplier - binds to game property
     */
    protected Text multiplier;

    /**
     * the main BorderPane which organises the UI elements for the scene
     */
    protected BorderPane mainPane = new BorderPane();



    /**
     * Create a new Single Player challenge scene
     * @param gameWindow the Game Window
     */
    public ChallengeScene(GameWindow gameWindow) {
        super(gameWindow);
        logger.info("Creating Challenge Scene");
    }

    /**
     * Set up the game object and model
     */
    public void setupGame() {
        logger.info("Starting a new challenge");

        //Start new game
        game = new Game(5, 5);
        Multimedia.playMusic("game.wav");
    }

    /**
     * Initialise the scene and start the game
     */
    @Override
    public void initialise() {
        logger.info("Initialising Challenge");
        //start game
        game.start();
        //handle keyboard input
        scene.setOnKeyPressed(this::handleKey);
    }


    /**
     * Build the Challenge window by adding all UI components. Also adds listeners to handle
     * interactions between the model (GameBoard) and the Game itself.
     */
    @Override
    public void build() {
        logger.info("Building " + this.getClass().getName());

        setupGame();

        root = new GamePane(gameWindow.getWidth(),gameWindow.getHeight());

        //add background stack pane
        var challengePane = new StackPane();
        challengePane.setMaxWidth(gameWindow.getWidth());
        challengePane.setMaxHeight(gameWindow.getHeight());
        challengePane.getStyleClass().add("menu-background");
        root.getChildren().add(challengePane);

        challengePane.getChildren().add(mainPane);

        //initialise all game boards
        createGameBoards();

        //add title to top of border pane
        var title = createTitle("Challenge Mode");
        mainPane.setTop(title);

        //add main game board to centre of border pane
        mainPane.setCenter(board);
        board.setAlignment(Pos.CENTER_LEFT);

        //create the right side of the border pane
        var rightSide = createRightPane();
        mainPane.setRight(rightSide);
        var incomingHeader = new Text("Incoming");
        incomingHeader.getStyleClass().add("heading");
        rightSide.getChildren().addAll(showLivesInfo(),incomingHeader,nextPieceBoard,followingPieceBoard);

        //create left side of border pane
        var leftSide = createLeftPane();
        mainPane.setLeft(leftSide);
        leftSide.getChildren().addAll(showScoreInfo(),showMultiplierInfo(),showLevelInfo(),showHighscoreInfo());

        bindProperties();

        handleListeners();

    }

    /**
     * Binds game properties to the UI text so that they update accordingly
     */
    protected void bindProperties() {
        game.setHighscore(getHighScore());
        score.textProperty().bind(game.scoreProperty().asString());
        level.textProperty().bind(game.levelProperty().asString());
        lives.textProperty().bind(game.livesProperty().asString());
        multiplier.textProperty().bind(game.multiplierProperty().asString().concat("x"));
        highscore.textProperty().bind(game.highscoreProperty().asString());
    }

    /**
     * Add listeners to the game and game boards to handle the events
     */
    protected void handleListeners() {
        //Handle block on game board being hovered over
        board.setOnBlockHovered(this::blockHover);
        //Handle block on game board grid being clicked
        board.setOnBlockClick(this::blockClicked);
        //Handle right click on game board
        board.setOnRightClicked(this::rightClicked);
        //Handle next piece being spawned the game
        game.setNextPieceListener(this::nextPiece);
        //Handle lines being cleared in the game
        game.setLineClearedListener(this::lineCleared);
        //Handle game ending
        game.setGameOverListener(game -> gameOver());
        //Handle game looping
        game.setOnGameLoopListener(timeDelay -> Platform.runLater(()->{
            //create UI timer
            var timeBar = createTimer(timeDelay);
            mainPane.setBottom(timeBar);
        }));
        //Handle block being clicked on the next piece board
        nextPieceBoard.setOnBlockClick(this::rightClicked);
        //Handle block being clicked on following piece board
        followingPieceBoard.setOnBlockClick(this::swapClicked);
    }

    /**
     * Ends game by switching to the scores scene
     */
    private void gameOver() {
        Multimedia.playAudio("explode.wav");
        gameWindow.loadScene(new ScoresScene(gameWindow,game));
    }

    /**
     * Create the 3 game boards:
     * The main game board on which the game is played
     * The piece board to show the current piece
     * The following piece board to show the upcoming piece
     */
    protected void createGameBoards() {
        board = new GameBoard(game.getGrid(),gameWindow.getWidth()/2,gameWindow.getWidth()/2);
        nextPieceBoard = new PieceBoard(new Grid(3,3),gameWindow.getWidth()/6,gameWindow.getWidth()/6);
        nextPieceBoard.getBlock(1,1).indicatorBlock();
        followingPieceBoard = new PieceBoard(new Grid(3,3),gameWindow.getWidth()/7,gameWindow.getWidth()/7);
    }

    /**
     * Creates title from given String heading with correct positioning and styling
     * @param heading the title text
     * @return returns the title
     */
    protected Text createTitle(String heading){
        var title = new Text(heading);
        title.getStyleClass().add("title");
        BorderPane.setAlignment(title, Pos.TOP_CENTER);
        BorderPane.setMargin(title, new Insets(10,10,0,10));
       return title;
    }

    /**
     * Create a FlowPane used to hold multiple UI components with correct positioning for right hand
     * side of the scene
     * @return returns FlowPane for the right hand side of scene
     */
    protected FlowPane createRightPane() {
        var rightSide = new FlowPane(Orientation.VERTICAL);
        rightSide.setVgap(20);
        rightSide.setPadding(new Insets(0,70,10,0));
        return rightSide;
    }

    /**
     * Create a FlowPane used to multiple UI components with correct positioning for the left hand
     * side of the scene
     * @return returns FlowPane for the left hand side of scene
     */
    protected FlowPane createLeftPane(){
        var leftSide = new FlowPane(Orientation.VERTICAL);
        leftSide.setVgap(20);
        leftSide.setPadding(new Insets(0,0,10,10));
        return leftSide;
    }

    /**
     * Create a VBox to hold the display for the score information with correct styling
     * @return the VBox holding score text and header
     */
    protected VBox showScoreInfo(){
        var scoreInfo = new VBox();
        var scoreHeading = new Text("Score");
        scoreHeading.getStyleClass().add("heading");
        score = new Text();
        score.getStyleClass().add("score");
        scoreInfo.getChildren().addAll(scoreHeading,score);
        return scoreInfo;
    }

    /**
     * Create a VBox to hold the display for the level information with correct styling
     * @return the VBox holding the level text and header
     */
    protected VBox showLevelInfo(){
        var levelInfo = new VBox();
        var levelHeading = new Text("Level");
        levelHeading.getStyleClass().add("heading");
        level = new Text();
        level.getStyleClass().add("level");
        levelInfo.getChildren().addAll(levelHeading,level);
        return levelInfo;
    }

    /**
     * Create a VBox to hold the display for the lives information with correct styling
     * @return the VBox holding the lives text and header
     */
    protected VBox showLivesInfo(){
        var livesInfo = new VBox();
        livesInfo.setAlignment(Pos.TOP_LEFT);
        var livesHeading = new Text("Lives");
        livesHeading.getStyleClass().add("heading");
        lives = new Text();
        lives.getStyleClass().add("lives");
        livesInfo.getChildren().addAll(livesHeading,lives);
        return livesInfo;
    }

    /**
     * Create a VBox to hold the display for the multiplier information with correct styling
     * @return the VBox holding the multiplier text and header
     */
    protected VBox showMultiplierInfo(){
        var multiplierInfo = new VBox();
        var multiplierHeading = new Text("Multiplier");
        multiplierHeading.getStyleClass().add("heading");
        multiplier = new Text();
        multiplier.getStyleClass().add("multiplier");
        multiplierInfo.getChildren().addAll(multiplierHeading,multiplier);
        return multiplierInfo;
    }

    /**
     * Create a VBox to hold the display for the high score information with correct styling
     * @return the VBox holding the high score text and header
     */
    protected VBox showHighscoreInfo(){
        var highscoreInfo = new VBox();
        var highscoreHeading = new Text("Highscore");
        highscoreHeading.getStyleClass().add("heading");
        highscore = new Text();
        highscore.getStyleClass().add("highscore");
        highscoreInfo.getChildren().addAll(highscoreHeading,highscore);
        return highscoreInfo;
    }


    /**
     * Reads top high score from ordered scores file. Else returns 0.
     * @return the top high score
     */
    public Integer getHighScore(){
        try{
            //read the first line as file is ordered
            BufferedReader reader = new BufferedReader(new FileReader("scores.txt"));
            var line = reader.readLine();
            var score = line.split(":");
            //return the score
            return Integer.valueOf(score[1]);
        }catch (IOException e) {
            logger.error(e);
            //return 0 if file cannot be found or any other IO exception
            return 0;
        }
    }

    /**
     * Creates the UI countdown timer which decreases in size and changes colour from green
     * to red as time runs out.
     * @param time the countdown time
     * @return the rectangle used to show the timer
     */
    protected Rectangle createTimer(int time){
        logger.info("Making timer with {} time",time);
        //create timer bar using a rectangle that spans the window width
        var timeBar = new Rectangle(gameWindow.getWidth(),30);
        //set initial colour to green
        timeBar.setFill(Color.GREEN);

        //create timeline to provide smooth transitions at different times
        Timeline timeline = new Timeline(
            //starting keyframe triggers event that causes colour change at specified times
            //width of time bar starts as width of game window
            new KeyFrame(Duration.ZERO, event -> {
                var ft1 = new FillTransition(Duration.millis(time/3), timeBar, Color.GREEN, Color.YELLOW);
                var ft2 = new FillTransition(Duration.millis(time/3),timeBar,Color.YELLOW,Color.ORANGE);
                var ft3 = new FillTransition(Duration.millis(time/3), timeBar, Color.ORANGE, Color.RED);
                //play fill transitions one after the other
                var seq = new SequentialTransition(ft1,ft2,ft3);
                seq.play();
            },new KeyValue(timeBar.widthProperty(), gameWindow.getWidth())),

            //end key frame has width of the time bar as 0
            new KeyFrame(Duration.millis(time),new KeyValue(timeBar.widthProperty(),0))
        );

        //play timeline to animate the timer bar
        timeline.play();
        return timeBar;
    }

    /**
     * Adds hover effect to current aimed block and removes hover from previously aimed block
     * @param block the game block being aimed at
     */
    protected void blockHover(GameBlock block){
        setPreviouslyAimedBlock();
        undoHover();
        aimedBlock = new GameBlockCoordinate(block.getX(), block.getY());
        //add hover effect
        block.hover();
    }

    /**
     * Sets the previously aimed block as the current aimed block if it's not null.
     * Used before hovering a new block.
     */
    protected void setPreviouslyAimedBlock(){
        if (aimedBlock != null){
            previouslyAimedBlock = new GameBlockCoordinate(aimedBlock.getX(),aimedBlock.getY());
        }
    }

    /**
     * Calls game block method to remove hover effect on the game block stored as the previously
     * aimed block.
     */
    protected void undoHover(){
        if (previouslyAimedBlock != null) {
            board.getBlock(previouslyAimedBlock.getX(), previouslyAimedBlock.getY()).hoverRemove();
        }
    }

    /**
     * Handles a right click by rotating the current piece and displaying it on the next piece board
     * @param gameBlock the block that was clicked
     */
    protected void rightClicked(GameBlock gameBlock) {
        //play sound effect
        Multimedia.playAudio("rotate.wav");
        //display rotated piece
        nextPieceBoard.setPiece(game.rotateCurrentPiece());
    }

    /**
     * Rotates the current game piece with the given number of rotations
     * and displays it on the next piece board. Used for keyboard control.
     * @param num the number of rotations
     */
    protected void rotatePiece(int num){
        //play sound effect
        Multimedia.playAudio("rotate.wav");
        //display rotated piece
        nextPieceBoard.setPiece(game.rotateCurrentPiece(num));
    }

    /**
     * Handles the following piece board being clicked. Swaps the current and following game pieces
     * and displays them
     * @param gameBlock the block that was clicked
     */
    protected void swapClicked(GameBlock gameBlock){
        //play sound effect
        Multimedia.playAudio("transition.wav");
        //swap pieces
        var swappedPieces = game.swapCurrentPiece();
        //display swapped pieces
        nextPieceBoard.setPiece(swappedPieces[0]);
        followingPieceBoard.setPiece(swappedPieces[1]);
    }

    /**
     * Swaps the current and following game pieces and displays them. Used for keyboard control instead
     * of mouse click.
     */
    protected void swapClicked(){
        //play sound effect
        Multimedia.playAudio("transition.wav");
        //swap pieces
        var swappedPieces = game.swapCurrentPiece();
        //display swapped pieces
        nextPieceBoard.setPiece(swappedPieces[0]);
        followingPieceBoard.setPiece(swappedPieces[1]);
    }

    /**
     * Handle when a block is clicked
     * @param gameBlock the Game Block that was clicked
     */
    protected void blockClicked(GameBlock gameBlock) {
        game.blockClicked(gameBlock);
    }

    /**
     * Handle when the next game piece has been generated. Displays updated game pieces on piece
     * boards
     * @param currentGamePiece the new current game piece
     * @param followingGamePiece the new following game piece
     */
    protected void nextPiece(GamePiece currentGamePiece,GamePiece followingGamePiece){
        nextPieceBoard.setPiece(currentGamePiece);
        followingPieceBoard.setPiece(followingGamePiece);
    }

    /**
     * Handle when a line has been cleared. Triggers fade out effect on cleared blocks.
     * @param blocks the blocks that have been cleared
     */
    protected void lineCleared(HashSet<GameBlockCoordinate> blocks){
        //play sound effect
        Multimedia.playAudio("clear.wav");
        //trigger fade out effect on blocks
        board.fadeOut(blocks);
    }


    /**
     * Handle keyboard input on scene
     * @param keyEvent the key that was pressed
     */
    protected void handleKey(KeyEvent keyEvent){
        logger.info(keyEvent.getCode()+" key pressed");
        //if ESC pressed, end game
        if (keyEvent.getCode()== ESCAPE){
            game.endGame();
            closeScene();
        }
        //handle game keyboard controls
        else{
            gameControls(keyEvent);
        }
    }

    /**
     * Handles Keyboard controls for the Game
     * @param keyEvent the key pressed
     */
    protected void gameControls(KeyEvent keyEvent){
        switch (keyEvent.getCode()){
            //change aim by moving up
            case W, UP -> {
                if (aimedBlock.getY()>0){
                setPreviouslyAimedBlock();
                undoHover();
                aimedBlock = aimedBlock.subtract(0,1);
                logger.info("Hovering at {}",board.getBlock(aimedBlock.getX(),aimedBlock.getY()));
                board.getBlock(aimedBlock.getX(),aimedBlock.getY()).hover();
                }
            }
            //change aim by moving left
            case A,LEFT -> {
                if (aimedBlock.getX()>0) {
                    setPreviouslyAimedBlock();
                    undoHover();
                    aimedBlock = aimedBlock.subtract(1, 0);
                    board.getBlock(aimedBlock.getX(), aimedBlock.getY()).hover();
                }
            }
            //change aim by moving down
            case S,DOWN -> {
                if (aimedBlock.getY()<4) {
                    setPreviouslyAimedBlock();
                    undoHover();
                    aimedBlock = aimedBlock.add(0, 1);
                    board.getBlock(aimedBlock.getX(), aimedBlock.getY()).hover();
                }
            }
            //change aim by moving right
            case D,RIGHT ->{
                if (aimedBlock.getX()<4) {
                    setPreviouslyAimedBlock();
                    undoHover();
                    aimedBlock = aimedBlock.add(1, 0);
                    board.getBlock(aimedBlock.getX(), aimedBlock.getY()).hover();
                }
            }
            case Q,Z,OPEN_BRACKET -> rotatePiece(3);
            case E,C,CLOSE_BRACKET -> rotatePiece(1);
            //place block at current aim
            case ENTER, X -> blockClicked(board.getBlock(aimedBlock.getX(),aimedBlock.getY()));
            //swap pieces
            case SPACE, R -> swapClicked();

        }
    }

    /**
     * Closes challenge scene by loading up menu scene in the game window
     */
    protected void closeScene() {
        gameWindow.startMenu();
    }


}
