package plc.project.parser;

import plc.project.lexer.Token;

import java.util.List;

import static com.google.common.base.Preconditions.checkState;

/**
 * This style of parser is called <em>recursive descent</em>. Each rule in our
 * grammar has dedicated function, and references to other rules correspond to
 * calling that function. Recursive rules are therefore supported by actual
 * recursive calls, while operator precedence is encoded via the grammar.
 *
 * <p>The parser has a similar architecture to the lexer, just with
 * {@link Token}s instead of characters. As before, {@link TokenStream#peek} and
 * {@link TokenStream#match} help with traversing the token stream. Instead of
 * emitting tokens, you will instead need to extract the literal value via
 * {@link TokenStream#get} to be added to the relevant AST.
 */
public final class Parser {

    private final TokenStream tokens;

    //list of tokens passed in ex: ([token1: ident, literal: LET], [token2:......])
    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    public Ast.Source parseSource() throws ParseException {
        throw new UnsupportedOperationException("TODO"); //TODO
        //loop through tokens and call whatever
        //check if statement or expression, then call that
        //call stmt, that will call specific stmt, which calls expr, which calls specific expr
    }

    public Ast.Stmt parseStmt() throws ParseException {
        if (tokens.peek("LET")) {
            parseLetStmt();
            parseSource();  //recursive call back to base
        }
        else if (tokens.peek("DEF")) {
            parseDefStmt();
        }
        else if (tokens.peek("DEF")) {
            parseDefStmt();
        }
        else if (tokens.peek("IF")) {
            parseIfStmt();
        }
        else if (tokens.peek("FOR")) {
            parseForStmt();
        }
        else if (tokens.peek("RETURN")) {
            parseReturnStmt();
        }
        else if (tokens.peek("DEF")) {
            parseExpressionOrAssignmentStmt();
        }
        //either start here or expr. create loop for tokens
        //while tokens.....
        //add to a new array of __ objects (maybe ast), returns that
        return null;
    }

    private Ast.Stmt.Let parseLetStmt() throws ParseException {
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    private Ast.Stmt.Def parseDefStmt() throws ParseException {
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    private Ast.Stmt.If parseIfStmt() throws ParseException {
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    private Ast.Stmt.For parseForStmt() throws ParseException {
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    private Ast.Stmt.Return parseReturnStmt() throws ParseException {
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    private Ast.Stmt parseExpressionOrAssignmentStmt() throws ParseException {
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    public Ast.Expr parseExpr() throws ParseException {
        try{
            return parseLogicalExpr();  //simply calls logical expression
        }
        catch (Exception exception) {
            throw new ParseException("Logical expression not valid");
        }
    }

    private Ast.Expr parseLogicalExpr() throws ParseException {
        return parseComparisonExpr();
    }

    private Ast.Expr parseComparisonExpr() throws ParseException {

        throw new UnsupportedOperationException("TODO"); //TODO
    }

    private Ast.Expr parseAdditiveExpr() throws ParseException {
        //call multiplicativeexpr, order of recursiveness
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    private Ast.Expr parseMultiplicativeExpr() throws ParseException {
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    private Ast.Expr parseSecondaryExpr() throws ParseException {
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    private Ast.Expr parsePrimaryExpr() throws ParseException {
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    private Ast.Expr.Literal parseLiteralExpr() throws ParseException {
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    private Ast.Expr.Group parseGroupExpr() throws ParseException {
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    private Ast.Expr.ObjectExpr parseObjectExpr() throws ParseException {
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    private Ast.Expr parseVariableOrFunctionExpr() throws ParseException {
        checkState(tokens.match(Token.Type.IDENTIFIER));
        var name = tokens.get(-1).literal();
        return new Ast.Expr.Variable(name); //todo functions
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at (index + offset).
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Returns the token at (index + offset).
         */
        public Token get(int offset) {
            checkState(has(offset));
            return tokens.get(index + offset);
        }

        /**
         * Returns true if the next characters match their corresponding
         * pattern. Each pattern is either a {@link Token.Type}, matching tokens
         * of that type, or a {@link String}, matching tokens with that literal.
         * In effect, {@code new Token(Token.Type.IDENTIFIER, "literal")} is
         * matched by both {@code peek(Token.Type.IDENTIFIER)} and
         * {@code peek("literal")}.
         */
        public boolean peek(Object... patterns) {
            if (!has(patterns.length - 1)) {
                return false;
            }
            //patterns.length is # of args passed in
            for (int offset = 0; offset < patterns.length; offset++) {
                var token = tokens.get(index + offset);
                var pattern = patterns[offset];
                checkState(pattern instanceof Token.Type || pattern instanceof String, pattern);
                if (!token.type().equals(pattern) && !token.literal().equals(pattern)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Equivalent to peek, but also advances the token stream.
         */
        public boolean match(Object... patterns) {
            var peek = peek(patterns);
            if (peek) {
                index += patterns.length;
            }
            return peek;
        }

    }

}
