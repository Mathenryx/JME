package br.facens;

import br.facens.constants.RegexConstants;
import br.facens.lexer.Lexer;
import br.facens.lexer.Token;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        try {
            // Abre o arquivo para leitura
            BufferedReader reader = new BufferedReader(new FileReader("src/main/resources/file.txt"));

            String line;
            ArrayList<Token> tokens = new ArrayList<>();
            StringBuilder stringBuilder = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append(" ");
            }
            String fileContent = stringBuilder.toString();

            fileContent = addSpaces(fileContent);
            fileContent = fileContent.replaceAll("\\s+", " ");
            // Divide a linha em tokens com base em espaços em branco
            String[] tokenLexemes = fileContent.split("(?<=\\s)(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

            for (String tokenLexeme : tokenLexemes) {
                // Analise o token normalmente e adiciona na lista de tokens
                Token token = Lexer.analyze(tokenLexeme.trim());
                tokens.add(token);

            }

            // Fecha o arquivo após a leitura
            reader.close();

            // Exibe os tokens
            for (Token token : tokens) {
                System.out.println(token);
            }
        } catch (
                IOException e) {
            e.printStackTrace();
        }
    }

    public static String addSpaces(String input) {
        StringBuilder result = new StringBuilder();
        boolean insideString = false; // Indica se estamos dentro de uma string delimitada por aspas
        boolean isEscaped = false; // Indica se o caractere atual está escapado

        for (int i = 0; i < input.length(); i++) {
            char currentChar = input.charAt(i);

            if (currentChar == '"' && !isEscaped) {
                // Verifica se encontramos uma aspa dupla não escapada, indicando o início ou fim de uma string
                insideString = !insideString;
            }

            if (currentChar == '\\') {
                isEscaped = !isEscaped; // Inverte o estado de escapamento ao encontrar uma barra invertida
            } else {
                isEscaped = false; // Se não encontrarmos uma barra invertida, resetamos o estado de escapamento
            }

            // Verifica se estamos dentro de uma string ou se o caractere atual é um espaço em branco
            if (insideString || Character.isWhitespace(currentChar)) {
                result.append(currentChar);
            } else if (isOperatorComparatorOrSyntax(currentChar)) {
                result.append(' ');
                result.append(currentChar);
                if (isDoubleOperator(input, i)) {
                    result.append(input.charAt(++i));
                }
                result.append(' ');
            } else {
                result.append(currentChar);
            }
        }
        return result.toString(); // Retorna a string com espaços adicionados conforme necessário
    }

    private static boolean isOperatorComparatorOrSyntax(char c) {
        // Verifica se o caractere atual é um operador de um único caractere
        String pattern = RegexConstants.operator + "|" + RegexConstants.comparator + "|" + RegexConstants.syntax;
        return String.valueOf(c).matches(pattern);
    }

    private static boolean isDoubleOperator(String input, int index) {
        // Verifica se o caractere atual e o próximo caractere formam um operador de dois caracteres
        if (index < input.length() - 1) {
            String twoCharOperator = input.charAt(index) + String.valueOf(input.charAt(index + 1));
            return twoCharOperator.matches(RegexConstants.operator) || twoCharOperator.matches(RegexConstants.comparator);
        }
        return false;
    }


}