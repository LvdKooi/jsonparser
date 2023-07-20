package nl.kooi.jsonparser.parser;

import nl.kooi.jsonparser.json.ArrayWriterState;
import nl.kooi.jsonparser.json.Token;
import nl.kooi.jsonparser.json.WriterState;

public record ArrayTokenCommand(
        char[] stillToBeProcessed,
        Token token,
        char character,
        ArrayWriterState state) {
}
