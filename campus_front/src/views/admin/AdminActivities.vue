<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Refresh, View } from '@element-plus/icons-vue'
import { activityApi, clubApi } from '@/api'
import { ACTIVITY_STATUS_OPTIONS } from '@/utils/dict'
import { formatDateTime } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'
import PageHeader from '@/components/PageHeader.vue'
import ActivityDetailDialog from '@/components/ActivityDetailDialog.vue'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const status = ref('')
const clubId = ref(null)
const clubs = ref([])
const detailVisible = ref(false)
const detailLoading = ref(false)
const current = ref(null)

const statusOptions = [{ value: '', label: '全部状态' }, ...ACTIVITY_STATUS_OPTIONS]

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
    const page = await activityApi.listForAdmin({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      status: status.value || null,
      clubId: clubId.value || null,
    })
    list.value = page.items || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}

async function view(row) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    current.value = await activityApi.detail(row.id)
  } finally {
    detailLoading.value = false
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm('仅草稿/被驳回活动可删除，确认删除吗？', '删除活动', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  await activityApi.remove(row.id)
  ElMessage.success('已删除')
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
    <PageHeader title="全部活动" subtitle="查看所有社团的活动状态与报名情况">
      <template #actions>
        <el-select v-model="status" clearable placeholder="活动状态" style="width: 150px" @change="load">
          <el-option
            v-for="item in statusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-select v-model="clubId" clearable filterable placeholder="社团" style="width: 180px" @change="load">
          <el-option v-for="club in clubs" :key="club.id" :label="club.name" :value="club.id" />
        </el-select>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </template>
    </PageHeader>

    <el-card shadow="never" class="mt-20">
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="活动" min-width="240">
          <template #default="{ row }">
            <div class="title">{{ row.title }}</div>
            <div class="muted small">{{ row.clubName }} · {{ row.venueName || row.venueText }}</div>
          </template>
        </el-table-column>
        <el-table-column label="活动时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="报名" width="130">
          <template #default="{ row }">
            {{ (row.capacity || 0) - (row.remaining || 0) }} / {{ row.capacity }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="view(row)">详情</el-button>
            <el-button
              v-if="['草稿', '被驳回'].includes(row.status)"
              link
              type="danger"
              :icon="Delete"
              @click="remove(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无活动" /></template>
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

    <ActivityDetailDialog v-model="detailVisible" :activity="current" :loading="detailLoading" />
  </div>
</template>

<style scoped>
.title {
  font-weight: 600;
  margin-bottom: 4px;
}
.small {
  font-size: 12px;
}
</style>
