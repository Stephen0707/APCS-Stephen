package test;

import java.awt.Color;

import gameOfLife.Tile;

// Shim that allows age customization (e.g., for unit-testing
// LifeTile's getNumActiveNeighbors method)  
public class GnanTileShim implements Tile
{
	private int age;
	
	public GnanTileShim(int ageP)
	{
		age = ageP;
	}
	
	@Override
	public int getAge()
	{
		return age;
	}

	@Override
	public Color getColor()
	{
		GameOfLifeTest.golAssertTrue("Your getNumActiveNeighbors method should not call getColor on its neighbors to determine if they're active.  The only reliable way to determine whether a neighbor is active is to check whether its age is positive.",
				false);
		
		return null;
	}

	@Override
	public Tile getUpdatedTile(Tile[] neighbors)
	{
		return this;
	}

}
