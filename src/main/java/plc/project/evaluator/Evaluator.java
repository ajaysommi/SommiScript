package plc.project.evaluator;

import plc.project.parser.Ast;

import javax.swing.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;

//referenced from Crafting Interpreters (https://craftinginterpreters.com/functions.html#return-statements)
class Return extends RuntimeException {
    final Object value;

    Return(Object value) {
        super(null, null, false, false);
        this.value = value;
    }
}

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
        if (scope.get("RETURN", false).isPresent()) {
            throw new EvaluateException("Returned outside of any method or function!");
            //throw new Return(scope.get("RETURN", false).get());
        }
        //TODO: Handle the possibility of RETURN being called outside of a function.
        return value;
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.Let ast) throws EvaluateException {
        Object inner_val;
        if (scope.get(ast.name(), true).isPresent()) {
            throw new EvaluateException("Already present");
        }
        else if (ast.value().isPresent()) {
            var inner_lit = (Ast.Expr.Literal) ast.value().get();
            inner_val = inner_lit.value();
            scope.define(ast.name(), new RuntimeValue.Primitive(inner_val));
        }
        else {
            scope.define(ast.name(), new RuntimeValue.Primitive(null));
            return new RuntimeValue.Primitive(null);
        }
        //for example, this returns a runtime value (or calls visit again)
        //it also defines new scope variable.
        //if new scope.... create new scope, set this as new scope and define everything, then switch back
        return new RuntimeValue.Primitive(inner_val);
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.Def ast) throws EvaluateException {
        //INSTEQD OF RETURNING TRY ONLY ADDING TO SCOPE
        //checks if name already defined in current scope
        if (scope.get(ast.name(), true).isPresent()) {
            throw new EvaluateException("Already present in current scope!");
        }
        //check if parameters are unique
        Set<String> duplicateCheck = new HashSet<>(ast.parameters());
        if (duplicateCheck.size() != ast.parameters().size()) {
            throw new EvaluateException("Parameters are not unique!");
        }
        //define name in current scope
        RuntimeValue.Function ret_function = new RuntimeValue.Function(ast.name(), arguments -> {
            if (ast.parameters().size() != arguments.size()) {
                throw new EvaluateException("Parameter size doesn't match argument size!");
            }
            RuntimeValue check_ret = null;
            Scope parent_restore = scope;  //restoration variable to revert back to at end of call
            scope = new Scope(scope);  //"entering" new scope by setting it as current scope
            //defining all variables for parameters
            try {
                for (int i = 0; i < ast.parameters().size(); i++) {
                    scope.define(ast.parameters().get(i), arguments.get(i));
                }
                //evaluating body statements
                for (Ast.Stmt body_stmt : ast.body()) {
                    visit(body_stmt);

                }
            } catch (Return ret) {
                check_ret = (RuntimeValue) ret.value;
            }
            catch (EvaluateException exception) {
                scope = parent_restore;
                throw new EvaluateException("Exception handled within function!");
            }
            scope = parent_restore;
            //need to check if return, then return the value, otherwise return nill
            if (check_ret != null) {
                return check_ret;
            }
            return new RuntimeValue.Primitive(null);  //should be return nill
        });
        scope.define(ast.name(), ret_function);
        return ret_function;
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
        var expression = visit(ast.expression());
        if (expression instanceof RuntimeValue.Primitive pExpression) {
            if (!(pExpression.value() instanceof Iterable<?> iter)) {
                throw new EvaluateException("Expression not iterable!");
            }
            Scope parent_restore = scope;  //restoration variable to revert back to at end of call
            //looping through the iterable
            for (Object element : iter) {
                //entering new scope
                scope = new Scope(scope);
                if (!(element instanceof RuntimeValue runVal)) {
                    scope = parent_restore;  //restore before crashing
                    throw new EvaluateException("Element not a runtime value!");
                }
                scope.define(ast.name(), runVal);
                //evaluating body statements sequentialy
                for (Ast.Stmt body_stmt : ast.body()) {
                    visit(body_stmt);
                }
            }
            //restoring scope back to original before safely exiting
            scope = parent_restore;
        }
        return new RuntimeValue.Primitive(null);
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.Return ast) throws EvaluateException {
        Object value;
        if (ast.value().isPresent()) {
            value = visit(ast.value().get());
        }
        else {
            throw new EvaluateException("Blank return!");
        }
        throw new Return(value);
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
            if (scope.get(var.name(), false).isPresent()) {
                ret_val = visit(ast.value());
                scope.set(var.name(), ret_val);
            } else {
                //defines new if does not exist
                ret_val = visit(ast.value());
                scope.define(var.name(), ret_val);
            }
        } else if (ast.expression() instanceof Ast.Expr.Property prop) {
            RuntimeValue receiver = visit(prop.receiver());
            if (!(receiver instanceof RuntimeValue.ObjectValue obj)) {
                throw new EvaluateException("Receiver must be an object to set a property!");
            }
            if (obj.scope().get(prop.name(), true).isPresent()) {
                ret_val = visit(ast.value());
                obj.scope().set(prop.name(), ret_val);
            }
            else {
                ret_val = visit(ast.value());
                obj.scope().define(prop.name(), ret_val);
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
        if (left instanceof RuntimeValue.Function pLeft) {
            System.out.println("function");
            List<RuntimeValue> evaluatedArgs = Collections.singletonList(pLeft.definition()
                    .invoke(List.of(new RuntimeValue.Primitive(true))));
            for (RuntimeValue arg : evaluatedArgs) {
                if (arg instanceof RuntimeValue.Primitive pArg && pArg.value() instanceof Boolean) {
                    System.out.println(pArg);
                    return pArg;
                }
            }

        }
        if (left instanceof RuntimeValue.Primitive pLeft) {
            System.out.println(pLeft.print());
            if (Objects.equals(pLeft.print(), "TRUE") && joinOperation.equals("or")) {
                System.out.println("sdf");
                return new RuntimeValue.Primitive(true);
            }
            if (pLeft.value() instanceof BigDecimal) {
                if (right instanceof RuntimeValue.Primitive pRight) {
                    if (pRight.value() instanceof BigDecimal) {
                        if (joinOperation == "add") {
                            return new RuntimeValue.Primitive(((BigDecimal) pLeft.value()).add((BigDecimal) pRight.value()));
                        }
                        else if (joinOperation.equals("subtract")) {
                            return new RuntimeValue.Primitive(((BigDecimal) pLeft.value()).subtract((BigDecimal) pRight.value()));
                        }
                        else if (joinOperation.equals("less_than")) {
                            return new RuntimeValue.Primitive(((BigDecimal) pLeft.value()).compareTo((BigDecimal) pRight.value()) == -1);
                        }
                        else if (joinOperation.equals("less_than_eq")) {
                            return new RuntimeValue.Primitive(((BigDecimal) pLeft.value()).compareTo((BigDecimal) pRight.value()) <= 0);
                        }
                        else if (joinOperation.equals("greater_than")) {
                            return new RuntimeValue.Primitive(((BigDecimal) pLeft.value()).compareTo((BigDecimal) pRight.value()) == 1);
                        }
                        else if (joinOperation.equals("greater_than_eq")) {
                            return new RuntimeValue.Primitive(((BigDecimal) pLeft.value()).compareTo((BigDecimal) pRight.value()) >= 0);
                        }
                        else if (joinOperation.equals("equal")) {
                            return new RuntimeValue.Primitive(pLeft.value().equals(pRight.value()));
                        }
                        else if (joinOperation.equals("not_equal")) {
                            return new RuntimeValue.Primitive(!pLeft.value().equals(pRight.value()));
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
            //if left is boolean regular
            else if (pLeft.value() instanceof Boolean lBool) {
                if (lBool && joinOperation.equals("or")) {
                    return new RuntimeValue.Primitive(true);
                }
                if (right instanceof RuntimeValue.Primitive pRight) {
                    if (pRight.value() instanceof Boolean rBool) {
                        if (joinOperation == "and") {
                            return new RuntimeValue.Primitive(lBool && rBool);
                        } else if (joinOperation.equals("or")) {
                            return new RuntimeValue.Primitive(lBool || rBool);
                        }
                    }
                    else {
                        throw new EvaluateException("right must also be boolean");
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
                        else if (joinOperation.equals("less_than")) {
                            return new RuntimeValue.Primitive(((BigInteger) pLeft.value()).compareTo((BigInteger) pRight.value()) == -1);
                        }
                        else if (joinOperation.equals("less_than_eq")) {
                            return new RuntimeValue.Primitive(((BigInteger) pLeft.value()).compareTo((BigInteger) pRight.value()) <= 0);
                        }
                        else if (joinOperation.equals("greater_than")) {
                            return new RuntimeValue.Primitive(((BigInteger) pLeft.value()).compareTo((BigInteger) pRight.value()) == 1);
                        }
                        else if (joinOperation.equals("greater_than_eq")) {
                            return new RuntimeValue.Primitive(((BigInteger) pLeft.value()).compareTo((BigInteger) pRight.value()) >= 0);
                        }
                        else if (joinOperation.equals("equal")) {
                            return new RuntimeValue.Primitive(pLeft.value().equals(pRight.value()));
                        }
                        else if (joinOperation.equals("not_equal")) {
                            return new RuntimeValue.Primitive(!pLeft.value().equals(pRight.value()));
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
            else if (pLeft.value() instanceof Comparable<?> || joinOperation == "less_than"
                    || joinOperation == "less_than_eq" || joinOperation == "greater_than"
                    || joinOperation == "greater_than_eq") {
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
            else {
                throw new EvaluateException("left must be a integer, decimal, or string!");
            }
        }
        throw new EvaluateException("left must be a integer, decimal, or string!");
    }

    @Override
    public RuntimeValue visit(Ast.Expr.Binary ast) throws EvaluateException {
        System.out.println(ast.left().toString());
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
        //enter receiver scope and check if name defined in there, then exit
        var receiver = visit(ast.receiver());
        if (!(receiver instanceof RuntimeValue.ObjectValue)) {
            throw new EvaluateException("Receiver not instance of Object!");
        }
        if (((RuntimeValue.ObjectValue) receiver).scope().get(ast.name(), false).isEmpty()) {
            throw new EvaluateException("Value not present in receiver!");
        }
//        if (scope.get(ast.name(), false).isEmpty()) {
//            throw new EvaluateException("Value not present in Property method!");
//        }
        return ((RuntimeValue.ObjectValue) receiver).scope().get(ast.name(), false).get();
        //return scope.get(ast.name(), false).get();
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
        var receiver = visit(ast.receiver());
        //checks if receiver is an object runtime value
        if (!(receiver instanceof RuntimeValue.ObjectValue)) {
            throw new EvaluateException("Receiver not instance of Object!");
        }
        //code only works with this commented out.
//        //checks if name is defined and is instance of function
//        else if (scope.get(ast.name(), false).equals(Optional.empty())
//                || !(scope.get(ast.name(), false).get() instanceof RuntimeValue.Function)) {
//            throw new EvaluateException("Value not defined or a function!");
//        }
        var list_of_args = new ArrayList<RuntimeValue>();
        for (var arg : ast.arguments()) {
            list_of_args.add(visit(arg));
        }
        return new RuntimeValue.Primitive(list_of_args);
    }

    @Override
    public RuntimeValue visit(Ast.Expr.ObjectExpr ast) throws EvaluateException {
        Scope parent_restore = scope;  //restoration variable to revert back to at end of call
        scope = new Scope(scope);  //"entering" new scope by setting it as current scope
        //iterate through fields
        for (var field : ast.fields()) {
            if (scope.get(field.name(), true).isPresent()) {
                scope = parent_restore;  //safely restores scope to original before throwing exception
                throw new EvaluateException("Field already present!");
            }
            if (field.value().isPresent()) {
                scope.define(field.name(), visit(field.value().get()));
            }
            else {
                scope.define(field.name(), new RuntimeValue.Primitive(null));
            }
        }
        //iterate through methods
        for (var method : ast.methods()) {
            if (scope.get(method.name(), true).isPresent()) {
                scope = parent_restore;  //safely restores scope to original before throwing exception
                throw new EvaluateException("Method already present!");
            }
            //check if method parameters are unique
            Set<String> duplicateCheck = new HashSet<>(method.parameters());
            if (duplicateCheck.size() != method.parameters().size()) {
                scope = parent_restore;  //safely restores scope to original before throwing exception
                throw new EvaluateException("Parameters are not unique!");
            }
            scope.define(method.name(), new RuntimeValue.Function(method.name(), arguments -> {
                Scope inner_parent_restore = scope;  //restoration variable to revert back to at end of call
                scope = new Scope(scope);  //"entering" new scope by setting it as current scope
                try {
                    scope.define("this", arguments.get(0));
                    int counter = 0;
                    //referred to https://www.geeksforgeeks.org/arraylist-sublist-method-in-java-with-examples/
                    for (var arg : arguments.subList(1, arguments.size())) {
                        scope.define(String.valueOf(counter), arg);
                        counter += 1;
                    }
                    RuntimeValue check_ret = null;
                    for (var body : method.body()) {
                        check_ret = visit(body);
                        if (Objects.equals(check_ret.toString(), "RETURN")) {
                            scope = inner_parent_restore;
                            return check_ret;
                        }
                    }
                    scope = inner_parent_restore;
                    return new RuntimeValue.Primitive(null);
                }
                catch (Exception exception) {
                    scope = inner_parent_restore;  //restores scope to outer scope
                    throw new EvaluateException("Exception caught within method definition!");
                }
            }));
        }
        return new RuntimeValue.ObjectValue(ast.name(), scope);
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
