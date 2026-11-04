<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { clubApi, userApi } from '@/api'
import useUserInfoStore from '@/stores/userInfo'
import { ROLE_OPTIONS, roleLabel } from '@/utils/dict'
import { formatDateTime } from '@/utils/format'
import RoleTag from '@/components/RoleTag.vue'
import PageHeader from '@/components/PageHeader.vue'

const userInfoStore = useUserInfoStore()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const clubs = ref([])

const currentId = computed(() => Number(userInfoStore.info?.id))

const filtered = computed(() => {
  if (!keyword.value.trim()) return list.value
  const key = keyword.value.toLowerCase()
  return list.value.filter(
    (item) =>
      item.username?.toLowerCase().includes(key) ||
      item.nickname?.toLowerCase().includes(key),
  )
})

async function loadClubs() {
  try {
    clubs.value = await clubApi.list()
  } catch {
    clubs.value = []
  }
}

async function load() {
  loading.value = true
  try {
    const page = await userApi.list({ pageNum: pageNum.value, pageSize: pageSize.value })
    list.value = (page.items || []).map((item) => ({
      ...item,
      clubId: item.clubId || null,
    }))
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}

async function changeRole(row) {
  await userApi.updateRole(row.username, row.roleId)
  ElMessage.success(`已把 ${row.username} 设为${roleLabel(row.roleId)}`)
  load()
}

async function changeClub(row) {
  // 后端 updateClub 目前对 null 会 NPE，清空时先传 0；后端修好后可改回 null。
  await userApi.updateClub({ username: row.username, clubId: row.clubId || 0 })
  ElMessage.success('所属社团已更新')
  load()
}

async function toggleStatus(row) {
  try {
    await ElMessageBox.confirm(
      `确认${row.status === 1 ? '禁用' : '启用'}用户「${row.username}」吗？`,
      '修改用户状态',
      { type: 'warning' },
    )
  } catch {
    return
  }
  if (row.status === 1) await userApi.disable(row.username)
  else await userApi.enable(row.username)
  ElMessage.success('状态已更新')
  load()
}

function onPageChange(value) {
  pageNum.value = value
  load()
}

onMounted(() => {
  Promise.all([loadClubs(), load()])
})
</script>

<template>
  <div>
    <PageHeader title="用户管理" subtitle="管理用户角色、所属社团和账号状态">
      <template #actions>
        <el-input v-model="keyword" placeholder="搜索用户名/昵称" clearable style="width: 220px">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </template>
    </PageHeader>

    <el-card shadow="never" class="mt-20">
      <el-table v-loading="loading" :data="filtered" stripe>
        <el-table-column label="用户" min-width="160">
          <template #default="{ row }">
            <div class="name">{{ row.nickname || row.username }}</div>
            <div class="muted small">@{{ row.username }}</div>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="170">
          <template #default="{ row }">
            <el-select
              v-model="row.roleId"
              size="small"
              :disabled="row.id === currentId"
              @change="changeRole(row)"
            >
              <el-option
                v-for="item in ROLE_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="所属社团" width="200">
          <template #default="{ row }">
            <el-select
              v-model="row.clubId"
              size="small"
              clearable
              filterable
              placeholder="未绑定"
              :disabled="row.id === currentId"
              @change="changeClub(row)"
            >
              <el-option v-for="club in clubs" :key="club.id" :label="club.name" :value="club.id" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最后登录" width="170">
          <template #default="{ row }">{{ formatDateTime(row.lastLoginAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.id !== currentId"
              link
              :type="row.status === 1 ? 'danger' : 'success'"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <span v-else class="muted">当前账号</span>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无用户" /></template>
      </el-table>
      <el-pagination
        v-if="total > pageSize"
        class="pagination"
        background
        layout="total, prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="pageNum"
        @current-change="onPageChange"
      />
    </el-card>
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
