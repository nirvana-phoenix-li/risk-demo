import axios from 'axios'

const http = axios.create({ baseURL: '/api', timeout: 10000 })

http.interceptors.response.use(
  res => {
    const body = res.data
    if (body.code !== 0) {
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body.data
  },
  err => Promise.reject(new Error(err.response?.data?.message || err.message))
)

export const listRules = () => http.get('/rule-engine/rules')
export const saveRule = data => http.post('/rule-engine/rules', data)
export const deleteRule = id => http.delete(`/rule-engine/rules/${id}`)
export const tryRule = (id, input) => http.post(`/rule-engine/rules/${id}/try`, input)

export const listVars = () => http.get('/rule-engine/vars')
export const saveVar = data => http.post('/rule-engine/vars', data)
export const deleteVar = id => http.delete(`/rule-engine/vars/${id}`)

export const listFlows = () => http.get('/rule-engine/flows')
export const getFlow = id => http.get(`/rule-engine/flows/${id}`)
export const saveFlow = data => http.post('/rule-engine/flows', data)
export const deleteFlow = id => http.delete(`/rule-engine/flows/${id}`)
export const runFlow = (id, input) => http.post(`/rule-engine/flows/${id}/run`, input)

export const listScripts = () => http.get('/field-complete/scripts')
export const getScript = id => http.get(`/field-complete/scripts/${id}`)
export const saveScript = data => http.post('/field-complete/scripts', data)
export const executeFieldComplete = data => http.post('/field-complete/execute', data)
