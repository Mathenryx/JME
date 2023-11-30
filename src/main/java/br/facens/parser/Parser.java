package br.facens.parser;

import br.facens.exceptions.SemanticException;
import br.facens.exceptions.SyntaxException;
import br.facens.lexer.Token;
import br.facens.lexer.TokenType;
import br.facens.parser.symbol.ArraySymbol;
import br.facens.parser.symbol.FunctionSymbol;
import br.facens.parser.symbol.Symbol;
import br.facens.parser.symbol.SymbolTable;

import java.util.ArrayList;
import java.util.List;

import static br.facens.lexer.TokenType.COMPARATOR;
import static br.facens.lexer.TokenType.FLOAT;
import static br.facens.lexer.TokenType.IDENTIFIER;
import static br.facens.lexer.TokenType.INTEGER;
import static br.facens.lexer.TokenType.KEYWORD;
import static br.facens.lexer.TokenType.OPERATOR;
import static br.facens.lexer.TokenType.PUNCTUATION;
import static br.facens.lexer.TokenType.STRING;
import static br.facens.lexer.TokenType.TYPE;

public class Parser {

    private final List<Token> tokens;
    private int currentTokenIndex;
    private Token currentToken;
    private final SymbolTable symbolTable;
    private FunctionSymbol functionSymbol;
    private final OperationExecutor operationExecutor;
    private final List<String> printList;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.currentTokenIndex = 0;
        this.currentToken = tokens.get(0);
        this.symbolTable = new SymbolTable();
        this.functionSymbol = null;
        this.operationExecutor = new OperationExecutor();
        this.printList = new ArrayList<>();
    }

    public void parse() {
        symbolTable.pushScope();
        collectFunctionDeclarations();
        program();
    }

    private void program() {
        mainClass();
    }

    // Classe main
    private void mainClass() {
        consume(TokenType.KEYWORD, "class");
        consume(TokenType.KEYWORD, "Main");
        consume(PUNCTUATION, "{");

        declarations();

        mainMethod();

        declarations();

        consume(PUNCTUATION, "}");
    }

    private void mainMethod() {
        consume(TYPE, "void");
        consume(KEYWORD, "main");
        consume(PUNCTUATION, "(");
        consume(PUNCTUATION, ")");

        symbolTable.pushScope();
        block();
        symbolTable.popScope();
    }

    private void declarations() {
        while (!peek(PUNCTUATION, "}") && !peekNext(KEYWORD, "main")) {
            declaration();
        }
    }

    private void declaration() {// TODO: FIX
        String type = currentToken.getValue();
        consume(getTypes());
        if (peek(IDENTIFIER)) {
            if (peekNext(PUNCTUATION, "(")) {
                functionDeclaration(type);
            } else {
                varDeclaration(type);
            }
        } else if (peek(PUNCTUATION, "[")) {
            functionDeclaration(type);
        } else {
            varDeclaration(type);
        }
    }

    private void functionDeclaration(String returnType) {
        if (peek(PUNCTUATION, "[")) {
            consume(PUNCTUATION, "[");
            consume(PUNCTUATION, "]");
        }

        this.functionSymbol = (FunctionSymbol) symbolTable.getFunctionSymbol(currentToken.getValue());
        consume(IDENTIFIER);

        symbolTable.pushScope();
        consume(PUNCTUATION, "(");

        parameters();

        consume(PUNCTUATION, ")");

        block();
        symbolTable.popScope();

        if (!"void".equals(returnType) && !this.functionSymbol.hasReturn()) {
            throw new SemanticException("function must return type: " + returnType);
        }

        this.functionSymbol = null;
    }

    private void parameters() {
        if (peek(TYPE)) {
            parameter();

            while (peek(TokenType.PUNCTUATION, ",")) {
                consume(TokenType.PUNCTUATION, ",");
                parameter();
            }
        }
    }

    private void parameter() {
        String type = currentToken.getValue();
        consume(getTypes());

        if (peek(PUNCTUATION, "[")) {
            consume(TokenType.PUNCTUATION, "[");
            consume(TokenType.PUNCTUATION, "]");
        }

        String id = currentToken.getValue();
        consume(IDENTIFIER);

        Symbol symbol = new Symbol(id, type);
        symbol.setInitialized(true);
        symbolTable.addSymbol(id, symbol);
    }

    private void varDeclaration(String type) {
        if (peek(TokenType.PUNCTUATION, "[")) {
            arrayDeclaration(type);
        } else {
            String id = currentToken.getValue();
            if (symbolTable.hasSymbolCurrentScope(id)) {
                throw new SemanticException("Duplicate Variable: Variable " + id + " has already been declared");
            }
            Symbol symbol = new Symbol(id, type);
            symbolTable.addSymbol(id, symbol);

            consume(IDENTIFIER);
            if (peek(OPERATOR, "=")) {
                assignment(id);
            }
        }
        consume(PUNCTUATION, ";");

    }

    private void arrayDeclaration(String type) {
        consume(PUNCTUATION, "[");
        consume(PUNCTUATION, "]");
        String id = currentToken.getValue();
        consume(IDENTIFIER);

        symbolTable.addSymbol(id, new ArraySymbol(id, type));

        if (peek(OPERATOR, "=")) {
            if (peekNext(KEYWORD, "new")) {
                consume(OPERATOR, "=");
                String arrayType = arrayCreation(id);
                if (!arrayType.equals(type)) {
                    throw new SemanticException("Type error: Cannot assign array of type " + arrayType + " to array of type " + type);
                }
            } else {
                assignmentArray(id);
            }
        }
    }

    private String arrayCreation(String id) {
        consume(KEYWORD, "new");
        String type = currentToken.getValue();
        consume(getTypes());

        operationExecutor.setIsInsideArray(true);
        String arraySizeString = arrayAccess(id);
        operationExecutor.setIsInsideArray(false);
        int arraySize = Integer.parseInt(arraySizeString == null ? "100" : arraySizeString);

        ArraySymbol symbol = (ArraySymbol) symbolTable.getVariableSymbol(id);
        for (int i = 0; i < arraySize; i++) {
            symbol.addValue("0");
        }
        symbol.setInitialized(true);

        return type;
    }

    private String arrayAccess(String id) {
        consume(TokenType.PUNCTUATION, "[");
        String indexType = expression();

        ArraySymbol symbol = (ArraySymbol) symbolTable.getVariableSymbol(id);

        if (!indexType.matches("int")) {
            throw new SemanticException("Type error: Array index must be an integer, but got " + indexType);
        }

        String arrayIndex = operationExecutor.executeInsideArrayOperations();
        int index = Integer.parseInt(arrayIndex == null ? "100" : arrayIndex);
        if (symbol.isInitialized() && index >= symbol.getValues().size()) {
            throw new SemanticException("Array (" + id + ") index out of bounds: " + index);
        }

        consume(TokenType.PUNCTUATION, "]");
        return arrayIndex;
    }

    private void block() {
        consume(TokenType.PUNCTUATION, "{");

        statements();
        consume(TokenType.PUNCTUATION, "}");

    }

    private void statements() {
        while (isStatementStart()) {
            operationExecutor.clear();
            statement();
        }
    }

    private boolean isStatementStart() {
        return peek(KEYWORD, "if") ||
                peek(KEYWORD, "while") ||
                peek(KEYWORD, "for") ||
                peek(KEYWORD, "foreach") ||
                peek(KEYWORD, "print") ||
                peek(KEYWORD, "return") ||
                peek(IDENTIFIER) ||
                peek(OPERATOR, "++") ||
                peek(OPERATOR, "--") ||
                peek(TYPE) ||
                peek(KEYWORD, "break") ||
                peek(PUNCTUATION, "{");
    }

    private void statement() {
        if (peek(TokenType.KEYWORD, "if")) {
            ifStatement();
        } else if (peek(TokenType.KEYWORD, "while")) {
            whileStatement();
        } else if (peek(TokenType.KEYWORD, "for")) {
            forStatement();
        } else if (peek(TokenType.KEYWORD, "foreach")) {
            forEachStatement();
        } else if (peek(TokenType.KEYWORD, "print")) {
            printStatement();
        } else if (peek(TokenType.KEYWORD, "return")) {
            returnStatement();
        } else if (peek(TokenType.IDENTIFIER)) {
            if (peekNext(TokenType.PUNCTUATION, "(")) {
                functionCallStatement();
                consume(TokenType.PUNCTUATION, ";");

            } else if (peekNext(OPERATOR, "++")) {
                incrementStatement();
                consume(PUNCTUATION, ";");

            } else if (peekNext(OPERATOR, "--")) {
                decrementStatement();
                consume(PUNCTUATION, ";");

            } else {
                assignmentStatement();
            }
        } else if (peek(OPERATOR, "++")) {
            incrementStatement();
        } else if (peek(OPERATOR, "--")) {
            decrementStatement();
        } else if (peek(TYPE)) {
            String type = currentToken.getValue();
            consume(getTypes());
            varDeclaration(type);
        } else if (peek(KEYWORD, "break")) {
            consume(KEYWORD, "break");
            consume(PUNCTUATION, ";");

        } else {
            symbolTable.pushScope();
            block();
            symbolTable.popScope();
        }
    }

    private void ifStatement() {
        consume(TokenType.KEYWORD, "if");
        consume(TokenType.PUNCTUATION, "(");
        expression();
        consume(TokenType.PUNCTUATION, ")");
        symbolTable.pushScope();
        block();
        symbolTable.popScope();

        if (peek(TokenType.KEYWORD, "else")) {
            consume(TokenType.KEYWORD, "else");
            if (peek(TokenType.KEYWORD, "if")) {
                ifStatement();
            } else {
                symbolTable.pushScope();
                block();
                symbolTable.popScope();
            }
        }
    }

    private void whileStatement() {
        consume(KEYWORD, "while");
        consume(PUNCTUATION, "(");
        expression();
        consume(PUNCTUATION, ")");
        symbolTable.pushScope();
        block();
        symbolTable.popScope();
    }

    private void forEachStatement() {
        symbolTable.pushScope();
        consume(KEYWORD, "foreach");
        consume(PUNCTUATION, "(");

        String type = currentToken.getValue();
        consume(getTypes());

        String id = currentToken.getValue();
        consume(IDENTIFIER);

        Symbol symbol = new Symbol(id, type);
        symbol.setInitialized(true);
        symbolTable.addSymbol(id, symbol);

        consume(PUNCTUATION, ":");
        consume(IDENTIFIER);
        consume(PUNCTUATION, ")");
        block();
        symbolTable.popScope();
    }

    private void forStatement() {
        symbolTable.pushScope();
        consume(KEYWORD, "for");
        consume(PUNCTUATION, "(");
        forInit();
        consume(PUNCTUATION, ";");
        expression();
        consume(PUNCTUATION, ";");
        expression();
        consume(PUNCTUATION, ")");
        block();
        symbolTable.popScope();
    }

    private void forInit() {
        String type = null;
        Symbol symbol = null;
        String id = null;

        if (peek(TokenType.TYPE, "int")) {
            type = currentToken.getValue();
            consume(TYPE, "int");
            id = currentToken.getValue();
            symbol = new Symbol(id, type);
        } else if (peek(TokenType.TYPE)) {
            throw new SemanticException("Type error: Expected int in for init, but got: " + currentToken.getValue());
        }

        if (type != null) {
            symbolTable.addSymbol(id, symbol);
        }

        consume(TokenType.IDENTIFIER);

        if (peek(OPERATOR, "=")) {
            assignment(id);
        }
    }

    private void printStatement() {
        consume(KEYWORD, "print");
        consume(PUNCTUATION, "(");
        expression();
        String print = operationExecutor.executeOperations();
        printList.add(print);
        consume(PUNCTUATION, ")");
        consume(PUNCTUATION, ";");

    }

    private void returnStatement() {
        consume(KEYWORD, "return");

        if (!peek(PUNCTUATION, ";")) {
            String actualReturnType = expression();

            if (!this.functionSymbol.getType().equals(actualReturnType)) {
                throw new SemanticException("Type error: Incorrect return type. Expected: " + this.functionSymbol.getType() + ", but got: " + actualReturnType);
            }
        }

        consume(PUNCTUATION, ";");

    }

    private String functionCallStatement() {
        String functionName = currentToken.getValue();
        Symbol symbol = symbolTable.getFunctionSymbol(functionName);

        if (symbol == null) {
            throw new SemanticException("Undeclared function: Function " + functionName + " not declared.");
        } else if (!(symbol instanceof FunctionSymbol)) {
            throw new SemanticException("Undeclared function: " + functionName + " is not a function");
        }

        FunctionSymbol functionSymbol = (FunctionSymbol) symbol;
        operationExecutor.setFunctionSymbol(functionSymbol);

        consume(IDENTIFIER);
        consume(PUNCTUATION, "(");

        List<String> argumentTypes = new ArrayList<>();
        if (!peek(PUNCTUATION, ")")) {
            argumentTypes = arguments();
        }

        consume(PUNCTUATION, ")");

        if (argumentTypes.size() != functionSymbol.getParameterTypes().size()) {
            throw new SemanticException("Function call error: Incorrect number of arguments for function " + functionName + ".");
        }

        for (int i = 0; i < argumentTypes.size(); i++) {
            if (!argumentTypes.get(i).equals(functionSymbol.getParameterTypes().get(i))) {
                throw new SemanticException("Type error: Incorrect type of argument for function " + functionName + ".");
            }
        }
        return functionSymbol.getType();
    }

    private List<String> arguments() {
        List<String> argumentTypes = new ArrayList<>();
        argumentTypes.add(expression());

        while (peek(PUNCTUATION, ",")) {
            consume(PUNCTUATION, ",");
            argumentTypes.add(expression());
        }
        return argumentTypes;
    }

    private void incrementStatement() {
        String id;
        if (peek(IDENTIFIER)) {
            id = currentToken.getValue();
            consume(IDENTIFIER);
            consume(OPERATOR, "++");
        } else {
            consume(OPERATOR, "++");
            id = currentToken.getValue();
            consume(IDENTIFIER);
        }

        Symbol symbol = symbolTable.getVariableSymbol(id);
        if (!"int".equals(symbol.getType())) {
            throw new SemanticException("Type error: Cannot increment non-integer variable " + id);
        }
        int newValue = Integer.parseInt(symbol.getValue());
        newValue++;
        symbol.setValue(String.valueOf(newValue));
    }

    private void decrementStatement() {
        String id;
        if (peek(IDENTIFIER)) {
            id = currentToken.getValue();
            consume(IDENTIFIER);
            consume(OPERATOR, "--");
        } else {
            id = currentToken.getValue();
            consume(OPERATOR, "--");
            consume(IDENTIFIER);
        }

        Symbol symbol = symbolTable.getVariableSymbol(id);
        if (!"int".equals(symbol.getType())) {
            throw new SemanticException("Type error: Cannot decrement non-integer variable " + id);
        }
        int newValue = Integer.parseInt(symbol.getValue());
        newValue--;
        symbol.setValue(String.valueOf(newValue));
    }

    private void assignmentStatement() {
        String id = currentToken.getValue();
        consume(IDENTIFIER);

        if (peek(PUNCTUATION, "[")) {
            operationExecutor.setIsInsideArray(true);
            String arrayIndex = arrayAccess(id);
            operationExecutor.setIsInsideArray(false);

            if (!symbolTable.hasSymbol(id)) {
                throw new SemanticException("Unassigned variable: Array " + id + " not initialized ");
            }
            assignmentArrayIndexed(id, arrayIndex);
        } else {
            assignment(id);
        }

        consume(PUNCTUATION, ";");

    }

    private void assignment(String id) {
        Symbol symbol = symbolTable.getVariableSymbol(id);

        if (symbol == null) {
            throw new SemanticException("Undeclared Variable: Variable " + id + " not declared ");
        }

        String leftType = symbol.getType();
        String operator = null;
        if (peek(OPERATOR, "+=") || peek(OPERATOR, "-=")) {
            operator = currentToken.getValue();
            if (!"int".equals(leftType) && !"float".equals(leftType)) {
                throw new SemanticException("Type error: Cannot use += or -= with non-numeric variable " + id);
            }
            consume(OPERATOR);
        } else {
            consume(OPERATOR, "=");
        }

        String rightType = expression();

        if (!leftType.equals(rightType)) {
            throw new SemanticException("Type error: Cannot assign " + rightType + " to " + leftType + " ");
        }

        String value = operationExecutor.executeOperations();
        if ("+=".equals(operator)) {
            operationExecutor.pushOperator(value);
            operationExecutor.pushOperator("+");
            operationExecutor.pushOperator(symbol.getValue());
            value = operationExecutor.executeOperations();
        } else if ("-=".equals(operator)) {
            operationExecutor.pushOperator(value);
            operationExecutor.pushOperator("-");
            operationExecutor.pushOperator(symbol.getValue());
            value = operationExecutor.executeOperations();
        }

        symbol.setValue(value);
        symbol.setInitialized(true);
    }


    private void assignmentArray(String id) {
        ArraySymbol symbol = (ArraySymbol) symbolTable.getVariableSymbol(id);

        if (symbol == null) {
            throw new SemanticException("Undeclared variable: Variable " + id + " not declared ");
        }

        String leftType = symbol.getType();

        consume(OPERATOR, "=");

        String rightType = expression();
        FunctionSymbol rightFunctionSymbol = operationExecutor.getFunctionSymbol();
        ArraySymbol rightArraySymbol = operationExecutor.getArraySymbol();

        if (!leftType.equals(rightType)) {
            throw new SemanticException("Type error: Cannot assign " + rightType + " to " + leftType + " ");
        } else if (rightFunctionSymbol != null && !rightFunctionSymbol.isArray()) {
            throw new SemanticException("Type error: Return of function is not an array");
        } else if (rightFunctionSymbol != null) {
            symbol.addValue(operationExecutor.executeOperations());
        } else if (rightArraySymbol == null) {
            throw new SemanticException("Type error: Cannot assign an variable to an array");
        } else {
            symbol.setValues(rightArraySymbol.getValues());
        }
        operationExecutor.setFunctionSymbol(null);
        operationExecutor.setArraySymbol(null);
        symbol.setInitialized(true);
    }

    private void assignmentArrayIndexed(String id, String index) {
        ArraySymbol symbol = (ArraySymbol) symbolTable.getVariableSymbol(id);

        if (symbol == null) {
            throw new SemanticException("Undeclared variable: Variable " + id + " not declared ");
        }

        String leftType = symbol.getType();

        if (peek(OPERATOR, "+=") || peek(OPERATOR, "-=")) {
            if (!"int".equals(leftType) && !"float".equals(leftType)) {
                throw new SemanticException("Type error: Cannot use " + currentToken.getValue() + " with non-numeric variable " + id);
            }
            consume(OPERATOR);
        } else {
            consume(OPERATOR, "=");
        }

        String rightType = expression();

        if (!leftType.equals(rightType)) {
            throw new SemanticException("Type error: Cannot assign " + rightType + " to " + leftType + " ");
        }

        symbol.setArrayValue(Integer.parseInt(index), operationExecutor.executeOperations());
    }

    private String expression() {
        if (peek(OPERATOR, "++")) {
            incrementStatement();
            return "void";
        } else if (peek(OPERATOR, "--")) {
            decrementStatement();
            return "void";
        } else if (peek(IDENTIFIER)) {
            if (peekNext(OPERATOR, "++")) {
                incrementStatement();
                return "void";
            } else if (peekNext(OPERATOR, "--")) {
                decrementStatement();
                return "void";
            } else {
                return logicalExpression();
            }
        } else {
            return logicalExpression();
        }
    }

    private String logicalExpression() {
        String leftType = equalityExpression();

        while (peek(COMPARATOR, "&&") || peek(COMPARATOR, "||")) {
            String operator = currentToken.getValue();
            operationExecutor.pushOperator(operator);
            consume(COMPARATOR);

            String rightType = equalityExpression();

            if (!leftType.equals(rightType) || !leftType.equals("boolean")) {
                throw new SemanticException("Type error: Cannot perform operation " + operator + " on " + leftType + " and " + rightType);
            }
            leftType = "boolean";
        }
        return leftType;
    }

    private String equalityExpression() {
        String leftType = comparisonExpression();

        while (peek(COMPARATOR, "==") || peek(COMPARATOR, "!=")) {
            String operator = currentToken.getValue();
            operationExecutor.pushOperator(operator);
            consume(COMPARATOR);

            String rightType = comparisonExpression();

            if (!leftType.equals(rightType)) {
                throw new SemanticException("Type error: Cannot perform operation " + operator + " on " + leftType + " and " + rightType);
            }
            leftType = "boolean";
        }
        return leftType;
    }

    private String comparisonExpression() {
        String leftType = termExpression();

        while (peek(COMPARATOR, "<") || peek(COMPARATOR, "<=") || peek(COMPARATOR, ">") || peek(COMPARATOR, ">=")) {
            String comparator = currentToken.getValue();
            operationExecutor.pushOperator(comparator);
            consume(COMPARATOR);

            String rightType = termExpression();

            if (!leftType.equals(rightType) || (!leftType.equals("int") && !leftType.equals("float"))) {
                throw new SemanticException("Type error: Cannot perform operation " + comparator + " on " + leftType + " and " + rightType);
            }
            leftType = "boolean";
        }
        return leftType;
    }

    private String termExpression() {
        String leftType = factorExpression();

        while (peek(OPERATOR, "+") || peek(OPERATOR, "-")) {
            String operator = currentToken.getValue();
            if (operationExecutor.isInsideArray()) {
                operationExecutor.pushInsideArrayOperator(operator);
            } else {
                operationExecutor.pushOperator(operator);
            }
            consume(OPERATOR);

            String rightType = factorExpression();

            if (!leftType.equals(rightType)) {
                if (!(operator.equals("+") && (leftType.equals("string") || rightType.equals("string")))) {
                    throw new SemanticException("Type error: Cannot perform operation " + operator + " on " + leftType + " and " + rightType);
                }
            }

            if (operator.equals("+") && (leftType.equals("string") || rightType.equals("string"))) {
                leftType = "string";
            } else if (operator.equals("+") && leftType.equals("boolean")) {
                throw new SemanticException("Type error: Cannot perform addition on boolean type");
            } else if (operator.equals("-") && leftType.equals("boolean")) {
                throw new SemanticException("Type error: Cannot perform subtraction on boolean type");
            }
        }
        return leftType;
    }

    private String factorExpression() {
        String leftType = unaryExpression();

        while (peek(OPERATOR, "*") || peek(OPERATOR, "/") || peek(OPERATOR, "%")) {
            String operator = currentToken.getValue();
            if (operationExecutor.isInsideArray()) {
                operationExecutor.pushInsideArrayOperator(operator);
            } else {
                operationExecutor.pushOperator(operator);
            }
            consume(OPERATOR);

            String rightType = unaryExpression();

            if (!leftType.equals(rightType)) {
                throw new SemanticException("Type error: Cannot perform operation " + operator + " on " + leftType + " and " + rightType);
            }

            if (!leftType.equals("int") && !leftType.equals("float")) {
                throw new SemanticException("Type error: Cannot perform operation " + operator + " on " + leftType);
            }
        }
        return leftType;
    }

    private String unaryExpression() {
        if (peek(COMPARATOR, "!")) {
            consume(COMPARATOR, "!");
            String operandType = unaryExpression();

            if (!operandType.equals("boolean")) {
                throw new SemanticException("Type error: Cannot perform operation ! on " + operandType);
            }
            return "boolean";
        } else {
            String value;
            if (peek(IDENTIFIER)) {
                if (peekNext(PUNCTUATION, "(")) {
                    Symbol symbol = symbolTable.getFunctionSymbol(currentToken.getValue());
                    value = getDefaultValue(symbol);
                } else {
                    Symbol symbol = symbolTable.getVariableSymbol(currentToken.getValue());
                    value = symbol.getValue();
                }
            } else {
                value = currentToken.getValue();
            }
            if (value != null) {
                if (operationExecutor.isInsideArray()) {
                    operationExecutor.pushInsideArrayOperator(value);
                } else {
                    operationExecutor.pushOperator(value);
                }
            }

            return primaryExpression();
        }
    }

    private String primaryExpression() {
        if (peek(INTEGER)) {
            consume(INTEGER);
            return "int";
        } else if (peek(FLOAT)) {
            consume(FLOAT);
            return "float";
        } else if (peek(STRING)) {
            consume(STRING);
            return "string";
        } else if (peek(KEYWORD, "true") || peek(KEYWORD, "false")) {
            consume(KEYWORD);
            return "boolean";
        } else if (peek(KEYWORD, "null")) {
            consume(KEYWORD);
            return "null";
        } else if (peek(IDENTIFIER)) {
            if (peekNext(PUNCTUATION, "(")) {
                return functionCallStatement();
            }

            String identifier = currentToken.getValue();
            Symbol symbol = symbolTable.getVariableSymbol(identifier);

            if (symbolTable.getVariableSymbol(identifier) instanceof ArraySymbol) {
                operationExecutor.setArraySymbol((ArraySymbol) symbolTable.getVariableSymbol(identifier));
            }

            if (!symbolTable.hasSymbol(identifier)) {
                throw new SemanticException("Undeclared variable: Variable " + identifier + " not declared " + currentTokenIndex);
            } else if (!symbol.isInitialized()) {
                throw new SemanticException("Unassigned variable: Variable " + identifier + " not initialized");
            } else {
                consume(IDENTIFIER);
                if (peek(PUNCTUATION, "[")) {
                    operationExecutor.setIsInsideArray(true);
                    arrayAccess(identifier);
                    operationExecutor.setIsInsideArray(false);
                }
            }
            return symbol.getType();
        } else if (peek(PUNCTUATION, "(")) {
            consume(PUNCTUATION, "(");
            String type = expression();
            consume(PUNCTUATION, ")");
            return type;
        } else {
            return "void";
        }
    }

    private boolean peek(TokenType type) {
        return currentToken.getType() == type;
    }

    private boolean peek(TokenType type, String value) {
        return currentToken.getType() == type && value.equals(currentToken.getValue());
    }

    private boolean peekNext(TokenType type, String value) {
        return currentTokenIndex + 1 < tokens.size() &&
                tokens.get(currentTokenIndex + 1).getType() == type &&
                value.equals(tokens.get(currentTokenIndex + 1).getValue());
    }

    private void consume(TokenType type) throws SyntaxException {
        if (currentToken.getType() == type) {
            next();
        } else {
            throw new SyntaxException(
                    String.format("Syntax error: Expected type (%s) but found type (%s)\n Token: %s\n index: %d",
                            type,
                            currentToken.getType(),
                            currentToken.toString(),
                            currentTokenIndex
                    ));

        }
    }

    private void consume(TokenType type, String value) {
        if (currentToken.getType() == type && (value == null || value.equals(currentToken.getValue()))) {
            next();
        } else {
            throw new SyntaxException(
                    String.format("Syntax error: Expected type(%s) and value(%s) but found type (%s) and value(%s)" +
                                    "\n Token: %s\n index: %d",
                            type,
                            value,
                            currentToken.getType(),
                            currentToken.getValue(),
                            currentToken.toString(),
                            currentTokenIndex
                    ));
        }
    }

    private void consume(List<String> values) {
        if (currentToken.getType() == TYPE && values.contains(currentToken.getValue())) {
            next();
        } else {
            throw new SyntaxException(
                    String.format("Syntax error: Expected type(%s) and values(%s) but found type (%s) and value(%s)" +
                                    "\n Token: %s\n index: %d",
                            TYPE,
                            values,
                            currentToken.getType(),
                            currentToken.getValue(),
                            currentToken.toString(),
                            currentTokenIndex
                    ));
        }
    }

    private void next() {
        currentTokenIndex++;
        if (currentTokenIndex < tokens.size()) {
            currentToken = tokens.get(currentTokenIndex);
        }
    }

    private List<String> getTypes() {
        return new ArrayList<>(List.of("int", "float", "string", "boolean", "void"));
    }

    private String getDefaultValue(Symbol symbol) {
        switch (symbol.getType()) {
            case "string":
                return "";
            case "int":
                return "0";
            case "float":
                return "0.0";
            case "boolean":
                return "false";
            default:
                return null;
        }
    }

    private void collectFunctionDeclarations() {
        while (currentTokenIndex < tokens.size()) {
            boolean isArray = false;
            boolean hasReturn = false;
            if (peek(TYPE)) {
                if (!"void".equals(currentToken.getValue())) {
                    hasReturn = true;
                }
                String returnType = currentToken.getValue();
                next();
                if (peek(PUNCTUATION, "[")) {
                    next();
                    next();
                    isArray = true;
                }
                if (peek(IDENTIFIER)) {
                    String functionName = currentToken.getValue();
                    next();
                    if (peek(PUNCTUATION, "(")) {
                        next();

                        List<String> parameterTypes = new ArrayList<>();
                        while (!peek(PUNCTUATION, ")")) {
                            if (peek(TYPE)) {
                                parameterTypes.add(currentToken.getValue());
                                next();
                            } else if (peek(PUNCTUATION, ",")) {
                                next();
                            } else {
                                next();
                            }
                        }
                        FunctionSymbol functionSymbol = new FunctionSymbol(functionName, returnType, parameterTypes);
                        functionSymbol.setInitialized(true);
                        functionSymbol.setHasReturn(hasReturn);
                        functionSymbol.setIsArray(isArray);
                        symbolTable.addSymbol(functionName, functionSymbol);
                    }
                }
            }
            next();
        }
        currentTokenIndex = 0;
        currentToken = tokens.get(currentTokenIndex);
    }
}
