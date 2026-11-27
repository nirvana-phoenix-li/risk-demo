<template>
  <el-card>
    <div class="rc-page-header">
      <div>
        <div class="ph-title">
          <span class="ph-badge">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="6" height="6" rx="1.5"/><rect x="15" y="15" width="6" height="6" rx="1.5"/><path d="M9 6h6a2 2 0 0 1 2 2v4"/></svg>
          </span>
          流程编排
        </div>
        <div class="ph-desc">将规则与字段补全脚本编排为执行链路，实时运行查看决策轨迹</div>
      </div>
    </div>

    <div class="console-bar">
      <div class="left">
        <el-select v-model="currentFlowId" placeholder="选择流程" style="width: 240px" @change="loadFlow">
          <el-option v-for="f in flows" :key="f.flowId" :value="f.flowId" :label="`${f.name} (${f.flowId})`" />
        </el-select>
        <el-button @click="openCreate">新建流程</el-button>
        <el-button type="danger" plain :disabled="!currentFlowId" @click="removeFlow">删除流程</el-button>
        <el-divider direction="vertical" />
        <el-button type="primary" plain @click="openRuleCreate">+ 新建规则</el-button>
      </div>
      <div class="right">
        <el-button type="success" :disabled="!currentFlowId" @click="openRun">运行</el-button>
        <el-button type="primary" :disabled="!currentFlowId" @click="saveCurrent">保存编排</el-button>
      </div>
    </div>

    <el-alert v-if="!currentFlowId" type="info" :closable="false" class="empty-tip">
      <template #title>
        <span>请先在上方<strong>选择</strong>或<strong>新建</strong>一个风控流程</span>
      </template>
    </el-alert>

    <template v-else>
      <el-table :data="nodes" stripe>
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="nodeId" label="节点ID" width="150" />
        <el-table-column label="类型" width="130">
          <template #default="{ row }">
            <el-tag :type="row.type === 'RULE' ? 'warning' : 'primary'">
              {{ row.type === 'RULE' ? '规则判断' : '字段补全' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="引用对象" min-width="200">
          <template #default="{ row }">
            <el-select v-model="row.refId" size="small" style="width: 100%">
              <el-option v-for="o in refOptions(row.type)" :key="o.value" :value="o.value" :label="o.label" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="命中动作" width="160">
          <template #default="{ row }">
            <el-select v-model="row.onHit" size="small" :disabled="row.type !== 'RULE'" style="width: 130px">
              <el-option label="命中继续" value="CONTINUE" />
              <el-option label="命中短路" value="BREAK" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250">
          <template #default="{ row, $index }">
            <el-button v-if="row.type === 'RULE'" size="small" type="primary" plain @click="openRuleEdit(row)">编辑规则</el-button>
            <el-button size="small" :disabled="$index === 0" @click="move($index, -1)">上移</el-button>
            <el-button size="small" :disabled="$index === nodes.length - 1" @click="move($index, 1)">下移</el-button>
            <el-button size="small" type="danger" @click="nodes.splice($index, 1)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="add-row">
        <el-button @click="addNode('RULE')">+ 添加规则节点</el-button>
        <el-button @click="addNode('ENRICH')">+ 添加补全节点</el-button>
      </div>
    </template>
    <el-dialog v-model="createVisible" title="新建流程" width="420">
      <el-form label-width="90px">
        <el-form-item label="流程ID">
          <el-input v-model="newFlowId" placeholder="如 flow-loan" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="newFlowName" placeholder="如 贷款申请风控" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="create">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="runVisible" title="运行流程" width="820">
      <el-input v-model="runInputJson" type="textarea" :rows="8"
        placeholder='入参 JSON，如 {"userId": "u001", "amount": 15000, "scene": "LOGIN", "channel": "NORMAL"}' />
      <el-alert v-if="runError" :title="runError" type="error" :closable="false" style="margin-top: 12px" />

      <template v-if="runResult">
        <div class="decision-card" :class="decisionClass(runResult.decision)">
          <div class="dc-left">
            <div class="dc-label">最终决策</div>
            <div class="dc-value">{{ runResult.decision }}</div>
          </div>
          <div class="dc-divider"></div>
          <div class="dc-right">
            <div class="dc-label">风险总分</div>
            <div class="dc-score">{{ runResult.totalScore }}</div>
          </div>
        </div>
        <div class="trace-title">执行轨迹</div>
        <el-table :data="runResult.trace" size="small">
          <el-table-column prop="nodeId" label="节点" width="110" />
          <el-table-column prop="nodeName" label="名称" width="160" />
          <el-table-column prop="type" label="类型" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="row.type === 'RULE' ? 'warning' : 'primary'">{{ row.type }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="命中" width="80">
            <template #default="{ row }">
              <el-tag v-if="row.hit === true" size="small" type="danger">命中</el-tag>
              <el-tag v-else-if="row.hit === false" size="small" type="success">未命中</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="scoreDelta" label="得分" width="70" />
          <el-table-column prop="elapsedMs" label="耗时(ms)" width="90" />
          <el-table-column prop="detail" label="明细" />
        </el-table>
      </template>

      <template #footer>
        <el-button @click="runVisible = false">关闭</el-button>
        <el-button type="primary" @click="run">执行</el-button>
      </template>
    </el-dialog>

    <RuleEditDialog v-model="ruleEditVisible" :rule="editingRule" @saved="loadRules" />
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listFlows, getFlow, saveFlow, deleteFlow, runFlow, listRules, listScripts } from '../api'
import RuleEditDialog from '../components/RuleEditDialog.vue'

const flows = ref([])
const rules = ref([])
const scripts = ref([])
const currentFlowId = ref('')
const currentFlowName = ref('')
const nodes = ref([])
const createVisible = ref(false)
const newFlowId = ref('')
const newFlowName = ref('')
const runVisible = ref(false)
const runInputJson = ref('{}')
const runResult = ref(null)
const runError = ref('')
const ruleEditVisible = ref(false)
const editingRule = ref(null)

async function loadRules() {
  rules.value = await listRules()
}

function openRuleEdit(row) {
  editingRule.value = rules.value.find(r => r.ruleId === row.refId) || { ruleId: row.refId, name: '' }
  ruleEditVisible.value = true
}

function openRuleCreate() {
  editingRule.value = null
  ruleEditVisible.value = true
}

const decisionTag = d => ({ REJECT: 'danger', REVIEW: 'warning', PASS: 'success' }[d] || 'info')
const decisionClass = d => ({ REJECT: 'is-reject', REVIEW: 'is-review', PASS: 'is-pass' }[d] || 'is-info')

function refOptions(type) {
  if (type === 'RULE') {
    return rules.value.map(r => ({ value: r.ruleId, label: `${r.name} (${r.ruleId})` }))
  }
  return scripts.value.map(s => ({ value: s.scriptId, label: `${s.name} (${s.scriptId})` }))
}

async function load() {
  flows.value = await listFlows()
  rules.value = await listRules()
  scripts.value = await listScripts()
}

async function loadFlow() {
  if (!currentFlowId.value) return
  const flow = await getFlow(currentFlowId.value)
  currentFlowName.value = flow.name
  nodes.value = flow.nodes.map(n => ({ ...n }))
}

function openCreate() {
  newFlowId.value = ''
  newFlowName.value = ''
  createVisible.value = true
}

async function create() {
  if (!newFlowId.value) {
    ElMessage.warning('流程ID不能为空')
    return
  }
  await saveFlow({ flowId: newFlowId.value, name: newFlowName.value || newFlowId.value, nodes: [] })
  createVisible.value = false
  await load()
  currentFlowId.value = newFlowId.value
  await loadFlow()
  ElMessage.success('已创建，请添加节点后保存')
}

function addNode(type) {
  nodes.value.push({ nodeId: `n${nodes.value.length + 1}`, type, refId: '', onHit: 'CONTINUE' })
}

function move(index, delta) {
  const target = index + delta
  const arr = nodes.value
  ;[arr[index], arr[target]] = [arr[target], arr[index]]
}

async function saveCurrent() {
  if (nodes.value.some(n => !n.refId)) {
    ElMessage.warning('存在未选择引用对象的节点')
    return
  }
  await saveFlow({ flowId: currentFlowId.value, name: currentFlowName.value, nodes: nodes.value })
  ElMessage.success('编排已保存')
  await load()
}

async function removeFlow() {
  await ElMessageBox.confirm(`确认删除流程 ${currentFlowId.value}？`, '提示', { type: 'warning' })
  await deleteFlow(currentFlowId.value)
  currentFlowId.value = ''
  nodes.value = []
  await load()
  ElMessage.success('已删除')
}

function openRun() {
  runInputJson.value = JSON.stringify(
    { userId: 'u001', amount: 15000, scene: 'LOGIN', channel: 'NORMAL' }, null, 2)
  runResult.value = null
  runError.value = ''
  runVisible.value = true
}

async function run() {
  runResult.value = null
  runError.value = ''
  let input
  try {
    input = JSON.parse(runInputJson.value)
  } catch (e) {
    runError.value = 'JSON 格式错误: ' + e.message
    return
  }
  try {
    runResult.value = await runFlow(currentFlowId.value, input)
  } catch (e) {
    runError.value = e.message
  }
}

onMounted(load)
</script>

<style scoped>
/* 页头徽章 */
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
.console-bar .left,
.console-bar .right { display: flex; gap: 12px; align-items: center; }

.empty-tip { border-radius: 12px; }

.add-row {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed #e2e8f0;
}

/* 决策结果卡片 */
.decision-card {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-top: 18px;
  padding: 18px 26px;
  border-radius: 14px;
  color: #fff;
  background: linear-gradient(135deg, #64748b 0%, #475569 100%);
  box-shadow: 0 12px 28px -12px rgba(71, 85, 105, 0.55);
}
.decision-card.is-reject {
  background: linear-gradient(135deg, #ef4444 0%, #b91c1c 100%);
  box-shadow: 0 12px 28px -12px rgba(220, 38, 38, 0.6);
}
.decision-card.is-review {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  box-shadow: 0 12px 28px -12px rgba(217, 119, 6, 0.6);
}
.decision-card.is-pass {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  box-shadow: 0 12px 28px -12px rgba(5, 150, 105, 0.6);
}
.decision-card .dc-label {
  font-size: 12px;
  opacity: 0.85;
  letter-spacing: 0.08em;
}
.decision-card .dc-value {
  font-size: 30px;
  font-weight: 800;
  letter-spacing: 0.04em;
  margin-top: 2px;
}
.decision-card .dc-score {
  font-size: 30px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  margin-top: 2px;
}
.dc-divider {
  width: 1px;
  height: 40px;
  background: rgba(255, 255, 255, 0.35);
}

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
</style>
