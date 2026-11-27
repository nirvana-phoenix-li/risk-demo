<template>
  <el-dialog :model-value="modelValue" :title="isCreate ? '新建规则' : '编辑规则: ' + form.ruleId" width="640"
    @update:model-value="v => emit('update:modelValue', v)" @open="onOpen">
    <el-form :model="form" label-width="110px">
      <el-form-item label="规则ID">
        <el-input v-model="form.ruleId" :disabled="!isCreate" placeholder="如 rule-night" />
      </el-form-item>
      <el-form-item label="名称">
        <el-input v-model="form.name" placeholder="如 夜间交易" />
      </el-form-item>
      <el-form-item label="规则模式">
        <el-radio-group v-model="form.ruleType">
          <el-radio-button label="EXPR">表达式</el-radio-button>
          <el-radio-button label="STRUCT">变量+操作符+阈值</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="form.ruleType === 'EXPR'" label="条件表达式">
        <el-input v-model="form.condition" type="textarea" :rows="3"
          placeholder="Aviator 表达式，如&#10;amount > 10000 && scene == 'WITHDRAW'" />
      </el-form-item>
      <template v-else>
        <el-form-item label="左变量">
          <el-select v-model="form.leftVar" filterable allow-create placeholder="选择或输入变量编码" style="width: 100%">
            <el-option v-for="v in vars" :key="v.varId" :value="v.varId" :label="`${v.name} (${v.varId})`" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作符">
          <el-select v-model="form.operator" style="width: 200px" placeholder="选择操作符">
            <el-option v-for="o in operatorOptions" :key="o.value" :value="o.value" :label="o.label" />
          </el-select>
        </el-form-item>
        <el-form-item label="阈值类型">
          <el-radio-group v-model="form.thresholdType">
            <el-radio-button label="VALUE">固定值</el-radio-button>
            <el-radio-button label="VAR">引用变量</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.thresholdType === 'VALUE'" label="阈值">
          <el-input v-model="form.thresholdValue" placeholder="数字直接填，如 10000；字符串如 WITHDRAW；布尔 true/false" />
        </el-form-item>
        <el-form-item v-else label="阈值变量">
          <el-select v-model="form.thresholdVar" filterable placeholder="选择数值型变量" style="width: 100%">
            <el-option v-for="v in numericVars" :key="v.varId" :value="v.varId" :label="`${v.name} (${v.varId})`" />
          </el-select>
        </el-form-item>
      </template>
      <el-form-item label="命中分数">
        <el-input-number v-model="form.hitScore" :min="0" :max="100" />
      </el-form-item>
      <el-form-item label="命中动作">
        <el-select v-model="form.hitAction" style="width: 160px">
          <el-option label="PASS 放行" value="PASS" />
          <el-option label="REVIEW 人工审核" value="REVIEW" />
          <el-option label="REJECT 拒绝" value="REJECT" />
        </el-select>
      </el-form-item>
      <el-form-item label="启用">
        <el-switch v-model="form.enabled" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { saveRule, listVars } from '../api'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  /** null = 新建；对象 = 编辑该规则 */
  rule: { type: Object, default: null }
})
const emit = defineEmits(['update:modelValue', 'saved'])

const vars = ref([])
const isCreate = ref(true)
const form = ref({})

const OP_LABELS = { GT: '大于 >', GE: '大于等于 >=', LT: '小于 <', LE: '小于等于 <=', EQ: '等于 ==', NE: '不等于 !=', CONTAINS: '包含 contains' }
const OPS_BY_TYPE = {
  NUMBER: ['GT', 'GE', 'LT', 'LE', 'EQ', 'NE'],
  STRING: ['EQ', 'NE', 'CONTAINS'],
  BOOLEAN: ['EQ', 'NE']
}

const numericVars = computed(() => vars.value.filter(v => v.valueType === 'NUMBER'))

const operatorOptions = computed(() => {
  const v = vars.value.find(x => x.varId === form.value.leftVar)
  const ops = v ? (OPS_BY_TYPE[v.valueType] || Object.keys(OP_LABELS)) : Object.keys(OP_LABELS)
  return ops.map(o => ({ value: o, label: OP_LABELS[o] }))
})

watch(() => form.value.leftVar, () => {
  if (form.value.operator && !operatorOptions.value.some(o => o.value === form.value.operator)) {
    form.value.operator = ''
  }
})

async function onOpen() {
  try {
    vars.value = await listVars()
  } catch (e) { /* 变量加载失败不阻塞表单 */ }
  isCreate.value = !props.rule
  form.value = props.rule
    ? { ...props.rule, ruleType: props.rule.ruleType || 'EXPR', thresholdType: props.rule.thresholdType || 'VALUE' }
    : {
        ruleId: '', name: '', ruleType: 'EXPR', condition: '',
        leftVar: '', operator: '', thresholdType: 'VALUE', thresholdValue: '', thresholdVar: '',
        hitScore: 10, hitAction: 'REVIEW', enabled: true
      }
}

async function save() {
  const f = form.value
  if (!f.ruleId) {
    ElMessage.warning('规则ID 不能为空')
    return
  }
  if (f.ruleType === 'EXPR' && !f.condition) {
    ElMessage.warning('条件表达式不能为空')
    return
  }
  if (f.ruleType === 'STRUCT') {
    if (!f.leftVar || !f.operator) {
      ElMessage.warning('左变量和操作符不能为空')
      return
    }
    if (f.thresholdType === 'VALUE' && !f.thresholdValue) {
      ElMessage.warning('阈值不能为空')
      return
    }
    if (f.thresholdType === 'VAR' && !f.thresholdVar) {
      ElMessage.warning('阈值变量不能为空')
      return
    }
  }
  await saveRule({
    ruleId: f.ruleId,
    name: f.name,
    ruleType: f.ruleType,
    condition: f.condition,
    leftVar: f.leftVar,
    operator: f.operator,
    thresholdType: f.thresholdType,
    thresholdValue: f.thresholdValue,
    thresholdVar: f.thresholdVar,
    hitScore: f.hitScore,
    hitAction: f.hitAction,
    enabled: f.enabled
  })
  ElMessage.success('已保存')
  emit('update:modelValue', false)
  emit('saved', f.ruleId)
}
</script>
