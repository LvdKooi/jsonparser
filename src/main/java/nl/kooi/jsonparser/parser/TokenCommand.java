package nl.kooi.jsonparser.parser;

import nl.kooi.jsonparser.json.Token;
import nl.kooi.jsonparser.json.WriterState;

public record TokenCommand(
        String remainingString,
        Token token,
        String character,
        WriterState state) {
}
