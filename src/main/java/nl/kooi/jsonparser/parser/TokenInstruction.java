package nl.kooi.jsonparser.parser;

import nl.kooi.jsonparser.json.Token;
import nl.kooi.jsonparser.json.WriterState;

public record TokenInstruction(Token token, String character, WriterState state) {
}
