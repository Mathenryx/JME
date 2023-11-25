package br.facens.constants;

public class RegexConstants {

    public final static String KEYWORD_REGEX = "\\b(class|Main|main|new|if|else|for|foreach|while|break|return|" +
            "true|false|null|class|print)\\b";
    public final static String TYPE_REGEX = "\\b(int|float|string|boolean|void)\\b";
    public final static String OPERATOR_REGEX = "(?:(?!==)[-+*/%=^]+|\\+\\+|--|\\+=|-=)";
    public final static String COMPARATOR_REGEX = "(==|!=|!|<|<=|>|>=|&&|\\|\\|)";
    public final static String PUNCTUATION_REGEX = "[;(),\\[\\]{}:]";
    public final static String IDENTIFIER_REGEX = "\\b[A-Za-z_][A-Za-z_0-9]*\\b";
    public final static String INTEGER_REGEX = "\\b\\d+\\b";
    public final static String FLOAT_REGEX = "\\b\\d+\\.\\d+\\b";
    public final static String STRING_REGEX = "^\"[^\"]*\"$";

}
