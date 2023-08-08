package test;

import static org.junit.Assert.assertTrue;
import java.awt.Color;
import java.nio.file.FileSystemNotFoundException;

import javax.swing.JPanel;

import core.CustomAppearance;
import core.MediaManager;

// Super-class for other potential MediaManager implementations used by
// the tests.  This just provides a default implementation of the MediaManager
// methods, which mostly do nothing but rudimentary range validation checks.

public class TestMediaShim implements MediaManager
{
	private int rows;
	private int cols;
	private boolean initialized = false;

	@Override
	public void initialize(CustomAppearance appearance, JPanel customPanel)
	{
		rows = appearance.getRows();
		cols = appearance.getColumns();
		initialized = true;
	}

	@Override
	public int getRows()
	{
		return rows;
	}

	@Override
	public int getColumns()
	{
		return cols;
	}


	@Override
	public void drawImage(int row, int column, String imageFilename)
	{
		verifyRowColRange(row, column, "drawImage");
		
		// Try loading image just to verify it actually exists; else an exception
		// will be thrown and the test will fail.  This test
		// shim won't actually try to draw the image anywhere
		System.out.println("Attempting to load '" + imageFilename + "'...");
		try
		{
			core.ObjectLandPanel.loadImageFromFilename(imageFilename);
		}
		catch (FileSystemNotFoundException e)
		{
			System.out.println("ERROR!  Could not find the image file.  Please ensure that you add ");
			System.out.println("any custom image files directly inside the images folder, alongside ");
			System.out.println("frowny.png and smiley.png.  Do NOT add images inside ZippedImages.zip.");
			throw e;
		}
	}

	@Override
	public void eraseImage(int row, int column)
	{
		verifyRowColRange(row, column, "eraseImage");
	}

	@Override
	public void drawText(int row, int column, String text, Color color)
	{
		verifyRowColRange(row, column, "drawText");
	}

	@Override
	public void drawText(int row, int column, String text, Color color,
			int pointSize)
	{
		verifyRowColRange(row, column, "drawText");
	}

	@Override
	public void pause(int ms)
	{

	}


	@Override
	public void playNote(int note, int duration)
	{
	}

	@Override
	public void playChord(int[] notes, int duration)
	{
	}

	@Override
	public String getPressedKey()
	{
		return null;
	}

	@Override
	public int getMouseRow()
	{
		return -1;
	}

	@Override
	public int getMouseColumn()
	{
		return -1;
	}

	private void verifyRowColRange(int row, int column, String methodName)
	{
		assertTrue("The (row, col) specified to " + methodName + " was (" + row + ", " + column + "), which is out of bounds.\n" +
				"This grid only allows rows in the range 0 through " + (rows - 1) + " inclusive,\n" +
				"and columns in the range 0 through " + (cols - 1) + " inclusive.\n" +
				"Either change the row & col you are passing, or create a different-sized grid by \n" +
				"changing the parameters you are passing to the core.API.initialize method.",
				(0 <= row && row < rows && 0 <= column && column < cols));
	}

	@Override
	public void close()
	{
		rows = 0;
		cols = 0;
	}

	@Override
	public void updateGridAppearance() 
	{
	}

	@Override
	public void updateTitle()
	{
	}
	
	@Override
	public void paintSolidColor(int row, int column, Color color)
	{
		verifyRowColRange(row, column, "paintSolidColor");
	}

	@Override
	public void setInstrument(int num)
	{
	}

	@Override
	public boolean isMouseButtonPressed(int button)
	{
		return false;
	}

	public boolean getInitialized()
	{
		return initialized;
	}

	@Override
	public int getWindowLocationX()
	{
		return 0;
	}

	@Override
	public int getWindowLocationY()
	{
		return 0;
	}

	@Override
	public void updateWindowLocationX(int x)
	{
	}

	@Override
	public void updateWindowLocationY(int y)
	{
	}

	@Override
	public void disallowMultipleKeyCalls(String errorMessage)
	{
	}

	@Override
	public void allowMultipleKeyCalls()
	{
	}
}
