package br.facens.parser.symbol;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class SymbolTable {
    private final Stack<Map<String, Symbol>> scopes = new Stack<>();

    public void pushScope() {
        scopes.push(new HashMap<>());
    }

    public void popScope() {
        scopes.pop();
    }

    public void addSymbol(String identifier, Symbol symbol) {
        scopes.peek().put(identifier, symbol);
    }

    public Symbol getVariableSymbol(String identifier) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Map<String, Symbol> scope = scopes.get(i);
            if (scope.containsKey(identifier) && !scope.get(identifier).isFunction()) {
                return scope.get(identifier);
            }
        }
        return null;
    }

    public Symbol getFunctionSymbol(String identifier) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Map<String, Symbol> scope = scopes.get(i);
            if (scope.containsKey(identifier) && scope.get(identifier).isFunction()) {
                return scope.get(identifier);
            }
        }
        return null;
    }

    public boolean hasSymbol(String identifier) {
        return getVariableSymbol(identifier) != null;
    }

    public boolean hasSymbolCurrentScope(String identifier) {
        Map<String, Symbol> currentScope = scopes.get(scopes.size() - 1);
        return currentScope.get(identifier) != null;
    }
}
