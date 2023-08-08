package test;

import java.awt.Color;

// For clearer error messages, this tries to convert
// a Color object into one of the Color constants we expect
// a Tile might have, or a one-letter abbreviation for use
// by the Illustrator
public class ColorString
{
	private static class ColorWithStrings
	{
		private Color color;
		private String name;
		private String abbreviation;

		public ColorWithStrings(Color colorP, String nameP, String abbreviationP)
		{
			color = colorP;
			name = nameP;
			abbreviation = abbreviationP;
		}
	};

	private static final ColorWithStrings[] colorsWithStrings =
		{
				new ColorWithStrings(Color.BLUE, "Color.BLUE", "B"),
				new ColorWithStrings(Color.DARK_GRAY, "Color.DARK_GRAY", "D"),
				new ColorWithStrings(Color.GREEN, "Color.GREEN", "G"),
				new ColorWithStrings(Color.LIGHT_GRAY, "Color.LIGHT_GRAY", "L"),
				new ColorWithStrings(Color.MAGENTA, "Color.MAGENTA", "M"),
				new ColorWithStrings(Color.ORANGE, "Color.ORANGE", "O"),
				new ColorWithStrings(Color.RED, "Color.RED", "R"),
				new ColorWithStrings(Color.WHITE, "Color.WHITE", "W"),
				new ColorWithStrings(Color.YELLOW, "Color.YELLOW", "Y"),
				new ColorWithStrings(Color.BLACK, "Color.BLACK", " "),
		};

	private enum StringKind { NAME, ABBREVIATION };

	public static String nameFromColor(Color color)
	{
		return stringFromColor(color, StringKind.NAME);
	}
	
	public static String abbreviationFromColor(Color color)
	{
		return stringFromColor(color, StringKind.ABBREVIATION);
	}
	
	public static String getColorAbbreviationLegend(String linePrefix)
	{
		String ret = "";
		for (ColorWithStrings cs : colorsWithStrings)
		{
			if (cs.abbreviation.equals(" "))
			{
				continue;
			}
			ret += linePrefix + cs.abbreviation + " = " + cs.name + "\n";
		}
		
		return ret;
	}
	
	private static String stringFromColor(Color color, StringKind kind)
	{
		for (ColorWithStrings cs : colorsWithStrings)
		{
			if (cs.color.equals(color))
			{
				if (kind == StringKind.NAME)
				{
					return cs.name;
				}
				return cs.abbreviation;
			}
		}

		// Default catch-all
		if (kind == StringKind.NAME)
		{
			return color.toString();
		}
		
		throw new UnsupportedOperationException("Internal test failure.  Unrecognized color " + color.toString());
	}
}
