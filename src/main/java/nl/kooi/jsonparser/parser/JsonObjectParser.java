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

import static nl.kooi.jsonparser.json.FieldType.ARRAY;
import static nl.kooi.jsonparser.json.FieldType.STRING;
import static nl.kooi.jsonparser.json.Token.*;
import static nl.kooi.jsonparser.json.WriterStatus.*;
import static nl.kooi.jsonparser.parser.ParserUtil.getNestedObjectString;

public class JsonObjectParser {

    public static JsonObject parse(String objectString) {

        var finalState = new WriterState();

        while (finalState.characterCounter() != objectString.length()) {
            finalState = createTokenCommand(objectString.substring(finalState.characterCounter()).toCharArray(), objectString.charAt(finalState.characterCounter()), finalState)
                    .map(JsonObjectParser::handleToken)
                    .orElse(finalState).incrementCharacterCounter();
        }

        return finalState.mainObject();
    }

    private static WriterState handleToken(TokenCommand tokenCommand) {
        UnaryOperator<WriterState> handler = switch (tokenCommand.token()) {
            case BRACE_OPEN -> writerState -> handleOpenBrace(writerState, tokenCommand);
            case D_QUOTE -> JsonObjectParser::handleDoubleQuote;
            case BRACE_CLOSED -> JsonObjectParser::handleClosingBrace;
            case SEMI_COLON -> JsonObjectParser::handleSemiColon;
            case COMMA -> JsonObjectParser::handleComma;
            case TEXT, BOOLEAN, NUMBER -> writerState -> writeCharacterToState(writerState, tokenCommand);
            case SQ_BRACKET_OPEN -> JsonObjectParser::handleOpenSquareBracket;
            case SQ_BRACKET_CLOSED -> JsonObjectParser::handleClosedSquareBracket;
            default -> UnaryOperator.identity();
        };

        return handleToken(tokenCommand.token(), tokenCommand.state(), handler);
    }

    private static WriterState handleToken(Token token, WriterState state, UnaryOperator<WriterState> writerStateFunction) {
        return state.getLastToken().filter(tok -> token == tok).filter(tok -> tok == TEXT).map(tokenRemainsText -> writerStateFunction.apply(state)).orElseGet(() -> writerStateFunction.apply(state.addToken(token)));
    }

    private static WriterState writeCharacterToState(WriterState state, TokenCommand tokenCommand) {
        return switch (state.identifierStatus()) {
            case WRITING -> state.writeCharacterToIdentifier(tokenCommand.character());
            case FINISHED -> state.writeCharacterToValueField(tokenCommand.character());
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

        return updateStateWithDoubleQuoteForIdentifier(updatedState).orElseGet(() -> updateStateWithDoubleQuoteForValueField(updatedState).orElseGet(() -> updateStateWithDoubleQuoteForArrayValue(updatedState).orElse(updatedState)));
    }

    private static Optional<WriterState> updateStateWithDoubleQuoteForArrayValue(WriterState updatedState) {
        return Optional.ofNullable(updatedState).filter(state -> ARRAY == state.currentFieldType()).map(WriterState::addValueToArray);
    }

    private static Optional<WriterState> updateStateWithDoubleQuoteForValueField(WriterState updatedState) {
        if (hasStatus(updatedState::identifierStatus, FINISHED) && hasStatusNotIn(updatedState::valueFieldStatus, WRITING, FINISHED)) {
            return Optional.of(updatedState.moveValueFieldToWritingState(STRING));
        }

        if (hasStatus(updatedState::valueFieldStatus, WRITING) && updatedState.currentFieldType() != ARRAY) {
            return Optional.of(updatedState.moveValueFieldToFinishState());
        }

        return Optional.empty();
    }

    private static Optional<WriterState> updateStateWithDoubleQuoteForIdentifier(WriterState updatedState) {
        if (hasStatus(updatedState::identifierStatus, NOT_STARTED)) {
            return Optional.of(updatedState.moveIdentifierToWritingState());
        }

        if (hasStatus(updatedState::identifierStatus, WRITING)) {
            return Optional.of(updatedState.moveIdentifierToFinishState());
        }

        return Optional.empty();
    }

    private static WriterState handleOpenBrace(WriterState state, TokenCommand tokenCommand) {
        if (state.mainObject() == null) {

            return hasStatusNotIn(state::valueFieldStatus, WRITING, FINISHED) ? state.addInitialMainObject() : state;

        }

        return handleNestedObject(state, tokenCommand.stillToBeProcessed());
    }

    private static WriterState handleNestedObject(WriterState state, char[] stillToBeProcessed) {
        var nestedObjectString = getNestedObjectString(stillToBeProcessed);

        var updatedState = state.incrementCharacterCounterBy(nestedObjectString.length() - 1);
        return updatedState.writeObjectToValueField(JsonObjectParser.parse(nestedObjectString));
    }

    private static WriterState handleClosingBrace(WriterState state) {
        return finishValueField(state);
    }

    private static WriterState handleOpenSquareBracket(WriterState state) {
        return state.createArrayContentField();
    }

    private static WriterState handleClosedSquareBracket(WriterState state) {
        return hasStatus(state.currentValue()::status, WRITING) ? state.addValueToArray().moveValueFieldToFinishState() : state.moveValueFieldToFinishState();
    }

    private static WriterState finishValueField(WriterState state) {
        return hasStatus(state::valueFieldStatus, WRITING) ? state.moveValueFieldToFinishState() : state;
    }

    private static Optional<TokenCommand> createTokenCommand(char[] stillToBeProcessed, char character, WriterState state) {
        if (state.writingTextField() && !isDoubleQuote(character)) {
            return Optional.of(new TokenCommand(stillToBeProcessed, TEXT, character, state));
        }

        if (!state.writingTextField() && isSpace(character)) {
            return Optional.of(new TokenCommand(stillToBeProcessed, SPACE, character, state));
        }

        if (isProcessingNonTextValue(state, character)) {
            return isNumberRelatedCharacter(character) ? Optional.of(new TokenCommand(stillToBeProcessed, NUMBER, character, state)) : Optional.of(new TokenCommand(stillToBeProcessed, BOOLEAN, character, state));
        }

        return findToken(character).map(token -> new TokenCommand(stillToBeProcessed, token, character, state));
    }

    private static boolean isProcessingNonTextValue(WriterState state, char character) {
        return state.getLastToken().filter(isIn(SEMI_COLON, SQ_BRACKET_OPEN, SPACE, NUMBER, BOOLEAN)).isPresent() && findToken(character).filter(Token::isJsonFormatToken).isEmpty();
    }

    private static Optional<Token> findToken(char character) {
        return Arrays.stream(Token.values()).filter(Objects::nonNull).filter(token -> token.getMatchingCharacter().filter(tokenChar -> tokenChar.equals(character)).isPresent()).findFirst();
    }

    private static boolean isDoubleQuote(char character) {
        return D_QUOTE.getMatchingCharacter()
                .filter(tokenMatchesChar(character))
                .isPresent();
    }

    private static boolean isSpace(char character) {
        return SPACE.getMatchingCharacter()
                .filter(tokenMatchesChar(character))
                .isPresent();
    }

    private static Predicate<Character> tokenMatchesChar(char character) {
        return tokenCharacter -> Optional.ofNullable(tokenCharacter)
                .filter(tokenChar -> tokenChar.equals(character))
                .isPresent();
    }

    private static boolean isNumberRelatedCharacter(char character) {
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

    private static boolean isNumber(char character) {
        try {
            Integer.valueOf(character);
            return true;
        } catch (NumberFormatException exc) {
            return false;
        }
    }

    private static boolean isDecimalPoint(char character) {
        return '.' == character;
    }

    private static boolean isMinus(char character) {
        return '-' == character;
    }
}
