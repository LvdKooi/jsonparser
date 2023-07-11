package nl.kooi.jsonparser.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;

import static nl.kooi.jsonparser.json.FieldType.STRING;
import static nl.kooi.jsonparser.json.FieldType.UNKNOWN;
import static nl.kooi.jsonparser.json.WriterStatus.*;

public record WriterState(JsonObject mainObject,
                          Stack<Token> tokenStack,
                          FieldType currentFieldType,
                          Pair<String, WriterStatus> identifier,
                          Pair<?, WriterStatus> valueField,
                          boolean writingTextField) {

    public WriterState() {
        this(null, new Stack<>(), FieldType.UNKNOWN, new Pair<>("", WriterStatus.NOT_STARTED), new Pair<>("", WriterStatus.NOT_STARTED), false);
    }

    public WriterState(JsonObject mainObject, Stack<Token> tockenStack, Pair<String, WriterStatus> identifier, Pair<?, WriterStatus> stringField, boolean receivedDoubleQuote) {
        this(mainObject, tockenStack, FieldType.UNKNOWN, identifier, stringField, receivedDoubleQuote);
    }

    public WriterState addInitialMainObject() {
        return new WriterState(new JsonObject(null), this.tokenStack, this.identifier, this.valueField, this.writingTextField);
    }

    public WriterState addToken(Token token) {
        var newStack = this.tokenStack.stream().collect(Collectors.toCollection(Stack::new));
        newStack.add(token);

        return new WriterState(this.mainObject, newStack, this.currentFieldType, this.identifier, this.valueField, this.writingTextField);
    }

    public Optional<Token> getLastToken() {
        return Optional.ofNullable(this.tokenStack)
                .filter(stack -> !stack.isEmpty())
                .map(Stack::peek);
    }

    public WriterStatus identifierStatus() {
        return this.identifier.right();
    }

    public WriterStatus valueFieldStatus() {
        return this.valueField == null ? NOT_STARTED : this.valueField.right();
    }

    public WriterState receiveDoubleQuote() {
        return new WriterState(this.mainObject, this.tokenStack, this.currentFieldType, this.identifier, this.valueField, !this.writingTextField);
    }

    public WriterState writeCharacterToIdentifier(String character) {
        return new WriterState(this.mainObject, this.tokenStack, new Pair<>(this.identifier.left().concat(character), this.identifier.right()), this.valueField, this.writingTextField);
    }

    public WriterState writeCharacterToValueField(String character) {
        return new WriterState(this.mainObject, this.tokenStack, this.currentFieldType, this.identifier, new Pair<>(((String) this.valueField.left()).concat(character), WRITING), this.writingTextField);
    }

    public WriterState moveIdentifierToWritingState() {
        return new WriterState(this.mainObject, this.tokenStack, this.currentFieldType, new Pair<>(this.identifier.left(), WRITING), this.valueField(), this.writingTextField);
    }

    public WriterState moveIdentifierToFinishState() {
        return new WriterState(this.mainObject, this.tokenStack, new Pair<>(this.identifier.left(), FINISHED), this.valueField(), this.writingTextField);
    }

    public WriterState moveValueFieldToFinishState() {
        return flushNode();
    }

    public WriterState moveValueFieldToWritingState(FieldType fieldType) {
        return new WriterState(this.mainObject, this.tokenStack, fieldType, this.identifier, new Pair<>("", WRITING), this.writingTextField);
    }

    public WriterState moveValueFieldToNotStartedState() {
        return new WriterState(this.mainObject, this.tokenStack, UNKNOWN, this.identifier, new Pair<>("", NOT_STARTED), this.writingTextField);
    }

    private WriterState flushNode() {
        var jsonNodes = mainObject.jsonNodes();

        var node = createJsonNodeOfCorrectType(new JsonNode(identifier.left(), valueField.left()));

        if (jsonNodes == null) {
            jsonNodes = new JsonNode[]{node};
        } else {
            var list = new ArrayList<>(Arrays.asList(jsonNodes));
            list.add(node);
            jsonNodes = list.toArray(JsonNode[]::new);
        }

        return new WriterState(new JsonObject(jsonNodes), this.tokenStack, new Pair<>("", WriterStatus.NOT_STARTED), new Pair<>("", WriterStatus.NOT_STARTED), false);
    }

    private JsonNode createJsonNodeOfCorrectType(JsonNode jsonNode) {
        if (currentFieldType == STRING) {
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
