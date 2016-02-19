package de.citec.sc.helper;

/**
 * Thios class provides some string utils.
 * 
 * @author hterhors
 *
 *         Feb 18, 2016
 */
public class StringUtils {

	/**
	 * Checks if the given input string is solely in uppercase.
	 * 
	 * @param s
	 *            the string to check.
	 * @return true if the string is in uppercase.
	 */
	public static boolean isUpperCase(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (Character.isLowerCase(s.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Converts a given string into lowercase if it is not in uppercase.
	 * 
	 * @param s
	 * @return
	 */
	public static String toLowercaseIfNotUppercase(String s) {

		if (isUpperCase(s))
			return s;
		else
			return s.toLowerCase();
	}

}
