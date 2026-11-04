<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Download, Refresh, Select } from '@element-plus/icons-vue'
import { activityApi, checkinApi, registrationApi } from '@/api'
import { REGISTRATION_STATUS_OPTIONS } from '@/utils/dict'
import { formatDateTime } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'
import PageHeader from '@/components/PageHeader.vue'

const route = useRoute()
const router = useRouter()
const activityId = Number(route.params.id)

const loading = ref(false)
const activity = ref(null)
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const status = ref('')
const stats = ref('')
const checkinVisible = ref(false)
const ticketNo = ref('')
const checkinLoading = ref(false)

const statusOptions = [{ value: '', label: '全部状态' }, ...REGISTRATION_STATUS_OPTIONS]

async function loadActivity() {
  try {
    activity.value = await activityApi.detail(activityId)
  } catch {
    activity.value = null
  }
}

async function load() {
  loading.value = true
  try {
    const page = await registrationApi.list({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      activityId,
      status: status.value || null,
    })
    list.value = page.items || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    stats.value = await checkinApi.count(activityId)
  } catch {
    stats.value = ''
  }
}

async function exportCsv() {
  await registrationApi.export({ activityId, status: status.value || null })
}

async function checkin() {
  if (!ticketNo.value.trim()) {
    ElMessage.warning('请输入票号')
    return
  }
  checkinLoading.value = true
  try {
    await checkinApi.checkin(ticketNo.value.trim())
    ElMessage.success('签到成功')
    checkinVisible.value = false
    ticketNo.value = ''
    await Promise.all([load(), loadStats()])
  } finally {
    checkinLoading.value = false
  }
}

function onPageChange(value) {
  pageNum.value = value
  load()
}

onMounted(() => {
  Promise.all([loadActivity(), load(), loadStats()])
})
</script>

<template>
  <div>
    <PageHeader
      :title="activity ? `${activity.title} · 报名名单` : '报名名单'"
      :subtitle="activity ? `${formatDateTime(activity.startTime)} · ${activity.venueName || activity.venueText || '地点待定'}` : ''"
    >
      <template #actions>
        <el-button :icon="ArrowLeft" @click="router.back()">返回</el-button>
        <el-select v-model="status" clearable placeholder="报名状态" style="width: 140px" @change="load">
          <el-option
            v-for="item in statusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="success" :icon="Select" @click="checkinVisible = true">签到</el-button>
        <el-button type="primary" :icon="Download" @click="exportCsv">导出 CSV</el-button>
      </template>
    </PageHeader>

    <el-alert
      v-if="stats"
      class="mt-20"
      type="success"
      :closable="false"
      show-icon
      :title="stats"
    />

    <el-card shadow="never" class="mt-20">
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="用户" min-width="160">
          <template #default="{ row }">
            <div>{{ row.nickname || row.username || `用户 #${row.userId}` }}</div>
            <div class="muted small">{{ row.username }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><StatusTag :status="row.status" kind="registration" /></template>
        </el-table-column>
        <el-table-column label="票号" min-width="200">
          <template #default="{ row }">{{ row.ticketNo || '—' }}</template>
        </el-table-column>
        <el-table-column label="报名时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="取消原因" min-width="140">
          <template #default="{ row }">{{ row.cancelReason || '—' }}</template>
        </el-table-column>
        <template #empty><el-empty description="还没有报名记录" /></template>
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

    <el-dialog v-model="checkinVisible" title="活动签到" width="420px">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="当前后端签到接口按“当前登录用户”校验票号；如果要以社团管理员身份代签，需要后端补充代签接口。"
      />
      <el-input v-model="ticketNo" class="mt-20" placeholder="请输入票号" clearable @keyup.enter="checkin" />
      <template #footer>
        <el-button @click="checkinVisible = false">取消</el-button>
        <el-button type="primary" :loading="checkinLoading" @click="checkin">确认签到</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.small {
  font-size: 12px;
}
</style>
