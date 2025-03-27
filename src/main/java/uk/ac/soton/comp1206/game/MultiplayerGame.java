package uk.ac.soton.comp1206.game;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import javafx.application.Platform;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.soton.comp1206.event.ChatReceivedListener;
import uk.ac.soton.comp1206.network.Communicator;
import org.javatuples.Triplet;

/**
 * The Multiplayer Game class extends the Game class and adds any additional game logic needed for
 * multiplayer games.
 * <br>
 * This includes communicating with the server to retrieve game pieces as well as
 * send and retrieve game updates for the users playing the game in order to keep track of the leaderboard.
 *
 */
public class MultiplayerGame extends Game{

  private static final Logger logger = LogManager.getLogger(MultiplayerGame.class);
  private Communicator communicator;

  /**
   * A linked list used to represent the queue of pieces retrieved from the server
   */
  private LinkedList<GamePiece> queue = new LinkedList<>();

  /**
   * An observable array list of the scores of the players in the game - represented using a Triplet
   * in the form Name,Score,Lives
   */

  private ObservableList<Triplet<String,Integer,String>> scoresList = FXCollections.observableArrayList();

  /**
   * The ListProperty used as a wrapper for the observable list of scores
   */
  private SimpleListProperty<Triplet<String,Integer,String>> multiplayerScores = new SimpleListProperty<>(scoresList);

  private ChatReceivedListener chatReceivedListener;


  /**
   * Create a new game with the specified rows and columns. Creates a corresponding grid model.
   * <br>
   * Handle messages from the communicator and requests the initial queue of pieces and scores for
   * all players.
   *
   * @param cols number of columns
   * @param rows number of rows
   * @param communicator used to communicate with the server by sending and receiving messages
   */
  public MultiplayerGame(int cols, int rows, Communicator communicator) {
    super(cols, rows);
    this.communicator = communicator;
    //handle messages from the server
    communicator.addListener(communication -> Platform.runLater(() ->
        handleMessage(communication)
    ));
    //queue the initial pieces
    queuePieces();
    //request the initial scores for all players
    communicator.send("SCORES");

  }

  /**
   * Handles received messages from the communicator using the server protocol
   * @param communication the message received from the server
   */
  private void handleMessage(String communication) {
    //enqueue received pieces from the server
      if (communication.startsWith("PIECE")) {
        enqueuePiece(communication);
      }
      //update leaderboard with player scores received from the server
      else if(communication.startsWith("SCORES")){
        updateLeaderboardScore(communication);
      }
      //listen for chat messages
      else if(communication.startsWith("MSG")){
        chatReceivedListener.chatReceived(communication);
      }
  }

  /**
   * Send message to the server to leave the game (and channel).
   */
  public void endGame(){
    communicator.send("DIE");
  }

  /**
   * Send chat message to server
   * @param message the chat message to send
   */
  public void sendChatMessage(String message){
    communicator.send("MSG "+message);
  }

  /**
   * Request the initial 4 pieces from the server to be added to the queue
   */
  private void queuePieces() {
    logger.info("Requesting initial game pieces");
    for (int i=0;i<4;i++) {
      communicator.send("PIECE");
    }
  }

  /**
   * Adds received piece from server to the local queue of pieces. Adds to the end of the queue.
   * @param message the message received from the server
   */
  private void enqueuePiece(String message) {
    //remove start of message
    message = message.replace("PIECE ","");
    //create a new game piece with value provided
    var piece = GamePiece.createPiece(Integer.parseInt(message));
    logger.info("Enqueueing {}",piece);
    //add game piece to the end of the queue
    queue.addLast(piece);
  }

  /**
   * Removes the first piece from the queue
   * @return the piece that has been removed
   */
  private GamePiece dequeuePiece(){
    //remove from front of queue
    var piece = queue.removeFirst();
    logger.info("Dequeue-ing {}",piece);
    return piece;
  }

  /**
   * Requests a piece from the server by following the server protocol
   */
  private void requestQueuePiece(){
    logger.info("Requesting piece from the server");
    communicator.send("PIECE");
  }

  /**
   * Overrides Game's method of spawning a random piece to instead dequeue a piece from the queue and
   * request a replacement piece so queue size remains the same.
   * <br>
   * Ensures all players get the same pieces.
   *
   * @return the dequeued piece
   */
  @Override
  public GamePiece spawnPiece(){
    logger.info("Spawning piece");
    sendBoardStatus();
    requestQueuePiece();
    return dequeuePiece();
  }

  /**
   * After score has been updated, updated score is sent to the server.
   * <br>
   * Uses Game's method to update the score
   * @param lines the number of lines cleared
   * @param blocks the number of blocks cleared
   */
  @Override
  public void changeScore(int lines, int blocks) {
    super.changeScore(lines, blocks);
    communicator.send("SCORE " + this.getScore());
  }

  /**
   * Sends the updated number of lives to the server after the game has looped
   */
  @Override
  public void gameLoop() {
    super.gameLoop();
    communicator.send("LIVES "+this.getLives());
  }

  /**
   * Sends the current board values to the server to protect against cheating
   */
  private void sendBoardStatus() {
    String boardValues = "BOARD ";
    var grid = this.getGrid();
    for (int x =0; x<grid.getCols();x++){
      for (int y=0;y<grid.getRows();y++){
        String blockVal = String.valueOf(grid.get(x,y));
        boardValues = boardValues.concat(blockVal+" ");
      }
    }
    communicator.send(boardValues);
  }

  /**
   * Updates the leaderboard when player scores are received from the server.
   * <br>
   * Adds name, score and lives to the observable list of scores.
   * @param message the scores received from the server
   */
  private void updateLeaderboardScore(String message) {
   scoresList.clear();
   var allScores = message.replace("SCORES ","").split("\n");
   for (String score :allScores){
     var scoreContent = score.split(":");
     logger.debug("Adding {},{},{} to scores",scoreContent[0],scoreContent[1],scoreContent[2]);
     //add score
     scoresList.add(new Triplet<>(scoreContent[0],Integer.valueOf(scoreContent[1]),scoreContent[2]));
     //sort list
     scoresList.sort(Collections.reverseOrder(Comparator.comparing(Triplet<String,Integer,String>::getValue1)));
   }
  }

  /**
   * Gets the multiplayerScores property. Used for binding.
   * @return the SimpleListProperty representing the multiplayer scores
   */
  public SimpleListProperty<Triplet<String,Integer,String>> multiplayerScoresProperty(){
    return multiplayerScores;
  }

  /**
   * Set the listener to handle an event when a chat message is received from the server
   * @param listener the listener to listen for a chat received event
   */
  public void setChatReceivedListener(ChatReceivedListener listener){
    chatReceivedListener = listener;
  }
}
