package uk.ac.soton.comp1206.component;

import javafx.beans.property.SimpleSetProperty;
import javafx.collections.SetChangeListener;
import javafx.geometry.Orientation;
import javafx.scene.layout.FlowPane;
import javafx.scene.text.Text;

/**
 * The UserList is a custom UI component that holds and displays the names of the users in a
 * channel.
 */
public class UserList extends FlowPane {

  /**
   * A set of all the users in a channel
   */
  public SimpleSetProperty<String> userList = new SimpleSetProperty<>();

  /**
   * Create a new UserList. Add a listener to update the list display when the set changes.
   */
  public UserList(){
    setOrientation(Orientation.HORIZONTAL);
    setHgap(3);
    setVgap(2);
    userList.addListener(
        (SetChangeListener<? super String>) (e) -> updateUserList((userList))
    );

  }

  /**
   * Update the display of users in the channel
   * @param userList the set of users in the channel
   */
  private void updateUserList(SimpleSetProperty<String> userList) {
    //remove old names before set was updated
    getChildren().clear();

    //add heading
    var title = new Text("Users: ");
    title.getStyleClass().add("smallHeading");
    getChildren().add(title);
    //loop through each user in the channel and display
    for (String user:userList){
      var username = new Text(user);
      username.getStyleClass().add("smallHeading");
      getChildren().add(username);
    }
  }

  /**
   * Get the user list property
   * @return the SetProperty holding the current users in the channel
   */
  public SimpleSetProperty<String> userListProperty() {
    return userList;
  }

}
