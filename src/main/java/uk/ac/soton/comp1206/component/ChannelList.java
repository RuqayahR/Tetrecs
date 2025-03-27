package uk.ac.soton.comp1206.component;

import javafx.beans.property.SimpleSetProperty;
import javafx.collections.SetChangeListener;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.soton.comp1206.event.ChannelClickedListener;

/**
 * The ChannelList provides UI for holding and displaying the list of current channels available on
 * the server.
 */
public class ChannelList extends VBox {

  private static final Logger logger = LogManager.getLogger(ChannelList.class);

  /**
   * The set property which contains the set of current channels
   */
  public SimpleSetProperty<String> channelList = new SimpleSetProperty<>();

  /**
   * Listens to when a channel is clicked
   */
  private ChannelClickedListener channelClickedListener;

  /**
   * Create a new ChannelList which listens to when the set of channels changes amd calls the method
   * to update the UI display
   */
  public ChannelList() {
    //Update score list when score array list is updated
    channelList.addListener(
        (SetChangeListener<? super String>) (e) -> updateChannelList((channelList))
    );
  }

  /**
   * Updates the UI display with the current channel list, channels are shown as buttons which when
   * pressed trigger the ChannelClickedListener
   *
   * @param channelList the set of all available channels
   */
  private void updateChannelList(SimpleSetProperty<String> channelList) {

    logger.info("Updating channel list");
    //Remove previous children
    getChildren().clear();

    //create a button for each channel
    for (String channel : channelList) {
      var channelButton = new Button(channel);
      channelButton.getStyleClass().add("channelItem");
      getChildren().add(channelButton);
      //when button is pressed, call listener
      channelButton.setOnAction((e) -> {
        channelClickedListener.channelClicked(channelButton.getText());
      });
    }

  }

  /**
   * Get the channel list property
   *
   * @return the SetProperty representing the set of available channels
   */
  public SimpleSetProperty<String> channelListProperty() {
    return channelList;
  }

  /**
   * Set the channelClearedListener
   *
   * @param listener the listener to listen to any clicked channel buttons
   */
  public void setChannelClickedListener(ChannelClickedListener listener) {
    this.channelClickedListener = listener;
  }

}
