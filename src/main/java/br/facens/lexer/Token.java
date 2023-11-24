package br.facens.lexer;

public class Token {
    private final TokenType type;
    private final String lexeme;

    public Token(TokenType type, String lexeme) {
        this.type = type;
        this.lexeme = lexeme;
    }

    public TokenType getType() {
        return this.type;
    }

    public String getValue() {
        return this.lexeme;
    }

    @Override
    public String toString() {
        return "<" + type + ", '" + lexeme + "'>";
    }
}
