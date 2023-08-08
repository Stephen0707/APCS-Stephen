// DO NOT MODIFY!!!

package gameOfLife;

public interface TileGrid
{
	/**
	 * Returns the Tile object stored at the specified row and column
	 * @param row Row of the Tile to return
	 * @param column Column of the Tile to return
	 * @return Tile object stored at the specified row and column
	 */
	public Tile getTile(int row, int column);
	
	/**
	 * Places the specified Tile object into the grid
	 * @param row Row where the Tile should be placed
	 * @param column Column where the Tile should be placed
	 * @param tile Tile object to add to the grid
	 */
	public void setTile(int row, int column, Tile tile); 
	
	/**
	 * Executes a single command to affect the grid's contents
	 * @param command A single command and its parameters, which
	 * affects the grid's contents.  Examples: "fill mono 0" or
	 * "evolve 10 20" 
	 */
	public void processCommand(String command);
}
