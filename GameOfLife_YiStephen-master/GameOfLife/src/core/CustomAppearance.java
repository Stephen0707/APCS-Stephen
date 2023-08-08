//---------------------------------------------------------------------------------
//DO NOT MODIFY!!!
//---------------------------------------------------------------------------------

package core;

import java.awt.Color;

/**
 * Create an instance of this class to customize the appearance of the
 * grid, including its size, colors, and thickness of the cell borders.
 * You can then pass that instance to the proper overload of
 * <B>core.API.initialize</B>.  If you continue to use the instance
 * you passed to core.API.initialize, by further changing
 * the colors, sizes, etc., then those changes will immediately appear
 * on the grid you previously initialized.
 *
 */

public class CustomAppearance
{
    private Color cellColor;
    private Color borderColor;

    private int targetLength;
    
    private int borderThicknessX;
    private int borderThicknessY;
    
    private int rows;
    private int cols;
    
    private String windowTitle;
    private int lastRequestedWindowLocationX;
    private int lastRequestedWindowLocationY;
    private boolean keepOnTop;
    
    private MediaManager mediaManager;
    private boolean isMidiOnly;

    /**
     * Creates a new instance of CustomAppearance with the specified
     * number of rows and columns you want to appear in the grid.  All other
     * appearance characteristics will be set to their default values until
     * you change them by calling the appropriate set methods.
     * @param totalRows The number of rows to appear in the grid (height). 
     * @param totalColumns The number of columns to appear in the grid (width).
     */
    public CustomAppearance(int totalRows, int totalColumns)
    {
        cellColor = new Color(2, 22, 91);
        borderColor = Color.YELLOW;
        targetLength = 600;
        borderThicknessX = 1;
        borderThicknessY = 1;
        lastRequestedWindowLocationX = -1;
        lastRequestedWindowLocationY = -1;
        keepOnTop = false;
        rows = totalRows;
        cols = totalColumns;
        windowTitle = "Object Land";
        mediaManager = null;
        isMidiOnly = false;
    }
    
    /**
     * For internal use only.  Do not call.
     */
    public void setMediaManager(MediaManager mediaManagerP)
    {
    	mediaManager = mediaManagerP;
    }
    
    /**
     * Changes the background color of all the
     * squares to the specified color.
     * If you do not call this method, the background color is dark blue by default.
     * @param cellColorP The color for the
     * background of all squares in the grid.
     */
    public void setCellColor(Color cellColorP)
    {
        cellColor = cellColorP;
        updateGridAppearance();
    }
    
    /**
     * Changes the color of the criss-cross border
     * that separates all the squares in the grid.
     * If you do not call this method, the border color is yellow by default. 
     * @param borderColorP  The color for the border
     * that separates all the squares in the grid.
     */
    public void setBorderColor(Color borderColorP)
    {
        borderColor = borderColorP;
        updateGridAppearance();
    }
    
    /**
     * Changes the size of the overall grid.
     * If you do not call this method, the target length is 800 by default. 
     * @param targetLengthP An approximate length
     * for the grid.  This value is used in conjunction
     * with the the aspect ratio of the grid (based
     * on the number of rows and columns) to determine
     * the actual height and width of the grid. 
     */
    public void setTargetLength(int targetLengthP)
    {
        targetLength = targetLengthP;
        updateGridAppearance();
    }
    
    /**
     * Changes the horizontal thickness of the border
     * that separates all the squares in the grid.
     * If you do not call this method, the horizontal border thickness is 1 by default. 
     * @param borderThicknessXP The new horizontal
     * thickness for the border.
     */
    public void setBorderThicknessX(int borderThicknessXP)
    {
        borderThicknessX = borderThicknessXP;
        updateGridAppearance();
    }

    /**
     * Changes the vertical thickness of the border
     * that separates all the squares in the grid.
     * If you do not call this method, the vertical border thickness is 1 by default. 
     * @param borderThicknessYP The new vertical
     * thickness for the border.
     */
    public void setBorderThicknessY(int borderThicknessYP)
    {
        borderThicknessY= borderThicknessYP;
        updateGridAppearance();
    }

    /**
     * Moves the main window to the specified screen X coordinate 
     * @param windowLocationX New screen X-coordinate of the main window's desired location
     */
    public void setWindowLocationX(int windowLocationX)
    {
    	lastRequestedWindowLocationX = windowLocationX;
    	if (mediaManager != null)
    	{
    		mediaManager.updateWindowLocationX(lastRequestedWindowLocationX);
    	}
    }
    
    /**
     * Moves the main window to the specified screen Y coordinate 
     * @param windowLocationY New screen Y-coordinate of the main window's desired location
     */
    public void setWindowLocationY(int windowLocationY)
    {
    	lastRequestedWindowLocationY = windowLocationY;
    	if (mediaManager != null)
    	{
    		mediaManager.updateWindowLocationY(lastRequestedWindowLocationY);
    	}
    }
    
    /**
     * Changes whether the main window should always remain on top of other windows, even
     * when it is not active.
     * @param keepOnTopP true to keep this window on top of other windows at all times, false otherwise.
     */
    public void setKeepOnTop(boolean keepOnTopP)
    {
    	keepOnTop = keepOnTopP;
        updateGridAppearance();
    }
    
    /**
     * Changes the main window's title text
     * @param windowTitleP The new title text to display at the top of the main window. 
     */
    public void setWindowTitle(String windowTitleP)
    {
        windowTitle = windowTitleP;
        if (mediaManager != null)
        {
        	mediaManager.updateTitle();
        }
    }
    
    /**
     * For internal use only.  Do not call.
     */
    public void setMidiOnly(boolean b)
    {
        isMidiOnly = b;
    }
    
    /**
     * Gets the current background color of all the
     * squares.
     * @return The current background color of all the
     * squares
     */
    public Color getCellColor()
    {
        return cellColor;
    }
    
    /**
     * Gets the color of the criss-cross border
     * that separates all the squares in the grid.
     * @return The color of the criss-cross border
     * that separates all the squares in the grid.
     */
    public Color getBorderColor()
    {
        return borderColor;
    }
    
    /**
     * Gets the approximate length of the grid that was
     * used to determine its actual width and height.
     * This value is used in conjunction
     * with the the aspect ratio of the grid (based
     * on the number of rows and columns) to determine
     * the actual height and width of the grid. 
     * @return The approximate length of the grid that was
     * used to determine its actual width and height.
     */
    public int getTargetLength()
    {
        return targetLength;
    }
    
    /**
     * Gets the horizontal thickness of the border
     * that separates all the squares in the grid.
     * @return The horizontal thickness of the border
     * that separates all the squares in the grid.
     */
    public int getBorderThicknessX()
    {
        return borderThicknessX;
    }
    
    /**
     * Gets the vertical thickness of the border
     * that separates all the squares in the grid.
     * @return The vertical thickness of the border
     * that separates all the squares in the grid.
     */
    public int getBorderThicknessY()
    {
        return borderThicknessY;
    }
    
    /**
     * Gets the screen X-coordinate of the main window's current location.  This might be
     * a different value than was passed to setWindowLocationX earlier if the user
     * moved the window.
     * @return screen X-coordinate of the main window's current location.
     */
    public int getWindowLocationX()
    {
    	if (mediaManager == null)
    	{
    		return lastRequestedWindowLocationX;
    	}
    	return mediaManager.getWindowLocationX();
    }
    
    /**
     * Gets the screen Y-coordinate of the main window's current location.  This might be
     * a different value than was passed to setWindowLocationY earlier if the user
     * moved the window.
     * @return screen Y-coordinate of the main window's current location.
     */
    public int getWindowLocationY()
    {
    	if (mediaManager == null)
    	{
    		return lastRequestedWindowLocationY;
    	}
    	return mediaManager.getWindowLocationY();
    }
    
    /**
     * Gets a boolean indicating whether the main window should always remain on top of other windows
     * @return true if the main window should always be on top, false otherwise.
     */
    public boolean getKeepOnTop()
    {
    	return keepOnTop;
    }    
    
    /**
     * Gets the number of rows in the grid.
     * @return The number of rows in the grid.
     */
    public int getRows()
    {
        return rows;
    }
    
    /**
     * Gets the number of columns in the grid.
     * @return The number of columns in the grid.
     */
    public int getColumns()
    {
        return cols;
    }
    
    /**
     * Gets the text to use for the main window's title
     * @return The text to use for the main window's title
     */
    public String getWindowTitle()
    {
        return windowTitle;
    }
    
    /**
     * For internal use only.  Do not call.
     */
    public boolean isMidiOnly()
    {
        return isMidiOnly;
    }
    
    // All mutators call this so that appearance changes are propagated
    // to the grid so they are visible immediately
    private void updateGridAppearance()
    {
    	if (mediaManager != null)
    	{
    		mediaManager.updateGridAppearance();
    	}
    }
}
