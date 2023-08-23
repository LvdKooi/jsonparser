package nl.kooi.jsonparser.parser.state;

import nl.kooi.jsonparser.json.JsonObject;
import io.github.lvdkooi.Conditional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;

import static nl.kooi.jsonparser.parser.state.FieldType.*;
import static nl.kooi.jsonparser.parser.state.Token.BOOLEAN;
import static nl.kooi.jsonparser.parser.state.Token.NUMBER;
import static nl.kooi.jsonparser.parser.state.Token.*;
import static nl.kooi.jsonparser.parser.state.WriterStatus.*;
import static nl.kooi.jsonparser.parser.util.ParserUtil.findToken;
import static nl.kooi.jsonparser.parser.util.ParserUtil.isIn;

public record ArrayWriterState(List<Object> array,
                               Stack<Token> tokenStack,
                               FieldState<Object> currentValue,
                               boolean writingTextField,
                               int characterCounter) implements JsonWriterState {

    public ArrayWriterState() {
        this(null, new Stack<>(), new FieldState<>(new Object(), UNKNOWN, WriterStatus.NOT_STARTED), false, 0);
    }

    public ArrayWriterState incrementCharacterCounter() {
        return new ArrayWriterState(this.array, this.tokenStack, this.currentValue, this.writingTextField, this.characterCounter + 1);
    }

    public ArrayWriterState incrementCharacterCounterBy(int number) {
        return new ArrayWriterState(this.array, this.tokenStack, this.currentValue, this.writingTextField, this.characterCounter + number);
    }

    public ArrayWriterState addInitialArray() {
        return new ArrayWriterState(new ArrayList<>(), this.tokenStack, this.currentValue, this.writingTextField, this.characterCounter);
    }

    public ArrayWriterState addToken(Token token) {
        var newStack = this.tokenStack.stream().collect(Collectors.toCollection(Stack::new));
        newStack.add(token);

        return new ArrayWriterState(this.array, newStack, this.currentValue, this.writingTextField, this.characterCounter);
    }

    public Optional<Token> getLastToken() {
        return Optional.ofNullable(this.tokenStack)
                .filter(stack -> !stack.isEmpty())
                .map(Stack::peek);
    }

    public WriterStatus valueFieldStatus() {
        return this.currentValue == null ? NOT_STARTED : this.currentValue.status();
    }

    public ArrayWriterState receiveDoubleQuote() {
        return new ArrayWriterState(this.array, this.tokenStack, this.currentValue, !this.writingTextField, this.characterCounter);
    }

    public ArrayWriterState writeCharacterToValueField(char character) {
        return updateValueField(concatValueField(currentValue.value(), character));
    }

    static String concatValueField(Object currentValue, char character) {
        return Optional.ofNullable(currentValue)
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(str -> str.concat(Character.valueOf(character).toString()))
                .orElseGet(() -> Character.valueOf(character).toString());
    }


    private ArrayWriterState updateValueField(Object newObjectToBeAdded) {
        return new ArrayWriterState(this.array, this.tokenStack, new FieldState<>(newObjectToBeAdded, this.currentValue.fieldType(), WRITING), this.writingTextField, this.characterCounter);
    }

    public ArrayWriterState moveValueFieldToFinishState() {
        return addValueToArray();
    }

    public ArrayWriterState addValueToArray() {
        var newArray = new ArrayList<>(this.array);
        newArray.add(formatType(this.currentValue));

        return new ArrayWriterState(newArray, this.tokenStack, new FieldState<>(new Object(), UNKNOWN, WriterStatus.NOT_STARTED), this.writingTextField, this.characterCounter);
    }

    public ArrayWriterState writeObjectToValueField(JsonObject parsedObject) {
        var updatedState = new ArrayWriterState(this.array, this.addToken(Token.BRACE_CLOSED).tokenStack, new FieldState<>(parsedObject, OBJECT, FINISHED), false, this.characterCounter);

        return updatedState.addValueToArray();
    }

    public ArrayWriterState addArrayToArray(List<Object> array) {
        var newArray = new ArrayList<>(this.array);
        newArray.add(array);

        return new ArrayWriterState(newArray, this.addToken(Token.SQ_BRACKET_CLOSED).tokenStack, new FieldState<>(new Object(), UNKNOWN, WriterStatus.NOT_STARTED), this.writingTextField, this.characterCounter);
    }

    private Object formatType(FieldState<Object> fieldState) {
        return Conditional.apply((FieldState<Object> fs) -> fs.value())
                .when(fs -> fs.fieldType() == STRING || fs.fieldType() == OBJECT)
                .orApply(fs -> handleNumberType(fs.value().toString()))
                .when(fs -> isNumber(fs.value().toString()))
                .orApply(fs -> null)
                .when(fs -> isNullValue(fs.value().toString()))
                .applyToOrElseGet(fieldState, () -> Boolean.valueOf(fieldState.value().toString()));
    }

    private boolean isNullValue(String value) {
        return value.equals("null");
    }

    public ArrayWriterState moveValueFieldToWritingState(FieldType fieldType) {
        return new ArrayWriterState(this.array, this.tokenStack, new FieldState<>("", fieldType, WRITING), this.writingTextField, this.characterCounter);
    }

    public ArrayWriterState moveValueFieldToWritingStateForStringValue() {
        return moveValueFieldToWritingState(STRING);
    }

    private Number handleNumberType(String numberString) {
        if (numberString == null) {
            return null;
        }

        try {
            return Integer.valueOf(numberString);
        } catch (NumberFormatException exc) {
            return Double.valueOf(numberString);
        }
    }

    private boolean isNumber(String numberString) {
        try {
            Double.valueOf(numberString.trim());
            return true;
        } catch (NumberFormatException exc) {
            return false;
        }
    }

    public boolean isProcessingNonTextValue(char character) {
        return getLastToken()
                .filter(isIn(SEMI_COLON, SQ_BRACKET_OPEN, SPACE, NUMBER, BOOLEAN))
                .isPresent() &&
                findToken(character).filter(Token::isJsonFormatToken).isEmpty();
    }
}
