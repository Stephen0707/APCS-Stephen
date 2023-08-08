package test;

import java.awt.Color;

public class GameOfLifeTestMediaShim extends TestMediaShim
{
	private static class ColorToVerify
	{
		private Color color;
		private boolean verified;

		public ColorToVerify(Color colorP)
		{
			color = colorP;
			verified = false;
		}
	}

	private ColorToVerify[][] expectedColors;
	private Color prefillColor;

	@Override
	public void paintSolidColor(int row, int column, Color color)
	{
		super.paintSolidColor(row, column, color);

		if (expectedColors == null)
		{
			return;
		}
		
		ColorToVerify expectedColor = expectedColors[row][column];
		if (expectedColor == null)
		{
			return;
		}
		
		if (!expectedColor.color.equals(color))
		{
			String errorMessage = "core.API.paintSolidColor was called on row " + row + 
					", column " + column + " with the wrong color.";
			
			if (prefillColor != null && color.equals(prefillColor))
			{
				errorMessage += "  As stated in the spec, if your fill command calls your setTile method " +
						"for each space, then you'll needlessly redraw the entire grid each time the " +
						"fill command adds a Tile.  This will be very slow, and will cause this test to fail.  " +
						"Since paintSolidColor was called with '" +
						ColorString.nameFromColor(color) + 
						"', that might be the problem, so double-check it.";
			}
 
			GameOfLifeTest.golAssertColorsEqual(errorMessage, expectedColor.color, color);
		}

		expectedColor.verified = true;
	}

	public void clearExpectedAndPrefillColors(int rows, int cols)
	{
		expectedColors = new ColorToVerify[rows][cols];
		prefillColor = null;
	}

	public void addExpectedColors(int rowLow, int rowHigh, int colLow, int colHigh, Color color)
	{
		for (int row=rowLow; row <= rowHigh; row++)
		{
			for (int col=colLow; col <= colHigh; col++)
			{
				expectedColors[row][col] = new ColorToVerify(color);
			}
		}
	}
	
	// Call this to help detect a special kind of bug (student's fill command calling setTile method)
	public void addPrefillColor(Color color)
	{
		prefillColor = color;
	}
	
	public void verifyRemainingExpectedColors()
	{
		for (int row=0; row < getRows(); row++)
		{
			for (int col=0; col < getColumns(); col++)
			{
				ColorToVerify expectedColor = expectedColors[row][col];
				if (expectedColor != null && !expectedColor.verified)
				{
					GameOfLifeTest.golAssertTrue(
							"At least one cell in the grid was not painted correctly.  " +
									"core.API.paintSolidColor was never called for row " + row + ", column " +
									col + ".  Expected that cell to be painted with the color " + 
									ColorString.nameFromColor(expectedColor.color), false);
				}
			}
		}
	}

}
