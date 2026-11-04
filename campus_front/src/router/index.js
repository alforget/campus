import { createRouter, createWebHistory } from 'vue-router'
import { useTokenStore } from '@/stores/token'
import useUserInfoStore from '@/stores/userInfo'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true, title: '登录' },
  },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '首页' },
      },
      {
        path: 'activities',
        name: 'Activities',
        component: () => import('@/views/Activities.vue'),
        meta: { title: '活动广场' },
      },
      {
        path: 'my-registrations',
        name: 'MyRegistrations',
        component: () => import('@/views/MyRegistrations.vue'),
        meta: { title: '我的报名' },
      },
      {
        path: 'notifications',
        name: 'Notifications',
        component: () => import('@/views/Notifications.vue'),
        meta: { title: '通知中心' },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人中心' },
      },
      {
        path: 'club/activities',
        name: 'ClubActivities',
        component: () => import('@/views/club/ClubActivities.vue'),
        meta: { title: '我的活动', roles: [2] },
      },
      {
        path: 'club/activities/:id/registrations',
        name: 'ActivityRegistrations',
        component: () => import('@/views/club/ActivityRegistrations.vue'),
        meta: { title: '报名与签到', roles: [2] },
      },
      {
        path: 'club/booking',
        name: 'ClubBooking',
        component: () => import('@/views/club/ClubBooking.vue'),
        meta: { title: '场地预约', roles: [2] },
      },
      {
        path: 'venue/manage',
        name: 'VenueManage',
        component: () => import('@/views/venue/VenueManage.vue'),
        meta: { title: '场地管理', roles: [1, 3] },
      },
      {
        path: 'venue/bookings',
        name: 'VenueBookings',
        component: () => import('@/views/venue/VenueBookings.vue'),
        meta: { title: '预约管理', roles: [3] },
      },
      {
        path: 'admin/approvals',
        name: 'AdminApprovals',
        component: () => import('@/views/admin/AdminApprovals.vue'),
        meta: { title: '活动审核', roles: [1] },
      },
      {
        path: 'admin/activities',
        name: 'AdminActivities',
        component: () => import('@/views/admin/AdminActivities.vue'),
        meta: { title: '全部活动', roles: [1] },
      },
      {
        path: 'admin/users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/AdminUsers.vue'),
        meta: { title: '用户管理', roles: [1] },
      },
      {
        path: 'admin/clubs',
        name: 'AdminClubs',
        component: () => import('@/views/admin/AdminClubs.vue'),
        meta: { title: '社团管理', roles: [1] },
      },
      {
        path: '403',
        name: 'Forbidden',
        component: () => import('@/views/Forbidden.vue'),
        meta: { title: '无权限' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { public: true, title: '页面不存在' },
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach((to) => {
  const token = useTokenStore().token
  if (to.meta.public) {
    if (to.path === '/login' && token) {
      return '/dashboard'
    }
    return true
  }
  if (!token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  const info = useUserInfoStore().info
  const roleId = Number(info?.roleId)
  if (to.meta.roles && roleId && !to.meta.roles.includes(roleId)) {
    return '/403'
  }
  return true
})

export default router
