package nl.kooi.jsonparser.parser;


import nl.kooi.jsonparser.json.JsonObject;
import nl.kooi.jsonparser.json.Token;
import nl.kooi.jsonparser.json.WriterState;
import nl.kooi.jsonparser.json.WriterStatus;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static nl.kooi.jsonparser.json.FieldType.ARRAY;
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
            default -> UnaryOperator.identity();
        };

        return handleToken(token, state, handler);
    }

    private static WriterState handleToken(Token token,
                                           WriterState state,
                                           UnaryOperator<WriterState> writerStateFunction) {
        return state.getLastToken()
                .filter(tok -> token == tok)
                .filter(tok -> tok == TEXT)
                .map(tokenRemainsText -> writerStateFunction.apply(state))
                .orElseGet(() -> writerStateFunction.apply(state.addToken(token)));
    }

    private static WriterState writeCharacterToState(WriterState state, String character) {
        return switch (state.identifierStatus()) {
            case WRITING -> state.writeCharacterToIdentifier(character);
            case FINISHED -> state.writeCharacterToValueField(character);
            default -> state;
        };
    }

    private static WriterState handleSemiColon(WriterState state) {
        return state.moveValueFieldToNotStartedState();
    }

    private static WriterState handleComma(WriterState state) {

        if (hasStatusNotIn(state::valueFieldStatus, NOT_STARTED)) {

            if (state.currentFieldType() != ARRAY) {
                return state.moveValueFieldToFinishState();
            }

            return state.addValueToArray();
        }

        return state;
    }

    private static WriterState handleDoubleQuote(WriterState state) {
        var updatedState = state.receiveDoubleQuote();

        return updateStateWithDoubleQuoteForIdentifier(updatedState)
                .orElseGet(() -> updateStateWithDoubleQuoteForValueField(updatedState)
                        .orElseGet(() -> updateStateWithDoubleQuoteForArrayValue(updatedState)
                                .orElse(updatedState)));
    }

    private static Optional<WriterState> updateStateWithDoubleQuoteForArrayValue(WriterState updatedState) {
        return Optional.ofNullable(updatedState)
                .filter(state -> ARRAY == state.currentFieldType())
                .map(WriterState::addValueToArray);
    }

    private static Optional<WriterState> updateStateWithDoubleQuoteForValueField(WriterState updatedState) {
        if (hasStatus(updatedState::identifierStatus, FINISHED) &&
                hasStatusNotIn(updatedState::valueFieldStatus, WRITING, FINISHED)) {
            return Optional.of(updatedState.moveValueFieldToWritingState(STRING));
        } else if (hasStatus(updatedState::valueFieldStatus, WRITING) && updatedState.currentFieldType() != ARRAY) {
            return Optional.of(updatedState.moveValueFieldToFinishState());
        }
        return Optional.empty();
    }

    private static Optional<WriterState> updateStateWithDoubleQuoteForIdentifier(WriterState updatedState) {
        if (hasStatus(updatedState::identifierStatus, NOT_STARTED)) {
            return Optional.of(updatedState.moveIdentifierToWritingState());
        } else if (hasStatus(updatedState::identifierStatus, WRITING)) {
            return Optional.of(updatedState.moveIdentifierToFinishState());
        }
        return Optional.empty();
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
        return hasStatus(state.currentValue()::status, WRITING) ?
                state.addValueToArray().moveValueFieldToFinishState() :
                state.moveValueFieldToFinishState();
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

        if (state.getLastToken().filter(isIn(SEMI_COLON, SQ_BRACKET_OPEN, SPACE, NUMBER, BOOLEAN)).isPresent() &&
                tokenOptional.filter(Token::isJsonFormatToken).isEmpty()) {
            return isNumberRelatedCharacter(character) ? Optional.of(NUMBER) : Optional.of(BOOLEAN);
        }

        return tokenOptional;
    }

    private static boolean isNumberRelatedCharacter(String character) {
        return isNumber(character) || isDecimalPoint(character) || isMinus(character);
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
        return CompletableFuture.supplyAsync(() -> {
            Integer.valueOf(character);
            return true;
        }).exceptionally(exc -> false).join();
    }

    private static boolean isDecimalPoint(String character) {
        return ".".equals(character);
    }

    private static boolean isMinus(String character) {
        return "-".equals(character);
    }
}
