package uk.ac.soton.comp1206.event;

import uk.ac.soton.comp1206.component.GameBlock;

/**
 * The Block Hover listener is used to handle the event when a block in a GameBoard is hovered over
 * using the mouse. It passes the GameBlock that was hovered over.
 */
public interface BlockHoverListener {

  /**
   * Handle a block hover event
   * @param block the block that was hovered over
   */
  void blockHover(GameBlock block);
}
