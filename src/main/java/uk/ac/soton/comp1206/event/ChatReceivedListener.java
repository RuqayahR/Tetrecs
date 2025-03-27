package uk.ac.soton.comp1206.event;

/**
 * The Block Clicked listener is used to handle the event when a chat message is received in the
 * multiplayer game.It passes the message that was received from the server.
 */
public interface ChatReceivedListener {

  /**
   * Handle a chat received event
   * @param message the message that was received
   */
  void chatReceived(String message);
}
