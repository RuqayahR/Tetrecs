package uk.ac.soton.comp1206.event;

import java.util.HashSet;
import uk.ac.soton.comp1206.component.GameBlockCoordinate;

/**
 * The Line Cleared listener is used to handle the event when a line in the Game has been cleared.
 * It passes in a set of the GameBlockCoordinates of the GameBlocks that make up that line.
 *
 */
public interface LineClearedListener {

  /**
   * Handle a line cleared event
   * @param coordinates the co-ordinates of the game blocks that have been cleared
   */
  void lineCleared(HashSet<GameBlockCoordinate> coordinates);

}
