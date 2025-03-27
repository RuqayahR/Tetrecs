package uk.ac.soton.comp1206.event;

/**
 * The Game Loop listener is used to handle the event when the Game loops after reaching the end of
 * the countdown. It passes in the current timer delay for the next countdown.
 */
public interface GameLoopListener {

  /**
   * Handle a game loop event
   * @param timeDelay the current timer delay
   */
  void gameLooped(int timeDelay);

}
