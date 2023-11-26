package br.facens.parser.symbol;

public class Symbol {
    private final String identifier;
    private final String type;
    private String value;
    private boolean isInitialized;
    private final boolean isFunction;

    public Symbol(String identifier, String type, boolean isFunction) {
        this.identifier = identifier;
        this.type = type;
        this.value = null;
        this.isInitialized = false;
        this.isFunction = isFunction;
    }

    public Symbol(String identifier, String type) {
        this.identifier = identifier;
        this.type = type;
        this.isInitialized = false;
        this.isFunction = false;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getType() {
        return type;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public void setInitialized(boolean isInitialized) {
        this.isInitialized = isInitialized;
    }

    public boolean isFunction() {
        return isFunction;
    }
}

