package test;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import gameOfLife.Tile;

// Shim for unit-testing getUpdatedTile neighbors parameter
public class GutTileShim implements Tile
{
	private List<Integer> neighborConfigurations;
	private int iNeighborConfigurations;
	
	public GutTileShim(List<Integer> neighborConfigurationsP)
	{
		neighborConfigurations = neighborConfigurationsP;
		iNeighborConfigurations = 0;
	}

	@Override
	public int getAge()
	{
		return 1;
	}

	@Override
	public Color getColor()
	{
		return Color.CYAN;
	}

	@Override
	public Tile getUpdatedTile(Tile[] neighbors)
	{
		GameOfLifeTest.logln("\nVerifying neighbors passed to getUpdatedTile are correct...\n");
		
		int neighborConfiguration = neighborConfigurations.get(iNeighborConfigurations);
		iNeighborConfigurations++;
		
		GameOfLifeTest.golAssertTrue("Encountered an error during evolve command #" + iNeighborConfigurations + ".  getUpdatedTile was called with a null neighbors array as parameter.", (neighbors != null));
		GameOfLifeTest.golAssertEquals("Encountered an error during evolve command #" + iNeighborConfigurations + ".  getUpdatedTile was called with a neighbors array containing the wrong number of elements.", 8, neighbors.length);
		
		int expectedNumLiveNeighbors = countOnes(neighborConfiguration);
		int actualNumLiveNeighbors = getNumLiveNeighbors(neighbors, iNeighborConfigurations);
		
		GameOfLifeTest.golAssertEquals("Encountered an error during evolve command #" + iNeighborConfigurations + ".  getUpdatedTile was called with a neighbors array correctly containing 8 elements, but the number of 'live' neighbors (neighbors with a positive age) in that array was incorrect.",
				expectedNumLiveNeighbors, actualNumLiveNeighbors);
		
		return this;
	}
	
	private int countOnes(int base2)
	{
		// Only works because our base 2 numbers never exceed 8 bits!
		return base2 % 9;
	}
	
	private int getNumLiveNeighbors(Tile[] neighbors, int iNeighborConfigurations)
	{
        int ret = 0;
        for (Tile tile : neighbors)
        {
        	GameOfLifeTest.golAssertNotNull("Encountered an error during evolve command #" + iNeighborConfigurations + ".  getUpdatedTile was called with a neighbors array containing a null element (all neighbors must be non-null).",
    				tile);
    		
            if (tile.getAge() > 0)
            {
                ret++;
            }
        }        
        
        return ret;
	}
	
	public int getNumCallsToGetUpdatedTile()
	{
		return iNeighborConfigurations;
	}
}
