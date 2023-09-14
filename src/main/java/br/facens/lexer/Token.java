package br.facens.lexer;

public class Token {
    public TokenType type;
    public String lexeme;

    public Token(TokenType type, String lexeme) {
        this.type = type;
        this.lexeme = lexeme;
    }

    @Override
    public String toString() {
        return "<" + type + ", " + lexeme + ">";
    }
}
