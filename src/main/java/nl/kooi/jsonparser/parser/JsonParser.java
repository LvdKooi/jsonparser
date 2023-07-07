package nl.kooi.jsonparser.parser;


import nl.kooi.jsonparser.json.JsonObject;
import nl.kooi.jsonparser.json.Token;
import nl.kooi.jsonparser.json.WriterState;
import nl.kooi.jsonparser.json.WriterStatus;

import java.util.Arrays;
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
                    writeCharacterToState(state, character);
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
            case TEXT -> writerState -> writeCharacterToState(writerState, character);
            default -> currentState -> currentState;
        };

        return handleToken(token, state, handler);
    }

    private static WriterState writeCharacterToState(WriterState state, String character) {
        if (hasStatus(state::identifierStatus, WRITING)) {
            return state.writeCharacterToIdentifier(character);
        }

        if (hasStatus(state::identifierStatus, FINISHED) &&
                hasStatusNotIn(state::valueFieldStatus, FINISHED) &&
                (!isSpace(character) || state.getLastToken().filter(is(SEMI_COLON)).isEmpty())) {
            return state.writeCharacterToValueField(character);
        }

        return state;
    }

    private static Predicate<Token> is(Token token) {
        return t -> t == token;
    }

    private static Predicate<Token> isIn(Token... tokens) {
        return t -> Set.of(tokens).contains(t);
    }

    private static Predicate<Token> notIn(Token... tokens) {
        return t -> !Set.of(tokens).contains(t);
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

    private static WriterState handleToken(Token token,
                                           WriterState state,
                                           UnaryOperator<WriterState> writerStateFunction) {
        var updatedState = state.addToken(token);
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

    private static Optional<Token> maybeToken(String character, WriterState state) {
        var lastToken = state.getLastToken();
        var tokenOptional = Arrays.stream(Token.values())
                .filter(token -> token.getMatchingStrings().stream().anyMatch(tokenString -> tokenString.equals(character)))
                .findFirst();

        if (lastToken.filter(isIn(D_QUOTE, TEXT)).isPresent() &&
                (tokenOptional.isEmpty() || tokenOptional
                        .filter(isIn(BRACE_CLOSED, SEMI_COLON, D_QUOTE, COMMA).negate())
                        .isPresent())) {
            return Optional.of(TEXT);
        }

        return tokenOptional;
    }

    private static boolean hasStatus(Supplier<WriterStatus> statusSupplier, WriterStatus match) {
        return statusSupplier.get() == match;
    }

    private static boolean hasStatusNotIn(Supplier<WriterStatus> statusSupplier, WriterStatus... statuses) {
        return !Set.of(statuses).contains(statusSupplier.get());
    }

    private static boolean isSpace(String character) {
        return " ".equals(character);
    }
}
