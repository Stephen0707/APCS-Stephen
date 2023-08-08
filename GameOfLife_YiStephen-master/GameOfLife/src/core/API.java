//---------------------------------------------------------------------------------
//DO NOT MODIFY!!!
//---------------------------------------------------------------------------------

package core;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * 
 * Class of static-only methods for initializing and drawing
 * images and text into a grid, for generating sound effects,
 * and for accessing user input.  You can call any of these
 * public methods by typing <B>core.API.</B> followed by the
 * name of the method.
 * 
 * <P>You will typically call <B>core.API.initialize</B> early in your
 * main method to display the grid, and to prepare it for drawing
 * inside its cells.
 *
 */
public class API
{
    private static MediaManager mediaManager;
    private static boolean initialized = false;
    private static BiFunction<ObjectLandPanel, CustomAppearance, JPanel> panelGenerator = null;
    
    /**
     * Do not call; for internal use only
     */
    public static void useCustomFrame(BiFunction<ObjectLandPanel, CustomAppearance, JPanel> panelGeneratorP)
    {
    	// If just a single GameOfLife panel isn't good enough for you (like, in GameOfLife,
    	// where we want additional controls inside the window), call this early
    	// on, and pass it a method pointer that takes the ObjectLandPanel & CustomAppearance as
    	// parameters, and returns the panel you want.  (This panel would need to include the
    	// ObjectLandPanel as a child.)
    	panelGenerator = panelGeneratorP;
    }
    
    /**
     * Do not call; used for testing only.
     */
    public static void setCustomMediaManager(MediaManager mediaManagerP)
    {
        mediaManager = mediaManagerP;
    }
    
    /**
     * Do not call; used for testing only.
     */
    public static void disallowMultipleKeyCalls(String errorMessage)
    {
    	mediaManager.disallowMultipleKeyCalls(errorMessage);
    }

    /**
     * Do not call; used for testing only.
     */
    public static void allowMultipleKeyCalls()
    {
    	mediaManager.allowMultipleKeyCalls();
    }

    /**
     * Call initialize to display the grid with the specified number of rows and columns.
     * They will start off empty.  You MUST call one of the initialize overloads before
     * calling any other core.API methods.
     * This overload is for beginning users who want to display the
     * grid quickly and easily without customizing the appearance.
     * @param totalRows The number of rows to appear in the grid (height).
     * @param totalColumns The number of columns to appear in the grid (width).
     */
    public static void initialize(int totalRows, int totalColumns)
    {
        initialize(new CustomAppearance(totalRows, totalColumns));
    }
    
    /**
     * Call initialize to display the grid with the desired appearance.  You 
     * MUST call one of the initialize overloads before
     * calling any other core.API methods.  This overload is for
     * users who want to
     * customize the colors and sizes of the grid cells
     * and borders.
     * @param customAppearance An instance of the CustomAppearance class,
     * 		on which you have already called methods to alter the appearance
     * 		of the grid. 
     */
    public static void initialize(CustomAppearance customAppearance)
    {
        if (initialized)
        {
            throw new IllegalStateException("core.API.initialize was called more than once!  It may only be called once.");
        }
        
        initialized = true;

        ObjectLandPanel olPanel = null;
    	JPanel customPanel = null;
        
        if (mediaManager == null)
        {
        	// Media is not shimmed, so use the regular ObjectLandPanel
        	olPanel = new ObjectLandPanel();
        	mediaManager = olPanel;

        	// If a custom panel is requested, have the supplier generate it now
        	if (panelGenerator != null)
        	{
        		customPanel = panelGenerator.apply(olPanel, customAppearance);
        	}
        }
        
        // Do the actual ObjectLandPanel initialization (or test shim init)
        mediaManager.initialize(customAppearance, customPanel);
    }
    
    /**
     * Closes the grid window.  You do not need to call this unless you wish
     * to initialize more than one grid in your program.  In that case, you may initialize
     * and use grids one at a time, being sure to call close the prior grid before
     * initializing the next grid.
     */
    public static void close()
    {
        if (mediaManager != null)
        {
            mediaManager.close();
            mediaManager = null;
        }
        initialized = false;
    }
    
    /**
     * Returns the number of rows in the grid.
     * @return The number of rows in the grid.
     */
    public static int getRows()
    {
        ensureInitialized();
        return mediaManager.getRows();
    }

    /**
     * Returns the number of columns in the grid.
     * @return The number of columns in the grid.
     */
    public static int getColumns()
    {
        ensureInitialized();
        return mediaManager.getColumns();
    }
    
    /**
     * Draws the specified image at the given row and column.
     * @param row row of the cell in which to draw the image
     * @param column column of the cell in which to draw the image
     * @param imageFilename Filename, including extension, of the image file
     * to draw.  The file must first be copied to the images folder in your project.
     */
    public static void drawImage(int row, int column, String imageFilename)
    {
        ensureInitialized();
        mediaManager.drawImage(row, column, imageFilename);
    }
    
    /**
     * Makes the cell at the given row and column blank
     * @param row row of the cell to erase
     * @param column column of the cell to erase
     */
    public static void eraseImage(int row, int column)
    {
        ensureInitialized();
        mediaManager.eraseImage(row, column);
    }
    
    /**
     * Draws the specified text at the given row and column, with an automatically
     * chosen font point size so that the text will fit inside a single cell. 
     * @param row row of the cell in which to write the text
     * @param column column of the cell in which to write the text
     * @param text The text to write
     * @param color The color you want the text to show up in
     */
    public static void drawText(int row, int column, String text, Color color)
    {
        ensureInitialized();
        mediaManager.drawText(row, column, text, color);
    }
    
    /**
     * Draws the specified text at the given row and column, with a particular
     * font point size.  Typically you would use the other overload of this method
     * so that the point size is chosen for you automatically.  Only use this
     * if you want to choose your own point size for the font.
     * @param row row of the cell in which to write the text
     * @param column column of the cell in which to write the text
     * @param text The text to write
     * @param color The color you want the text to show up in
     * @param pointSize The font size to use when writing the text
     */
    public static void drawText(int row, int column, String text, Color color, int pointSize)
    {
        ensureInitialized();
        mediaManager.drawText(row, column, text, color, pointSize);
    }

    /**
     * Completely fills the specified row and column with the specified color.
     * @param row Row of the grid cell to paint
     * @param column Column of the grid cell to paint
     * @param color Color to paint the grid cell
     */
    public static void paintSolidColor(int row, int column, Color color)
    {
        ensureInitialized();
        mediaManager.paintSolidColor(row, column, color);
    }

    /**
     * Pauses for the specified number of milliseconds.  Useful for
     * slowing down or smoothing out animation
     * @param ms Number of milliseconds to pause
     */
    public static void pause(int ms)
    {
        ensureInitialized();
        mediaManager.pause(ms);
    }
    
    // Sound
    
    // Methods to play a single note or a chord (array of notes)

    /**
     * Plays the specified note through the MIDI engine.
     * @param note  An integer from 0 to 127, where 60 is middle C
     * @param duration How long to play the note in milliseconds 
     */
    public static void playNote(int note, int duration)
    {
        ensureInitialized();
        mediaManager.playNote(note, duration);
    }

    /**
     * Plays the specified "chord" (array of notes played simultaneously)
     * through the MIDI engine.
     * @param notes  An array of integers, each ranging from 0 to 127, where
     * 		60 is middle C.
     * @param duration How long to play the chord in milliseconds
     */
    public static void playChord(int[] notes, int duration)
    {
        ensureInitialized();
        mediaManager.playChord(notes, duration);
    }
    
    /**
     * Method to set the instrument to play through the MIDI engine
     * @param num a number from 0 to 127 that represents
     * the instrument to use
     */
    public static void setInstrument(int num)
    {
        ensureInitialized();
        mediaManager.setInstrument(num);
    }
    
    
    // User Input
    
    /**
     * Returns a String describing the key currently pressed, or
     * null if no key is currently pressed.  If a letter key is pressed,
     * this String will always be the upper-case version of that letter,
     * regardless of whether the SHIFT key is pressed (always "A" through "Z").
     * If a number key is pressed, this String will contain that number ("0"
     * through "9").  Other special keys are supported, such as arrow keys ("LEFT",
     * "UP", "RIGHT", "DOWN"), space bar ("SPACE"), "ENTER", "ESCAPE", and more.
     * Warning!  Calling this two times in a row will not necessarily return
     * the same String.  In fact, if this returns a non-null String, it is
     * likely to return null the next time you call it (unless the user has
     * quickly released and pressed another key in the meantime).  You should
     * therefore place the returned String into a variable if you expect
     * to use the String more than once. 
     * @return String describing the key currently pressed, or
     *      null if no key is currently pressed.
     */
    public static String getPressedKey()
    {
        ensureInitialized();
        return mediaManager.getPressedKey();
    }


    /**
     * Returns the row of the cell where the mouse is located.
     * @return Row number of the cell the mouse pointer is currently pointing at.
     * 		Returns -1 if the mouse is not somewhere over the grid. 
     */
    public static int getMouseRow()
    {
        ensureInitialized();
        return mediaManager.getMouseRow();
    }

    /**
     * Returns the column of the cell where the mouse is located.
     * @return Column number of the cell the mouse pointer is currently pointing at.
     * 		Returns -1 if the mouse is not somewhere over the grid. 
     */
    public static int getMouseColumn()
    {
        ensureInitialized();
        return mediaManager.getMouseColumn();
    }
    
    /**
     * Returns whether the left mouse button is currently pressed
     * @return true if the left mouse button is currently pressed; false otherwise
     */
    public static boolean isMouseLeftPressed()
    {
        ensureInitialized();
        return mediaManager.isMouseButtonPressed(MouseEvent.BUTTON1);
    }

    /**
     * Returns whether the right mouse button is currently pressed
     * @return true if the right mouse button is currently pressed; false otherwise
     */
    public static boolean isMouseRightPressed()
    {
        ensureInitialized();
        return mediaManager.isMouseButtonPressed(MouseEvent.BUTTON3);
    }

    // Private helper method to ensure you called core.API.initialize first
    private static void ensureInitialized()
    {
        if (!initialized)
        {
            throw new IllegalStateException("core.API.initialize must be called before any other core.API methods");
        }
    }
}