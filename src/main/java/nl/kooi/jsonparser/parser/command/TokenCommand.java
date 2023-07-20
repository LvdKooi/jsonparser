package nl.kooi.jsonparser.parser.command;

import nl.kooi.jsonparser.parser.state.Token;
import nl.kooi.jsonparser.parser.state.WriterState;

public record TokenCommand(
        char[] stillToBeProcessed,
        Token token,
        char character,
        WriterState state) {
}
