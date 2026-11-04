import { defineStore } from 'pinia'
import { ref } from 'vue'

const STORAGE_KEY = 'campus_user'

function readUser() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}')
  } catch {
    return {}
  }
}

const useUserInfoStore = defineStore('userInfo', () => {
  const info = ref(readUser())

  const setInfo = (value) => {
    info.value = value || {}
    localStorage.setItem(STORAGE_KEY, JSON.stringify(info.value))
  }

  const removeInfo = () => {
    info.value = {}
    localStorage.removeItem(STORAGE_KEY)
  }

  return { info, setInfo, removeInfo }
})

export default useUserInfoStore
