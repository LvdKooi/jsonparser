package nl.kooi.jsonparser.parser.util;

public class ParserUtil {

    public static String getNestedArrayString(char[] stillToBeProcessed) {
        return getNestedString(stillToBeProcessed, '[', ']');
    }

    public static String getNestedObjectString(char[] stillToBeProcessed) {
        return getNestedString(stillToBeProcessed, '{', '}');
    }

    private static String getNestedString(char[] stillToBeProcessed, char openCharacter, char closingCharacter) {
        var openBraceCounter = 0;
        var currentString = "";
        var closedBraceCounter = 0;

        for (var character : stillToBeProcessed) {
            currentString = currentString.concat(String.valueOf(character));

            if (character == closingCharacter) {
                closedBraceCounter++;
            }
            if (character == openCharacter) {
                openBraceCounter++;
            }

            if (closedBraceCounter == openBraceCounter) {
                break;
            }
        }

        return currentString;
    }
}
