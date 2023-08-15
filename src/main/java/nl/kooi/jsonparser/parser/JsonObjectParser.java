package nl.kooi.jsonparser.parser;


import nl.kooi.jsonparser.json.JsonObject;
import nl.kooi.jsonparser.monad.Conditional;
import nl.kooi.jsonparser.parser.command.TokenCommand;
import nl.kooi.jsonparser.parser.state.ObjectWriterState;
import nl.kooi.jsonparser.parser.state.Token;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static nl.kooi.jsonparser.parser.state.FieldType.ARRAY;
import static nl.kooi.jsonparser.parser.state.Token.TEXT;
import static nl.kooi.jsonparser.parser.state.WriterStatus.*;
import static nl.kooi.jsonparser.parser.util.ParserUtil.*;

public class JsonObjectParser {

    public static JsonObject parse(String objectString) {

        var finalState = new ObjectWriterState();

        while (finalState.characterCounter() != objectString.length()) {
            finalState = createTokenCommand(objectString, finalState)
                    .map(JsonObjectParser::handleToken)
                    .orElse(finalState)
                    .incrementCharacterCounter();
        }

        return finalState.mainObject();
    }

    private static ObjectWriterState handleToken(TokenCommand<ObjectWriterState> tokenCommand) {
        UnaryOperator<ObjectWriterState> handler = switch (tokenCommand.token()) {
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

    private static ObjectWriterState handleToken(Token token, ObjectWriterState state, UnaryOperator<ObjectWriterState> writerStateFunction) {
        return state.getLastToken()
                .filter(tok -> token == tok)
                .filter(tok -> tok == TEXT)
                .map(tokenRemainsText -> writerStateFunction.apply(state))
                .orElseGet(() -> writerStateFunction.apply(state.addToken(token)));
    }

    private static ObjectWriterState writeCharacterToState(ObjectWriterState state, TokenCommand<ObjectWriterState> tokenCommand) {
        return switch (state.identifierStatus()) {
            case WRITING -> state.writeCharacterToIdentifier(tokenCommand.character());
            case FINISHED -> state.writeCharacterToValueField(tokenCommand.character());
            default -> state;
        };
    }

    private static ObjectWriterState handleSemiColon(ObjectWriterState state) {
        return state.moveValueFieldToNotStartedState();
    }

    private static ObjectWriterState handleComma(ObjectWriterState state) {
        return Conditional.apply(ObjectWriterState::moveValueFieldToFinishState)
                .when(writerState -> hasStatusNotIn(writerState::valueFieldStatus, NOT_STARTED))
                .applyToOrElse(state, state);
    }

    private static ObjectWriterState handleDoubleQuote(ObjectWriterState state) {
        var updatedState = state.receiveDoubleQuote();

        return updateStateWithDoubleQuoteForIdentifier(updatedState)
                .orElseGet(() -> updateStateWithDoubleQuoteForValueField(updatedState)
                        .orElse(updatedState));
    }

    private static Optional<ObjectWriterState> updateStateWithDoubleQuoteForValueField(ObjectWriterState updatedState) {
        return Conditional.apply(ObjectWriterState::moveValueFieldToWritingStateForStringValue)
                .when(isFinishedWritingIdentifier())
                .orApply(ObjectWriterState::moveValueFieldToFinishState)
                .when(isWritingNonArrayValueField())
                .map(Optional::of)
                .applyToOrElseGet(updatedState, Optional::empty);
    }

    private static Predicate<ObjectWriterState> isFinishedWritingIdentifier() {
        return state -> hasStatus(state::identifierStatus, FINISHED) && hasStatusNotIn(state::valueFieldStatus, WRITING, FINISHED);
    }

    private static Predicate<ObjectWriterState> isWritingNonArrayValueField() {
        return state -> hasStatus(state::valueFieldStatus, WRITING) && state.currentFieldType() != ARRAY;
    }

    private static Optional<ObjectWriterState> updateStateWithDoubleQuoteForIdentifier(ObjectWriterState updatedState) {
        return Conditional.apply(ObjectWriterState::moveIdentifierToWritingState)
                .when(writerState -> hasStatus(writerState::identifierStatus, NOT_STARTED))
                .orApply(ObjectWriterState::moveIdentifierToFinishState)
                .when(writerState -> hasStatus(writerState::identifierStatus, WRITING))
                .map(Optional::of)
                .applyToOrElseGet(updatedState, Optional::empty);
    }

    private static ObjectWriterState handleOpenBrace(ObjectWriterState state, TokenCommand<ObjectWriterState> tokenCommand) {
        return Conditional.apply(ObjectWriterState::addInitialMainObject)
                .when(writerState -> Objects.isNull(writerState.mainObject()) && hasStatusNotIn(writerState::valueFieldStatus, WRITING, FINISHED))
                .orApply(Function.identity())
                .when(writerState -> Objects.isNull(writerState.mainObject()))
                .applyToOrElseGet(state, () -> handleNestedObject(state, tokenCommand.stillToBeProcessed()));
    }

    private static ObjectWriterState handleNestedObject(ObjectWriterState state, char[] stillToBeProcessed) {
        var nestedObjectString = getNestedObjectString(stillToBeProcessed);

        var updatedState = state.incrementCharacterCounterBy(nestedObjectString.length() - 1);
        var nestedObject = JsonObjectParser.parse(nestedObjectString);
        return updatedState.writeObjectToValueField(nestedObject);
    }

    private static ObjectWriterState handleClosingBrace(ObjectWriterState state) {
        return finishValueField(state);
    }

    private static ObjectWriterState handleOpenSquareBracket(ObjectWriterState state, TokenCommand<ObjectWriterState> command) {
        return handleNestedArray(state, command.stillToBeProcessed());
    }

    private static ObjectWriterState handleNestedArray(ObjectWriterState state, char[] stillToBeProcessed) {
        var nestedArrayString = getNestedArrayString(stillToBeProcessed);

        var updatedState = state.incrementCharacterCounterBy(nestedArrayString.length() - 1);
        return updatedState.writeArrayToValueField(JsonArrayParser.parse(nestedArrayString));
    }

    private static ObjectWriterState finishValueField(ObjectWriterState state) {
        return hasWritingStatus(state::valueFieldStatus) ? state.moveValueFieldToFinishState() : state;
    }

    private static Optional<TokenCommand<ObjectWriterState>> createTokenCommand(String objectString, ObjectWriterState state) {
        var stillToBeProcessed = objectString.substring(state.characterCounter()).toCharArray();
        var character = objectString.charAt(state.characterCounter());

        var command = new TokenCommand<ObjectWriterState>(stillToBeProcessed, character);

        return getOptionalTokenCommand(command)
                .applyToOrElseGet(state,
                        () -> findToken(character).map(token -> new TokenCommand<>(stillToBeProcessed, token, character, state)));
    }
}
