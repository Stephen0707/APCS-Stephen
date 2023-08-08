//---------------------------------------------------------------------------------
//DO NOT MODIFY!!!
//---------------------------------------------------------------------------------

package core;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

public class ObjectLandPanel extends JPanel implements MediaManager, ComponentListener 
{
    private boolean crashed;

    private int rows;
    private int cols;

    private Color cellColor;
    private Color borderColor;

    private int cellWidth;
    private int cellHeight;
    private int borderThicknessX;
    private int borderThicknessY;
    private HashMap<String, BufferedImage> imageCache;
    private Cell[][] cells;

    private CustomAppearance customAppearance;

    private MidiPlayer midiPlayer;
    private UserInputManager inputManager;

    // Need one of these to avoid serializable class w/out UUID warning
    private static final long serialVersionUID = -5688934251383342526L;

    public ObjectLandPanel()
    {
        // All initialization occurs in the interface implementation of initialize
    }

    @Override
    public void initialize(CustomAppearance customAppearanceP, JPanel customPanel)
    {
    	addComponentListener(this);
        crashed = false;
        customAppearance = customAppearanceP;

        // Grab window location before we have a window, so
        // we're assured of getting what the user set, if anything
        int windowLocationX = customAppearance.getWindowLocationX();
        int windowLocationY = customAppearance.getWindowLocationY();

        rows = customAppearance.getRows();
        cols = customAppearance.getColumns();
        imageCache = new HashMap<String, BufferedImage>();
        cells = new Cell[rows][cols];

        // Grabs requested dimensions & borders, and sets a preferred initial
        // size for the grid so the cells are initially square
        initAppearance();
        
        customAppearance.setMediaManager(this);
        String windowTitle = customAppearance.getWindowTitle();
        if (windowTitle == null)
        {
        	windowTitle = "Object Land";
        }
        JFrame frame = new JFrame(windowTitle);
        
        // If we're passed a customPanel (GameOfLife), that will be the main panel for the frame
        // (and it should contain this as a sub-panel).  Else use this as the main panel
        frame.add((customPanel == null) ? this : customPanel);
        
        makeMenuBar(frame);
        
        frame.pack();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);

        // This weird stuff ensures window is brought to front, even though
        // focus was in Eclipse console window before it was created
        frame.setExtendedState(JFrame.ICONIFIED);
        frame.setExtendedState(JFrame.NORMAL);

        // Now we can ensure window is initially placed where user wants it
        if (windowLocationX != -1 && windowLocationY != -1)
        {
        	frame.setLocation(windowLocationX, windowLocationY);
        }

        midiPlayer = new MidiPlayer();
        inputManager = new UserInputManager(this);
        frame.setAlwaysOnTop(customAppearance.getKeepOnTop());
    }
    
    private void makeMenuBar(JFrame frame)
    {
        JMenuBar menuBar = new JMenuBar();
        
        // Window (menu)
        JMenu windowMenu = new JMenu("Window");
        windowMenu.setMnemonic(KeyEvent.VK_W);
        windowMenu.getAccessibleContext().setAccessibleDescription("This menu contains window options");
        menuBar.add(windowMenu);
        
        // Keep on top (item)
        JCheckBoxMenuItem keepTopMenuItem = new JCheckBoxMenuItem("Keep on Top");
        keepTopMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        keepTopMenuItem.getAccessibleContext().setAccessibleDescription("Keeps this window on top of other windows");
        keepTopMenuItem.setSelected(ObjectLandPanel.this.customAppearance.getKeepOnTop());
        keepTopMenuItem.addActionListener(new ActionListener() 
        {
            @Override
            public void actionPerformed(ActionEvent arg0) 
            {
                ObjectLandPanel.this.customAppearance.setKeepOnTop(keepTopMenuItem.isSelected());
            }
        });
        
        // Options (item)
        windowMenu.add(keepTopMenuItem);
        JMenuItem optionsMenuItem = new JMenuItem("Options...", KeyEvent.VK_O);
        optionsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        optionsMenuItem.getAccessibleContext().setAccessibleDescription("Brings up custom appearance options dialog");
        optionsMenuItem.addActionListener(new ActionListener() 
        {
            @Override
            public void actionPerformed(ActionEvent arg0) 
            {
                WindowOptionsDialogManager dlgMgr = new WindowOptionsDialogManager(getFrame(), customAppearance);
                dlgMgr.show();
            }
        });
        windowMenu.add(optionsMenuItem);
        
        frame.setJMenuBar(menuBar);
    }    

    @Override
    public void close()
    {
        inputManager.close();
        midiPlayer.close();
        getFrame().dispose();
    }    

    public int getRows()
    {
        return rows;
    }

    public int getColumns()
    {
        return cols;
    }

    private int getTotalWidth()
    {
        return (cols *  cellWidth) + (cols * borderThicknessX * 2);
    }

    private int getTotalHeight()
    {
        return (rows *  cellHeight) + (rows * borderThicknessY * 2);
    }

    // Entire width, including border for each cell
    private int getCellWidth()
    {
        return cellWidth + (borderThicknessX * 2);
    }

    // Entire height, including border for each cell
    private int getCellHeight()
    {
        return cellHeight + (borderThicknessY * 2);
    }

    // Returns (x,y) coordinates for upper-left corner of cell
    // (where border would start to be drawn)
    private Point getCellCoordinates(int row, int col)
    {
        return new Point(col * getCellWidth(), row * getCellHeight());
    }

    // Returns row, col as ([0], [1]) of cell containing pos
    private int[] getCellRowCol(Point pos)
    {
        int[] ret = new int[2];

        ret[0] = pos.y / getCellHeight();
        ret[1] = pos.x / getCellWidth();
        
        // It's occasionally possible to get out of bounds
        // results if the window is getting resized while the mouse
        // events are processed.  Protect the caller
        
        if (ret[0] >= rows)
        {
        	ret[0] = rows - 1;
        }
        
        if (ret[1] >= cols)
        {
        	ret[1] = cols - 1;
        }

        return ret;
    }

    public void drawGrid()
    {
        invalidate();
        repaint();
        JFrame frame = getFrame();
        if (frame != null)
        {
        	frame.setAlwaysOnTop(customAppearance.getKeepOnTop());
        }
    }


    public void paint(Graphics g) 
    {
        Graphics2D g2d = (Graphics2D) g;
        for (int row = 0; row < rows; row++)
        {
            for (int col = 0; col < cols; col++)
            {
                paintCell(g2d, row, col);
            }
        }

        if (crashed)
        {
            g.setColor(Color.GREEN);
            g.drawLine(0, 0, getTotalWidth(), getTotalHeight());
            g.drawLine(getTotalWidth(), 0, 0, getTotalHeight());
            g.setFont(new Font(Font.DIALOG, Font.BOLD, 30));
            g.drawString("   Yay, you crashed me!  See Eclipse console for details.", 50, 50);
        }
    }	

    private void paintCell(Graphics2D g, int row, int col) 
    {
        Point cellCoordinates = getCellCoordinates(row, col);

        // Borders first!
        g.setColor(borderColor);

        // Top border
        g.fillRect(cellCoordinates.x, cellCoordinates.y, 
                (2 * borderThicknessX) + cellWidth, borderThicknessY);

        // Left border
        g.fillRect(cellCoordinates.x, cellCoordinates.y, 
                borderThicknessX, (2 * borderThicknessY) + cellHeight);

        // Right border
        g.fillRect(cellCoordinates.x + getCellWidth() - borderThicknessX, cellCoordinates.y, 
                borderThicknessX, (2 * borderThicknessY) + cellHeight);

        // Bottom border
        g.fillRect(cellCoordinates.x, cellCoordinates.y + getCellHeight() - borderThicknessY, 
                (2 * borderThicknessX) + cellWidth, borderThicknessY);

        // Cell background
        g.setColor(cellColor);
        g.fillRect(cellCoordinates.x + borderThicknessX, 
                cellCoordinates.y + borderThicknessY, cellWidth, cellHeight);

        // Cell content
        Cell cell = cells[row][col];
        if (cell != null)
        {
            cell.draw(g, cellCoordinates.x + borderThicknessX, cellCoordinates.y + borderThicknessY, cellWidth, cellHeight);
        }
    }

    private void ensureRowColValid(int row, int col)
    {
        if (row < 0 || row >= rows || col < 0 || col >= cols)
        {
            crashed = true;
            paintImmediately(0, 0, getTotalWidth(), getTotalHeight());
            throw new IllegalArgumentException(
                    "The (row, col) specified was (" + row + ", " + col + "), which is out of bounds.\n" +
                            "This grid only allows rows in the range 0 through " + (rows - 1) + " inclusive,\n" +
                            "and columns in the range 0 through " + (cols - 1) + " inclusive.\n" +
                            "Either change the row & col you are passing, or create a different-sized grid by \n" +
                    "changing the parameters you are passing to the core.API.initialize method.");
        }
    }

    public void drawImage(int row, int col, String imageFilename)
    {
        ensureRowColValid(row, col);

        BufferedImage img = null;

        // Image could exist in a few places.  Try them one by one.

        // 0: If it's in our cache, that would be super
        img = imageCache.get(imageFilename);
        
    	if (img == null)
    	{
    		try
    		{
    			// Try all the other places via this static helper
    			img = loadImageFromFilename(imageFilename);
    		}
			catch (FileSystemNotFoundException e)
			{
				crashed = true;
				paintImmediately(0, 0, getTotalWidth(), getTotalHeight());
				throw e;
			}
    		
    		// Cache was missed, so add image to cache now
            imageCache.put(imageFilename, img);
        }

        cells[row][col] = new ImageCell(img);
        drawGrid();    
    }
    
    public static File getImagesFolder()
    {
    	// This is where images folder will be when run from Eclipse
    	File ret = new File("bin/images");
    	if (ret.exists())
    	{
    		return ret;
    	}
    	
    	// This is where images folder will be when run from buildci
    	ret = new File("src/images");
    	if (ret.exists())
    	{
    		return ret;
    	}
    	
    	throw new FileSystemNotFoundException(
    			"Cannot find images folder under src or bin.  Did you delete your images folder?");
    }
    
    public static BufferedImage loadImageFromFilename(String imageFilename)
    {
    	BufferedImage img = null;
    	File imagesFolder = getImagesFolder();

    	// 1: First, does it exist as its own file under the images folder?
    	File file = new File(imagesFolder, imageFilename);
		if (file.exists())
    	{
    		try
    		{
    			img = ImageIO.read(file);
    		}
    		catch (IOException e)
    		{
    			throw new FileSystemNotFoundException(
    					"Unable to open image file " + file.getAbsolutePath() + "\n" + e);
    		}
    	}
    	else
    	{
    		// 2: Does it exist inside a JAR file?  (Useful if running as a JAR program.)
    		InputStream is = ObjectLandPanel.class.getResourceAsStream("/images/" + imageFilename);
    		if (is != null)
    		{
    			try
    			{
    				img = ImageIO.read(is);
    			}
    			catch (IOException e)
    			{
    				throw new FileSystemNotFoundException(
    						"Unable to open image resource file /images/" + imageFilename + "\n" + e);
    			}
    		}
    	}

    	// 3: Does it exist inside the ZippedImages.zip file in the images folder?
    	if (img == null)
    	{
    		img = getBufferedImageFromZip(imagesFolder, "ZippedImages.zip", imageFilename);
    	}

        // Out of ideas, let's give up
        if (img == null)
        {
            throw new FileSystemNotFoundException(
                    "Unable to open image file " + file.getAbsolutePath() + "\n");
        }
        
        return img;
    }

    private static BufferedImage getBufferedImageFromZip(File imagesFolder, String zipFilePath, String imageFilename)
    {
        ZipFile zipFile = null;
        File zipFileObj = new File(imagesFolder, zipFilePath);

        try
        {
            zipFile = new ZipFile(zipFileObj);
        }
        catch (IOException e)
        {
            throw new FileSystemNotFoundException(
                    "Unable to open '" + zipFileObj.getAbsolutePath() + "'\n" + e);
        }

        ZipEntry ze = getEntryFromZipFileCaseInsensitive(zipFile, imageFilename);
        if (ze == null)
        {
            throw new FileSystemNotFoundException(
                    "Unable to find image file '" + imageFilename + "' under the images folder or inside " + zipFileObj.getAbsolutePath() + "\n");
        }

        InputStream is = null;
        try
        {
            is = zipFile.getInputStream(ze);
        }
        catch (IOException e)
        {
            throw new FileSystemNotFoundException(
                    "Unable to open image file '" + imageFilename + "' from " + zipFileObj.getAbsolutePath() + "\n" + e);
        }

        BufferedImage img = null;
        try
        {
            img = ImageIO.read(is);
        }
        catch (IOException e)
        {
            throw new FileSystemNotFoundException(
                    "Unable to read image file '" + imageFilename + "' from " + zipFileObj.getAbsolutePath() + "\n" + e);
        }

        try
        {
            zipFile.close();
        }
        catch (IOException e)
        {
        }

        return img;
    }	    

    private static ZipEntry getEntryFromZipFileCaseInsensitive(ZipFile zipFile, String imageFilename)
    {
        // Do it the simple way, in case the casing is already correct
        ZipEntry ze = zipFile.getEntry(imageFilename);
        if (ze != null)
        {
            return ze;
        }

        // Iterate and check each one
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements())
        {
            ze = entries.nextElement();
            if (ze.getName().equalsIgnoreCase(imageFilename))
            {
                return ze;
            }
        }

        return null;
    }

    public void eraseImage(int row, int col)
    {
        ensureRowColValid(row, col);

        cells[row][col] = null;
        drawGrid();    
    }

    public void drawText(int row, int col, String text, Color color)
    {
        ensureRowColValid(row, col);

        cells[row][col] = new TextCell(text, color);
        drawGrid();
    }

    public void drawText(int row, int col, String text, Color color, int pointSize)
    {
        ensureRowColValid(row, col);

        cells[row][col] = new TextCell(text, color, pointSize);
        drawGrid();
    }

    public void paintSolidColor(int row, int col, Color color)
    {
        ensureRowColValid(row, col);

        cells[row][col] = new SolidCell(color);
        drawGrid();
    }

    public int getMouseRow()
    {
        Point pos = this.getMousePosition();
        if (pos == null)
        {
            return -1;
        }
        return getCellRowCol(pos)[0];
    }

    public int getMouseColumn()
    {
        Point pos = this.getMousePosition();
        if (pos == null)
        {
            return -1;
        }
        return getCellRowCol(pos)[1];
    }


    @Override
    public boolean isMouseButtonPressed(int button)
    {
        return inputManager.isMouseButtonPressed(button);
    }

    // Cell types

    private interface Cell
    {
        public void draw(Graphics2D g, int x, int y, int width, int height);
    }

    private class ImageCell implements Cell
    {
        private BufferedImage image;

        public ImageCell(BufferedImage imageP)
        {
            image = imageP;
        }

        public void draw(Graphics2D g, int x, int y, int width, int height)
        {
            // Image cell
            AffineTransform xform = new AffineTransform(
                    ((double) width) / image.getWidth(), 0, 
                    0, ((double) height) / image.getHeight(),
                    x, y);

            g.drawImage(image, xform, null /* listener */);            
        }
    }

    private class TextCell implements Cell
    {
        private String text;
        private Color color;
        private int pointSize;

        public TextCell(String textP, Color colorP)
        {
            this(textP, colorP, -1);
        }

        public TextCell(String textP, Color colorP, int pointSizeP)
        {
            text = textP;
            color = colorP;
            pointSize = pointSizeP;
        }

        public void draw(Graphics2D g, int x, int y, int width, int height)
        {
            int pointSizeToUse = (pointSize == -1) ? (int) (height * 0.5) : pointSize;
            Font font = new Font(Font.DIALOG, Font.BOLD, pointSizeToUse);
            FontRenderContext frc = g.getFontRenderContext();
            g.setColor(color);

            String[] textLines = text.split("\n");

            // Adjust so y is at bottom of cell.  Use TextLayout for the letter "A",
            // plus the total number of lines of text in order to get an estimate
            // of the vertical size of the text
            TextLayout layoutForVerticalCentering = new TextLayout("A", font, frc);
            Rectangle2D boundsForVerticalCentering = layoutForVerticalCentering.getBounds();
            double heightOfText = boundsForVerticalCentering.getHeight() * (textLines.length - 1);
            double remainingCellHeight = height - heightOfText;
            y = (int) ((y + height) - (remainingCellHeight / 2) - heightOfText + (boundsForVerticalCentering.getHeight() / 2));

            // For each line of text, figure out its horizontal length to center it
            // on its line within the cell.
            for (int lineNum=0; lineNum < textLines.length; lineNum++)
            {
                String textLine = textLines[lineNum];
                if (textLine.isEmpty())
                {
                    continue;
                }
                TextLayout layout = new TextLayout(textLine, font, frc);
                Rectangle2D bounds = layout.getBounds();
                int yToUse = (int) (y + (lineNum * bounds.getHeight()));

                layout.draw(g, x + (width - (float) bounds.getWidth()) / 2, yToUse);
            }
        }
    }

    private class SolidCell implements Cell
    {
        private Color color;

        public SolidCell(Color colorP)
        {
            color = colorP;
        }

        public void draw(Graphics2D g, int x, int y, int width, int height)
        {
            g.setColor(color);
            g.fillRect(x, y, width, height);
        }
    }

    @Override
    public void pause(int ms)
    {
        try
        {
            Thread.sleep(ms);
        }
        catch(InterruptedException e)
        {

        }
    }

    @Override
    public void playNote(int note, int duration)
    {
        midiPlayer.playNote(note, duration);
    }

    @Override
    public void playChord(int[] notes, int duration)
    {
        midiPlayer.playChord(notes, duration, 64);
    }
    
    @Override
    public void setInstrument(int num)
    {
        midiPlayer.setInstrument(num);
    }

    @Override
    public String getPressedKey()
    {
        return inputManager.getPressedKey();
    }

    @Override
    public void updateGridAppearance() 
    {
		initAppearance(getWidth(), getHeight());
    }
    
    @Override
    public void updateTitle()
    {
    	JFrame frame = getFrame();
    	if (frame != null)
    	{
    		frame.setTitle(customAppearance.getWindowTitle());
    	}
    }
    
    private JFrame getFrame()
    {
    	return (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, this);
    }

    // Called once, during initialization
    private void initAppearance()
    {
    	// Finagle the initial width & height parameters to our overload
    	// so that the cells end up being square.  If user resizes the frame later,
    	// we have no choice but to make the cells rectangular at that point
        int largerCellCount = Math.max(rows,  cols);
        int targetLength = customAppearance.getTargetLength();
        double fullCellSize = ((double) targetLength) / largerCellCount;
        int initialWidth = (int) (fullCellSize * cols);
        int initialHeight = (int) (fullCellSize * rows);

        // Do the actual appearance init
        initAppearance(initialWidth, initialHeight);
        
        // Tell our parent this how big we want to be now
        Dimension dimension = new Dimension(initialWidth, initialHeight);
        setPreferredSize(dimension);
    }
    
    // Called during initialization, and any time the size or CustomAppearance changes
    private void initAppearance(int width, int height)
    {
        borderThicknessX = customAppearance.getBorderThicknessX();
        borderThicknessY = customAppearance.getBorderThicknessY();
        
        int totalBorderX = borderThicknessX * 2 * cols; 
        int totalBorderY = borderThicknessY * 2 * rows; 
        
        cellWidth = (int) (((double) width - totalBorderX) / cols); 
        cellHeight = (int) (((double) height - totalBorderY) / rows); 
        
        cellColor = customAppearance.getCellColor();
        borderColor = customAppearance.getBorderColor();
        
        drawGrid();
    }
    
	@Override
	public void disallowMultipleKeyCalls(String errorMessage)
	{
		if (inputManager != null)
		{
			inputManager.disallowMultipleKeyCalls(errorMessage);
		}
	}

	@Override
	public void allowMultipleKeyCalls()
	{
		if (inputManager != null)
		{
			inputManager.allowMultipleKeyCalls();
		}
	}
    

	@Override
	public int getWindowLocationX()
	{
		JFrame frame = getFrame();
		if (frame == null)
		{
			return -1;
		}
		return (int) frame.getLocation().getX();
	}

	@Override
	public int getWindowLocationY()
	{
		JFrame frame = getFrame();
		if (frame == null)
		{
			return -1;
		}
		return (int) frame.getLocation().getY();
	}

	@Override
	public void updateWindowLocationX(int x)
	{
		getFrame().setLocation(x, getWindowLocationY());
	}

	@Override
	public void updateWindowLocationY(int y)
	{
		getFrame().setLocation(getWindowLocationX(), y);
	}
	
	// ComponentListener methods.  Only care about resizing

	@Override
	public void componentResized(ComponentEvent arg0)
	{
		initAppearance(getWidth(), getHeight());
	}

	@Override
	public void componentHidden(ComponentEvent arg0)
	{
	}

	@Override
	public void componentMoved(ComponentEvent arg0)
	{
	}

	@Override
	public void componentShown(ComponentEvent arg0)
	{
	}
}