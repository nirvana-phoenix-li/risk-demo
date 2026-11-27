package com.demo.risk.ruleflow;

import com.demo.risk.ruleflow.model.VariableInfo;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class VarStore {

    private static final Set<String> VALID_VAR_TYPES =
            new java.util.HashSet<>(Arrays.asList("INPUT", "EXPR"));
    private static final Set<String> VALID_VALUE_TYPES =
            new java.util.HashSet<>(Arrays.asList("NUMBER", "STRING", "BOOLEAN"));

    private final Map<String, VariableInfo> vars = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        save(var("amount", "交易金额", "INPUT", "amount", "NUMBER", "入参中的交易金额"));
        save(var("scene", "业务场景", "INPUT", "scene", "STRING", "LOGIN/PAYMENT/WITHDRAW"));
        save(var("bigAmountThreshold", "大额阈值(场景相关)", "INPUT", "thresholds.bigAmount", "NUMBER",
                "从入参 thresholds.bigAmount 读取，不同场景可传不同阈值"));
        save(var("amountDouble", "双倍金额", "EXPR", "amount * 2", "NUMBER", "演示计算型变量"));
    }

    private VariableInfo var(String id, String name, String type, String source, String valueType, String desc) {
        VariableInfo info = new VariableInfo();
        info.setVarId(id);
        info.setName(name);
        info.setVarType(type);
        info.setSource(source);
        info.setValueType(valueType);
        info.setDescription(desc);
        return info;
    }

    public void save(VariableInfo info) {
        if (!VALID_VAR_TYPES.contains(info.getVarType())) {
            throw new RuleFlowException("varType 必须是 INPUT/EXPR: " + info.getVarType());
        }
        if (!VALID_VALUE_TYPES.contains(info.getValueType())) {
            throw new RuleFlowException("valueType 必须是 NUMBER/STRING/BOOLEAN: " + info.getValueType());
        }
        ExpressionBuilder.requireIdentifier(info.getVarId(), "varId");
        if ("EXPR".equals(info.getVarType())) {
            try {
                com.googlecode.aviator.AviatorEvaluator.validate(info.getSource());
            } catch (Exception e) {
                throw new RuleFlowException("变量[" + info.getName() + "]表达式语法错误: " + e.getMessage());
            }
        }
        vars.put(info.getVarId(), info);
    }

    public VariableInfo get(String varId) {
        return vars.get(varId);
    }

    public VariableInfo require(String varId) {
        VariableInfo info = vars.get(varId);
        if (info == null) {
            throw new RuleFlowException("变量不存在: " + varId);
        }
        return info;
    }

    public VariableInfo delete(String varId) {
        return vars.remove(varId);
    }

    /** 按 varId 排序返回，保证 EXPR 变量的解析顺序稳定 */
    public java.util.List<VariableInfo> list() {
        return vars.values().stream()
                .sorted(Comparator.comparing(VariableInfo::getVarId))
                .collect(Collectors.toList());
    }
}
