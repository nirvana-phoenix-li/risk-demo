package com.demo.risk.fieldcomplete;

import com.demo.risk.common.Result;
import com.demo.risk.fieldcomplete.model.ExecuteRequest;
import com.demo.risk.fieldcomplete.model.ScriptInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FieldCompleteControllerTest {

    private FieldCompleteController controller;

    @BeforeEach
    void setUp() {
        ScriptStore store = new ScriptStore();
        store.init();
        controller = new FieldCompleteController(store, new GroovyScriptEngine());
    }

    @Test
    void executeWithInlineScript() {
        ExecuteRequest req = new ExecuteRequest();
        req.setScriptContent("def r = [:]\nr.fullName = input.firstName + '-' + input.lastName\nreturn r");
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("firstName", "San");
        input.put("lastName", "Zhang");
        req.setInput(input);

        Result<Map<String, Object>> result = controller.execute(req);

        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getData().get("fullName")).isEqualTo("San-Zhang");
        assertThat(result.getData().get("firstName")).isEqualTo("San");
    }

    @Test
    void executeWithSavedDemoScript() {
        ExecuteRequest req = new ExecuteRequest();
        req.setScriptId("demo-mask");
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("phone", "13812345678");
        input.put("idCard", "110101199001011234");
        req.setInput(input);

        Result<Map<String, Object>> result = controller.execute(req);

        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getData().get("phone")).isEqualTo("138****5678");
        assertThat(result.getData().get("idCard")).isEqualTo("1101**********1234");
        assertThat(result.getData().get("idCardValid")).isEqualTo(true);
    }

    @Test
    void executeWithoutScriptFailsWith400() {
        ExecuteRequest req = new ExecuteRequest();
        req.setInput(new LinkedHashMap<>());

        Result<Map<String, Object>> result = controller.execute(req);

        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getData()).isNull();
    }

    @Test
    void executeWithUnknownScriptThrows() {
        ExecuteRequest req = new ExecuteRequest();
        req.setScriptId("ghost");
        req.setInput(new LinkedHashMap<>());

        assertThatThrownBy(() -> controller.execute(req))
                .isInstanceOf(GroovyScriptException.class)
                .hasMessageContaining("脚本不存在");
    }

    @Test
    void scriptCrudRoundTrip() {
        ScriptInfo info = new ScriptInfo();
        info.setScriptId("t1");
        info.setName("测试脚本");
        info.setContent("return [ok: true]");

        assertThat(controller.saveScript(info).getCode()).isEqualTo(0);
        assertThat(controller.getScript("t1").getData().getContent()).isEqualTo("return [ok: true]");
        assertThat(controller.listScripts().getData())
                .extracting(ScriptInfo::getScriptId)
                .contains("t1");

        assertThat(controller.deleteScript("t1").getCode()).isEqualTo(0);
        assertThat(controller.deleteScript("t1").getCode()).isEqualTo(404);
    }
}
