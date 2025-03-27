package uk.ac.soton.comp1206.component;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.soton.comp1206.game.GamePiece;
import uk.ac.soton.comp1206.game.Grid;

/**
 * The PieceBoard is a UI component used for showing a singular game piece in the center of the
 * board
 */
public class PieceBoard extends GameBoard {

  private static final Logger logger = LogManager.getLogger(PieceBoard.class);


  /**
   * Create a new piece board
   * @param grid the linked grid
   * @param width the visual width of the board
   * @param height the visual height of the board
   */
  public PieceBoard(Grid grid, double width, double height) {
    super(grid, width, height);
  }


  /**
   * Display a given game piece on the board
   * @param piece the game piece to display
   */
  public void setPiece(GamePiece piece){
    logger.info("Next piece is "+ piece);
    //set piece onto board
    int[][] blocks = piece.getBlocks();
    for(int i=0;i<3;i++){
      for(int j =0;j<3;j++){
        this.grid.set(i,j,blocks[i][j]);
      }
    }

  }



}
