<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, UserFilled } from '@element-plus/icons-vue'
import { clubApi, userApi } from '@/api'
import { formatDateTime } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'

const loading = ref(false)
const clubs = ref([])
const users = ref([])
const createVisible = ref(false)
const assignVisible = ref(false)
const saving = ref(false)
const createForm = reactive({ clubName: '' })
const assignForm = reactive({ clubName: '', username: '' })

async function load() {
  loading.value = true
  try {
    clubs.value = await clubApi.list()
  } finally {
    loading.value = false
  }
}

async function loadUsers() {
  try {
    const page = await userApi.list({ pageNum: 1, pageSize: 100 })
    users.value = page.items || []
  } catch {
    users.value = []
  }
}

function openCreate() {
  createForm.clubName = ''
  createVisible.value = true
}

async function createClub() {
  if (!createForm.clubName.trim()) {
    ElMessage.warning('请输入社团名称')
    return
  }
  saving.value = true
  try {
    await clubApi.add(createForm.clubName.trim())
    ElMessage.success('社团已创建')
    createVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

function openAssign(row) {
  assignForm.clubName = row.name
  assignForm.username = ''
  assignVisible.value = true
}

async function assignAdmin() {
  if (!assignForm.username) {
    ElMessage.warning('请选择管理员')
    return
  }
  saving.value = true
  try {
    await clubApi.assignAdmin(assignForm.clubName, assignForm.username)
    ElMessage.success('管理员已分配')
    assignVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  try {
    await ElMessageBox.confirm(
      `确认${row.status === 1 ? '禁用' : '启用'}社团「${row.name}」吗？`,
      '修改社团状态',
      { type: 'warning' },
    )
  } catch {
    return
  }
  if (row.status === 1) await clubApi.disable(row.name)
  else await clubApi.enable(row.name)
  ElMessage.success('状态已更新')
  load()
}

onMounted(() => {
  Promise.all([load(), loadUsers()])
})
</script>

<template>
  <div>
    <PageHeader title="社团管理" subtitle="创建社团、分配社团管理员、启用或禁用社团">
      <template #actions>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">创建社团</el-button>
      </template>
    </PageHeader>

    <el-card shadow="never" class="mt-20">
      <el-table v-loading="loading" :data="clubs" stripe>
        <el-table-column label="社团" min-width="180">
          <template #default="{ row }">
            <div class="name">{{ row.name }}</div>
            <div class="muted small">{{ row.description || '暂无介绍' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="管理员" width="150">
          <template #default="{ row }">{{ row.leaderName || '未分配' }}</template>
        </el-table-column>
        <el-table-column label="待开始预约" width="130">
          <template #default="{ row }">{{ row.bookingNumbers ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="UserFilled" @click="openAssign(row)">
              分配管理员
            </el-button>
            <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无社团" /></template>
      </el-table>
    </el-card>

    <el-dialog v-model="createVisible" title="创建社团" width="420px">
      <el-form label-width="80px">
        <el-form-item label="社团名称" required>
          <el-input v-model="createForm.clubName" maxlength="50" placeholder="请输入社团名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="createClub">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignVisible" title="分配社团管理员" width="460px">
      <el-form label-width="90px">
        <el-form-item label="社团">
          <el-input v-model="assignForm.clubName" disabled />
        </el-form-item>
        <el-form-item label="管理员">
          <el-select v-model="assignForm.username" filterable placeholder="选择用户" style="width: 100%">
            <el-option
              v-for="user in users"
              :key="user.id"
              :label="`${user.nickname || user.username}（${user.username}）`"
              :value="user.username"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="分配后该用户会被设置为社团管理员，并绑定到这个社团。"
      />
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="assignAdmin">确认分配</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.name {
  font-weight: 600;
}
.small {
  font-size: 12px;
}
</style>
