package nl.kooi.jsonparser.parser;


import nl.kooi.jsonparser.json.JsonObject;
import nl.kooi.jsonparser.json.Token;
import nl.kooi.jsonparser.json.WriterState;
import nl.kooi.jsonparser.json.WriterStatus;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static nl.kooi.jsonparser.json.FieldType.STRING;
import static nl.kooi.jsonparser.json.WriterStatus.*;

public class JsonParser {

    public static JsonObject parse(String objectString) {
        var state = new WriterState();

        for (var character : objectString.split("")) {
            var tokenOptional = maybeToken(character);
            state = tokenOptional.isPresent() ?
                    handleToken(tokenOptional.get(), state) :
                    writeCharacterToState(state, character);
        }

        return state.mainObject();
    }


    private static WriterState handleToken(Token token, WriterState state) {
        UnaryOperator<WriterState> function = switch (token) {
            case BRACE_OPEN -> JsonParser::handleOpenBrace;
            case D_QUOTE -> JsonParser::handleDoubleQuote;
            case BRACE_CLOSED -> JsonParser::handleClosingBrace;
            case SEMI_COLON -> JsonParser::handleSemiColon;
            case COMMA -> JsonParser::handleComma;
            case SQ_BRACKET_OPEN, SQ_BRACKET_CLOSED -> currentState -> currentState;
        };

        return handleToken(token, state, function);
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
        return state.moveValueFieldToNotStartedState();
    }


    private static WriterState handleComma(WriterState status) {
        if (hasStatusNotIn(status::valueFieldStatus, NOT_STARTED)) {
            return status.moveValueFieldToFinishState();
        }

        return status;
    }

    private static WriterState handleToken(Token token, WriterState state, UnaryOperator<WriterState> writerStateFunction) {
        var updatedState = state.addTokenToStack(token);
        return writerStateFunction.apply(updatedState);
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
