package uk.ac.soton.comp1206.event;

/**
 * The Channel Clicked listener is used to handle the event when a channel in the ChannelList
 * is clicked. It passes the name of the Channel that was clicked in the message.
 */
public interface ChannelClickedListener {

  /**
   * Handle a channel clicked event
   * @param channel the channel that has been clicked
   */
  void channelClicked(String channel);

}
