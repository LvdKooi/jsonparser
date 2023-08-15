package nl.kooi.jsonparser.parser.util;

import nl.kooi.jsonparser.monad.Conditional;
import nl.kooi.jsonparser.parser.command.TokenCommand;
import nl.kooi.jsonparser.parser.state.JsonWriterState;
import nl.kooi.jsonparser.parser.state.Token;
import nl.kooi.jsonparser.parser.state.WriterStatus;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static nl.kooi.jsonparser.parser.state.Token.D_QUOTE;
import static nl.kooi.jsonparser.parser.state.Token.SPACE;
import static nl.kooi.jsonparser.parser.state.WriterStatus.WRITING;

public class ParserUtil {

    public static String getNestedArrayString(char[] stillToBeProcessed) {
        return getNestedString(stillToBeProcessed, '[', ']');
    }

    public static String getNestedObjectString(char[] stillToBeProcessed) {
        return getNestedString(stillToBeProcessed, '{', '}');
    }

    private static String getNestedString(char[] stillToBeProcessed, char openCharacter, char closingCharacter) {
        var openBraceCounter = 0;
        var currentString = "";
        var closedBraceCounter = 0;

        for (var character : stillToBeProcessed) {
            currentString = currentString.concat(String.valueOf(character));

            if (character == closingCharacter) {
                closedBraceCounter++;
            }
            if (character == openCharacter) {
                openBraceCounter++;
            }

            if (closedBraceCounter == openBraceCounter) {
                break;
            }
        }

        return currentString;
    }

    public static boolean isSpace(char character) {
        return SPACE.getMatchingCharacter()
                .filter(tokenMatchesChar(character))
                .isPresent();
    }

    public static Predicate<Character> tokenMatchesChar(char character) {
        return tokenCharacter -> Optional.ofNullable(tokenCharacter)
                .filter(tokenChar -> tokenChar.equals(character))
                .isPresent();
    }

    public static boolean isNumber(char character) {
        try {
            Integer.valueOf(character);
            return true;
        } catch (NumberFormatException exc) {
            return false;
        }
    }

    public static boolean isDecimalPoint(char character) {
        return '.' == character;
    }

    public static boolean isMinus(char character) {
        return '-' == character;
    }

    public static boolean isNumberRelatedCharacter(char character) {
        return isNumber(character) || isDecimalPoint(character) || isMinus(character);
    }

    public static boolean isDoubleQuote(char character) {
        return D_QUOTE.getMatchingCharacter()
                .filter(tokenMatchesChar(character))
                .isPresent();
    }

    public static Optional<Token> findToken(char character) {
        return Arrays.stream(Token.values()).filter(Objects::nonNull).filter(token -> token.getMatchingCharacter().filter(tokenChar -> tokenChar.equals(character)).isPresent()).findFirst();

    }

    public static Predicate<Token> isIn(Token... tokens) {
        return t -> Set.of(tokens).contains(t);
    }

    public static <T extends JsonWriterState> Conditional<T, Optional<TokenCommand<T>>> getOptionalTokenCommand(TokenCommand<T> command) {
        return Conditional
                .apply(command::forText)
                .when(writerState -> writerState.writingTextField() && !isDoubleQuote(command.character()))
                .orApply(command::forSpace)
                .when(writerState -> !writerState.writingTextField() && isSpace(command.character()))
                .orApply(command::forNumber)
                .when(writerState -> writerState.isProcessingNonTextValue(command.character()) && isNumberRelatedCharacter(command.character()))
                .orApply(command::forBoolean)
                .when(writerState -> writerState.isProcessingNonTextValue(command.character()))
                .map(Optional::of);
    }

    public static boolean hasWritingStatus(Supplier<WriterStatus> statusSupplier) {
        return hasStatus(statusSupplier, WRITING);
    }

    public static boolean hasStatus(Supplier<WriterStatus> statusSupplier, WriterStatus match) {
        return statusSupplier.get() == match;
    }

    public static boolean hasStatusNotIn(Supplier<WriterStatus> statusSupplier, WriterStatus... statuses) {
        return !Set.of(statuses).contains(statusSupplier.get());
    }
}
