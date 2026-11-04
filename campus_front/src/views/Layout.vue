<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowDown,
  Bell,
  Calendar,
  Checked,
  DataLine,
  HomeFilled,
  List,
  OfficeBuilding,
  Setting,
  Tickets,
  User,
  UserFilled,
} from '@element-plus/icons-vue'
import avatarDefault from '@/assets/default.png'
import { authApi, notificationApi } from '@/api'
import { useTokenStore } from '@/stores/token'
import useUserInfoStore from '@/stores/userInfo'
import { ROLE, roleLabel } from '@/utils/dict'

const route = useRoute()
const router = useRouter()
const tokenStore = useTokenStore()
const userInfoStore = useUserInfoStore()
const unread = ref(0)

const roleId = computed(() => Number(userInfoStore.info?.roleId))
const nickname = computed(
  () => userInfoStore.info?.nickname || userInfoStore.info?.username || '同学',
)
const avatar = computed(() => userInfoStore.info?.avatarUrl || avatarDefault)

const groups = computed(() => {
  const list = [
    {
      title: '活动',
      items: [
        { index: '/dashboard', label: '首页', icon: HomeFilled },
        { index: '/activities', label: '活动广场', icon: Calendar },
        { index: '/my-registrations', label: '我的报名', icon: Tickets },
        { index: '/notifications', label: '通知中心', icon: Bell },
      ],
    },
  ]
  if (roleId.value === ROLE.CLUB_ADMIN) {
    list.push({
      title: '社团管理',
      items: [
        { index: '/club/activities', label: '我的活动', icon: List },
        { index: '/club/booking', label: '场地预约', icon: OfficeBuilding },
      ],
    })
  }
  if (roleId.value === ROLE.VENUE_ADMIN || roleId.value === ROLE.ADMIN) {
    const venueItems = [{ index: '/venue/manage', label: '场地与时段', icon: OfficeBuilding }]
    if (roleId.value === ROLE.VENUE_ADMIN) {
      venueItems.push({ index: '/venue/bookings', label: '预约管理', icon: Checked })
    }
    list.push({ title: '场地管理', items: venueItems })
  }
  if (roleId.value === ROLE.ADMIN) {
    list.push({
      title: '系统管理',
      items: [
        { index: '/admin/approvals', label: '活动审核', icon: Checked },
        { index: '/admin/activities', label: '全部活动', icon: DataLine },
        { index: '/admin/users', label: '用户管理', icon: UserFilled },
        { index: '/admin/clubs', label: '社团管理', icon: OfficeBuilding },
      ],
    })
  }
  list.push({
    title: '个人',
    items: [{ index: '/profile', label: '个人中心', icon: Setting }],
  })
  return list
})

const pageTitle = computed(() => route.meta.title || '校园活动平台')

async function loadInfo() {
  try {
    const info = await authApi.info()
    userInfoStore.setInfo(info)
    if (route.meta.roles && !route.meta.roles.includes(Number(info.roleId))) {
      router.replace('/403')
      return
    }
    await loadUnread()
  } catch {
    // 401 已由请求拦截器处理
  }
}

async function loadUnread() {
  if (!userInfoStore.info?.id) return
  try {
    const page = await notificationApi.list(
      {
        pageNum: 1,
        pageSize: 1,
        userId: userInfoStore.info.id,
        readStatus: 0,
      },
      true,
    )
    unread.value = page?.total || 0
  } catch {
    unread.value = 0
  }
}

async function logout() {
  try {
    await ElMessageBox.confirm('确认退出登录吗？', '提示', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await authApi.logout()
  } catch {
    // 后端异常不阻塞本地退出
  }
  tokenStore.removeToken()
  userInfoStore.removeInfo()
  ElMessage.success('已退出登录')
  router.push('/login')
}

function handleCommand(command) {
  if (command === 'logout') logout()
  else router.push(`/${command}`)
}

onMounted(loadInfo)
</script>

<template>
  <el-container class="layout-container">
    <el-aside width="224px" class="aside">
      <div class="brand">
        <div class="brand-mark" aria-hidden="true">校</div>
        <div>
          <div class="brand-name">校园活动</div>
          <div class="brand-sub">Campus Events</div>
        </div>
      </div>
      <el-menu
        :default-active="route.path"
        active-text-color="#ffd04b"
        background-color="#232323"
        text-color="#d7dae0"
        router
        class="menu"
      >
        <template v-for="group in groups" :key="group.title">
          <div class="menu-group">{{ group.title }}</div>
          <el-menu-item v-for="item in group.items" :key="item.index" :index="item.index">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <div class="page-title-sm">{{ pageTitle }}</div>
          <el-tag size="small" effect="plain">{{ roleLabel(roleId) }}</el-tag>
        </div>
        <div class="header-right">
          <el-badge :value="unread" :hidden="!unread" :max="99">
            <el-button circle text @click="router.push('/notifications')">
              <el-icon :size="20"><Bell /></el-icon>
            </el-button>
          </el-badge>
          <el-dropdown placement="bottom-end" @command="handleCommand">
            <span class="user-box">
              <el-avatar :src="avatar" :size="34" />
              <span class="nickname">{{ nickname }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile" :icon="User">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
      <el-footer class="footer">校园活动报名与场地预约平台 · 2026</el-footer>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout-container {
  height: 100vh;
}
.aside {
  background: #232323;
  overflow-y: auto;
}
.brand {
  height: 86px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 20px;
  color: #fff;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.brand-mark {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 42px;
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, #409eff 0%, #6a5acd 100%);
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.28);
}
.brand-name {
  font-weight: 700;
  letter-spacing: 1px;
}
.brand-sub {
  font-size: 11px;
  color: #9aa0aa;
  margin-top: 3px;
  letter-spacing: 1px;
}
.menu {
  border-right: none;
  padding: 8px 10px 40px;
}
.menu-group {
  color: #7d838e;
  font-size: 11px;
  padding: 16px 14px 6px;
  letter-spacing: 1px;
}
.menu :deep(.el-menu-item) {
  border-radius: 8px;
  height: 44px;
  margin: 2px 0;
}
.menu :deep(.el-menu-item.is-active) {
  background: rgba(255, 208, 75, 0.12);
}
.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #ebeef5;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.page-title-sm {
  font-size: 16px;
  font-weight: 600;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.user-box {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}
.nickname {
  font-size: 14px;
  color: #303133;
}
.main {
  background: #f5f7fa;
  padding: 20px;
}
.footer {
  height: 40px;
  line-height: 40px;
  text-align: center;
  color: #a0a4ab;
  font-size: 12px;
  background: #f5f7fa;
}
</style>
