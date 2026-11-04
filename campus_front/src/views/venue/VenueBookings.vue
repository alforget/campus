<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { bookingApi, clubApi } from '@/api'
import { BOOKING_STATUS_OPTIONS } from '@/utils/dict'
import { formatDate, formatDateTime, formatTime } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'
import PageHeader from '@/components/PageHeader.vue'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const status = ref('')
const clubId = ref(null)
const clubs = ref([])

const statusOptions = [{ value: '', label: '全部状态' }, ...BOOKING_STATUS_OPTIONS]

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
    const page = await bookingApi.list({
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

async function forceCancel(row) {
  let reason = ''
  try {
    const result = await ElMessageBox.prompt('强制取消会释放时段并通知社团，请填写原因。', '强制取消预约', {
      confirmButtonText: '确认取消',
      cancelButtonText: '返回',
      inputPlaceholder: '请输入取消原因',
    })
    reason = result.value
  } catch {
    return
  }
  if (!reason) {
    ElMessage.warning('请填写取消原因')
    return
  }
  await bookingApi.cancelByVenueAdmin(row.id, reason)
  ElMessage.success('已强制取消')
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
    <PageHeader title="预约管理" subtitle="查看全部场地预约，处理冲突和强制取消">
      <template #actions>
        <el-select v-model="status" clearable placeholder="预约状态" style="width: 140px" @change="load">
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
        <el-table-column label="场地" min-width="180">
          <template #default="{ row }">
            <div>{{ row.venueName || `场地 #${row.venueId}` }}</div>
            <div class="muted small">{{ row.venueLocation }}</div>
          </template>
        </el-table-column>
        <el-table-column label="时段" width="210">
          <template #default="{ row }">
            {{ formatDate(row.slotDate) }} {{ formatTime(row.startTime) }} - {{ formatTime(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column label="社团" min-width="150">
          <template #default="{ row }">{{ row.clubName || `社团 #${row.clubId}` }}</template>
        </el-table-column>
        <el-table-column label="申请人" width="130">
          <template #default="{ row }">{{ row.username || `用户 #${row.userId}` }}</template>
        </el-table-column>
        <el-table-column label="用途" min-width="140" prop="purpose" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><StatusTag :status="row.status" kind="booking" /></template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === '已通过'" link type="danger" @click="forceCancel(row)">
              强制取消
            </el-button>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无预约记录" /></template>
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
.small {
  font-size: 12px;
}
</style>
