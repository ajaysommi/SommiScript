package plc.project.parser;

import org.checkerframework.checker.units.qual.A;
import plc.project.lexer.Token;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
        var statement_list = new ArrayList<Ast.Stmt>();
        while (tokens.has(0)) {
            statement_list.add(parseStmt());
        }
        return new Ast.Source(statement_list);
    }

    public Ast.Stmt parseStmt() throws ParseException {
        if (tokens.match("LET")) {
            return parseLetStmt();
        }
        else if (tokens.match("DEF")) {
            return parseDefStmt();
        }
        else if (tokens.match("IF")) {
            return parseIfStmt();
        }
        else if (tokens.match("FOR")) {
            return parseForStmt();
        }
        else if (tokens.match("RETURN")) {
            return parseReturnStmt();
        }
        else {
            return parseExpressionOrAssignmentStmt();
        }
    }

    private Ast.Stmt.Let parseLetStmt() throws ParseException {
        if (!tokens.match(Token.Type.IDENTIFIER)) {
            throw new ParseException("No identifier!");
        }
        var name = tokens.get(-1).literal();
        Ast.Expr value = null;
        if (tokens.match("=")) {
            value = parseExpr();
        }
        if (!tokens.match(";")) {
            throw new ParseException("Missing semicolon!");
        }
        return new Ast.Stmt.Let(name, Optional.ofNullable(value));
    }

    private Ast.Stmt.Def parseDefStmt() throws ParseException {
        if (!tokens.match(Token.Type.IDENTIFIER)) {
            throw new ParseException("No identifier!");
        }
        boolean multiple_params = false;
        var name = tokens.get(-1).literal();
        var parameters = new ArrayList<String>();
        var body = new ArrayList<Ast.Stmt>();
        if (tokens.match("(")){
            //enters while-loop to check for identifiers
            while (!tokens.match(")") && tokens.has(0)) {
                if (multiple_params) {
                    if (tokens.match(",")) {
                        if (tokens.match(Token.Type.IDENTIFIER)) {
                            parameters.add(tokens.get(-1).literal());
                        }
                        else {  //exception handling
                            throw new ParseException("Missing identifier after comma!");
                        }
                    }
                }
                else {
                    if (tokens.match(Token.Type.IDENTIFIER)) {
                        parameters.add(tokens.get(-1).literal());
                    }
                }
                multiple_params = true;
            }
        }
        if (tokens.match("DO")) {
            while (!tokens.match("END") && tokens.has(0)) {
                body.add(parseStmt());
            }
        }
        return new Ast.Stmt.Def(name, parameters, body);
    }

    private Ast.Stmt.If parseIfStmt() throws ParseException {
        var condition = parseExpr();
        var thenBody = new ArrayList<Ast.Stmt>();
        var elseBody = new ArrayList<Ast.Stmt>();
        if (!tokens.match("DO")) {
            throw new ParseException("Syntax error: missing DO");
        }
        while (!tokens.match("END")) {
            if (tokens.match("ELSE")) {
                if (!tokens.peek("END")) {
                    elseBody.add(parseStmt());
                }
            }
            else {
                thenBody.add(parseStmt());
            }
        }
        return new Ast.Stmt.If(condition, thenBody, elseBody);
    }

    private Ast.Stmt.For parseForStmt() throws ParseException {
        if (!tokens.match(Token.Type.IDENTIFIER)) {
            throw new ParseException("No identifier!");
        }
        var name = tokens.get(-1).literal();
        if (!tokens.match("IN")) {
            throw new ParseException("No IN!");
        }
        var expression = parseExpr();
        if (!tokens.match("DO")) {
            throw new ParseException("No DO!");
        }
        var stmt_list = new ArrayList<Ast.Stmt>();
        while (!tokens.match("END")) {
            stmt_list.add(parseStmt());
        }
        return new Ast.Stmt.For(name, expression, stmt_list);
    }

    private Ast.Stmt.Return parseReturnStmt() throws ParseException {
        Ast.Expr value = null;
        if (tokens.match(";")) {
            return new Ast.Stmt.Return(Optional.ofNullable(value));
        }
        value = parseExpr();
        if (tokens.match(";")) {
            return new Ast.Stmt.Return(Optional.ofNullable(value));
        }
        throw new ParseException("Syntax error: missing semicolon!");
    }

    private Ast.Stmt parseExpressionOrAssignmentStmt() throws ParseException {
        var expression = parseExpr();
        Ast.Expr value = null;
        boolean assignment = false;
        if (tokens.match(";")) {
            return new Ast.Stmt.Expression(expression);
        }
        else if (tokens.match("=")) {
            value = parseExpr();
            assignment = true;
        }
        if (tokens.match(";") && assignment) {
            return new Ast.Stmt.Assignment(expression, value);
        }
        throw new ParseException("Incorrect Syntax!");
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
        var comp_expr = parseComparisonExpr();
        while (tokens.match("AND") || tokens.match("OR")) {
            var operator = tokens.get(-1).literal();
            var right = parseComparisonExpr();
            comp_expr = new Ast.Expr.Binary(operator, comp_expr, right);
        }
        return comp_expr;
    }

    private Ast.Expr parseComparisonExpr() throws ParseException {
        var add_expr = parseAdditiveExpr();
        while (tokens.match("<") || tokens.match("<=") | tokens.match(">")
                | tokens.match(">=") | tokens.match("==") | tokens.match("!=")) {
            var operator = tokens.get(-1).literal();
            var right = parseAdditiveExpr();
            add_expr = new Ast.Expr.Binary(operator, add_expr, right);
        }
        return add_expr;
    }

    private Ast.Expr parseAdditiveExpr() throws ParseException {
        var mul_expr = parseMultiplicativeExpr();
        while (tokens.match("+") || tokens.match("-")) {
            var operator = tokens.get(-1).literal();
            var right = parseMultiplicativeExpr();
            mul_expr = new Ast.Expr.Binary(operator, mul_expr, right);
        }
        return mul_expr;
    }

    private Ast.Expr parseMultiplicativeExpr() throws ParseException {
        var second_expr = parseSecondaryExpr();
        while (tokens.match("*") || tokens.match("/")) {
            var operator = tokens.get(-1).literal();
            var right = parseSecondaryExpr();
            second_expr = new Ast.Expr.Binary(operator, second_expr, right);
        }
        return second_expr;
    }

    private Ast.Expr parseSecondaryExpr() throws ParseException {
        var primary_expr = parsePrimaryExpr();
        while (tokens.match(".") ) {
            if (tokens.match(Token.Type.IDENTIFIER)) {
                var identifier = tokens.get(-1).literal();
                var list_of_params = new ArrayList<Ast.Expr>();
                if (tokens.match("(")) {
                    while (!tokens.match(")")) {
                        if (tokens.peek(")")) {
                            list_of_params.add(parseExpr());
                        }
                        if (tokens.peek(",")) {
                            list_of_params.add(parseExpr());
                        }
                    }
                    //create list of expr, keep calling group and add to list until )
                    //outside of this loop return new .method and pass in List
                }
                else {
                    return new Ast.Expr.Property(primary_expr, identifier);
                }
                primary_expr = new Ast.Expr.Method(primary_expr, identifier, list_of_params);
            }
            else {
                throw new ParseException("No identifier following period!");
            }
        }
        return primary_expr;
    }

    private Ast.Expr parsePrimaryExpr() throws ParseException {
        try {
            //literal
            if (tokens.peek("NIL") || tokens.peek("TRUE") || tokens.peek("FALSE")
                    || tokens.peek(Token.Type.INTEGER) || tokens.peek(Token.Type.DECIMAL)
                    || tokens.peek(Token.Type.CHARACTER) || tokens.peek(Token.Type.STRING)) {
                return parseLiteralExpr();
            }
            //group
            else if (tokens.peek("(")) {
                return parseGroupExpr();
            }
            //object
            else if (tokens.match("OBJECT")) {
                return parseObjectExpr();
            }
            //var_or_fun
            else if (tokens.match(Token.Type.IDENTIFIER)) {
                return parseVariableOrFunctionExpr();
            }
        }
        catch (Exception exception ){
            throw new ParseException("Invalid primary expression!");
        }
        return null;
    }

    private Ast.Expr.Literal parseLiteralExpr() throws ParseException {
        var ret_obj = tokens.get(0).literal();
        var ret_type = tokens.get(0).type();
        tokens.index++;
        System.out.println(ret_obj);
        if (Objects.equals(ret_obj, "NIL")) {
            return new Ast.Expr.Literal(null);
        }
        else if (Objects.equals(ret_obj, "TRUE")) {
            return new Ast.Expr.Literal(true);
        }
        else if (Objects.equals(ret_obj, "FALSE")) {
            return new Ast.Expr.Literal(false);
        }
        else if (ret_type == Token.Type.INTEGER) {
            return new Ast.Expr.Literal(new BigInteger(ret_obj));
        }
        else if (ret_type == Token.Type.DECIMAL) {
            return new Ast.Expr.Literal(new BigDecimal(ret_obj));
        }
        else if (ret_type == Token.Type.CHARACTER) {
            return new Ast.Expr.Literal(ret_obj.charAt(1));
        }
        else if (ret_obj.charAt(0) == '\"' || ret_obj.charAt(0) == '\'') {
            String escape_escaper = ret_obj.substring(1, ret_obj.length() - 1).replace("\\n", "\n");
            return new Ast.Expr.Literal(escape_escaper);
        }
        return new Ast.Expr.Literal(ret_obj);
    }

    private Ast.Expr.Group parseGroupExpr() throws ParseException {
//        checkState(tokens.match("("));  //moves index
//        var ret_store = parseExpr();
//        if (tokens.match("(")) {
//            checkState(tokens.match("("));
//            return new Ast.Expr.Group(ret_store);
//        }
//        throw new ParseException("Missing closing parenthesis!");
        checkState(tokens.match("("));  //moves index
        var ret_store = parseExpr();
        if (tokens.match(")")) {
            return new Ast.Expr.Group(ret_store);
        }
        throw new ParseException("Missing closing parenthesis!");
    }

    private Ast.Expr.ObjectExpr parseObjectExpr() throws ParseException {
        String name = null;
        var fields = new ArrayList<Ast.Stmt.Let>();
        var methods = new ArrayList<Ast.Stmt.Def>();
        if (tokens.match(Token.Type.IDENTIFIER, "DO")) {
            name = tokens.get(-2).literal();
        }
        else if (!tokens.match("DO")) {
            throw new ParseException("Missing DO in statement!");
        }
        while (!tokens.match("END")) {
            if (tokens.match("LET")) {
                fields.add(parseLetStmt());
            }
            else if (tokens.match("DEF")) {
                methods.add(parseDefStmt());
            }
        }
        return new Ast.Expr.ObjectExpr(Optional.ofNullable(name), fields, methods);
    }

    private Ast.Expr parseVariableOrFunctionExpr() throws ParseException {
        var name = tokens.get(-1).literal();
        boolean multiple_expr = false;
        if (!tokens.match("(")) {
            return new Ast.Expr.Variable(name);
        }
        else {
            var arguments = new ArrayList<Ast.Expr>();
            while (!tokens.match(")")) {
                if (!multiple_expr) {
                    arguments.add(parseExpr());
                }
                else {
                    if (!tokens.match(",")) {
                        throw new ParseException("Syntax error: missing comma!");
                    }
                    arguments.add(parseExpr());
                }
                multiple_expr = true;
            }
            return new Ast.Expr.Function(name, arguments);
        }
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
