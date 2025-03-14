package plc.project.evaluator;

import plc.project.parser.Ast;

import javax.swing.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class Evaluator implements Ast.Visitor<RuntimeValue, EvaluateException> {

    private Scope scope;

    public Evaluator(Scope scope) {
        this.scope = scope;
    }

    @Override
    public RuntimeValue visit(Ast.Source ast) throws EvaluateException {
        RuntimeValue value = new RuntimeValue.Primitive(null);
        for (var stmt : ast.statements()) {
            value = visit(stmt);
        }
        scope.get("RETURN", false);
        //TODO: Handle the possibility of RETURN being called outside of a function.
        return value;
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.Let ast) throws EvaluateException {
        if (scope.get(ast.name(), true).isPresent()) {
            throw new EvaluateException("Already present");
        }
        else if (ast.value().isPresent()) {
            scope.define(ast.name(), new RuntimeValue.Primitive(ast.value()));
        }
        else {
            scope.define(ast.name(), new RuntimeValue.Primitive(null));
            return new RuntimeValue.Primitive(null);
        }
        //for example, this returns a runtime value (or calls visit again)
        //it also defines new scope variable.
        //if new scope.... create new scope, set this as new scope and define everything, then switch back
        return new RuntimeValue.Primitive(ast.value());
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.Def ast) throws EvaluateException {
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.If ast) throws EvaluateException {
        var condition = visit(ast.condition());
        RuntimeValue ret_val = null;
        Scope parent_restore = scope;  //restoration variable to revert back to at end of call
        Scope new_scope = new Scope(scope);  //new child scope to be used
        if (condition instanceof RuntimeValue.Primitive cond) {
            //checks if condition is a boolean
            if (!(cond.value() instanceof Boolean)) {
                throw new EvaluateException("Condition not boolean!");
            }
            scope = new_scope;  //"entering" new scope by setting it as current scope
            if (Objects.equals(ast.condition(), new Ast.Expr.Literal(true))) {
                System.out.println("true");
                for (var each_stmt : ast.thenBody()) {
                    ret_val = visit(each_stmt);
                }
            }
            else if (Objects.equals(ast.condition(), new Ast.Expr.Literal(false))) {
                System.out.println("false");
                for (var each_stmt : ast.elseBody()) {
                    ret_val = visit(each_stmt);
                }
            }
            scope = parent_restore;
        }
        else {
            throw new EvaluateException("Ast condition not of primitive type!");
        }
        System.out.println(ret_val);
        return ret_val;
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.For ast) throws EvaluateException {
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.Return ast) throws EvaluateException {
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.Expression ast) throws EvaluateException {
        return visit(ast.expression());
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.Assignment ast) throws EvaluateException {
        RuntimeValue ret_val = null;
        if (!(ast.expression() instanceof Ast.Expr.Variable || ast.expression() instanceof Ast.Expr.Property)) {
            throw new EvaluateException("Expression not variable or property!");
        } else if (ast.expression() instanceof Ast.Expr.Variable var) {
            if (scope.get(ast.expression().toString(), false).isPresent()) {
                ret_val = visit(ast.value());
                scope.set(ast.expression().toString(), ret_val);
            }
        } else if (ast.expression() instanceof Ast.Expr.Property prop) {  //if expression is Property
            if (scope.get(ast.expression().toString(), false).isPresent()) {
                ret_val = visit(ast.value());
                scope.set(ast.expression().toString(), ret_val);
            }
        }
        return ret_val;
    }

    @Override
    public RuntimeValue visit(Ast.Expr.Literal ast) throws EvaluateException {
        return new RuntimeValue.Primitive(ast.value());
    }

    @Override
    public RuntimeValue visit(Ast.Expr.Group ast) throws EvaluateException {
        return visit(ast.expression());
    }

    //helper function to reduce boilerplate code in binary
    public RuntimeValue binaryHelper(String operation, Ast.Expr.Binary ast) throws EvaluateException {
        String joinOperation = "";
        switch (operation) {
            case "-": {
                joinOperation = "subtract";
                break;
            }
            case "+": {
                joinOperation = "add";
                break;
            }
            case "*": {
                joinOperation = "multiply";
                break;
            }
            case "/": {
                joinOperation = "divide";
                break;
            }
            case "<": {
                joinOperation = "less_than";
                break;
            }
            case "<=": {
                joinOperation = "less_than_eq";
                break;
            }
            case ">": {
                joinOperation = "greater_than";
                break;
            }
            case ">=": {
                joinOperation = "greater_than_eq";
                break;
            }
            case "==": {
                joinOperation = "equal";
                break;
            }
            case "!=": {
                joinOperation = "not_equal";
                break;
            }
            case "AND": {
                joinOperation = "and";
                break;
            }
            case "OR": {
                joinOperation = "or";
                break;
            }
        }
        var left = visit(ast.left());
        var right = visit(ast.right());
        if (left instanceof RuntimeValue.Primitive pLeft) {
            if (pLeft.value() instanceof BigDecimal) {
                if (right instanceof RuntimeValue.Primitive pRight) {
                    if (pRight.value() instanceof BigDecimal) {
                        if (joinOperation == "add") {
                            return new RuntimeValue.Primitive(((BigDecimal) pLeft.value()).add((BigDecimal) pRight.value()));
                        }
                        else if (joinOperation.equals("subtract")) {
                            return new RuntimeValue.Primitive(((BigDecimal) pLeft.value()).subtract((BigDecimal) pRight.value()));
                        }
                        else if (joinOperation.equals("multiply")) {
                            return new RuntimeValue.Primitive(((BigDecimal) pLeft.value()).multiply((BigDecimal) pRight.value()));
                        }
                        else if (joinOperation.equals("divide")) {
                            return new RuntimeValue.Primitive(((BigDecimal) pLeft.value()).divide((BigDecimal) pRight.value(),
                                    RoundingMode.HALF_EVEN)
                            );
                        }
                    }
                    else if (pRight.value() instanceof String) {
                        return new RuntimeValue.Primitive(pLeft.value().toString() + pRight.value().toString());
                    }
                    else {
                        throw new EvaluateException("right must be a decimal");
                    }
                }
            }
            //if left is string
            else if (pLeft.value() instanceof String && joinOperation == "add") {
                if (right instanceof RuntimeValue.Primitive pRight) {
                    var retVal = (pLeft.value().toString() + pRight.value().toString());
                    return new RuntimeValue.Primitive(retVal);
                }
            }
            //if left is integer
            else if (pLeft.value() instanceof BigInteger) {
                if (right instanceof RuntimeValue.Primitive pRight) {
                    if (pRight.value() instanceof BigInteger) {
                        if (joinOperation == "add") {
                            return new RuntimeValue.Primitive(((BigInteger) pLeft.value()).add((BigInteger) pRight.value()));
                        }
                        else if (joinOperation.equals("subtract")) {
                            return new RuntimeValue.Primitive(((BigInteger) pLeft.value()).subtract((BigInteger) pRight.value()));
                        }
                        else if (joinOperation.equals("multiply")) {
                            return new RuntimeValue.Primitive(((BigInteger) pLeft.value()).multiply((BigInteger) pRight.value()));
                        }
                        else if (joinOperation.equals("divide")) {
                            return new RuntimeValue.Primitive(((BigInteger) pLeft.value()).divide((BigInteger) pRight.value()));
                        }
                    }
                    else if (pRight.value() instanceof String) {
                        return new RuntimeValue.Primitive(pLeft.value().toString() + pRight.value().toString());
                    }
                    else {
                        throw new EvaluateException("right must be a integer");
                    }
                }
            }
            //if left is comparable (greater than, less than, etc.)
            else if (pLeft.value() instanceof Comparable<?> || joinOperation == "less_than") {
                if (right instanceof RuntimeValue.Primitive pRight) {
                    int comp_val = 0;
                    if (pRight.value() instanceof Comparable<?>) {
                        if (joinOperation == "less_than") {
                            comp_val = (((Comparable<Object>) pLeft.value()).compareTo((Object)pRight.value()));
                            if (comp_val < 0) {
                                return new RuntimeValue.Primitive(true);
                            } else if (comp_val > 0) {
                                return new RuntimeValue.Primitive(false);
                            }
                        } else if (joinOperation.equals("less_than_eq")) {
                            comp_val = (((Comparable<Object>) pLeft.value()).compareTo((Object)pRight.value()));
                            if (comp_val <= 0) {
                                return new RuntimeValue.Primitive(true);
                            } else {
                                return new RuntimeValue.Primitive(false);
                            }
                        } else if (joinOperation.equals("greater_than")) {
                            comp_val = (((Comparable<Object>) pLeft.value()).compareTo((Object)pRight.value()));
                            if (comp_val > 0) {
                                return new RuntimeValue.Primitive(true);
                            } else if (comp_val < 0) {
                                return new RuntimeValue.Primitive(false);
                            }
                        } else if (joinOperation.equals("greater_than_eq")) {
                            comp_val = (((Comparable<Object>) pLeft.value()).compareTo((Object)pRight.value()));
                            if (comp_val >= 0) {
                                return new RuntimeValue.Primitive(true);
                            } else {
                                return new RuntimeValue.Primitive(false);
                            }
                        }
                    } else if (pRight.value() instanceof String) {
                        return new RuntimeValue.Primitive(pLeft.value().toString() + pRight.value().toString());
                    } else {
                        throw new EvaluateException("right must also be comparable");
                    }
                }
            }
            //if left is boolean
            else if (pLeft.value() instanceof Boolean) {
                if (right instanceof RuntimeValue.Primitive pRight) {
                    if (pRight.value() instanceof Boolean) {
                        if (joinOperation == "and") {
                            return new RuntimeValue.Primitive(((BigDecimal) pLeft.value()).add((BigDecimal) pRight.value()));
                        } else if (joinOperation.equals("or")) {
                            return new RuntimeValue.Primitive(((BigDecimal) pLeft.value()).subtract((BigDecimal) pRight.value()));
                        }
                    }
                    else {
                        throw new EvaluateException("right must also be boolean");
                    }
                }
            }
            else {
                throw new EvaluateException("left must be a integer, decimal, or string!");
            }
        }
        throw new EvaluateException("left must be a integer, decimal, or string!");
    }

    @Override
    public RuntimeValue visit(Ast.Expr.Binary ast) throws EvaluateException {
        return binaryHelper(ast.operator(), ast);
    }

    @Override
    public RuntimeValue visit(Ast.Expr.Variable ast) throws EvaluateException {
        if (scope.get(ast.name(), false).equals(Optional.empty())) {
            throw new EvaluateException("Value not present!");
        }
        return scope.get(ast.name(), false).get();
    }

    @Override
    public RuntimeValue visit(Ast.Expr.Property ast) throws EvaluateException {
        var receiver = visit(ast.receiver());
        if (!(receiver instanceof RuntimeValue.ObjectValue)) {
            throw new EvaluateException("Receiver not instance of Object!");
        }
        if (scope.get(ast.name(), false).equals(Optional.empty())) {
            throw new EvaluateException("Value not present!");
        }
        return scope.get(ast.name(), false).get();
    }

    @Override
    public RuntimeValue visit(Ast.Expr.Function ast) throws EvaluateException {
        if (scope.get(ast.name(), false).isEmpty()
                || !(scope.get(ast.name(), false).get() instanceof RuntimeValue.Function)) {
            throw new EvaluateException("Nothing defined or not instance of function!");
        }
        var list_of_args = new ArrayList<RuntimeValue>();
        for (var argument : ast.arguments()) {
            list_of_args.add(visit(argument));  //iterate through arguments and add to list_of_args array
        }
        //gets value of scope (Optional<RunTimeValue>), then casts as a function to get definition
        //within definition can use the invoke function.
        return ((RuntimeValue.Function)(scope.get(ast.name(),
                false)).get()).definition().invoke(list_of_args);
    }

    @Override
    public RuntimeValue visit(Ast.Expr.Method ast) throws EvaluateException {
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    @Override
    public RuntimeValue visit(Ast.Expr.ObjectExpr ast) throws EvaluateException {
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    /**
     * Helper function for extracting RuntimeValues of specific types. If the
     * type is subclass of {@link RuntimeValue} the check applies to the value
     * itself, otherwise the value is expected to be a {@link RuntimeValue.Primitive}
     * and the check applies to the primitive value.
     */
    private static <T> T requireType(RuntimeValue value, Class<T> type) throws EvaluateException {
        //To be discussed in lecture 3/5.
        if (RuntimeValue.class.isAssignableFrom(type)) {
            if (!type.isInstance(value)) {
                throw new EvaluateException("Expected value to be of type " + type + ", received " + value.getClass() + ".");
            }
            return (T) value;
        } else {
            var primitive = requireType(value, RuntimeValue.Primitive.class);
            if (!type.isInstance(primitive.value())) {
                var received = primitive.value() != null ? primitive.value().getClass() : null;
                throw new EvaluateException("Expected value to be of type " + type + ", received " + received + ".");
            }
            return (T) primitive.value();
        }
    }

}
