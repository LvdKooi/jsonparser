package nl.kooi.jsonparser.json;

import java.util.ArrayList;
import java.util.Arrays;

import static nl.kooi.jsonparser.json.WriterStatus.FINISHED;
import static nl.kooi.jsonparser.json.WriterStatus.WRITING;

public record WriterState(JsonObject mainObject,
                          Pair<String, WriterStatus> identifier,
                          Pair<String, WriterStatus> stringField) {

    public WriterState() {
        this(null, new Pair<>("", WriterStatus.NOT_STARTED), new Pair<>("", WriterStatus.NOT_STARTED));
    }

    public WriterStatus identifierStatus() {
        return this.identifier.right();
    }

    public WriterStatus stringFieldStatus() {
        return this.stringField.right();
    }

    public WriterState writeCharacterToIdentifier(String character) {
        return new WriterState(this.mainObject, new Pair<>(this.identifier.left().concat(character), this.identifier.right()), this.stringField);

    }

    public WriterState writeCharacterToStringField(String character) {
        return new WriterState(this.mainObject, this.identifier, new Pair<>(this.stringField.left().concat(character), this.stringField.right()));
    }

    public WriterState moveIdentifierToWritingState() {
        return new WriterState(this.mainObject, new Pair<>(this.identifier.left(), WRITING), this.stringField());
    }

    public WriterState moveIdentifierToFinishState() {
        return new WriterState(this.mainObject, new Pair<>(this.identifier.left(), FINISHED), this.stringField());
    }

    public WriterState moveStringFieldToFinishState() {
        return new WriterState(this.mainObject, this.identifier, new Pair<>(this.stringField.left(), FINISHED));
    }

    public WriterState moveStringFieldToWritingState() {
        return new WriterState(this.mainObject, this.identifier, new Pair<>(this.stringField.left(), WRITING));
    }

    public WriterState writeNode() {

        var jsonNodes = mainObject.jsonNodes();

        if (jsonNodes == null) {
            jsonNodes = new JsonNode[]{new JsonNode(identifier.left(), stringField.left())};
        } else {
            var list = new ArrayList<>(Arrays.asList(jsonNodes));
            list.add(new JsonNode(identifier.left(), stringField.left()));
            jsonNodes = list.toArray(JsonNode[]::new);
        }

        return new WriterState(new JsonObject(jsonNodes), new Pair<>("", WriterStatus.NOT_STARTED), new Pair<>("", WriterStatus.NOT_STARTED));
    }
}
