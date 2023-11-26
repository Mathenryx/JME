package br.facens.parser;

import br.facens.lexer.Token;
import br.facens.lexer.TokenType;
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
        try {
            symbolTable.pushScope();
            collectFunctionDeclarations();
            program();
            System.out.println("Análise sintática bem-sucedida!");
        } catch (RuntimeException e) {
            e.printStackTrace();
            System.out.println("Erro de análise sintática: " + e);
        }
    }

    private void program() {
        mainClass();
    }

    // Classe main
    private void mainClass() {
        match(TokenType.KEYWORD, "class");
        match(TokenType.KEYWORD, "Main");
        match(PUNCTUATION, "{");

        declarations();

        mainMethod();

        declarations();

        match(PUNCTUATION, "}");
    }

    private void mainMethod() {
        match(TYPE, "void");
        match(KEYWORD, "main");
        match(PUNCTUATION, "(");
        match(PUNCTUATION, ")");

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
        match(getTypes());
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
            match(PUNCTUATION, "[");
            match(PUNCTUATION, "]");
        }

        this.functionSymbol = (FunctionSymbol) symbolTable.getFunctionSymbol(currentToken.getValue());
        match(IDENTIFIER);

        symbolTable.pushScope();
        match(PUNCTUATION, "(");

        parameters();

        match(PUNCTUATION, ")");


        block();
        symbolTable.popScope();

        if (!"void".equals(returnType) && !this.functionSymbol.hasReturn()) {
            throw new RuntimeException("Erro semântico: função deveria retornar um: " + returnType);
        }

        this.functionSymbol = null;
    }

    private void parameters() {
        if (peek(TYPE)) {
            parameter();

            while (peek(TokenType.PUNCTUATION, ",")) {
                match(TokenType.PUNCTUATION, ",");
                parameter();
            }
        }
    }

    private void parameter() {
        String type = currentToken.getValue();
        match(getTypes());

        if (peek(PUNCTUATION, "[")) {
            match(TokenType.PUNCTUATION, "[");
            match(TokenType.PUNCTUATION, "]");
        }

        String id = currentToken.getValue();
        match(IDENTIFIER);

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
                throw new RuntimeException("Variable " + id + " has already been declared");
            }
            Symbol symbol = new Symbol(id, type);
            symbolTable.addSymbol(id, symbol);

            match(IDENTIFIER);
            if (peek(OPERATOR, "=")) {
                assignment(id);
            }
        }
        match(PUNCTUATION, ";");
    }

    private void arrayDeclaration(String type) {
        match(PUNCTUATION, "[");
        match(PUNCTUATION, "]");
        String id = currentToken.getValue();
        match(IDENTIFIER);

        symbolTable.addSymbol(id, new Symbol(id, type));

        if (peek(OPERATOR, "=")) {
            if (peekNext(KEYWORD, "new")) {
                match(OPERATOR, "=");
                String arrayType = arrayCreation(id);
                if (!arrayType.equals(type)) {
                    throw new RuntimeException("Type error: Cannot assign array of type " + arrayType + " to array of type " + type);
                }
            } else {
                assignment(id);
            }
        }
    }

    private String arrayCreation(String id) {
        if (peek(KEYWORD, "new")) {
            match(KEYWORD, "new");
            String type = currentToken.getValue();
            match(getTypes());
            String arraySizeString = arrayAccess();
            int arraySize = Integer.parseInt(arraySizeString == null ? "10" : arraySizeString);

            for (int i = 0; i < arraySize; i++) {
                symbolTable.addSymbol(id + "[" + i + "]", new Symbol(id + "[" + i + "]", type));
            }

            Symbol symbol = symbolTable.getVariableSymbol(id);
            symbol.setInitialized(true);
            return type;
        } else {
            return functionCallStmt();
        }
    }

    private String arrayAccess() {
        match(TokenType.PUNCTUATION, "[");
        //String arrayValue = currentToken.getValue();
        String indexType = expression();

        if (!indexType.matches("int")) {
            throw new RuntimeException("Array index must be an integer, but got " + indexType);
        }
        match(TokenType.PUNCTUATION, "]");
        return operationExecutor.executeOperations();
    }

    private void block() {
        match(TokenType.PUNCTUATION, "{");
        statements();
        match(TokenType.PUNCTUATION, "}");
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
            ifStmt();
        } else if (peek(TokenType.KEYWORD, "while")) {
            whileStmt();
        } else if (peek(TokenType.KEYWORD, "for")) {
            forStmt();
        } else if (peek(TokenType.KEYWORD, "foreach")) {
            forEachStmt();
        } else if (peek(TokenType.KEYWORD, "print")) {
            printStmt();
        } else if (peek(TokenType.KEYWORD, "return")) {
            returnStmt();
        } else if (peek(TokenType.IDENTIFIER)) {
            if (peekNext(TokenType.PUNCTUATION, "(")) {
                functionCallStmt();
                match(TokenType.PUNCTUATION, ";");
            } else if (peekNext(OPERATOR, "++")) {
                incrementStmt();
                match(PUNCTUATION, ";");
            } else if (peekNext(OPERATOR, "--")) {
                decrementStmt();
                match(PUNCTUATION, ";");
            } else {
                assignmentStmt();
            }
        } else if (peek(OPERATOR, "++")) {
            incrementStmt();
        } else if (peek(OPERATOR, "--")) {
            decrementStmt();
        } else if (peek(TYPE)) {
            String type = currentToken.getValue();
            match(getTypes());
            varDeclaration(type);
        } else if (peek(KEYWORD, "break")) {
            match(KEYWORD, "break");
            match(PUNCTUATION, ";");
        } else {
            symbolTable.pushScope();
            block();
            symbolTable.popScope();
        }
    }

    private void ifStmt() {
        match(TokenType.KEYWORD, "if");
        match(TokenType.PUNCTUATION, "(");
        expression();
        match(TokenType.PUNCTUATION, ")");
        symbolTable.pushScope();
        block();
        symbolTable.popScope();

        if (peek(TokenType.KEYWORD, "else")) {
            match(TokenType.KEYWORD, "else");
            if (peek(TokenType.KEYWORD, "if")) {
                ifStmt();
            } else {
                symbolTable.pushScope();
                block();
                symbolTable.popScope();
            }
        }
    }

    private void whileStmt() {
        match(KEYWORD, "while");
        match(PUNCTUATION, "(");
        expression();
        match(PUNCTUATION, ")");
        symbolTable.pushScope();
        block();
        symbolTable.popScope();
    }

    private void forEachStmt() {
        symbolTable.pushScope();
        match(KEYWORD, "foreach");
        match(PUNCTUATION, "(");


        String type = currentToken.getValue();
        match(getTypes());

        String id = currentToken.getValue();
        match(IDENTIFIER);

        Symbol symbol = new Symbol(id, type);
        symbol.setInitialized(true);
        symbolTable.addSymbol(id, symbol);

        match(PUNCTUATION, ":");
        match(IDENTIFIER);
        match(PUNCTUATION, ")");
        block();
        symbolTable.popScope();
    }

    private void forStmt() {
        symbolTable.pushScope();
        match(KEYWORD, "for");
        match(PUNCTUATION, "(");
        forInit();
        match(PUNCTUATION, ";");
        expression();
        match(PUNCTUATION, ";");
        expression();
        match(PUNCTUATION, ")");
        block();
        symbolTable.popScope();
    }

    private void forInit() {
        String type = null;
        Symbol symbol = null;
        String id = null;

        if (peek(TokenType.TYPE, "int")) {
            type = currentToken.getValue();
            match(TYPE, "int");
            id = currentToken.getValue();
            symbol = new Symbol(id, type);
        } else if (peek(TokenType.TYPE)) {
            //TODO: adicionar exceção de int no for
        }

        if (type != null) {
            symbolTable.addSymbol(id, symbol);
        }

        match(TokenType.IDENTIFIER);

        if (peek(OPERATOR, "=")) {
            assignment(id);
        }
    }

    private void printStmt() {
        match(KEYWORD, "print");
        match(PUNCTUATION, "(");
        expression();
        String print = operationExecutor.executeOperations();
        printList.add(print);
        match(PUNCTUATION, ")");
        match(PUNCTUATION, ";");
    }

    private void returnStmt() {
        match(KEYWORD, "return");

        if (!peek(PUNCTUATION, ";")) {
            this.functionSymbol.setHasReturn(true);
            String actualReturnType = expression();

            if (!this.functionSymbol.getType().equals(actualReturnType)) {
                throw new RuntimeException("Erro semântico: Tipo de retorno incorreto. Esperado: " + this.functionSymbol.getType() + ", mas foi: " + actualReturnType);
            }
        }

        match(PUNCTUATION, ";");
    }

    private String functionCallStmt() {
        String functionName = currentToken.getValue();
        Symbol symbol = symbolTable.getFunctionSymbol(functionName);

        if (symbol == null) {
            throw new RuntimeException("Função " + functionName + " não declarada.");
        } else if (!(symbol instanceof FunctionSymbol)) {
            throw new RuntimeException("Erro: " + functionName + " não é uma função.");
        }

        FunctionSymbol functionSymbol = (FunctionSymbol) symbol;

        match(IDENTIFIER);
        match(PUNCTUATION, "(");

        List<String> argumentTypes = new ArrayList<>();
        if (!peek(PUNCTUATION, ")")) {
            argumentTypes = arguments();
        }

        match(PUNCTUATION, ")");

        if (argumentTypes.size() != functionSymbol.getParameterTypes().size()) {
            throw new RuntimeException("Número incorreto de argumentos para a função " + functionName + ".");
        }

        for (int i = 0; i < argumentTypes.size(); i++) {
            if (!argumentTypes.get(i).equals(functionSymbol.getParameterTypes().get(i))) {
                throw new RuntimeException("Tipo incorreto de argumento para a função " + functionName + ".");
            }
        }
        return functionSymbol.getType();
    }

    private List<String> arguments() {
        List<String> argumentTypes = new ArrayList<>();
        argumentTypes.add(expression());

        while (peek(TokenType.PUNCTUATION, ",")) {
            match(TokenType.PUNCTUATION, ",");
            argumentTypes.add(expression());
        }
        return argumentTypes;
    }

    private void incrementStmt() {
        if (peek(TokenType.IDENTIFIER)) {
            match(TokenType.IDENTIFIER);
            match(OPERATOR, "++");
        } else {
            match(OPERATOR, "++");
            match(TokenType.IDENTIFIER);
        }
    }

    private void decrementStmt() {
        if (peek(TokenType.IDENTIFIER)) {
            match(TokenType.IDENTIFIER);
            match(OPERATOR, "--");
        } else {
            match(OPERATOR, "--");
            match(TokenType.IDENTIFIER);
        }
    }

    private void assignmentStmt() {
        String id = currentToken.getValue();
        match(IDENTIFIER);

        if (peek(PUNCTUATION, "[")) {
            String arrayIndex = arrayAccess();
            if (!symbolTable.hasSymbol(id)) {
                throw new RuntimeException("Array " + id + " not initialized ");
            }
            assignment(id + "[" + arrayIndex + "]");
        } else {
            assignment(id);
        }

        match(PUNCTUATION, ";");
    }


    private void assignment(String id) {
        Symbol symbol = symbolTable.getVariableSymbol(id);

        if (symbol == null) {
            throw new RuntimeException("Variable " + id + " not declared " + currentTokenIndex);
        }

        String leftType = symbol.getType();

        if (peek(OPERATOR, "+=")) {
            match(OPERATOR, "+=");
        } else if (peek(OPERATOR, "-=")) {
            match(OPERATOR, "-=");
        } else {
            match(OPERATOR, "=");
        }

        String rightType = expression();

        if (!leftType.equals(rightType)) {
            throw new RuntimeException("Type error: Cannot assign " + rightType + " to " + leftType + " " + currentTokenIndex);
        }
        symbol.setInitialized(true);
        symbol.setValue(operationExecutor.executeOperations());
    }


    private String expression() {
        if (peek(OPERATOR, "++")) {
            incrementStmt();
            return "void";
        } else if (peek(OPERATOR, "--")) {
            decrementStmt();
            return "void";
        } else if (peek(IDENTIFIER)) {
            if (peekNext(OPERATOR, "++")) {
                incrementStmt();
                return "void";
            } else if (peekNext(OPERATOR, "--")) {
                decrementStmt();
                return "void";
            } else {
                return logicalExpr();
            }
        } else {
            return logicalExpr();
        }
    }

    private String logicalExpr() {
        String leftType = equalityExpr();

        while (peek(COMPARATOR, "&&") || peek(COMPARATOR, "||")) {
            String operator = currentToken.getValue();
            operationExecutor.pushOperator(operator);
            match(COMPARATOR);

            String rightType = equalityExpr();

            if (!leftType.equals(rightType) || !leftType.equals("boolean")) {
                throw new RuntimeException("Type error: Cannot perform operation " + operator + " on " + leftType + " and " + rightType);
            }
            leftType = "boolean";
        }
        return leftType;
    }

    private String equalityExpr() {
        String leftType = comparisonExpr();

        while (peek(COMPARATOR, "==") || peek(COMPARATOR, "!=")) {
            String operator = currentToken.getValue();
            operationExecutor.pushOperator(operator);
            match(COMPARATOR);

            String rightType = comparisonExpr();

            if (!leftType.equals(rightType)) {
                throw new RuntimeException("Type error: Cannot perform operation " + operator + " on " + leftType + " and " + rightType);
            }
            leftType = "boolean";
        }
        return leftType;
    }

    private String comparisonExpr() {
        String leftType = termExpr();

        while (peek(COMPARATOR, "<") || peek(COMPARATOR, "<=") || peek(COMPARATOR, ">") || peek(COMPARATOR, ">=")) {
            String comparator = currentToken.getValue();
            operationExecutor.pushOperator(comparator);
            match(COMPARATOR);

            String rightType = termExpr();

            if (!leftType.equals(rightType) || (!leftType.equals("int") && !leftType.equals("float"))) {
                throw new RuntimeException("Type error: Cannot perform operation " + comparator + " on " + leftType + " and " + rightType);
            }
            leftType = "boolean";
        }
        return leftType;
    }

    private String termExpr() {
        String leftType = factorExpr();

        while (peek(OPERATOR, "+") || peek(OPERATOR, "-")) {
            String operator = currentToken.getValue();
            operationExecutor.pushOperator(operator);
            match(OPERATOR);

            String rightType = factorExpr();

            if (!leftType.equals(rightType)) {
                if (!(operator.equals("+") && (leftType.equals("string") || rightType.equals("string")))) {
                    throw new RuntimeException("Type error: Cannot perform operation " + operator + " on " + leftType + " and " + rightType);
                }
            }

            if (operator.equals("+") && (leftType.equals("string") || rightType.equals("string"))) {
                leftType = "string";
            } else if (operator.equals("+") && leftType.equals("boolean")) {
                throw new RuntimeException("Type error: Cannot perform addition on boolean type");
            } else if (operator.equals("-") && leftType.equals("boolean")) {
                throw new RuntimeException("Type error: Cannot perform subtraction on boolean type");
            }
        }
        return leftType;
    }

    private String factorExpr() {
        String leftType = unaryExpr();

        while (peek(OPERATOR, "*") || peek(OPERATOR, "/") || peek(OPERATOR, "%")) {
            String operator = currentToken.getValue();
            operationExecutor.pushOperator(operator);
            match(OPERATOR);

            String rightType = unaryExpr();

            if (!leftType.equals(rightType)) {
                throw new RuntimeException("Type error: Cannot perform operation " + operator + " on " + leftType + " and " + rightType);
            }

            if (!leftType.equals("int") && !leftType.equals("float")) {
                throw new RuntimeException("Type error: Cannot perform operation " + operator + " on " + leftType);
            }
        }
        return leftType;
    }

    private String unaryExpr() {
        if (peek(COMPARATOR, "!")) {
            match(COMPARATOR, "!");
            String operandType = unaryExpr();

            if (!operandType.equals("boolean")) {
                throw new RuntimeException("Type error: Cannot perform operation ! on " + operandType);
            }
            return "boolean";
        } else {
            String value;
            if (peek(IDENTIFIER) && !peekNext(PUNCTUATION, "(")) {
                Symbol symbol = symbolTable.getVariableSymbol(currentToken.getValue());
                value = symbol.getValue();
            } else {
                value = currentToken.getValue();
            }
            if(value != null) {
                operationExecutor.pushOperator(value);
            }
            return primaryExpr();
        }
    }

    private String primaryExpr() {
        if (peek(INTEGER)) {
            match(INTEGER);
            return "int";
        } else if (peek(FLOAT)) {
            match(FLOAT);
            return "float";
        } else if (peek(STRING)) {
            match(STRING);
            return "string";
        } else if (peek(KEYWORD, "true") || peek(KEYWORD, "false")) {
            match(KEYWORD);
            return "boolean";
        } else if (peek(KEYWORD, "null")) {
            match(KEYWORD);
            return "null";
        } else if (peek(IDENTIFIER)) {
            if (peekNext(PUNCTUATION, "(")) {
                return functionCallStmt();
            }

            String identifier = currentToken.getValue();
            Symbol symbol = symbolTable.getVariableSymbol(identifier);

            if (!symbolTable.hasSymbol(identifier)) {
                throw new RuntimeException("Variable " + identifier + " not declared " + currentTokenIndex);
            } else if (!symbol.isInitialized()) {
                throw new RuntimeException("Variable " + identifier + " not initialized");
            } else {
                match(IDENTIFIER);
                if (peek(PUNCTUATION, "[")) {
                    arrayAccess();
                }
            }
            return symbol.getType();
        } else if (peek(PUNCTUATION, "(")) {
            match(PUNCTUATION, "(");
            String type = expression();
            match(PUNCTUATION, ")");
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

    private boolean peekNext(TokenType type) {
        return currentTokenIndex + 1 < tokens.size() &&
                tokens.get(currentTokenIndex + 1).getType() == type;
    }

    private boolean peekNext(TokenType type, String value) {
        return currentTokenIndex + 1 < tokens.size() &&
                tokens.get(currentTokenIndex + 1).getType() == type &&
                value.equals(tokens.get(currentTokenIndex + 1).getValue());
    }

    private void match(TokenType type) {
        if (currentToken.getType() == type) {
            next();
        } else {
            throw new RuntimeException(
                    String.format("Syntax error: Expected type (%s) but found type (%s)\n Token: %s\n index: %d",
                            type,
                            currentToken.getType(),
                            currentToken.toString(),
                            currentTokenIndex
                    ));

        }
    }

    private void match(TokenType type, String value) {
        if (currentToken.getType() == type && (value == null || value.equals(currentToken.getValue()))) {
            next();
        } else {
            throw new RuntimeException(
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

    private void match(List<String> values) {
        if (currentToken.getType() == TYPE && values.contains(currentToken.getValue())) {
            next();
        } else {
            throw new RuntimeException(
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

    public void collectFunctionDeclarations() {
        while (currentTokenIndex < tokens.size()) {
            if (peek(TYPE)) {
                String returnType = currentToken.getValue();
                next();
                if (peek(PUNCTUATION, "[")) {
                    next();
                    next();
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
