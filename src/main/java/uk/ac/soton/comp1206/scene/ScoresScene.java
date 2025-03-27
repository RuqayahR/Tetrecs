package uk.ac.soton.comp1206.scene;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import javafx.application.Platform;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javatuples.Triplet;
import uk.ac.soton.comp1206.component.Leaderboard;
import uk.ac.soton.comp1206.component.ScoresList;
import uk.ac.soton.comp1206.event.CommunicationsListener;
import uk.ac.soton.comp1206.game.Game;
import uk.ac.soton.comp1206.game.MultiplayerGame;
import uk.ac.soton.comp1206.network.Communicator;
import uk.ac.soton.comp1206.ui.GamePane;
import uk.ac.soton.comp1206.ui.GameWindow;

/**
 * The Scores scene handles showing scores local and online high scores at the end of a game. If the
 * user beats any of the scores, their score is also shown and saved.
 */
public class ScoresScene extends BaseScene{

  private static final Logger logger = LogManager.getLogger(ScoresScene.class);

  /**
   * The game that has just been played
   */
  private Game game;

  /**
   * The observable list of local high scores
   */
  private ObservableList<Pair<String,Integer>> scoresList = FXCollections.observableArrayList();

  /**
   * The wrapper for the observable local high scores to expose it as a property. Used for binding.
   */
  private SimpleListProperty<Pair<String, Integer>> localScores = new SimpleListProperty<>(scoresList);

  /**
   * Observable list of online high scores
   */
  private ObservableList<Pair<String,Integer>> onlineScoresList = FXCollections.observableArrayList();

  /**
   * The wrapper for the observable online high scores to expose it as a property. Used for binding.
   */
  private SimpleListProperty<Pair<String, Integer>> remoteScores = new SimpleListProperty<>(onlineScoresList);

  /**
   * List property used for binding the scores from the multiplayer game
   */
  private SimpleListProperty<Triplet<String,Integer,String>> multiplayerList;

  /**
   * Buffered reader used for reading the scores file
   */
  private BufferedReader reader;
  private VBox mainDisplay;
  private BorderPane mainPane;

  /**
   * Communicator used for sending and receiving messages from the server
   */
  private final Communicator communicator;

  /**
   * Used for determining whether the game was a multiplayer game or not
   */
  private boolean multiplayer = false;

  /**
   * Create a new scores scene
   * @param gameWindow the Game Window
   * @param game the Game object that has just been played
   */
  public ScoresScene(GameWindow gameWindow, Game game) {
    super(gameWindow);
    logger.info("Creating Scores Scene");
    this.game = game;
    //use game window communicator
    this.communicator = gameWindow.getCommunicator();
    checkMultiplayer();
  }

  /**
   * Checks if the last game played was a multiplayer game. If true, loads in the multiplayer scores
   * from the multiplayer game
   */
  private void checkMultiplayer() {
    if (game instanceof MultiplayerGame) {
      //set multiplayer boolean
      multiplayer=true;
      //get multiplayer scores list property
      multiplayerList = ((MultiplayerGame) game).multiplayerScoresProperty();
    }
  }

  /**
   * Initialise the scene by loading local and online scores
   */
  @Override
  public void initialise() {
    logger.info("Initialising the scores scene");
    //handle keyboard input
    scene.setOnKeyPressed(this::handleKey);
    //check if no local scores exist
    writeDefaultScores();
    //load local and online scores
    loadOnlineScores();
    loadScores();
  }

  /**
   * Show either multiplayer or local high scores on the left of the screen and online high scores
   * on the right of the screen
   */
  private void showScores() {
    //logger.info("Displaying scores");
    mainDisplay.getChildren().removeAll();
    //add title
    var heading = new Text("High Scores");
    heading.getStyleClass().add("title");
    mainDisplay.getChildren().add(heading);

    //show local scores if not a multiplayer game
    if (!multiplayer) {
      var localScoresDisplay = createLocalScoresDisplay();
      mainPane.setLeft(localScoresDisplay);
    }
    //show multiplayer scores
    else{
      var multiplayerScoresDisplay = createMultiplayerScoresDisplay();
      mainPane.setLeft(multiplayerScoresDisplay);
    }

    //show online highscores
    var remoteScoresDisplay = createRemoteScoresDisplay();
    mainPane.setRight(remoteScoresDisplay);
  }

  /**
   * Create UI component for displaying the multiplayer game scores. Shown in a VBox with heading and
   * leaderboard of scores.
   * @return the VBox showing multiplayer scores
   */
  private VBox createMultiplayerScoresDisplay() {
    var multiplayerScores = new VBox();
    multiplayerScores.setSpacing(5);
    multiplayerScores.setAlignment(Pos.TOP_CENTER);
    multiplayerScores.setPadding(new Insets(20,20,0,150));
    //add heading
    var scoreLabel = new Text("Multiplayer Scores");
    scoreLabel.getStyleClass().add("heading");
    multiplayerScores.getChildren().add(scoreLabel);
    //create new leaderboard
    var scoreBox = new Leaderboard();
    multiplayerScores.getChildren().add(scoreBox);
    scoreBox.setFinalScores();
    //bind multiplayer scores to leaderboard list
    scoreBox.multiplayerScoresProperty().bind(multiplayerListProperty());
    return multiplayerScores;
  }

  /**
   * Create UI component for displaying the local high scores. Shown in a VBox with heading and
   * ScoresList to show scores.
   * @return the VBox displaying the local high scores
   */
  private VBox createLocalScoresDisplay(){
    var localScores = new VBox();
    localScores.setSpacing(5);
    localScores.setAlignment(Pos.TOP_CENTER);
    localScores.setPadding(new Insets(20,20,0,150));
    var scoreLabel = new Text("Local Scores");
    scoreLabel.getStyleClass().add("heading");
    localScores.getChildren().add(scoreLabel);
    var scoreBox = new ScoresList();
    localScores.getChildren().add(scoreBox);
    scoreBox.scoresListProperty().bind(localScoresProperty());
    return localScores;
  }

  /**
   * Create UI component for displaying the online high scores. Shown in a VBox with heading and
   * ScoresList to show scores.
   * @return the VBox displaying the online high scores
   */
  private VBox createRemoteScoresDisplay(){
    var remoteScores = new VBox();
    remoteScores.setSpacing(5);
    remoteScores.setAlignment(Pos.TOP_CENTER);
    remoteScores.setPadding(new Insets(20,150,0,20));
    var scoreLabel = new Text("Online Scores");
    scoreLabel.getStyleClass().add("heading");
    remoteScores.getChildren().add(scoreLabel);
    var scoreBox = new ScoresList();
    remoteScores.getChildren().add(scoreBox);
    scoreBox.onlineScoresListProperty().bind(remoteScoresProperty());
    return remoteScores;
  }


  /**
   * Checks if the game score was higher than any of the current local high scores or online high
   * scores. Does not compare to local high scores if the game was multiplayer.
   */
  private void scoreCheck() {
    logger.info("Checking score");
    int check;
    //check if player beat both online and local high scores
    if (scoresList.get(9).getValue()<game.getScore() &&
        onlineScoresList.get(9).getValue() < game.getScore() && !multiplayer){
      check = 1;
      logger.info("New local and online high score");
      inputUsername(check);
      return;
    }
    else{
      //check if player beat local high score
      if( scoresList.get(9).getValue() < game.getScore() && !multiplayer){
        check = 2;
        logger.info("New local high score");
        inputUsername(check);
        return;
      }
      //check if player beat online high score
      else if (onlineScoresList.get(9).getValue() < game.getScore()){
        check = 3;
        logger.info("New online high score");
        inputUsername(check);
        return;
      }
    }
    //display the scores
    showScores();
  }

  /**
   * Create the UI for the user to enter their username if they have beat a high score
   * @param check the high score type - online, local or both
   */
  private void inputUsername(int check){
    logger.info("Prompting user to enter username");
    //create new text field
    TextField nameInput = new TextField();
    nameInput.setPromptText("Enter your username");
    nameInput.getStyleClass().add("Textfield");
    //create submit button
    Button submit = new Button("Submit");
    submit.getStyleClass().add("submit");
    Text header = new Text("You got a high score!");
    header.getStyleClass().add("title");
    mainDisplay.getChildren().addAll(nameInput,submit,header);
    //add functionality to submit button
    submit.setOnAction(event -> {
      logger.info("Submit button pressed");
      //add new score
      addNewScore(nameInput.getText(),check);
      mainDisplay.getChildren().removeAll(nameInput,submit,header);
      showScores();
    });
    //add keyboard functionality to press enter
    nameInput.setOnKeyPressed(event -> {
      if (event.getCode() != KeyCode.ENTER) return;
      //add new score
      addNewScore(nameInput.getText(),check);
      mainDisplay.getChildren().removeAll(nameInput,submit,header);
      showScores();
    });
  }

  /**
   * Adds the new high score to the relevant list of high scores
   * @param name the username the player has entered
   * @param check the type of high score - local, online or both
   */
  private void addNewScore(String name, int check) {
    //new local and online high score
    if (check == 1){
      logger.info("Inputting score with name: {} to online and local scores",name);
      updateLocalScores(name);
      updateRemoteScores(name);
    }
    //new local high score
    else if (check == 2) {
      logger.info("Inputting score with name: {} to local scores",name);
      updateLocalScores(name);
    }
    //new online high score
    else {
      logger.info("Inputting score with name: {} to online scores",name);
      updateRemoteScores(name);
    }
  }

  /**
   * Add the score as a new Pair to the local high scores list
   * @param name the username the player entered
   */
  private void updateLocalScores(String name){
    scoresList.add(new Pair<>(name, game.getScore()));
    //sort the list
    scoresList.sort(
        Collections.reverseOrder(Comparator.comparing(Pair<String, Integer>::getValue)));
    //write updated scores to file
    writeScores("scores.txt");
  }

  /**
   * Add the score as a new Pair to the online high scores list
   * @param name the username the user entered
   */
  private void updateRemoteScores(String name){
    onlineScoresList.add(new Pair<>(name, game.getScore()));
    onlineScoresList.sort(
        Collections.reverseOrder(Comparator.comparing(Pair<String, Integer>::getValue)));
    writeOnlineScore(name);
  }

  /**
   * Build the scores screen by adding all UI components.
   */
  @Override
  public void build() {
    logger.info("Building " + this.getClass().getName());

    root = new GamePane(gameWindow.getWidth(),gameWindow.getHeight());

    //create background
    var scoresPane = new StackPane();
    scoresPane.setMaxWidth(gameWindow.getWidth());
    scoresPane.setMaxHeight(gameWindow.getHeight());
    scoresPane.getStyleClass().add("menu-background");
    root.getChildren().add(scoresPane);

    mainPane = new BorderPane();
    scoresPane.getChildren().add(mainPane);

    mainDisplay = new VBox();
    mainDisplay.setAlignment(Pos.CENTER);
    mainDisplay.setPadding(new Insets(20,0,10,0));
    mainDisplay.setSpacing(5);
    mainPane.setTop(mainDisplay);

    //add title image
    var titleImage = new ImageView(new Image(this.getClass().getResource("/images/TetrECS.png").toExternalForm()));
    titleImage.setPreserveRatio(true);
    titleImage.setFitWidth(gameWindow.getWidth()/1.5);

    //add title
    var title = new Text("Game Over");
    title.getStyleClass().add("bigtitle");
    mainDisplay.getChildren().addAll(titleImage,title);
  }

  /**
   * Get the local scores property
   * @return the ListProperty containing the local high scores
   */
  public SimpleListProperty<Pair<String, Integer>> localScoresProperty(){
    return localScores;
  }

  /**
   * Get the remote scores property
   * @return the ListProperty containing the online high scores
   */
  public SimpleListProperty<Pair<String, Integer>> remoteScoresProperty(){
    return remoteScores;
  }

  /**
   * Get the multiplayer scores property
   * @return the ListProperty containing the multiplayer scores from the multiplayer game
   */
  public SimpleListProperty<Triplet<String,Integer,String>> multiplayerListProperty(){
    return multiplayerList;
  }

  /**
   * Read the local high scores from the scores.txt file and add to the list of local scores
   */
  public void loadScores(){
    try {
      logger.info("Reading local high scores");
      //read from scores.txt file
      reader = new BufferedReader(new FileReader("scores.txt"));
        while (reader.ready()) {
          var line = reader.readLine();
          logger.info(line);
          var sc = line.split(":");
          //add to new score Pair to list
          var score = new Pair<>(sc[0],Integer.valueOf(sc[1]));
          scoresList.add(score);
        }
        //close reader
      reader.close();
    } catch(IOException e){
      logger.error(e);
    }
  }

  /**
   * Write the updated top 10 local high scores to the file
   * @param file the file to write to
   */
  public void writeScores(String file){
    try {
      logger.info("Writing local high scores to {}",file);
      BufferedWriter writer = new BufferedWriter(new FileWriter(file));
      for (int i=0;i<10;i++){
        var name = scoresList.get(i).getKey();
        var points = scoresList.get(i).getValue().toString();
        logger.debug("Writing {} and {}",name,points);
        //write score on new line
        writer.write(name+":"+points);
        writer.newLine();
      }
      writer.close();
    }
    catch (IOException e){
      logger.error(e);
    }
  }

  /**
   * If scores.txt file does not exist, create a default set of scores to go into a new scores.txt
   * file
   */
  public void writeDefaultScores(){
    try{
      File file = new File("scores.txt");
      //if file does not exist
      if (!file.exists()) {
        logger.info("Scores file does not exist. Writing default scores.");
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        //write 10 default scores
        for (int i = 0; i < 10; i++) {
          writer.write("Default:0");
          writer.newLine();
        }
        writer.close();
      }
    }
    catch(IOException e){
      logger.error(e);
    }
  }

  /**
   * Receive list of online high scores from the server
   */
  public void loadOnlineScores(){
    //send request
    communicator.send("HISCORES");
    //listen for received message
    communicator.addListener((message)->{
      if (!message.startsWith("HISCORES")) return;
      Platform.runLater(()-> {
        //load online high scores into list
        parseOnlineScores(message);
        scoreCheck();
      });
    });
  }

  /**
   * Add the online high scores, received from the server, to the list containing online high scores
   * as Pairs.
   * @param message the list of all current channels
   */
  private void parseOnlineScores(String message) {
      logger.debug(message);
      var scores = (message.replace("HISCORES", "")).split("\n");
      for (String score : scores) {
        logger.debug("Before split {}", score);
        var scoreContent = (score.split(":"));
        //add score
        onlineScoresList.add(new Pair<>(scoreContent[0], Integer.valueOf(scoreContent[1].trim())));
      }
  }

  /**
   * Send new online high score to the server
   * @param name the username the user entered
   */
  private void writeOnlineScore(String name){
    logger.info("Writing online score");
    var points = String.valueOf(game.getScore());
    //send to server
    communicator.send("HISCORE "+name+":"+points);
  }

  /**
   * Handle keyboard control
   * @param keyEvent the key pressed
   */
  private void handleKey(KeyEvent keyEvent) {
    logger.info(keyEvent.toString()+" key pressed");
    if (keyEvent.getCode() != KeyCode.ESCAPE) return;
    //if key was ESCAPE, return to menu
    gameWindow.startMenu();
  }



}
