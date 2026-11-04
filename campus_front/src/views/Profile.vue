<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Lock, Upload, User } from '@element-plus/icons-vue'
import { authApi, uploadApi } from '@/api'
import { useTokenStore } from '@/stores/token'
import useUserInfoStore from '@/stores/userInfo'
import { roleLabel } from '@/utils/dict'
import { formatDateTime } from '@/utils/format'
import RoleTag from '@/components/RoleTag.vue'
import PageHeader from '@/components/PageHeader.vue'

const router = useRouter()
const tokenStore = useTokenStore()
const userInfoStore = useUserInfoStore()

const active = ref('profile')
const saving = ref(false)
const pwdSaving = ref(false)

const form = reactive({
  nickname: '',
  email: '',
  phone: '',
  avatarUrl: '',
})

const pwd = reactive({
  oldPassword: '',
  newPassword: '',
  repeatPassword: '',
})

async function load() {
  const info = await authApi.info()
  userInfoStore.setInfo(info)
  form.nickname = info.nickname || ''
  form.email = info.email || ''
  form.phone = info.phone || ''
  form.avatarUrl = info.avatarUrl || ''
}

async function saveProfile() {
  if (!form.nickname.trim()) {
    ElMessage.warning('请输入昵称')
    return
  }
  saving.value = true
  try {
    await authApi.updateProfile({ ...form })
    ElMessage.success('资料已更新')
    await load()
  } finally {
    saving.value = false
  }
}

async function uploadAvatar(options) {
  try {
    const url = await uploadApi.avatar(options.file)
    form.avatarUrl = url
    await authApi.updateProfile({ ...form, avatarUrl: url })
    ElMessage.success('头像已更新')
    await load()
  } catch {
    // 拦截器提示
  }
}

async function savePassword() {
  if (!pwd.oldPassword || !pwd.newPassword) {
    ElMessage.warning('请填写完整密码')
    return
  }
  if (pwd.newPassword !== pwd.repeatPassword) {
    ElMessage.warning('两次新密码不一致')
    return
  }
  pwdSaving.value = true
  try {
    await authApi.updatePassword({
      oldPassword: pwd.oldPassword,
      newPassword: pwd.newPassword,
      repeatPassword: pwd.repeatPassword,
    })
    ElMessage.success('密码已修改，请重新登录')
    tokenStore.removeToken()
    userInfoStore.removeInfo()
    router.push('/login')
  } finally {
    pwdSaving.value = false
  }
}

async function logout() {
  try {
    await ElMessageBox.confirm('确认退出登录吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await authApi.logout()
  } catch {
    // ignore
  }
  tokenStore.removeToken()
  userInfoStore.removeInfo()
  router.push('/login')
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="个人中心" subtitle="维护资料、头像和登录密码">
      <template #actions>
        <el-button @click="logout">退出登录</el-button>
      </template>
    </PageHeader>

    <el-row :gutter="18" class="mt-20">
      <el-col :xs="24" :md="8">
        <el-card shadow="never" class="profile-card">
          <div class="avatar-wrap">
            <el-avatar :src="form.avatarUrl || undefined" :size="92">
              <el-icon><User /></el-icon>
            </el-avatar>
            <el-upload
              :show-file-list="false"
              :http-request="uploadAvatar"
              accept="image/png,image/jpeg"
            >
              <el-button size="small" :icon="Upload" class="mt-12">更换头像</el-button>
            </el-upload>
          </div>
          <div class="profile-name">{{ userInfoStore.info.nickname || userInfoStore.info.username }}</div>
          <div class="muted">@{{ userInfoStore.info.username }}</div>
          <div class="profile-meta">
            <RoleTag :role-id="userInfoStore.info.roleId" />
            <span class="muted">社团 ID：{{ userInfoStore.info.clubId || '未绑定' }}</span>
          </div>
          <el-divider />
          <div class="muted small">
            注册时间：{{ formatDateTime(userInfoStore.info.createdAt) }}
          </div>
          <div class="muted small">
            最后登录：{{ formatDateTime(userInfoStore.info.lastLoginAt) }}
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="16">
        <el-card shadow="never">
          <el-tabs v-model="active">
            <el-tab-pane label="基本资料" name="profile">
              <el-form label-width="86px" :model="form" style="max-width: 520px">
                <el-form-item label="昵称" required>
                  <el-input v-model="form.nickname" maxlength="20" />
                </el-form-item>
                <el-form-item label="邮箱">
                  <el-input v-model="form.email" maxlength="128" placeholder="example@campus.edu.cn" />
                </el-form-item>
                <el-form-item label="手机号">
                  <el-input v-model="form.phone" maxlength="20" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="saving" @click="saveProfile">保存资料</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
            <el-tab-pane label="修改密码" name="password">
              <el-form label-width="86px" :model="pwd" style="max-width: 520px">
                <el-form-item label="原密码">
                  <el-input v-model="pwd.oldPassword" type="password" show-password :prefix-icon="Lock" />
                </el-form-item>
                <el-form-item label="新密码">
                  <el-input v-model="pwd.newPassword" type="password" show-password :prefix-icon="Lock" />
                </el-form-item>
                <el-form-item label="确认密码">
                  <el-input v-model="pwd.repeatPassword" type="password" show-password :prefix-icon="Lock" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="pwdSaving" @click="savePassword">
                    修改密码
                  </el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
            <el-tab-pane label="角色说明" name="role">
              <div class="role-explain">
                <h3>{{ roleLabel(userInfoStore.info.roleId) }}</h3>
                <p>
                  {{
                    Number(userInfoStore.info.roleId) === 1
                      ? '系统管理员可以审核活动、管理用户与社团、查看全部数据。'
                      : Number(userInfoStore.info.roleId) === 2
                        ? '社团管理员可以发布活动、查看报名、签到和预约场地。'
                        : Number(userInfoStore.info.roleId) === 3
                          ? '场地管理员可以维护场馆和时段，处理预约。'
                          : '学生可以浏览活动、报名、候补、查看电子票和通知。'
                  }}
                </p>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.profile-card {
  text-align: center;
}
.avatar-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.profile-name {
  margin-top: 14px;
  font-size: 18px;
  font-weight: 600;
}
.profile-meta {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}
.small {
  font-size: 12px;
  margin-top: 6px;
}
.role-explain h3 {
  margin-top: 0;
}
.role-explain p {
  color: #606266;
  line-height: 1.8;
}
</style>
