package br.facens.parser.symbol;

import java.util.ArrayList;
import java.util.List;

public class ArraySymbol extends Symbol {
    private final List<String> values;

    public ArraySymbol(String identifier, String returnType) {
        super(identifier, returnType, false);
        this.values = new ArrayList<>();
    }

    public List<String> getValues() {
        return values;
    }

    public void addValue(String value) {
        this.values.add(value);
    }

    public void setValues(List<String> values) {
        this.values.addAll(values);
    }

    public void setArrayValue(int index, String value) {
        values.set(index, value);
    }

}