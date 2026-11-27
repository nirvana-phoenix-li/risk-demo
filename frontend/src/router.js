import { createRouter, createWebHashHistory } from 'vue-router'
import RuleConfig from './views/RuleConfig.vue'
import FlowOrchestration from './views/FlowOrchestration.vue'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', redirect: '/rules' },
    { path: '/rules', component: RuleConfig },
    { path: '/flows', component: FlowOrchestration }
  ]
})

export default router
