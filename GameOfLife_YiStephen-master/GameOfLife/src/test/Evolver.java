package test;

import java.awt.Color;
import java.security.InvalidParameterException;
import java.util.ArrayList;

// Simulates evolving tiles, to determine "expected" patterns
// the tests compare with the student's actual results
public class Evolver
{
	private TileInfo[][] grid;

	public Evolver (int rows, int cols, TileType fillTileType, TileInfo[] tileInfos, CopycatPattern copycatPattern)
	{
		grid = new TileInfo[rows][cols];
		
		// Before reading the "interesting" tiles out of tileInfos, fill
		// the grid with the "uninteresting" fill tile type (of which some
		// will be overwritten with tileInfos).
		for (int row = 0; row < rows; row++)
		{
			for (int col = 0; col < cols; col++)
			{
				grid[row][col] = new TileInfo(fillTileType, 0 /* age */, row, col);
			}
		}

		for (TileInfo tileInfo : tileInfos)
		{
			grid[tileInfo.row][tileInfo.col] =
					new TileInfo(
							tileInfo.type,
							tileInfo.getAge(),
							tileInfo.row,
							tileInfo.col);
		}
		
		if (copycatPattern == CopycatPattern.MIRROR)
		{
			// right half: copycat mirror left
			for (int row=0; row < rows; row++)
			{
				for (int col=(cols + 1) / 2; col < cols; col++)

				{
					grid[row][col] = new TileInfo(
							row, col, 
							row, cols - 1 - col, // leader row and col
							grid); 
				}
			}
		}
		else if (copycatPattern == CopycatPattern.KALEIDOSCOPE)
		{
			// FUTURE: Implement Kaleidoscope pattern
		}
	}

	public void evolve(int numSteps)
	{
		for (int i=0; i < numSteps; i++)
		{
			evolveGridOneStep();
		}
	}

	private void evolveGridOneStep()
	{
		int rows = grid.length;
		int cols = grid[0].length;
		TileInfo[][] newGrid = new TileInfo[rows][cols];

		for (int row=0; row < rows; row++)
		{
			for (int col=0; col < cols; col++)
			{
				newGrid[row][col] = evolveTileOneStep(row, col);
			}
		}

		grid = newGrid;
	}

	private TileInfo evolveTileOneStep(int row, int col)
	{
		TileInfo origTile = grid[row][col];
		
		switch(origTile.type)
		{
			case NONE:
				return new TileInfo(TileType.NONE, 0, row, col);
				
			case CONSTANT:
				return new TileInfo(TileType.CONSTANT, origTile.getAge(), row, col);

			case RAINBOW:
				return new TileInfo(TileType.RAINBOW, origTile.getAge() + 1, row, col);
				
			case MONO:
				return getUpdatedMonoTileInfo(origTile);
				
			case IMMIGRATION:
				return getUpdatedColoredTileInfo(TileType.IMMIGRATION, origTile);
				
			case COPYCAT:
				return evolveTileOneStep(origTile.leaderRow, origTile.leaderCol);
				
			case QUAD:
				return getUpdatedColoredTileInfo(TileType.QUAD, origTile);

			default:
				GameOfLifeTest.logln("Internal test error, unknown TileType " + origTile.type);
				throw new UnsupportedOperationException();
		}
	}
	
	private Color[] getActiveNeighborColors(TileInfo orig)
	{
		ArrayList<Color> ret = new ArrayList<Color>();
		int rows = grid.length;
		int cols = grid[0].length;
		
		for (int rowOffset = -1; rowOffset <= 1; rowOffset++)
		{
			for (int colOffset = -1; colOffset <= 1; colOffset++)
			{
				if (rowOffset == 0 && colOffset == 0)
				{
					continue;
				}
				
				int row = (orig.row + rows + rowOffset) % rows;
				int col = (orig.col + cols + colOffset) % cols;
				if (grid[row][col].getAge() > 0)
				{
					ret.add(grid[row][col].getColor());
				}
			}
		}
		
		return ret.toArray(new Color[0]);
	}
	
	private int getNewLifeTileAge(TileInfo orig, Color[] neighbors)
	{
		int newAge = 0;
		if (orig.getAge() == 0)
		{
			if (neighbors.length == 3)
			{
				newAge = 1;
			}
		}
		else
		{
			if (neighbors.length == 2 || neighbors.length == 3)
			{
				newAge = orig.getAge() + 1;
			}
		}
	
		return newAge;
	}
	
	private TileInfo getUpdatedMonoTileInfo(TileInfo orig)
	{
		Color[] neighbors = getActiveNeighborColors(orig);
		
		int newAge = getNewLifeTileAge(orig, neighbors);

		return new TileInfo(TileType.MONO, newAge, orig.row, orig.col);
	}

	private Color getMajorityColor(final Color[] colorSet, Color[] neighbors)
	{
	    // Pick the majority color of the 3 neighbor colors
        int[] colorVotes = new int[colorSet.length];
        
        for (Color color : neighbors)
        {
        	for (int i=0; i < colorSet.length; i++)
        	{
                if (color.equals(colorSet[i]))
                {
                    colorVotes[i]++;
                }
        	}
        }
        
        int highestVotes = colorVotes[0];
        int iMajorityColor = 0;
        
        for (int i=1; i < colorVotes.length; i++)
        {
        	if (colorVotes[i] > highestVotes)
        	{
        		highestVotes = colorVotes[i];
        		iMajorityColor = i;
        	}
        }
        
        if (highestVotes == 1)
        {
        	// This would indicate a tie.  Pick the unrepresented color
        	for (int i=0; i < colorVotes.length; i++)
        	{
        		if (colorVotes[i] == 0)
        		{
        			return colorSet[i];
        		}
        	}

        	throw new UnsupportedOperationException("Internal test failure: Unable to find unrepresented color");
        }
        
        return colorSet[iMajorityColor];
	}
	
	private TileInfo getUpdatedColoredTileInfo(TileType tileType, TileInfo orig)
	{
		if (tileType != TileType.IMMIGRATION && tileType != TileType.QUAD)
		{
			throw new InvalidParameterException("Internal test failure: Unsupported tile type: " + tileType);
		}

		Color[] neighbors = getActiveNeighborColors(orig);
		
		int newAge = getNewLifeTileAge(orig, neighbors);

		Color color;
		if (newAge == 0)
		{
			// Dormant = black
			color = Color.BLACK;
		}
		else if (newAge == 1)
		{
			// Just born, so figure out color by taking majority
			Color[] colorSet;
			if (tileType == TileType.IMMIGRATION)
			{
				colorSet = new Color[] { Color.BLUE, Color.GREEN }; 
			}
			else
			{
				// QUAD
				colorSet = new Color[] { Color.BLUE, Color.GREEN, Color.RED, Color.YELLOW };
			}

			color = getMajorityColor(colorSet, neighbors);
		}
		else
		{
			// Otherwise, color remains as it was
			color = orig.getColor();
		}
		return new TileInfo(tileType, newAge, color, orig.row, orig.col);
	}
	
	public TileInfo getTileInfo(int row, int col)
	{
		return grid[row][col];
	}
	
	public TileInfo[][] getTileInfoMatrix()
	{
		return grid;
	}
	
	// To help with debugging test code
	public String toString()
	{
		String ret = "";
		for (int row = 0; row < grid.length; row++)
		{
			for (int col = 0; col < grid[0].length; col++)
			{
				TileInfo tile = grid[row][col];
				ret += TileInfo.tileNameFromType(tile.type) + " " + tile.getAge() + "\t";
			}
			ret += "\n";
		}
		return ret;
	}

}
