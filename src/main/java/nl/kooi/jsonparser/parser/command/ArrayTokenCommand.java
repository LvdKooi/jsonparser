package nl.kooi.jsonparser.parser.command;

import nl.kooi.jsonparser.parser.state.ArrayWriterState;
import nl.kooi.jsonparser.parser.state.Token;

public record ArrayTokenCommand(
        char[] stillToBeProcessed,
        Token token,
        char character,
        ArrayWriterState state) {
}
