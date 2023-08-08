package coreGolGUI;


import java.awt.BorderLayout;
import javax.swing.JPanel;
import core.CustomAppearance;
import core.ObjectLandPanel;

// The big cheese around presenting the GameOfLife with GUI controls.  This
// panel contains HeaderPanel (with controls) and ObjectLandPanel (the grid).
// It also establishes the input demuxer to combine GUI commands and
// Scanner console commands, and establishes a dedicated mouse listener.
public class GameOfLifePanel extends JPanel
{
	// ----------------------------------------------------------------------
	// STATICS
	// ----------------------------------------------------------------------
	
    // Need one of these to avoid serializable class w/out UUID warning
	private static final long serialVersionUID = 8353462461769949516L;
	
	private static boolean testMode = false;
	private static InputDemux inputDemux = null;
	
	/**
	 * Do not call; for internal testing purposes only
	 */
	public static void enableTestMode()
	{
		testMode = true;
	}

	/**
	 * Call this first thing in main to enable the extra GameOfLife controls inside the
	 * frame housing the ObjectLandPanel grid.
	 */
	public static void useGameOfLifeControls()
	{
		// We don't want any of this stdin demux nonsense gumming up the works
		// when automated tests run, so refuse to any GUI GoL customizations then.
		if (!testMode)
		{
			inputDemux = new InputDemux();
			core.API.useCustomFrame(GameOfLifePanel::createGameOfLifeFrame);
		}
	}
	
	// Callback passed to core.API to create and initialize a GameOfLifePanel
	// at the right time
	private static JPanel createGameOfLifeFrame(ObjectLandPanel olPanel, CustomAppearance ca)
	{
		return new GameOfLifePanel(olPanel, ca);
	}
	
	// ----------------------------------------------------------------------
	// INSTANCE
	// ----------------------------------------------------------------------
	
	private HeaderPanel headerPanel;
	private ObjectLandPanel olPanel;
	
	public GameOfLifePanel(ObjectLandPanel olPanelP, CustomAppearance ca)
	{
		headerPanel = new HeaderPanel(this);
		olPanel = olPanelP;
		
		// If student didn't customize window title away from
		// the Object Land default, set it to Game of Life now
		if (ca.getWindowTitle().equals("Object Land"))
		{
			ca.setWindowTitle("Game of Life");
		}
		
		new MouseListener(this, olPanel);
		setLayout(new BorderLayout());
		add(headerPanel, BorderLayout.PAGE_START);
		add(olPanel, BorderLayout.CENTER);
		setVisible(true);
	}
	
	public void sendSetCommand()
	{
		int row = olPanel.getMouseRow();
		int col = olPanel.getMouseColumn();
		if (row < 0 || row >= olPanel.getRows() || col < 0 || col >= olPanel.getColumns())
		{
			return;
		}
		
		inputDemux.sendSupplementalCommand(
				"set " +
						row + " " + col + " " +
						headerPanel.getTileType() + " " +
						headerPanel.getTileAge()  + " ");
	}
	
	public void sendFillCommand()
	{
		inputDemux.sendSupplementalCommand(
				"fill " +
						headerPanel.getTileType() + " " +
						headerPanel.getTileAge());
	}

	
	public void sendEvolveCommand()
	{
		inputDemux.sendSupplementalCommand(
				"evolve " + headerPanel.getEvolveSteps() + " " + headerPanel.getEvolvePause());
	}
}
