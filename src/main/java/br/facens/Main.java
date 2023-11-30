package br.facens;

import br.facens.exceptions.LexerException;
import br.facens.exceptions.SemanticException;
import br.facens.exceptions.SyntaxException;
import br.facens.lexer.Lexer;
import br.facens.lexer.Token;
import br.facens.parser.Parser;
import br.facens.reader.FileReader;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            try {
                System.out.println("\n========== Inserting File ==========");
                System.out.print("Insert file name (or 'exit' to close): ");
                String fileName = scanner.nextLine();

                if (fileName.equalsIgnoreCase("exit")) {
                    System.out.println("Closing program...");
                    break;
                } else if (!fileName.endsWith(".jme")) {
                    System.out.println("File extension must be .jme");
                } else {
                    System.out.println("\n========== Reading Files ==========");
                    List<String> lexemes = FileReader.read("src/main/resources/" + fileName);
                    System.out.println("Files Read Successfully");

                    System.out.println("\n========== Lexical Analysis ==========");
                    List<Token> tokens = Lexer.analyze(lexemes);
                    System.out.println("Files Tokenizes Successfully");

                    System.out.println("\n========== Parser ==========");
                    Parser parser = new Parser(tokens);
                    parser.parse();
                    System.out.println("Parser Completed Successfully!\n");
                }


            } catch (IOException | LexerException | SyntaxException | SemanticException e) {
                e.printStackTrace();
            }
        }
        scanner.close();
    }
}