export const ROLE = {
  ADMIN: 1,
  CLUB_ADMIN: 2,
  VENUE_ADMIN: 3,
  STUDENT: 4,
}

export const ROLE_LABELS = {
  1: '系统管理员',
  2: '社团管理员',
  3: '场地管理员',
  4: '学生',
}

export const ROLE_OPTIONS = [
  { value: 1, label: '系统管理员' },
  { value: 2, label: '社团管理员' },
  { value: 3, label: '场地管理员' },
  { value: 4, label: '学生' },
]

export const ACTIVITY_STATUS_OPTIONS = [
  { value: '草稿', label: '草稿' },
  { value: '待审核', label: '待审核' },
  { value: '被驳回', label: '被驳回' },
  { value: '已发布', label: '已发布' },
  { value: '报名中', label: '报名中' },
  { value: '报名已结束', label: '报名已结束' },
  { value: '已开始', label: '已开始' },
  { value: '已结束', label: '已结束' },
  { value: '已取消', label: '已取消' },
]

export const REGISTRATION_STATUS_OPTIONS = [
  { value: '成功', label: '已报名' },
  { value: '候补', label: '候补中' },
  { value: '已签到', label: '已签到' },
  { value: '未出席', label: '未出席' },
  { value: '取消', label: '已取消' },
]

export const BOOKING_STATUS_OPTIONS = [
  { value: '草稿', label: '待提交' },
  { value: '已通过', label: '已预约' },
  { value: '已取消', label: '已取消' },
  { value: '待审核', label: '待审核' },
  { value: '已驳回', label: '已驳回' },
]

export const SLOT_STATUS_OPTIONS = [
  { value: 'OPEN', label: '可预约' },
  { value: 'BOOKED', label: '已预约' },
  { value: 'CLOSED', label: '已关闭' },
]

export const SLOT_STATUS_LABELS = {
  OPEN: '可预约',
  BOOKED: '已预约',
  CLOSED: '已关闭',
  开放: '可预约',
  已预约: '已预约',
  关闭: '已关闭',
}

export function roleLabel(roleId) {
  return ROLE_LABELS[Number(roleId)] || '未知角色'
}

export function activityStatusType(status) {
  const map = {
    草稿: 'info',
    待审核: 'warning',
    被驳回: 'danger',
    已发布: 'success',
    报名中: 'primary',
    报名已结束: 'info',
    已开始: 'warning',
    已结束: 'info',
    已取消: 'danger',
  }
  return map[status] || 'info'
}

export function registrationStatusType(status) {
  const map = {
    成功: 'success',
    候补: 'warning',
    已签到: 'primary',
    未出席: 'danger',
    取消: 'info',
  }
  return map[status] || 'info'
}

export function bookingStatusType(status) {
  const map = {
    草稿: 'info',
    待审核: 'warning',
    已通过: 'success',
    已驳回: 'danger',
    已取消: 'info',
  }
  return map[status] || 'info'
}

export function slotStatusType(status) {
  const map = {
    OPEN: 'success',
    BOOKED: 'warning',
    CLOSED: 'info',
    开放: 'success',
    已预约: 'warning',
    关闭: 'info',
  }
  return map[status] || 'info'
}
