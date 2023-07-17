package nl.kooi.jsonparser.parser;

import nl.kooi.jsonparser.json.Token;
import nl.kooi.jsonparser.json.WriterState;

public record TokenCommand(
        char[] stillToBeProcessed,
        Token token,
        char character,
        WriterState state) {
}
