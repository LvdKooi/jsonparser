package nl.kooi.jsonparser.parser;

import nl.kooi.jsonparser.json.JsonObject;
import nl.kooi.jsonparser.json.Token;

import java.util.Arrays;
import java.util.Optional;

public class JsonParser {


    public static JsonObject parse(String jsonString) {
        return handleComplexJsonString(jsonString);
    }

    private static JsonObject handleComplexJsonString(String jsonString) {
        return readObject(jsonString);
    }

    private static JsonObject readObject(String objectString) {
        var characters = objectString.split("");
        JsonObject mainObject = null;

        for (var character : characters) {

            var tokenOptional = maybeToken(character);

            if (tokenOptional.isPresent()) {
                var token = tokenOptional.get();
                switch (token) {
                    case BRACE_OPEN -> mainObject = new JsonObject(null);
                    case BRACE_CLOSED -> {
                        return mainObject;
                    }
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
