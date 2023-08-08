package gameOfLife;

public class Main
{
	/**
	 * This method asks the user to enter, first, the number of
	 * rows (as its own line), and second,
	 * the number of columns (as its own line), that the grid should contain.
	 * It then creates an instance of the World class with these
	 * dimensions.  It will then prompt the user for commands, one by one,
	 * until the user types "quit", at which point it will return the World
	 * instance it created.  Each command the user enters should be passed
	 * to the World object's processCommand method.
	 * 
	 * @return the World instance created
	 */
	public static TileGrid createWorldAndAcceptCommands()
	{
		// HEY YOU!!
		//
		// For Checkpoint 1, you must implement this method fully.
		//
		// Don't forget to return the World you created instead of null
		
		// Fix this!
		return null;
	}
	
	
	/**
	 * This is where your program begins executing.  You should understand
	 * what this code does, but you don't need to change this.  Read
	 * the comments.
	 */
	public static void main(String[] args)
	{
		// This adds the ability to use the mouse to send commands
		// to your Scanner.  It is only safe to call this at
		// the very beginning of main.  You can safely remove this line,
		// but why would you?  This makes things fun.
		coreGolGUI.GameOfLifePanel.useGameOfLifeControls();
		
		// This calls your method above, which will have your
		// loop that executes commands from the user.  You need
		// to keep this call here!
		createWorldAndAcceptCommands();

		// This makes sure the Game of Life window closes when the user
		// is done.  You can safely remove this line, but why would you?
		core.API.close();
	}
}
