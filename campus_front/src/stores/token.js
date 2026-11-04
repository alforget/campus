import { defineStore } from 'pinia'
import { ref } from 'vue'

const STORAGE_KEY = 'campus_token'

export const useTokenStore = defineStore('token', () => {
  const token = ref(localStorage.getItem(STORAGE_KEY) || '')

  const setToken = (value) => {
    token.value = value || ''
    if (token.value) {
      localStorage.setItem(STORAGE_KEY, token.value)
    } else {
      localStorage.removeItem(STORAGE_KEY)
    }
  }

  const removeToken = () => setToken('')

  return { token, setToken, removeToken }
})
