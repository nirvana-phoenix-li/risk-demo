package com.demo.risk.ruleflow;

import com.demo.risk.ruleflow.model.RuleInfo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 结构化规则（变量 + 操作符 + 阈值）→ Aviator 表达式。
 * 阈值支持固定值（自动识别数字/布尔/字符串）和变量引用。
 */
public final class ExpressionBuilder {

    public static final Set<String> OPERATORS =
            new HashSet<>(Arrays.asList("GT", "GE", "LT", "LE", "EQ", "NE", "CONTAINS"));

    private static final Set<String> NUMERIC_ONLY_OPS = new HashSet<>(Arrays.asList("GT", "GE", "LT", "LE"));

    private static final Pattern NUMBER_PATTERN = Pattern.compile("^-?\\d+(\\.\\d+)?$");
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    private static final Map<String, String> SYMBOLS = new HashMap<>();

    static {
        SYMBOLS.put("GT", ">");
        SYMBOLS.put("GE", ">=");
        SYMBOLS.put("LT", "<");
        SYMBOLS.put("LE", "<=");
        SYMBOLS.put("EQ", "==");
        SYMBOLS.put("NE", "!=");
    }

    private ExpressionBuilder() {
    }

    public static String build(RuleInfo rule) {
        String left = rule.getLeftVar();
        String op = rule.getOperator();

        requireIdentifier(left, "leftVar");
        if (!OPERATORS.contains(op)) {
            throw new RuleFlowException("不支持的操作符: " + op);
        }
        if ("CONTAINS".equals(op)) {
            if (!"VALUE".equals(rule.getThresholdType())) {
                throw new RuleFlowException("CONTAINS 操作符只支持固定值阈值");
            }
            return String.format("contains(%s, %s)", left, quote(rule.getThresholdValue()));
        }

        String right;
        if ("VAR".equals(rule.getThresholdType())) {
            requireIdentifier(rule.getThresholdVar(), "thresholdVar");
            // Aviator 把 nil 当作最小值参与比较，阈值变量缺失时会误命中，需加 nil 保护
            return "(" + rule.getThresholdVar() + " != nil) && (" + left + " " + SYMBOLS.get(op) + " "
                    + rule.getThresholdVar() + ")";
        }
        right = literal(rule.getThresholdValue());
        return left + " " + SYMBOLS.get(op) + " " + right;
    }

    public static void requireIdentifier(String value, String fieldName) {
        if (value == null || !IDENTIFIER_PATTERN.matcher(value).matches()) {
            throw new RuleFlowException(fieldName + " 必须是合法变量编码: " + value);
        }
    }

    public static boolean isNumericOp(String op) {
        return NUMERIC_ONLY_OPS.contains(op);
    }

    /** 数字和布尔原样输出，其余按字符串处理（去除引号防注入） */
    private static String literal(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new RuleFlowException("阈值不能为空");
        }
        String v = value.trim();
        if (NUMBER_PATTERN.matcher(v).matches() || "true".equals(v) || "false".equals(v)) {
            return v;
        }
        return quote(v);
    }

    private static String quote(String value) {
        return "'" + value.replace("'", "").replace("\"", "") + "'";
    }
}
