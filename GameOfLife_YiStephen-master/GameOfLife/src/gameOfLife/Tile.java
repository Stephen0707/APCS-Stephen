// DO NOT MODIFY!!!

package gameOfLife;

import java.awt.Color;

public interface Tile
{
	/**
	 * Returns the age of this tile
	 * 
	 * @return The age of this tile, as a non-negative integer.  0 indicates
	 * the tile is "dormant", positive numbers indicate the tile is "active",
	 * with larger numbers indicating older tiles.
	 */
    int getAge();
    
    
    
    
    /**
     * Returns the color this Tile should be painted in the World.
     * 
     * @return The color this Tile should be painted in the World
     */
    Color getColor();
    
    
    
    
    /**
     * Retrieves the replacement Tile object after this Tile has evolved
     * for one step.  The returned Tile object may have a different state
     * from the originally invoked Tile object BUT this method must NOT
     * change the state of the originally invoked Tile object
     *  
     * POSTCONDITION: The invoked Tile object's state is unchanged
     * 
     * @param neighbors An array of the eight Tile objects that completely surround
     * the invoked Tile object.
     * @return The Tile object which should replace this (originally invoked)
     * Tile object after a single evolution step.
     */
    Tile getUpdatedTile(Tile[] neighbors);
}
