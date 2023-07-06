package nl.kooi.jsonparser.parser;


import nl.kooi.jsonparser.json.JsonObject;
import nl.kooi.jsonparser.json.Token;
import nl.kooi.jsonparser.json.WriterState;
import nl.kooi.jsonparser.json.WriterStatus;

import java.util.Arrays;
import java.util.Optional;

public class JsonParser {

    public static JsonObject parse(String jsonString) {
        return handleComplexJsonString(jsonString);
    }

    private static JsonObject handleComplexJsonString(String objectString) {
        var characters = objectString.split("");
        var state = new WriterState();

        for (var character : characters) {

            var tokenOptional = maybeToken(character);

            if (tokenOptional.isPresent()) {
                var token = tokenOptional.get();
                switch (token) {
                    case BRACE_OPEN -> state = new WriterState(new JsonObject(null), state.identifier(), state.stringField());
                    case D_QUOTE -> {

                        if (state.identifierStatus() == WriterStatus.NOT_STARTED) {
                            state = state.moveIdentifierToWritingState();
                        } else if (state.identifierStatus() == WriterStatus.WRITING) {
                            state = state.moveIdentifierToFinishState();
                            continue;
                        }

                        if (state.identifierStatus() == WriterStatus.FINISHED && state.stringFieldStatus() == WriterStatus.NOT_STARTED) {
                            state = state.moveStringFieldToWritingState();
                        } else if (state.stringFieldStatus() == WriterStatus.WRITING) {
                            state = state.writeNode();
                        }
                    }
                    case BRACE_CLOSED -> {
                        return state.mainObject();
                    }

                    case COMMA, SEMI_COLON, SQ_BRACKET_OPEN, SQ_BRACKET_CLOSED -> {
                    }

                }
            } else {
                if (state.identifierStatus() == WriterStatus.WRITING) {
                    state = state.writeCharacterToIdentifier(character);
                }

                if (state.stringFieldStatus() == WriterStatus.WRITING) {
                    state = state.writeCharacterToStringField(character);
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
