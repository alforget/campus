<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Close, Refresh, View } from '@element-plus/icons-vue'
import { activityApi } from '@/api'
import { formatDateTime } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'
import PageHeader from '@/components/PageHeader.vue'
import ActivityDetailDialog from '@/components/ActivityDetailDialog.vue'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const detailVisible = ref(false)
const detailLoading = ref(false)
const current = ref(null)

async function load() {
  loading.value = true
  try {
    const page = await activityApi.listForAdmin({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      status: '待审核',
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

async function approve(row) {
  try {
    await ElMessageBox.confirm(`确认通过「${row.title}」的审核吗？`, '通过审核', {
      confirmButtonText: '通过',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  await activityApi.audit(row.id)
  ElMessage.success('已通过审核')
  load()
}

async function reject(row) {
  let reason = ''
  try {
    const result = await ElMessageBox.prompt('驳回原因会通知社团管理员，请填写 5—200 字。', '驳回活动', {
      confirmButtonText: '确认驳回',
      cancelButtonText: '取消',
      inputPlaceholder: '请输入驳回原因',
      inputValidator: (value) => {
        if (!value || value.trim().length < 5) return '驳回原因至少 5 个字'
        if (value.length > 200) return '驳回原因不能超过 200 个字'
        return true
      },
    })
    reason = result.value
  } catch {
    return
  }
  await activityApi.reject(row.id, reason)
  ElMessage.success('已驳回')
  load()
}

function onPageChange(value) {
  pageNum.value = value
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="活动审核" subtitle="审核社团提交的活动，通过后学生即可看到">
      <template #actions>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </template>
    </PageHeader>

    <el-card shadow="never" class="mt-20">
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="活动" min-width="240">
          <template #default="{ row }">
            <div class="title">{{ row.title }}</div>
            <div class="muted small">{{ row.clubName }} · {{ row.summary }}</div>
          </template>
        </el-table-column>
        <el-table-column label="活动时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="报名时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.signupStart) }}</template>
        </el-table-column>
        <el-table-column label="名额" width="90">
          <template #default="{ row }">{{ row.capacity }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="view(row)">详情</el-button>
            <el-button link type="success" :icon="Check" @click="approve(row)">通过</el-button>
            <el-button link type="danger" :icon="Close" @click="reject(row)">驳回</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="没有待审核活动" /></template>
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
