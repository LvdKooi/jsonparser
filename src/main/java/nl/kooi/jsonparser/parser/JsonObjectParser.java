package nl.kooi.jsonparser.parser;


import nl.kooi.jsonparser.json.JsonObject;
import nl.kooi.jsonparser.monad.Conditional;
import nl.kooi.jsonparser.parser.command.TokenCommand;
import nl.kooi.jsonparser.parser.state.Token;
import nl.kooi.jsonparser.parser.state.WriterState;
import nl.kooi.jsonparser.parser.state.WriterStatus;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static nl.kooi.jsonparser.parser.state.FieldType.ARRAY;
import static nl.kooi.jsonparser.parser.state.Token.TEXT;
import static nl.kooi.jsonparser.parser.state.WriterStatus.*;
import static nl.kooi.jsonparser.parser.util.ParserUtil.*;

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

    private static WriterState handleToken(TokenCommand<WriterState> tokenCommand) {
        UnaryOperator<WriterState> handler = switch (tokenCommand.token()) {
            case BRACE_OPEN -> writerState -> handleOpenBrace(writerState, tokenCommand);
            case D_QUOTE -> JsonObjectParser::handleDoubleQuote;
            case BRACE_CLOSED -> JsonObjectParser::handleClosingBrace;
            case SEMI_COLON -> JsonObjectParser::handleSemiColon;
            case COMMA -> JsonObjectParser::handleComma;
            case TEXT, BOOLEAN, NUMBER -> writerState -> writeCharacterToState(writerState, tokenCommand);
            case SQ_BRACKET_OPEN -> writerState -> handleOpenSquareBracket(writerState, tokenCommand);
            default -> UnaryOperator.identity();
        };

        return handleToken(tokenCommand.token(), tokenCommand.state(), handler);
    }


    private static WriterState handleToken(Token token, WriterState state, UnaryOperator<WriterState> writerStateFunction) {
        return state.getLastToken().filter(tok -> token == tok).filter(tok -> tok == TEXT).map(tokenRemainsText -> writerStateFunction.apply(state)).orElseGet(() -> writerStateFunction.apply(state.addToken(token)));
    }

    private static WriterState writeCharacterToState(WriterState state, TokenCommand<WriterState>  tokenCommand) {
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
        return Conditional.apply(WriterState::moveValueFieldToFinishState)
                .when(writerState -> hasStatusNotIn(writerState::valueFieldStatus, NOT_STARTED))
                .applyToOrElse(state, state);
    }

    private static WriterState handleDoubleQuote(WriterState state) {
        var updatedState = state.receiveDoubleQuote();

        return updateStateWithDoubleQuoteForIdentifier(updatedState)
                .orElseGet(() -> updateStateWithDoubleQuoteForValueField(updatedState)
                        .orElse(updatedState));
    }

    private static Optional<WriterState> updateStateWithDoubleQuoteForValueField(WriterState updatedState) {
        return Conditional.apply(WriterState::moveValueFieldToWritingStateForStringValue)
                .when(isFinishedWritingIdentifier())
                .orApply(WriterState::moveValueFieldToFinishState)
                .when(isWritingNonArrayValueField())
                .map(Optional::of)
                .applyToOrElseGet(updatedState, Optional::empty);
    }

    private static Predicate<WriterState> isFinishedWritingIdentifier() {
        return state -> hasStatus(state::identifierStatus, FINISHED) && hasStatusNotIn(state::valueFieldStatus, WRITING, FINISHED);
    }

    private static Predicate<WriterState> isWritingNonArrayValueField() {
        return state -> hasStatus(state::valueFieldStatus, WRITING) && state.currentFieldType() != ARRAY;
    }

    private static Optional<WriterState> updateStateWithDoubleQuoteForIdentifier(WriterState updatedState) {
        return Conditional.apply(WriterState::moveIdentifierToWritingState)
                .when(writerState -> hasStatus(writerState::identifierStatus, NOT_STARTED))
                .orApply(WriterState::moveIdentifierToFinishState)
                .when(writerState -> hasStatus(writerState::identifierStatus, WRITING))
                .map(Optional::of)
                .applyToOrElseGet(updatedState, Optional::empty);
    }

    private static WriterState handleOpenBrace(WriterState state, TokenCommand<WriterState>  tokenCommand) {
        return Conditional.apply(WriterState::addInitialMainObject)
                .when(writerState -> Objects.isNull(writerState.mainObject()) && hasStatusNotIn(writerState::valueFieldStatus, WRITING, FINISHED))
                .orApply(Function.identity())
                .when(writerState -> Objects.isNull(writerState.mainObject()))
                .applyToOrElseGet(state, () -> handleNestedObject(state, tokenCommand.stillToBeProcessed()));
    }

    private static WriterState handleNestedObject(WriterState state, char[] stillToBeProcessed) {
        var nestedObjectString = getNestedObjectString(stillToBeProcessed);

        var updatedState = state.incrementCharacterCounterBy(nestedObjectString.length() - 1);
        var nestedObject = JsonObjectParser.parse(nestedObjectString);
        return updatedState.writeObjectToValueField(nestedObject);
    }

    private static WriterState handleClosingBrace(WriterState state) {
        return finishValueField(state);
    }

    private static WriterState handleOpenSquareBracket(WriterState state, TokenCommand<WriterState>  command) {
        return handleNestedArray(state, command.stillToBeProcessed());
    }

    private static WriterState handleNestedArray(WriterState state, char[] stillToBeProcessed) {
        var nestedArrayString = getNestedArrayString(stillToBeProcessed);

        var updatedState = state.incrementCharacterCounterBy(nestedArrayString.length() - 1);
        return updatedState.writeArrayToValueField(JsonArrayParser.parse(nestedArrayString));
    }

    private static WriterState finishValueField(WriterState state) {
        return hasStatus(state::valueFieldStatus, WRITING) ? state.moveValueFieldToFinishState() : state;
    }

    private static Optional<TokenCommand<WriterState>> createTokenCommand(char[] stillToBeProcessed, char character, WriterState state) {
        var command = new TokenCommand<WriterState>(stillToBeProcessed, character);

        return getOptionalTokenCommand(command).applyToOrElseGet(state, () -> findToken(character).map(token -> new TokenCommand<>(stillToBeProcessed, token, character, state)));
    }

    private static boolean hasStatus(Supplier<WriterStatus> statusSupplier, WriterStatus match) {
        return statusSupplier.get() == match;
    }

    private static boolean hasStatusNotIn(Supplier<WriterStatus> statusSupplier, WriterStatus... statuses) {
        return !Set.of(statuses).contains(statusSupplier.get());
    }
}
