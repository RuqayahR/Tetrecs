package uk.ac.soton.comp1206.event;

import uk.ac.soton.comp1206.game.Game;

/**
 * The Game Over listener is used to handle the event when the game is ended after the player
 * runs out of lives. It passes the Game object that has been played.
 */

public interface GameOverListener {

  /**
   * Handle a game over event
   * @param game the game that has been played
   */
  void gameOver(Game game);

}
