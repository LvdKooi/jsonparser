package nl.kooi.jsonparser.json;

import java.util.Objects;

public record FieldState<T>(T currentValue, FieldType fieldType, WriterStatus status) {

    public FieldState {
        Objects.requireNonNull(currentValue);
        Objects.requireNonNull(fieldType);
        Objects.requireNonNull(status);
    }

    public static FieldState<String> forStringField(String currentValue, WriterStatus status) {
        return new FieldState<>(currentValue, FieldType.STRING, status);
    }
}
