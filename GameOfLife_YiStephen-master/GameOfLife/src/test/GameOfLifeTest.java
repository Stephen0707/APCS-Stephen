package test;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import gameOfLife.Main;
import gameOfLife.Tile;
import gameOfLife.TileGrid;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	GameOfLifeTest.Checkpoint1Test.class,
	GameOfLifeTest.Checkpoint2Test.class,
	GameOfLifeTest.Checkpoint3Test.class,
	GameOfLifeTest.Checkpoint4Test.class,
	GameOfLifeTest.FinalTest.class,
})

public class GameOfLifeTest 
{
	private static final boolean enableLogging = true;
	
	// Static helper methods used by the tests and by other
	// classes in the test package

	public static void log(String s)
	{
		if (enableLogging)
		{
			System.out.print(s);
		}
	}

	public static void logln(String s)
	{
		if (enableLogging)
		{
			System.out.println(s);
		}
	}
	
	public static void golAssertTrue(String message, boolean actual)
	{
		if (actual)
		{
			return;
		}
		
		logAssertMessage(message, "true", "" + actual);
		org.junit.Assert.assertTrue(message, actual);
	}

	public static void golAssertEquals(String message, int expected, int actual)
	{
		if (expected == actual)
		{
			return;
		}
		
		logAssertMessage(message, "" + expected, "" + actual);
		org.junit.Assert.assertEquals(message, expected, actual);
	}

	public static void golAssertEquals(String message, boolean expected, boolean actual)
	{
		if (expected == actual)
		{
			return;
		}
		
		logAssertMessage(message, "" + expected, "" + actual);
		org.junit.Assert.assertEquals(message, expected, actual);
	}

	public static void golAssertEquals(String message, Object expected, Object actual)
	{
		if (expected.equals(actual))
		{
			return;
		}
		
		logAssertMessage(message, expected.toString(), actual.toString());
		org.junit.Assert.assertEquals(message, expected, actual);
	}

	public static void golAssertNotNull(String message, Object actual)
	{
		if (actual != null)
		{
			return;
		}
		
		logAssertMessage(message, "non-null", "null");
		org.junit.Assert.assertNotNull(message, actual);
	}

	public static void golAssertColorsEqual(String message, Color expected, Color actual)
	{
		if (expected.equals(actual))
		{
			return;
		}
		
		logAssertMessage(message, ColorString.nameFromColor(expected), ColorString.nameFromColor(actual));
		org.junit.Assert.assertTrue(message, false);
	}
	
	private static void logAssertMessage(String message, String expected, String actual)
	{
		logln("\n------------------------------------------------------------------------------------------------");
		logln("FAILURE:");
		logln(message);
		logln("The value EXPECTED by the test did not match the ACTUAL value provided by the student");
		logln("EXPECTED: " + expected + "     ACTUAL: " + actual);
		logln("------------------------------------------------------------------------------------------------");
	}

	// Helper code used by the tests (which subclass this)
	private static class CheckpointBase
	{
		// Wrapper class to temporarily redirect stdin
		private static class SystemInStreamResetter implements AutoCloseable
		{
			private InputStream origIn;

			public SystemInStreamResetter(InputStream origInP)
			{
				origIn = origInP;
			}

			@Override
			public void close()
			{
				java.lang.System.setIn(origIn);
			}
		}

		public static final String SHAPE_OSCILLATOR = "" +
				"OOO";

		public static final String SHAPE_GLIDER = "" +
				".O.\n" +
				"..O\n" +
				"OOO";

		// Used in calls to the illustrator to draw the grid based on Tile objects
		// actually added to student's World.  Using TileType.NONE, since the actual
		// tile type is not used when determining the illustration characters for a tile
		private final BiFunction<Integer, Integer, TileInfo> tileInfoFromUserWorld =
				(row, col) -> TileInfo.tileInfoFromTile(TileType.NONE, row, col, callGetTile(row, col));
				
		private GameOfLifeTestMediaShim testMediaShim;
		private TileGrid world;
		private int worldRows = -1;
		private int worldCols = -1;

		public TileGrid getWorld()
		{
			return world;
		}

		public int getRows()
		{
			return worldRows;
		}

		public int getCols()
		{
			return worldCols;
		}

		public void testInitialGridPaint(int rows, int cols)
		{
			GameOfLifeTest.logln("Verifying all cells in grid are initially painted Color.DARK_GRAY...");

			createMediaShimIfNecessary();
			testMediaShim.clearExpectedAndPrefillColors(rows, cols);
			testMediaShim.addExpectedColors(0, rows - 1, 0, cols - 1, Color.DARK_GRAY);

			// This verifies all calls to paintSolidColor are correct 
			createWorldWithNoCommands(rows, cols);

			// This verifies no calls to paintSolidColor were omitted
			testMediaShim.verifyRemainingExpectedColors();
		}

		// I'm an N^2 algorithm, so don't call me with big numbers
		public void testTileArrayInitialized(int rows, int cols)
		{
			GameOfLifeTest.logln("Verifying the tile array is initialized with different ConstantTile objects...");
			createWorldWithNoCommands(rows, cols);

			for (int row=0; row < rows; row++)
			{
				for (int col=0; col < cols; col++)
				{
					Tile tile = verifyConstantTile(0, row, col);

					// Does this Tile's address match any prior Tiles?
					for (int prior = 0; prior < row * cols + col; prior++)
					{
						int priorRow = prior / cols;
						int priorCol = prior % cols;
						if (world.getTile(priorRow, priorCol) == tile)
						{
							golAssertTrue(
									"The Tile array is supposed to be initialized with different " +
											"instances of the ConstantTile class.  However, the Tile at at row " + 
											priorRow + ", column " + priorCol + " is the same instance as the Tile at row " +
											row + ", column " + col,
											false);
						}
					}
				}
			}
		}

		// Returns a block of set commands to match the array passed in.
		// Command string includes trailing newline
		private String setTileCommandsFromTileInfos(TileInfo[] tileInfos)
		{
			String commands = "";
			for (int i=0; i < tileInfos.length; i++)
			{
				commands += "set ";
				commands += tileInfos[i].row + " ";
				commands += tileInfos[i].col + " ";
				commands += TileInfo.tileNameFromType(tileInfos[i].type) + " ";
				commands += tileInfos[i].getAge() + "\n";
			}
			return commands;
		}

		public void testSetMultipleTiles(int rows, int cols, TileInfo[] tileInfos)
		{
			String commands = setTileCommandsFromTileInfos(tileInfos);

			// Send the commands to the student's code
			createWorldAndSendCommands(rows, cols, commands);

			// Verify each of the tiles that was created
			for (int i=0; i < tileInfos.length; i++)
			{
				verifyTile(tileInfos[i].type, tileInfos[i].getAge(), tileInfos[i].row, tileInfos[i].col);
			}
		}
		
		public boolean doesClassImplementTile(String className, Class<?> theClass)
		{
			GameOfLifeTest.logln("Found a " + className + " class that implements the following interfaces:");
			Class<?>[] interfaces = theClass.getInterfaces();
			boolean interfaceFound = false;
			for (Class<?> interfaceCls : interfaces)
			{
				GameOfLifeTest.logln("\t" + interfaceCls.getName());
				if (interfaceCls.equals(Tile.class))
				{
					interfaceFound = true;
				}
			}

			return interfaceFound;
		}

		public void testSetCopycats(int rows, int cols, TileInfo[] tileInfos, boolean setCopycatsFirst)
		{
			String commands = getCopycatCommands(tileInfos, setCopycatsFirst);

			TileInfo[][] tiMatrix = tileInfoMatrixFromArray(rows, cols, tileInfos);

			// Send the commands to the student's code
			createWorldAndSendCommands(rows, cols, commands);

			// Verify each of the copycat Tiles matches the age and color of its leader
			verifyCopycatMirrorTiles(tiMatrix);
		}

		private void verifyCopycatMirrorTiles(TileInfo[][] tiMatrix)
		{
			for (int row = 0; row < worldRows; row++)
			{
				for (int col = (worldCols + 1) / 2; col < worldCols; col++)
				{
					verifyCopycatMirrorTile(tiMatrix, row, col);
				}
			}
		}

		public void testSetCopycatsAndEvolve(int rows, int cols, TileType fillTileType, int numStepsPerEvolveCommand, int numEvolveCommands, TileInfo[] tileInfos, boolean setCopycatsFirst, CopycatPattern copycatPattern)
		{
			String commands = "";
			if (fillTileType != TileType.NONE)
			{
				commands += getFill0CommandForTileType(fillTileType) + "\n";
			}
			commands += getCopycatCommands(tileInfos, setCopycatsFirst);
			commands += getEvolveCommands(numStepsPerEvolveCommand, numEvolveCommands);

			// Send the commands to the student's code
			createWorldAndSendCommands(rows, cols, commands);

			// Do the same on our side and verify they match
			evolveAndVerifyWithStdIn(fillTileType, numStepsPerEvolveCommand, numEvolveCommands, tileInfos, copycatPattern);
		}

		private String getCopycatCommands(TileInfo[] tileInfos, boolean setCopycatsFirst)
		{
			String commands = "";
			String copycatCmd = "setCopycats mirror\n"; 
			if (setCopycatsFirst)
			{
				commands += copycatCmd;
			}
			commands += setTileCommandsFromTileInfos(tileInfos);
			if (!setCopycatsFirst)
			{
				commands += copycatCmd;
			}

			return commands;
		}


		// Convert linear array of tileInfos into a sparse 2D array large
		// enough to have an element for each grid cell.
		private TileInfo[][] tileInfoMatrixFromArray(int rows, int cols, TileInfo[] tileInfos)
		{
			TileInfo[][] ret = new TileInfo[rows][cols];

			// Initialize matrix with a bunch of constant age 0 tiles
			for (int row = 0; row < rows; row++)
			{
				for (int col = 0; col < cols; col++)
				{
					ret[row][col] = new TileInfo(TileType.CONSTANT, 0 /* age */, row, col);
				}
			}

			// Overwrite with tileInfos
			for (TileInfo ti : tileInfos)
			{
				ret[ti.row][ti.col] = ti;
			}

			return ret;
		}

		// row,col should contain a copycat tile.  Finds the leader in tiMatrix
		// and ensures it matches the age & color of its leader
		private void verifyCopycatMirrorTile(TileInfo[][] tiMatrix, int row, int col)
		{
			int cols = tiMatrix[0].length;
			int leaderCol = cols - 1 - col;

			//GameOfLifeTest.log("Verifying CopycatTile at row " + row + ", column " + col + " is copying its leader at row " + row + ", column " + leaderCol + "... ");

			TileInfo leader = tiMatrix[row][leaderCol];
			Tile copycat = callGetTile(row, col, TileType.COPYCAT);

			golAssertEquals(
					"Tried calling getAge on the CopycatTile at row " + row + ", column " + col +
					".  It did not return the same value as its leader at row " + row + ", column " + leaderCol,
					leader.getAge(),
					copycat.getAge());
			golAssertColorsEqual(
					"Tried calling getColor on the CopycatTile at row " + row + ", column " + col +
					".  It did not return the same value as its leader at row " + row + ", column " + leaderCol,
					leader.getColor(),
					copycat.getColor());
		}

		private String getFill0CommandForTileType(TileType fillTileType)
		{
			return "fill " + TileInfo.tileNameFromType(fillTileType) + " 0";
		}

		// tests whether getUpdatedTile returns the correct result.
		public void testGetUpdatedTileResult(int rows, int cols, int rowCenter, int colCenter, int centerAge, TileType tileType, int neighborConfig, int base)
		{
			logln("This test verifies that getUpdatedTile returns the correct result from every Tile in the grid");
			
			NeighborConfigurationRangeToCommands cmds = new NeighborConfigurationRangeToCommands(
					rows, cols,
					rowCenter, colCenter,
					TileInfo.tileNameFromType(tileType),
					neighborConfig, neighborConfig, base, 1 /* stepSize */);

			List<String> neighborSetCommands = cmds.getCommands();

			String commands = "";

			// Fill
			commands += getFill0CommandForTileType(tileType) + "\n";

			// Put the center tile in
			commands += "set " + rowCenter + " " + colCenter + " " + TileInfo.tileNameFromType(tileType) + " " + centerAge + "\n";

			// Add its neighbors via set commands
			commands += neighborSetCommands.get(0);

			// Send initial commands to the student's code
			createWorldAndSendCommands(rows, cols, commands);

			// Do the evolve on our end
			TileInfo[] tileInfos = tileInfosFromNeighborConfig(
					tileType,		// center 
					tileType,		// neighbor 
					centerAge, 
					rowCenter, colCenter, 
					neighborConfig);
			
			Illustrator illustrator = new Illustrator(rows, cols);
			//illustrator.logIllustrationLegend();
			logln("With the following starting configuration...");
			String[] illustrationPre = illustrator.getIllustration(tileInfos);
			illustrator.logSingleIllustration(illustrationPre);
			logln("...calling getUpdatedTile on each tile in the grid:");
			
			Evolver evolver = new Evolver(
					rows, cols,
					tileType,
					tileInfos,
					CopycatPattern.NONE);
			evolver.evolve(1 /* num steps */);

			for (int row = 0; row < rows; row++)
			{
				for (int col = 0; col < cols; col++)
				{
					Tile studentTile = callGetTile(row, col);
					TileInfo expectedUpdatedTileInfo = evolver.getTileInfo(row, col);
					verifyGetUpdatedTile(expectedUpdatedTileInfo, row, col, studentTile);
				}
			}
		}

		private void verifyGetUpdatedTile(TileInfo expectedUpdatedTileInfo, int row, int col, Tile studentTile)
		{
			int origAge = studentTile.getAge();
			Color origColor = studentTile.getColor();
			
			Tile updatedTile = callGetUpdatedTile(row, col, studentTile);
			
			// Verify that the state of the original tile did not change
			// as a result of calling getUpdatedTile

			golAssertEquals(
					"Calling getUpdatedTile is not allowed to change the state of the original Tile, but its age changed.",
					origAge, studentTile.getAge());

			golAssertColorsEqual(
					"Calling getUpdatedTile is not allowed to change the state of the original Tile, but its color changed.",
					origColor, studentTile.getColor());
			
			// Verify the updated tile has the correct state

			GameOfLifeTest.logln("Calling getAge and getColor on the Tile returned by getUpdatedTile...");
			golAssertEquals("getAge returned the wrong result", 
					expectedUpdatedTileInfo.getAge(), updatedTile.getAge());

			golAssertColorsEqual(
					"getColor returned the wrong result.",
					expectedUpdatedTileInfo.getColor(), updatedTile.getColor());
		}
		
		// Uses stdin to verify evolve.  Advantage of this form of verification is that we're
		// using stdin, just like an interactive user.
		public void testSetAndEvolveMultipleTilesWithStdIn(int rows, int cols, int numEvolveCommands, TileInfo[] tileInfos)
		{
			testSetAndEvolveMultipleTilesWithStdIn(rows, cols, TileType.NONE, 1, numEvolveCommands, tileInfos);
		}

		public void testSetAndEvolveMultipleTilesWithStdIn(int rows, int cols, TileType fillTileType, int numStepsPerEvolveCommand, int numEvolveCommands, TileInfo[] tileInfos)
		{
			String commands = "";
			if (fillTileType != TileType.NONE)
			{
				commands += getFill0CommandForTileType(fillTileType) + "\n";
			}
			commands += setTileCommandsFromTileInfos(tileInfos);
			commands += getEvolveCommands(numStepsPerEvolveCommand, numEvolveCommands);

			// Send the commands to the student's code
			createWorldAndSendCommands(rows, cols, commands);

			evolveAndVerifyWithStdIn(fillTileType, numStepsPerEvolveCommand, numEvolveCommands, tileInfos, CopycatPattern.NONE);
		}

		// Uses direct calls to processCommand to verify evolve.  Advantage of this form
		// of verification is that we can evolve multiple steps, and verify at each step,
		// and roll it all into a single test
		public void testSetAndEvolveMultipleTilesWithProcessCommand(int rows, int cols, TileType fillTileType, int numTotalEvolveSteps, TileInfo[] tileInfos)
		{
			// Use std in just to init and set original tile config
			String commands = "";
			if (fillTileType != TileType.NONE)
			{
				commands += getFill0CommandForTileType(fillTileType) + "\n";
			}
			commands += setTileCommandsFromTileInfos(tileInfos);

			// Send the initial commands to the student's code
			createWorldAndSendCommands(rows, cols, commands);

			evolveAndVerifyWithProcessCommand(fillTileType, numTotalEvolveSteps, tileInfos);
		}

		public void testSetAndEvolveMultipleTilesInMultipleConfigs(int rows, int cols, TileType fillTileType, 
				int rowCenter, int colCenter, TileType centerType, int centerAge, 
				TileType neighborType, int numTotalEvolveStepsPerConfig, 
				int neighborConfigLow, int neighborConfigHigh, int base, int stepSize)
		{
			logln("This test verifies the result of evolving the world is correct.  There may be several ");
			logln("tile configurations verified in this test, so scroll to the end of the output to see ");
			logln("the configuration that failed to evolve correctly.");

			NeighborConfigurationRangeToCommands cmds = new NeighborConfigurationRangeToCommands(
					rows, cols,
					rowCenter, colCenter,
					TileInfo.tileNameFromType(neighborType),
					neighborConfigLow, neighborConfigHigh, base, stepSize);

			List<String> commands = cmds.getCommands();
			List<Integer> neighborConfigurations = cmds.getNeighborConfigList();

			// Send initial commands to the student's code
			createWorldWithNoCommands(rows, cols);

			Illustrator illustrator = new Illustrator(rows, cols);
			//illustrator.logIllustrationLegend();

			// Each iteration constitutes a setup + evolve
			for (int i=0; i < commands.size(); i++)
			{
				logln("\nPreparing a new tile configuration to test.  Scroll to the end of the output");
				logln("to see the configuration that failed to evolve correctly\n");
				logln("Sending the following commands:");
				
				// Fill
				String fillCommand = getFill0CommandForTileType(fillTileType);
				callProcessCommand(fillCommand);

				// Put the center tile in
				callProcessCommand("set " + rowCenter + " " + colCenter + " " + TileInfo.tileNameFromType(centerType) + " " + centerAge);

				// Add its neighbors via set commands, and then do the evolve command
				String commandBlock = commands.get(i);
				for (String command : commandBlock.split("\n"))
				{
					callProcessCommand(command);
				}

				int neighborConfig = neighborConfigurations.get(i);
				TileInfo[] tileInfos = tileInfosFromNeighborConfig(
						centerType, 
						neighborType, 
						centerAge, 
						rowCenter, colCenter, 
						neighborConfig);

				String[] illustrationPre = illustrator.getIllustration(tileInfos);

				// Tell student to evolve
				callProcessCommand(getEvolveCommand(numTotalEvolveStepsPerConfig));

				// Do the evolve on our end and verify
				Evolver evolver = new Evolver(
						rows, cols,
						fillTileType,
						tileInfos,
						CopycatPattern.NONE);
				evolver.evolve(1 /* num steps */);

				String[] illustrationExpectedPost = illustrator.getIllustration(evolver.getTileInfoMatrix());
				String[] illustrationActualPost = illustrator.getIllustration(tileInfoFromUserWorld);
				
				illustrator.logIllustrationsNoLegend(illustrationPre, illustrationExpectedPost, illustrationActualPost, 1 /* num steps */);

				// verify!
				verifyTiles(evolver);
			}
		}

		private String getEvolveCommands(int numStepsPerEvolveCommand, int numEvolveCommands)
		{
			String commands = "";

			for (int i=0; i < numEvolveCommands; i++)
			{
				commands += getEvolveCommand(numStepsPerEvolveCommand) + "\n";
			}

			return commands;
		}

		private String getEvolveCommand(int numSteps)
		{
			return "evolve " + numSteps + " 1";
		}

		// Uses direct calls to processCommand to verify evolve.  Advantage of this form
		// of verification is that we can evolve multiple steps, and verify at each step,
		// and roll it all into a single test
		private void evolveAndVerifyWithProcessCommand(TileType fillTileType, int numTotalEvolveSteps, TileInfo[] tileInfos)
		{
			// TODO: Make sure I check how this looks
			Illustrator illustrator = new Illustrator(worldRows, worldCols);
			//illustrator.logIllustrationLegend();
			
			Evolver evolver = new Evolver(worldRows, worldCols, fillTileType, tileInfos, CopycatPattern.NONE);

			for (int step = 1; step <= numTotalEvolveSteps; step++)
			{
				GameOfLifeTest.logln("Evolving step #" + step + ".  Sending the following command:");

				String[] illustrationPre = illustrator.getIllustration(tileInfos);
				
				// Have student do one evolve step
				callProcessCommand("evolve 1 1");
				String[] illustrationActualPost = illustrator.getIllustration(tileInfoFromUserWorld);

				// Have evolver do its own evolve step for verification
				evolver.evolve(1 /* num steps */);
				String[] illustrationExpectedPost = illustrator.getIllustration(evolver.getTileInfoMatrix());

				illustrator.logIllustrationsNoLegend(illustrationPre, illustrationExpectedPost, illustrationActualPost, 1);

				verifyTiles(evolver);
			}
		}

		private void verifyTiles(Evolver evolver)
		{
			for (int row = 0; row < worldRows; row++)
			{
				for (int col = 0; col < worldCols; col++)
				{
					TileInfo tileInfo = evolver.getTileInfo(row, col);
					if (tileInfo == null || tileInfo.type == TileType.NONE)
					{
						continue;
					}
					verifyTile(tileInfo.type, tileInfo.getAge(), tileInfo.getColor(), row, col);
				}
			}	
		}
		
		// Uses stdin to verify evolve.  Advantage of this form of verification is that we're
		// using stdin, just like an interactive user.
		private void evolveAndVerifyWithStdIn(TileType fillTileType, int numStepsPerEvolveCommand, int numEvolveCommands, TileInfo[] tileInfos, CopycatPattern copycatPattern)
		{
			Illustrator illustrator = new Illustrator(worldRows, worldCols);
			int numTotalSteps = numStepsPerEvolveCommand * numEvolveCommands;
			String[] illustrationPre = illustrator.getIllustration(tileInfos, copycatPattern);
			
			// Figure out expected state after the evolution
			Evolver evolver = new Evolver(worldRows, worldCols, fillTileType, tileInfos, copycatPattern);
			evolver.evolve(numTotalSteps);
			String[] illustrationExpectedPost = illustrator.getIllustration(evolver.getTileInfoMatrix());
			String[] illustrationActualPost = illustrator.getIllustration(tileInfoFromUserWorld);
			
			illustrator.logIllustrationsNoLegend(illustrationPre, illustrationExpectedPost, illustrationActualPost, numTotalSteps);

			// Verify the student tiles match ours.  Skip all copycat tiles, as those
			// will be verified separately, with more specific error messages

			for (int row = 0; row < worldRows; row++)
			{
				if (copycatPattern == CopycatPattern.KALEIDOSCOPE && row >= (row + 1) / 2)
				{
					continue;
				}

				for (int col = 0; col < worldCols; col++)
				{
					if (copycatPattern != CopycatPattern.NONE && col >= (col + 1) / 2)
					{
						continue;
					}

					TileInfo tileInfo = evolver.getTileInfo(row, col);
					if (tileInfo == null || tileInfo.type == TileType.NONE)
					{
						continue;
					}
					verifyTile(tileInfo.type, tileInfo.getAge(), tileInfo.getColor(), row, col);
				}
			}

			if (copycatPattern == CopycatPattern.NONE)
			{
				return;
			}

			// Verify copycat tiles
			if (copycatPattern == CopycatPattern.MIRROR)
			{
				verifyCopycatMirrorTiles(evolver.getTileInfoMatrix());
			}

			// FUTURE: Implement Kaleidoscope pattern
		}

		private void verifyTile(TileType type, int age, int row, int col)
		{
			verifyTile(type, age, null /* color */, row, col);
		}

		// If color is null, derives what the color should be from the age.
		// This doesn't work so well for immigration tiles, so specify color
		// explicitly for those
		private void verifyTile(TileType type, int age, Color color, int row, int col)
		{
			switch(type)
			{
				case NONE:
					break;

				case CONSTANT:
					verifyConstantTile(age, row, col, true /* verifyGetUpdatedTile */);
					break;

				case RAINBOW:
					verifyRainbowTile(age, row, col);
					break;

				case MONO:
					verifyMonoTile(age, row, col);
					break;

				case IMMIGRATION:
					verifyColoredTile(TileType.IMMIGRATION, age, color, row, col);
					break;

				case QUAD:
					verifyColoredTile(TileType.QUAD, age, color, row, col);
					break;

				default:
					GameOfLifeTest.logln("Internal test error, unknown TileType " + type);
					throw new UnsupportedOperationException();
			}
		}

		public Tile verifyConstantTile(int age, int row, int col)
		{
			return verifyConstantTile(age, row, col, false /* verifyGetUpdatedTile */);
		}
		
		private Tile callGetTile(int row, int col)
		{
			return callGetTile(row, col, TileType.NONE);
		}

		private Tile callGetTile(int row, int col, TileType expectedTileType)
		{
			Tile tile = getWorld().getTile(row, col);

			golAssertNotNull(
					"getTile incorrectly returned null at row " + row + ", column " + col, 
					tile);
			
			if (expectedTileType != TileType.NONE)
			{
				golAssertEquals(
						"getTile(" + row + ", " + col + ") returned a Tile of the wrong type",
						TileInfo.tileClassNameFromType(expectedTileType),
						tile.getClass().getName());
			}		

			return tile;
		}

		private Tile[] getStudentNeighbors(int row, int col)
		{
			Tile[] ret = new Tile[8];
			int iRet = 0;

			for (int r = -1; r < 2; r++)
			{
				for (int c = -1; c < 2; c++)
				{
					if (r == 0 && c == 0)
					{
						continue;
					}

					ret[iRet++] = 
							callGetTile((row + worldRows + r) % worldRows, 
									(col + worldCols + c) % worldCols);
				}
			}

			return ret;
		}

		private Tile callGetUpdatedTile(int row, int col, Tile tile)
		{
			Tile[] studentNeighbors = getStudentNeighbors(row, col);

			GameOfLifeTest.logln("Calling getUpdatedTile on Tile at row " + row + ", column " + col + "...");
			Tile updatedTile = tile.getUpdatedTile(studentNeighbors);
			
			golAssertNotNull("getUpdatedTile incorrectly returned null", updatedTile);
			
			return updatedTile;
		}
		
		public void verifyConstantTileWithSetTileMethod(int age, int row1, int col1, int row2, int col2, boolean verifyGetUpdatedTile)
		{
			logln("Calling getTile(" + row1 + ", " + col1 + ") on student's World object to get the ConstantTile at that location");
			Tile tile = verifyConstantTile(age, row1, col1, true /* verifyGetUpdatedTile */);
			logln("Calling setTile method on student's World object to add the above ConstantTile to row " + row2 + ", column " + col2);
			getWorld().setTile(row2, col2, tile);
			logln("Calling getTile(" + row2 + ", " + col2 + ") on student's World object to get the ConstantTile at that location");
			verifyConstantTile(age, row2, col2, true /* verifyGetUpdatedTile */);
		}

		public Tile verifyConstantTile(int age, int row, int col, boolean verifyGetUpdatedTile)
		{
			// High-frequency logging, intentionally commented out
			//GameOfLifeTest.logln("Inspecting ConstantTile at row " + row + ", column " + col + "...");
			Tile tile = callGetTile(row, col, TileType.CONSTANT);

			golAssertEquals(
					"The Tile at row " + row + ", column " + col + " returned the wrong result from its getAge method",
					age, tile.getAge());

			Color expectedColor = TileInfo.constantTileColorFromAge(age);  

			golAssertColorsEqual(
					"The Tile at row " + row + ", column " + col + 
					" returned the wrong color from its getColor method. ",
					expectedColor, tile.getColor());

			if (verifyGetUpdatedTile)
			{
				verifyGetUpdatedTile(
						new TileInfo(TileType.CONSTANT, age, expectedColor, row, col),
						row, col,
						tile);
			}

			return tile;
		}
		
		public void verifyRainbowTileWithSetTileMethod(int age, int row1, int col1, int row2, int col2)
		{
			logln("Calling getTile(" + row1 + ", " + col1 + ") on student's World object to get the RainbowTile at that location");
			Tile tile = verifyRainbowTile(age, row1, col1);
			logln("Calling setTile method on student's World object to add the above RainbowTile to row " + row2 + ", column " + col2);
			getWorld().setTile(row2, col2, tile);
			logln("Calling getTile(" + row2 + ", " + col2 + ") on student's World object to get the RainbowTile at that location");
			verifyRainbowTile(age, row2, col2);
		}

		public Tile verifyRainbowTile(int age, int row, int col)
		{
			// High-frequency logging, intentionally commented out
			//GameOfLifeTest.logln("Inspecting RainbowTile at row " + row + ", column " + col + "...");
			Tile tile = callGetTile(row, col, TileType.RAINBOW);

			golAssertEquals(
					"The Tile at row " + row + ", column " + col + " returned the wrong result from its getAge method",
					age, tile.getAge());

			Color expectedColor = TileInfo.rainbowTileColorFromAge(age);

			golAssertColorsEqual(
					"The Tile at row " + row + ", column " + col + 
					" returned the wrong color from its getColor method. ",
					expectedColor, tile.getColor());

			verifyGetUpdatedTile(
					new TileInfo(
							TileType.RAINBOW, 
							age + 1, 
							TileInfo.rainbowTileColorFromAge(age + 1),
							row, col), 
					row, col,
					tile);

			return tile;
		}

		public Tile verifyMonoTile(int age, int row, int col)
		{
			// High-frequency logging, intentionally commented out
			//GameOfLifeTest.logln("Inspecting MonoTile at row " + row + ", column " + col + "...");
			Tile tile = callGetTile(row, col, TileType.MONO);

			golAssertEquals(
					"The Tile at row " + row + ", column " + col + " returned the wrong result from its getAge method",
					age, tile.getAge());

			Color expectedColor = TileInfo.monoTileColorFromAge(age);

			golAssertColorsEqual(
					"The Tile at row " + row + ", column " + col + 
					" returned the wrong color from its getColor method. ",
					expectedColor, tile.getColor());

			return tile;
		}

		public Tile verifyColoredTile(TileType tileType, int age, Color expectedColor, int row, int col)
		{
			// High-frequency logging, intentionally commented out
			//GameOfLifeTest.logln("Inspecting ImmigrationTile at row " + row + ", column " + col + "...");
			if (tileType != TileType.IMMIGRATION && tileType != TileType.QUAD)
			{
				throw new InvalidParameterException("Internal test failure: Unsupported tile type: " + tileType);
			}

			Tile tile = callGetTile(row, col, tileType);

			golAssertEquals(
					"The Tile at row " + row + ", column " + col + " returned the wrong result from its getAge method",
					age, tile.getAge());

			if (expectedColor == null)
			{
				if (tileType == TileType.IMMIGRATION)
				{
					expectedColor = TileInfo.immigrationTileConstructedColorFromAge(age);
				}
				else
				{
					// Quad
					expectedColor = TileInfo.quadTileConstructedColorFromAge(age);
				}
			}

			golAssertColorsEqual(
					"The Tile at row " + row + ", column " + col + 
					" returned the wrong color from its getColor method. ",
					expectedColor, tile.getColor());

			return tile;
		}

		// Convert neighbor configuration number to an array of TileInfos
		// Neighbor config can be any base
		public TileInfo[] tileInfosFromNeighborConfig(TileType centerType, TileType neighborType, int age, int row, int col, int neighborConfig)
		{
			// Return array of 8 neighbors plus the tile in question
			TileInfo[] ret = new TileInfo[9];

			NeighborConfigurationRangeIterator.callWithDigit(
					neighborConfig,
					(digit, place) -> ret[place] = new TileInfo(
							neighborType,
							digit,
							((row + NeighborConfigurationRangeIterator.offsets[place][0] + worldRows) % worldRows), 
							((col + NeighborConfigurationRangeIterator.offsets[place][1] + worldCols) % worldCols)));  

			ret[8] = new TileInfo(centerType, age, row, col);

			return ret;
		}

		public TileInfo[] tileInfosFromConstantNeighborConfig(TileType centerType, int age, int row, int col, int neighborConfig)
		{
			return tileInfosFromNeighborConfig(centerType, TileType.CONSTANT, age, row, col, neighborConfig);
		}


		public void testGetUpdatedTileNeighbors(int rows, int cols, int rowCenter, int colCenter, int neighborConfigLow, int neighborConfigHigh)
		{
			logln("This test verifies the neighbor array parameter passed to getUpdatedTile on the Tile in ");
			logln("row " + rowCenter + ", column " + colCenter + ".  There may be several tile configurations");
			logln("verified in this test, so scroll to the end of the output to see the configuration that failed.");
			
			NeighborConfigurationRangeToCommands cmds = new NeighborConfigurationRangeToCommands(
					rows, cols,
					rowCenter, colCenter,
					"constant",
					neighborConfigLow, neighborConfigHigh,
					2,			// These tests are only called with base 2 neighbor configs
					1);			// step size
			ArrayList<String> commands = cmds.getCommands();
			List<Integer> neighborConfigurations = cmds.getNeighborConfigList();

			createWorldWithNoCommands(rows, cols);

			GutTileShim tile = new GutTileShim(neighborConfigurations);
			logln("Calling setTile method on student's World object to add a special test tile to row " + rowCenter + ", column " + colCenter);
			logln("This is the tile on which the neighbor array parameter passed to getUpdatedTile will be tested.");
			getWorld().setTile(rowCenter, colCenter, tile);

			Illustrator illustrator = new Illustrator(worldRows, worldCols);
			//illustrator.logIllustrationLegend();
			
			for (int iNeighborConfig = 0; iNeighborConfig < neighborConfigurations.size(); iNeighborConfig++)
			{
				logln("\nPreparing a new tile configuration to test.  Scroll to the end of the output");
				logln("to see the configuration that failed to evolve correctly\n");
				logln("Sending the following commands:");
				
				String commandBlock = commands.get(iNeighborConfig);
				int neighborConfig = neighborConfigurations.get(iNeighborConfig);
				
				// Send the set commands for this config
				String[] commandArr = commandBlock.split("\n");
				for (String command : commandArr)
				{
					callProcessCommand(command);
				}
				
				TileInfo[] tileInfos = tileInfosFromNeighborConfig(
						TileType.MONO,		// center 
						TileType.MONO,		// neighbor 
						1,					// centerAge 
						rowCenter, colCenter, 
						neighborConfig);
				String[] worldPic = illustrator.getIllustration(tileInfos, rowCenter, colCenter);
				logln("\nAbove commands should produce the following Tile layout:");
				logln("(where *** represents the test tile on which the neighbor array parameter passed to getUpdatedTile will be tested)");
				illustrator.logSingleIllustration(worldPic);

				// Send the evolve command
				logln("\nSending the following command:");
				callProcessCommand("evolve 1 1");

				// During the above evolve command, GutTileShim will verify the neighbors
				// passed to getUpdatedTile are correct
			}

			golAssertEquals("getUpdatedTile was called the wrong number of times.", 
					neighborConfigurations.size(),
					tile.getNumCallsToGetUpdatedTile());

		}

		// only base 2 neighbor configs supported
		public void testGetNumActiveNeighbors(int rows, int cols, TileType tileType, int rowCenter, int colCenter, int neighborConfig)
		{
			switch (tileType)
			{
				case MONO:
				case IMMIGRATION:
				case QUAD:
					break;

				default:
					throw new UnsupportedOperationException("Internal test failure; unexpected tile type: " + tileType);
			}

			String tileName = TileInfo.tileNameFromType(tileType);
			NeighborConfigurationToTileShims shims = new NeighborConfigurationToTileShims(
					rows, cols,
					neighborConfig,
					tileName);

			// Gnan = getNumActiveNeighbors
			GnanTileShim[] tileShims = shims.getTileShims();

			createWorldWithNoCommands(rows, cols);

			// Put a subclass of LifeTile in the center
			logln("Sending the following command:");
			callProcessCommand("set " + rowCenter + " " + colCenter + " " + tileName + " 1");
			Tile lifeTile = callGetTile(rowCenter, colCenter, tileType);
			
			// call student's getNumActiveNeighbors
			int actualGnan = callGetNumActiveNeighbors(lifeTile, tileShims, TileInfo.tileNameFromType(tileType));
			
			// Count the 1s in neighborConfig to get the expected
			// num of active neighbors.  Mod 9 only works cuz
			// neighborConfig <= 8 bits
			int expectedGnan = neighborConfig % 9;

			golAssertEquals("getNumActiveNeighbors returned the wrong result",
					expectedGnan, actualGnan);
		}

		private int callGetNumActiveNeighbors(Tile lifeTile, Tile[] neighbors, String tileClassName)
		{
			GameOfLifeTest.logln("Calling getNumActiveNeighbors on student's " + tileClassName + " tile class");
			Method gnanMethod = findGetNumActiveNeighborsMethod(lifeTile.getClass(), tileClassName);
			Object retObj = null;
			try
			{
				// Must cast neighbors to Object so reflection knows we intend to pass
				// a single parameter to the student's method; else the array could
				// be converted into a vararg list against our will
				retObj = gnanMethod.invoke(lifeTile, (Object) neighbors);
			}
			catch (InvocationTargetException e)
			{
				GameOfLifeTest.logln("Unable to call getNumActiveNeighbors on the student's " + tileClassName + " tile class");
				GameOfLifeTest.logln(e.getMessage());
			}
			catch (IllegalAccessException e)
			{
				GameOfLifeTest.logln("Unable to call getNumActiveNeighbors on the student's " + tileClassName + " tile class");
				GameOfLifeTest.logln(e.getMessage());
			}
			
			golAssertNotNull("Failed to call getNumActiveNeighbors.  Verify you have implemented that method with the exact signature from the spec",
					retObj);

			return (int) retObj;
		}

		public Method findGetNumActiveNeighborsMethod(Class<?> cls, String clsName)
		{
			Method gnanMethod = null;
			try
			{
				gnanMethod = cls.getMethod(
						"getNumActiveNeighbors", 
						new Class<?>[]{ Tile[].class  });
			}
			catch (NoSuchMethodException e)
			{
				gnanMethod = null;
			}

			golAssertNotNull("Cannot find a method named getNumActiveNeighbors taking Tile[] as parameter in the " + clsName + " class.", gnanMethod);

			golAssertTrue("Found a method named getNumActiveNeighbors taking Tile[] as parameter in the " + clsName + " class, but it is not declared as returning int.  Actual return type is: " +
					gnanMethod.getReturnType().getName(),
					int.class.equals(gnanMethod.getReturnType()));
			return gnanMethod;
		}


		public void createMediaShimIfNecessary()
		{
			if (testMediaShim != null)
			{
				return;
			}

			testMediaShim = new GameOfLifeTestMediaShim();
			core.API.setCustomMediaManager(testMediaShim);
		}


		public void createWorldAndSendCommands(int rows, int cols, String programInput)
		{
			GameOfLifeTest.logln("Creating world with " + rows + " rows, " + cols + " columns");
			createMediaShimIfNecessary();

			// Prepend command list with dimensions of grid, so student knows
			// how to create World object
			programInput = rows + "\n" + cols + "\n" + programInput;

			// Normalize ending (remove newline) before dealing with quit
			if (programInput.charAt(programInput.length() - 1) == '\n')
			{
				programInput = programInput.substring(0,  programInput.length() - 1);
			}
			
			// When logging output, we don't include "quit" in the list of commands
			// sent to the World, to avoid student confusion over the fact that
			// we often send additional commands afterward (via processCommand).
			// So keep the String we log separate from the actual String we send
			String programInputToLog = programInput;
			String programInputToSend = programInput + "\nquit";				

			byte[] inputBytes = null;
			try	
			{
				inputBytes = programInputToSend.getBytes(StandardCharsets.UTF_8.name());
			}
			catch (UnsupportedEncodingException e)
			{
				GameOfLifeTest.logln("Internal test error, UnsupportedEncodingException for input stream.");
				GameOfLifeTest.logln(e.toString());
				golAssertEquals("Internal Test Failure; See Test Output for failure info", true, false);
			} 
			InputStream stream = new ByteArrayInputStream(inputBytes);

			// Unconditionally change System.in to use our own InputStream, so student
			// code using Scanner will see our input.
			//
			// Use wrapper class to automatically reinstate the original
			// System.in after the student's createWorldAndAcceptCommands has returned

			try (SystemInStreamResetter autoIn = new SystemInStreamResetter(java.lang.System.in))
			{
				java.lang.System.setIn(stream);

				// Run student code

				GameOfLifeTest.logln("BEGIN: Calling student's createWorldAndAcceptCommands with the following sent to the Scanner:");
				GameOfLifeTest.logln(">>");
				GameOfLifeTest.logln(programInputToLog);
				GameOfLifeTest.logln("<<");
				world = Main.createWorldAndAcceptCommands();
				GameOfLifeTest.logln("END: Student's createWorldAndAcceptCommands returned");
			}
			catch (NoSuchElementException e)
			{
				if (e.toString().toLowerCase().contains("no line found"))
				{
					GameOfLifeTest.logln("An exception was thrown which MIGHT indicate your code does not handle \"quit\" correctly.  Ensure you do not attempt to read from your Scanner after \"quit\" has been sent.");
				}
				throw e;
			}

			worldRows = rows;
			worldCols = cols;

			golAssertNotNull("createWorldAndAcceptCommands returned null instead of a newly created World object",  world);
			golAssertTrue("The World constructor did not call core.API.initialize", testMediaShim.getInitialized());
			golAssertEquals("core.API.initialize was called with the wrong number of rows", rows, testMediaShim.getRows());
			golAssertEquals("core.API.initialize was called with the wrong number of columns", cols, testMediaShim.getColumns());
		}

		public void createWorldWithNoCommands(int rows, int cols)
		{
			createWorldAndSendCommands(rows, cols, "");
		}

		private void callProcessCommand(String command)
		{
			//GameOfLifeTest.logln("Calling processCommand with \"" + command + "\"");
			GameOfLifeTest.logln(command);
			world.processCommand(command);
		}


		public void testFillViaStdIn(TileType type, int age, String priorCommands, int rows, int cols)
		{
			// Tests fill by sending String "fill" command
			// to StdIn to be read by student's commandLoop,
			// and uses getTile (via verifyTile) to verify
			// all the Tiles are correct

			createWorldAndSendCommands(
					rows, cols, 
					priorCommands + 
					"fill " + TileInfo.tileNameFromType(type) + " " + age);

			// Verify each of the tiles that was created
			for (int row=0; row < rows; row++)
			{
				for (int col=0; col < cols; col++)
				{
					verifyTile(type, age, row, col);
				}
			}
		}

		public void testFillViaProcessCommand(TileType type, int age, Color color, int rows, int cols)
		{
			// Tests fill by sending String "fill" command
			// individually to processCommand, using the
			// media shim to verify paintSolidColor is
			// called correctly

			createMediaShimIfNecessary();

			createWorldAndSendCommands(rows, cols, "");

			// This verifies all calls to paintSolidColor are correct
			testMediaShim.clearExpectedAndPrefillColors(rows, cols);
			testMediaShim.addExpectedColors(0, rows - 1, 0, cols - 1, color);
			testMediaShim.addPrefillColor(Color.DARK_GRAY);

			logln("Sending the following command:");
			callProcessCommand("fill " + TileInfo.tileNameFromType(type) + " " + age);

			// This verifies no calls to paintSolidColor were omitted
			testMediaShim.verifyRemainingExpectedColors();
		}
		
		public void testSetConstantTile(int rows, int cols, int row, int col, int age, boolean testPaint)
		{
			createMediaShimIfNecessary();
			String setCommand = "set " + row + " " + col + " constant " + age;
			
			if (testPaint)
			{
				createWorldWithNoCommands(rows, cols);
	
				// This verifies all calls to paintSolidColor are correct
				testMediaShim.clearExpectedAndPrefillColors(rows, cols);
				testMediaShim.addExpectedColors(row, row, col, col, TileInfo.constantTileColorFromAge(age));
				
				logln("Sending the following command:");
				callProcessCommand(setCommand);

				// This verifies no calls to paintSolidColor were omitted
				testMediaShim.verifyRemainingExpectedColors();
			}
			else
			{
				createWorldAndSendCommands(rows, cols, setCommand);
				verifyConstantTile(age, row, col, true /* verifyGetUpdatedTile */);
			}
		}

		public void testSetRainbowTile(int rows, int cols, int row, int col, int age, boolean testPaint)
		{
			createMediaShimIfNecessary();
			String setCommand = "set " + row + " " + col + " rainbow " + age;
			
			if (testPaint)
			{
				createWorldWithNoCommands(rows, cols);
	
				// This verifies all calls to paintSolidColor are correct
				testMediaShim.clearExpectedAndPrefillColors(rows, cols);
				testMediaShim.addExpectedColors(row, row, col, col, TileInfo.rainbowTileColorFromAge(age));
				
				logln("Sending the following command:");
				callProcessCommand(setCommand);

				// This verifies no calls to paintSolidColor were omitted
				testMediaShim.verifyRemainingExpectedColors();
			}
			else
			{
				createWorldAndSendCommands(rows, cols, setCommand);
				verifyRainbowTile(age, row, col);
			}
		}

		public void testSetShape(int rows, int cols, TileType fillTileType, int row, int col, String shapeName, String shapePattern)
		{
			String commands = "";
			if (fillTileType != TileType.NONE)
			{
				commands += getFill0CommandForTileType(fillTileType) + "\n";
			}
			commands += "setShape " + row + " " + col + " " + shapeName;

			// Send the commands to the student's code
			createWorldAndSendCommands(rows, cols, commands);

			verifyShape(fillTileType, row, col, shapePattern);
		}

		private void verifyShape(TileType fillTileType, int row, int col, String shapePattern)
		{
			String[] shapePatternLines = shapePattern.split("\n");
			int width = shapePatternLines[0].length();
			int height = shapePatternLines.length;

			GameOfLifeTest.logln("Verifying tiles outside of the shape's area remain untouched...");

			// Above
			for (int r = 0; r < row; r++)
			{
				for (int c = 0; c < worldCols; c++)
				{
					verifyTile(fillTileType, 0, r, c);
				}
			}

			// Below
			for (int r = row + height; r < worldRows; r++)
			{
				for (int c = 0; c < worldCols; c++)
				{
					verifyTile(fillTileType, 0, r, c);
				}
			}

			// Left
			for (int r = 0; r < worldRows; r++)
			{
				for (int c = 0; c < col; c++)
				{
					verifyTile(fillTileType, 0, r, c);
				}
			}

			// Right
			for (int r = 0; r < worldRows; r++)
			{
				for (int c = col + width; c < worldCols; c++)
				{
					verifyTile(fillTileType, 0, r, c);
				}
			}

			GameOfLifeTest.logln("Verifying tiles inside shape's area are correct...");

			for (int r = 0; r < height; r++)
			{
				for (int c = 0; c < width; c++)
				{
					char expected = shapePatternLines[r].charAt(c);
					if (expected == '.')
					{
						verifyTile(fillTileType, 0, row + r, col + c);
					}
					else if (expected == 'O')
					{
						verifyTile(TileType.MONO, 1, row + r, col + c);
					}
					else
					{
						golAssertTrue("Internal test error, unexpected shape character: '" + expected + "'", false);
					}
				}
			}
		}
	}


	public static class Checkpoint1Test extends CheckpointBase
	{
		@Before
		public void initialize()
		{
			coreGolGUI.GameOfLifePanel.enableTestMode();
		}
		
		@Test(timeout = 1000)
		public void level1CreateWorld75x74()
		{
			createWorldWithNoCommands(75, 74);
		}

		@Test(timeout = 1000)
		public void level1CreateWorld5x4()
		{
			createWorldWithNoCommands(5, 4);
		}

		@Test(timeout = 1000)
		public void level1CreateWorld2x3()
		{
			createWorldWithNoCommands(2, 3);
		}

		@Test(timeout = 1000)
		public void level1CreateWorld20x100()
		{
			createWorldWithNoCommands(20, 100);
		}

		@Test(timeout = 1000)
		public void level1CreateWorld1x1()
		{
			createWorldWithNoCommands(1, 1);
		}

		@Test(timeout = 1000)
		public void level1CreateWorld100x100()
		{
			createWorldWithNoCommands(100, 100);
		}

		@Test(timeout = 1000)
		public void level2InitialGridPaint100x200()
		{
			testInitialGridPaint(100, 200);
		}

		@Test(timeout = 1000)
		public void level2InitialGridPaint250x125()
		{
			testInitialGridPaint(250, 125);
		}

		@Test(timeout = 1000)
		public void level2TileArrayInitialized5x7()
		{
			testTileArrayInitialized(5, 7);
		}

		@Test(timeout = 1000)
		public void level2TileArrayInitialized6x4()
		{
			testTileArrayInitialized(6, 4);
		}

		@Test(timeout = 1000)
		public void level3TestGetUpdatedTileOnConstantTileAge0()
		{
			int rows = 99;
			int cols = 92;

			createWorldWithNoCommands(rows, cols);
			for (int row = 0; row < rows; row++)
			{
				for (int col = 0; col < cols; col++)
				{
					verifyConstantTile(0, row, col, true /* verifyGetUpdatedTile */);
				}
			}
		}

		@After
		public void close()
		{
			core.API.close();
		}
	}

	public static class Checkpoint2Test extends CheckpointBase
	{
		@Before
		public void initialize()
		{
			coreGolGUI.GameOfLifePanel.enableTestMode();
		}

		@Test(timeout = 1000)
		public void level1SetConstantTile1a()
		{
			testSetConstantTile(100, 101, 99, 100, 0, false /* testPaint */);
		}

		@Test(timeout = 1000)
		public void level1SetConstantTile1b()
		{
			testSetConstantTile(100, 101, 99, 100, 0, true /* testPaint */);
		}

		@Test(timeout = 1000)
		public void level1SetConstantTile2a()
		{
			testSetConstantTile(101, 100, 100, 99, 0, false /* testPaint */);
		}

		@Test(timeout = 1000)
		public void level1SetConstantTile2b()
		{
			testSetConstantTile(101, 100, 100, 99, 0, true /* testPaint */);
		}

		@Test(timeout = 1000)
		public void level1SetConstantTile3a()
		{
			testSetConstantTile(101, 100, 0, 0, 1, false /* testPaint */);
		}

		@Test(timeout = 1000)
		public void level1SetConstantTile3b()
		{
			testSetConstantTile(101, 100, 0, 0, 1, true /* testPaint */);
		}

		@Test(timeout = 1000)
		public void level1SetConstantTile4a()
		{
			testSetConstantTile(100, 101, 1, 0, 1, false /* testPaint */);
		}

		@Test(timeout = 1000)
		public void level1SetConstantTile4b()
		{
			testSetConstantTile(100, 101, 1, 0, 1, true /* testPaint */);
		}

		@Test(timeout = 1000)
		public void level1SetConstantTile5()
		{
			createWorldAndSendCommands(
					10, 20,
					"set 5 15 constant 1");
			verifyConstantTileWithSetTileMethod(1, 5, 15, 9, 19, true /* verifyGetUpdatedTile */);
		}
		
		@Test(timeout = 1000)
		public void level1SetConstantTile6()
		{
			createWorldAndSendCommands(
					20, 10,
					"set 15 5 constant 1");
			verifyConstantTileWithSetTileMethod(1, 15, 5, 19, 9, true /* verifyGetUpdatedTile */);
		}
		
		@Test(timeout = 1000)
		public void level1SetRainbowTile1a()
		{
			testSetRainbowTile(57, 58, 23, 36, 1, false /* testPaint */);
		}

		@Test(timeout = 1000)
		public void level1SetRainbowTile1b()
		{
			testSetRainbowTile(57, 58, 23, 36, 1, true /* testPaint */);
		}

		@Test(timeout = 1000)
		public void level1SetRainbowTile2a()
		{
			testSetRainbowTile(22, 22, 21, 21, 6, false /* testPaint */);
		}

		@Test(timeout = 1000)
		public void level1SetRainbowTile2b()
		{
			testSetRainbowTile(22, 22, 21, 21, 6, true /* testPaint */);
		}

		@Test(timeout = 1000)
		public void level1SetRainbowTile3a()
		{
			testSetRainbowTile(2, 2, 0, 1, 7, false /* testPaint */);
		}

		@Test(timeout = 1000)
		public void level1SetRainbowTile3b()
		{
			testSetRainbowTile(2, 2, 0, 1, 7, true /* testPaint */);
		}

		@Test(timeout = 1000)
		public void level1SetRainbowTile4()
		{
			createWorldAndSendCommands(
					10, 100, 
					"set 9 99 rainbow 5");

			verifyRainbowTileWithSetTileMethod(5, 9, 99, 3, 99);
		}

		@Test(timeout = 1000)
		public void level1FillConstantAge1TilesCmdLoop()
		{
			testFillViaStdIn(TileType.CONSTANT, 1, "", 10, 10);
		}

		@Test(timeout = 1000)
		public void level1FillConstantAge1TilesProcCmd()
		{
			testFillViaProcessCommand(TileType.CONSTANT, 1, Color.LIGHT_GRAY, 11, 10);
		}

		@Test(timeout = 1000)
		public void level1FillConstantAge0TilesCmdLoop()
		{
			testFillViaStdIn(TileType.CONSTANT, 0, "", 9, 8);
		}

		@Test(timeout = 1000)
		public void level1FillConstantAge0TilesProcCmd()
		{
			testFillViaProcessCommand(TileType.CONSTANT, 0, Color.DARK_GRAY, 8, 9);
		}

		@Test(timeout = 1000)
		public void level1FillRainbowAge5TilesCmdLoop()
		{
			testFillViaStdIn(TileType.RAINBOW, 5, "", 10, 10);
		}

		@Test(timeout = 1000)
		public void level1FillRainbowAge5TilesProcCmd()
		{
			testFillViaProcessCommand(TileType.RAINBOW, 5, Color.MAGENTA, 11, 10);
		}

		@Test(timeout = 1000)
		public void level1FillRainbowAge50TilesCmdLoop()
		{
			testFillViaStdIn(TileType.RAINBOW, 50, "", 9, 8);
		}

		@Test(timeout = 1000)
		public void level1FillRainbowAge50TilesProcCmd()
		{
			testFillViaProcessCommand(TileType.RAINBOW, 50, Color.YELLOW, 8, 9);
		}


		@Test(timeout = 1000)
		public void level2SetTwoConstantTiles()
		{
			testSetMultipleTiles(
					5, 5,
					new TileInfo[]
							{
									new TileInfo(
											TileType.CONSTANT, 13, 3, 2),
									new TileInfo(
											TileType.CONSTANT, 0, 2, 3),
							});
		}

		@Test(timeout = 1000)
		public void level2SetTwoRainbowTiles()
		{
			testSetMultipleTiles(
					5, 5,
					new TileInfo[]
							{
									new TileInfo(
											TileType.RAINBOW, 8, 4, 1),
									new TileInfo(
											TileType.RAINBOW, 7, 1, 4),
							});
		}

		@Test(timeout = 1000)
		public void level2SetTwoMixedTiles1()
		{
			testSetMultipleTiles(
					5, 5,
					new TileInfo[]
							{
									new TileInfo(
											TileType.CONSTANT, 10, 4, 4),
									new TileInfo(
											TileType.RAINBOW, 1, 0, 0),
							});
		}

		@Test(timeout = 1000)
		public void level2SetTwoMixedTiles2()
		{
			testSetMultipleTiles(
					5, 5,
					new TileInfo[]
							{
									new TileInfo(
											TileType.RAINBOW, 2, 1, 2),
									new TileInfo(
											TileType.CONSTANT, 0, 3, 4),
							});
		}

		@Test(timeout = 1000)
		public void level3SetThenFillRainbowAge27TilesCmdLoop()
		{
			testFillViaStdIn(
					TileType.RAINBOW, 27, 
					"set 1 5 constant 0\n" +
							"set 1 6 constant 2\n" +
							"set 1 7 rainbow 3\n" +
							"set 1 8 rainbow 4\n" +
							"set 1 19 constant 5\n",
							2, 20);
		}

		@Test(timeout = 1000)
		public void level3Set100MixedTiles()
		{
			TileInfo[] tileInfos = new TileInfo[100];

			int iTileInfos = 0;

			// First row are constant tiles of differing ages
			for (int i=0; i < 10; i++)
			{
				tileInfos[iTileInfos++] =
						new TileInfo(
								TileType.CONSTANT,
								i,		// age
								0,		// row
								i);		// col
			}

			// Next eight rows are rainbow tiles of differing ages
			int age = 1;
			for (int row=1; row <= 8; row++)
			{
				for (int col=0; col < 10; col++)
				{
					tileInfos[iTileInfos++] =
							new TileInfo(
									TileType.RAINBOW,
									age++,	// age
									row,
									col);
				}
			}

			// Final row are more constant tiles
			for (int i=0; i < 10; i++)
			{
				tileInfos[iTileInfos++] =
						new TileInfo(
								TileType.CONSTANT,
								(i % 2) * i * 10,	// age
								9,					// row
								i);					// col
			}

			testSetMultipleTiles(10, 10, tileInfos);
		}


		@Test(timeout = 1000)
		public void level3EvolveConstantTiles()
		{
			testSetAndEvolveMultipleTilesWithStdIn(
					5, 4,			// row, cols
					1,				// numSteps
					new TileInfo[]
							{
									new TileInfo(
											TileType.CONSTANT, 2, 1, 0),
									new TileInfo(
											TileType.CONSTANT, 0, 4, 3),
							});
		}

		@Test(timeout = 1000)
		public void level3EvolveRainbowTiles()
		{
			testSetAndEvolveMultipleTilesWithStdIn(
					2, 1,			// row, cols
					1,				// numSteps
					new TileInfo[]
							{
									new TileInfo(
											TileType.RAINBOW, 3, 1, 0),
									new TileInfo(
											TileType.RAINBOW, 1, 0, 0),
							});
		}

		@Test(timeout = 1000)
		public void level4Evolve10MixedTiles()
		{
			testSetAndEvolveMultipleTilesWithStdIn(
					2, 5,			// row, cols
					30,				// numSteps
					new TileInfo[]
							{
									new TileInfo(
											TileType.RAINBOW, 3, 0, 0),
									new TileInfo(
											TileType.CONSTANT, 3, 0, 1),
									new TileInfo(
											TileType.RAINBOW, 8, 0, 2),
									new TileInfo(
											TileType.CONSTANT, 4, 0, 3),
									new TileInfo(
											TileType.RAINBOW, 13, 0, 4),
									new TileInfo(
											TileType.CONSTANT, 0, 1, 0),
									new TileInfo(
											TileType.RAINBOW, 18, 1, 1),
									new TileInfo(
											TileType.CONSTANT, 0, 1, 2),
									new TileInfo(
											TileType.RAINBOW, 23, 1, 3),
									new TileInfo(
											TileType.CONSTANT, 1, 1, 4),
							});
		}

		@Test(timeout = 5000)
		public void level4Evolve100MixedTiles()
		{
			TileInfo[] tileInfos = new TileInfo[100];

			int iTileInfos = 0;

			// First row are constant tiles of differing ages
			for (int i=0; i < 10; i++)
			{
				tileInfos[iTileInfos++] =
						new TileInfo(
								TileType.CONSTANT,
								i,		// age
								0,		// row
								i);		// col
			}

			// Next eight rows are rainbow tiles of differing ages
			int age = 1;
			for (int row=1; row <= 8; row++)
			{
				for (int col=0; col < 10; col++)
				{
					tileInfos[iTileInfos++] =
							new TileInfo(
									TileType.RAINBOW,
									age++,	// age
									row,
									col);
				}
			}

			// Final row are more constant tiles
			for (int i=0; i < 10; i++)
			{
				tileInfos[iTileInfos++] =
						new TileInfo(
								TileType.CONSTANT,
								((i + 1) % 2) * i * 10,	// age
								9,					// row
								i);					// col
			}


			testSetAndEvolveMultipleTilesWithStdIn(
					100, 100,			// row, cols
					20,				// numSteps
					tileInfos);
		}

		@Test(timeout = 1000)
		public void level5LineContinuation1()
		{
			createWorldAndSendCommands(
					10, 20,
					"set 5 15 &\nconstant 1");
			verifyConstantTileWithSetTileMethod(1, 5, 15, 9, 19, true /* verifyGetUpdatedTile */);
		}

		@Test(timeout = 1000)
		public void level5LineContinuation2()
		{
			createWorldAndSendCommands(
					10, 20,
					"set &\n5 &\n15 &\nconstant &\n1");
			verifyConstantTileWithSetTileMethod(1, 5, 15, 9, 19, true /* verifyGetUpdatedTile */);
		}

		@Test(timeout = 1000)
		public void level5LineContinuation3()
		{
			createWorldAndSendCommands(
					10, 20,
					"s&\ne&\nt&\n &\n5&\n &\n15 &\nco&\nnst&\nant &\n1");
			verifyConstantTileWithSetTileMethod(1, 5, 15, 9, 19, true /* verifyGetUpdatedTile */);
		}

		@After
		public void close()
		{
			core.API.close();
		}
	}
	
	public static class Checkpoint3Test extends CheckpointBase
	{
		@Before
		public void initialize()
		{
			coreGolGUI.GameOfLifePanel.enableTestMode();
		}

		@Test(timeout = 1000)
		public void level1VerifyLifeTileClass()
		{
			ClassLoader classLoader = ClassLoader.getSystemClassLoader();
			Class<?> lifeTileClass = null;
			try
			{
				lifeTileClass = classLoader.loadClass("gameOfLife.LifeTile");
			}
			catch (ClassNotFoundException e)
			{
				lifeTileClass = null;
			}
			golAssertNotNull("Cannot find a class named 'LifeTile' in the project.", lifeTileClass);

			boolean interfaceFound = doesClassImplementTile("LifeTile", lifeTileClass);
			golAssertTrue("LifeTile is not supposed to implement the Tile interface.", !interfaceFound);

			GameOfLifeTest.logln("...and declares the following methods:");
			Method[] methods = lifeTileClass.getMethods();
			for (Method method : methods)
			{
				if (method.getDeclaringClass().equals(lifeTileClass))
				{
					GameOfLifeTest.logln("\t" + method);
				}
			}

			GameOfLifeTest.logln("");

			findGetNumActiveNeighborsMethod(lifeTileClass, "LifeTile");

			Method gaMethod = null;
			try
			{
				gaMethod = lifeTileClass.getMethod("getAge", (Class<?>[]) null);
			}
			catch (NoSuchMethodException e)
			{
				gaMethod = null;
			}

			golAssertNotNull("Cannot find a method named getAge taking no parameters in the LifeTile class.",
					gaMethod);
		}

		@Test(timeout = 1000)
		public void level1VerifyMonoTileClass()
		{
			ClassLoader classLoader = ClassLoader.getSystemClassLoader();
			Class<?> monoTileClass = null;
			try
			{
				monoTileClass = classLoader.loadClass("gameOfLife.MonoTile");
			}
			catch (ClassNotFoundException e)
			{
				monoTileClass = null;
			}
			golAssertNotNull("Cannot find a class named 'MonoTile' in the project.", monoTileClass);

			boolean interfaceFound = doesClassImplementTile("MonoTile", monoTileClass);
			golAssertTrue("MonoTile is supposed to implement the Tile interface.", interfaceFound);

			golAssertEquals("Your 'MonoTile' class does not have the correct super class",
					"gameOfLife.LifeTile", monoTileClass.getSuperclass().getName());

			Method gnanMethod = findGetNumActiveNeighborsMethod(monoTileClass, "MonoTile");

			String declaringClass = gnanMethod.getDeclaringClass().getName(); 
			if (!declaringClass.equals("gameOfLife.LifeTile"))
			{
				golAssertTrue("Your getNumActiveNeighbors method should be implemented in your " +
						"LifeTile class, and not overridden in a subclass.  This way, all subclasses " +
						"can reuse the same implementation.  However, it is actually overridden " +
						"in your " + declaringClass + " class.", false);
			}
		}

		@Test(timeout = 1000)
		public void level2GetNumActiveNeighborsMonoNwNeSwSeCentered()
		{
			testGetNumActiveNeighbors(
					3, 		// numRows
					3, 		// numCols
					TileType.MONO,
					1, 		// row number for the MonoTile we're testing
					1, 		// col number for (ditto)
					10100101);	// config, sets only diagonals to live 
		}

		@Test(timeout = 1000)
		public void level2GetNumActiveNeighborsMonoNwESCentered()
		{
			testGetNumActiveNeighbors(
					3, 		// numRows
					3, 		// numCols
					TileType.MONO,
					1, 		// row number for the MonoTile we're testing
					1, 		// col number for (ditto)
					10001010);	// config, sets only NW, E, S  
		}

		@Test(timeout = 1000)
		public void level2GetNumActiveNeighborsMonoNSwCentered()
		{
			testGetNumActiveNeighbors(
					3, 		// numRows
					3, 		// numCols
					TileType.MONO,
					1, 		// row number for the MonoTile we're testing
					1, 		// col number for (ditto)
					/*0*/1000100);	// config, sets only N, SW (leading zeroes confuse Java!) 
		}

		@Test(timeout = 1000)
		public void level2GetNumActiveNeighborsMonoSeCentered()
		{
			testGetNumActiveNeighbors(
					3, 		// numRows
					3, 		// numCols
					TileType.MONO,
					1, 		// row number for the MonoTile we're testing
					1, 		// col number for (ditto)
					/*0000000*/1);	// config low, sets only SE (leading zeroes confuse Java!) 
		}

		@Test(timeout = 1000)
		public void level2GetNumActiveNeighborsMonoWSCentered()
		{
			testGetNumActiveNeighbors(
					3, 		// numRows
					3, 		// numCols
					TileType.MONO,
					1, 		// row number for the MonoTile we're testing
					1, 		// col number for (ditto)
					/*000*/10010);	// config low, sets only W, S (leading zeroes confuse Java!) 
		}

		@Test(timeout = 1000)
		public void level2GetNumActiveNeighborsMonoNoneCentered()
		{
			testGetNumActiveNeighbors(
					3, 		// numRows
					3, 		// numCols
					TileType.MONO,
					1, 		// row number for the MonoTile we're testing
					1, 		// col number for (ditto)
					0);	// config, no active neighbors 
		}

		@Test(timeout = 1000)
		public void level2GetNumActiveNeighborsMonoAllCentered()
		{
			testGetNumActiveNeighbors(
					3, 		// numRows
					3, 		// numCols
					TileType.MONO,
					1, 		// row number for the MonoTile we're testing
					1, 		// col number for (ditto)
					11111111);	// config, sets only diagonals to live 
		}

		@Test(timeout = 1000)
		public void level2FillMonoAge1TilesCmdLoop()
		{
			testFillViaStdIn(TileType.MONO, 1, "", 10, 10);
		}

		@Test(timeout = 1000)
		public void level2FillMonoAge1TilesProcCmd()
		{
			testFillViaProcessCommand(TileType.MONO, 1, Color.WHITE, 11, 10);
		}

		@Test(timeout = 1000)
		public void level2FillMonoAge0TilesCmdLoop()
		{
			testFillViaStdIn(TileType.MONO, 0, "", 9, 8);
		}

		@Test(timeout = 1000)
		public void level2FillMonoAge0TilesProcCmd()
		{
			testFillViaProcessCommand(TileType.MONO, 0, Color.BLACK, 8, 9);
		}

		@Test(timeout = 1000)
		public void level2GetUpdatedTileNeighborsMonoNwNeSwSeCentered()
		{
			testGetUpdatedTileNeighbors(
					3, 		// numRows
					3, 		// numCols
					1, 		// row number for the Tile Shim (which is the one validating the calls)
					1, 		// col number for (ditto)
					10100101,	// config low, sets only diagonals to live 
					10100101);	// same, so only one config is tested
		}

		@Test(timeout = 1000)
		public void level2GetUpdatedTileResultMonoNwNeSwSeCentered()
		{
			testGetUpdatedTileResult(
					3, 			// numRows
					3, 			// numCols
					1, 			// row number for the main center Tile
					1, 			// col number for the main center Tile
					1,			// age of center tile
					TileType.MONO,
					10100101,	// neighbor config, sets only diagonals to live 
					2);			// MONO Tile use base 2 for neighbor configs
		}

		@Test(timeout = 1000)
		public void level2GetUpdatedTileNeighborsMonoNwESCentered()
		{
			testGetUpdatedTileNeighbors(
					3, 		// numRows
					3, 		// numCols
					1, 		// row number for the Tile Shim (which is the one validating the calls)
					1, 		// col number for (ditto)
					10001010,	// config low, sets only NW, E, S 
					10001010);	// same, so only one config is tested
		}

		@Test(timeout = 1000)
		public void level2GetUpdatedTileResultMonoNwESCentered()
		{
			testGetUpdatedTileResult(
					3, 		// numRows
					3, 		// numCols
					1, 		// row number for the Tile Shim (which is the one validating the calls)
					1, 		// col number for (ditto)
					1,			// age of center tile
					TileType.MONO,
					10001010,	// config, sets only NW, E, S 
					2);			// MONO Tile use base 2 for neighbor configs
		}

		@Test(timeout = 1000)
		public void level2GetUpdatedTileNeighborsMonoNSwCentered()
		{
			testGetUpdatedTileNeighbors(
					3, 		// numRows
					3, 		// numCols
					1, 		// row number for the Tile Shim (which is the one validating the calls)
					1, 		// col number for (ditto)
					/*0*/1000100,	// config low, sets only N, SW (leading zeroes confuse Java!) 
					/*0*/1000100);	// same, so only one config is tested
		}

		@Test(timeout = 1000)
		public void level2GetUpdatedTileResultMonoNSwCentered()
		{
			testGetUpdatedTileResult(
					3, 		// numRows
					3, 		// numCols
					1, 		// row number for the Tile Shim (which is the one validating the calls)
					1, 		// col number for (ditto)
					1,			// age of center tile
					TileType.MONO,
					/*0*/1000100,	// config, sets only N, SW (leading zeroes confuse Java!) 
					2);			// MONO Tile use base 2 for neighbor configs
		}

		@Test(timeout = 1000)
		public void level2GetUpdatedTileNeighborsMonoSeCentered()
		{
			testGetUpdatedTileNeighbors(
					3, 		// numRows
					3, 		// numCols
					1, 		// row number for the Tile Shim (which is the one validating the calls)
					1, 		// col number for (ditto)
					/*0000000*/1,	// config low, sets only SE (leading zeroes confuse Java!)
					/*0000000*/1);	// same, so only one config is tested
		}

		@Test(timeout = 1000)
		public void level2GetUpdatedTileResultMonoSeCentered()
		{
			testGetUpdatedTileResult(
					3, 		// numRows
					3, 		// numCols
					1, 		// row number for the Tile Shim (which is the one validating the calls)
					1, 		// col number for (ditto)
					1,			// age of center tile
					TileType.MONO,
					/*0000000*/1,	// config, sets only SE (leading zeroes confuse Java!)
					2);			// MONO Tile use base 2 for neighbor configs
		}

		@Test(timeout = 1000)
		public void level2GetUpdatedTileNeighborsMonoWSCentered()
		{
			testGetUpdatedTileNeighbors(
					3, 		// numRows
					3, 		// numCols
					1, 		// row number for the Tile Shim (which is the one validating the calls)
					1, 		// col number for (ditto)
					/*000*/10010,	// config low, sets only W, S (leading zeroes confuse Java!)
					/*000*/10010);	// same, so only one config is tested
		}

		@Test(timeout = 1000)
		public void level2GetUpdatedTileResultMonoWSCentered()
		{
			testGetUpdatedTileResult(
					3, 		// numRows
					3, 		// numCols
					1, 		// row number for the Tile Shim (which is the one validating the calls)
					1, 		// col number for (ditto)
					1,			// age of center tile
					TileType.MONO,
					/*000*/10010,	// config, sets only W, S (leading zeroes confuse Java!)
					2);			// MONO Tile use base 2 for neighbor configs
		}

		@Test(timeout = 1000)
		public void level2GetUpdatedTileNeighborsMonoNwNWCorneredNw()
		{
			testGetUpdatedTileNeighbors(
					3, 		// numRows
					3, 		// numCols
					0, 		// row number for the Tile Shim (which is the one validating the calls)
					0, 		// col number for (ditto)
					11010000,	// config low, sets only Nw, N, W
					11010000);	// same, so only one config is tested
		}

		@Test(timeout = 1000)
		public void level2GetUpdatedTileResultMonoNwNWCorneredNw()
		{
			testGetUpdatedTileResult(
					3, 		// numRows
					3, 		// numCols
					0, 		// row number for the Tile Shim (which is the one validating the calls)
					0, 		// col number for (ditto)
					1,			// age of center tile
					TileType.MONO,
					11010000,	// config, sets only Nw, N, W
					2);			// MONO Tile use base 2 for neighbor configs
		}

		@Test(timeout = 1000)
		public void level2GetUpdatedTileNeighborsMonoNESeCorneredNe()
		{
			testGetUpdatedTileNeighbors(
					3, 		// numRows
					3, 		// numCols
					0, 		// row number for the Tile Shim (which is the one validating the calls)
					2, 		// col number for (ditto)
					/*0*/1001001,	// config low, sets only N, E, Se
					/*0*/1001001);	// same, so only one config is tested
		}

		@Test(timeout = 1000)
		public void level2GetUpdatedTileResultMonoNESeCorneredNe()
		{
			testGetUpdatedTileResult(
					3, 		// numRows
					3, 		// numCols
					0, 		// row number for the Tile Shim (which is the one validating the calls)
					2, 		// col number for (ditto)
					1,			// age of center tile
					TileType.MONO,
					/*0*/1001001,	// config, sets only N, E, Se
					2);			// MONO Tile use base 2 for neighbor configs
		}

		@Test(timeout = 1000)
		public void level2GetUpdatedTileNeighborsMonoNeWESwSeCorneredSe()
		{
			testGetUpdatedTileNeighbors(
					3, 		// numRows
					3, 		// numCols
					2, 		// row number for the Tile Shim (which is the one validating the calls)
					2, 		// col number for (ditto)
					/*00*/110101,	// config low, sets only Ne, W, E, Sw, Se
					/*00*/110101);	// same, so only one config is tested
		}

		@Test(timeout = 1000)
		public void level2GetUpdatedTileResultMonoNeWESwSeCorneredSe()
		{
			testGetUpdatedTileResult(
					3, 		// numRows
					3, 		// numCols
					2, 		// row number for the Tile Shim (which is the one validating the calls)
					2, 		// col number for (ditto)
					1,			// age of center tile
					TileType.MONO,
					/*00*/110101,	// config, sets only Ne, W, E, Sw, Se
					2);			// MONO Tile use base 2 for neighbor configs
		}

		@Test(timeout = 1000)
		public void level2GetUpdatedTileNeighborsMonoWSwCorneredSw()
		{
			testGetUpdatedTileNeighbors(
					3, 		// numRows
					3, 		// numCols
					2, 		// row number for the Tile Shim (which is the one validating the calls)
					0, 		// col number for (ditto)
					/*000*/10100,	// config low, sets only W, Sw
					/*000*/10100);	// same, so only one config is tested
		}

		@Test(timeout = 1000)
		public void level2GetUpdatedTileResultMonoWSwCorneredSw()
		{
			testGetUpdatedTileResult(
					3, 		// numRows
					3, 		// numCols
					2, 		// row number for the Tile Shim (which is the one validating the calls)
					0, 		// col number for (ditto)
					1,			// age of center tile
					TileType.MONO,
					/*000*/10100,	// config, sets only W, Sw
					2);			// MONO Tile use base 2 for neighbor configs
		}

		@Test(timeout = 1000)
		public void level3GetUpdatedTileNeighborsMonoAllCombosCentered()
		{
			testGetUpdatedTileNeighbors(
					3, 		// numRows
					3, 		// numCols
					1, 		// row number for the Tile Shim (which is the one validating the calls)
					1, 		// col number for (ditto)
					0, 		// lowest neighbor configuration to verify
					11111111);	// highest neighbor configuration to verify
		}

		@Test(timeout = 1000)
		public void level3GetUpdatedTileNeighborsMonoAllCombosCorneredNw()
		{
			testGetUpdatedTileNeighbors(
					3, 		// numRows
					3, 		// numCols
					0, 		// row number for the Tile Shim (which is the one validating the calls)
					0, 		// col number for (ditto)
					0, 		// lowest neighbor configuration to verify
					11111111);	// highest neighbor configuration to verify
		}

		@Test(timeout = 1000)
		public void level3GetUpdatedTileNeighborsMonoAllCombosCorneredNe()
		{
			testGetUpdatedTileNeighbors(
					3, 		// numRows
					3, 		// numCols
					0, 		// row number for the Tile Shim (which is the one validating the calls)
					2, 		// col number for (ditto)
					0, 		// lowest neighbor configuration to verify
					11111111);	// highest neighbor configuration to verify
		}

		@Test(timeout = 1000)
		public void level3GetUpdatedTileNeighborsMonoAllCombosCorneredSw()
		{
			testGetUpdatedTileNeighbors(
					3, 		// numRows
					3, 		// numCols
					2, 		// row number for the Tile Shim (which is the one validating the calls)
					0, 		// col number for (ditto)
					0, 		// lowest neighbor configuration to verify
					11111111);	// highest neighbor configuration to verify
		}

		@Test(timeout = 1000)
		public void level3GetUpdatedTileNeighborsMonoAllCombosCorneredSe()
		{
			testGetUpdatedTileNeighbors(
					3, 		// numRows
					3, 		// numCols
					2, 		// row number for the Tile Shim (which is the one validating the calls)
					2, 		// col number for (ditto)
					0, 		// lowest neighbor configuration to verify
					11111111);	// highest neighbor configuration to verify
		}

		@Test(timeout = 1000)
		public void level4EvolveMonoWithNwNeSwSeCentered()
		{
			GameOfLifeTest.logln("Testing 4 live neighbors turns MonoTile from active to still active");
			int rows = 3;
			int cols = 3;
			testSetAndEvolveMultipleTilesWithStdIn(
					rows, cols,
					1,				// numSteps
					tileInfosFromConstantNeighborConfig(
							TileType.MONO, 
							1, 		// Age of MONO tile to evolve
							1, 1,	// Row,Col of MONO tile 
							10100101));	// Neighbors: NW, NE, SW, SE
		}

		@Test(timeout = 1000)
		public void level4EvolveMonoWithNeSCentered()
		{
			GameOfLifeTest.logln("Testing 2 live neighbor turns MonoTile from active to dormant");
			int rows = 3;
			int cols = 3;
			testSetAndEvolveMultipleTilesWithStdIn(
					rows, cols,
					1,				// numSteps
					tileInfosFromConstantNeighborConfig(
							TileType.MONO, 
							1, 		// Age of MONO tile to evolve
							1, 1,	// Row,Col of MONO tile 
							/*00*/100010));	// Neighbors: NE, S
		}

		@Test(timeout = 1000)
		public void level4EvolveMonoWithNWECentered()
		{
			GameOfLifeTest.logln("Testing 3 live neighbors turns MonoTile from dormant to active");
			int rows = 3;
			int cols = 3;
			testSetAndEvolveMultipleTilesWithStdIn(
					rows, cols,
					1,				// numSteps
					tileInfosFromConstantNeighborConfig(
							TileType.MONO, 
							0, 		// Age of MONO tile to evolve
							1, 1,	// Row,Col of MONO tile 
							/*0*/1011000));	// Neighbors: N, W, E
		}

		@Test(timeout = 1000)
		public void level4EvolveMonoWithSSeCentered()
		{
			GameOfLifeTest.logln("Testing 2 live neighbors turns MonoTile from dormant to still dormant");
			int rows = 3;
			int cols = 3;
			testSetAndEvolveMultipleTilesWithStdIn(
					rows, cols,
					1,				// numSteps
					tileInfosFromConstantNeighborConfig(
							TileType.MONO, 
							0, 		// Age of MONO tile to evolve
							1, 1,	// Row,Col of MONO tile 
							/*000000*/11));	// Neighbors: S, SE
		}

		private TileInfo[] getMonoGlider(int row, int col)
		{
			return new TileInfo[]
					{
							new TileInfo(TileType.MONO, 1, row, col + 1),	
							new TileInfo(TileType.MONO, 1, row + 1, col + 2),	
							new TileInfo(TileType.MONO, 1, row + 2, col),	
							new TileInfo(TileType.MONO, 1, row + 2, col + 1),	
							new TileInfo(TileType.MONO, 1, row + 2, col + 2),	
					};
		}

		@Test(timeout = 1000)
		public void level4EvolveMonoGlider1Step()
		{
			GameOfLifeTest.logln("Evolving a glider of MonoTiles for 1 step");
			int rows = 10;
			int cols = 10;
			testSetAndEvolveMultipleTilesWithStdIn(
					rows, cols,
					TileType.MONO,	// Fill with MonoTiles of age 0 first
					1,				// numStepsPerEvolveCommand
					1,				// numEvolveCommands
					getMonoGlider(1, 1));
		}

		@Test(timeout = 1000)
		public void level4EvolveMonoBorderGlider1Step()
		{
			GameOfLifeTest.logln("Evolving a glider of MonoTiles at the lower-right corner for 1 step");
			int rows = 10;
			int cols = 10;
			testSetAndEvolveMultipleTilesWithStdIn(
					rows, cols,
					TileType.MONO,	// Fill with MonoTiles of age 0 first
					1,				// numStepsPerEvolveCommand
					1,				// numEvolveCommands
					getMonoGlider(7, 7));
		}

		@Test(timeout = 1000)
		public void level5EvolveMonoOscillator5Steps()
		{
			GameOfLifeTest.logln("Evolving line of MonoTiles of length 3 for 5 steps");
			int rows = 5;
			int cols = 5;
			testSetAndEvolveMultipleTilesWithStdIn(
					rows, cols,
					TileType.MONO,	// Fill with MonoTiles of age 0 first
					5,				// numStepsPerEvolveCommand
					1,				// numEvolveCommands
					new TileInfo[]
							{
									new TileInfo(TileType.MONO, 1, 1, 1),	
									new TileInfo(TileType.MONO, 1, 2, 1),	
									new TileInfo(TileType.MONO, 1, 3, 1),	
							});
		}

		@Test(timeout = 1000)
		public void level5EvolveMonoOscillator6Steps()
		{
			GameOfLifeTest.logln("Evolving line of MonoTiles of length 3 for 6 steps");
			int rows = 5;
			int cols = 5;
			testSetAndEvolveMultipleTilesWithStdIn(
					rows, cols,
					TileType.MONO,	// Fill with MonoTiles of age 0 first
					6,				// numStepsPerEvolveCommand
					1,				// numEvolveCommands
					new TileInfo[]
							{
									new TileInfo(TileType.MONO, 1, 1, 1),	
									new TileInfo(TileType.MONO, 1, 2, 1),	
									new TileInfo(TileType.MONO, 1, 3, 1),	
							});
		}

		@Test(timeout = 1000)
		public void level5EvolveMonoGlider2Steps()
		{
			GameOfLifeTest.logln("Evolving a glider of MonoTiles for 2 steps");
			int rows = 10;
			int cols = 10;
			testSetAndEvolveMultipleTilesWithStdIn(
					rows, cols,
					TileType.MONO,	// Fill with MonoTiles of age 0 first
					2,				// numStepsPerEvolveCommand
					1,				// numEvolveCommands
					getMonoGlider(1, 1));
		}

		@Test(timeout = 1000)
		public void level5EvolveMonoGlider3Steps()
		{
			GameOfLifeTest.logln("Evolving a glider of MonoTiles for 3 steps");
			int rows = 10;
			int cols = 10;
			testSetAndEvolveMultipleTilesWithStdIn(
					rows, cols,
					TileType.MONO,	// Fill with MonoTiles of age 0 first
					3,				// numStepsPerEvolveCommand
					1,				// numEvolveCommands
					getMonoGlider(1, 1));
		}

		@Test(timeout = 1000)
		public void level5EvolveMonoGlider4Steps()
		{
			GameOfLifeTest.logln("Evolving a glider of MonoTiles for 4 steps");
			int rows = 10;
			int cols = 10;
			testSetAndEvolveMultipleTilesWithStdIn(
					rows, cols,
					TileType.MONO,	// Fill with MonoTiles of age 0 first
					4,				// numStepsPerEvolveCommand
					1,				// numEvolveCommands
					getMonoGlider(1, 1));
		}

		@Test(timeout = 1000)
		public void level5EvolveMonoBorderGlider2Steps()
		{
			GameOfLifeTest.logln("Evolving a glider of MonoTiles at the lower-right corner for 2 steps");
			int rows = 10;
			int cols = 10;
			testSetAndEvolveMultipleTilesWithStdIn(
					rows, cols,
					TileType.MONO,	// Fill with MonoTiles of age 0 first
					2,				// numStepsPerEvolveCommand
					1,				// numEvolveCommands
					getMonoGlider(7, 7));
		}

		@Test(timeout = 1000)
		public void level5EvolveMonoBorderGlider3Steps()
		{
			GameOfLifeTest.logln("Evolving a glider of MonoTiles at the lower-right corner for 3 steps");
			int rows = 10;
			int cols = 10;
			testSetAndEvolveMultipleTilesWithStdIn(
					rows, cols,
					TileType.MONO,	// Fill with MonoTiles of age 0 first
					3,				// numStepsPerEvolveCommand
					1,				// numEvolveCommands
					getMonoGlider(7, 7));
		}

		@Test(timeout = 1000)
		public void level5EvolveMonoBorderGlider4Steps()
		{
			GameOfLifeTest.logln("Evolving a glider of MonoTiles at the lower-right corner for 4 steps");
			int rows = 10;
			int cols = 10;
			testSetAndEvolveMultipleTilesWithStdIn(
					rows, cols,
					TileType.MONO,	// Fill with MonoTiles of age 0 first
					4,				// numStepsPerEvolveCommand
					1,				// numEvolveCommands
					getMonoGlider(7, 7));
		}

		@Test(timeout = 1000)
		public void level5EvolveMonoBorderGliderMultipleSteps()
		{
			GameOfLifeTest.logln("Evolving a glider of MonoTiles at the lower-right corner for many steps");
			int rows = 10;
			int cols = 10;
			testSetAndEvolveMultipleTilesWithProcessCommand(
					rows, cols,
					TileType.MONO,	// Fill with MonoTiles of age 0 first
					12,				// numTotalEvolveSteps
					getMonoGlider(7, 7));
		}

		@Test(timeout = 5000)
		public void level5EvolveMonoAllCombinations()
		{
			testSetAndEvolveMultipleTilesInMultipleConfigs(
					3, 3, 			// rows, cols,
					TileType.MONO,	// fillTileType,
					1, 1, 			// rowCenter, colCenter
					TileType.MONO,	// centerType
					1,				// center tile's starting age
					TileType.MONO,	// neighbor tile type
					1,				// numTotalEvolveStepsPerConfig,
					0,				// neighborConfigLow
					11111111,		// neighborConfigHigh
					2,				// MonoTile tests have base 2 neighbor configs
					1);				// step size
		}

		@After
		public void close()
		{
			core.API.close();
		}
	}

	public static class Checkpoint4Test extends CheckpointBase
	{
		@Before
		public void initialize()
		{
			coreGolGUI.GameOfLifePanel.enableTestMode();
		}

		@Test(timeout = 1000)
		public void level1CopycatMirrorSimple1()
		{
			GameOfLifeTest.logln("Testing setCopycats mirror on 1x2 grid, with one RainbowTile");
			testSetCopycats(
					1, 2, // rows, cols
					new TileInfo[]
							{
									new TileInfo(TileType.RAINBOW, 2, 0, 0),
							},
							true); // setCopycatsFirst
		}

		@Test(timeout = 1000)
		public void level1CopycatMirrorSimple2()
		{
			GameOfLifeTest.logln("Testing setCopycats mirror on 1x2 grid, with one RainbowTile");
			testSetCopycats(
					1, 2, // rows, cols
					new TileInfo[]
							{
									new TileInfo(TileType.RAINBOW, 3, 0, 0),
							},
							false); // setCopycatsFirst

		}

		@Test(timeout = 1000)
		public void level1SetShapeOscillator1()
		{
			GameOfLifeTest.logln("Testing setShape oscillator surrounded by Constant Tiles.");

			testSetShape(
					4, 5,				// grid total rows, cols
					TileType.CONSTANT,
					1, 1,				// shape starting row, col
					"oscillator",
					SHAPE_OSCILLATOR);
		}


		@Test(timeout = 1000)
		public void level1SetShapeOscillator2()
		{
			GameOfLifeTest.logln("Testing setShape oscillator surrounded by Mono Tiles.");

			testSetShape(
					5, 7,				// grid total rows, cols
					TileType.MONO,
					2, 3,				// shape starting row, col
					"oscillator",
					SHAPE_OSCILLATOR);
		}

		@Test(timeout = 1000)
		public void level1SetShapeGlider1()
		{
			GameOfLifeTest.logln("Testing setShape glider surrounded by Constant Tiles.");

			testSetShape(
					5, 5,				// grid total rows, cols
					TileType.CONSTANT,
					1, 1,				// shape starting row, col
					"glider",
					SHAPE_GLIDER);
		}		

		@Test(timeout = 1000)
		public void level1SetShapeGlider2()
		{
			GameOfLifeTest.logln("Testing setShape glider surrounded by Mono Tiles.");

			testSetShape(
					8, 6,				// grid total rows, cols
					TileType.CONSTANT,
					4, 2,				// shape starting row, col
					"glider",
					SHAPE_GLIDER);
		}	

		@Test(timeout = 1000)
		public void level2CopycatMirrorMedium1()
		{
			GameOfLifeTest.logln("Testing setCopycats mirror on 3x6 grid, with various tiles");
			testSetCopycats(
					3, 6, // rows, cols
					new TileInfo[]
							{
									new TileInfo(TileType.RAINBOW, 4, 0, 0),
									new TileInfo(TileType.CONSTANT, 1, 1, 1),
									new TileInfo(TileType.MONO, 1, 2, 2),
							},
							true); // setCopycatsFirst

		}

		@Test(timeout = 1000)
		public void level2CopycatMirrorMedium2()
		{
			GameOfLifeTest.logln("Testing setCopycats mirror on 3x7 grid, with various tiles");
			testSetCopycats(
					3, 7, // rows, cols
					new TileInfo[]
							{
									new TileInfo(TileType.RAINBOW, 5, 0, 2),
									new TileInfo(TileType.CONSTANT, 3, 1, 2),
									new TileInfo(TileType.MONO, 0, 2, 0),
									new TileInfo(TileType.MONO, 1, 2, 3),
							},
							false); // setCopycatsFirst

		}

		@Test(timeout = 1000)
		public void level3CopycatMirrorAndEvolve1()
		{
			int rows = 5;
			int cols = 10;
			GameOfLifeTest.logln("Testing setCopycats mirror with 1-step evolve on a " + rows + "x" + cols + " grid");

			testSetCopycatsAndEvolve(
					rows, cols,
					TileType.MONO,	// Fill with MonoTiles of age 0 first
					1,				// numStepsPerEvolveCommand
					1,				// numEvolveCommands
					new TileInfo[]
							{
									new TileInfo(TileType.MONO, 1, 1, 2),	
									new TileInfo(TileType.MONO, 1, 2, 2),	
									new TileInfo(TileType.MONO, 1, 3, 2),	
							},
							true,			// setCopycatsFirst
							CopycatPattern.MIRROR);
		}

		@Test(timeout = 1000)
		public void level3CopycatMirrorAndEvolve2()
		{
			int rows = 5;
			int cols = 10;
			GameOfLifeTest.logln("Testing setCopycats mirror with 2-step evolve on a " + rows + "x" + cols + " grid");

			testSetCopycatsAndEvolve(
					rows, cols,
					TileType.MONO,	// Fill with MonoTiles of age 0 first
					2,				// numStepsPerEvolveCommand
					1,				// numEvolveCommands
					new TileInfo[]
							{
									new TileInfo(TileType.MONO, 1, 1, 2),	
									new TileInfo(TileType.MONO, 1, 2, 2),	
									new TileInfo(TileType.MONO, 1, 3, 2),	
							},
							false,			// setCopycatsFirst
							CopycatPattern.MIRROR);
		}

		@Test(timeout = 1000)
		public void level3CopycatMirrorAndEvolve3()
		{
			int rows = 5;
			int cols = 8;
			GameOfLifeTest.logln("Testing setCopycats mirror with 5-step evolve on a " + rows + "x" + cols + " grid with some Copycat tiles touching Mono tiles");

			testSetCopycatsAndEvolve(
					rows, cols,
					TileType.MONO,	// Fill with MonoTiles of age 0 first
					5,				// numStepsPerEvolveCommand
					1,				// numEvolveCommands
					new TileInfo[]
							{
									new TileInfo(TileType.MONO, 1, 1, 1),	
									new TileInfo(TileType.MONO, 1, 2, 1),	
									new TileInfo(TileType.MONO, 1, 3, 1),	
							},
							true,			// setCopycatsFirst
							CopycatPattern.MIRROR);
		}

		@After
		public void close()
		{
			core.API.close();
		}
	}

	public static class FinalTest extends CheckpointBase
	{
		@Before
		public void initialize()
		{
			coreGolGUI.GameOfLifePanel.enableTestMode();
		}

		@Test(timeout = 1000)
		public void level1VerifyImmigrationTileClass()
		{
			ClassLoader classLoader = ClassLoader.getSystemClassLoader();
			Class<?> immigrationTileClass = null;
			try
			{
				immigrationTileClass = classLoader.loadClass("gameOfLife.ImmigrationTile");
			}
			catch (ClassNotFoundException e)
			{
				immigrationTileClass = null;
			}
			golAssertNotNull("Cannot find a class named 'ImmigrationTile' in the project.", immigrationTileClass);

			boolean interfaceFound = doesClassImplementTile("ImmigrationTile", immigrationTileClass);
			golAssertTrue("ImmigrationTile is supposed to implement the Tile interface.", interfaceFound);

			golAssertEquals("Your 'ImmigrationTile' class does not have the correct super class",
					"gameOfLife.LifeTile", immigrationTileClass.getSuperclass().getName());

			Method gnanMethod = findGetNumActiveNeighborsMethod(immigrationTileClass, "ImmigrationTile");

			String declaringClass = gnanMethod.getDeclaringClass().getName(); 
			if (!declaringClass.equals("gameOfLife.LifeTile"))
			{
				golAssertTrue("Your getNumActiveNeighbors method should be implemented in your " +
						"LifeTile class, and not overridden in a subclass.  This way, all subclasses " +
						"can reuse the same implementation.  However, it is actually overridden " +
						"in your " + declaringClass + " class.", false);
			}
		}

		@Test(timeout = 1000)
		public void level2FillImmigrationAge0TilesCmdLoop()
		{
			testFillViaStdIn(TileType.IMMIGRATION, 0, "", 10, 11);
		}

		@Test(timeout = 1000)
		public void level2FillImmigrationAge0TilesProcCmd()
		{
			int age = 0;
			testFillViaProcessCommand(TileType.IMMIGRATION, age, TileInfo.immigrationTileConstructedColorFromAge(age), 11, 10);
		}

		@Test(timeout = 1000)
		public void level2FillImmigrationAge1TilesCmdLoop()
		{
			testFillViaStdIn(TileType.IMMIGRATION, 1, "", 8, 9);
		}

		@Test(timeout = 1000)
		public void level2FillImmigrationAge1TilesProcCmd()
		{
			int age = 1;
			testFillViaProcessCommand(TileType.IMMIGRATION, age, TileInfo.immigrationTileConstructedColorFromAge(age), 9, 8);
		}

		@Test(timeout = 1000)
		public void level2FillImmigrationAge2TilesCmdLoop()
		{
			testFillViaStdIn(TileType.IMMIGRATION, 2, "", 7, 8);
		}

		@Test(timeout = 1000)
		public void level2FillImmigrationAge2TilesProcCmd()
		{
			int age = 2;
			testFillViaProcessCommand(TileType.IMMIGRATION, age, TileInfo.immigrationTileConstructedColorFromAge(age), 8, 7);
		}

		@Test(timeout = 1000)
		public void level2GetNumActiveNeighborsImmigrationNwNeSwSeCentered()
		{
			testGetNumActiveNeighbors(
					3, 		// numRows
					3, 		// numCols
					TileType.IMMIGRATION,
					1, 		// row number for the ImmigrationTile we're testing
					1, 		// col number for (ditto)
					10100101);	// config, sets only diagonals to live 
		}

		@Test(timeout = 1000)
		public void level2GetNumActiveNeighborsImmigrationNwESCentered()
		{
			testGetNumActiveNeighbors(
					3, 		// numRows
					3, 		// numCols
					TileType.IMMIGRATION,
					1, 		// row number for the ImmigrationTile we're testing
					1, 		// col number for (ditto)
					10001010);	// config, sets only NW, E, S  
		}

		@Test(timeout = 1000)
		public void level2GetNumActiveNeighborsImmigrationNSwCentered()
		{
			testGetNumActiveNeighbors(
					3, 		// numRows
					3, 		// numCols
					TileType.IMMIGRATION,
					1, 		// row number for the ImmigrationTile we're testing
					1, 		// col number for (ditto)
					/*0*/1000100);	// config, sets only N, SW (leading zeroes confuse Java!) 
		}

		@Test(timeout = 1000)
		public void level2GetNumActiveNeighborsImmigrationSeCentered()
		{
			testGetNumActiveNeighbors(
					3, 		// numRows
					3, 		// numCols
					TileType.IMMIGRATION,
					1, 		// row number for the ImmigrationTile we're testing
					1, 		// col number for (ditto)
					/*0000000*/1);	// config low, sets only SE (leading zeroes confuse Java!) 
		}

		@Test(timeout = 1000)
		public void level2GetNumActiveNeighborsImmigrationWSCentered()
		{
			testGetNumActiveNeighbors(
					3, 		// numRows
					3, 		// numCols
					TileType.IMMIGRATION,
					1, 		// row number for the ImmigrationTile we're testing
					1, 		// col number for (ditto)
					/*000*/10010);	// config low, sets only W, S (leading zeroes confuse Java!) 
		}

		@Test(timeout = 1000)
		public void level2GetNumActiveNeighborsImmigrationNoneCentered()
		{
			testGetNumActiveNeighbors(
					3, 		// numRows
					3, 		// numCols
					TileType.IMMIGRATION,
					1, 		// row number for the ImmigrationTile we're testing
					1, 		// col number for (ditto)
					0);	// config, no active neighbors 
		}

		@Test(timeout = 1000)
		public void level2GetNumActiveNeighborsImmigrationAllCentered()
		{
			testGetNumActiveNeighbors(
					3, 		// numRows
					3, 		// numCols
					TileType.IMMIGRATION,
					1, 		// row number for the ImmigrationTile we're testing
					1, 		// col number for (ditto)
					11111111);	// config, sets only diagonals to live 
		}
		
		@Test(timeout = 1000)
		public void level3GetUpdatedTileResultImmigrationWithNwNeSwSeCentered()
		{
			GameOfLifeTest.logln("Testing 4 live neighbors turns ImmigrationTile from active to still active");
			
			testGetUpdatedTileResult(
					3, 		// numRows
					3, 		// numCols
					1, 		// row number for the Tile Shim (which is the one validating the calls)
					1, 		// col number for (ditto)
					1,		// age of center tile
					TileType.IMMIGRATION,
					10100102,	// Neighbors: NW, NE, SW, SE
					3);			// Immigration Tile use base 3 for neighbor configs
		}

		@Test(timeout = 1000)
		public void level3GetUpdatedTileResultImmigrationWithNeSCentered()
		{
			GameOfLifeTest.logln("Testing 2 live neighbors turns ImmigrationTile from active to dormant");

			testGetUpdatedTileResult(
					3, 				// numRows
					3, 				// numCols
					1, 				// row number for the Tile Shim (which is the one validating the calls)
					1, 				// col number for (ditto)
					1,				// age of center tile
					TileType.IMMIGRATION,
					/*00*/100010,	// Neighbors: NE, S
					3);				// Immigration Tile use base 3 for neighbor configs
		}

		@Test(timeout = 1000)
		public void level3GetUpdatedTileResultImmigrationWithNWECentered()
		{
			GameOfLifeTest.logln("Testing 3 live neighbors turns ImmigrationTile from dormant to active");

			testGetUpdatedTileResult(
					3, 				// numRows
					3, 				// numCols
					1, 				// row number for the Tile Shim (which is the one validating the calls)
					1, 				// col number for (ditto)
					0,				// age of center tile
					TileType.IMMIGRATION,
					/*0*/2012000,	// Neighbors: N, W, E
					3);				// Immigration Tile use base 3 for neighbor configs
		}

		@Test(timeout = 1000)
		public void level3GetUpdatedTileResultImmigrationWithSSeCentered()
		{
			GameOfLifeTest.logln("Testing 2 live neighbors turns ImmigrationTile from dormant to still dormant");

			testGetUpdatedTileResult(
					3, 				// numRows
					3, 				// numCols
					1, 				// row number for the Tile Shim (which is the one validating the calls)
					1, 				// col number for (ditto)
					0,				// age of center tile
					TileType.IMMIGRATION,
					/*000000*/11,	// Neighbors: S, SE
					3);				// Immigration Tile use base 3 for neighbor configs			
		}
		
		@Test(timeout = 1000)
		public void level4EvolveImmigrationWithNwNeSwSeCentered()
		{
			GameOfLifeTest.logln("Testing 4 live neighbors turns ImmigrationTile from active to still active");
			int rows = 3;
			int cols = 3;
			testSetAndEvolveMultipleTilesWithStdIn(
					rows, cols,
					1,				// numSteps
					tileInfosFromNeighborConfig(
							TileType.IMMIGRATION, 	// center tile type
							TileType.IMMIGRATION, 	// neighbor tile type
							1, 		// Age of immigration tile to evolve
							1, 1,	// Row,Col of immigration tile 
							10100102));	// Neighbors: NW, NE, SW, SE
		}

		@Test(timeout = 1000)
		public void level4EvolveImmigrationWithNeSCentered()
		{
			GameOfLifeTest.logln("Testing 2 live neighbors turns ImmigrationTile from active to dormant");
			int rows = 3;
			int cols = 3;
			testSetAndEvolveMultipleTilesWithStdIn(
					rows, cols,
					1,				// numSteps
					tileInfosFromNeighborConfig(
							TileType.IMMIGRATION, 	// center tile type
							TileType.IMMIGRATION, 	// neighbor tile type
							1, 		// Age of immigration tile to evolve
							1, 1,	// Row,Col of immigration tile 
							/*00*/100010));	// Neighbors: NE, S
		}

		@Test(timeout = 1000)
		public void level4EvolveImmigrationWithNWECentered()
		{
			GameOfLifeTest.logln("Testing 3 live neighbors turns ImmigrationTile from dormant to active");
			int rows = 3;
			int cols = 3;
			testSetAndEvolveMultipleTilesWithStdIn(
					rows, cols,
					1,				// numSteps
					tileInfosFromNeighborConfig(
							TileType.IMMIGRATION, 	// center tile type
							TileType.IMMIGRATION, 	// neighbor tile type
							0, 		// Age of immigration tile to evolve
							1, 1,	// Row,Col of immigration tile 
							/*0*/2012000));	// Neighbors: N, W, E
		}

		@Test(timeout = 1000)
		public void level4EvolveImmigrationWithSSeCentered()
		{
			GameOfLifeTest.logln("Testing 2 live neighbors turns ImmigrationTile from dormant to still dormant");
			int rows = 3;
			int cols = 3;
			testSetAndEvolveMultipleTilesWithStdIn(
					rows, cols,
					1,				// numSteps
					tileInfosFromNeighborConfig(
							TileType.IMMIGRATION, 	// center tile type
							TileType.IMMIGRATION, 	// neighbor tile type
							0, 		// Age of immigration tile to evolve
							1, 1,	// Row,Col of immigration tile 
							/*000000*/11));	// Neighbors: S, SE
		}
		
		private TileInfo[] getImmigrationGlider(int row, int col)
		{
			return new TileInfo[]
					{
							new TileInfo(TileType.IMMIGRATION, 2, row, col + 1),	
							new TileInfo(TileType.IMMIGRATION, 2, row + 1, col + 2),	
							new TileInfo(TileType.IMMIGRATION, 1, row + 2, col),	
							new TileInfo(TileType.IMMIGRATION, 1, row + 2, col + 1),	
							new TileInfo(TileType.IMMIGRATION, 2, row + 2, col + 2),	
					};
		}

		@Test(timeout = 1000)
		public void level4EvolveImmigrationGlider1Step()
		{
			logln("Evolving a glider of ImmigrationTiles for 1 step");
			int rows = 10;
			int cols = 10;
			testSetAndEvolveMultipleTilesWithStdIn(
					rows, cols,
					TileType.IMMIGRATION,	// Fill with ImmigrationTiles of age 0 first
					1,						// numStepsPerEvolveCommand
					1,						// numEvolveCommands
					getImmigrationGlider(1, 1));
		}

		@Test(timeout = 1000)
		public void level4EvolveImmigrationBorderGlider1Step()
		{
			logln("Evolving a glider of ImmigrationTiles at the lower-right corner for 1 step");
			int rows = 10;
			int cols = 10;
			testSetAndEvolveMultipleTilesWithStdIn(
					rows, cols,
					TileType.IMMIGRATION,	// Fill with ImmigrationTiles of age 0 first
					1,						// numStepsPerEvolveCommand
					1,						// numEvolveCommands
					getImmigrationGlider(7, 7));
		}

		@Test(timeout = 5000)
		public void level5EvolveImmigrationAge0AllCombinations()
		{
			testSetAndEvolveMultipleTilesInMultipleConfigs(
					3, 3, 					// rows, cols,
					TileType.IMMIGRATION,	// fillTileType,
					1, 1, 					// rowCenter, colCenter
					TileType.IMMIGRATION,	// centerType
					0,						// center tile's starting age
					TileType.IMMIGRATION,	// neighbor tile type
					1,						// numTotalEvolveStepsPerConfig,
					0,						// neighborConfigLow
					22222222,				// neighborConfigHigh
					3,						// ImmigrationTile tests have base 3 neighbor configs
					1);						// step size
		}

		@Test(timeout = 5000)
		public void level5EvolveImmigrationAge1AllCombinations()
		{
			testSetAndEvolveMultipleTilesInMultipleConfigs(
					3, 3, 					// rows, cols,
					TileType.IMMIGRATION,	// fillTileType,
					1, 1, 					// rowCenter, colCenter
					TileType.IMMIGRATION,	// centerType
					1,						// center tile's starting age
					TileType.IMMIGRATION,	// neighbor tile type
					1,						// numTotalEvolveStepsPerConfig,
					0,						// neighborConfigLow
					22222222,				// neighborConfigHigh
					3,						// ImmigrationTile tests have base 3 neighbor configs
					1);						// step size
		}

		@Test(timeout = 5000)
		public void level5EvolveImmigrationAge2AllCombinations()
		{
			testSetAndEvolveMultipleTilesInMultipleConfigs(
					3, 3, 					// rows, cols,
					TileType.IMMIGRATION,	// fillTileType,
					1, 1, 					// rowCenter, colCenter
					TileType.IMMIGRATION,	// centerType
					2,						// center tile's starting age
					TileType.IMMIGRATION,	// neighbor tile type
					1,						// numTotalEvolveStepsPerConfig,
					0,						// neighborConfigLow
					22222222,				// neighborConfigHigh
					3,						// ImmigrationTile tests have base 3 neighbor configs
					1);						// step size
		}

		@Test(timeout = 1000)
		public void level5EvolveImmigrationGlider2Steps()
		{
			GameOfLifeTest.logln("Evolving a glider of ImmigrationTiles for 2 steps");
			int rows = 10;
			int cols = 10;
			testSetAndEvolveMultipleTilesWithStdIn(
					rows, cols,
					TileType.IMMIGRATION,	// Fill with ImmigrationTiles of age 0 first
					2,						// numStepsPerEvolveCommand
					1,						// numEvolveCommands
					getImmigrationGlider(1, 1));
		}

		@Test(timeout = 1000)
		public void level5EvolveImmigrationGlider3Steps()
		{
			GameOfLifeTest.logln("Evolving a glider of ImmigrationTiles for 3 steps");
			int rows = 10;
			int cols = 10;
			testSetAndEvolveMultipleTilesWithStdIn(
					rows, cols,
					TileType.IMMIGRATION,	// Fill with ImmigrationTiles of age 0 first
					3,						// numStepsPerEvolveCommand
					1,						// numEvolveCommands
					getImmigrationGlider(1, 1));
		}

		@Test(timeout = 1000)
		public void level5EvolveImmigrationGlider4Steps()
		{
			GameOfLifeTest.logln("Evolving a glider of ImmigrationTiles for 4 steps");
			int rows = 10;
			int cols = 10;
			testSetAndEvolveMultipleTilesWithStdIn(
					rows, cols,
					TileType.IMMIGRATION,	// Fill with ImmigrationTiles of age 0 first
					4,						// numStepsPerEvolveCommand
					1,						// numEvolveCommands
					getImmigrationGlider(1, 1));
		}

		@Test(timeout = 1000)
		public void level5EvolveImmigrationBorderGlider2Steps()
		{
			GameOfLifeTest.logln("Evolving a glider of ImmigrationTiles at the lower-right corner for 2 steps");
			int rows = 10;
			int cols = 10;
			testSetAndEvolveMultipleTilesWithStdIn(
					rows, cols,
					TileType.IMMIGRATION,	// Fill with ImmigrationTiles of age 0 first
					2,						// numStepsPerEvolveCommand
					1,						// numEvolveCommands
					getImmigrationGlider(7, 7));
		}

		@Test(timeout = 1000)
		public void level5EvolveImmigrationBorderGlider3Steps()
		{
			GameOfLifeTest.logln("Evolving a glider of ImmigrationTiles at the lower-right corner for 3 steps");
			int rows = 10;
			int cols = 10;
			testSetAndEvolveMultipleTilesWithStdIn(
					rows, cols,
					TileType.IMMIGRATION,	// Fill with ImmigrationTiles of age 0 first
					3,						// numStepsPerEvolveCommand
					1,						// numEvolveCommands
					getImmigrationGlider(7, 7));
		}
		@Test(timeout = 1000)
		public void level5EvolveImmigrationBorderGlider4Steps()
		{
			GameOfLifeTest.logln("Evolving a glider of ImmigrationTiles at the lower-right corner for 4 steps");
			int rows = 10;
			int cols = 10;
			testSetAndEvolveMultipleTilesWithStdIn(
					rows, cols,
					TileType.IMMIGRATION,	// Fill with ImmigrationTiles of age 0 first
					4,						// numStepsPerEvolveCommand
					1,						// numEvolveCommands
					getImmigrationGlider(7, 7));
		}

		@Test(timeout = 1000)
		public void level5EvolveImmigrationBorderGliderMultipleSteps()
		{
			GameOfLifeTest.logln("Evolving a glider of ImmigrationTiles at the lower-right corner for many steps");
			int rows = 10;
			int cols = 10;
			testSetAndEvolveMultipleTilesWithProcessCommand(
					rows, cols,
					TileType.IMMIGRATION,	// Fill with ImmigrationTiles of age 0 first
					12,						// numTotalEvolveSteps
					getImmigrationGlider(7, 7));
		}

		@After
		public void close()
		{
			core.API.close();
		}
	}










	public static class ExtraCreditQuadTest extends CheckpointBase
	{
		@Before
		public void initialize()
		{
			coreGolGUI.GameOfLifePanel.enableTestMode();
		}

		@Test(timeout = 1000)
		public void level1VerifyQuadTileClass()
		{
			ClassLoader classLoader = ClassLoader.getSystemClassLoader();
			Class<?> quadTileClass = null;
			try
			{
				quadTileClass = classLoader.loadClass("gameOfLife.QuadTile");
			}
			catch (ClassNotFoundException e)
			{
				quadTileClass = null;
			}
			golAssertNotNull("Cannot find a class named 'QuadTile' in the project.", quadTileClass);

			boolean interfaceFound = doesClassImplementTile("QuadTile", quadTileClass);
			golAssertTrue("QuadTile is supposed to implement the Tile interface.", interfaceFound);

			golAssertEquals("Your 'QuadTile' class does not have the correct super class",
					"gameOfLife.LifeTile", quadTileClass.getSuperclass().getName());

			Method gnanMethod = findGetNumActiveNeighborsMethod(quadTileClass, "QuadTile");

			String declaringClass = gnanMethod.getDeclaringClass().getName(); 
			if (!declaringClass.equals("gameOfLife.LifeTile"))
			{
				golAssertTrue("Your getNumActiveNeighbors method should be implemented in your " +
						"LifeTile class, and not overridden in a subclass.  This way, all subclasses " +
						"can reuse the same implementation.  However, it is actually overridden " +
						"in your " + declaringClass + " class.", false);
			}
		}

		@Test(timeout = 1000)
		public void level2FillQuadAge0TilesCmdLoop()
		{
			testFillViaStdIn(TileType.QUAD, 0, "", 10, 11);
		}

		@Test(timeout = 1000)
		public void level2FillQuadAge0TilesProcCmd()
		{
			int age = 0;
			testFillViaProcessCommand(TileType.QUAD, age, TileInfo.quadTileConstructedColorFromAge(age), 11, 10);
		}

		@Test(timeout = 1000)
		public void level2FillQuadAge1TilesCmdLoop()
		{
			testFillViaStdIn(TileType.QUAD, 1, "", 8, 9);
		}

		@Test(timeout = 1000)
		public void level2FillQuadAge1TilesProcCmd()
		{
			int age = 1;
			testFillViaProcessCommand(TileType.QUAD, age, TileInfo.quadTileConstructedColorFromAge(age), 9, 8);
		}

		@Test(timeout = 1000)
		public void level2FillQuadAge2TilesCmdLoop()
		{
			testFillViaStdIn(TileType.QUAD, 2, "", 7, 8);
		}

		@Test(timeout = 1000)
		public void level2FillQuadAge2TilesProcCmd()
		{
			int age = 2;
			testFillViaProcessCommand(TileType.QUAD, age, TileInfo.quadTileConstructedColorFromAge(age), 8, 7);
		}

		@Test(timeout = 1000)
		public void level2GetNumActiveNeighborsQuadNwNeSwSeCentered()
		{
			testGetNumActiveNeighbors(
					3, 		// numRows
					3, 		// numCols
					TileType.QUAD,
					1, 		// row number for the QuadTile we're testing
					1, 		// col number for (ditto)
					10100101);	// config, sets only diagonals to live 
		}

		@Test(timeout = 1000)
		public void level2GetNumActiveNeighborsQuadNwESCentered()
		{
			testGetNumActiveNeighbors(
					3, 		// numRows
					3, 		// numCols
					TileType.QUAD,
					1, 		// row number for the QuadTile we're testing
					1, 		// col number for (ditto)
					10001010);	// config, sets only NW, E, S  
		}

		@Test(timeout = 1000)
		public void level2GetNumActiveNeighborsQuadNSwCentered()
		{
			testGetNumActiveNeighbors(
					3, 		// numRows
					3, 		// numCols
					TileType.QUAD,
					1, 		// row number for the QuadTile we're testing
					1, 		// col number for (ditto)
					/*0*/1000100);	// config, sets only N, SW (leading zeroes confuse Java!) 
		}

		@Test(timeout = 1000)
		public void level2GetNumActiveNeighborsQuadSeCentered()
		{
			testGetNumActiveNeighbors(
					3, 		// numRows
					3, 		// numCols
					TileType.QUAD,
					1, 		// row number for the QuadTile we're testing
					1, 		// col number for (ditto)
					/*0000000*/1);	// config low, sets only SE (leading zeroes confuse Java!) 
		}

		@Test(timeout = 1000)
		public void level2GetNumActiveNeighborsQuadWSCentered()
		{
			testGetNumActiveNeighbors(
					3, 		// numRows
					3, 		// numCols
					TileType.QUAD,
					1, 		// row number for the QuadTile we're testing
					1, 		// col number for (ditto)
					/*000*/10010);	// config low, sets only W, S (leading zeroes confuse Java!) 
		}

		@Test(timeout = 1000)
		public void level2GetNumActiveNeighborsQuadNoneCentered()
		{
			testGetNumActiveNeighbors(
					3, 		// numRows
					3, 		// numCols
					TileType.QUAD,
					1, 		// row number for the QuadTile we're testing
					1, 		// col number for (ditto)
					0);	// config, no active neighbors 
		}

		@Test(timeout = 1000)
		public void level2GetNumActiveNeighborsQuadAllCentered()
		{
			testGetNumActiveNeighbors(
					3, 		// numRows
					3, 		// numCols
					TileType.QUAD,
					1, 		// row number for the QuadTile we're testing
					1, 		// col number for (ditto)
					11111111);	// config, sets only diagonals to live 
		}

		@Test(timeout = 1000)
		public void level3EvolveQuadWithNwNeSwSeCentered()
		{
			GameOfLifeTest.logln("Testing 4 live neighbors turns QuadTile from active to still active");
			int rows = 3;
			int cols = 3;
			testSetAndEvolveMultipleTilesWithStdIn(
					rows, cols,
					1,				// numSteps
					tileInfosFromNeighborConfig(
							TileType.QUAD, 	// center tile type
							TileType.QUAD, 	// neighbor tile type
							1, 		// Age of quad tile to evolve
							1, 1,	// Row,Col of quad tile 
							10100102));	// Neighbors: NW, NE, SW, SE
		}

		@Test(timeout = 1000)
		public void level3EvolveQuadWithNeSCentered()
		{
			GameOfLifeTest.logln("Testing 2 live neighbors turns QuadTile from active to dormant");
			int rows = 3;
			int cols = 3;
			testSetAndEvolveMultipleTilesWithStdIn(
					rows, cols,
					1,				// numSteps
					tileInfosFromNeighborConfig(
							TileType.QUAD, 	// center tile type
							TileType.QUAD, 	// neighbor tile type
							1, 		// Age of quad tile to evolve
							1, 1,	// Row,Col of quad tile 
							/*00*/100010));	// Neighbors: NE, S
		}

		@Test(timeout = 1000)
		public void level3EvolveQuadWithNWECentered()
		{
			GameOfLifeTest.logln("Testing 3 live neighbors turns QuadTile from dormant to active");
			int rows = 3;
			int cols = 3;
			testSetAndEvolveMultipleTilesWithStdIn(
					rows, cols,
					1,				// numSteps
					tileInfosFromNeighborConfig(
							TileType.QUAD, 	// center tile type
							TileType.QUAD, 	// neighbor tile type
							0, 		// Age of quad tile to evolve
							1, 1,	// Row,Col of quad tile 
							/*0*/2012000));	// Neighbors: N, W, E
		}

		@Test(timeout = 1000)
		public void level3EvolveQuadWithSSeCentered()
		{
			GameOfLifeTest.logln("Testing 2 live neighbors turns QuadTile from dormant to still dormant");
			int rows = 3;
			int cols = 3;
			testSetAndEvolveMultipleTilesWithStdIn(
					rows, cols,
					1,				// numSteps
					tileInfosFromNeighborConfig(
							TileType.QUAD, 	// center tile type
							TileType.QUAD, 	// neighbor tile type
							0, 		// Age of quad tile to evolve
							1, 1,	// Row,Col of quad tile 
							/*000000*/11));	// Neighbors: S, SE
		}

		private TileInfo[] getQuadGlider(int row, int col, int variation)
		{
			// In order to ensure we get the most "interesting" color changes,
			// we need to be particular about which tile gets which initial age...
			final int[][] ageVariations =
				{
						{1, 2, 3},
						{1, 2, 4},
						{1, 3, 4},
						{1, 4, 2},
						{2, 1, 3},
						{2, 1, 4},
						{3, 1, 2},
						{3, 1, 4},
						{4, 3, 2},	
				};
			
			int[] ageVariation = ageVariations[variation];
			return new TileInfo[]
					{
							// These three should be the same color
							new TileInfo(TileType.QUAD, ageVariation[0], row, col + 1),	
							new TileInfo(TileType.QUAD, ageVariation[0], row + 1, col + 2),	
							new TileInfo(TileType.QUAD, ageVariation[0], row + 2, col + 2),
							
							// These two should be different from above and from
							// each other
							new TileInfo(TileType.QUAD, ageVariation[1], row + 2, col),	
							new TileInfo(TileType.QUAD, ageVariation[2], row + 2, col + 1),	
					};
		}

		@Test(timeout = 1000)
		public void level3EvolveQuadGlider1Step()
		{
			logln("Evolving a glider of QuadTiles for 1 step");
			int rows = 10;
			int cols = 10;
			testSetAndEvolveMultipleTilesWithStdIn(
					rows, cols,
					TileType.QUAD,	// Fill with QuadTiles of age 0 first
					1,						// numStepsPerEvolveCommand
					1,						// numEvolveCommands
					getQuadGlider(1, 1, 0));
		}

		@Test(timeout = 1000)
		public void level3EvolveQuadBorderGlider1Step()
		{
			logln("Evolving a glider of QuadTiles at the lower-right corner for 1 step");
			int rows = 10;
			int cols = 10;
			testSetAndEvolveMultipleTilesWithStdIn(
					rows, cols,
					TileType.QUAD,	// Fill with QuadTiles of age 0 first
					1,						// numStepsPerEvolveCommand
					1,						// numEvolveCommands
					getQuadGlider(7, 7, 1));
		}

		@Test(timeout = 5000)
		public void level4EvolveQuadAge0AllCombinations()
		{
			testSetAndEvolveMultipleTilesInMultipleConfigs(
					3, 3, 					// rows, cols,
					TileType.QUAD,	// fillTileType,
					1, 1, 					// rowCenter, colCenter
					TileType.QUAD,	// centerType
					0,						// center tile's starting age
					TileType.QUAD,	// neighbor tile type
					1,						// numTotalEvolveStepsPerConfig,
					0,						// neighborConfigLow
					44444444,				// neighborConfigHigh
					5,						// QuadTile tests have base 5 neighbor configs
					257);					// step size
		}

		@Test(timeout = 5000)
		public void level4EvolveQuadAge1AllCombinations()
		{
			testSetAndEvolveMultipleTilesInMultipleConfigs(
					3, 3, 					// rows, cols,
					TileType.QUAD,	// fillTileType,
					1, 1, 					// rowCenter, colCenter
					TileType.QUAD,	// centerType
					1,						// center tile's starting age
					TileType.QUAD,	// neighbor tile type
					1,						// numTotalEvolveStepsPerConfig,
					0,						// neighborConfigLow
					44444444,				// neighborConfigHigh
					5,						// QuadTile tests have base 5 neighbor configs
					263);					// step size
		}

		@Test //(timeout = 5000)
		public void level4EvolveQuadAge2AllCombinations()
		{
			testSetAndEvolveMultipleTilesInMultipleConfigs(
					3, 3, 					// rows, cols,
					TileType.QUAD,	// fillTileType,
					1, 1, 					// rowCenter, colCenter
					TileType.QUAD,	// centerType
					2,						// center tile's starting age
					TileType.QUAD,	// neighbor tile type
					1,						// numTotalEvolveStepsPerConfig,
					0,						// neighborConfigLow
					44444444,				// neighborConfigHigh
					5,						// QuadTile tests have base 5 neighbor configs
					269);					// step size
		}

		@Test(timeout = 1000)
		public void level4EvolveQuadGlider2Steps()
		{
			GameOfLifeTest.logln("Evolving a glider of QuadTiles for 2 steps");
			int rows = 10;
			int cols = 10;
			testSetAndEvolveMultipleTilesWithStdIn(
					rows, cols,
					TileType.QUAD,	// Fill with QuadTiles of age 0 first
					2,						// numStepsPerEvolveCommand
					1,						// numEvolveCommands
					getQuadGlider(1, 1, 8));
		}

		@Test(timeout = 1000)
		public void level4EvolveQuadGlider3Steps()
		{
			GameOfLifeTest.logln("Evolving a glider of QuadTiles for 3 steps");
			int rows = 10;
			int cols = 10;
			testSetAndEvolveMultipleTilesWithStdIn(
					rows, cols,
					TileType.QUAD,	// Fill with QuadTiles of age 0 first
					3,						// numStepsPerEvolveCommand
					1,						// numEvolveCommands
					getQuadGlider(1, 1, 2));
		}

		@Test(timeout = 1000)
		public void level4EvolveQuadGlider4Steps()
		{
			GameOfLifeTest.logln("Evolving a glider of QuadTiles for 4 steps");
			int rows = 10;
			int cols = 10;
			testSetAndEvolveMultipleTilesWithStdIn(
					rows, cols,
					TileType.QUAD,	// Fill with QuadTiles of age 0 first
					4,						// numStepsPerEvolveCommand
					1,						// numEvolveCommands
					getQuadGlider(1, 1, 3));
		}

		@Test(timeout = 1000)
		public void level4EvolveQuadBorderGlider2Steps()
		{
			GameOfLifeTest.logln("Evolving a glider of QuadTiles at the lower-right corner for 2 steps");
			int rows = 10;
			int cols = 10;
			testSetAndEvolveMultipleTilesWithStdIn(
					rows, cols,
					TileType.QUAD,	// Fill with QuadTiles of age 0 first
					2,						// numStepsPerEvolveCommand
					1,						// numEvolveCommands
					getQuadGlider(7, 7, 4));
		}

		@Test(timeout = 1000)
		public void level4EvolveQuadBorderGlider3Steps()
		{
			GameOfLifeTest.logln("Evolving a glider of QuadTiles at the lower-right corner for 3 steps");
			int rows = 10;
			int cols = 10;
			testSetAndEvolveMultipleTilesWithStdIn(
					rows, cols,
					TileType.QUAD,	// Fill with QuadTiles of age 0 first
					3,						// numStepsPerEvolveCommand
					1,						// numEvolveCommands
					getQuadGlider(7, 7, 5));
		}

		@Test(timeout = 1000)
		public void level4EvolveQuadBorderGlider4Steps()
		{
			GameOfLifeTest.logln("Evolving a glider of QuadTiles at the lower-right corner for 4 steps");
			int rows = 10;
			int cols = 10;
			testSetAndEvolveMultipleTilesWithStdIn(
					rows, cols,
					TileType.QUAD,	// Fill with QuadTiles of age 0 first
					4,						// numStepsPerEvolveCommand
					1,						// numEvolveCommands
					getQuadGlider(7, 7, 6));
		}

		@Test(timeout = 1000)
		public void level4EvolveQuadBorderGliderMultipleSteps()
		{
			GameOfLifeTest.logln("Evolving a glider of QuadTiles at the lower-right corner for many steps");
			int rows = 10;
			int cols = 10;
			testSetAndEvolveMultipleTilesWithProcessCommand(
					rows, cols,
					TileType.QUAD,	// Fill with QuadTiles of age 0 first
					12,						// numTotalEvolveSteps
					getQuadGlider(7, 7, 7));
		}

		@After
		public void close()
		{
			core.API.close();
		}
	}
}
