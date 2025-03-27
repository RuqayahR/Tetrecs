package uk.ac.soton.comp1206.scene;

import static javafx.scene.input.KeyCode.ESCAPE;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.soton.comp1206.component.GameBoard;
import uk.ac.soton.comp1206.component.Leaderboard;
import uk.ac.soton.comp1206.component.PieceBoard;
import uk.ac.soton.comp1206.game.Grid;
import uk.ac.soton.comp1206.game.MultiplayerGame;
import uk.ac.soton.comp1206.ui.GamePane;
import uk.ac.soton.comp1206.ui.GameWindow;
import uk.ac.soton.comp1206.utility.Multimedia;

/**
 * The Multiplayer scene holds the UI for a multiplayer game and handles any
 * listeners used to link the game model and state to the UI.
 */
public class MultiplayerScene extends ChallengeScene{

  private static final Logger logger = LogManager.getLogger(MultiplayerScene.class);

  private TextFlow chat;
  private TextField chatInput;

  private ScrollPane chatScroller;

  /**
   * The game being played
   */
  private MultiplayerGame game;



  /**
   * Create a new Multiplayer scene
   *
   * @param gameWindow the Game Window
   */
  public MultiplayerScene(GameWindow gameWindow) {
    super(gameWindow);
  }

  /**
   * Build the multiplayer game window by adding all UI components. Also adds listeners to handle
   * interactions between the model (GameBoard) and the Game itself.
   */
  @Override
  public void build(){
    logger.info("Building " + this.getClass().getName());

    setupGame();

    root = new GamePane(gameWindow.getWidth(),gameWindow.getHeight());

    //create background
    var challengePane = new StackPane();
    challengePane.setMaxWidth(gameWindow.getWidth());
    challengePane.setMaxHeight(gameWindow.getHeight());
    challengePane.getStyleClass().add("menu-background");
    root.getChildren().add(challengePane);

    var mainPane = new BorderPane();
    challengePane.getChildren().add(mainPane);

    createGameBoards();

    //create title
    var title = createTitle("Multiplayer");
    mainPane.setTop(title);

    mainPane.setCenter(board);
    board.setAlignment(Pos.CENTER_LEFT);

    //create right side display
    var rightSide = createRightPane();
    mainPane.setRight(rightSide);
    var incomingHeader = new Text("Incoming");
    incomingHeader.getStyleClass().add("heading");
    rightSide.getChildren().addAll(showLivesInfo(),showLeaderboard(),incomingHeader,nextPieceBoard,followingPieceBoard);

    //create left side display
    var leftSide = createLeftPane();
    mainPane.setLeft(leftSide);
    leftSide.getChildren().addAll(showScoreInfo(),createChatBox());

    //bind score and lives properties
    score.textProperty().bind(game.scoreProperty().asString());
    lives.textProperty().bind(game.livesProperty().asString());

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
    game.setGameOverListener(game -> {
      //send message to server
      this.game.endGame();
      gameWindow.loadScene(new ScoresScene(gameWindow,game));
    });
    //Handle game looping
    game.setOnGameLoopListener(timeDelay -> Platform.runLater(()->{
      //create UI timer
      var timeBar = createTimer(timeDelay);
      mainPane.setBottom(timeBar);
    }));
    //Handle chat message recieved in game
    game.setChatReceivedListener(this::receiveChatMessage);
    //Handle block being clicked on the next piece board
    nextPieceBoard.setOnBlockClick(this::rightClicked);
    //Handle block being clicked on following piece board
    followingPieceBoard.setOnBlockClick(this::swapClicked);
    Platform.runLater(()-> root.requestFocus());
  }

  /**
   * Show the UI display of the leaderboard of player scores
   * @return the VBox containing the heading and scores
   */
  private VBox showLeaderboard() {
    var boardBox = new VBox(2);
    //add heading
    var heading = new Text("Scoreboard");
    heading.getStyleClass().add("heading");
    var leaderBoard = new Leaderboard();
    boardBox.getChildren().addAll(heading,leaderBoard);
    //bind leaderboard scores to the multiplayer game scores
    leaderBoard.multiplayerScoresProperty().bind(this.game.multiplayerScoresProperty());
    return boardBox;
  }

  @Override
  public void initialise() {
    logger.info("Initialising Multiplayer");
    game.start();
    scene.setOnKeyPressed(this::handleKey);
  }

  /**
   * Set up the game object and model
   */
  @Override
  public void setupGame() {
    logger.info("Starting a new challenge");

    //Start new game
    game = new MultiplayerGame(5, 5,gameWindow.getCommunicator());
    super.game = game;
    //play background music
    Multimedia.playMusic("game.wav");
  }

  /**
   * Receive a chat message from the server and add to chat display
   * @param receivedMessage the message received
   */
  private void receiveChatMessage(String receivedMessage) {
    receivedMessage = receivedMessage.replace("MSG ","");
    Text message = new Text(receivedMessage+"\n");
    chat.getChildren().add(message);
    chatScroller.setVvalue(1.0);
    root.requestFocus();
  }

  @Override
  protected void createGameBoards() {
    board = new GameBoard(game.getGrid(),gameWindow.getWidth()/2.5,gameWindow.getWidth()/2.5);
    nextPieceBoard = new PieceBoard(new Grid(3,3),gameWindow.getWidth()/7,gameWindow.getWidth()/7);
    nextPieceBoard.getBlock(1,1).indicatorBlock();
    followingPieceBoard = new PieceBoard(new Grid(3,3),gameWindow.getWidth()/8,gameWindow.getWidth()/8);
  }

  /**
   * Create chat message display for messages received from the server. Uses a text flow and scroll
   * pane to organise the messages.
   * @return returns the scroll pane
   */
  private ScrollPane createMessageDisplay() {
    chat = new TextFlow();
    chat.setLineSpacing(10);
    chat.getStyleClass().add("messages");
    //add an intro message to the text flow
    var introMessage = new Text("In-Game Chat: Type to send a message\n");
    introMessage.getStyleClass().add("introMessage");
    chat.getChildren().add(introMessage);
    chatScroller = new ScrollPane();
    chatScroller.getStyleClass().add("scroller");
    //change sizing
    chatScroller.setMaxWidth(150);
    chatScroller.setMaxHeight(250);
    chatScroller.setFitToWidth(true);
    chatScroller.setContent(chat);
    chatScroller.setPadding(new Insets(10,0,10,10));
    chatScroller.setMinViewportHeight(250);
    chatScroller.setMinViewportWidth(150);
    return chatScroller;
  }

  /**
   * Creates a chat box to display chat messages and adds input at the bottom for sending a message
   * @return the VBox for holding the chat
   */
  private VBox createChatBox(){
    var chatBox = new VBox();
    var scroller = createMessageDisplay();
    chatInput = new TextField();
    chatInput.setPromptText("Send message");
    //send chat message when user presses enter
    chatInput.setOnKeyPressed(event -> {
      if (event.getCode()!=KeyCode.ENTER) return;
      game.sendChatMessage(chatInput.getText());
      //clear text field
      chatInput.clear();
    });
    chatBox.getStyleClass().add("gameBox");
    chatBox.getChildren().addAll(scroller,chatInput);
    return chatBox;
  }

  /**
    Handle keyboard input
    @param keyEvent the key that was pressed
   */

  protected void handleKey(KeyEvent keyEvent){
    logger.info(keyEvent.getCode()+" key pressed");
    if (keyEvent.getCode()== ESCAPE){
      game.endGame();
      closeScene();
    }
    else{
      //check if text field is the focus
      if (chatInput.isFocused()) return;
      //else do keyboard controls for game
      gameControls(keyEvent);
    }
  }


  /**
   * Closes the scene by sending a message to the server and loading up the main menu
   */
  @Override
  protected void closeScene(){
    gameWindow.startMenu();
    this.game.endGame();
  }

}
