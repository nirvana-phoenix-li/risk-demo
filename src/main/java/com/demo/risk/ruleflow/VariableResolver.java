package com.demo.risk.ruleflow;

import com.demo.risk.ruleflow.model.VariableInfo;
import com.googlecode.aviator.AviatorEvaluator;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 把变量解析进执行环境：先解析全部 INPUT 型变量（从入参按路径取值），
 * 再按 varId 顺序解析 EXPR 型变量（可引用入参字段和 INPUT 变量）。
 */
@Component
public class VariableResolver {

    private final VarStore varStore;

    public VariableResolver(VarStore varStore) {
        this.varStore = varStore;
    }

    public Map<String, Object> resolve(Map<String, Object> input) {
        Map<String, Object> env = new LinkedHashMap<>(input);

        for (VariableInfo v : varStore.list()) {
            if ("INPUT".equals(v.getVarType())) {
                env.put(v.getVarId(), navigate(input, v.getSource()));
            }
        }
        for (VariableInfo v : varStore.list()) {
            if ("EXPR".equals(v.getVarType())) {
                Object value;
                try {
                    value = AviatorEvaluator.execute(v.getSource(), env, true);
                } catch (Exception e) {
                    // 引用字段缺失等运行期错误按未解析处理（语法错误已在保存时拦截）
                    value = null;
                }
                env.put(v.getVarId(), value);
            }
        }
        return env;
    }

    /** 支持 a.b.c 形式的嵌套路径取值，缺失返回 null */
    private Object navigate(Map<String, Object> input, String path) {
        Object current = input;
        for (String key : path.split("\\.")) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map<?, ?>) current).get(key);
            if (current == null) {
                return null;
            }
        }
        return current;
    }
}
