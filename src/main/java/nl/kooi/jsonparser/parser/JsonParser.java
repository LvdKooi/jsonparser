package nl.kooi.jsonparser.parser;


import nl.kooi.jsonparser.json.JsonObject;
import nl.kooi.jsonparser.json.Token;
import nl.kooi.jsonparser.json.WriterState;
import nl.kooi.jsonparser.json.WriterStatus;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
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

        for (var character : characters) {

            var tokenOptional = maybeToken(character);
            state = tokenOptional.isPresent() ?
                    handleToken(tokenOptional.get(), state) :
                    writeCharacterToState(state, character);
        }

        return state.mainObject();
    }


    private static WriterState handleToken(Token token, WriterState state) {
        return switch (token) {
            case BRACE_OPEN -> handleOpenBrace(state);
            case D_QUOTE -> handleDoubleQuote(state);
            case BRACE_CLOSED -> handleClosingBrace(state);
            case SEMI_COLON -> handleSemiColon(state);
            case COMMA -> handleComma(state);
            case SQ_BRACKET_OPEN, SQ_BRACKET_CLOSED -> state;
        };
    }

    private static WriterState writeCharacterToState(WriterState state, String character) {
        if (hasStatus(state::identifierStatus, WRITING)) {
            return state.writeCharacterToIdentifier(character);
        }

        if (hasStatus(state::identifierStatus, FINISHED) && hasStatusNotIn(state::valueFieldStatus, FINISHED) && (!character.equals(" ") || state.tokenStack().peek() != Token.SEMI_COLON)) {
            return state.writeCharacterToValueField(character);
        }

        return state;
    }

    private static WriterState handleSemiColon(WriterState state) {
        var updatedState = state.addTokenToStack(Token.SEMI_COLON);
        return updatedState.moveValueFieldToNotStartedState();
    }


    private static WriterState handleComma(WriterState state) {
        var updatedState = state.addTokenToStack(Token.COMMA);
        if (hasStatusNotIn(updatedState::valueFieldStatus, NOT_STARTED)) {
            return updatedState.moveValueFieldToFinishState();
        }

        return updatedState;
    }

    private static WriterState handleDoubleQuote(WriterState state) {
        var updatedState = state.addTokenToStack(Token.D_QUOTE);
        if (hasStatus(updatedState::identifierStatus, NOT_STARTED)) {
            return updatedState.moveIdentifierToWritingState();
        } else if (hasStatus(updatedState::identifierStatus, WRITING)) {
            return updatedState.moveIdentifierToFinishState();
        }

        if (hasStatus(updatedState::identifierStatus, FINISHED) && hasStatusNotIn(updatedState::valueFieldStatus, WRITING, FINISHED)) {
            return updatedState.moveValueFieldToWritingState(STRING);
        } else if (hasStatus(updatedState::valueFieldStatus, WRITING)) {
            return updatedState.moveValueFieldToFinishState();
        }

        return updatedState;
    }

    private static WriterState handleOpenBrace(WriterState state) {
        var updatedState = state.addTokenToStack(Token.BRACE_OPEN);
        if (hasStatusNotIn(updatedState::valueFieldStatus, WRITING, FINISHED)) {
            return updatedState.addInitialMainObject();
        }

        return updatedState;
    }

    private static WriterState handleClosingBrace(WriterState state) {
        var updatedState = state.addTokenToStack(Token.BRACE_CLOSED);
        if (hasStatus(updatedState::valueFieldStatus, WRITING)) {
            return updatedState.moveValueFieldToFinishState();
        }

        return updatedState;
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
