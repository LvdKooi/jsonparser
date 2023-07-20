package nl.kooi.jsonparser.parser.state;

import java.util.Objects;

public record FieldState<T>(T value, FieldType fieldType, WriterStatus status) {

    public FieldState {
        Objects.requireNonNull(value);
        Objects.requireNonNull(fieldType);
        Objects.requireNonNull(status);
    }

    public static FieldState<String> identifier(String currentValue, WriterStatus status) {
        return new FieldState<>(currentValue, FieldType.STRING, status);
    }
}
