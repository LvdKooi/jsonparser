package nl.kooi.jsonparser.parser;


import nl.kooi.jsonparser.json.ArrayWriterState;
import nl.kooi.jsonparser.json.Token;
import nl.kooi.jsonparser.json.WriterStatus;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static nl.kooi.jsonparser.json.FieldType.STRING;
import static nl.kooi.jsonparser.json.Token.*;
import static nl.kooi.jsonparser.json.WriterStatus.*;
import static nl.kooi.jsonparser.parser.ParserUtil.getNestedArrayString;
import static nl.kooi.jsonparser.parser.ParserUtil.getNestedObjectString;

public class JsonArrayParser {

    public static List<Object> parse(String arrayString) {

        var finalState = new ArrayWriterState();

        while (finalState.characterCounter() != arrayString.length()) {
            finalState = createTokenCommand(arrayString.substring(finalState.characterCounter()).toCharArray(), arrayString.charAt(finalState.characterCounter()), finalState)
                    .map(JsonArrayParser::handleToken)
                    .orElse(finalState).incrementCharacterCounter();
        }

        return finalState.array();
    }

    private static ArrayWriterState handleToken(ArrayTokenCommand tokenCommand) {
        UnaryOperator<ArrayWriterState> handler = switch (tokenCommand.token()) {
            case BRACE_OPEN -> state -> JsonArrayParser.handleOpenBrace(state, tokenCommand);
            case D_QUOTE -> JsonArrayParser::handleDoubleQuote;
            case COMMA -> JsonArrayParser::handleComma;
            case TEXT, BOOLEAN, NUMBER -> ArrayWriterState -> writeCharacterToState(tokenCommand.state(), tokenCommand);
            case SQ_BRACKET_OPEN -> state -> handleOpenSquareBracket(state, tokenCommand);
            case SQ_BRACKET_CLOSED -> JsonArrayParser::handleClosedSquareBracket;
            default -> UnaryOperator.identity();
        };

        return handleToken(tokenCommand.token(), tokenCommand.state(), handler);
    }

    private static ArrayWriterState handleToken(Token token, ArrayWriterState state, UnaryOperator<ArrayWriterState> ArrayWriterStateFunction) {
        return state
                .getLastToken()
                .filter(tok -> token == tok)
                .filter(tok -> tok == TEXT)
                .map(tokenRemainsText -> ArrayWriterStateFunction.apply(state))
                .orElseGet(() -> ArrayWriterStateFunction.apply(state.addToken(token)));
    }

    private static ArrayWriterState writeCharacterToState(ArrayWriterState state, ArrayTokenCommand tokenCommand) {
        return state.writeCharacterToValueField(tokenCommand.character());
    }

    private static ArrayWriterState handleComma(ArrayWriterState state) {
        if (hasStatusNotIn(state::valueFieldStatus, NOT_STARTED)) {
            return state.addValueToArray();
        }

        return state;
    }

    private static ArrayWriterState handleDoubleQuote(ArrayWriterState state) {
        var updatedState = state.receiveDoubleQuote();

        return updateStateWithDoubleQuoteForValueField(updatedState).orElse(updatedState);
    }

    private static Optional<ArrayWriterState> updateStateWithDoubleQuoteForValueField(ArrayWriterState updatedState) {
        if (hasStatusNotIn(updatedState::valueFieldStatus, WRITING, FINISHED)) {
            return Optional.of(updatedState.moveValueFieldToWritingState(STRING));
        }

        if (hasStatus(updatedState::valueFieldStatus, WRITING)) {
            return Optional.of(updatedState.moveValueFieldToFinishState());
        }

        return Optional.empty();
    }

    private static ArrayWriterState handleOpenBrace(ArrayWriterState state, ArrayTokenCommand tokenCommand) {
        return handleNestedObject(state, tokenCommand.stillToBeProcessed());
    }

    private static ArrayWriterState handleNestedObject(ArrayWriterState state, char[] stillToBeProcessed) {
        var nestedObjectString = getNestedObjectString(stillToBeProcessed);

        var updatedState = state.incrementCharacterCounterBy(nestedObjectString.length() - 1);
        return updatedState.writeObjectToValueField(JsonObjectParser.parse(nestedObjectString));
    }

    private static ArrayWriterState handleOpenSquareBracket(ArrayWriterState state, ArrayTokenCommand command) {
        if (state.array() == null) {
            return hasStatusNotIn(state::valueFieldStatus, WRITING, FINISHED) ? state.addInitialArray() : state;
        }

        return handleNestedArray(state, command.stillToBeProcessed());
    }

    private static ArrayWriterState handleNestedArray(ArrayWriterState state, char[] stillToBeProcessed) {
        var arrayString = getNestedArrayString(stillToBeProcessed);

        var array = JsonArrayParser.parse(arrayString);

        var updatedState = state.incrementCharacterCounterBy(arrayString.length() - 1);

        return updatedState.addArrayToArray(array);
    }

    private static ArrayWriterState handleClosedSquareBracket(ArrayWriterState state) {
        return finishValueField(state);
    }

    private static ArrayWriterState finishValueField(ArrayWriterState state) {
        return hasStatus(state::valueFieldStatus, WRITING) ? state.moveValueFieldToFinishState() : state;
    }

    private static Optional<ArrayTokenCommand> createTokenCommand(char[] stillToBeProcessed, char character, ArrayWriterState state) {
        if (state.writingTextField() && !isDoubleQuote(character)) {
            return Optional.of(new ArrayTokenCommand(stillToBeProcessed, TEXT, character, state));
        }

        if (!state.writingTextField() && isSpace(character)) {
            return Optional.of(new ArrayTokenCommand(stillToBeProcessed, SPACE, character, state));
        }

        if (isProcessingNonTextValue(state, character)) {
            return isNumberRelatedCharacter(character) ? Optional.of(new ArrayTokenCommand(stillToBeProcessed, NUMBER, character, state)) : Optional.of(new ArrayTokenCommand(stillToBeProcessed, BOOLEAN, character, state));
        }

        return findToken(character).map(token -> new ArrayTokenCommand(stillToBeProcessed, token, character, state));
    }

    private static boolean isProcessingNonTextValue(ArrayWriterState state, char character) {
        return state
                .getLastToken()
                .filter(isIn(SEMI_COLON, SQ_BRACKET_OPEN, SPACE, NUMBER, BOOLEAN))
                .isPresent() &&

                findToken(character).filter(Token::isJsonFormatToken).isEmpty();
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
