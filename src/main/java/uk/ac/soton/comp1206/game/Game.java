package uk.ac.soton.comp1206.game;

import java.util.HashSet;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.soton.comp1206.component.GameBlock;
import uk.ac.soton.comp1206.component.GameBlockCoordinate;
import uk.ac.soton.comp1206.event.GameLoopListener;
import uk.ac.soton.comp1206.event.GameOverListener;
import uk.ac.soton.comp1206.event.LineClearedListener;
import uk.ac.soton.comp1206.event.NextPieceListener;
import uk.ac.soton.comp1206.utility.Multimedia;

/**
 * The Game class handles the main logic, state and properties of the TetrECS game. Methods to manipulate the game state
 * and to handle actions made by the player should take place inside this class.
 */
public class Game {

    private static final Logger logger = LogManager.getLogger(Game.class);

    private NextPieceListener nextPieceListener;
    private LineClearedListener lineClearedListener;
    private GameLoopListener gameLoopListener;
    private GameOverListener gameOverListener;

    /**
     * Number of rows
     */
    protected final int rows;

    /**
     * Number of columns
     */
    protected final int cols;

    /**
     * The grid model linked to the game
     */
    protected final Grid grid;

    /**
     * The current piece to be played
     */
    protected GamePiece currentPiece;

    /**
     * The next upcoming piece that will be played after the current piece
     */
    protected GamePiece followingPiece;


    /**
     * Player's current score, initial value of 0.
     */
    private IntegerProperty score = new SimpleIntegerProperty(0);
    /**
     * Player's current level, initial value of 0.
     */
    private IntegerProperty level = new SimpleIntegerProperty(0);
    /**
     * Number of lives remaining for the player, initial value of 3.
     */
    private IntegerProperty lives = new SimpleIntegerProperty(3);
    /**
     * Score multiplier, initial value of 1.
     */
    private IntegerProperty multiplier = new SimpleIntegerProperty(1);

    /**
     * The top high score from the local scores
     */
    private IntegerProperty highscore = new SimpleIntegerProperty();
    private int nextThousand = 1;

    /**
     * Game timer to count down how long is left for piece to be played before a life is lost.
     */
    private Timer gameTimer = new Timer();

    /**
     * Task for the timer to do once it runs out
     */
    private TimerTask task;

    /**
     * Create a new game with the specified rows and columns. Creates a corresponding grid model.
     * @param cols number of columns
     * @param rows number of rows
     */
    public Game(int cols, int rows) {
        this.cols = cols;
        this.rows = rows;

        //Create a new grid model to represent the game state
        this.grid = new Grid(cols,rows);
    }

    /**
     * Start the game
     */
    public void start() {
        logger.info("Starting game");
        initialiseGame();
    }

    /**
     * Initialise a new game and set up anything that needs to be done at the start
     */
    public void initialiseGame() {
        logger.info("Initialising game");
        //set up initial game pieces
        followingPiece=spawnPiece();
        currentPiece=nextPiece();
        //call listener with initial game pieces
        nextPieceListener.nextPiece(currentPiece,followingPiece);
    }

    /**
     * Handle what should happen when a particular block is clicked
     * @param gameBlock the block that was clicked
     */
    public void blockClicked(GameBlock gameBlock) {
        logger.debug("Block clicked at {},{}",gameBlock.getX(),gameBlock.getY());
        //Get the position of this block
        int x = gameBlock.getX();
        int y = gameBlock.getY();
        var piecePlayed = grid.playPiece(currentPiece,x,y);
        //check if piece was played successfully
        if (piecePlayed) {
            logger.debug("Piece played after clicking block");
            Multimedia.playAudio("place.wav");
            //cancel the current timer task
            task.cancel();
            //remove cancelled tasks from timer
            gameTimer.purge();
            //generate next piece
            nextPiece();
            //clear lines
            afterPiece();
        }
        else {
            Multimedia.playAudio("fail.wav");
        }
    }

    /**
     * Clear any full vertical and horizontal lines that have been made after placing a piece
     */
    private void afterPiece() {
        //keep track of lives cleared
        var linesCleared = 0;
        //keep track of blocks that have been cleared
        var clearedBlocks = new HashSet<GameBlockCoordinate>();

        //find full horizontal rows
        for(int i=0;i<cols;i++){
            int counter = 0;
            for(int j=0; j<rows;j++){
                //count non-empty blocks
                if (grid.get(i,j)==0)continue;
                counter++;
            }
            //check if non-empty blocks span number of rows
            if (counter==rows){
                //if yes, add another line cleared
                linesCleared++;
                for (int j=0;j<rows;j++){
                    //add each block in that row to the cleared blocks set
                    clearedBlocks.add(new GameBlockCoordinate(i,j));
                }
            }
        }

        //find full vertical columns
        for(int j=0;j<rows;j++){
            int counter = 0;
            for(int i=0; i<cols;i++){
                //count non-empty blocks
                if (grid.get(i,j)==0)continue;
                counter++;
            }
            //check if non-empty blocks span number of columns
            if (counter==cols){
                //if yes, add another line cleared
                linesCleared++;
                for (int i = 0; i < cols; i++) {
                    //add each block in that column to the cleared blocks set
                    clearedBlocks.add(new GameBlockCoordinate(i,j));
                }
            }
        }

        if (!clearedBlocks.isEmpty()){
        clearBlocks(clearedBlocks);
        }

        logger.info("{} blocks cleared and {} lines cleared.",clearedBlocks.size(),linesCleared);
        //change score and multiplier accordingly
        changeScore(linesCleared,clearedBlocks.size());
        changeMultiplier(linesCleared);
    }

    /**
     * Clear the blocks made by the full lines on the grid by setting their value to 0
     * @param blocks the blocks to clear
     */
    private void clearBlocks(HashSet<GameBlockCoordinate> blocks) {
        logger.debug("Clearing {} blocks",blocks.size());
        //call the listener
        lineClearedListener.lineCleared(blocks);
        for (GameBlockCoordinate block:blocks){
            //set each block's value to 0 on the grid
            getGrid().set(block.getX(),block.getY(),0);
        }
    }

    /**
     * Get the grid model inside this game representing the game state of the board
     * @return game grid model
     */
    public Grid getGrid() {
        return grid;
    }

    /**
     * Get the number of columns in this game
     * @return number of columns
     */
    public int getCols() {
        return cols;
    }

    /**
     * Get the number of rows in this game
     * @return number of rows
     */
    public int getRows() {
        return rows;
    }

    /**
     * Spawns in a random piece by creating a GamePiece with a random value from 0-14
     * @return the randomly generated piece
     */
    public GamePiece spawnPiece(){
        Random r = new Random();
        var piece = GamePiece.createPiece(r.nextInt(15));
        logger.info("Spawning in new piece {}",piece);
        return piece;
    }

    /**
     * Replaces the current piece with the following piece and spawns in a new piece for
     * the following piece.
     * @return the updated current piece
     */
    public GamePiece nextPiece(){
        currentPiece = followingPiece;
        followingPiece = spawnPiece();
        //call the listener
        nextPieceListener.nextPiece(currentPiece,followingPiece);

        //reset the timer as the piece has been played
        resetTimer();

        return currentPiece;
    }

    /**
     * Resets the game timer countdown by scheduling a new timer task which runs the gameloop with
     * the current timer delay.
     */
    private void resetTimer() {
        logger.info("Resetting game timer");
        //create new timer task
        task = new TimerTask() {
            public void run() {
                gameLoop();
            }
        };
        //call the game loop listener with the current timer delay
        Platform.runLater(() ->{
            gameLoopListener.gameLooped(getTimerDelay());
        });
        //schedule game timer
        gameTimer.schedule(task,getTimerDelay());
    }

    /**
     * Set the value of the score integer property
     * @param score the value to set score to
     */
    public void setScore(int score){
        logger.info("Score set to {}",score);
        this.score.set(score);
    }

    /**
     * Get the value of the score integer property
     * @return the value of score
     */
    public int getScore(){
        logger.debug("Retrieving value of score as {}",this.score.get());
        return this.score.get();
    }

    /**
     * Get the score property
     * @return the IntegerProperty representing the score
     */
    public IntegerProperty scoreProperty(){
        return score;
    }

    /**
     * Set the value of the level integer property
     * @param level the value to set the level to
     */
    public void setLevel(int level){
        logger.info("Level set to {}",level);
        this.level.set(level);
    }

    /**
     * Set the value of the highscore integer property
     * @param highscore the value to set the highscore to
     */
    public void setHighscore(int highscore){
        logger.info("Highscore set to {}",highscore);
        this.highscore.set(highscore);
    }

    /**
     * Get the value of the highscore integer property
     * @return the value of highscore
     */
    public int getHighscore(){
        logger.debug("Retrieving the value of highscore to be {}",highscore.get());
        return this.highscore.get();
    }

    /**
     * Get the highscore property
     * @return return the IntegerProperty representing the highscore
     */
    public IntegerProperty highscoreProperty(){
        return highscore;
    }

    /**
     * Get the value of the level integer property
     * @return the value of level
     */
    public int getLevel(){
        logger.debug("Retrieving the value of level to be {}",this.level.get());
        return this.level.get();
    }

    /**
     * Get the level property
     * @return return the IntegerProperty representing the level
     */
    public IntegerProperty levelProperty(){
        return level;
    }

    /**
     * Set the value of lives integer property
     * @param lives the value to set the lives to
     */
    public void setLives(int lives){
        logger.info("Lives set to {}",lives);
        this.lives.set(lives);
    }

    /**
     * Get the value of the lives integer property
     * @return the value of lives
     */
    public int getLives(){
        logger.debug("Retrieving the value of lives to be {}",this.lives.get());
        return this.lives.get();
    }

    /**
     * Get the lives property
     * @return the IntegerProperty representing the lives
     */
    public IntegerProperty livesProperty(){
        return lives;
    }

    /**
     * Set the value of the multiplier integer property
     * @param multiplier the value to set the multiplier to
     */
    public void setMultiplier(int multiplier){
        logger.info("Multiplier set to {}",multiplier);
        this.multiplier.set(multiplier);
    }

    /**
     * Get the value of the multiplier integer property
     * @return the value of the multiplier
     */
    public int getMultiplier(){
        logger.debug("Retrieved the value of multiplier to be {}",this.multiplier.get());
        return this.multiplier.get();
    }

    /**
     * Get the multiplier property
     * @return the IntegerProperty representing the multiplier
     */
    public IntegerProperty multiplierProperty(){
        return multiplier;
    }

    /**
     * Update the value of score depending on the number of blocks and lines cleared
     * @param lines the number of lines cleared
     * @param blocks the number of blocks cleared
     */
    public void changeScore(int lines, int blocks){
        //update the value of the score
        this.score.set(score.getValue()+(lines*blocks*10*multiplier.getValue()));
        logger.info("Updated score to {}",score.getValue());
        //update level and highscore
        updateLevel();
        updateHighscore();
    }

    /**
     * Update the value of the highscore to be the maximum of the current highscore or the user's score
     */
    public void updateHighscore(){
        highscore.set(Math.max(highscore.get(),score.get()));
        logger.info("Highscore updated to {}",highscore.get());
    }

    /**
     * Update the level if it has reached the next thousand points
     */
    public void updateLevel(){
        //check if score is greater than the next thousand milestone
        if (score.getValue()!=0 && score.getValue()>nextThousand*1000){
            Multimedia.playAudio("level.wav");
            //update the level and increment the next thousand to be aiming for
            this.level.set(level.getValue()+1);
            this.nextThousand++;
            logger.info("Level updated to {}",this.level.get());
        }
    }

    /**
     * Increases the multiplier if more than one line has been cleared. Else, sets back to 1.
     * @param lines the number of lines that have been cleared
     */
    public void changeMultiplier(int lines){
        //check if a line has been cleared
        if (lines>0){
            //increment the multipier
            multiplier.set(multiplier.getValue()+1);
            logger.info("Updated multiplier to {}",multiplier.getValue());
        }else multiplier.set(1);
        logger.info("Multiplier reset back to {}",multiplier.getValue());
    }

    /**
     * Set the NextPieceListener
     * @param listener the listener to listen to any nextPiece calls
     */
    public void setNextPieceListener(NextPieceListener listener){
        this.nextPieceListener = listener;
    }

    /**
     * Set the lineClearedListener
     * @param listener the listener to listen to any lineCleared calls
     */
    public void setLineClearedListener(LineClearedListener listener){
        this.lineClearedListener = listener;
    }

    /**
     * Set the GameLoopListener
     * @param listener the listener to listen to any gameLooped calls
     */
    public void setOnGameLoopListener(GameLoopListener listener){
        this.gameLoopListener = listener;
    }

    /**
     * Set the GameOverListener
     * @param listener the listener to listen to any gameOver calls
     */
    public void setGameOverListener(GameOverListener listener){
        this.gameOverListener = listener;
    }

    /**
     * Rotate the current piece once
     * @return the current piece after it has been rotated
     */
    public GamePiece rotateCurrentPiece(){
        logger.info("Rotating piece {}",currentPiece);
        currentPiece.rotate();
        return currentPiece;
    }

    /**
     * Rotate the current piece with the given number of rotations
     * @param num the number of rotations
     * @return the current piece after it has been rotated
     */
    public GamePiece rotateCurrentPiece(int num){
        logger.info("Rotating piece {}",currentPiece);
        currentPiece.rotate(num);
        return currentPiece;
    }



    /**
     * Swaps the current piece and the following piece
     * @return the updated current and following piece after they have been swapped
     */
    public GamePiece[] swapCurrentPiece(){
        logger.info("Swapping {} with {}",currentPiece,followingPiece);
        //store in temporary variable
        var temp = currentPiece;
        currentPiece = followingPiece;
        followingPiece = temp;
        return new GamePiece[]{currentPiece,followingPiece};
    }

    /**
     * Calculate the current timer delay as at the maximum of either 2500 milliseconds
     * or 12000 - 500 * the current level
     * @return the current timer delay
     */
    public int getTimerDelay(){
        return Math.max(2500,12000-(500*level.getValue()));
    }

    /**
     * Removes a life and loops the game again, unless lives are already at 0 in which case the
     * game is ended.
     */
    public void gameLoop(){
        //check if lives have run out
        if (this.lives.get() == 0){
            logger.info("Lives have run out");
            //cancel timer task and timer
            task.cancel();
            gameTimer.purge();
            gameTimer.cancel();
            Platform.runLater(()->{
                //call the listener
                gameOverListener.gameOver(this);
            });
        }
        else{
            Multimedia.playAudio("lifelose.wav");
            //decrement lives
            lives.set(lives.getValue()-1);
            logger.info("Game looped, lives remaining:{}",lives.get());
            //generate a new piece
            currentPiece = nextPiece();
            //reset multiplier
            multiplier.set(1);
        }

    }

    /**
     * Cancel the timer at the end of the game
     */
    public void endGame() {
        gameTimer.cancel();
    }

}
