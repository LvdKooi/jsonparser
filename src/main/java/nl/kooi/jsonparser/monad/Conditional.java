package nl.kooi.jsonparser.monad;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Conditional<T, R> {
    private final Queue<Pair<Predicate<T>, Function<T, R>>> actionQueue;
    private final Function<T, R> currentFunction;

    private Conditional(Queue<Pair<Predicate<T>, Function<T, R>>> actionQueue, Function<T, R> currentFunction) {
        this.actionQueue = actionQueue;
        this.currentFunction = currentFunction;
    }

    public static <T, R> Conditional<T, R> apply(Function<T, R> function) {
        Objects.requireNonNull(function);
        return new Conditional<>(new ArrayDeque<>(1), function);
    }

    public Conditional<T, R> when(Predicate<T> condition) {
        assertCurrentFunctionAndPredicateAreValid(condition);

        var queue = new ArrayDeque<>(this.actionQueue);
        queue.add(new Pair<>(condition, currentFunction));

        return new Conditional<>(queue, null);
    }

    public Conditional<T, R> orApply(Function<T, R> function) {
        Objects.requireNonNull(function);
        return new Conditional<>(this.actionQueue, function);
    }

    public <U> Conditional<T, U> map(Function<R, U> mapFunction) {
        Objects.requireNonNull(mapFunction);

        var queue = this.actionQueue
                .stream()
                .map(pair -> new Pair<>(pair.key(), pair.value().andThen(mapFunction)))
                .collect(Collectors.toCollection(ArrayDeque::new));

        return new Conditional<>(queue, null);
    }

    public R applyToOrElseGet(T object, Supplier<? extends R> supplier) {
        Objects.requireNonNull(supplier);

        return Optional.ofNullable(object)
                .flatMap(this::findMatchingFunction)
                .orElseGet(() -> obj -> supplier.get())
                .apply(object);
    }

    public R applyToOrElse(T object, R defaultValue) {
        return Optional.ofNullable(object)
                .flatMap(this::findMatchingFunction)
                .orElseGet(() -> obj -> defaultValue)
                .apply(object);
    }

    private Optional<Function<T, R>> findMatchingFunction(T t) {
        return actionQueue
                .stream()
                .filter(entry -> entry.key().test(t))
                .findFirst()
                .map(Pair::value);
    }

    private void assertCurrentFunctionAndPredicateAreValid(Predicate<T> predicate) {
        Objects.requireNonNull(currentFunction, "The function that belongs to this condition is not yet set. " +
                "A predicate can only be added after an apply(Function<T, R> function) or orApply(Function<T, R> function).");
        Objects.requireNonNull(predicate);
    }

    private record Pair<T, R>(T key, R value) {

        private Pair {
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);
        }
    }
}