package uk.ac.soton.comp1206.game;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Grid is a model which holds the state of a game board. It is made up of a set of Integer
 * values arranged in a 2D arrow, with rows and columns.
 * <p>
 * Each value inside the Grid is an IntegerProperty can be bound to enable modification and display
 * of the contents of the grid.
 * <p>
 * The Grid contains functions related to modifying the model, for example, placing a piece inside
 * the grid.
 * <p>
 * The Grid should be linked to a GameBoard for its display.
 */
public class Grid {

  private static final Logger logger = LogManager.getLogger(Grid.class);

  /**
   * The number of columns in this grid
   */
  private final int cols;

  /**
   * The number of rows in this grid
   */
  private final int rows;

  /**
   * The grid is a 2D arrow with rows and columns of SimpleIntegerProperties.
   */
  private final SimpleIntegerProperty[][] grid;

  /**
   * Create a new Grid with the specified number of columns and rows and initialise them
   *
   * @param cols number of columns
   * @param rows number of rows
   */
  public Grid(int cols, int rows) {
    this.cols = cols;
    this.rows = rows;

    //Create the grid itself
    grid = new SimpleIntegerProperty[cols][rows];

    //Add a SimpleIntegerProperty to every block in the grid
    for (var y = 0; y < rows; y++) {
      for (var x = 0; x < cols; x++) {
        grid[x][y] = new SimpleIntegerProperty(0);
      }
    }
  }

  /**
   * Get the Integer property contained inside the grid at a given row and column index. Can be used
   * for binding.
   *
   * @param x column
   * @param y row
   * @return the IntegerProperty at the given x and y in this grid
   */
  public IntegerProperty getGridProperty(int x, int y) {
    return grid[x][y];
  }

  /**
   * Update the value at the given x and y index within the grid
   *
   * @param x     column
   * @param y     row
   * @param value the new value
   */
  public void set(int x, int y, int value) {
    grid[x][y].set(value);
  }

  /**
   * Get the value represented at the given x and y index within the grid
   *
   * @param x column
   * @param y row
   * @return the value
   */
  public int get(int x, int y) {
    try {
      //Get the value held in the property at the x and y index provided
      return grid[x][y].get();
    } catch (ArrayIndexOutOfBoundsException e) {
      //No such index
      return -1;
    }
  }

  /**
   * Get the number of columns in this game
   *
   * @return number of columns
   */
  public int getCols() {
    return cols;
  }

  /**
   * Get the number of rows in this game
   *
   * @return number of rows
   */
  public int getRows() {
    return rows;
  }

  //check cells around piece we are trying to play - is there space for it?
  //if yes, we can 'play the piece' by changing the numbers of the values for those grid positions
    /**
     * Check if a piece can be played at given grid co-ordinates
     *
     * @param piece the game piece to be played
     * @param x the x co-ordinate of the grid
     * @param y the y co-ordinate of the grid
     * @return whether the piece can be played or not
     */
  public boolean canPlayPiece(GamePiece piece, int x, int y) {
    logger.info("Checking if {} piece can be played at ({},{})",piece,x,y);
    //offset co-ordinates so piece is played by centre
    x = x - 1;
    y = y - 1;
    int[][] blocks = piece.getBlocks();

    //loop through rows and columns of the piece's blocks
    for (int i = 0; i < blocks.length; i++) {
      for (int j = 0; j < blocks[i].length; j++) {
        int pieceValue = blocks[i][j];
        //ignore empty blocks with value of 0
          if (pieceValue == 0) {
              continue;
          }
         //if value of the grid at co-ordinates of that game block is not 0, piece cannot be played
        int gridValue = get(i + x, j + y);
          if (gridValue != 0) {
            logger.info("{} piece unable to be played due to conflict at {},{}",piece,i+x,j+y);
            //return false as piece cannot be played
              return false;
          }
      }
    }
    //return true as no existing blocks on the grid clashing with the piece
    return true;
  }

  /**
   * Places a piece on the grid at the given x and y co-ordinates, by
   * updating the grid blocks if the piece can be played
   * @param piece the piece to play
   * @param x the x co-ordinate
   * @param y the y co-ordinate
   * @return whether the piece has been played
   */
  public boolean playPiece(GamePiece piece, int x, int y) {
    logger.info("Playing {} piece at {},{}",piece,x,y);
      //check if the piece can't be played
      if (!canPlayPiece(piece, x, y)) {
        //return false as the piece was not played
          return false;
      }
    //offset piece to place center rather than top left
    x = x - 1;
    y = y - 1;
    int[][] blocks = piece.getBlocks();

    //loop through the piece's game blocks
    for (int i = 0; i < blocks.length; i++) {
      for (int j = 0; j < blocks[i].length; j++) {
        int pieceValue = blocks[i][j];
        //ignore empty blocks with value of 0
          if (pieceValue == 0) {
              continue;
          }
          //update the relevant grid block with the piece's value
        logger.debug("Piece block played at" + i + j);
        set(i + x, j + y, pieceValue);
      }
    }
    logger.info("{} piece successfully played at ({},{})",piece,x,y);
    return true;
  }

}
