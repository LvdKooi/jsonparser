package nl.kooi.jsonparser.parser;


import nl.kooi.jsonparser.json.JsonObject;
import nl.kooi.jsonparser.json.Token;
import nl.kooi.jsonparser.json.WriterState;
import nl.kooi.jsonparser.json.WriterStatus;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;

import static nl.kooi.jsonparser.json.FieldType.STRING;
import static nl.kooi.jsonparser.json.WriterStatus.*;

public class JsonParser {

    public static JsonObject parse(String jsonString) {
        return handleComplexJsonString(jsonString);
    }

    private static JsonObject handleComplexJsonString(String objectString) {
        var characters = objectString.split("");
        var state = new WriterState();
        var tokenStack = new Stack<Token>();

        for (var character : characters) {

            var tokenOptional = maybeToken(character);

            if (tokenOptional.isPresent()) {
                var token = tokenOptional.get();
                tokenStack.push(token);
                switch (token) {
                    case BRACE_OPEN -> state = state.addInitialMainObject();
                    case D_QUOTE -> {

                        if (state.identifierStatus() == NOT_STARTED) {
                            state = state.moveIdentifierToWritingState();
                        } else if (state.identifierStatus() == WriterStatus.WRITING) {
                            state = state.moveIdentifierToFinishState();
                            continue;
                        }

                        if (state.identifierStatus() == FINISHED && !Set.of(WRITING, FINISHED).contains(state.valueFieldStatus())) {
                            state = state.moveValueFieldToWritingState(STRING);
                        } else if (state.valueFieldStatus() == WriterStatus.WRITING) {
                            state = state.moveValueFieldToFinishState();
                        }
                    }
                    case BRACE_CLOSED -> {
                        if (state.valueFieldStatus() == WriterStatus.WRITING) {
                            state = state.moveValueFieldToFinishState();
                        }
                        return state.mainObject();
                    }

                    case SEMI_COLON -> state = state.moveValueFieldToNotStartedState();
                    case COMMA -> {
                        if (state.identifierStatus() != NOT_STARTED) {
                            state = state.moveValueFieldToFinishState();
                        }
                    }

                    case SQ_BRACKET_OPEN, SQ_BRACKET_CLOSED -> {
                    }

                }
            } else {
                if (state.identifierStatus() == WriterStatus.WRITING) {
                    state = state.writeCharacterToIdentifier(character);
                }

                if (state.identifierStatus() == FINISHED && state.valueFieldStatus() != FINISHED && (!character.equals(" ") || tokenStack.peek() != Token.SEMI_COLON)) {
                    state = state.writeCharacterToValueField(character);
                }
            }
        }

        return null;
    }

    private static Optional<Token> maybeToken(String character) {
        return Arrays.stream(Token.values())
                .filter(token -> token.getMatchingStrings().stream().anyMatch(tokenString -> tokenString.equals(character)))
                .findFirst();
    }
}
