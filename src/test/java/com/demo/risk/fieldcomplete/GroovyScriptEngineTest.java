package com.demo.risk.fieldcomplete;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GroovyScriptEngineTest {

    private GroovyScriptEngine engine;

    @BeforeEach
    void setUp() {
        engine = new GroovyScriptEngine();
    }

    @Test
    void mergesReturnedFieldsIntoInput() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("userId", "u001");

        Map<String, Object> result = engine.execute("k1",
                "def r = [:]\nr.userId = input.userId.toUpperCase()\nr.tag = 'vip'\nreturn r", input);

        assertThat(result.get("userId")).isEqualTo("U001");
        assertThat(result.get("tag")).isEqualTo("vip");
    }

    @Test
    void keepsOriginalFieldsWhenScriptOnlyAdds() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("amount", 500);

        Map<String, Object> result = engine.execute("k2", "return [extra: 1]", input);

        assertThat(result.get("amount")).isEqualTo(500);
        assertThat(result.get("extra")).isEqualTo(1);
    }

    @Test
    void sameCacheKeyReusesCompiledScript() {
        Map<String, Object> first = engine.execute("k3", "return [n: input.x * 2]", inputOf("x", 21));
        Map<String, Object> second = engine.execute("k3", "return [n: input.x * 2]", inputOf("x", 5));

        assertThat(first.get("n")).isEqualTo(42);
        assertThat(second.get("n")).isEqualTo(10);
    }

    @Test
    void syntaxErrorThrows() {
        assertThatThrownBy(() -> engine.execute("k4", "def r = [:]; r.a = >>> broken", new LinkedHashMap<>()))
                .isInstanceOf(GroovyScriptException.class)
                .hasMessageContaining("脚本语法错误");
    }

    @Test
    void runtimeErrorThrows() {
        assertThatThrownBy(() -> engine.execute("k5", "return input.missing.deep.field", new LinkedHashMap<>()))
                .isInstanceOf(GroovyScriptException.class)
                .hasMessageContaining("脚本运行失败");
    }

    @Test
    void nonMapReturnDoesNotChangeInput() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("a", 1);

        Map<String, Object> result = engine.execute("k6", "return 42", input);

        assertThat(result.get("a")).isEqualTo(1);
        assertThat(result).hasSize(1);
    }

    private Map<String, Object> inputOf(String k, Object v) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(k, v);
        return m;
    }
}
