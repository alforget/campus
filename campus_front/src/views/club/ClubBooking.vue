<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { bookingApi, venueApi } from '@/api'
import { SLOT_STATUS_LABELS } from '@/utils/dict'
import { formatDate, formatDateTime, formatTime } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'
import PageHeader from '@/components/PageHeader.vue'

const loading = ref(false)
const venues = ref([])
const venueId = ref(null)
const slotDate = ref(new Date().toISOString().slice(0, 10))
const slots = ref([])
const selectedSlot = ref(null)
const purpose = ref('')
const bookings = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(100)
const status = ref('')

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: '草稿', label: '待提交' },
  { value: '已通过', label: '已预约' },
  { value: '已取消', label: '已取消' },
]

const selectedLabel = computed(() => {
  if (!selectedSlot.value) return ''
  return `${selectedSlot.value.venueName || ''} ${formatDate(selectedSlot.value.slotDate)} ${formatTime(
    selectedSlot.value.startTime,
  )}-${formatTime(selectedSlot.value.endTime)}`
})

async function loadVenues() {
  try {
    venues.value = (await venueApi.list()).filter((item) => item.status === 1)
    if (!venueId.value && venues.value.length) venueId.value = venues.value[0].id
  } catch {
    venues.value = []
  }
}

async function loadSlots() {
  if (!venueId.value || !slotDate.value) return
  try {
    const page = await venueApi.slots({
      pageNum: 1,
      pageSize: 100,
      id: venueId.value,
      slotDate: slotDate.value,
    })
    slots.value = page.items || []
  } catch {
    slots.value = []
  }
}

async function loadBookings() {
  loading.value = true
  try {
    const page = await bookingApi.listForClub({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      status: status.value || null,
    })
    bookings.value = page.items || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}

function chooseSlot(slot) {
  if (slot.status !== 'OPEN') return
  if (slotHasBooking(slot.id)) {
    ElMessage.warning('这个时段你已经有一条预约记录，请先在下方处理')
    return
  }
  selectedSlot.value = slot
}

function slotHasBooking(slotId) {
  return bookings.value.some(
    (item) =>
      Number(item.slotId) === Number(slotId) &&
      !['已取消', '已驳回'].includes(item.status),
  )
}

async function book() {
  if (!selectedSlot.value) {
    ElMessage.warning('请先选择一个可预约时段')
    return
  }
  if (slotHasBooking(selectedSlot.value.id)) {
    ElMessage.warning('这个时段你已经预约过了，不能重复预约')
    return
  }
  if (!purpose.value.trim()) {
    ElMessage.warning('请填写预约用途')
    return
  }
  await bookingApi.create({ slotId: selectedSlot.value.id, purpose: purpose.value.trim() })
  ElMessage.success('预约已创建，请在下方提交')
  selectedSlot.value = null
  purpose.value = ''
  await Promise.all([loadSlots(), loadBookings()])
}

async function submit(row) {
  await bookingApi.submit(row.id)
  ElMessage.success('预约已提交')
  await Promise.all([loadSlots(), loadBookings()])
}

async function cancel(row) {
  let reason = ''
  try {
    const result = await ElMessageBox.prompt('开始前 24 小时内不能取消；确认取消这个预约吗？', '取消预约', {
      confirmButtonText: '确认取消',
      cancelButtonText: '返回',
      inputPlaceholder: '可填写取消原因',
    })
    reason = result.value || '个人原因'
  } catch {
    return
  }
  await bookingApi.cancel(row.id, reason)
  ElMessage.success('已取消预约')
  await Promise.all([loadSlots(), loadBookings()])
}

function onPageChange(value) {
  pageNum.value = value
  loadBookings()
}

onMounted(async () => {
  await loadVenues()
  await Promise.all([loadSlots(), loadBookings()])
})
</script>

<template>
  <div>
    <PageHeader title="场地预约" subtitle="先选择场地与日期，再选择时段预约；提交后时段才会被占用">
      <template #actions>
        <el-button :icon="Refresh" @click="loadBookings">刷新我的预约</el-button>
      </template>
    </PageHeader>

    <el-card shadow="never" class="mt-20">
      <div class="booking-form">
        <el-select v-model="venueId" placeholder="选择场地" style="width: 220px" @change="loadSlots">
          <el-option
            v-for="venue in venues"
            :key="venue.id"
            :label="`${venue.name}（${venue.location || '校内'}）`"
            :value="venue.id"
          />
        </el-select>
        <el-date-picker
          v-model="slotDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择日期"
          style="width: 160px"
          @change="loadSlots"
        />
        <el-button :icon="Search" @click="loadSlots">查询时段</el-button>
        <el-input v-model="purpose" placeholder="预约用途，例如：社团例会" style="width: 220px" />
        <el-button type="primary" :disabled="!selectedSlot" @click="book">预约所选时段</el-button>
      </div>
      <div v-if="selectedLabel" class="selected-tip">已选择：{{ selectedLabel }}</div>

      <el-row :gutter="12" class="mt-20">
        <el-col v-for="slot in slots" :key="slot.id" :xs="12" :sm="8" :md="6">
          <div
            class="slot-card"
            :class="{ disabled: slot.status !== 'OPEN', active: selectedSlot?.id === slot.id }"
            @click="chooseSlot(slot)"
          >
            <div class="slot-time">{{ formatTime(slot.startTime) }} — {{ formatTime(slot.endTime) }}</div>
            <div class="slot-meta">
              <span>{{ slot.venueName }}</span>
              <el-tag :type="slot.status === 'OPEN' ? 'success' : slot.status === 'BOOKED' ? 'warning' : 'info'" size="small">
                {{ SLOT_STATUS_LABELS[slot.status] || slot.status }}
              </el-tag>
            </div>
          </div>
        </el-col>
      </el-row>
      <el-empty v-if="!slots.length" description="该日期没有可查询的时段" />
    </el-card>

    <el-card shadow="never" class="mt-20">
      <template #header>
        <div class="page-header">
          <span>我的预约</span>
          <el-select v-model="status" clearable placeholder="预约状态" style="width: 140px" @change="loadBookings">
            <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </div>
      </template>
      <el-table v-loading="loading" :data="bookings" stripe>
        <el-table-column label="场地" min-width="160">
          <template #default="{ row }">
            <div>{{ row.venueName || `场地 #${row.venueId}` }}</div>
            <div class="muted small">{{ row.venueLocation }}</div>
          </template>
        </el-table-column>
        <el-table-column label="时段" width="220">
          <template #default="{ row }">
            {{ formatDate(row.slotDate) }} {{ formatTime(row.startTime) }} - {{ formatTime(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column label="用途" min-width="140" prop="purpose" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><StatusTag :status="row.status" kind="booking" /></template>
        </el-table-column>
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === '草稿'" link type="success" @click="submit(row)">提交</el-button>
            <el-button v-if="row.status === '已通过'" link type="danger" @click="cancel(row)">取消</el-button>
          </template>
        </el-table-column>
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
.booking-form {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}
.selected-tip {
  margin-top: 10px;
  color: #409eff;
  font-size: 13px;
}
.slot-card {
  border: 1px solid #e4e7ed;
  border-radius: 10px;
  padding: 12px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s;
}
.slot-card:hover {
  border-color: #409eff;
}
.slot-card.active {
  border-color: #409eff;
  background: #ecf5ff;
}
.slot-card.disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
.slot-time {
  font-weight: 600;
  margin-bottom: 8px;
}
.slot-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #909399;
  font-size: 12px;
}
.small {
  font-size: 12px;
}
</style>
