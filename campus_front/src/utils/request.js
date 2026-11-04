import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useTokenStore } from '@/stores/token'
import useUserInfoStore from '@/stores/userInfo'

const service = axios.create({
  baseURL: '/api',
  timeout: 20000,
})

service.interceptors.request.use((config) => {
  const tokenStore = useTokenStore()
  if (tokenStore.token) {
    config.headers.Authorization = tokenStore.token
  }
  return config
})

function clearLogin() {
  useTokenStore().removeToken()
  useUserInfoStore().removeInfo()
}

service.interceptors.response.use(
  (response) => {
    if (response.config.responseType === 'blob') {
      return response
    }
    const body = response.data
    if (body && (body.code === 0 || body.code === 200)) {
      return body.data
    }
    const msg = body?.msg || '请求失败'
    if (!response.config.silent) {
      ElMessage.error(msg)
    }
    return Promise.reject(new Error(msg))
  },
  (error) => {
    const status = error.response?.status
    const body = error.response?.data
    if (status === 401) {
      clearLogin()
      if (router.currentRoute.value.path !== '/login') {
        router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
      }
      ElMessage.error('登录已过期，请重新登录')
    } else if (error.config?.responseType === 'blob') {
      if (!error.config?.silent) ElMessage.error('文件下载失败')
    } else if (!error.config?.silent) {
      ElMessage.error(body?.msg || (status ? `请求失败（${status}）` : '网络异常，请检查后端是否启动'))
    }
    return Promise.reject(error)
  },
)

export function saveBlob(response, filename) {
  const blob = response.data
  const contentType = response.headers?.['content-type'] || ''
  if (contentType.includes('application/json')) {
    ElMessage.error('导出失败')
    return
  }
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

export default service
