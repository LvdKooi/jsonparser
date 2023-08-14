package nl.kooi.jsonparser.parser.state;

public interface JsonWriterState {
    boolean writingTextField();

    boolean isProcessingNonTextValue(char character);
}
