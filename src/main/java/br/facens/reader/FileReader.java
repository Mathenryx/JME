package br.facens.reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

import static br.facens.constants.RegexConstants.COMPARATOR_REGEX;
import static br.facens.constants.RegexConstants.OPERATOR_REGEX;
import static br.facens.constants.RegexConstants.PUNCTUATION_REGEX;

public class FileReader {

    public static List<String> read(String path) throws IOException {

        // Abre o arquivo para leitura
        BufferedReader reader = new BufferedReader(new java.io.FileReader(path));

        String line;
        StringBuilder stringBuilder = new StringBuilder();

        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line).append(" ");
        }

        // Fecha o arquivo após a leitura
        reader.close();

        String fileContent = stringBuilder.toString();

        fileContent = addSpaces(fileContent);
        fileContent = fileContent.replaceAll("\\s+", " ");

        // Divide a linha em tokens com base em espaços em branco
        return List.of(fileContent.split("(?<=\\s)(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"));

    }

    private static String addSpaces(String input) {
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
            } else if (isOperatorComparatorOrPunctuation(currentChar)) {
                result.append(' ');
                result.append(currentChar);
                if (isDoubleOperatorOrPunctuation(input, i)) {
                    result.append(input.charAt(++i));
                }
                result.append(' ');
            } else {
                result.append(currentChar);
            }
        }
        return result.toString(); // Retorna a string com espaços adicionados conforme necessário
    }

    private static boolean isOperatorComparatorOrPunctuation(char c) {
        // Verifica se o caractere atual é um operador de um único caractere
        String pattern = OPERATOR_REGEX + "|" + COMPARATOR_REGEX + "|" + PUNCTUATION_REGEX;
        return String.valueOf(c).matches(pattern);
    }

    private static boolean isDoubleOperatorOrPunctuation(String input, int index) {
        // Verifica se o caractere atual e o próximo caractere formam um operador de dois caracteres
        if (index < input.length() - 1) {
            String twoCharOperator = input.charAt(index) + String.valueOf(input.charAt(index + 1));
            return twoCharOperator.matches(OPERATOR_REGEX) || twoCharOperator.matches(COMPARATOR_REGEX);
        }
        return false;
    }

}
