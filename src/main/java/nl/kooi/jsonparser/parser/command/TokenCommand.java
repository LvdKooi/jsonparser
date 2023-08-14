package nl.kooi.jsonparser.parser.command;

import nl.kooi.jsonparser.parser.state.Token;

import static nl.kooi.jsonparser.parser.state.Token.*;

public record TokenCommand<T>(
        char[] stillToBeProcessed,
        Token token,
        char character,
        T state) {


    public TokenCommand(char[] stillToBeProcessed,
                        char character) {
        this(stillToBeProcessed, null, character, null);
    }

    public TokenCommand<T> forSpace(T state) {
        return new TokenCommand<>(stillToBeProcessed, SPACE, character, state);
    }


    public TokenCommand<T> forNumber(T state) {
        return new TokenCommand<>(stillToBeProcessed, NUMBER, character, state);
    }


    public TokenCommand<T> forBoolean(T state) {
        return new TokenCommand<>(stillToBeProcessed, BOOLEAN, character, state);
    }

    public TokenCommand<T> forText(T state) {
        return new TokenCommand<>(stillToBeProcessed, TEXT, character, state);
    }
}
