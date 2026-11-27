<template>
  <el-card>
    <div class="vm-page-header">
      <div>
        <div class="ph-title">
          <span class="ph-badge">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M4 7V5h3M20 7V5h-3M4 17v2h3M20 17v2h-3"/>
              <path d="M9 10a2 2 0 0 1 4 0v4a2 2 0 0 0 4 0"/>
            </svg>
          </span>
          变量管理
        </div>
        <div class="ph-desc">INPUT 型从入参按路径取值（支持 a.b.c 嵌套），EXPR 型按 Aviator 表达式计算衍生值</div>
      </div>
      <el-button type="primary" @click="openCreate">新建变量</el-button>
    </div>

    <el-table :data="vars" stripe>
      <el-table-column prop="varId" label="变量编码" width="200">
        <template #default="{ row }">
          <code class="vm-code">{{ row.varId }}</code>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="名称" width="180" />
      <el-table-column label="类型" width="110">
        <template #default="{ row }">
          <el-tag :type="row.varType === 'EXPR' ? 'warning' : 'primary'" effect="light">
            {{ row.varType === 'EXPR' ? '计算 EXPR' : '入参 INPUT' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="取值来源">
        <template #default="{ row }">
          <code class="vm-code">{{ row.source }}</code>
        </template>
      </el-table-column>
      <el-table-column prop="valueType" label="值类型" width="100" />
      <el-table-column prop="description" label="说明" min-width="160" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button size="small" type="primary" plain @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" plain @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <div class="empty-state">
          <p class="empty-title">暂无变量</p>
          <p class="empty-sub">点击右上角「新建变量」创建第一个变量</p>
        </div>
      </template>
    </el-table>

    <el-dialog v-model="editVisible" :title="isCreate ? '新建变量' : '编辑变量'" width="560">
      <el-form :model="form" label-width="110px">
        <el-form-item label="变量编码">
          <el-input v-model="form.varId" :disabled="!isCreate" placeholder="仅字母/数字/下划线，如 bigAmountThreshold" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="如 大额阈值(场景相关)" />
        </el-form-item>
        <el-form-item label="变量类型">
          <el-radio-group v-model="form.varType">
            <el-radio-button label="INPUT">入参取值</el-radio-button>
            <el-radio-button label="EXPR">表达式计算</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="form.varType === 'EXPR' ? '计算表达式' : '取值路径'">
          <el-input v-model="form.source" type="textarea" :rows="2"
            :placeholder="form.varType === 'EXPR'
              ? &quot;Aviator 表达式，如 amount * 2 或 scene == 'WITHDRAW' ? 1 : 0&quot;
              : '入参属性路径，支持嵌套，如 amount 或 thresholds.bigAmount'" />
        </el-form-item>
        <el-form-item label="值类型">
          <el-select v-model="form.valueType" style="width: 160px">
            <el-option label="NUMBER 数值" value="NUMBER" />
            <el-option label="STRING 字符串" value="STRING" />
            <el-option label="BOOLEAN 布尔" value="BOOLEAN" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" placeholder="变量的业务含义" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listVars, saveVar, deleteVar } from '../api'

const vars = ref([])
const editVisible = ref(false)
const isCreate = ref(false)
const form = ref({})

async function load() {
  vars.value = await listVars()
}

function openCreate() {
  isCreate.value = true
  form.value = { varId: '', name: '', varType: 'INPUT', source: '', valueType: 'NUMBER', description: '' }
  editVisible.value = true
}

function openEdit(row) {
  isCreate.value = false
  form.value = { ...row }
  editVisible.value = true
}

async function save() {
  const f = form.value
  if (!f.varId || !f.source) {
    ElMessage.warning('变量编码和取值来源不能为空')
    return
  }
  await saveVar(f)
  ElMessage.success('已保存')
  editVisible.value = false
  await load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除变量 ${row.varId}？引用它的结构化规则将无法命中。`, '提示', { type: 'warning' })
  await deleteVar(row.varId)
  ElMessage.success('已删除')
  await load()
}

onMounted(load)
</script>

<style scoped>
.vm-page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 18px;
}
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
.vm-code {
  background: #f1f5f9;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  color: #4f46e5;
}
.empty-title { margin: 12px 0 0; font-size: 15px; font-weight: 600; color: #64748b; }
.empty-sub { margin: 4px 0 0; font-size: 13px; color: #94a3b8; }
</style>
