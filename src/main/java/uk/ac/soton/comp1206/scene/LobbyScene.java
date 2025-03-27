package uk.ac.soton.comp1206.scene;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import javafx.application.Platform;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.soton.comp1206.component.ChannelList;
import uk.ac.soton.comp1206.component.UserList;
import uk.ac.soton.comp1206.network.Communicator;
import uk.ac.soton.comp1206.ui.GamePane;
import uk.ac.soton.comp1206.ui.GameWindow;

/**
 * The lobby scene holds the UI for the multiplayer lobby. This is where players can join/leave a
 * lobby, chat or start the game if they are the host.
 */
public class LobbyScene extends BaseScene {

  private static final Logger logger = LogManager.getLogger(LobbyScene.class);

  /**
   * Communicator used for sending and receiving messages from the server
   */
  private Communicator communicator;
  /**
   * The observable set of current channels on the server
   */
  private ObservableSet<String> channelSet = FXCollections.observableSet();
  /**
   * A wrapper for the observable set of channels. Used for binding.
   */
  private SimpleSetProperty<String> channelsWrapper = new SimpleSetProperty<>(channelSet);
  /**
   * The observable set of users in a channel
   */
  private ObservableSet<String> usersList = FXCollections.observableSet();
  /**
   * A wrapper for the observable set of users, used for binding.
   */
  private SimpleSetProperty<String> usersWrapper = new SimpleSetProperty<>(usersList);
  private BorderPane mainPane;
  private TextFlow chat;
  private VBox channelBox;
  private HBox buttons;

  private ScrollPane chatScroller;
  private TextField nameInput;

  /**
   * Timer used to request channels from the server at a regular interval.
   */
  private Timer timer = new Timer();

  /**
   * Create a new scene, passing in the GameWindow the scene will be displayed in. Uses the
   * GameWindow communicator.
   *
   * @param gameWindow the game window
   */
  public LobbyScene(GameWindow gameWindow) {
    super(gameWindow);
    logger.info("Creating lobby scene");
    communicator = gameWindow.getCommunicator();
  }

  /**
   * Initialise the scene by requesting channels from server and setting a communicator listener
   */
  @Override
  public void initialise() {
    requestChannels();
    communicator.addListener(communication -> Platform.runLater(() ->
        handleMessage(communication)
    ));
    scene.setOnKeyPressed(this::handleKey);

  }

  /**
   * Handles received messages from the server, via the communicator, using the network protocol.
   *
   * @param message the message received
   */
  private void handleMessage(String message) {
    //list of channels received
    if (message.startsWith("CHANNELS")) {
      parseChannelList(message);
    }
    //user requests to join channel
    if (message.startsWith("JOIN")) {
      createChannelBox(message);
    }
    //list of all users in a channel
    if (message.startsWith("USERS")) {
      parseUserList(message);
    }
    //chat message received from player
    if (message.startsWith("MSG")) {
      receiveChatMessage(message);
    }
    //error message recieved from server
    if (message.startsWith("ERROR")) {
      //display error message with alert
      var errorMessage = message.replace("ERROR", "");
      Alert error = new Alert(Alert.AlertType.ERROR, errorMessage);
      error.showAndWait();
    }
    //user leaves a channel
    if (message.startsWith("PARTED")) {
      mainPane.getChildren().remove(channelBox);
    }
    //user is host of the channel and can start the game
    if (message.startsWith("HOST")) {
      addHostControl();
    }
    //game is starting
    if (message.startsWith("START")) {
      gameWindow.loadScene(new MultiplayerScene(gameWindow));
      timer.cancel();
    }
    //player's name has updated
    if (message.startsWith("NICK")) {
      var name = message.split(":")[0];
      name = name.replace("NICK ", "");
      usersList.remove(name);
    }
  }

  /**
   * Add a button to start the game for the host of the channel
   */
  private void addHostControl() {
    Button start = new Button("Start Game");
    buttons.getChildren().add(start);
    start.setOnAction(event -> communicator.send("START"));
  }

  /**
   * Add users in the current channel to the set of users
   *
   * @param message the list of users in the channel
   */
  private void parseUserList(String message) {
    //reset the set
    usersList.clear();
    message = message.replace("USERS ", "");
    logger.info("List of users in the current channel: {}", message);
    var receivedUsers = message.split("\n");
    //add all the users to the set
    usersList.addAll(Arrays.asList(receivedUsers));
  }


  /**
   * Add the current channels on the server to the list of channels
   *
   * @param message the list of all current channels
   */
  private void parseChannelList(String message) {
    //reset
    channelSet.clear();
    message = message.replace("CHANNELS ", "");
    logger.info("List of all current channels {}", message);
    var receivedChannels = message.split("\n");
    channelSet.addAll(Arrays.asList(receivedChannels));
  }

  /**
   * Request the list of current channels on the server every 2 seconds by sending a message via the
   * communicator.
   */
  private void requestChannels() {
    //create a new timer
    //timer = new Timer();

    //create a new timer task
    var task = new TimerTask() {
      @Override
      public void run() {
        //request list of channels from the server
        communicator.send("LIST");
      }
    };
    //task scheduled every 2 seconds
    timer.scheduleAtFixedRate(task, 0, 2000);
  }

  /**
   * Create a new channel on the server
   *
   * @param name the name of the channel
   */
  private void startNewChannel(String name) {
    communicator.send("CREATE " + name);
  }

  /**
   * Join an existing channel on the server
   *
   * @param channel the name of the channel to join
   */
  private void joinChannel(String channel) {
    logger.info("JOIN " + channel);
    communicator.send("JOIN " + channel);
  }

  /**
   * Build the lobby window by adding all the UI components.
   */
  @Override
  public void build() {
    logger.info("Building " + this.getClass().getName());

    root = new GamePane(gameWindow.getWidth(), gameWindow.getHeight());

    //create background stack pane
    var lobbyPane = new StackPane();
    lobbyPane.setMaxWidth(gameWindow.getWidth());
    lobbyPane.setMaxHeight(gameWindow.getHeight());
    lobbyPane.getStyleClass().add("menu-background");
    root.getChildren().add(lobbyPane);

    //create the main border pane
    mainPane = new BorderPane();
    lobbyPane.getChildren().add(mainPane);

    //create the top title and position correctly
    var titleBox = new HBox();
    var title = new Text("Multiplayer");
    title.getStyleClass().add("title");
    titleBox.getChildren().add(title);
    titleBox.setAlignment(Pos.CENTER);
    mainPane.setTop(titleBox);

    //create left side VBox
    var leftVBox = new VBox(50);
    mainPane.setLeft(leftVBox);

    //add header for channels
    var channelsHeader = createChannelsHeader();
    channelsHeader.setPadding(new Insets(5, 0, 0, 10));
    leftVBox.getChildren().add(channelsHeader);

    var scroller = new ScrollPane();
    scroller.getStyleClass().add("scroller");
    scroller.setFitToWidth(true);
    scroller.setMinViewportHeight(gameWindow.getHeight() - 210);
    //create ChannelList and add to the left side Vbox
    var channels = new ChannelList();
    channels.setAlignment(Pos.CENTER);
    //bind channel set to the channel list
    channels.channelListProperty().bind(channelsWrapperProperty());
    scroller.setContent(channels);
    leftVBox.getChildren().add(scroller);
    //add listener to listen for clicked channels
    channels.setChannelClickedListener(this::joinChannel);

  }


  /**
   * Create the header for the channels list with desired styling and positioning. Includes the text
   * heading and button for creating a new game
   *
   * @return the Vbox containing the heading and button
   */
  private VBox createChannelsHeader() {
    var channelListHeader = new VBox(5);
    var heading = new Text("Current Games");
    heading.getStyleClass().add("heading");
    Button newGame = createNewGameButton(channelListHeader);
    channelListHeader.getChildren().addAll(heading, newGame);
    channelListHeader.setAlignment(Pos.CENTER);
    return channelListHeader;
  }

  /**
   * Create a button that allows the user to create a new channel as well as a text field for the
   * user to enter the new channel name. Sends a new channel creation to the server when pressed.
   *
   * @param channelListHeader the VBox to add and remove the text field from
   * @return the button to create a new game
   */
  private Button createNewGameButton(VBox channelListHeader) {
    var button = new Button("Host New Game");
    button.getStyleClass().add("channelItem");
    //when button is pressed, text field is created for user to enter the channel name into
    button.setOnAction(event -> {
      //if text field already open, remove text field
      if (nameInput != null) {
        channelListHeader.getChildren().remove(nameInput);
      }
      nameInput = new TextField();
      nameInput.setPromptText("Enter Game Name");
      nameInput.getStyleClass().add("Textfield");
      //add the text field
      channelListHeader.getChildren().add(nameInput);
      //when keyboard Enter is pressed, start a new channel
      nameInput.setOnKeyPressed(event1 -> {
        //ignore non Enter keys
        if (event1.getCode() != KeyCode.ENTER) {
          return;
        }
        //ignore empty text field
        if (nameInput.getText().isBlank()) {
          return;
        }
        //start a new channel
        startNewChannel(nameInput.getText());
        //clear text from text field and remove from Vbox
        nameInput.clear();
        channelListHeader.getChildren().remove(nameInput);
      });
    });
    return button;
  }

  /**
   * Create the channel box for when user is in a channel. Includes the users in the channel, a chat
   * box and the button to leave the channel.
   *
   * @param message the channel name
   */
  private void createChannelBox(String message) {
    var channel = message.replace("JOIN", "");
    channelBox = new VBox(5);
    channelBox.getStyleClass().add("gameBox");
    //display channel name
    var header = new Text("Game: " + channel);
    header.getStyleClass().add("heading");
    //get users in channel
    var userList = createUserList();
    userList.setPadding(new Insets(10, 10, 10, 10));
    //create chat box
    var scroller = createChatBox();
    var chatInput = new TextField();
    chatInput.setPromptText("Send a message");
    buttons = new HBox(250);
    buttons.setPadding(new Insets(10, 10, 10, 10));
    var leave = new Button("Leave Game");
    //add functionality for sending a chat message
    buttons.getChildren().add(leave);
    chatInput.setOnKeyPressed(event -> {
      if (event.getCode() != KeyCode.ENTER) {
        return;
      }
      sendChatMessage(chatInput.getText());
      chatInput.clear();
    });
    //add functionality for leaving the lobby
    leave.setOnAction(event -> leaveGame());
    channelBox.getChildren().addAll(header, userList, scroller, chatInput, buttons);
    mainPane.setRight(channelBox);
  }

  /**
   * Leave the current channel
   */
  private void leaveGame() {
    communicator.send("PART");
  }

  /**
   * Send a chat message or change nickname if the message starts with '/nick'
   *
   * @param message the message to send
   */
  private void sendChatMessage(String message) {
    if (message.startsWith("/nick")) {
      message = message.replace("/nick ", "");
      communicator.send("NICK " + message);
    } else {
      communicator.send("MSG " + message);
    }
  }

  /**
   * Receive a chat message from the server. Display in chat box text flow.
   *
   * @param receivedMessage the received message
   */
  private void receiveChatMessage(String receivedMessage) {
    receivedMessage = receivedMessage.replace("MSG ", "");
    Text message = new Text(receivedMessage + "\n");
    chat.getChildren().add(message);
    chatScroller.setVvalue(1.0);
  }

  /**
   * Create a UserList UI component to display the users in the channel. Binds to the list of users
   * received from the server
   *
   * @return returns the display of users in the channel
   */
  private UserList createUserList() {
    var userList = new UserList();
    //bind set of users
    userList.userListProperty().bind(usersWrapperProperty());
    return userList;
  }

  /**
   * Creates a chat box to display chat messages using a text flow and scroll pane.
   *
   * @return the scroll pane for holding the chat
   */
  private ScrollPane createChatBox() {
    //hold text in text flow
    chat = new TextFlow();
    chat.setLineSpacing(10);
    chat.getStyleClass().add("messages");
    var introMessage = new Text("Welcome to the lobby \nType /nick NewName to change your name \n");
    introMessage.getStyleClass().add("introMessage");
    chat.getChildren().add(introMessage);
    chatScroller = new ScrollPane();
    chatScroller.getStyleClass().add("scroller");
    //vertical scroll
    chatScroller.setFitToWidth(true);
    //add text to the scroll pane
    chatScroller.setContent(chat);
    chatScroller.setPadding(new Insets(10, 10, 10, 10));
    chatScroller.setMinViewportHeight(350);
    return chatScroller;
  }

  /**
   * Getter for the channelWrapper set property which holds the set of current channels on the
   * server
   *
   * @return returns the simple set property
   */
  public SimpleSetProperty<String> channelsWrapperProperty() {
    return channelsWrapper;
  }

  /**
   * Getter for the usersWrapper set property which holds the set of current users in the channel
   *
   * @return returns the simple set property
   */
  public SimpleSetProperty<String> usersWrapperProperty() {
    return usersWrapper;
  }

  /**
   * Handle keyboard control
   *
   * @param keyEvent the key pressed
   */
  private void handleKey(KeyEvent keyEvent) {
    logger.info(keyEvent.toString() + " key pressed");
    if (keyEvent.getCode() != KeyCode.ESCAPE) {
      return;
    }
    //if key was ESCAPE, return to menu
    communicator.send("PART");
    gameWindow.startMenu();
    //leave channel
  }

}