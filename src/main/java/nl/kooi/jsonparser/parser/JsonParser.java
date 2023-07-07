package nl.kooi.jsonparser.parser;


import nl.kooi.jsonparser.json.JsonObject;
import nl.kooi.jsonparser.json.Token;
import nl.kooi.jsonparser.json.WriterState;
import nl.kooi.jsonparser.json.WriterStatus;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.function.Supplier;

import static nl.kooi.jsonparser.json.FieldType.STRING;
import static nl.kooi.jsonparser.json.WriterStatus.*;

public class JsonParser {

    public static JsonObject parse(String jsonString) {
        return handleComplexJsonString(jsonString);
    }

    private static JsonObject handleComplexJsonString(String objectString) {
        var characters = objectString.split("");
        var state = new WriterState();
        var tokenStack = new Stack<Token>();

        for (var character : characters) {

            var tokenOptional = maybeToken(character);

            if (tokenOptional.isPresent()) {
                var token = tokenOptional.get();
                tokenStack.push(token);
                state = switch (token) {
                    case BRACE_OPEN -> handleOpenBrace(state);
                    case D_QUOTE -> handleDoubleQuote(state);
                    case BRACE_CLOSED -> handleClosingBrace(state);
                    case SEMI_COLON -> state.moveValueFieldToNotStartedState();
                    case COMMA -> handleComma(state);
                    case SQ_BRACKET_OPEN, SQ_BRACKET_CLOSED -> state;
                };
            } else {
                state = writeCharacterToState(state, tokenStack, character);
            }
        }

        return state.mainObject();
    }

    private static WriterState writeCharacterToState(WriterState state, Stack<Token> tokenStack, String character) {
        if (hasStatus(state::identifierStatus, WRITING)) {
            return state.writeCharacterToIdentifier(character);
        }

        if (hasStatus(state::identifierStatus, FINISHED) && hasStatusNotIn(state::valueFieldStatus, FINISHED) && (!character.equals(" ") || tokenStack.peek() != Token.SEMI_COLON)) {
            return state.writeCharacterToValueField(character);
        }

        return state;
    }


    private static WriterState handleComma(WriterState state) {
        if (hasStatusNotIn(state::valueFieldStatus, NOT_STARTED)) {
            return state.moveValueFieldToFinishState();
        }

        return state;
    }

    private static WriterState handleDoubleQuote(WriterState state) {
        if (hasStatus(state::identifierStatus, NOT_STARTED)) {
            return state.moveIdentifierToWritingState();
        } else if (hasStatus(state::identifierStatus, WRITING)) {
            return state.moveIdentifierToFinishState();
        }

        if (hasStatus(state::identifierStatus, FINISHED) && hasStatusNotIn(state::valueFieldStatus, WRITING, FINISHED)) {
            return state.moveValueFieldToWritingState(STRING);
        } else if (hasStatus(state::valueFieldStatus, WRITING)) {
            return state.moveValueFieldToFinishState();
        }

        return state;
    }

    private static WriterState handleOpenBrace(WriterState state) {
        if (hasStatusNotIn(state::valueFieldStatus, WRITING, FINISHED)) {
            return state.addInitialMainObject();
        }

        return state;
    }

    private static WriterState handleClosingBrace(WriterState state) {
        if (hasStatus(state::valueFieldStatus, WRITING)) {
            return state.moveValueFieldToFinishState();
        }

        return state;
    }

    private static Optional<Token> maybeToken(String character) {
        return Arrays.stream(Token.values())
                .filter(token -> token.getMatchingStrings().stream().anyMatch(tokenString -> tokenString.equals(character)))
                .findFirst();
    }

    private static boolean hasStatus(Supplier<WriterStatus> statusSupplier, WriterStatus match) {
        return statusSupplier.get() == match;
    }

    private static boolean hasStatusNotIn(Supplier<WriterStatus> statusSupplier, WriterStatus... statuses) {
        return !Set.of(statuses).contains(statusSupplier.get());
    }
}
