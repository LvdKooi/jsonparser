package nl.kooi.jsonparser.json;

import java.util.ArrayList;
import java.util.List;

public record ArrayState(FieldType fieldType, List<String> value) {

    public static ArrayState initialType() {
        return new ArrayState(FieldType.UNKNOWN, new ArrayList<>());
    }
}
