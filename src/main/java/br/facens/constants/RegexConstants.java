package br.facens.constants;

public class RegexConstants {

    public final static String keyword = "\\b(function|if|else|switch|case|default|for|while|break|return|" +
            "true|false|null|class|try|catch|finally|throw)\\b";
    public final static String type = "\\b(int|float|string|boolean|void)\\b";
    public final static String operator = "[-+*/%=^]+|\\+\\+|--|\\+=|-=";
    public final static String comparator = "(==|!=|!|<|<=|>|>=|&&|\\|\\|)";
    public final static String syntax = "[;(),.\\[\\]{}]";
    public final static String identifier = "\\b[A-Za-z_][A-Za-z_0-9]*\\b";
    public final static String integerR = "\\b\\d+\\b";
    public final static String floatR = "\\b\\d+\\.\\d+\\b";
    public final static String stringR = "^\"[^\"]*\"$";
}
