package nl.kooi.jsonparser.json;

import java.util.Objects;

public record Pair<T, V>(T left, V right) {

    public Pair {
        Objects.requireNonNull(left);
        Objects.requireNonNull(right);
    }
}
