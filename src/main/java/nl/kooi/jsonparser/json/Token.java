package nl.kooi.jsonparser.json;

import java.util.Arrays;

public enum Token {
    SEMI_COLON(":", true),
    D_QUOTE("\"", true),
    COMMA(",", true),
    SQ_BRACKET_OPEN("[", true),
    SQ_BRACKET_CLOSED("]", true),
    BRACE_OPEN("{", true),
    BRACE_CLOSED("}", true),
    TEXT(null, false),
    BOOLEAN(null, false),
    NUMBER(null, false),
    SPACE(" ", false);

    private final String matchingString;
    private final boolean jsonFormatToken;


    Token(String matchingString, boolean jsonFormatToken) {
        this.matchingString = matchingString;
        this.jsonFormatToken = jsonFormatToken;

    }

    public String getMatchingString() {
        return matchingString;
    }

    public static boolean isJsonFormatToken(String tokenString) {
        return Arrays.stream(Token.values())
                .filter(token -> token.matchingString != null)
                .filter(token -> token.matchingString.equals(tokenString))
                .anyMatch(token -> token.jsonFormatToken);
    }

    public boolean isJsonFormatToken() {
        return jsonFormatToken;
    }


}
