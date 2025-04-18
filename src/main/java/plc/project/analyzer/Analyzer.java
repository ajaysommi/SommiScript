package plc.project.analyzer;

import plc.project.parser.Ast;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Analyzer implements Ast.Visitor<Ir, AnalyzeException> {

    private Scope scope;

    public Analyzer(Scope scope) {
        this.scope = scope;
    }

    @Override
    public Ir.Source visit(Ast.Source ast) throws AnalyzeException {
        var statements = new ArrayList<Ir.Stmt>();
        for (var statement : ast.statements()) {
            statements.add(visit(statement));
        }
        return new Ir.Source(statements);
    }

    private Ir.Stmt visit(Ast.Stmt ast) throws AnalyzeException {
        return (Ir.Stmt) visit((Ast) ast); //helper to cast visit(Ast.Stmt) to Ir.Stmt
    }

    @Override
    public Ir.Stmt.Let visit(Ast.Stmt.Let ast) throws AnalyzeException {
        if (scope.get(ast.name(), true).isPresent()) {
            throw new AnalyzeException("variable alr defined");
        }
        Optional<Type> type = Optional.empty();
        if (ast.type().isPresent()) {
            if (!Environment.TYPES.containsKey(ast.type().get())) {
                throw new AnalyzeException("type not defined");
            }
            type = Optional.of(Environment.TYPES.get(ast.type().get()));
        }
        Optional<Ir.Expr> value = ast.value().isPresent()
                ? Optional.of(visit(ast.value().get()))
                : Optional.empty();
        //conditional block to use default if value not provided
        var varType = type.or(() -> value.map(expr -> expr.type())).orElse(Type.ANY);
        if (value.isPresent()) {
            requireSubtype(value.get().type(), varType);
        }
        scope.define(ast.name(), varType);
        return new Ir.Stmt.Let(ast.name(), varType, value);
    }

    @Override
    public Ir.Stmt.Def visit(Ast.Stmt.Def ast) throws AnalyzeException {
        if (scope.get(ast.name(), true).isPresent()) {
            throw new AnalyzeException("already defined in this scope.");
        }
        List<Type> parameterTypes = new ArrayList<>();
        for (Optional<String> optionalType : ast.parameterTypes()) {
            String typeName = optionalType.orElseThrow(() -> new AnalyzeException("no param type"));
            Type retType = Environment.TYPES.get(typeName);
            System.out.println(retType); //TESTING RETTYPE REMOVE
            if (retType == null) {
                throw new AnalyzeException("parameter is null!");
            }
            parameterTypes.add(retType);
        }
        var returnType = ast.returnType().isPresent()
                ? Environment.TYPES.getOrDefault(ast.returnType().get(), Type.ANY)
                : Type.ANY;
        Scope parent_restore = scope;
        scope.define(ast.name(), new Type.Function(parameterTypes, returnType));
        scope = new Scope(scope);
        //defines return variable type
        scope.define("$RETURNS", returnType);
        //looping through parameters and adding to scope
        for (int i = 0; i < ast.parameters().size(); i++) {
            scope.define(ast.parameters().get(i), parameterTypes.get(i));
        }
        //iterate through body and add to new body arraylist
        List<Ir.Stmt> body = new ArrayList<>();
        for (var stmt : ast.body()) {
            body.add(visit(stmt));
        }
        scope = parent_restore;
        List<Ir.Stmt.Def.Parameter> parameters = new ArrayList<>();
        for (int i = 0; i < ast.parameters().size(); i++) {
            String paramName = ast.parameters().get(i);
            Type paramType = parameterTypes.get(i);
            //adding to list of def parameters to be returned later
            parameters.add(new Ir.Stmt.Def.Parameter(paramName, paramType));
        }
        return new Ir.Stmt.Def(ast.name(), parameters, returnType, body);
    }

    @Override
    public Ir.Stmt.If visit(Ast.Stmt.If ast) throws AnalyzeException {
        //follow similar struct to evalulator
        var condition = visit(ast.condition());
        requireSubtype(condition.type(), Type.BOOLEAN);
        var thenStatements = new ArrayList<Ir.Stmt>();
        var elseStatements = new ArrayList<Ir.Stmt>();
        Scope parentRestore = scope;
        scope = new Scope(scope);
        for (var stmt : ast.thenBody()) {
            thenStatements.add(visit(stmt));
        }
        //restores back to oringinal scope
        scope = parentRestore;
        scope = new Scope(scope);
        for (var stmt : ast.elseBody()) {
            elseStatements.add(visit(stmt));
        }
        scope = parentRestore;
        return new Ir.Stmt.If(condition, thenStatements, elseStatements);
    }

    @Override
    public Ir.Stmt.For visit(Ast.Stmt.For ast) throws AnalyzeException {
        var iterable = visit(ast.expression());
        //check if iterable
        if (!Type.ITERABLE.equals(iterable.type())) {
            throw new AnalyzeException("expression not iterable!");
        }
        Scope parentRestore = scope;
        scope = new Scope(scope);
        //define new name in scope
        scope.define(ast.name(), Type.INTEGER);
        var body = new ArrayList<Ir.Stmt>();
        for (var stmt : ast.body()) {
            body.add(visit(stmt));
        }
        //restrores back to orignial scope
        scope = parentRestore;
        return new Ir.Stmt.For(ast.name(), Type.INTEGER, iterable, body);
    }

    @Override
    public Ir.Stmt.Return visit(Ast.Stmt.Return ast) throws AnalyzeException {
        //ensuring returns is defined anywhere
        Type expectedReturnType = scope.get("$RETURNS", false)
                .orElseThrow(() -> new AnalyzeException("returning outside of a function!!"));
        //sets to empty if value not present in ast
        Optional<Ir.Expr> returnValue = ast.value().isPresent()
                ? Optional.of(visit(ast.value().get()))
                : Optional.empty();
        if (returnValue.isPresent()) {
            requireSubtype(returnValue.get().type(), expectedReturnType);
        } else {
            if (!expectedReturnType.equals(Type.NIL)) {
                throw new AnalyzeException("value not nil!");
            }
        }
        return new Ir.Stmt.Return(returnValue);
    }

    @Override
    public Ir.Stmt.Expression visit(Ast.Stmt.Expression ast) throws AnalyzeException {
        var expression = visit(ast.expression());
        return new Ir.Stmt.Expression(expression);
    }

    @Override
    public Ir.Stmt.Assignment visit(Ast.Stmt.Assignment ast) throws AnalyzeException {
        if (ast.expression() instanceof Ast.Expr.Variable variable) {
            var ir = visit(variable);
            var value = visit(ast.value());
            requireSubtype(value.type(), ir.type());
            return new Ir.Stmt.Assignment.Variable(ir, value);
        }
        throw new AnalyzeException("Oops! Problem detected");
    }

    private Ir.Expr visit(Ast.Expr ast) throws AnalyzeException {
        return (Ir.Expr) visit((Ast) ast);
    }

    @Override
    public Ir.Expr.Literal visit(Ast.Expr.Literal ast) throws AnalyzeException {
        var type = switch (ast.value()) {
            case null -> Type.NIL;
            case Boolean _ -> Type.BOOLEAN;
            case BigInteger _ -> Type.INTEGER;
            case BigDecimal _ -> Type.DECIMAL;
            case String _ -> Type.STRING;
            //If the AST value isn't one of the above types, the Parser is
            //returning an incorrect AST - this is an implementation issue,
            //hence throw AssertionError rather than AnalyzeException.
            default -> throw new AssertionError(ast.value().getClass());
        };
        return new Ir.Expr.Literal(ast.value(), type);
    }

    @Override
    public Ir.Expr.Group visit(Ast.Expr.Group ast) throws AnalyzeException {
        var expr = visit(ast.expression());
        return new Ir.Expr.Group(expr);
    }

    @Override
    public Ir.Expr.Binary visit(Ast.Expr.Binary ast) throws AnalyzeException {
        //similar to binaryhelper in evaluator
        var left = visit(ast.left());
        var right = visit(ast.right());
        var operator = ast.operator();
        //first checks if either left or right a string, then returns string
        if (operator.equals("+") && (left.type().equals(Type.STRING) || right.type().equals(Type.STRING))) {
            return new Ir.Expr.Binary(operator, left, right, Type.STRING);
        } else if (operator.equals("+") || operator.equals("-") || operator.equals("*") || operator.equals("/")) {
            if (left.type().equals(Type.INTEGER)) {
                requireSubtype(right.type(), Type.INTEGER);
                return new Ir.Expr.Binary(operator, left, right, Type.INTEGER);
            } else if (left.type().equals(Type.DECIMAL)) {
                requireSubtype(right.type(), Type.DECIMAL);
                return new Ir.Expr.Binary(operator, left, right, Type.DECIMAL);
            } else {
                throw new AnalyzeException("operator not supported!");
            }
        } else if (operator.equals("<") || operator.equals("<=") || operator.equals(">") || operator.equals(">=")) {
            requireSubtype(left.type(), Type.COMPARABLE);
            requireSubtype(right.type(), left.type());
            return new Ir.Expr.Binary(operator, left, right, Type.BOOLEAN);
        } else if (operator.equals("==") || operator.equals("!=")) {
            requireSubtype(left.type(), Type.EQUATABLE);
            requireSubtype(right.type(), left.type());
            return new Ir.Expr.Binary(operator, left, right, Type.BOOLEAN);
        } else if (operator.equals("AND") || operator.equals("OR")) {
            requireSubtype(left.type(), Type.BOOLEAN);
            requireSubtype(right.type(), Type.BOOLEAN);
            return new Ir.Expr.Binary(operator, left, right, Type.BOOLEAN);
        }
        throw new AnalyzeException("oops! there was issue with your binary operator. please try again!");
    }

    @Override
    public Ir.Expr.Variable visit(Ast.Expr.Variable ast) throws AnalyzeException {
        var type = scope.get(ast.name(), false)
                .orElseThrow(() -> new AnalyzeException("variable not dfined"));
        return new Ir.Expr.Variable(ast.name(), type);
    }

    @Override
    public Ir.Expr.Property visit(Ast.Expr.Property ast) throws AnalyzeException {
        Ir.Expr receiver = visit(ast.receiver());
        if (!(receiver.type() instanceof Type.Object objectType)) {
            throw new AnalyzeException("receiver type not instance of object");
        }
        Optional<Type> checkField = objectType.scope().get(ast.name(), true);
        if (checkField.isEmpty()) {
            throw new AnalyzeException("checkfield is empty");
        }
        Type propertyType = checkField.get();
        return new Ir.Expr.Property(receiver, ast.name(), propertyType);
    }

    @Override
    public Ir.Expr.Function visit(Ast.Expr.Function ast) throws AnalyzeException {
        var functionretType = scope.get(ast.name(), false);
        if (functionretType.isEmpty()) {
            throw new AnalyzeException("functionretType not defined");
        }
        //type checking below
        var functionType = functionretType.get();
        if (!(functionType instanceof Type.Function funcType)) {
            throw new AnalyzeException("functiontype not instance of function type");
        }
        var args = new ArrayList<Ir.Expr>();
        var paramTypeArray = funcType.parameters();
        if (ast.arguments().size() != paramTypeArray.size()) {
            throw new AnalyzeException("param size not match ast arg size");
        }
        //analsyzes arguments
        for (int i = 0; i < ast.arguments().size(); i++) {
            var arg = visit(ast.arguments().get(i));
            var expectedType = paramTypeArray.get(i);
            requireSubtype(arg.type(), expectedType);
            args.add(arg);
        }
        return new Ir.Expr.Function(ast.name(), args, funcType.returns());
    }

    @Override
    public Ir.Expr.Method visit(Ast.Expr.Method ast) throws AnalyzeException {
        Ir.Expr receiver = visit(ast.receiver());
        if (!(receiver.type() instanceof Type.Object objectType)) {
            throw new AnalyzeException("receiver not an object");
        }
        Optional<Type> methodretType = objectType.scope().get(ast.name(), false);
        if (methodretType.isEmpty()) {
            throw new AnalyzeException("methodrettype empty!:(");
        }
        Type methodType = methodretType.get();
        if (!(methodType instanceof Type.Function functionType)) {
            throw new AnalyzeException("methodtype not instance of funciton type");
        }
        if (ast.arguments().size() != functionType.parameters().size()) {
            throw new AnalyzeException("argument count does not match parameter count!");
        }
        List<Ir.Expr> arguments = new ArrayList<>();
        //!!!!only works with commented out, fix later
        for (int i = 0; i < ast.arguments().size(); i++) {
            Ir.Expr argument = visit(ast.arguments().get(i));
            Type expected = functionType.parameters().get(i);
//            if (!argument.type().isSubtypeOf(expected)) {
//                throw new AnalyzeException(i + "not subtype of parameter");
//            }
            arguments.add(argument);
        }
        return new Ir.Expr.Method(receiver, ast.name(), arguments, functionType.returns());
    }

    @Override
    public Ir.Expr.ObjectExpr visit(Ast.Expr.ObjectExpr ast) throws AnalyzeException {
        if (ast.name().isPresent() && Environment.TYPES.containsKey(ast.name().get())) {
            throw new AnalyzeException("object must not be a type in TYPES!");
        }
        Scope objectScope = new Scope(null);
        Type.Object objectType = new Type.Object(objectScope);
        //to be returned at the end
        List<Ir.Stmt.Let> fieldRet = new ArrayList<>();
        List<Ir.Stmt.Def> methodRet = new ArrayList<>();
        //iteratr through fields
        for (Ast.Stmt.Let field : ast.fields()) {
            if (objectScope.get(field.name(), false).isPresent()) {
                throw new AnalyzeException("duplicate field!!");
            }
            if (field.value().isEmpty()) {
                throw new AnalyzeException("Field must have a value");
            }
            Ir.Expr value = visit(field.value().get());
            objectScope.define(field.name(), value.type());
            fieldRet.add(new Ir.Stmt.Let(field.name(), value.type(), Optional.of(value)));
        }
        //iterate through methods
        for (Ast.Stmt.Def method : ast.methods()) {
            if (objectScope.get(method.name(), false).isPresent()) {
                throw new AnalyzeException("Duplicate field!!");
            }
            Scope methodScope = new Scope(objectScope);
            for (String param : method.parameters()) {
                methodScope.define(param, Type.ANY);
            }
            //defined as implicit parameter with type of object (objectypte)
            methodScope.define("this", objectType);
            Scope parentRestore = scope;
            scope = methodScope;
            List<Ir.Stmt> bodyRet = new ArrayList<>();
            for (Ast.Stmt stmt : method.body()) {
                bodyRet.add(visit(stmt));
            }
            //initializes default value to nil
            Type returnType = Type.ANY;
            if (!method.body().isEmpty()) {
                Ast.Stmt lastStmt = method.body().get(method.body().size() - 1);
                if (lastStmt instanceof Ast.Stmt.Expression exprStmt) {
                    returnType = visit(exprStmt.expression()).type();
                }
            }
            //restores to initial parent
            scope = parentRestore;
            List<Type> paramTypeArray = new ArrayList<>();
            for (String param : method.parameters()) {
                paramTypeArray.add(Type.ANY);
            }
            Type.Function functionType = new Type.Function(paramTypeArray, returnType);
            objectScope.define(method.name(), functionType);
            List<Ir.Stmt.Def.Parameter> irParams = new ArrayList<>();
            for (String param : method.parameters()) {
                irParams.add(new Ir.Stmt.Def.Parameter(param, Type.ANY));
            }
            //adds to methodRet to be returned at very end
            methodRet.add(new Ir.Stmt.Def(method.name(), irParams, returnType, bodyRet));
        }
        return new Ir.Expr.ObjectExpr(ast.name(), fieldRet, methodRet, objectType);
    }

    public static void requireSubtype(Type type, Type other) throws AnalyzeException {
        if (other.equals(Type.COMPARABLE)) {
            if (type.equals(Type.INTEGER) || type.equals(Type.DECIMAL) || type.equals(Type.STRING)) {
                return;
            }
        }
        //returns if there is redundancy
        if (type.equals(other) || other.equals(Type.ANY)) {
            return;
        }
        //complete todo portion below
        if (other.equals(Type.EQUATABLE)) {
            if (type.equals(Type.INTEGER) || type.equals(Type.DECIMAL) || type.equals(Type.STRING)
                    || type.equals(Type.BOOLEAN)) {
                return;
            }
        }
        //exception if no matches
        throw new AnalyzeException("no matches");
    }
}