package test;

import java.util.function.BiFunction;

// Helpers to generate ASCII illustrations of the World, for
// more helpful test error messages
public class Illustrator
{
	private int rows;
	private int cols;
	
	public Illustrator(int rowsP, int colsP)
	{
		rows = rowsP;
		cols = colsP;
	}
	
	// Used for getting an ASCII illustration of the grid based on a particular array
	// of TileInfo objects, a copycat pattern, optionally a shim location
	
	public String[] getIllustration(TileInfo[] tileInfos, CopycatPattern copycatPattern)
	{
		return getIllustration(tileInfos, -1 /* shimRow */, -1 /* shimCol */, copycatPattern);
	}

	public String[] getIllustration(TileInfo[] tileInfos, int shimRow, int shimCol, CopycatPattern copycatPattern)
	{
		return getIllustration((row, col) -> TileInfo.findTileInfo(tileInfos, cols, row, col, shimRow, shimCol, copycatPattern));
	}
	
	
	// Used for getting an ASCII illustration of the grid based on a particular array
	// of TileInfo objects, assuming no copycat tiles, and optionally a shim location
	
	public String[] getIllustration(TileInfo[] tileInfos)
	{
		return getIllustration(tileInfos, -1 /* shimRow */, -1 /* shimCol */);
	}
	
	public String[] getIllustration(TileInfo[] tileInfos, int shimRow, int shimCol)
	{
		return getIllustration(tileInfos, shimRow, shimCol, CopycatPattern.NONE);
	}
	
	// Used for getting an ASCII illustration of the grid based on a 2D matrix
	// of TileInfo objects, assuming no copycat tiles
	public String[] getIllustration(TileInfo[][] tiMatrix)
	{
		return getIllustration((row, col) -> tiMatrix[row][col]);
	}
	
	// Used for getting an ASCII illustration of the grid based on a passed
	// method that gets a TileInfo object for a particular row/col on demand
	public String[] getIllustration(BiFunction<Integer, Integer, TileInfo> tileInfoGetter)
	{
		String[] ret = new String[rows];
		for (int row=0; row < rows; row++)
		{
			ret[row] = "";
			for (int col = 0; col < cols; col++)
			{
				ret[row] += getSingleTileIllustration(tileInfoGetter.apply(row, col));
			}
		}
		
		return ret;
	}
	
	// Prints all acquired illustrations with a legend
	public void logIllustrations(String[] illustrationPre, String[] illustrationExpectedPost, String[] illustrationActualPost, int numSteps)
	{
		logIllustrationLegend();
		logIllustrationsNoLegend(illustrationPre, illustrationExpectedPost, illustrationActualPost, numSteps);
	}
	
	// Prints all acquired illustrations without a legend
	public void logIllustrationsNoLegend(String[] illustrationPre, String[] illustrationExpectedPost, String[] illustrationActualPost, int numSteps)
	{
		GameOfLifeTest.logln("");
		GameOfLifeTest.logln("Expected evolution:");
		logIllustration(illustrationPre, illustrationExpectedPost, numSteps);
		GameOfLifeTest.logln("");
		GameOfLifeTest.logln("Actual evolution:");
		logIllustration(illustrationPre, illustrationActualPost, numSteps);
		GameOfLifeTest.logln("");
	}
	
	// Prints a single world illustration.  Useful for tests that don't do a
	// full evolve of the world, and therefore don't need to log a pre and post
	// side by side.
	public void logSingleIllustration(String[] illustration)
	{
		for (String line : illustration)
		{
			GameOfLifeTest.logln(line);
		}
		GameOfLifeTest.logln("");
	}
	
	// Prints a legend
	public void logIllustrationLegend()
	{
		GameOfLifeTest.logln("In the following illustrations of the world's evolution,");
		GameOfLifeTest.logln(".		Represents a dormant tile");
		GameOfLifeTest.logln("AC		Represents an active tile, where");
		GameOfLifeTest.logln("  		A is the age of the tile, and");
		GameOfLifeTest.logln("  		C is the color of the tile, with the following abbreviations:");
		GameOfLifeTest.log(ColorString.getColorAbbreviationLegend("  		    "));
		GameOfLifeTest.logln("  		    (black is shown as an empty space)");
	}	

	private String getSingleTileIllustration(TileInfo tileInfo)
	{
		if (tileInfo == null)
		{
			return "  .  ";
		}
		
		if (tileInfo.type == TileType.TESTSHIM)
		{
			return " *** ";
		}
		
		return (tileInfo.getAge() == 0) ? 
				"  .  " : 
					" " + getAgeString(tileInfo) + ColorString.abbreviationFromColor(tileInfo.getColor()) + " ";
	}
	
	private String getAgeString(TileInfo tileInfo)
	{
		int age = tileInfo.getAge();
		if (age < 10)
		{
			return " " + age;
		}
		
		return "" + age;
	}
	
	private void logIllustration(String[] pre, String[] post, int numSteps)
	{
		int len = pre.length;
		for (int line = 0; line < len / 2 - 1; line++)
		{
			GameOfLifeTest.logln(pre[line] + "               " + post[line]);
		}
		GameOfLifeTest.logln(pre[len / 2 - 1] +  getIllustrationStepsString(numSteps) + post[len / 2 - 1]);
		GameOfLifeTest.logln(pre[len / 2] +  "   -------->   " + post[len / 2]);
		for (int line = len / 2 + 1; line < len; line++)
		{
			GameOfLifeTest.logln(pre[line] + "               " + post[line]);
		}
	}
	
	private String getIllustrationStepsString(int numSteps)
	{
		String ret = numSteps + " step(s)";
		int spaces = 15 - ret.length();
		ret = getSpaces(spaces / 2) + ret + getSpaces(spaces / 2 + spaces % 2);
		return ret;
	}
	
	// Don't call me with a parameter bigger than 10, dummy.
	private String getSpaces(int numSpaces)
	{
		final String spaces = "          ";
		final int len = spaces.length();
		if (numSpaces > spaces.length())
		{
			throw new UnsupportedOperationException("Internal test error, getSpaces called with invalid parameter.");
		}
		return spaces.substring(0, numSpaces);
	}
}
