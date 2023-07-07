package nl.kooi.jsonparser.json;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum Token {
    SEMI_COLON(":"),
    D_QUOTE("\""),
    COMMA(","),
    SQ_BRACKET_OPEN("["),
    SQ_BRACKET_CLOSED("]"),
    BRACE_OPEN("{"),
    BRACE_CLOSED("}"),
    TEXT(),
    BOOLEAN(),
    NUMBER();

    private final Set<String> matchingStrings;

    Token(String... matchingStrings) {
        this.matchingStrings = Arrays.stream(matchingStrings).collect(Collectors.toSet());

    }

    public Set<String> getMatchingStrings() {
        return matchingStrings;
    }

    ;

}
