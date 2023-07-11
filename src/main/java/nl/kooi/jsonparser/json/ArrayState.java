package nl.kooi.jsonparser.json;

import java.util.ArrayList;
import java.util.List;

public record ArrayState(List<Object> value) {

    public static ArrayState initialType() {
        return new ArrayState(new ArrayList<>());
    }
}
