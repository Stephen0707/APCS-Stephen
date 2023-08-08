package test;

import java.awt.Color;

import gameOfLife.Tile;

// Collection of fields that represent the state of a single
// Tile object.  Also some static helpers useful for determining
// expected behavior of various tile types
public class TileInfo
{
	public final TileType type;
	public final int row;
	public final int col;

	private final int age;
	private final Color color;

	public final int leaderRow;
	public final int leaderCol;
	private final TileInfo[][] tiMatrix;

	public static TileInfo tileInfoFromTile(TileType tileType, int row, int col, Tile tile)
	{
		return new TileInfo(tileType, tile.getAge(), tile.getColor(), row, col);
	}
	
	public static TileInfo findTileInfo(TileInfo[] tileInfos, int cols, int row, int col, int shimRow, int shimCol, CopycatPattern copycatPattern)
	{
		if (copycatPattern == CopycatPattern.MIRROR)
		{
			if (col >= (cols + 1) / 2)
			{
				col = cols - 1 - col;
			}
		}
		
		// FUTURE: Implement Kaleidoscope pattern
		
		if (row == shimRow && col == shimCol)
		{
			return new TileInfo(TileType.TESTSHIM, 0, row, col);
		}
		
		for (TileInfo tileInfo : tileInfos)
		{
			if (tileInfo.row == row && tileInfo.col == col)
			{
				return tileInfo;
			}
		}
		
		return null;
	}
	
	public static Color constantTileColorFromAge(int age)
	{
		return (age == 0) ? Color.DARK_GRAY : Color.LIGHT_GRAY;
	}
	
	public static Color rainbowTileColorFromAge(int age)
	{
		final Color[] COLORS =
			{
					Color.RED,
					Color.ORANGE,
					Color.YELLOW,
					Color.GREEN,
					Color.BLUE,
					Color.MAGENTA,
			};

		return COLORS[age % COLORS.length];
	}
	
	public static Color monoTileColorFromAge(int age)
	{
		return age == 0 ? Color.BLACK : Color.WHITE;		
	}
	
	public static Color immigrationTileConstructedColorFromAge(int age)
	{
		if (age == 0)
		{
			return Color.BLACK;
		}
		
		if (age == 1)
		{
			return Color.GREEN;
		}
		
		if (age == 2)
		{
			return Color.BLUE;
		}
		
		throw new IllegalArgumentException("Internal test error, cannot calculate original immigration color from age " + age);
	}
	
	public static Color quadTileConstructedColorFromAge(int age)
	{
		final Color[] colors = 
			{
					Color.BLACK,
					Color.BLUE,
					Color.GREEN,
					Color.RED,
					Color.YELLOW,
			};
		
		if (age >= colors.length)
		{
			throw new IllegalArgumentException("Internal test error, cannot calculate original quad color from age " + age);
		}
		
		return colors[age];
	}

	// For creating most tile types.  Even works for immigration tiles, but only
	// a newly-formed tile (as opposed to a representation of an evolved immigration
	// tile, whose color can't be determined by the age)
	public TileInfo(TileType typeP, int ageP, int rowP, int colP)
	{
		type = typeP;
		age = ageP;
		if (typeP == TileType.IMMIGRATION)
		{
			color = immigrationTileConstructedColorFromAge(ageP);
		}
		else
		{
			color = null;
		}
		row = rowP;
		col = colP;
		
		leaderRow = -1;
		leaderCol = -1;
		tiMatrix = null;
		
	}
	
	// For creating a copycat tile
	public TileInfo(int rowP, int colP, int leaderRowP, int leaderColP, TileInfo[][] tiMatrixP)
	{
		type = TileType.COPYCAT;
		age = -1;
		color = null;
		row = rowP;
		col = colP;
		
		leaderRow = leaderRowP;
		leaderCol = leaderColP;
		tiMatrix = tiMatrixP;
	}
	
	// For creating a color tile, like immigration
	public TileInfo(TileType typeP, int ageP, Color colorP, int rowP, int colP)
	{
		type = typeP;
		age = ageP;
		color = colorP;
		row = rowP;
		col = colP;
		
		leaderRow = -1;
		leaderCol = -1;
		tiMatrix = null;
	}
	
	public int getAge()
	{
		if (type == TileType.COPYCAT)
		{
			return tiMatrix[leaderRow][leaderCol].getAge();
		}
		
		return age;
	}
	
	public Color getColor()
	{
		// If we're primed with a specific color, just return that
		if (color != null)
		{
			return color;
		}
		
		// Else try to derive what color we should be automatically 
		switch(type)
		{
			case CONSTANT:
				return constantTileColorFromAge(age);

			case RAINBOW:
				return rainbowTileColorFromAge(age);
				
			case MONO:
				return monoTileColorFromAge(age);
				
			case COPYCAT:
				return tiMatrix[leaderRow][leaderCol].getColor();
				
			case IMMIGRATION:
				return immigrationTileConstructedColorFromAge(age);
				
			case QUAD:
				return quadTileConstructedColorFromAge(age);
				
			case NONE:
				return Color.BLACK;
				
			default:
				throw new UnsupportedOperationException("Internal test error in getColor: unknown TileType " + type);
		}
	}
	
	public static String tileNameFromType(TileType type)
	{
		switch(type)
		{
			case CONSTANT:
				return "constant";

			case RAINBOW:
				return "rainbow";
				
			case MONO:
				return "mono";
				
			case COPYCAT:
				return "copycat";
				
			case IMMIGRATION:
				return "immigration";
				
			case QUAD:
				return "quad";

			default:
				GameOfLifeTest.logln("Internal test error, unknown TileType " + type);
				throw new UnsupportedOperationException();
		}
	}

	public static String tileClassNameFromType(TileType type)
	{
		switch(type)
		{
			case CONSTANT:
				return "gameOfLife.ConstantTile";

			case RAINBOW:
				return "gameOfLife.RainbowTile";
				
			case MONO:
				return "gameOfLife.MonoTile";
				
			case COPYCAT:
				return "gameOfLife.CopycatTile";
				
			case IMMIGRATION:
				return "gameOfLife.ImmigrationTile";
				
			case QUAD:
				return "gameOfLife.QuadTile";

			default:
				GameOfLifeTest.logln("Internal test error, unknown TileType " + type);
				throw new UnsupportedOperationException();
		}
	}
}

