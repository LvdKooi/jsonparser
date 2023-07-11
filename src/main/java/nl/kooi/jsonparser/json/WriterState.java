package nl.kooi.jsonparser.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;

import static nl.kooi.jsonparser.json.FieldType.*;
import static nl.kooi.jsonparser.json.WriterStatus.*;

public record WriterState(JsonObject mainObject,
                          Stack<Token> tokenStack,
                          FieldType currentFieldType,
                          FieldState<String> identifier,
                          Pair<?, WriterStatus> currentValue,
                          boolean writingTextField) {

    public WriterState() {
        this(null, new Stack<>(), FieldType.UNKNOWN, FieldState.forStringField("", WriterStatus.NOT_STARTED), new Pair<>(new Object(), WriterStatus.NOT_STARTED), false);
    }

    public WriterState(JsonObject mainObject, Stack<Token> tokenStack, FieldState<String> identifier, Pair<?, WriterStatus> stringField, boolean receivedDoubleQuote) {
        this(mainObject, tokenStack, FieldType.UNKNOWN, identifier, stringField, receivedDoubleQuote);
    }

    public WriterState addInitialMainObject() {
        return new WriterState(new JsonObject(null), this.tokenStack, this.identifier, this.currentValue, this.writingTextField);
    }

    public WriterState addToken(Token token) {
        var newStack = this.tokenStack.stream().collect(Collectors.toCollection(Stack::new));
        newStack.add(token);

        return new WriterState(this.mainObject, newStack, this.currentFieldType, this.identifier, this.currentValue, this.writingTextField);
    }

    public Optional<Token> getLastToken() {
        return Optional.ofNullable(this.tokenStack)
                .filter(stack -> !stack.isEmpty())
                .map(Stack::peek);
    }

    public WriterStatus identifierStatus() {
        return this.identifier.status();
    }

    public WriterStatus valueFieldStatus() {
        return this.currentValue == null ? NOT_STARTED : this.currentValue.right();
    }

    public WriterState receiveDoubleQuote() {
        return new WriterState(this.mainObject, this.tokenStack, this.currentFieldType, this.identifier, this.currentValue, !this.writingTextField);
    }

    public WriterState writeCharacterToIdentifier(String character) {
        return new WriterState(this.mainObject, this.tokenStack, FieldState.forStringField(this.identifier.currentValue().concat(character), this.identifier.status()), this.currentValue, this.writingTextField);
    }

    public WriterState writeCharacterToValueField(String character) {
        return updateValueField(concatValueField(currentValue.left(), character));
    }

    static String concatValueField(Object currentValue, String character) {
        return Optional.ofNullable(currentValue)
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(str -> str.concat(character))
                .orElse(character);
    }

    public WriterState createArrayContentField() {
        return updateValueField(new ArrayList<>(), ARRAY);
    }

    private WriterState updateValueField(Object newObjectToBeAdded) {
        return updateValueField(newObjectToBeAdded, this.currentFieldType);
    }

    private WriterState updateValueField(Object newObjectToBeAdded, FieldType fieldType) {
        return new WriterState(this.mainObject, this.tokenStack, fieldType, this.identifier, new Pair<>(newObjectToBeAdded, WRITING), this.writingTextField);
    }

    public WriterState moveIdentifierToWritingState() {
        return new WriterState(this.mainObject, this.tokenStack, this.currentFieldType, FieldState.forStringField(this.identifier.currentValue(), WRITING), this.currentValue(), this.writingTextField);
    }

    public WriterState moveIdentifierToFinishState() {
        return new WriterState(this.mainObject, this.tokenStack, FieldState.forStringField(this.identifier.currentValue(), FINISHED), this.currentValue(), this.writingTextField);
    }

    public WriterState moveValueFieldToFinishState() {
        return flushNode();
    }

    public WriterState moveValueFieldToWritingState(FieldType fieldType) {
        return new WriterState(this.mainObject, this.tokenStack, fieldType, this.identifier, new Pair<>("", WRITING), this.writingTextField);
    }

    public WriterState moveValueFieldToNotStartedState() {
        return new WriterState(this.mainObject, this.tokenStack, UNKNOWN, this.identifier, new Pair<>(new Object(), NOT_STARTED), this.writingTextField);
    }

    private WriterState flushNode() {
        var jsonNodes = mainObject.jsonNodes();

        var node = createJsonNodeOfCorrectType(new JsonNode(identifier.currentValue(), currentValue.left()));

        if (jsonNodes == null) {
            jsonNodes = new JsonNode[]{node};
        } else {
            var list = new ArrayList<>(Arrays.asList(jsonNodes));
            list.add(node);
            jsonNodes = list.toArray(JsonNode[]::new);
        }

        return new WriterState(new JsonObject(jsonNodes), this.tokenStack, FieldState.forStringField("", WriterStatus.NOT_STARTED), new Pair<>("", WriterStatus.NOT_STARTED), false);
    }

    private JsonNode createJsonNodeOfCorrectType(JsonNode jsonNode) {
        if (currentFieldType == STRING || currentFieldType == ARRAY) {
            return jsonNode;
        }

        if (isInteger(jsonNode)) {
            return new JsonNode(jsonNode.identifier(), Integer.valueOf(((String) jsonNode.content()).trim()));
        }

        if (isDouble(jsonNode)) {
            return new JsonNode(jsonNode.identifier(), Double.valueOf(((String) jsonNode.content()).trim()));
        }

        if (isBoolean(jsonNode)) {
            return new JsonNode(jsonNode.identifier(), Boolean.valueOf(((String) jsonNode.content()).trim()));
        }

        throw new UnsupportedOperationException("Other types than String, Boolean or Number are not supported yet");
    }

    private boolean isDouble(JsonNode jsonNode) {
        if (currentFieldType != STRING) {
            try {
                Double.valueOf(((String) jsonNode.content()).trim());
                return true;
            } catch (NumberFormatException exc) {
                return false;
            }
        }

        return false;
    }

    private boolean isInteger(JsonNode jsonNode) {
        if (currentFieldType != STRING) {
            try {
                Integer.valueOf(((String) jsonNode.content()).trim());
                return true;
            } catch (NumberFormatException exc) {
                return false;
            }
        }

        return false;
    }

    private boolean isBoolean(JsonNode jsonNode) {
        if (currentFieldType != STRING) {
            var value = ((String) jsonNode.content()).trim();
            return "true".equals(value) || "false".equals(value);
        }

        return false;
    }

}
