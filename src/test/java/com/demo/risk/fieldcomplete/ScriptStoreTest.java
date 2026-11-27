package com.demo.risk.fieldcomplete;

import com.demo.risk.fieldcomplete.model.ScriptInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScriptStoreTest {

    private ScriptStore store;

    @BeforeEach
    void setUp() {
        store = new ScriptStore();
        store.init();
    }

    @Test
    void preloadsDemoScripts() {
        assertThat(store.get("demo-basic")).isNotNull();
        assertThat(store.get("demo-mask")).isNotNull();
        assertThat(store.get("demo-basic").getContent()).contains("def result");
        assertThat(store.get("demo-mask").getContent()).contains("idCard");
    }

    @Test
    void saveAndGetRoundTrip() {
        ScriptInfo info = new ScriptInfo();
        info.setScriptId("t1");
        info.setContent("return [ok: true]");

        store.save(info);

        assertThat(store.get("t1")).isSameAs(info);
    }

    @Test
    void saveOverwritesSameId() {
        store.save(script("t2", "return 1"));
        store.save(script("t2", "return 2"));

        assertThat(store.get("t2").getContent()).isEqualTo("return 2");
    }

    @Test
    void requireThrowsForMissingScript() {
        assertThatThrownBy(() -> store.require("ghost"))
                .isInstanceOf(GroovyScriptException.class)
                .hasMessageContaining("脚本不存在");
    }

    @Test
    void deleteRemovesScript() {
        store.save(script("t3", "return 3"));

        assertThat(store.delete("t3")).isNotNull();
        assertThat(store.get("t3")).isNull();
        assertThat(store.delete("t3")).isNull();
    }

    private ScriptInfo script(String id, String content) {
        ScriptInfo info = new ScriptInfo();
        info.setScriptId(id);
        info.setContent(content);
        return info;
    }
}
