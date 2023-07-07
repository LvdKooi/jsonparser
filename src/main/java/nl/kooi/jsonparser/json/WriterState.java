package nl.kooi.jsonparser.json;

import java.util.ArrayList;
import java.util.Arrays;

import static nl.kooi.jsonparser.json.FieldType.STRING;
import static nl.kooi.jsonparser.json.FieldType.UNKNOWN;
import static nl.kooi.jsonparser.json.WriterStatus.*;

public record WriterState(JsonObject mainObject,
                          FieldType currentFieldType,
                          Pair<String, WriterStatus> identifier,
                          Pair<?, WriterStatus> valueField) {

    public WriterState() {
        this(null, FieldType.UNKNOWN, new Pair<>("", WriterStatus.NOT_STARTED), new Pair<>("", WriterStatus.NOT_STARTED));
    }

    public WriterState(JsonObject mainObject, Pair<String, WriterStatus> identifier, Pair<?, WriterStatus> stringField) {
        this(mainObject, FieldType.UNKNOWN, identifier, stringField);
    }

    public WriterState addInitialMainObject() {
        return new WriterState(new JsonObject(null), this.identifier, this.valueField);
    }

    public WriterStatus identifierStatus() {
        return this.identifier.right();
    }

    public WriterStatus valueFieldStatus() {
        return this.valueField == null ? NOT_STARTED : this.valueField.right();
    }

    public WriterState writeCharacterToIdentifier(String character) {
        return new WriterState(this.mainObject, new Pair<>(this.identifier.left().concat(character), this.identifier.right()), this.valueField);

    }

    public WriterState writeCharacterToValueField(String character) {
        return new WriterState(this.mainObject, this.currentFieldType, this.identifier, new Pair<>(((String) this.valueField.left()).concat(character), WRITING));
    }

    public WriterState moveIdentifierToWritingState() {
        return new WriterState(this.mainObject, this.currentFieldType, new Pair<>(this.identifier.left(), WRITING), this.valueField());
    }

    public WriterState moveIdentifierToFinishState() {
        return new WriterState(this.mainObject, new Pair<>(this.identifier.left(), FINISHED), this.valueField());
    }

    public WriterState moveValueFieldToFinishState() {
        return flushNode();
    }

    public WriterState moveValueFieldToWritingState(FieldType fieldType) {
        return new WriterState(this.mainObject, fieldType, this.identifier, new Pair<>("", WRITING));
    }

    public WriterState moveValueFieldToNotStartedState() {
        return new WriterState(this.mainObject, UNKNOWN, this.identifier, new Pair<>("", NOT_STARTED));
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

        return new WriterState(new JsonObject(jsonNodes), new Pair<>("", WriterStatus.NOT_STARTED), new Pair<>("", WriterStatus.NOT_STARTED));
    }

    private JsonNode createJsonNodeOfCorrectType(JsonNode jsonNode) {
        if (currentFieldType == STRING) {
            return jsonNode;
        }

        if (isNumber(jsonNode)) {
            return new JsonNode(jsonNode.identifier(), Integer.valueOf(((String) jsonNode.content()).trim()));
        }

        if (isBoolean(jsonNode)) {
            return new JsonNode(jsonNode.identifier(), Boolean.valueOf(((String) jsonNode.content()).trim()));
        }

        throw new UnsupportedOperationException("Other types than String, Boolean or Number are not supported yet");
    }

    private boolean isNumber(JsonNode jsonNode) {
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