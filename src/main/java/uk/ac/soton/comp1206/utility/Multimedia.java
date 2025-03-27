package uk.ac.soton.comp1206.utility;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Multimedia class handles playing any music or audio sounds by using MediaPlayers.
 */
public class Multimedia {

  private static final Logger logger = LogManager.getLogger(Multimedia.class);
  private static boolean musicEnabled = true;
  private static boolean audioEnabled = true;

  /**
   * Handles playing background music
   */
  private static MediaPlayer musicPlayer;
  /**
   * Handles playing any sound effects
   */
  private static MediaPlayer audioPlayer;

  /**
   * Plays a given audio file if audio is enabled by using a MediaPlayer.
   * @param file the audio file to play
   */

  public static void playAudio(String file){
    //check if audio is disabled
    if (!audioEnabled) return;

    //retrieve audio file
    String toPlay = Multimedia.class.getResource("/sounds/"+file).toExternalForm();
    logger.info("Playing audio "+ toPlay);

    try{
      Media play = new Media(toPlay);
      audioPlayer = new MediaPlayer(play);
      //play audio
      audioPlayer.play();
    }catch (Exception e){
      //disable audio
      audioEnabled = false;
      logger.error(e);
      logger.error("Unable to play audio, disabling audio");
    }
  }

  /**
   * Plays a given audio file as background music if music is enabled, using a MediaPlayer.
   * <br>
   * Background music loops.
   * <br>
   * Any previous music that is still playing is stopped.
   * @param file the music file to play
   */
  public static void playMusic(String file){
    //check if music is disabled
    if (!musicEnabled) return;

    //retrieve music file
    String toPlay = Multimedia.class.getResource("/music/" + file).toExternalForm();
    logger.info("Playing background music: "+ toPlay);

    try{
      //stops any previous music playing
      if(musicPlayer!=null){
        musicPlayer.stop();
      }
      //play music in a loop
      Media play = new Media(toPlay);
      musicPlayer = new MediaPlayer(play);
      musicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
      musicPlayer.play();

    }catch (Exception e){
      //disable music if not able to play
      musicEnabled = false;
      e.printStackTrace();
      logger.error("Unable to play background music, disabling music");
    }
  }

}
