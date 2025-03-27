package uk.ac.soton.comp1206.event;

import uk.ac.soton.comp1206.component.GameBlock;

/**
 * The Right-Clicked listener is used to handle the event when a block in a GameBoard is right-clicked.
 * It passes the GameBlock that was clicked in the message
 */
public interface RightClickedListener {

  /**
   * Handle a right click event
   * @param block the block that was clicked
   */
 void rightClicked(GameBlock block);

}
