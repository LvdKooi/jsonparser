package nl.kooi.jsonparser.parser;


import io.github.lvdkooi.Conditional;
import nl.kooi.jsonparser.parser.command.TokenCommand;
import nl.kooi.jsonparser.parser.state.ArrayWriterState;
import nl.kooi.jsonparser.parser.state.Token;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static io.github.lvdkooi.Conditional.applyIf;
import static nl.kooi.jsonparser.parser.state.Token.TEXT;
import static nl.kooi.jsonparser.parser.state.WriterStatus.*;
import static nl.kooi.jsonparser.parser.util.ParserUtil.*;

public class JsonArrayParser {

    public static List<Object> parse(String arrayString) {

        var finalState = new ArrayWriterState();

        while (finalState.characterCounter() != arrayString.length()) {
            finalState = createTokenCommand(arrayString, finalState)
                    .map(JsonArrayParser::handleToken)
                    .orElse(finalState)
                    .incrementCharacterCounter();
        }

        return finalState.array();
    }

    private static ArrayWriterState handleToken(TokenCommand<ArrayWriterState> tokenCommand) {
        UnaryOperator<ArrayWriterState> handler = switch (tokenCommand.token()) {
            case BRACE_OPEN -> state -> JsonArrayParser.handleOpenBrace(state, tokenCommand);
            case D_QUOTE -> JsonArrayParser::handleDoubleQuote;
            case COMMA -> JsonArrayParser::handleComma;
            case TEXT, BOOLEAN, NUMBER, NULL -> state -> writeCharacterToState(tokenCommand.state(), tokenCommand);
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

    private static ArrayWriterState writeCharacterToState(ArrayWriterState state, TokenCommand<ArrayWriterState> tokenCommand) {
        return state.writeCharacterToValueField(tokenCommand.character());
    }

    private static ArrayWriterState handleComma(ArrayWriterState state) {
        return Conditional.of(state)
                .firstMatching(
                        applyIf(arrayWriterState -> hasStatusNotIn(arrayWriterState::valueFieldStatus, NOT_STARTED), ArrayWriterState::addValueToArray)
                )
                .orElse(state);
    }

    private static ArrayWriterState handleDoubleQuote(ArrayWriterState state) {
        var updatedState = state.receiveDoubleQuote();

        return updateStateWithDoubleQuoteForValueField(updatedState).orElse(updatedState);
    }

    private static Optional<ArrayWriterState> updateStateWithDoubleQuoteForValueField(ArrayWriterState updatedState) {
        return Conditional.of(updatedState)
                .firstMatching(
                        applyIf(state -> hasStatusNotIn(state::valueFieldStatus, WRITING, FINISHED), ArrayWriterState::moveValueFieldToWritingStateForStringValue),
                        applyIf(status -> hasWritingStatus(status::valueFieldStatus), ArrayWriterState::moveValueFieldToFinishState)
                )
                .map(Optional::of).orElseGet(Optional::empty);

    }

    private static ArrayWriterState handleOpenBrace(ArrayWriterState state, TokenCommand<ArrayWriterState> tokenCommand) {
        return handleNestedObject(state, tokenCommand.stillToBeProcessed());
    }

    private static ArrayWriterState handleNestedObject(ArrayWriterState state, char[] stillToBeProcessed) {
        var nestedObjectString = getNestedObjectString(stillToBeProcessed);

        var updatedState = state.incrementCharacterCounterBy(nestedObjectString.length() - 1);
        return updatedState.writeObjectToValueField(JsonObjectParser.parse(nestedObjectString));
    }

    private static ArrayWriterState handleOpenSquareBracket(ArrayWriterState state, TokenCommand<ArrayWriterState> command) {
        return Conditional.of(state)
                .firstMatching(
                        applyIf(hasNotStartedHandlingAnArray(), ArrayWriterState::addInitialArray),
                        applyIf(Objects::isNull, Function.identity())
                )
                .orElseGet(() -> handleNestedArray(state, command.stillToBeProcessed()));
    }

    private static Predicate<ArrayWriterState> hasNotStartedHandlingAnArray() {
        return state -> Objects.isNull(state.array()) && hasStatusNotIn(state::valueFieldStatus, WRITING, FINISHED);
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
        return hasWritingStatus(state::valueFieldStatus) ? state.moveValueFieldToFinishState() : state;
    }

    private static Optional<TokenCommand<ArrayWriterState>> createTokenCommand(String arrayString, ArrayWriterState state) {
        var stillToBeProcessed = arrayString.substring(state.characterCounter()).toCharArray();
        var character = arrayString.charAt(state.characterCounter());

        var tokenCommand = new TokenCommand<ArrayWriterState>(stillToBeProcessed, character);

        return getOptionalTokenCommand(state, tokenCommand)
                .orElseGet(
                        () -> findToken(character).map(token -> new TokenCommand<>(stillToBeProcessed, token, character, state)));
    }
}
