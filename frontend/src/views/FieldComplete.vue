<template>
  <el-card>
    <div class="fc-page-header">
      <div>
        <div class="ph-title">
          <span class="ph-badge">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M8 4H5a1 1 0 0 0-1 1v3"/>
              <path d="M16 4h3a1 1 0 0 1 1 1v3"/>
              <path d="M20 16v3a1 1 0 0 1-1 1h-3"/>
              <path d="M8 20H5a1 1 0 0 1-1-1v-3"/>
              <path d="M8 12h8M12 8v8"/>
            </svg>
          </span>
          字段补全
        </div>
        <div class="ph-desc">编写 Groovy 脚本解析补充入参 JSON，实时查看字段变化对比</div>
      </div>
    </div>

    <div class="console-bar">
      <div class="left">
        <el-select v-model="selectedScriptId" placeholder="从脚本库加载" style="width: 220px" clearable @change="loadScript">
          <el-option v-for="s in scripts" :key="s.scriptId" :value="s.scriptId" :label="`${s.name} (${s.scriptId})`" />
        </el-select>
        <el-button @click="openSave">保存到脚本库</el-button>
      </div>
      <div class="right">
        <el-button type="primary" @click="run">运行补全</el-button>
      </div>
    </div>

    <div class="editor-grid">
      <div class="editor-pane">
        <div class="pane-label">入参 JSON <span class="hint">脚本中通过 input.xxx 读取</span></div>
        <el-input v-model="inputJson" type="textarea" :rows="14" class="fc-editor" />
      </div>
      <div class="editor-pane">
        <div class="pane-label">Groovy 脚本 <span class="hint">return 一个 Map，字段将合并回结果</span></div>
        <el-input v-model="scriptContent" type="textarea" :rows="14" class="fc-editor" />
      </div>
    </div>

    <el-alert v-if="runError" :title="runError" type="error" :closable="false" class="run-error" />

    <template v-if="result">
      <div class="result-header">
        <div class="trace-title">字段变化对比</div>
        <div class="diff-summary">
          <el-tag type="success" effect="light">新增 {{ diffCount.added }}</el-tag>
          <el-tag type="warning" effect="light">修改 {{ diffCount.modified }}</el-tag>
          <el-tag type="danger" effect="light">删除 {{ diffCount.removed }}</el-tag>
          <el-tag type="info" effect="plain">不变 {{ diffCount.same }}</el-tag>
        </div>
      </div>

      <el-table :data="diffRows" border size="small" :row-class-name="rowClass">
        <el-table-column prop="field" label="字段" width="200">
          <template #default="{ row }">
            <code class="fc-code">{{ row.field }}</code>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTag(row.status)" effect="light">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="before" label="处理前">
          <template #default="{ row }">
            <span :class="{ muted: row.status === 'added' }">{{ row.before }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="after" label="处理后">
          <template #default="{ row }">
            <span :class="{ muted: row.status === 'removed' }">{{ row.after }}</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="trace-title">补全后 JSON</div>
      <pre class="result-json">{{ outputJson }}</pre>
    </template>
    <el-alert v-else-if="!runError" title="点击右上角「运行补全」查看字段变化" type="info" :closable="false" class="run-error" />

    <el-dialog v-model="saveVisible" title="保存到脚本库" width="420">
      <el-form label-width="90px">
        <el-form-item label="脚本ID">
          <el-input v-model="saveForm.scriptId" placeholder="如 my-enrich" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="saveForm.name" placeholder="如 我的补全脚本" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="saveVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listScripts, saveScript, executeFieldComplete } from '../api'

const scripts = ref([])
const selectedScriptId = ref('')
const inputJson = ref(JSON.stringify({ userId: 'u001', amount: 15000, phone: '13812345678', idCard: '110101199001011234' }, null, 2))
const scriptContent = ref(`def r = [:]

if (input.phone) {
    def p = input.phone as String
    r.phoneMasked = p.substring(0, 3) + '****' + p.substring(p.length() - 4)
}

r.highAmount = (input.amount != null && input.amount > 10000)
r.enrichTime = new java.text.SimpleDateFormat('yyyy-MM-dd HH:mm:ss').format(new Date())

return r`)
const runError = ref('')
const result = ref(null)
const outputJson = ref('')
const diffRows = ref([])
const saveVisible = ref(false)
const saveForm = ref({ scriptId: '', name: '' })

const diffCount = computed(() => {
  const c = { added: 0, modified: 0, removed: 0, same: 0 }
  diffRows.value.forEach(r => { c[r.status]++ })
  return c
})

function statusTag(status) {
  return { added: 'success', modified: 'warning', removed: 'danger', same: 'info' }[status]
}
function statusText(status) {
  return { added: '新增', modified: '修改', removed: '删除', same: '不变' }[status]
}
function rowClass({ row }) {
  return `fc-row-${row.status}`
}
function fmt(v) {
  if (v === undefined) return '—'
  if (v !== null && typeof v === 'object') return JSON.stringify(v)
  return String(v)
}
function deepEq(a, b) {
  return JSON.stringify(a) === JSON.stringify(b)
}
function buildDiff(before, after) {
  const rows = []
  const keys = new Set([...Object.keys(before), ...Object.keys(after)])
  keys.forEach(k => {
    const inB = Object.prototype.hasOwnProperty.call(before, k)
    const inA = Object.prototype.hasOwnProperty.call(after, k)
    let status
    if (!inB && inA) status = 'added'
    else if (inB && !inA) status = 'removed'
    else status = deepEq(before[k], after[k]) ? 'same' : 'modified'
    rows.push({ field: k, status, before: fmt(before[k]), after: fmt(after[k]) })
  })
  return rows
}

async function load() {
  scripts.value = await listScripts()
}

function loadScript(scriptId) {
  if (!scriptId) return
  const s = scripts.value.find(x => x.scriptId === scriptId)
  if (s) {
    scriptContent.value = s.content
    ElMessage.success(`已加载脚本: ${s.name}`)
  }
}

function openSave() {
  saveForm.value = { scriptId: selectedScriptId.value || '', name: '' }
  saveVisible.value = true
}

async function save() {
  if (!saveForm.value.scriptId) {
    ElMessage.warning('脚本ID不能为空')
    return
  }
  await saveScript({ scriptId: saveForm.value.scriptId, name: saveForm.value.name || saveForm.value.scriptId, content: scriptContent.value })
  ElMessage.success('已保存到脚本库')
  saveVisible.value = false
  selectedScriptId.value = saveForm.value.scriptId
  await load()
}

async function run() {
  runError.value = ''
  result.value = null
  let input
  try {
    input = JSON.parse(inputJson.value)
  } catch (e) {
    runError.value = '入参 JSON 格式错误: ' + e.message
    return
  }
  if (!scriptContent.value.trim()) {
    runError.value = '脚本内容不能为空'
    return
  }
  try {
    const output = await executeFieldComplete({ scriptContent: scriptContent.value, input })
    result.value = output
    outputJson.value = JSON.stringify(output, null, 2)
    diffRows.value = buildDiff(input, output)
  } catch (e) {
    runError.value = e.message
  }
}

onMounted(load)
</script>

<style scoped>
/* 页头 */
.fc-page-header { margin-bottom: 18px; }
.ph-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 17px;
  font-weight: 700;
  color: #1e293b;
}
.ph-badge {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #4f46e5;
  background: linear-gradient(135deg, #eef2ff 0%, #e0e7ff 100%);
  border: 1px solid #e0e7ff;
}
.ph-desc { margin-top: 6px; font-size: 13px; color: #94a3b8; }

/* 控制台操作条 */
.console-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  margin-bottom: 18px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
  border-radius: 12px;
}
.console-bar .left, .console-bar .right { display: flex; gap: 12px; align-items: center; }

/* 编辑器双栏 */
.editor-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.editor-pane { min-width: 0; }
.pane-label {
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 8px;
  display: flex;
  align-items: baseline;
  gap: 10px;
}
.pane-label .hint { font-size: 12px; font-weight: 400; color: #94a3b8; }
.fc-editor :deep(.el-textarea__inner) {
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
}

.run-error { margin-top: 16px; }

/* 结果区 */
.result-header { display: flex; align-items: center; justify-content: space-between; margin-top: 20px; }
.diff-summary { display: flex; gap: 8px; }
.trace-title {
  margin-top: 18px;
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 700;
  color: #475569;
  display: flex;
  align-items: center;
  gap: 8px;
}
.trace-title::before {
  content: '';
  width: 3px;
  height: 14px;
  border-radius: 2px;
  background: linear-gradient(180deg, #6366f1, #a78bfa);
}
.fc-code {
  background: #f1f5f9;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  color: #4f46e5;
}
.muted { color: #cbd5e1; }

:deep(.fc-row-added) { background: #f0fdf4; }
:deep(.fc-row-modified) { background: #fffbeb; }
:deep(.fc-row-removed) { background: #fef2f2; }

.result-json {
  margin: 0;
  padding: 14px 16px;
  background: #0f172a;
  color: #a5b4fc;
  border-radius: 10px;
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 12.5px;
  line-height: 1.6;
  max-height: 320px;
  overflow: auto;
}
</style>
