<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Calendar,
  Checked,
  OfficeBuilding,
  Tickets,
  Bell,
  Plus,
  UserFilled,
} from '@element-plus/icons-vue'
import { activityApi, bookingApi, notificationApi, registrationApi, venueApi } from '@/api'
import useUserInfoStore from '@/stores/userInfo'
import { ROLE, roleLabel } from '@/utils/dict'
import { formatDateTime, imageUrl, shortText } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'

const router = useRouter()
const userInfoStore = useUserInfoStore()
const roleId = computed(() => Number(userInfoStore.info?.roleId))
const nickname = computed(
  () => userInfoStore.info?.nickname || userInfoStore.info?.username || '同学',
)

const loading = ref(false)
const activities = ref([])
const activityTotal = ref(0)
const myTotal = ref(0)
const checkedIn = ref(0)
const unread = ref(0)
const pending = ref(0)
const bookingTotal = ref(0)
const venueTotal = ref(0)

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 11) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const quickActions = computed(() => {
  const actions = [
    { label: '活动广场', icon: Calendar, path: '/activities', type: 'primary' },
    { label: '我的报名', icon: Tickets, path: '/my-registrations' },
    { label: '通知中心', icon: Bell, path: '/notifications' },
  ]
  if (roleId.value === ROLE.CLUB_ADMIN) {
    actions.push({ label: '发布活动', icon: Plus, path: '/club/activities?create=1' })
    actions.push({ label: '预约场地', icon: OfficeBuilding, path: '/club/booking' })
  }
  if (roleId.value === ROLE.VENUE_ADMIN || roleId.value === ROLE.ADMIN) {
    actions.push({ label: '场地管理', icon: OfficeBuilding, path: '/venue/manage' })
  }
  if (roleId.value === ROLE.VENUE_ADMIN) {
    actions.push({ label: '预约管理', icon: Checked, path: '/venue/bookings' })
  }
  if (roleId.value === ROLE.ADMIN) {
    actions.push({ label: '活动审核', icon: Checked, path: '/admin/approvals' })
    actions.push({ label: '用户管理', icon: UserFilled, path: '/admin/users' })
  }
  return actions
})

const fourthStat = computed(() => {
  if (roleId.value === ROLE.ADMIN) {
    return { label: '待审核活动', value: pending.value, hint: '需要处理' }
  }
  if (roleId.value === ROLE.CLUB_ADMIN) {
    return { label: '我的预约', value: bookingTotal.value, hint: '场地预约记录' }
  }
  if (roleId.value === ROLE.VENUE_ADMIN) {
    return { label: '预约记录', value: bookingTotal.value, hint: '全部场地预约' }
  }
  return { label: '已签到', value: checkedIn.value, hint: '累计签到活动' }
})

async function load() {
  loading.value = true
  try {
    const [activityRes, myRes, notificationRes] = await Promise.allSettled([
      activityApi.listForStudent({ pageNum: 1, pageSize: 6 }),
      registrationApi.listForStudent({ pageNum: 1, pageSize: 100 }),
      notificationApi.list(
        {
          pageNum: 1,
          pageSize: 1,
          userId: userInfoStore.info?.id,
          readStatus: 0,
        },
        true,
      ),
    ])
    if (activityRes.status === 'fulfilled') {
      activities.value = activityRes.value.items || []
      activityTotal.value = activityRes.value.total || 0
    }
    if (myRes.status === 'fulfilled') {
      myTotal.value = myRes.value.total || 0
      checkedIn.value = (myRes.value.items || []).filter((item) => item.status === '已签到').length
    }
    if (notificationRes.status === 'fulfilled') unread.value = notificationRes.value.total || 0

    if (roleId.value === ROLE.ADMIN) {
      const pendingRes = await activityApi.listForAdmin({
        pageNum: 1,
        pageSize: 1,
        status: '待审核',
      })
      pending.value = pendingRes.total || 0
    }
    if (roleId.value === ROLE.CLUB_ADMIN) {
      const bookingRes = await bookingApi.listForClub({ pageNum: 1, pageSize: 1 })
      bookingTotal.value = bookingRes.total || 0
    }
    if (roleId.value === ROLE.VENUE_ADMIN) {
      const [venueRes, allBookingRes] = await Promise.all([
        venueApi.list(),
        bookingApi.list({ pageNum: 1, pageSize: 1 }),
      ])
      venueTotal.value = venueRes.length || 0
      bookingTotal.value = allBookingRes.total || 0
    }
  } finally {
    loading.value = false
  }
}

function goActivity(id) {
  router.push({ path: '/activities', query: { id } })
}

onMounted(load)
</script>

<template>
  <div v-loading="loading">
    <el-card shadow="never" class="welcome">
      <div class="welcome-inner">
        <div>
          <div class="hello">{{ greeting }}，{{ nickname }}</div>
          <div class="muted">
            {{ roleLabel(roleId) }} · 今天也去看看校园里正在发生什么
          </div>
        </div>
        <div class="quick-actions">
          <el-button
            v-for="action in quickActions"
            :key="action.path"
            :type="action.type || 'default'"
            plain
            @click="router.push(action.path)"
          >
            <el-icon><component :is="action.icon" /></el-icon>
            {{ action.label }}
          </el-button>
        </div>
      </div>
    </el-card>

    <el-row :gutter="16" class="mt-20">
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">活动广场</div>
          <div class="stat-value">{{ activityTotal }}</div>
          <div class="stat-hint">可浏览活动</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">我的报名</div>
          <div class="stat-value">{{ myTotal }}</div>
          <div class="stat-hint">累计报名</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">未读通知</div>
          <div class="stat-value">{{ unread }}</div>
          <div class="stat-hint">待查看消息</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">{{ fourthStat.label }}</div>
          <div class="stat-value">{{ fourthStat.value }}</div>
          <div class="stat-hint">{{ fourthStat.hint }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt-20">
      <el-col :span="16">
        <el-card shadow="never">
          <template #header>
            <div class="page-header">
              <span>近期活动</span>
              <el-link type="primary" :underline="false" @click="router.push('/activities')">
                查看全部
              </el-link>
            </div>
          </template>
          <el-empty v-if="!activities.length" description="暂时没有活动" />
          <div v-else class="activity-list">
            <div
              v-for="activity in activities"
              :key="activity.id"
              class="activity-item"
              @click="goActivity(activity.id)"
            >
              <div class="activity-cover">
                <img
                  v-if="activity.coverUrl"
                  :src="imageUrl(activity.coverUrl)"
                  alt=""
                  @error="$event.target.style.display = 'none'"
                />
                <div v-else class="cover-placeholder">CAMPUS</div>
              </div>
              <div class="activity-main">
                <div class="activity-title">{{ activity.title }}</div>
                <div class="muted activity-summary">
                  {{ shortText(activity.summary, 50) }}
                </div>
                <div class="activity-meta">
                  <StatusTag :status="activity.status" size="small" />
                  <span>{{ formatDateTime(activity.startTime) }}</span>
                  <span>{{ activity.venueName || activity.venueText || '地点待定' }}</span>
                  <span>剩余 {{ activity.remaining ?? '-' }}</span>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="side-card">
          <template #header>角色与权限</template>
          <div class="role-box">
            <div class="role-name">{{ roleLabel(roleId) }}</div>
            <div class="muted">
              {{
                roleId === ROLE.ADMIN
                  ? '可以审核活动、管理用户和社团、查看全部数据'
                  : roleId === ROLE.CLUB_ADMIN
                    ? '可以发布活动、查看报名、预约场地和签到'
                    : roleId === ROLE.VENUE_ADMIN
                      ? '可以管理场地与时段、处理预约'
                      : '可以浏览活动、报名、查看电子票和通知'
              }}
            </div>
          </div>
        </el-card>
        <el-card shadow="never" class="side-card mt-20">
          <template #header>使用提示</template>
          <ul class="tips">
            <li>报名成功后请到“我的报名”查看票号。</li>
            <li>活动满员时可能进入候补，取消后会按顺序转正。</li>
            <li>场地预约需要先选择场地和日期，再选择时段。</li>
            <li>社团管理员发布活动前，建议先确认场地时段。</li>
          </ul>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.welcome {
  background: linear-gradient(120deg, #eef5ff 0%, #f7f9ff 55%, #fff 100%);
  border: 1px solid #e4ecff;
}
.welcome-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
}
.hello {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 6px;
}
.quick-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.stat-card {
  border-radius: 12px;
}
.stat-label {
  color: #909399;
  font-size: 13px;
}
.stat-value {
  font-size: 30px;
  font-weight: 700;
  margin: 8px 0 4px;
  color: #303133;
}
.stat-hint {
  font-size: 12px;
  color: #b0b4bc;
}
.activity-list {
  display: flex;
  flex-direction: column;
}
.activity-item {
  display: flex;
  gap: 14px;
  padding: 12px 4px;
  border-bottom: 1px solid #f0f2f5;
  cursor: pointer;
}
.activity-item:last-child {
  border-bottom: none;
}
.activity-item:hover {
  background: #fafcff;
}
.activity-cover {
  width: 110px;
  height: 70px;
  border-radius: 8px;
  overflow: hidden;
  flex: 0 0 110px;
  background: linear-gradient(135deg, #409eff, #6a5acd);
}
.activity-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.activity-main {
  min-width: 0;
  flex: 1;
}
.activity-title {
  font-weight: 600;
  margin-bottom: 4px;
}
.activity-summary {
  font-size: 13px;
  margin-bottom: 6px;
}
.activity-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #909399;
  flex-wrap: wrap;
}
.role-box {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.role-name {
  font-size: 18px;
  font-weight: 600;
}
.tips {
  margin: 0;
  padding-left: 18px;
  color: #606266;
  line-height: 2;
  font-size: 13px;
}
</style>
