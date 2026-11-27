import { createRouter, createWebHashHistory } from 'vue-router'
import RuleConfig from './views/RuleConfig.vue'
import FlowOrchestration from './views/FlowOrchestration.vue'
import FieldComplete from './views/FieldComplete.vue'
import VariableManage from './views/VariableManage.vue'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', redirect: '/rules' },
    { path: '/rules', component: RuleConfig },
    { path: '/vars', component: VariableManage },
    { path: '/flows', component: FlowOrchestration },
    { path: '/field', component: FieldComplete }
  ]
})

export default router
