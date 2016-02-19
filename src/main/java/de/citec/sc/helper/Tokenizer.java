package de.citec.sc.helper;

import java.util.regex.Pattern;

/**
 * This class provides a tokenization-tool that does not store information about
 * token offsets/ onsets etc. Thereto it uses some patterns to unify or remove
 * some chars.
 * 
 * @author hterhors
 *
 *         Feb 18, 2016
 */
public class Tokenizer {

	public static Pattern p1 = Pattern.compile("\\[[0-9]+\\]|\\[|\\]|\\(|\\)|\\{|\\}|'");
	public static Pattern p2 = Pattern.compile("(-|\\,|\\.|:)(?![0-9])|; |\"|\\?");
	public static Pattern p3 = Pattern.compile("'s");
	public static Pattern p4 = Pattern.compile("\\\\u[0-9A-F]{4}");
	public static Pattern p5 = Pattern.compile(" +|\t+");

	public static boolean toLowercaseIfNotUpperCase = true;

	/**
	 * Tokenize a given input string using different patterns.
	 * 
	 * @param text
	 *            the text to tokenize.
	 * 
	 * @param toLowercaseIfNotUpperCase
	 *            if tokens should be converted to lowercase if they are not
	 *            uppercase.
	 * @param splitCharacter
	 *            the split character for each token.
	 * @return
	 */
	public static String bagOfWordsTokenizer(String text, boolean toLowercaseIfNotUpperCase,
			final String splitCharacter) {
		text = text.replaceAll(p1.pattern(), "");
		text = text.replaceAll(p2.pattern(), splitCharacter);
		text = text.replaceAll(p3.pattern(), "s");
		text = text.replaceAll(p4.pattern(), splitCharacter);
		text = text.replaceAll(p5.pattern(), splitCharacter);

		if (toLowercaseIfNotUpperCase) {

			StringBuffer lowercasedText = new StringBuffer();

			for (String token : text.split(splitCharacter)) {
				lowercasedText.append(StringUtils.toLowercaseIfNotUppercase(token));
				lowercasedText.append(splitCharacter);
			}

			text = lowercasedText.toString().trim();

		}

		return text;
	}
}
