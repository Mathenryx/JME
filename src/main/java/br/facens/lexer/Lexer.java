package br.facens.lexer;

import br.facens.constants.RegexConstants;
import br.facens.exceptions.LexerException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static br.facens.constants.RegexConstants.comparator;
import static br.facens.constants.RegexConstants.floatR;
import static br.facens.constants.RegexConstants.identifier;
import static br.facens.constants.RegexConstants.integerR;
import static br.facens.constants.RegexConstants.keyword;
import static br.facens.constants.RegexConstants.operator;
import static br.facens.constants.RegexConstants.stringR;
import static br.facens.constants.RegexConstants.punctuation;
import static br.facens.constants.RegexConstants.type;

public class Lexer {
    private final static String regex =
            String.join("|",
                    keyword,
                    type,
                    operator,
                    comparator,
                    punctuation,
                    identifier,
                    integerR,
                    floatR,
                    stringR
            );

    public static List<Token> analyze(List<String> lexemes) throws LexerException {
        List<Token> tokens = new ArrayList<>();
        int count = 0;
        for (String lexeme : lexemes) {
            // Analise o token normalmente e adiciona na lista de tokens
            Token token = tokenize(lexeme.trim());
            tokens.add(token);
            System.out.println(count++ + " - " + token);
        }

        return tokens;
    }

    private static Token tokenize(String lexeme) throws LexerException {

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
            } else if (lexeme.matches(punctuation)) {
                type = TokenType.PUNCTUATION;
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
                throw new LexerException("Erro lexer encontrado no lexeme: " + lexeme);
            }

            return new Token(type, lexeme);
        }

        throw new LexerException("Erro lexer encontrado no lexeme: " + lexeme);
    }
}
