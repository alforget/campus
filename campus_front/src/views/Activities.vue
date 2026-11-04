<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Calendar, Search } from '@element-plus/icons-vue'
import { activityApi, registrationApi } from '@/api'
import { ACTIVITY_STATUS_OPTIONS } from '@/utils/dict'
import { formatDateTime, imageUrl, shortText } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'
import ActivityDetailDialog from '@/components/ActivityDetailDialog.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const activities = ref([])
const pageNum = ref(1)
const pageSize = ref(8)
const total = ref(0)
const status = ref('')
const keyword = ref('')
const myRegistrations = ref([])
const detailVisible = ref(false)
const detailLoading = ref(false)
const current = ref(null)

const statusOptions = [{ value: '', label: '全部状态' }, ...ACTIVITY_STATUS_OPTIONS]

const filtered = computed(() => {
  let list = activities.value
  if (status.value) {
    list = list.filter((item) => item.status === status.value)
  }
  if (keyword.value.trim()) {
    const key = keyword.value.trim().toLowerCase()
    list = list.filter(
      (item) =>
        item.title?.toLowerCase().includes(key) ||
        item.summary?.toLowerCase().includes(key) ||
        item.clubName?.toLowerCase().includes(key),
    )
  }
  return list
})

const myMap = computed(() => {
  const map = {}
  myRegistrations.value.forEach((item) => {
    map[item.activityId] = item
  })
  return map
})

const registered = computed(() => {
  const item = myMap.value[current.value?.id]
  return item && ['成功', '候补', '已签到'].includes(item.status)
})

const canRegister = computed(
  () =>
    current.value?.status === '报名中' &&
    ((current.value?.remaining ?? 0) > 0 || current.value?.waitlistEnabled === 1),
)

async function load() {
  loading.value = true
  try {
    const page = await activityApi.listForStudent({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    })
    activities.value = page.items || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}

async function loadMine() {
  try {
    const page = await registrationApi.listForStudent({ pageNum: 1, pageSize: 100 })
    myRegistrations.value = page.items || []
  } catch {
    myRegistrations.value = []
  }
}

async function openDetail(id) {
  if (!id) return
  detailVisible.value = true
  detailLoading.value = true
  try {
    current.value = await activityApi.detail(id)
  } finally {
    detailLoading.value = false
  }
}

function handleCard(id) {
  router.replace({ path: '/activities', query: { id } })
  openDetail(id)
}

async function register() {
  if (!current.value) return
  const idempotencyKey =
    (crypto?.randomUUID && crypto.randomUUID()) ||
    `reg-${current.value.id}-${Date.now()}-${Math.random().toString(16).slice(2)}`
  try {
    const data = await registrationApi.register(current.value.id, idempotencyKey)
    ElMessage.success(data || '报名成功')
    await Promise.all([load(), loadMine()])
    if (current.value) await openDetail(current.value.id)
  } catch {
    // 错误消息由拦截器提示
  }
}

function onPageChange(value) {
  pageNum.value = value
  load()
}

watch(
  () => route.query.id,
  (id) => {
    if (id) openDetail(id)
  },
)

onMounted(async () => {
  await Promise.all([load(), loadMine()])
  if (route.query.id) openDetail(route.query.id)
})
</script>

<template>
  <div v-loading="loading">
    <div class="page-header">
      <div>
        <h2 class="page-title">活动广场</h2>
        <div class="page-subtitle">发现正在报名的校园活动，选择你感兴趣的一场</div>
      </div>
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索活动标题 / 社团"
          clearable
          style="width: 220px"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="status" placeholder="活动状态" clearable style="width: 150px">
          <el-option
            v-for="item in statusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </div>
    </div>

    <el-empty v-if="!loading && !filtered.length" description="暂时没有符合条件的活动" />
    <el-row :gutter="18" class="mt-20">
      <el-col v-for="activity in filtered" :key="activity.id" :xs="24" :sm="12" :lg="8">
        <el-card shadow="hover" class="activity-card" @click="handleCard(activity.id)">
          <div class="cover">
            <img
              v-if="activity.coverUrl"
              :src="imageUrl(activity.coverUrl)"
              alt=""
              @error="$event.target.style.display = 'none'"
            />
            <div v-else class="cover-placeholder">CAMPUS</div>
            <div class="cover-status">
              <StatusTag :status="activity.status" size="small" />
            </div>
          </div>
          <div class="body">
            <div class="title">{{ activity.title }}</div>
            <div class="summary muted">{{ shortText(activity.summary, 55) }}</div>
            <div class="meta">
              <span><el-icon><Calendar /></el-icon> {{ formatDateTime(activity.startTime) }}</span>
              <span>{{ activity.venueName || activity.venueText || '地点待定' }}</span>
            </div>
            <div class="quota">
              <span>剩余 {{ activity.remaining ?? '-' }} / {{ activity.capacity || '-' }}</span>
              <el-tag v-if="myMap[activity.id]" size="small" type="success" effect="light">
                {{ myMap[activity.id].status }}
              </el-tag>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

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

    <ActivityDetailDialog
      v-model="detailVisible"
      :activity="current"
      :registered="!!registered"
      :can-register="canRegister"
      :loading="detailLoading"
      @register="register"
    />
  </div>
</template>

<style scoped>
.activity-card {
  margin-bottom: 18px;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.activity-card:hover {
  transform: translateY(-3px);
}
.cover {
  height: 160px;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #409eff, #6a5acd);
}
.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.cover-status {
  position: absolute;
  right: 10px;
  top: 10px;
}
.body {
  padding-top: 4px;
}
.title {
  font-size: 16px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.summary {
  font-size: 13px;
  height: 38px;
  line-height: 19px;
  margin: 8px 0;
  overflow: hidden;
}
.meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: #909399;
  font-size: 12px;
}
.meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.quota {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #ebeef5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #606266;
  font-size: 12px;
}
</style>
