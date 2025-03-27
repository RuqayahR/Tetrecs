package uk.ac.soton.comp1206.event;

import uk.ac.soton.comp1206.game.GamePiece;

/**
 * The Next Piece listener is used to handle the event when the Game generates the next piece after
 * a piece has been played. It passes the new current and following Game Pieces.
 *
 */
public interface NextPieceListener {

  /**
   * Handles a generated next piece event
   * @param currentGamePiece the updated current piece to play
   * @param followingGamePiece the new following piece
   */
  void nextPiece(GamePiece currentGamePiece,GamePiece followingGamePiece);

}
