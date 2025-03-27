package uk.ac.soton.comp1206.component;

import java.util.ArrayList;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javatuples.Triplet;

/**
 * The Leaderboard is a UI component that displays and holds the scores for the multiplayer game
 * players.
 */
public class Leaderboard extends ScoresList {

  private static final Logger logger = LogManager.getLogger(Leaderboard.class);

  /**
   * The list of the player scores, represented using a Triple holding name,score and lives
   */
  private SimpleListProperty<Triplet<String, Integer, String>> multiplayerScores = new SimpleListProperty<>();

  /**
   * Whether this leaderboard is being used for the final scores display
   */
  private boolean finalScores = false;

  /**
   * Create the Leaderboard with a listener to listen to when the player scores have changed, call
   * the relevant method to update the display.
   */
  public Leaderboard() {
    //Update score list when score array list is updated
    multiplayerScores.addListener(
        (ListChangeListener<? super Triplet<String, Integer, String>>) (e) -> {
          //check if leaderboard is being used for final scores display or in game display
          if (!finalScores) {
            updateScoreList(multiplayerScores);
          } else {
            showFinalScores(multiplayerScores);
          }
        }

    );
  }

  /**
   * Update the display of scores received from the server with correct formatting and styling
   *
   * @param list the player scores received from the server
   */
  private void updateScoreList(SimpleListProperty<Triplet<String, Integer, String>> list) {
    //Remove previous children
    getChildren().clear();
    //loop through each score
    for (Triplet<String, Integer, String> score : list) {
      logger.info("Showing {} {} {}", score.getValue0(), score.getValue1(), score.getValue2());
      var name = score.getValue0();
      var points = String.valueOf(score.getValue1());
      String lives;
      if (!score.getValue2().equals("DEAD")) {
        lives = " (".concat(score.getValue2().concat(" lives)"));
      } else {
        lives = " ".concat(score.getValue2());
      }
      //create text with name, score and lives
      var text = new Text(name.concat(": ").concat(points).concat(lives));
      text.setTextAlignment(TextAlignment.CENTER);
      text.getStyleClass().add("smallHeading");
      HBox.setHgrow(text, Priority.ALWAYS);
      //if player is dead, strikethrough
      if (score.getValue2().equals("DEAD")) {
        text.setStrikethrough(true);
      }
      //add all scores to display
      getChildren().add(text);
    }
  }

  /**
   * Display the final multiplayer scores for the score scene with the correct formatting and
   * styling
   *
   * @param list the list of player scores recieved from the server
   */
  private void showFinalScores(
      SimpleListProperty<Triplet<String, Integer, String>> list) {
    getChildren().clear();

    //create an array list of the individual scores to be displayed
    ArrayList<Text> scoreTexts = new ArrayList<>();
    var count = 0;
    //loop through each score
    for (Triplet<String, Integer, String> score : list) {
      logger.info("Showing {} {}", score.getValue0(), score.getValue1());
      count++;
      //display top 10 scores
      if (count > 10) {
        break;
      }
      var scoreText = new Text(score.getValue0().concat(": ".concat(score.getValue1().toString())));
      scoreText.setTextAlignment(TextAlignment.CENTER);
      HBox.setHgrow(scoreText, Priority.ALWAYS);
      scoreTexts.add(scoreText);
    }
    //use reveal method to show scores one by one
    reveal(scoreTexts);
  }

  /**
   * Set final scores attribute as true - used for scores scene display
   */
  public void setFinalScores() {
    this.finalScores = true;
  }

  /**
   * Get the multiplayer scores property
   *
   * @return the ListProperty holding the multiplayer scores
   */
  public SimpleListProperty<Triplet<String, Integer, String>> multiplayerScoresProperty() {
    return multiplayerScores;
  }

}
