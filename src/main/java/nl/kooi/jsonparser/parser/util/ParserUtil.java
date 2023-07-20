package nl.kooi.jsonparser.parser.util;

public class ParserUtil {

    public static String getNestedArrayString(char[] stillToBeProcessed) {
        var openBraceCounter = 0;
        var currentString = "";
        var closedBraceCounter = 0;

        for (var character : stillToBeProcessed) {
            currentString = currentString.concat(String.valueOf(character));

            switch (character) {
                case ']' -> closedBraceCounter++;
                case '[' -> openBraceCounter++;
            }

            if (closedBraceCounter == openBraceCounter) {
                break;
            }
        }

        return currentString;
    }

    public static String getNestedObjectString(char[] stillToBeProcessed) {
        var openBraceCounter = 0;
        var currentString = "";
        var closedBraceCounter = 0;

        for (var character : stillToBeProcessed) {
            currentString = currentString.concat(String.valueOf(character));

            switch (character) {
                case '}' -> closedBraceCounter++;
                case '{' -> openBraceCounter++;
            }

            if (closedBraceCounter == openBraceCounter) {
                break;
            }
        }

        return currentString;
    }
}
