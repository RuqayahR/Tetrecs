package uk.ac.soton.comp1206.scene;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.soton.comp1206.component.PieceBoard;
import uk.ac.soton.comp1206.game.GamePiece;
import uk.ac.soton.comp1206.game.Grid;
import uk.ac.soton.comp1206.ui.GamePane;
import uk.ac.soton.comp1206.ui.GameWindow;

/**
 * The instruction scene shows the instructions on how to play the game as well as the possible
 * game pieces that can be given in a game.
 */
public class InstructionsScene extends BaseScene{

  private static final Logger logger = LogManager.getLogger(InstructionsScene.class);

  /**
   * Create a new instructions scene
   * @param gameWindow the Game Window
   */
  public InstructionsScene(GameWindow gameWindow) {
    super(gameWindow);
    logger.info("Creating Instructions Scene");
  }

  /**
   * Initialise the scene
   */
  @Override
  public void initialise() {
    scene.setOnKeyPressed(this::handleKey);
  }

  /**
   * Build the instructions screen by adding all UI components. Includes a title, description
   * and instructions image as well as all the game pieces shown on piece boards.
   */
  @Override
  public void build() {
    logger.info("Building " + this.getClass().getName());

    root = new GamePane(gameWindow.getWidth(),gameWindow.getHeight());

    //create background
    var instructionPane = new StackPane();
    instructionPane.setMaxWidth(gameWindow.getWidth());
    instructionPane.setMaxHeight(gameWindow.getHeight());
    instructionPane.getStyleClass().add("menu-background");
    root.getChildren().add(instructionPane);

    var mainPane = new BorderPane();
    instructionPane.getChildren().add(mainPane);

    //add instructions image
    var top = new VBox();
    var instructionsImage = new ImageView(new Image(this.getClass().getResource("/images/Instructions.png").toExternalForm()));
    instructionsImage.setPreserveRatio(true);
    instructionsImage.setFitWidth(gameWindow.getWidth()/1.5);

    //add title and description
    var title = new Text("Instructions");
    title.getStyleClass().add("heading");
    var text = new Text("TetrECS is a fast-paced gravity-free block placement game, where you "
        + "must survive by clearing rows through careful placement of the upcoming blocks before the"
        + " time runs out. Lose all 3 lives and you're out!");
    var description = new TextFlow(text);
    description.setTextAlignment(TextAlignment.CENTER);
    text.getStyleClass().add("instructions");


    top.getChildren().addAll(title,description,instructionsImage);
    mainPane.setTop(top);
    top.setAlignment(Pos.TOP_CENTER);
    top.setPadding(new Insets(5,0,0,0));

    var bottom = new VBox();
    var caption = new Text("Game Pieces");
    caption.getStyleClass().add("heading");
    var gridPane = new GridPane();
    gridPane.setHgap(10);
    gridPane.setVgap(10);

    //create 15 piece boards with corresponding game pieces shown
    int pieceCount= 0;
    for (int i =0;i<3;i++){
      for(int j=0;j<5;j++){
        var pieceGrid = new PieceBoard(new Grid(3,3),50,50);
        var gamePiece = GamePiece.createPiece(pieceCount);
        logger.info("Displaying {} in pieceBoard",gamePiece);
        pieceGrid.setPiece(gamePiece);
        gridPane.add(pieceGrid,j,i);
        pieceCount++;
      }
    }

    bottom.setSpacing(5);
    bottom.getChildren().addAll(caption,gridPane);
    mainPane.setBottom(bottom);
    bottom.setAlignment(Pos.BOTTOM_CENTER);
    gridPane.setAlignment(Pos.CENTER);
    bottom.setPadding(new Insets(3,0,15,0));

  }

  /**
   * Handle keyboard input
   * @param keyEvent the key pressed
   */
  private void handleKey(KeyEvent keyEvent) {
    logger.info(keyEvent.toString()+" key pressed");
    if (keyEvent.getCode() != KeyCode.ESCAPE) return;
    //if ESCAPE was pressed, load menu screen
    gameWindow.startMenu();

  }
}
