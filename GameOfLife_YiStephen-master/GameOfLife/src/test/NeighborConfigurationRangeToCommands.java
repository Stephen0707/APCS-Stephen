package test;

import java.util.ArrayList;
import java.util.List;

// Converts a range of neighbor configurations to a
// list of blocks of set commands (one block per neighbor
// config iterated)
public class NeighborConfigurationRangeToCommands
{
	private int rows;
	private int cols;
	private int rowCenter;
	private int colCenter;
	private String neighborTileType;
	private ArrayList<String> commands;
	private ArrayList<Integer> neighborConfigurations;

	// Each element is a command block (to set a single configuration).  Each command block
	// is terminated with a newline.
	public ArrayList<String> getCommands()
	{
		return commands;
	}

	public List<Integer> getNeighborConfigList()
	{
		return neighborConfigurations;
	}

	private void addAllNeighborSetCommands(int neighborConfig)
	{
		// Iterate through each bit in neighborConfig, adding a "set" command
		// for each one
		class CommandBlockManager
		{
			private String commandBlock = "";

			public CommandBlockManager()
			{
				NeighborConfigurationRangeIterator.callWithDigit(
						neighborConfig,
						(digit, place) -> commandBlock += "set " + 
								((rowCenter + NeighborConfigurationRangeIterator.offsets[place][0] + rows) % rows) + 
								" " + ((colCenter + NeighborConfigurationRangeIterator.offsets[place][1] + cols) % cols) + 
								" " + neighborTileType + " " + digit + "\n");
			}
		}

		CommandBlockManager commandBlockManager = new CommandBlockManager();

		neighborConfigurations.add(neighborConfig);
		commands.add(commandBlockManager.commandBlock);
	}

	public NeighborConfigurationRangeToCommands(int rowsP, int colsP, int rowCenterP, int colCenterP, String neighborTileTypeP, int neighborConfigLow, int neighborConfigHigh, int base, int stepSize)
	{
		rows = rowsP;
		cols = colsP;
		rowCenter = rowCenterP;
		colCenter = colCenterP;
		neighborTileType = neighborTileTypeP;
		commands = new ArrayList<String>();
		neighborConfigurations = new ArrayList<Integer>();

		NeighborConfigurationRangeIterator.callWithNeighborConfigurations(
				neighborConfigLow, 
				neighborConfigHigh,
				base,
				stepSize,
				this::addAllNeighborSetCommands);
	}
}
