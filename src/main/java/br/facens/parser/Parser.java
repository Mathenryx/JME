package br.facens.parser;

import br.facens.lexer.Token;
import br.facens.lexer.TokenType;

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
    private List<String> userTypes = new ArrayList<>();

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.currentTokenIndex = 0;
        this.currentToken = tokens.get(0);
    }

    // Entry point for parsing the program
    public void parse() {
        try {
            program();
            System.out.println("Análise sintática bem-sucedida!");
        } catch (Exception e) {
            System.out.println("Erro de análise sintática: " + e.getMessage());
        }
    }

    // Inicio do programa
    private void program() {
        mainClass();
        classDecls();
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

    private void classDecls() {
        while (peek(KEYWORD, "class")) {
            classDecl();
        }
    }

    private void classDecl() {
        match(KEYWORD, "class");
        userTypes.add(currentToken.getValue());
        match(IDENTIFIER);
        match(PUNCTUATION, "{");

        declarations();

        match(PUNCTUATION, "}");
    }

    private void declarations() {
        while (!peek(PUNCTUATION, "}") && !peekNext(KEYWORD, "main")) {
            declaration();
        }
    }

    private void declaration() {// TODO: FIX
        match(TYPE, getTypes());
        if (peek(IDENTIFIER)) {

            if (peekNext(PUNCTUATION, "(")) {
                functionDeclaration();
            } else {
                varDeclaration();
            }
        } else if (peek(PUNCTUATION, "[")) {
            functionDeclaration();
        } else {
            varDeclaration();
        }
    }

    private void functionDeclaration() {
        if (peek(PUNCTUATION, "[")) { // Verify if it's an array
            arrayAccess();
        }
        match(IDENTIFIER); // match the function name

        match(PUNCTUATION, "("); // match the opening parenthesis

        parameters(); // Parse function parameters

        match(PUNCTUATION, ")"); // match the closing parenthesis

        block(); // Parse the function body
    }

    private void parameters() {
        // Check if there are parameters
        if (peek(TYPE)) {
            parameter(); // Parse the first parameter

            // Check for additional parameters
            while (peek(TokenType.PUNCTUATION, ",")) {
                match(TokenType.PUNCTUATION, ","); // match the comma
                parameter(); // Parse the next parameter
            }
        }
    }

    private void parameter() {
        match(TYPE, getTypes()); // match the parameter type
        if (peek(PUNCTUATION, "[")) {
            arrayAccess();
        }
        match(IDENTIFIER); // match the parameter name
    }

    private void varDeclaration() {
        if (peek(TokenType.PUNCTUATION, "[")) { // Verify if it's an array
            arrayDeclaration();
        } else {
            match(IDENTIFIER); // match the variable name
            if (peek(OPERATOR, "=")) {
                match(OPERATOR, "="); // match the equals sign
                expression(); // Parse the expression for initialization
            }
        }
        match(PUNCTUATION, ";"); // match the semicolon
    }

    private void arrayDeclaration() {
        match(PUNCTUATION, "["); // match the opening bracket
        match(PUNCTUATION, "]"); // match the closing bracket
        match(IDENTIFIER); // match the array name

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
        match(TokenType.PUNCTUATION, "{"); // match the opening curly brace
        statements(); // Parse the statements inside the block
        match(TokenType.PUNCTUATION, "}"); // match the closing curly brace
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
            match(TYPE, getTypes());
            varDeclaration();
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
            expression();
        }

        match(PUNCTUATION, ";");
    }

    private void functionCallStmt() {
        match(IDENTIFIER);
        match(PUNCTUATION, "(");
        if (!peek(PUNCTUATION, ")")) {
            arguments();
        }
        match(PUNCTUATION, ")");
    }

    private void arguments() {
        expression(); // Parse the first argument

        // Parse additional arguments if there are any
        while (peek(TokenType.PUNCTUATION, ",")) {
            match(TokenType.PUNCTUATION, ","); // Match the comma
            expression(); // Parse the next argument
        }
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
        match(IDENTIFIER); // Match the identifier

        if (peek(PUNCTUATION, "[")) {
            // Array access assignment
            arrayAccess();
        }
        // Regular assignment
        assignment();

        match(PUNCTUATION, ";"); // Match the semicolon
    }

    private void assignment() {
        if (peek(OPERATOR, "+=")) {
            match(OPERATOR, "+=");
        } else if (peek(OPERATOR, "-=")) {
            match(OPERATOR, "-=");
        } else {
            match(OPERATOR, "=");
        }

        expression();
    }

    private void expression() {
        if (peek(KEYWORD, "new")) {
            objectCreation();
        } else if (peek(OPERATOR, "++")) {
            incrementStmt();
        } else if (peek(OPERATOR, "--")) {
            decrementStmt();
        } else if (peek(IDENTIFIER)) {
            if (peekNext(OPERATOR, "++")) {
                incrementStmt();
            } else if (peekNext(OPERATOR, "--")) {
                decrementStmt();
            } else {
                logicalExpr();
            }
        } else {
            logicalExpr();
        }
    }

    private void logicalExpr() {
        equalityExpr(); // Parse the left operand

        while (peek(COMPARATOR, "&&") || peek(COMPARATOR, "||")) {
            match(COMPARATOR); // Match the logical operator
            equalityExpr(); // Parse the right operand
        }
    }

    private void equalityExpr() {
        comparisonExpr(); // Parse the left operand

        while (peek(COMPARATOR, "==") || peek(COMPARATOR, "!=")) {
            match(COMPARATOR); // Match the equality operator
            comparisonExpr(); // Parse the right operand
        }
    }

    private void comparisonExpr() {
        termExpr(); // Parse the left operand

        while (peek(COMPARATOR, "<") || peek(COMPARATOR, "<=") || peek(COMPARATOR, ">") || peek(COMPARATOR, ">=")) {
            match(COMPARATOR); // Match the comparison operator
            termExpr(); // Parse the right operand
        }
    }

    private void termExpr() {
        factorExpr(); // Parse the left operand

        while (peek(OPERATOR, "+") || peek(OPERATOR, "-")) {
            match(OPERATOR); // Match the addition or subtraction operator
            factorExpr(); // Parse the right operand
        }
    }

    private void factorExpr() {
        unaryExpr(); // Parse the left operand

        while (peek(OPERATOR, "*") || peek(OPERATOR, "/") || peek(OPERATOR, "%")) {
            match(OPERATOR); // Match the multiplication, division, or modulo operator
            unaryExpr(); // Parse the right operand
        }
    }

    private void unaryExpr() {
        if (peek(COMPARATOR, "!")) {
            match(COMPARATOR, "!"); // Match the logical NOT operator
            unaryExpr(); // Parse the operand
        } else {
            primaryExpr(); // Parse the primary expression
        }
    }

    private void primaryExpr() {
        if (peek(INTEGER)) {
            match(INTEGER);
        } else if (peek(FLOAT)) {
            match(FLOAT);
        } else if (peek(STRING)) {
            match(STRING);
            //} else if (peek(BOOLEAN_LITERAL, null)) {
            //    match(BOOLEAN_LITERAL);
        } else if (peek(KEYWORD, "true")) {
            match(KEYWORD, "true");
        } else if (peek(KEYWORD, "false")) {
            match(KEYWORD, "false");
        } else if (peek(KEYWORD, "null")) {
            match(KEYWORD, "null");
        } else if (peek(IDENTIFIER)) {
            if (peekNext(PUNCTUATION, "(")) {
                functionCallStmt();
            } else {
                match(IDENTIFIER);
                if (peek(PUNCTUATION, "[")) {
                    arrayAccess();
                }
            }
        } else if (peek(PUNCTUATION, "(")) {
            match(PUNCTUATION, "(");
            expression();
            match(PUNCTUATION, ")");
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


    // ... Implement other parsing methods based on the grammar rules

    // Helper method to peek a token and advance the index
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

}
