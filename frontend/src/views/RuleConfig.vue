<template>
  <el-card>
    <div class="rc-page-header">
      <div>
        <div class="ph-title">
          <span class="ph-badge">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 6h16M4 12h10M4 18h7"/></svg>
          </span>
          规则列表
        </div>
        <div class="ph-desc">基于 Aviator 表达式的风控规则管理，支持实时试跑验证</div>
      </div>
      <el-button type="primary" @click="openCreate">
        <svg style="margin-right:6px" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="M12 5v14M5 12h14"/></svg>
        新建规则
      </el-button>
    </div>

    <el-table :data="rules" stripe>
      <el-table-column prop="ruleId" label="规则ID" width="180">
        <template #default="{ row }">
          <code class="rc-code">{{ row.ruleId }}</code>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="名称" width="160">
        <template #default="{ row }">
          <span style="font-weight:600">{{ row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column label="条件表达式 (Aviator)">
        <template #default="{ row }">
          <code class="rc-code">{{ row.expression }}</code>
        </template>
      </el-table-column>
      <el-table-column prop="hitScore" label="命中分" width="90" />
      <el-table-column prop="hitAction" label="命中动作" width="110">
        <template #default="{ row }">
          <el-tag :type="actionTag(row.hitAction)" effect="light">{{ row.hitAction }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="启用" width="80">
        <template #default="{ row }">
          <el-switch v-model="row.enabled" @change="toggleEnabled(row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="openTry(row)">试跑</el-button>
          <el-button size="small" type="primary" plain @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" plain @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <div class="empty-state">
          <svg viewBox="0 0 24 24" width="44" height="44" fill="none" stroke="#cbd5e1" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 2 4 5v6c0 5 3.4 9.7 8 11 4.6-1.3 8-6 8-11V5l-8-3z"/>
            <path d="M12 8v4M12 16h.01"/>
          </svg>
          <p class="empty-title">暂无风控规则</p>
          <p class="empty-sub">点击右上角「新建规则」创建第一条规则</p>
        </div>
      </template>
    </el-table>

    <RuleEditDialog v-model="editVisible" :rule="editingRule" @saved="load" />

    <el-dialog v-model="tryVisible" :title="`试跑规则: ${tryRuleId}`" width="640">
      <el-input v-model="tryInputJson" type="textarea" :rows="8" placeholder='入参 JSON，如 {"amount": 15000, "scene": "WITHDRAW"}' />
      <el-alert v-if="tryError" :title="tryError" type="error" :closable="false" style="margin-top: 12px" />
      <el-descriptions v-if="tryResult" :column="2" border style="margin-top: 12px">
        <el-descriptions-item label="是否命中">
          <el-tag :type="tryResult.hit ? 'danger' : 'success'">{{ tryResult.hit ? '命中' : '未命中' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="得分">{{ tryResult.hitScore }}</el-descriptions-item>
        <el-descriptions-item label="动作">{{ tryResult.hitAction || '-' }}</el-descriptions-item>
        <el-descriptions-item label="表达式">
          <code class="rc-code">{{ tryResult.condition }}</code>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="tryVisible = false">关闭</el-button>
        <el-button type="primary" @click="runTry">执行</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listRules, saveRule, deleteRule, tryRule } from '../api'
import RuleEditDialog from '../components/RuleEditDialog.vue'

const rules = ref([])
const editVisible = ref(false)
const editingRule = ref(null)
const tryVisible = ref(false)
const tryRuleId = ref('')
const tryInputJson = ref('{}')
const tryResult = ref(null)
const tryError = ref('')

const actionTag = action => ({ REJECT: 'danger', REVIEW: 'warning', PASS: 'success' }[action] || 'info')

async function load() {
  rules.value = await listRules()
}

function openCreate() {
  editingRule.value = null
  editVisible.value = true
}

function openEdit(row) {
  editingRule.value = row
  editVisible.value = true
}

async function toggleEnabled(row) {
  await saveRule(row)
  ElMessage.success(`${row.ruleId} 已${row.enabled ? '启用' : '停用'}`)
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除规则 ${row.ruleId}？`, '提示', { type: 'warning' })
  await deleteRule(row.ruleId)
  ElMessage.success('已删除')
  await load()
}

function openTry(row) {
  tryRuleId.value = row.ruleId
  tryInputJson.value = '{\n  "amount": 15000,\n  "scene": "WITHDRAW",\n  "channel": "NORMAL"\n}'
  tryResult.value = null
  tryError.value = ''
  tryVisible.value = true
}

async function runTry() {
  tryResult.value = null
  tryError.value = ''
  let input
  try {
    input = JSON.parse(tryInputJson.value)
  } catch (e) {
    tryError.value = 'JSON 格式错误: ' + e.message
    return
  }
  try {
    tryResult.value = await tryRule(tryRuleId.value, input)
  } catch (e) {
    tryError.value = e.message
  }
}

onMounted(load)
</script>

<style scoped>
/* 页头图标徽章 */
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

/* 空状态 */
.empty-state {
  padding: 18px 0 26px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}
.empty-title { margin: 12px 0 0; font-size: 15px; font-weight: 600; color: #64748b; }
.empty-sub { margin: 4px 0 0; font-size: 13px; color: #94a3b8; }
</style>
