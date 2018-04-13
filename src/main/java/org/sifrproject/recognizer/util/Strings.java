package org.sifrproject.recognizer.util;

import java.util.regex.Pattern;

@SuppressWarnings("all")
public enum Strings {
    ;

    private static final Pattern NORMALIZE_DIACRITICS = Pattern.compile("[\\p{M}]",Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern PUNCTUATION = Pattern.compile("\\p{Punct}",Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern ALPHANUM = Pattern.compile("^[\\pL\\pN]*$",Pattern.UNICODE_CHARACTER_CLASS);

    public static String normalizeString(final CharSequence input) {
        return NORMALIZE_DIACRITICS
                .matcher(input)
                .replaceAll("").toLowerCase();
    }

    public static String stripPunctuation(final CharSequence text){
        return PUNCTUATION
                .matcher(text)
                .replaceAll(" ");
    }

    public static String normalizeStringAndStripPuctuation(final CharSequence input) {
        return stripPunctuation(normalizeString(input));
    }

    public static boolean isAllCaps(final CharSequence input) {
        int position = 0;
        while (Character.isUpperCase(input.charAt(position)) && (position<input.length())) {
            position++;
        }
        return position == input.length();
    }

    public static boolean isAlphaNum(final CharSequence input){
        return ALPHANUM.matcher(input).matches();
    }
}
