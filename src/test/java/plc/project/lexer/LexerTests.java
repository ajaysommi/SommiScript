package plc.project.lexer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

public final class LexerTests {

    @ParameterizedTest
    @MethodSource
    void testWhitespace(String test, String input, boolean success) {
        test(input, List.of(), success);
    }

    public static Stream<Arguments> testWhitespace() {
        return Stream.of(
            Arguments.of("Space", " ", true),
            Arguments.of("Newline", "\n", true),
            Arguments.of("Many repeated", "      ", true),
            Arguments.of("Multiple", "   \n   ", true)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testComment(String test, String input, boolean success) {
        test(input, List.of(), success);
    }

    public static Stream<Arguments> testComment() {
        return Stream.of(
            Arguments.of("Comment", "//comment", true),
            Arguments.of("Multiple", "//first\n//second", true),
            Arguments.of("Three comments", "//first\n//second\n//another", true),
            Arguments.of("One backslash", "/onesinglebackslash", false),
            Arguments.of("Invalid escape", "//first\bsecond", true),
            Arguments.of("Valid escape", "//first\nsecond", false),
            Arguments.of("No comment", "//", true),
            Arguments.of("Should ignore", "//44<>string'c'//", true)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testIdentifier(String test, String input, boolean success) {
        test(input, List.of(new Token(Token.Type.IDENTIFIER, input)), success);
    }

    public static Stream<Arguments> testIdentifier() {
        return Stream.of(
            Arguments.of("Alphabetic", "getName", true),
            Arguments.of("Alphanumeric", "thelegend27", true),
            Arguments.of("Leading Hyphen", "-five", false),
            Arguments.of("Leading Digit", "1fish2fish", false),
            Arguments.of("Single character", "I", true),
            Arguments.of("Long with all other tokens", "Includes+-<>'s'904305", false),
            Arguments.of("Extreme limits", "IdentZUaizo930__aj-a", true),
            Arguments.of("String", "\"amianidentifier\"", false),
            Arguments.of("All underscores", "_______", true),
            Arguments.of("Period", ".", false),
            Arguments.of("Space in middle", "Ident ifier", false)
        );
    }

    //submission check

    @ParameterizedTest
    @MethodSource
    void testInteger(String test, String input, boolean success) {
        test(input, List.of(new Token(Token.Type.INTEGER, input)), success);
    }

    public static Stream<Arguments> testInteger() {
        return Stream.of(
            Arguments.of("Single Digit", "1", true),
            Arguments.of("Multiple Digits", "123", true),
            Arguments.of("Exponent", "1e10", true),
            Arguments.of("Missing Exponent Digits", "1e", false),
            Arguments.of("Negative Number", "-123", true),
            Arguments.of("Two dashes", "--123", false),
            Arguments.of("Negative Decimal", "-1.30", false),
            Arguments.of("Completely Invalid", "-.23", false),
            Arguments.of("Two Digits", "21", true),
            Arguments.of("Many Digits", "999999", true),
            Arguments.of("Single Zero", "0", true),
            Arguments.of("Explicitly Positive", "+1", true),
            Arguments.of("Explicitly Negative", "-91", true),
            Arguments.of("Positive and Negative", "+-13", false),
            Arguments.of("String After Decimal", "21.toString", false),
            Arguments.of("Nothing After Decimal", "21.", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDecimal(String test, String input, boolean success) {
        test(input, List.of(new Token(Token.Type.DECIMAL, input)), success);
    }

    public static Stream<Arguments> testDecimal() {
        return Stream.of(
            Arguments.of("Integer", "1", false),
            Arguments.of("Multiple Digits", "123.456", true),
            Arguments.of("Exponent", "1.0e10", true),
            Arguments.of("Trailing Decimal", "11.", false),
            Arguments.of("Negative Integer", "-123", false),
            Arguments.of("Two dashes", "--12.3", false),
            Arguments.of("Negative Decimal", "-1.30", true),
            Arguments.of("Completely Invalid", "-.23", false),
            Arguments.of("Two Decimal Digits", "2.1", true),
            Arguments.of("Many Digits", "999.999", true),
            Arguments.of("Single Zero", "0", false),
            Arguments.of("Explicitly Positive", "+1.3", true),
            Arguments.of("Explicitly Negative", "-91", false),
            Arguments.of("Positive and Negative", "+-13", false),
            Arguments.of("Nothing after Decimal", "555.", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testCharacter(String test, String input, boolean success) {
        test(input, List.of(new Token(Token.Type.CHARACTER, input)), success);
    }

    public static Stream<Arguments> testCharacter() {
        return Stream.of(
            Arguments.of("Alphabetic", "\'c\'", true),
            Arguments.of("Newline Escape", "\'\\n\'", true),
            Arguments.of("Unterminated", "\'u", false),
            Arguments.of("Multiple", "\'abc\'", false),
            Arguments.of("Extra \'", "\'a\'\'", false),
            Arguments.of("Empty", "\'\'", false),
            Arguments.of("Multiple", "\'\\o\'", false),
            Arguments.of("Number", "\'7\'", true),
            Arguments.of("Escape", "\'\\\'", true),
            Arguments.of("Long Sequence", "\'904813305\'", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testString(String test, String input, boolean success) {
        test(input, List.of(new Token(Token.Type.STRING, input)), success);
    }

    public static Stream<Arguments> testString() {
        return Stream.of(
            Arguments.of("Empty", "\"\"", true),
            Arguments.of("Alphabetic", "\"string\"", true),
            Arguments.of("Newline Escape", "\"Hello,\\nWorld\"", true),
            Arguments.of("Invalid Escape", "\"invalid\\escape\"", false),
            Arguments.of("No Close", "\"open string", false),
            Arguments.of("Extra Quote", "\"extra quote\"\"", false),
            Arguments.of("No Quotes", "no quotes", false),
            Arguments.of("Just Whitespace", "\"      \"", true),
            Arguments.of("Backslash Escape", "\"escaped\\\\\"", true),
            Arguments.of("Escaped Quote", "\"escaped\\\"quote\"", true)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testOperator(String test, String input, boolean success) {
        test(input, List.of(new Token(Token.Type.OPERATOR, input)), success);
    }

    public static Stream<Arguments> testOperator() {
        return Stream.of(
            Arguments.of("Character", "(", true),
            Arguments.of("Comparison", "<=", true),
            Arguments.of("Boolean Equal", "==", true),
            Arguments.of("Extra Equal", "===", false),
            Arguments.of("Extra Equal", "[[", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testInteraction(String test, String input, List<Token> expected) {
        test(input, expected, true);
    }

    public static Stream<Arguments> testInteraction() {
        return Stream.of(
            Arguments.of("Whitespace", "first second", List.of(
                new Token(Token.Type.IDENTIFIER, "first"),
                new Token(Token.Type.IDENTIFIER, "second")
            )),
            Arguments.of("Identifier Leading Hyphen", "-five", List.of(
                new Token(Token.Type.OPERATOR, "-"),
                new Token(Token.Type.IDENTIFIER, "five")
            )),
            Arguments.of("Identifier Leading Digit", "1fish2fish", List.of(
                new Token(Token.Type.INTEGER, "1"),
                new Token(Token.Type.IDENTIFIER, "fish2fish")
            )),
            Arguments.of("Integer Missing Exponent Digits", "1e", List.of(
                new Token(Token.Type.INTEGER, "1"),
                new Token(Token.Type.IDENTIFIER, "e")
            )),
            Arguments.of("Decimal Missing Decimal Digits", "1.", List.of(
                new Token(Token.Type.INTEGER, "1"),
                new Token(Token.Type.OPERATOR, ".")
            )),
            Arguments.of("Operator Multiple Operators", "<=>", List.of(
                new Token(Token.Type.OPERATOR, "<="),
                new Token(Token.Type.OPERATOR, ">")
            ))
        );
    }

    @ParameterizedTest
    @MethodSource
    void testException(String test, String input) {
        Assertions.assertThrows(LexException.class, () -> new Lexer(input).lex());
    }

    public static Stream<Arguments> testException() {
        return Stream.of(
            Arguments.of("Character Unterminated", "\'u"),
            Arguments.of("Character Multiple", "\'abc\'"),
            Arguments.of("String Invalid Escape", "\"invalid\\escape\""),
            Arguments.of("Empty Character", "\'\'")
        );
    }

    @ParameterizedTest
    @MethodSource
    void testProgram(String test, String input, List<Token> expected) {
        test(input, expected, true);
    }

    public static Stream<Arguments> testProgram() {
        return Stream.of(
            Arguments.of("Variable", "LET x = 5;", List.of(
                new Token(Token.Type.IDENTIFIER, "LET"),
                new Token(Token.Type.IDENTIFIER, "x"),
                new Token(Token.Type.OPERATOR, "="),
                new Token(Token.Type.INTEGER, "5"),
                new Token(Token.Type.OPERATOR, ";")
            )),
            Arguments.of("Print Function", "print(\"Hello, World!\");", List.of(
                new Token(Token.Type.IDENTIFIER, "print"),
                new Token(Token.Type.OPERATOR, "("),
                new Token(Token.Type.STRING, "\"Hello, World!\""),
                new Token(Token.Type.OPERATOR, ")"),
                new Token(Token.Type.OPERATOR, ";")
            )),
            Arguments.of("Car's Mileage", "2007 Honda Civic = 195000 \"miles\"", List.of(
                    new Token(Token.Type.INTEGER, "2007"),
                    new Token(Token.Type.IDENTIFIER, "Honda"),
                    new Token(Token.Type.IDENTIFIER, "Civic"),
                    new Token(Token.Type.OPERATOR, "="),
                    new Token(Token.Type.INTEGER, "195000"),
                    new Token(Token.Type.STRING, "\"miles\"")
            ))
        );
    }

    private static void test(String input, List<Token> expected, boolean success) {
        if (success) {
            var tokens = Assertions.assertDoesNotThrow(() -> new Lexer(input).lex());
            Assertions.assertEquals(expected, tokens);
        } else {
            //Consider both different results or exceptions to be acceptable.
            //This is a bit lenient, but makes adding tests much easier.
            try {
                Assertions.assertNotEquals(expected, new Lexer(input).lex());
            } catch (LexException ignored) {}
        }
    }

}
