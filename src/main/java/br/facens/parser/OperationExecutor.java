package br.facens.parser;

import java.util.Stack;

public class OperationExecutor {
    private final Stack<String> operations = new Stack<>();

    public void pushOperator(String operator) {
        operations.push(operator);
    }

    public void clear() {
        operations.clear();
    }

    public String executeOperations() {
        while (!operations.isEmpty()) {
            if (operations.size() == 1) {
                return operations.pop();
            }
            String rightOperand = operations.size() < 3 ? null : operations.pop();
            String operator = operations.pop();
            String leftOperand = operations.pop();

            if (leftOperand.matches("-?\\d+") && (rightOperand == null || rightOperand.matches("-?\\d+"))) {
                int left = Integer.parseInt(leftOperand);
                int right = rightOperand != null ? Integer.parseInt(rightOperand) : 0;
                if (operator.matches("[-+*/%=^]")) {
                    int result = executeIntegerOperation(left, right, operator);
                    operations.push(String.valueOf(result));
                } else {
                    boolean result = executeIntegerComparison(left, right, operator);
                    operations.push(String.valueOf(result));
                }
            } else if (leftOperand.matches("-?\\d+(\\.\\d+)?") && (rightOperand == null || rightOperand.matches("-?\\d+(\\.\\d+)?"))) {
                float left = Float.parseFloat(leftOperand);
                float right = rightOperand != null ? Float.parseFloat(rightOperand) : 0;
                if (operator.matches("[-+*/%=^]")) {
                    float result = executeFloatOperation(left, right, operator);
                    operations.push(String.valueOf(result));
                } else {
                    boolean result = executeFloatComparison(left, right, operator);
                    operations.push(String.valueOf(result));
                }
            } else if (leftOperand.equals("true") || leftOperand.equals("false") || (rightOperand != null && (rightOperand.equals("true") || rightOperand.equals("false")))) {
                boolean left = Boolean.parseBoolean(leftOperand);
                if (rightOperand == null) {
                    operations.push(String.valueOf(!left));
                } else {
                    boolean right = Boolean.parseBoolean(rightOperand);
                    boolean result = executeBooleanOperation(left, right, operator);
                    operations.push(String.valueOf(result));
                }
            } else {
                String right = rightOperand != null ? rightOperand : "";
                String result = executeStringOperation(leftOperand, right, operator);
                operations.push(result);
            }
        }
        return null;
    }


    private int executeIntegerOperation(int left, int right, String operator) {
        switch (operator) {
            case "+":
                return left + right;
            case "-":
                return left - right;
            case "*":
                return left * right;
            case "/":
                return left / right;
            case "%":
                return left % right;
            default:
                return 0;
        }
    }

    private boolean executeIntegerComparison(int left, int right, String operator) {
        switch (operator) {
            case "==":
                return left == right;
            case "!=":
                return left != right;
            case "<":
                return left < right;
            case "<=":
                return left <= right;
            case ">":
                return left > right;
            case ">=":
                return left >= right;
            default:
                return false;
        }
    }

    private float executeFloatOperation(float left, float right, String operator) {
        switch (operator) {
            case "+":
                return left + right;
            case "-":
                return left - right;
            case "*":
                return left * right;
            case "/":
                return left / right;
            default:
                return 0;
        }
    }

    private boolean executeFloatComparison(float left, float right, String operator) {
        switch (operator) {
            case "==":
                return left == right;
            case "!=":
                return left != right;
            case "<":
                return left < right;
            case "<=":
                return left <= right;
            case ">":
                return left > right;
            case ">=":
                return left >= right;
            default:
                return false;
        }
    }

    private boolean executeBooleanOperation(boolean left, boolean right, String operator) {
        switch (operator) {
            case "==":
                return left == right;
            case "!=":
                return left != right;
            case "&&":
                return left && right;
            case "||":
                return left || right;
            default:
                return false;
        }
    }

    private String executeStringOperation(String left, String right, String operator) {
        switch (operator) {
            case "+":
                return left + right;
            case "==":
                return left.equals(right) ? "true" : "false";
            case "!=":
                return !left.equals(right) ? "true" : "false";
            default:
                return "";
        }
    }


}

