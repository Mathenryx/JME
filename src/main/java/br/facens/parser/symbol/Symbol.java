package br.facens.parser.symbol;

public class Symbol {
    private final String identifier;
    private final String type;
    private boolean isInitialized;

    public Symbol(String identifier, String type) {
        this.identifier = identifier;
        this.type = type;
        this.isInitialized = false;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getType() {
        return type;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public void setInitialized(boolean isInitialized) {
        this.isInitialized = isInitialized;
    }
}

