<script setup>
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { authApi } from '@/api'
import { useTokenStore } from '@/stores/token'
import useUserInfoStore from '@/stores/userInfo'

const router = useRouter()
const route = useRoute()
const tokenStore = useTokenStore()
const userInfoStore = useUserInfoStore()

const isRegister = ref(false)
const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
  rePassword: '',
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 5, max: 20, message: '用户名长度为 5—20 位', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 5, max: 20, message: '密码长度为 5—20 位', trigger: 'blur' },
  ],
  rePassword: [
    {
      validator: (rule, value, callback) => {
        if (!value) callback(new Error('请再次输入密码'))
        else if (value !== form.password) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
}

function reset() {
  form.username = ''
  form.password = ''
  form.rePassword = ''
}

async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  if (isRegister.value && form.password !== form.rePassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  loading.value = true
  try {
    if (isRegister.value) {
      await authApi.register({ username: form.username, password: form.password })
      ElMessage.success('注册成功，请登录')
      isRegister.value = false
      form.password = ''
      form.rePassword = ''
      return
    }
    const token = await authApi.login({ username: form.username, password: form.password })
    tokenStore.setToken(token)
    const info = await authApi.info()
    userInfoStore.setInfo(info)
    ElMessage.success('登录成功')
    router.push(String(route.query.redirect || '/dashboard'))
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-row class="login-page">
    <el-col :span="14" class="bg">
      <div class="slogan">
        <div class="slogan-title">校园活动，一处发生</div>
        <div class="slogan-sub">报名 · 候补 · 场地 · 签到，一站式管理</div>
      </div>
    </el-col>
    <el-col :span="7" :offset="2" class="form">
      <el-form
        :model="form"
        :rules="rules"
        size="large"
        autocomplete="off"
        @keyup.enter="submit"
      >
        <el-form-item>
          <div class="form-title">
            <div class="logo-badge">校</div>
            <div>
              <h1>{{ isRegister ? '注册账号' : '登录' }}</h1>
              <p class="muted">校园活动报名与场地预约平台</p>
            </div>
          </div>
        </el-form-item>
        <el-form-item prop="username">
          <el-input v-model="form.username" :prefix-icon="User" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            :prefix-icon="Lock"
            type="password"
            show-password
            placeholder="请输入密码"
          />
        </el-form-item>
        <el-form-item v-if="isRegister" prop="rePassword">
          <el-input
            v-model="form.rePassword"
            :prefix-icon="Lock"
            type="password"
            show-password
            placeholder="请再次输入密码"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="submit" :loading="loading" @click="submit">
            {{ isRegister ? '注册' : '登录' }}
          </el-button>
        </el-form-item>
        <el-form-item class="switch">
          <span class="muted">{{ isRegister ? '已有账号？' : '还没有账号？' }}</span>
          <el-link
            type="primary"
            :underline="false"
            @click="
              isRegister = !isRegister;
              reset()
            "
          >
            {{ isRegister ? '返回登录' : '注册学生账号' }}
          </el-link>
        </el-form-item>
        <div class="hint muted">
          学生注册后默认为学生角色；社团管理员、场地管理员由系统管理员分配。
        </div>
      </el-form>
    </el-col>
  </el-row>
</template>

<style scoped>
.login-page {
  height: 100vh;
  background: #fff;
}
.bg {
  position: relative;
  background:
    linear-gradient(135deg, rgba(20, 30, 60, 0.35), rgba(64, 158, 255, 0.28)),
    url('@/assets/login_bg.jpg') no-repeat center / cover;
  border-radius: 0 24px 24px 0;
  display: flex;
  align-items: flex-end;
  padding: 60px;
}
.slogan {
  color: #fff;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
}
.slogan-title {
  font-size: 36px;
  font-weight: 700;
  letter-spacing: 3px;
}
.slogan-sub {
  margin-top: 12px;
  font-size: 15px;
  opacity: 0.9;
  letter-spacing: 1px;
}
.form {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding-right: 40px;
}
.form-title {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 10px;
  width: 100%;
}
.logo-badge {
  width: 54px;
  height: 54px;
  border-radius: 14px;
  background: linear-gradient(135deg, #409eff, #6a5acd);
  color: #fff;
  font-size: 24px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 18px rgba(64, 158, 255, 0.25);
}
h1 {
  margin: 0;
  font-size: 26px;
  color: #303133;
}
.form-title p {
  margin: 5px 0 0;
  font-size: 12px;
}
.submit {
  width: 100%;
}
.switch {
  display: flex;
  align-items: center;
  gap: 8px;
}
.hint {
  font-size: 12px;
  line-height: 1.6;
}
</style>
