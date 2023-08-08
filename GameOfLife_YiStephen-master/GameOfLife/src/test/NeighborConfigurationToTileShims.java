package test;

import java.util.ArrayList;

// Converts a single neighbor configuration to an
// array of TileShims representing those neighbors
public class NeighborConfigurationToTileShims
{
	private ArrayList<GnanTileShim> tileShims;
	
	public NeighborConfigurationToTileShims(int rowsP, int colsP, int neighborConfig, String tileName)
	{
		GameOfLifeTest.logln("This test verifies the student's getNumActiveNeighbors method, when called on a " + tileName + " tile, with elements in the neighbor Tile array having ages in the following order:");
		GameOfLifeTest.log("[");

		tileShims = new ArrayList<GnanTileShim>();
		
		NeighborConfigurationRangeIterator.callWithNeighborConfigurations(
				neighborConfig, 
				neighborConfig,
				-1,	1,				// Base, step size ignored if there's only one neighborConfig
				this::addAllNeighborTileShims);

		GameOfLifeTest.logln("]");
	}
	
	public GnanTileShim[] getTileShims()
	{
		return tileShims.toArray(new GnanTileShim[0]);
	}
	
	private void addTileShim(int age)
	{
		GameOfLifeTest.log("age " + age + ", ");
		tileShims.add(new GnanTileShim(age));
	}
	
	private void addAllNeighborTileShims(int neighborConfig)
	{
		// Iterate through each bit in neighborConfig, adding a shim for each one
		NeighborConfigurationRangeIterator.callWithDigit(
				neighborConfig,
				(digit, place) -> addTileShim(digit));
	}
}
