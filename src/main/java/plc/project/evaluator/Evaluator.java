package plc.project.evaluator;

import plc.project.parser.Ast;

import javax.swing.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;

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
        //TODO: Handle the possibility of RETURN being called outside of a function.
        return value;
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.Let ast) throws EvaluateException {
        throw new UnsupportedOperationException("TODO"); //TODO
        //for example, this returns a runtime value (or calls visit again)
        //it also defines new scope variable.
        //if new scope.... create new scope, set this as new scope and define everything, then switch back
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.Def ast) throws EvaluateException {
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.If ast) throws EvaluateException {
        throw new UnsupportedOperationException("TODO"); //TODO
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
        throw new UnsupportedOperationException("TODO"); //TODO
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
            else if (pLeft.value() instanceof String) {
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
            else if (pLeft.value() instanceof Comparable<?>) {
                if (right instanceof RuntimeValue.Primitive pRight) {
                    if (pRight.value() instanceof Comparable<?>) {
                        if (joinOperation == "less_than") {
                            return new RuntimeValue.Primitive(((BigDecimal) pLeft.value()).add((BigDecimal) pRight.value()));
                        } else if (joinOperation.equals("less_than_eq")) {
                            return new RuntimeValue.Primitive(((BigDecimal) pLeft.value()).subtract((BigDecimal) pRight.value()));
                        } else if (joinOperation.equals("greater_than")) {
                            return new RuntimeValue.Primitive(((BigDecimal) pLeft.value()).multiply((BigDecimal) pRight.value()));
                        } else if (joinOperation.equals("greater_than_eq")) {
                            return new RuntimeValue.Primitive(((BigDecimal) pLeft.value()).divide((BigDecimal) pRight.value()));
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
        try {
            return new RuntimeValue.Primitive(ast.name());
        }
        catch (Exception exception) {
            throw new EvaluateException("name not defined");
        }
        //return scope.get("pi", false).get();
    }

    @Override
    public RuntimeValue visit(Ast.Expr.Property ast) throws EvaluateException {
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    @Override
    public RuntimeValue visit(Ast.Expr.Function ast) throws EvaluateException {
        var list_of_args = new ArrayList<RuntimeValue>();
        if (!ast.name().isEmpty() && ast == RuntimeValue.Function) {
            for (var argument : ast.arguments()) {
                list_of_args.add(visit(argument));
            }
            return new RuntimeValue.Function(ast.name(), list_of_args);
        }
        throw new UnsupportedOperationException("TODO"); //TODO
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
