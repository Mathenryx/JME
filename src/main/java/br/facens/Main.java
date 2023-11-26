package br.facens;

import br.facens.exceptions.LexerException;
import br.facens.lexer.Lexer;
import br.facens.lexer.Token;
import br.facens.parser.Parser;
import br.facens.reader.FileReader;

import java.io.IOException;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        try {
            System.out.println("========== Reading Files ==========");
            List<String> lexemes = FileReader.read("src/main/resources/file2.txt");
            System.out.println("Files Read Successfully\n");

            System.out.println("========== Lexical Analysis ==========");
            List<Token> tokens = Lexer.analyze(lexemes);
            System.out.println("Files Tokenizes Successfully\n");

            System.out.println("========== Parser ==========");
            Parser parser = new Parser(tokens);
            parser.parse();

        } catch (IOException | LexerException e) {
            e.printStackTrace();
        }
    }
}