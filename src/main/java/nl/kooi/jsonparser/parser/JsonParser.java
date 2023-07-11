package nl.kooi.jsonparser.parser;


import nl.kooi.jsonparser.json.JsonObject;
import nl.kooi.jsonparser.json.Token;
import nl.kooi.jsonparser.json.WriterState;
import nl.kooi.jsonparser.json.WriterStatus;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static nl.kooi.jsonparser.json.FieldType.STRING;
import static nl.kooi.jsonparser.json.Token.*;
import static nl.kooi.jsonparser.json.WriterStatus.*;

public class JsonParser {

    public static JsonObject parse(String objectString) {
        var state = new WriterState();

        for (var character : objectString.split("")) {

            var tokenOptional = maybeToken(character, state);

            state = tokenOptional.isPresent() ?
                    handleToken(tokenOptional.get(), character, state) :
                    state;
        }

        return state.mainObject();
    }

    private static WriterState handleToken(Token token, String character, WriterState state) {
        UnaryOperator<WriterState> handler = switch (token) {
            case BRACE_OPEN -> JsonParser::handleOpenBrace;
            case D_QUOTE -> JsonParser::handleDoubleQuote;
            case BRACE_CLOSED -> JsonParser::handleClosingBrace;
            case SEMI_COLON -> JsonParser::handleSemiColon;
            case COMMA -> JsonParser::handleComma;
            case TEXT, BOOLEAN, NUMBER -> writerState -> writeCharacterToState(writerState, character);
            case SQ_BRACKET_OPEN -> JsonParser::handleOpenSquareBracket;
            case SQ_BRACKET_CLOSED -> JsonParser::handleClosedSquareBracket;
            default -> currentState -> currentState;
        };

        return handleToken(token, state, handler);
    }

    private static WriterState writeCharacterToState(WriterState state, String character) {
        if (hasStatus(state::identifierStatus, WRITING)) {
            return state.writeCharacterToIdentifier(character);
        }

        if (hasStatus(state::identifierStatus, FINISHED)) {
            return state.writeCharacterToValueField(character);
        }

        return state;
    }

    private static WriterState handleSemiColon(WriterState state) {
        return state.moveValueFieldToNotStartedState();
    }

    private static WriterState handleComma(WriterState state) {
        if (hasStatusNotIn(state::valueFieldStatus, NOT_STARTED)) {
            return state.moveValueFieldToFinishState();
        }

        return state;
    }

    private static WriterState handleToken(Token token,
                                           WriterState state,
                                           UnaryOperator<WriterState> writerStateFunction) {
        var updatedState = state.getLastToken()
                .filter(tok -> token == tok)
                .filter(tok -> tok == TEXT)
                .isPresent() ? state : state.addToken(token);

        return writerStateFunction.apply(updatedState);
    }

    private static WriterState handleDoubleQuote(WriterState state) {

        var updatedState = state.receiveDoubleQuote();

        if (hasStatus(updatedState::identifierStatus, NOT_STARTED)) {
            return updatedState.moveIdentifierToWritingState();
        } else if (hasStatus(updatedState::identifierStatus, WRITING)) {
            return updatedState.moveIdentifierToFinishState();
        }

        if (hasStatus(updatedState::identifierStatus, FINISHED) &&
                hasStatusNotIn(updatedState::valueFieldStatus, WRITING, FINISHED)) {
            return updatedState.moveValueFieldToWritingState(STRING);
        } else if (hasStatus(updatedState::valueFieldStatus, WRITING)) {
            return updatedState.moveValueFieldToFinishState();
        }

        return updatedState;
    }

    private static WriterState handleOpenBrace(WriterState state) {
        return hasStatusNotIn(state::valueFieldStatus, WRITING, FINISHED) ?
                state.addInitialMainObject() : state;
    }

    private static WriterState handleClosingBrace(WriterState state) {
        return finishValueField(state);
    }

    private static WriterState handleOpenSquareBracket(WriterState state) {
        return state.createArrayContentField();
    }

    private static WriterState handleClosedSquareBracket(WriterState state) {
        return finishValueField(state);
    }

    private static WriterState finishValueField(WriterState state) {
        return hasStatus(state::valueFieldStatus, WRITING) ?
                state.moveValueFieldToFinishState() : state;
    }

    private static Optional<Token> maybeToken(String character, WriterState state) {
        if (state.writingTextField() && !D_QUOTE.getMatchingString().equals(character)) {
            return Optional.of(TEXT);
        }

        if (!state.writingTextField() && SPACE.getMatchingString().equals(character)) {
            return Optional.of(SPACE);
        }

        var tokenOptional = Arrays.stream(Token.values())
                .filter(Objects::nonNull)
                .filter(token -> character.equals(token.getMatchingString()))
                .findFirst();


        if (state.getLastToken().filter(isIn(SEMI_COLON, SPACE, NUMBER, BOOLEAN)).isPresent() &&
                tokenOptional
                        .filter(Token::isJsonFormatToken)
                        .isEmpty()) {

            return isNumber(character) || isDecimalPoint(character) ? Optional.of(NUMBER) : Optional.of(BOOLEAN);
        }

        return tokenOptional;
    }

    private static boolean hasStatus(Supplier<WriterStatus> statusSupplier, WriterStatus match) {
        return statusSupplier.get() == match;
    }

    private static boolean hasStatusNotIn(Supplier<WriterStatus> statusSupplier, WriterStatus... statuses) {
        return !Set.of(statuses).contains(statusSupplier.get());
    }

    private static Predicate<Token> isIn(Token... tokens) {
        return t -> Set.of(tokens).contains(t);
    }

    private static boolean isNumber(String character) {
        try {
            Integer.valueOf(character);
            return true;
        } catch (NumberFormatException exc) {
            return false;
        }
    }

    private static boolean isDecimalPoint(String character) {
        return ".".equals(character);
    }
}
