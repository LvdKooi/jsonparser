package nl.kooi.jsonparser.parser.state;

import nl.kooi.jsonparser.json.JsonNode;
import nl.kooi.jsonparser.json.JsonObject;

import java.util.*;
import java.util.stream.Collectors;

import static nl.kooi.jsonparser.parser.state.FieldType.*;
import static nl.kooi.jsonparser.parser.state.Token.BOOLEAN;
import static nl.kooi.jsonparser.parser.state.Token.NUMBER;
import static nl.kooi.jsonparser.parser.state.Token.*;
import static nl.kooi.jsonparser.parser.state.WriterStatus.*;
import static nl.kooi.jsonparser.parser.util.ParserUtil.findToken;
import static nl.kooi.jsonparser.parser.util.ParserUtil.isIn;

public record ObjectWriterState(JsonObject mainObject,
                                Stack<Token> tokenStack,
                                FieldType currentFieldType,
                                FieldState<String> identifier,
                                FieldState<?> currentValue,
                                boolean writingTextField,
                                int characterCounter) implements JsonWriterState {

    public ObjectWriterState() {
        this(null, new Stack<>(), FieldType.UNKNOWN, FieldState.identifier("", WriterStatus.NOT_STARTED), new FieldState<>(new Object(), UNKNOWN, WriterStatus.NOT_STARTED), false, 0);
    }

    public ObjectWriterState(JsonObject mainObject, Stack<Token> tokenStack, FieldState<String> identifier, FieldState<?> currentValue, boolean receivedDoubleQuote, int characterCounter) {
        this(mainObject, tokenStack, FieldType.UNKNOWN, identifier, currentValue, receivedDoubleQuote, characterCounter);
    }

    public ObjectWriterState incrementCharacterCounter() {
        return new ObjectWriterState(this.mainObject, this.tokenStack, this.currentFieldType, this.identifier, this.currentValue, this.writingTextField, this.characterCounter + 1);
    }

    public ObjectWriterState incrementCharacterCounterBy(int number) {
        return new ObjectWriterState(this.mainObject, this.tokenStack, this.currentFieldType, this.identifier, this.currentValue, this.writingTextField, this.characterCounter + number);
    }

    public ObjectWriterState addInitialMainObject() {
        return new ObjectWriterState(new JsonObject(null), this.tokenStack, this.identifier, this.currentValue, this.writingTextField, this.characterCounter);
    }

    public ObjectWriterState addToken(Token token) {
        var newStack = this.tokenStack.stream().collect(Collectors.toCollection(Stack::new));
        newStack.add(token);

        return new ObjectWriterState(this.mainObject, newStack, this.currentFieldType, this.identifier, this.currentValue, this.writingTextField, this.characterCounter);
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
        return this.currentValue == null ? NOT_STARTED : this.currentValue.status();
    }

    public ObjectWriterState receiveDoubleQuote() {
        return new ObjectWriterState(this.mainObject, this.tokenStack, this.currentFieldType, this.identifier, this.currentValue, !this.writingTextField, this.characterCounter);
    }

    public ObjectWriterState writeCharacterToIdentifier(Character character) {
        return new ObjectWriterState(this.mainObject, this.tokenStack, FieldState.identifier(this.identifier.value().concat(character.toString()), this.identifier.status()), this.currentValue, this.writingTextField, this.characterCounter);
    }

    public ObjectWriterState writeCharacterToValueField(char character) {
        return updateValueField(concatValueField(currentValue.value(), character));
    }

    static String concatValueField(Object currentValue, char character) {
        return Optional.ofNullable(currentValue)
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(str -> str.concat(Character.valueOf(character).toString()))
                .orElseGet(() -> Character.valueOf(character).toString());
    }

    private ObjectWriterState updateValueField(Object newObjectToBeAdded) {
        return updateValueField(newObjectToBeAdded, this.currentValue.fieldType());
    }

    private ObjectWriterState updateValueField(Object newObjectToBeAdded, FieldType fieldType) {
        return new ObjectWriterState(this.mainObject, this.tokenStack, this.currentFieldType != ARRAY ? fieldType : ARRAY, this.identifier, new FieldState<>(newObjectToBeAdded, this.currentValue.fieldType(), WRITING), this.writingTextField, this.characterCounter);
    }

    public ObjectWriterState moveIdentifierToWritingState() {
        return new ObjectWriterState(this.mainObject, this.tokenStack, this.currentFieldType, FieldState.identifier(this.identifier.value(), WRITING), this.currentValue(), this.writingTextField, this.characterCounter);
    }

    public ObjectWriterState moveIdentifierToFinishState() {
        return new ObjectWriterState(this.mainObject, this.tokenStack, FieldState.identifier(this.identifier.value(), FINISHED), this.currentValue(), this.writingTextField, this.characterCounter);
    }

    public ObjectWriterState moveValueFieldToFinishState() {
        return flushNode();
    }

    public ObjectWriterState writeObjectToValueField(JsonObject parsedObject) {
        var updatedState = new ObjectWriterState(this.mainObject, this.addToken(Token.BRACE_CLOSED).tokenStack, OBJECT, this.identifier, new FieldState<>(parsedObject, OBJECT, FINISHED), false, this.characterCounter);

        return updatedState.flushNode();
    }

    public ObjectWriterState writeArrayToValueField(List<Object> array) {
        var updatedState = new ObjectWriterState(this.mainObject, this.addToken(Token.BRACE_CLOSED).tokenStack, ARRAY, this.identifier, new FieldState<>(array, ARRAY, FINISHED), false, this.characterCounter);

        return updatedState.flushNode();
    }

    public ObjectWriterState moveValueFieldToWritingState(FieldType fieldType) {
        return new ObjectWriterState(this.mainObject, this.tokenStack, this.currentFieldType, this.identifier, new FieldState<>("", fieldType, WRITING), this.writingTextField, this.characterCounter);
    }

    public ObjectWriterState moveValueFieldToWritingStateForStringValue() {
        return new ObjectWriterState(this.mainObject, this.tokenStack, this.currentFieldType, this.identifier, new FieldState<>("", STRING, WRITING), this.writingTextField, this.characterCounter);
    }


    public ObjectWriterState moveValueFieldToNotStartedState() {
        return new ObjectWriterState(this.mainObject, this.tokenStack, UNKNOWN, this.identifier, new FieldState<>(new Object(), UNKNOWN, NOT_STARTED), this.writingTextField, this.characterCounter);
    }

    private ObjectWriterState flushNode() {
        var jsonNodes = mainObject.jsonNodes();
        var valueToBeFlushed = currentValue.value();
        var node = createJsonNodeOfCorrectType(new JsonNode(identifier.value(), valueToBeFlushed));

        if (jsonNodes == null) {
            jsonNodes = new JsonNode[]{node};
        } else {
            var list = new ArrayList<>(Arrays.asList(jsonNodes));
            list.add(node);
            jsonNodes = list.toArray(JsonNode[]::new);
        }

        return new ObjectWriterState(new JsonObject(jsonNodes), this.tokenStack, FieldState.identifier("", WriterStatus.NOT_STARTED), new FieldState<>(new Object(), UNKNOWN, WriterStatus.NOT_STARTED), false, this.characterCounter);
    }

    private JsonNode createJsonNodeOfCorrectType(JsonNode jsonNode) {
        if (currentFieldType == STRING || currentFieldType == ARRAY || currentFieldType == OBJECT) {
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

    @Override
    public boolean isProcessingNonTextValue(char character) {
        return getLastToken()
                .filter(isIn(SEMI_COLON, SQ_BRACKET_OPEN, SPACE, NUMBER, BOOLEAN))
                .isPresent() &&
                findToken(character).filter(Token::isJsonFormatToken).isEmpty();
    }
}
