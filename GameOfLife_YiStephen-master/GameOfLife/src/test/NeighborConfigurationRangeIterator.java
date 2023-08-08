package test;

import java.util.function.*;

// A neighbor configuration is an 8-digit number, like
// 10011000, where each digit represents the age of one of
// the 8 neighbors.  Using base 2 (MonoTile) or base 3
// (ImmigrationTile) arithmetic, we can iterate through all
// neighbor configs in a specified range and "do something"
// for each config
public class NeighborConfigurationRangeIterator
{
	// Index of this array is the digit index of a neighbor configuration number
	// (0-based, counting from left).  Value in this array is the { row, col } offset
	// of that digit index (index 0 represents NW neighbor, index 1 represents N neighbor, etc.)
	public static final int[][] offsets =
		{
				{-1, -1},	
				{-1, 0},	
				{-1, 1},	
				{0, -1},	
				{0, 1},	
				{1, -1},	
				{1, 0},	
				{1, 1},	
		};
	
	private static final int[] digitPlaces = { 1, 10, 100, 1000, 10000, 100000, 1000000, 10000000 };

	/**
	 * Iterates through each digit of neighborConfiguration, calling the specified
	 * method reference for each one.
	 * 
	 * @param neighborConfiguration number whose digits represent ages (when expressed in base 10)
	 * 		representing a neighbor configuration.  Each digit of this number will be iterated over
	 * @param perDigit For each iterated digit, this method reference is called with the digit as
	 *  	first parameter, and its index as second parameter (index 0 = leftmost digit,
	 *  	index 1 = next digit, etc.)
	 */
	public static void callWithDigit(int neighborConfiguration, BiConsumer<Integer, Integer> perDigit)
	{
		// Turn neighborConfiguration into a list of set commands to add the
		// neighbors with the specified ages in the corresponding configuration
		for (int j=0; j < 8; j++)
		{
			int digit = (neighborConfiguration / digitPlaces[j]) % 10;
			perDigit.accept(digit, j);
		}
	}

	/**
	 * Iterates through all neighbor configuration in a range (counting in the specified
	 * base), calling the method reference for each neighbor configuration
	 * @param neighborConfigLow Low part of range, inclusive.  Must be a 
	 * 		neighborConfiguration number whose digits are the ages (when expressed in base 10)
	 * @param neighborConfigHigh Low part of range, inclusive.  Must be a 
	 * 		neighborConfiguration number whose digits are the ages (when expressed in base 10)
	 * @param perNeighborConfig method reference to call with currently iterated
	 * 		neighbor configuration
	 */
	public static void callWithNeighborConfigurations(int neighborConfigLow, int neighborConfigHigh,
			int base, int stepSize, IntConsumer perNeighborConfig)
	{
		int baseConverted = neighborConfigLow;
		int step = 0;

		while (baseConverted <= neighborConfigHigh)
		{
			if (step % stepSize == 0)
			{
				// Call supplied method reference with currently iterated
				// neighbor configuration
				perNeighborConfig.accept(baseConverted);			
			}
			
			step++;

			// Add 1 to baseConverted in base
			
			int digitPlace = 1;

			while (true)
			{
				// Increment digit in-place
				baseConverted += digitPlace;

				int digit = (baseConverted / digitPlace) % 10;

				if (digit < base)
				{
					// No more carries for this add 1
					break;
				}

				// digit == base, so subtract it out and continue the loop
				// to carry 1 to the next digit 
				baseConverted -= (base * digitPlace);

				digitPlace *= 10;
			}
		}
	}
}
