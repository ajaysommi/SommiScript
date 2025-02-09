package plc.project.lexer;

import org.checkerframework.checker.signature.qual.Identifier;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;

/**
 * The lexer works through a combination of {@link #lex()}, which repeatedly
 * calls {@link #lexToken()} and skips over whitespace/comments, and
 * {@link #lexToken()}, which determines the type of the next token and
 * delegates to the corresponding lex method.
 *
 * <p>Additionally, {@link CharStream} manages the lexer state and contains
 * {@link CharStream#peek} and {@link CharStream#match}. These are helpful
 * utilities for working with character state and building tokens.
 */
public final class Lexer {

    private final CharStream chars;
    boolean ThrowException = false;
    public Lexer(String input) {
        chars = new CharStream(input);
    }

    public List<Token> lex() throws LexException {
        var tokens = new ArrayList<Token>();
        while (chars.has(0)) {
            Token temp = lexToken();
            System.out.println(temp);
            if (temp != null) {  //does not add to tokens if whitespace or comment
                tokens.add(temp);
                if (ThrowException) {
                    throw new LexException("Exception Thrown! Do Not Resist");
                }
            }
        }
        return tokens;
    }

    private void lexComment() {
        if (chars.match("\\/")) {
            while (chars.match("[^\\n\\r]*")) {}
        }
    }

    private Token lexToken() throws LexException {
        if (chars.match("\\/")) {
            lexComment();
        } else if (chars.match("[A-Za-z_]")) {
            return lexIdentifier();
        } else if (chars.match("-")) {
            if (chars.match("[0-9]")) {
                return lexNumber();
            }
            return new Token(Token.Type.OPERATOR, chars.emit());
        } else if (chars.match("\\+")) {
            if (chars.match("[0-9]")) {
                return lexNumber();
            }
            return new Token(Token.Type.OPERATOR, chars.emit());
        } else if (chars.match("[+-]?[0-9]+")) {
            if (chars.peek("e", "[^0-9]*")
                    || (!chars.has(1) && !chars.peek("[0-9]"))) {
                return new Token(Token.Type.INTEGER, chars.emit());
            }
            else if (chars.peek("\\.", "[^0-9]*")
                    || (!chars.has(1) && !chars.peek("[0-9]"))) {
                return new Token(Token.Type.INTEGER, chars.emit());
            }
            return lexNumber();
        } else if (chars.match("'")) {
            if (chars.peek("([^'\\n\\r]|(\\\\[bnrt'\\\"]))", "'")
                    || chars.peek("([^'\\n\\r]|(\\\\[bnrt'\\\"]))", "([^'\\n\\r]|(\\\\[bnrt'\\\"]))", "'")) {
                return lexCharacter();
            }
            ThrowException = true;
            return new Token(Token.Type.OPERATOR, chars.emit());
        } else if (chars.match("\"")) {
            return lexString();
        } else if (chars.match("\\\\")) {
            return lexEscape();
        } else if (chars.match("[ \\n\\r\\t]+")) {
            lexWhitespace();
        } else if (chars.match("[<>!=]", "[=]?")) {
            System.out.println("first");
            return lexOperator();
        } else if (chars.match("[^A-Za-z_0-9'\"\\n\\r\\t]")) {
            System.out.println("next");
            return lexOperator();
        }
        return null;
    }

    private Token lexIdentifier() throws LexException {
        while (chars.match("[A-Za-z0-9_-]")) {}
        return new Token(Token.Type.IDENTIFIER, chars.emit());
    }

    private Token lexNumber() throws LexException {
        int decimal_flag = 0;  //keeps track of whether decimal or integer
        boolean anything_after_e = false;  //keeps track of characters after e
        try {
            if (chars.peek("\\.", "[^0-9]")) {
                return new Token(Token.Type.INTEGER, chars.emit());
            }
            if (chars.peek("\\.", "[0-9]")) {
                decimal_flag++;
            }
            if (chars.peek("e", "[^0-9]")) {
                return new Token(Token.Type.INTEGER, chars.emit());
            }
            if (chars.match("e")) {
                //if peek fails, return existing along with e as separate identifier
                if (!chars.peek("\\+?-?[0-9]+")) {
                    if (decimal_flag == 0) {
                        return new Token(Token.Type.INTEGER, chars.emit());
                    }
                    else if (decimal_flag == 1) {
                        return new Token(Token.Type.DECIMAL, chars.emit());
                    }
                }
                while (chars.match("\\+?-?[0-9]+")) {
                    anything_after_e = true;
                }
                if (anything_after_e && decimal_flag == 0) {
                    return new Token(Token.Type.INTEGER, chars.emit());
                }
                else if (anything_after_e && decimal_flag == 1) {
                    return new Token(Token.Type.DECIMAL, chars.emit());
                }
                else if (!anything_after_e && decimal_flag == 0) {
                    System.out.println("nothing after");
                    throw new LexException("nothing after");
                }
                else if (!anything_after_e && decimal_flag == 1) {
                    System.out.println("nothing after");
                    throw new LexException("nothing after");
                }
                else {
                    return new Token(Token.Type.IDENTIFIER, chars.emit());
                }
            }
            while (chars.match("[0-9]*(\\.?[0-9]*)?(e\\+?-?[0-9]*)?")) {
                if (chars.peek("\\.", "[^0-9]")) {
                    return new Token(Token.Type.INTEGER, chars.emit());
                }
                if (chars.match("\\.")) {
                    decimal_flag++;
                    while (chars.match("[0-9]+(e\\+?-?[0-9]+)?")) {
                        if (chars.peek("e", "[^0-9]")) {
                            if (decimal_flag == 1) {
                                return new Token(Token.Type.DECIMAL, chars.emit());
                            }
                            return new Token(Token.Type.INTEGER, chars.emit());
                        }
                        if (chars.match("e", "[0-9]")) {
                            while (chars.match("\\+?-?[0-9]+")) {}
                            if (decimal_flag == 1) {
                                return new Token(Token.Type.DECIMAL, chars.emit());
                            }
                            else if (decimal_flag >= 2) {
                                throw new LexException("Too many decimals!");
                            }
                            return new Token(Token.Type.INTEGER, chars.emit());
                        }
                    }
                    if (decimal_flag == 1) {
                        return new Token(Token.Type.DECIMAL, chars.emit());
                    }
                    else if (decimal_flag >= 2) {
                        throw new LexException("Too many decimals!");
                    }
                    return new Token(Token.Type.INTEGER, chars.emit());
                }
                if (chars.peek("e", "[^0-9]")) {
                    if (decimal_flag == 1) {
                        return new Token(Token.Type.DECIMAL, chars.emit());
                    }
                    return new Token(Token.Type.INTEGER, chars.emit());
                }
                if (chars.match("e", "[0-9]")) {
                    while (chars.match("\\+?-?[0-9]+")) {}
                    if (decimal_flag == 1) {
                        return new Token(Token.Type.DECIMAL, chars.emit());
                    }
                    else if (decimal_flag >= 2) {
                        throw new LexException("Too many decimals!");
                    }
                    return new Token(Token.Type.INTEGER, chars.emit());
                }
            }
            if (decimal_flag == 1) {
                return new Token(Token.Type.DECIMAL, chars.emit());
            }
            else if (decimal_flag >= 2) {
                throw new LexException("Too many decimals!");
            }
            return new Token(Token.Type.INTEGER, chars.emit());
        }
        catch (Exception exception) {
            throw new LexException("invalid syntax!");
        }
    }

    private Token lexCharacter() throws LexException {
        int char_counter = 0;
        try {
            while (chars.match("([^'\\n\\r]|(\\\\[bnrt'\\\"]))")) {
                char_counter++;
            }
            //check if only 1-2 characters (if escape character) and closing '
            if (chars.match("'") && char_counter <= 2) {
                return new Token(Token.Type.CHARACTER, chars.emit());
            }
            else {
                throw new LexException("Missing \\'");
            }
        }
        catch (Exception exception) {
            throw new LexException("Exception Caught!");
        }

    }

    private Token lexString() throws LexException {
        try {
            while (chars.match("([^\"\\n\\r]*(\\\\[bnrt\"\\\\])*)*")) {
                if (chars.match("\\\\")) {
                    if (chars.match("[bnrt\"\\\\]")) {
                        continue;
                    }
                    throw new LexException("Invalid escape!");
                }
            }
            if (chars.match("\"")) {
                return new Token(Token.Type.STRING, chars.emit());
            }
            throw new LexException("No Closing Quote!");
        }
        catch (Exception exception) {
            throw new LexException("Error in String!");
        }
    }

    private Token lexEscape() throws LexException {
        throw new LexException("Escaped");
    }

    public Token lexOperator() throws LexException {
        while (chars.match("[=]?")) {}
        return new Token(Token.Type.OPERATOR, chars.emit());
    }

    private void lexWhitespace() {
        chars.length--;
    }


    /**
     * A helper class for maintaining the state of the character stream (input)
     * and methods for building up token literals.
     */
    private static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        /**
         * Returns true if the next characters match their corresponding
         * pattern. Each pattern is a regex matching only ONE character!
         */
        public boolean peek(String... patterns) {
            //match on charactercharacter by character
            if (!has(patterns.length - 1)) {
                return false;
            }
            for (int offset = 0; offset < patterns.length; offset++) {
                var character = input.charAt(index + offset);
                if (!String.valueOf(character).matches(patterns[offset])) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Equivalent to peek, but also advances the character stream.
         */
        public boolean match(String... patterns) {
            var peek = peek(patterns);
            if (peek) {
                index += patterns.length;
                length += patterns.length;
            }
            return peek;
        }

        /**
         * Returns the literal built by all characters matched since the last
         * call to emit(); also resetting the length for subsequent tokens.
         */
        public String emit() {
            var literal = input.substring(index - length, index);
            length = 0;
            return literal;
        }

    }

}
