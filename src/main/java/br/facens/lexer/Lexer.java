package br.facens.lexer;

import br.facens.constants.RegexConstants;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static br.facens.constants.RegexConstants.comparator;
import static br.facens.constants.RegexConstants.floatR;
import static br.facens.constants.RegexConstants.identifier;
import static br.facens.constants.RegexConstants.integerR;
import static br.facens.constants.RegexConstants.keyword;
import static br.facens.constants.RegexConstants.operator;
import static br.facens.constants.RegexConstants.stringR;
import static br.facens.constants.RegexConstants.syntax;
import static br.facens.constants.RegexConstants.type;

public class Lexer {

    public static Token analyze(String lexeme) {
        String regex = String.join("|",
                keyword,
                type,
                operator,
                comparator,
                syntax,
                identifier,
                integerR,
                floatR,
                stringR
        );

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(lexeme);

        if (matcher.matches()) {
            TokenType type;

            if (lexeme.matches(keyword)) {
                type = TokenType.KEYWORD;
            } else if (lexeme.matches(RegexConstants.type)) {
                type = TokenType.TYPE;
            } else if (lexeme.matches(RegexConstants.operator)) {
                type = TokenType.OPERATOR;
            } else if (lexeme.matches(comparator)) {
                type = TokenType.COMPARATOR;
            } else if (lexeme.matches(syntax)) {
                type = TokenType.SYNTAX;
            } else if (lexeme.matches(identifier)) {
                type = TokenType.IDENTIFIER;
            } else if (lexeme.matches(integerR)) {
                type = TokenType.INTEGER;
            } else if (lexeme.matches(floatR)) {
                type = TokenType.FLOAT;
            } else if (lexeme.matches(stringR)) {
                type = TokenType.STRING;
                lexeme = lexeme.replace("\"", "");
            } else {
                return null;
            }

            return new Token(type, lexeme);
        }

        return null;
    }
}
