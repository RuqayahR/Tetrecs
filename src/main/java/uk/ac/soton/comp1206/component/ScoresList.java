package uk.ac.soton.comp1206.component;

import java.util.ArrayList;
import javafx.animation.FadeTransition;
import javafx.animation.SequentialTransition;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The ScoresList is a UI component that holds and displays a list of names and associated scores
 */
public class ScoresList extends VBox {

  private static final Logger logger = LogManager.getLogger(ScoresList.class);

  /**
   * The list of local scores
   */
  private SimpleListProperty<Pair<String,Integer>> scoresList = new SimpleListProperty<>();

  /**
   * The list of online scores
   */
  private SimpleListProperty<Pair<String,Integer>> onlineScoresList = new SimpleListProperty<>();

  /**
   * The set of colours for different scores
   */
  public static final Color[] COLOURS = {
      Color.DEEPPINK,
      Color.RED,
      Color.ORANGE,
      Color.YELLOW,
      Color.GREEN,
      Color.AQUA,
      Color.BLUE,
      Color.DARKBLUE,
      Color.PURPLE,
      Color.MEDIUMPURPLE,
      Color.DEEPPINK,
  };

  /**
   * Create a new ScoresList. Listen to when the local or online scores are changed and update the
   * display of scores.
   */
  public ScoresList(){

    //set styling
    getStyleClass().add("scorelist");
    setAlignment(Pos.CENTER);
    setSpacing(2);

    //Update score list when score array list is updated
    scoresList.addListener(
        (ListChangeListener<? super Pair<String, Integer>>) (e) -> updateScoreList(scoresList)
    );

    //Update online score list when array list is updated
    onlineScoresList.addListener(
        (ListChangeListener<? super Pair<String, Integer>>) (e) -> {
          logger.info("Adding online high scores");
          updateScoreList(onlineScoresList);
        }
    );
  }

  /**
   * Update the display of scores with correct formatting and styling.
   * @param list the list of scores
   */
  private void updateScoreList(SimpleListProperty<Pair<String,Integer>> list) {
    //Remove previous children
    getChildren().clear();

    //create an array list of the individual scores to be displayed
    ArrayList<Text> scoreTexts = new ArrayList<>();
    var count = 0;
    for (Pair<String,Integer> score : list){
      logger.info("Showing {} {}", score.getKey(),score.getValue());
      count++;
      //only add top 10 scores
      if (count>10)break;
      //create new text with name and score
      var scoreText = new Text(score.getKey().concat(": ".concat(score.getValue().toString())));
      scoreText.setTextAlignment(TextAlignment.CENTER);
      HBox.setHgrow(scoreText, Priority.ALWAYS);

      scoreTexts.add(scoreText);
    }

    //reveal each score text one by one
    reveal(scoreTexts);

  }

  /**
   * Reveals each text one by one with rainbow colours
   * @param scores the scores to display
   */
  public void reveal(ArrayList<Text> scores){
    //create new sequential transition to show scores one by one
    SequentialTransition seq = new SequentialTransition();
    var colorNum = 0;
    //loop through each score
    for (Text score : scores ){
      //assign the next colour
      score.setFill(COLOURS[colorNum]);
      score.setOpacity(0);
      getChildren().add(score);
      //fade in score
      FadeTransition ft = new FadeTransition(Duration.millis(1000),score);
      ft.setToValue(1);
      seq.getChildren().add(ft);
      colorNum++;
    }
    //play sequence
    seq.play();
  }

  /**
   * Get the scores list property
   * @return the ListProperty representing the local scores
   */
  public SimpleListProperty<Pair<String, Integer>> scoresListProperty() {
    return scoresList;
  }

  /**
   * Get the online scores list property
   * @return the ListProperty representing the online scores
   */
  public SimpleListProperty<Pair<String, Integer>> onlineScoresListProperty() {
    return onlineScoresList;
  }

}
