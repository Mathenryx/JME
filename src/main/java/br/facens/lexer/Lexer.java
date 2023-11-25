package br.facens.lexer;

import br.facens.constants.RegexConstants;
import br.facens.exceptions.LexerException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static br.facens.constants.RegexConstants.COMPARATOR_REGEX;
import static br.facens.constants.RegexConstants.FLOAT_REGEX;
import static br.facens.constants.RegexConstants.IDENTIFIER_REGEX;
import static br.facens.constants.RegexConstants.INTEGER_REGEX;
import static br.facens.constants.RegexConstants.KEYWORD_REGEX;
import static br.facens.constants.RegexConstants.OPERATOR_REGEX;
import static br.facens.constants.RegexConstants.STRING_REGEX;
import static br.facens.constants.RegexConstants.PUNCTUATION_REGEX;
import static br.facens.constants.RegexConstants.TYPE_REGEX;

public class Lexer {

    private final static String regex =
            String.join("|",
                    KEYWORD_REGEX,
                    TYPE_REGEX,
                    OPERATOR_REGEX,
                    COMPARATOR_REGEX,
                    PUNCTUATION_REGEX,
                    IDENTIFIER_REGEX,
                    INTEGER_REGEX,
                    FLOAT_REGEX,
                    STRING_REGEX
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

            if (lexeme.matches(KEYWORD_REGEX)) {
                type = TokenType.KEYWORD;
            } else if (lexeme.matches(RegexConstants.TYPE_REGEX)) {
                type = TokenType.TYPE;
            } else if (lexeme.matches(RegexConstants.OPERATOR_REGEX)) {
                type = TokenType.OPERATOR;
            } else if (lexeme.matches(COMPARATOR_REGEX)) {
                type = TokenType.COMPARATOR;
            } else if (lexeme.matches(PUNCTUATION_REGEX)) {
                type = TokenType.PUNCTUATION;
            } else if (lexeme.matches(IDENTIFIER_REGEX)) {
                type = TokenType.IDENTIFIER;
            } else if (lexeme.matches(INTEGER_REGEX)) {
                type = TokenType.INTEGER;
            } else if (lexeme.matches(FLOAT_REGEX)) {
                type = TokenType.FLOAT;
            } else if (lexeme.matches(STRING_REGEX)) {
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
