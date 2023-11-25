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
    private final List<String> userTypes;
    private final SymbolTable symbolTable;
    private FunctionSymbol functionSymbol;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.currentTokenIndex = 0;
        this.currentToken = tokens.get(0);
        this.userTypes = new ArrayList<>();
        this.symbolTable = new SymbolTable();
        functionSymbol = null;
    }

    // Entry point for parsing the program
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

    // Inicio do programa
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
        match(TYPE, "void"); // match the 'void' keyword
        match(KEYWORD, "main"); // match the 'main' keyword
        match(PUNCTUATION, "("); // match the opening parenthesis
        match(PUNCTUATION, ")"); // match the closing parenthesis
        block(); // Parse the main method's block
    }



    private void declarations() {
        while (!peek(PUNCTUATION, "}") && !peekNext(KEYWORD, "main")) {
            declaration();
        }
    }

    private void declaration() {// TODO: FIX
        String type = currentToken.getValue();
        match(TYPE, getTypes());
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
        if (peek(PUNCTUATION, "[")) { // Verify if it's an array
            arrayAccess();
        }

        this.functionSymbol = (FunctionSymbol) symbolTable.getSymbol(currentToken.getValue());
        match(IDENTIFIER); // match the function name

        match(PUNCTUATION, "("); // match the opening parenthesis

        parameters();

        match(PUNCTUATION, ")"); // match the closing parenthesis

        block(); // Parse the function body

        if (!"void".equals(returnType) && !this.functionSymbol.hasReturn()) {
            throw new RuntimeException("Erro semântico: função deveria retornar um: " + returnType);
        }

        this.functionSymbol = null;
    }

    private void parameters() {
        if (peek(TYPE)) {
            parameter(); // Parse the first parameter

            // Check for additional parameters
            while (peek(TokenType.PUNCTUATION, ",")) {
                match(TokenType.PUNCTUATION, ",");
                parameter();
            }
        }
    }

    private void parameter() {
        match(TYPE, getTypes());
        if (peek(PUNCTUATION, "[")) {
            arrayAccess();
        }
        match(IDENTIFIER);
    }

    private void varDeclaration(String type) {
        if (peek(TokenType.PUNCTUATION, "[")) { // Verify if it's an array
            arrayDeclaration(type);
        } else {
            String id = currentToken.getValue();
            match(IDENTIFIER); // match the variable name
            //SymbolTable symbolTable = symbolTables.peek();
            Symbol symbol = new Symbol(id, type);
            symbolTable.addSymbol(id, symbol);

            if (peek(OPERATOR, "=")) {
                match(OPERATOR, "="); // match the equals sign

                String leftType = symbol.getType();
                String rightType = expression();

                if (!leftType.equals(rightType)) {
                    throw new RuntimeException("Type error: Cannot assign " + rightType + " to " + leftType + " " + currentTokenIndex);
                }
                symbol.setInitialized(true);
            }
        }
        match(PUNCTUATION, ";"); // match the semicolon
    }

    private void arrayDeclaration(String type) {
        match(PUNCTUATION, "["); // match the opening bracket
        match(PUNCTUATION, "]"); // match the closing bracket
        String id = currentToken.getValue();
        match(IDENTIFIER); // match the variable name
        //SymbolTable symbolTable = symbolTables.peek();
        symbolTable.addSymbol(id, new Symbol(id, type));

        if (peek(OPERATOR, "=")) {
            match(OPERATOR, "="); // match the equals sign
            arrayCreation(); // Parse the array creation expression
        }
    }

    private void arrayCreation() {
        if (peek(KEYWORD, "new")) {
            match(KEYWORD, "new"); // match the 'new' keyword
            match(TYPE, getTypes()); // match the array type
            arrayAccess();
        } else {
            functionCallStmt();
        }
    }

    private void arrayAccess() {
        match(TokenType.PUNCTUATION, "["); // match the opening bracket
        expression(); // Parse the expression inside the brackets
        match(TokenType.PUNCTUATION, "]"); // match the closing bracket
    }

    private void block() {
        symbolTable.pushScope();
        match(TokenType.PUNCTUATION, "{");
        statements();
        ; // Parse the statements inside the block
        match(TokenType.PUNCTUATION, "}");
        symbolTable.popScope(); // Pop the symbol table from the stack
    }

    private void statements() {
        while (isStatementStart()) {
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
        // Implement the methods for other statement types based on the grammar rules
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
                match(TokenType.PUNCTUATION, ";"); // match the semicolon
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
            match(TYPE, getTypes());
            varDeclaration(type);
        } else if (peek(KEYWORD, "break")) {
            match(KEYWORD, "break");
            match(PUNCTUATION, ";");
        } else {
            block();
        }
    }

    private void ifStmt() {
        match(TokenType.KEYWORD, "if");
        match(TokenType.PUNCTUATION, "(");
        expression(); // Assuming you have a method for parsing expressions
        match(TokenType.PUNCTUATION, ")");
        block(); // Assuming you have a method for parsing blocks

        if (peek(TokenType.KEYWORD, "else")) {
            match(TokenType.KEYWORD, "else");
            if (peek(TokenType.KEYWORD, "if")) {
                ifStmt(); // Recursive call to handle else if
            } else {
                block(); // Assuming you have a method for parsing blocks
            }
        }
    }

    private void whileStmt() {
        match(KEYWORD, "while");
        match(PUNCTUATION, "(");
        expression(); // Assuming you have a method for parsing expressions
        match(PUNCTUATION, ")");
        block(); // Assuming you have a method for parsing blocks
    }

    private void forEachStmt() {
        match(KEYWORD, "foreach");
        match(PUNCTUATION, "(");
        match(TYPE, getTypes());
        match(IDENTIFIER);
        match(PUNCTUATION, ":");
        match(IDENTIFIER);
        match(PUNCTUATION, ")");
        block();
    }

    private void forStmt() {
        match(KEYWORD, "for");
        match(PUNCTUATION, "(");
        forInit();
        match(PUNCTUATION, ";");
        expression();
        match(PUNCTUATION, ";");
        expression();
        match(PUNCTUATION, ")");
        block();
    }

    private void forInit() {
        if (peek(TokenType.TYPE, "int")) {
            match(TYPE, "int");
        } else if (peek(TokenType.TYPE)) {
            //TODO: adicionar exceção de int no for
        }

        match(TokenType.IDENTIFIER);

        if (peek(OPERATOR, "=")) {
            match(OPERATOR, "=");
            expression();
        }
    }

    private void printStmt() {
        match(KEYWORD, "print");
        match(PUNCTUATION, "(");
        expression();
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

    private void functionCallStmt() {
        String functionName = currentToken.getValue();
        FunctionSymbol functionSymbol = (FunctionSymbol) symbolTable.getSymbol(functionName);

        if (functionSymbol == null) {
            throw new RuntimeException("Função " + functionName + " não declarada.");
        }

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
    }

    private List<String> arguments() {
        List<String> argumentTypes = new ArrayList<>();
        argumentTypes.add(expression());

        while (peek(TokenType.PUNCTUATION, ",")) {
            match(TokenType.PUNCTUATION, ","); // Match the comma
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
        match(IDENTIFIER); // Match the identifier

        if (peek(PUNCTUATION, "[")) {
            // Array access assignment
            arrayAccess();
        }
        // Regular assignment
        assignment(id);

        match(PUNCTUATION, ";"); // Match the semicolon
    }

    private void assignment(String id) {
        Symbol symbol = symbolTable.getSymbol(id);

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

        // Check the types of the operands
        if (!leftType.equals(rightType)) {
            throw new RuntimeException("Type error: Cannot assign " + rightType + " to " + leftType + " " + currentTokenIndex);
        }
        symbol.setInitialized(true);
    }


    private String expression() {
        if (peek(KEYWORD, "new")) {
            objectCreation();
            return "object"; // Assuming that the type of a new object is "object"
        } else if (peek(OPERATOR, "++")) {
            incrementStmt();
            return "void"; // Assuming that increment statements do not have a return value
        } else if (peek(OPERATOR, "--")) {
            decrementStmt();
            return "void"; // Assuming that decrement statements do not have a return value
        } else if (peek(IDENTIFIER)) {
            if (peekNext(OPERATOR, "++")) {
                incrementStmt();
                return "void"; // Assuming that increment statements do not have a return value
            } else if (peekNext(OPERATOR, "--")) {
                decrementStmt();
                return "void"; // Assuming that decrement statements do not have a return value
            } else if (peekNext(PUNCTUATION, "(")) {
                functionCallStmt();
                return "void"; // Assuming that function calls do not have a return value
            } else {
                return logicalExpr();
            }
        } else {
            return logicalExpr();
        }
    }

    private String logicalExpr() {
        String leftType = equalityExpr(); // Parse the left operand

        while (peek(COMPARATOR, "&&") || peek(COMPARATOR, "||")) {
            String operator = currentToken.getValue();
            match(COMPARATOR); // Match the logical operator
            String rightType = equalityExpr(); // Parse the right operand

            // Check the types of the operands
            if (!leftType.equals(rightType) || !leftType.equals("boolean")) {
                throw new RuntimeException("Type error: Cannot perform operation " + operator + " on " + leftType + " and " + rightType);
            }
        }
        return leftType; // Return the type of the expression
    }

    private String equalityExpr() {
        String leftType = comparisonExpr(); // Parse the left operand

        while (peek(COMPARATOR, "==") || peek(COMPARATOR, "!=")) {
            String operator = currentToken.getValue();
            match(COMPARATOR); // Match the equality operator
            String rightType = comparisonExpr(); // Parse the right operand

            // Check the types of the operands
            if (!leftType.equals(rightType)) {
                throw new RuntimeException("Type error: Cannot perform operation " + operator + " on " + leftType + " and " + rightType);
            }
        }
        return leftType; // Return the type of the expression
    }

    private String comparisonExpr() {
        String leftType = termExpr(); // Parse the left operand

        while (peek(COMPARATOR, "<") || peek(COMPARATOR, "<=") || peek(COMPARATOR, ">") || peek(COMPARATOR, ">=")) {
            String operator = currentToken.getValue();
            match(COMPARATOR); // Match the comparison operator
            String rightType = termExpr(); // Parse the right operand

            // Check the types of the operands
            if (!leftType.equals(rightType)) {
                throw new RuntimeException("Type error: Cannot perform operation " + operator + " on " + leftType + " and " + rightType);
            }
        }
        return leftType; // Return the type of the expression
    }

    private String termExpr() {
        String leftType = factorExpr(); // Parse the left operand

        while (peek(OPERATOR, "+") || peek(OPERATOR, "-")) {
            String operator = currentToken.getValue();
            match(OPERATOR); // Match the addition or subtraction operator
            String rightType = factorExpr(); // Parse the right operand

            // Check the types of the operands
            if (!leftType.equals(rightType)) {
                throw new RuntimeException("Type error: Cannot perform operation " + operator + " on " + leftType + " and " + rightType);
            }
        }
        return leftType; // Return the type of the expression
    }

    private String factorExpr() {
        String leftType = unaryExpr(); // Parse the left operand

        while (peek(OPERATOR, "*") || peek(OPERATOR, "/") || peek(OPERATOR, "%")) {
            String operator = currentToken.getValue();
            match(OPERATOR); // Match the multiplication, division, or modulo operator
            String rightType = unaryExpr(); // Parse the right operand

            // Check the types of the operands
            if (!leftType.equals(rightType)) {
                throw new RuntimeException("Type error: Cannot perform operation " + operator + " on " + leftType + " and " + rightType);
            }
        }
        return leftType; // Return the type of the expression
    }

    private String unaryExpr() {
        if (peek(COMPARATOR, "!")) {
            match(COMPARATOR, "!"); // Match the logical NOT operator
            String operandType = unaryExpr(); // Parse the operand

            // Check the type of the operand
            if (!operandType.equals("boolean")) {
                throw new RuntimeException("Type error: Cannot perform operation ! on " + operandType);
            }
            return "boolean"; // The result of a logical NOT operation is always boolean
        } else {
            return primaryExpr(); // Parse the primary expression
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
            String identifier = currentToken.getValue();
//            SymbolTable currentSymbolTable = symbolTables.peek();
//            Symbol symbol = currentSymbolTable.getSymbol(identifier);
            Symbol symbol = symbolTable.getSymbol(identifier);

            if (!symbolTable.hasSymbol(identifier)) {
                throw new RuntimeException("Variable " + identifier + " not declared " + currentTokenIndex);
            } else if (!symbol.isInitialized()) {
                throw new RuntimeException("Variable " + identifier + " not initialized");
            } else {
                match(IDENTIFIER);
            }

            return symbolTable.getSymbol(identifier).getType();
        } else if (peek(PUNCTUATION, "(")) {
            match(PUNCTUATION, "(");
            String type = expression();
            match(PUNCTUATION, ")");
            return type;
        } else {
            // For other cases where there is no return value
            return "void";
        }
    }

    private void objectCreation() {
        match(KEYWORD, "new"); // Match the "new" keyword

        match(IDENTIFIER); // Match the class name

        match(PUNCTUATION, "("); // Match the opening parenthesis

        if (!peek(PUNCTUATION, ")")) {
            // If there are arguments, parse them
            arguments();
        }

        match(PUNCTUATION, ")"); // Match the closing parenthesis

        match(PUNCTUATION, ";"); // Match the semicolon
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

    private void match(TokenType type, List<String> values) {
        if (currentToken.getType() == type && values.contains(currentToken.getValue())) {
            next();
        } else {
            throw new RuntimeException(
                    String.format("Syntax error: Expected type(%s) and values(%s) but found type (%s) and value(%s)" +
                                    "\n Token: %s\n index: %d",
                            type,
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
        List<String> types = new ArrayList<>();
        types.addAll(List.of("int", "float", "string", "boolean", "void"));
        types.addAll(userTypes);
        return types;
    }

    public void collectFunctionDeclarations() {
        while (currentTokenIndex < tokens.size()) {
            if (peek(TYPE)) {
                String returnType = currentToken.getValue();
                next();
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
