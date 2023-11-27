package br.facens.parser.symbol;

import java.util.List;

public class FunctionSymbol extends Symbol {
    private final List<String> parameterTypes;
    private boolean hasReturn;
    private boolean isArray;

    public FunctionSymbol(String identifier, String returnType, List<String> parameterTypes) {
        super(identifier, returnType, true);
        this.parameterTypes = parameterTypes;
        this.hasReturn = false;
    }

    public List<String> getParameterTypes() {
        return parameterTypes;
    }

    public void setHasReturn(boolean hasReturn) {
        this.hasReturn = hasReturn;
    }

    public boolean hasReturn() {
        return hasReturn;
    }

    public void setIsArray(boolean isArray) {
        this.isArray = isArray;
    }

    public boolean isArray() {
        return isArray;
    }
}