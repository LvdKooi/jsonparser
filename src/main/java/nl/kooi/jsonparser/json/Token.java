package nl.kooi.jsonparser.json;

import java.util.Optional;

public enum Token {
    SEMI_COLON(':', true),
    D_QUOTE('"', true),
    COMMA(',', true),
    SQ_BRACKET_OPEN('[', true),
    SQ_BRACKET_CLOSED(']', true),
    BRACE_OPEN('{', true),
    BRACE_CLOSED('}', true),
    TEXT(null, false),
    BOOLEAN(null, false),
    NUMBER(null, false),
    SPACE(' ', false);

    private final Character matchingCharacter;
    private final boolean jsonFormatToken;


    Token(Character matchingString, boolean jsonFormatToken) {
        this.matchingCharacter = matchingString;
        this.jsonFormatToken = jsonFormatToken;

    }

    public Optional<Character> getMatchingCharacter() {
        return Optional.ofNullable(matchingCharacter);
    }

    public boolean isJsonFormatToken() {
        return jsonFormatToken;
    }


}
