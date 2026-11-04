import request from '@/utils/request'
import { saveBlob } from '@/utils/request'

function toForm(data) {
  const form = new URLSearchParams()
  Object.keys(data || {}).forEach((key) => {
    if (data[key] !== undefined && data[key] !== null) {
      form.append(key, data[key])
    }
  })
  return form
}

export const authApi = {
  login: (data) => request.post('/user/login', toForm(data)),
  register: (data) => request.post('/user/register', toForm(data)),
  logout: () => request.delete('/user/deleteToken'),
  info: () => request.get('/user/info'),
  updateProfile: (data) => request.put('/user/update', data),
  updatePassword: (data) => request.patch('/user/updatePassword', data),
}

export const activityApi = {
  listForAdmin: (params) => request.get('/activity/list', { params }),
  listForStudent: (params) => request.get('/activity/listforstudent', { params }),
  listForClub: (params) => request.get('/activity/listforclub', { params }),
  detail: (id) => request.get('/activity/detail', { params: { id } }),
  create: (data) => request.post('/activity/add', data),
  update: (data) => request.put('/activity/update', data),
  submit: (id) => request.patch('/activity/submit', null, { params: { id } }),
  audit: (id) => request.patch('/activity/audit', null, { params: { id } }),
  reject: (id, rejectReason) =>
    request.put('/activity/reject', null, { params: { id, rejectReason } }),
  cancel: (id) => request.patch('/activity/cancel', null, { params: { id } }),
  remove: (id) => request.delete('/activity/delete', { params: { id } }),
  removeFromClub: (id) => request.delete('/activity/deleteFromClubAdmin', { params: { id } }),
}

export const registrationApi = {
  register: (activityId, idempotencyKey) =>
    request.post('/registration/add', null, {
      params: { activityId },
      headers: { 'Idempotency-Key': idempotencyKey },
    }),
  list: (params) => request.get('/registration/list', { params }),
  listForStudent: (params) => request.get('/registration/listforStudent', { params }),
  cancel: (registrationId, cancelReason) =>
    request.put('/registration/cancel', null, { params: { registrationId, cancelReason } }),
  export: async (params) => {
    const response = await request.get('/registration/export', { params, responseType: 'blob' })
    saveBlob(response, `activity_${params.activityId}_registrations.csv`)
  },
}

export const bookingApi = {
  create: (data) => request.post('/booking/add', toForm(data)),
  update: (data) => request.put('/booking/update', toForm(data)),
  submit: (id) => request.put('/booking/submit', null, { params: { id } }),
  cancel: (id, cancelReason) =>
    request.put('/booking/cancel', null, { params: { id, cancelReason } }),
  cancelByVenueAdmin: (id, cancelReason) =>
    request.put('/booking/cancelByVenueAdmin', null, { params: { id, cancelReason } }),
  listForClub: (params) => request.get('/booking/listForClubAdmin', { params }),
  list: (params) => request.get('/booking/list', { params }),
}

export const venueApi = {
  list: () => request.get('/venues/list'),
  detail: (id) => request.get('/venues/detail', { params: { id } }),
  add: (data) => request.post('/venues/add', toForm(data)),
  update: (data) => request.put('/venues/update', toForm(data)),
  remove: (id) => request.delete('/venues/delete', { params: { id } }),
  disable: (id) => request.put('/venues/disable', null, { params: { id } }),
  slots: (params) => request.get('/venueSlots/list', { params }),
  assign: () => request.post('/venueSlots/assign'),
  close: (slotId) => request.put('/venueSlots/close', null, { params: { slotId } }),
}

export const clubApi = {
  list: () => request.get('/club/list'),
  detail: (clubName) => request.get('/club/detail', { params: { clubName } }),
  add: (clubName) => request.post('/club/add', toForm({ clubName })),
  update: (data) => request.put('/club/update', data),
  disable: (clubName) => request.patch('/club/disable', null, { params: { clubName } }),
  enable: (clubName) => request.patch('/club/enable', null, { params: { clubName } }),
  assignAdmin: (clubName, username) =>
    request.put('/club/assignAdmin', null, { params: { clubName, username } }),
}

export const userApi = {
  list: (params) => request.get('/user/list', { params }),
  updateRole: (username, roleId) =>
    request.put('/user/updateRole', null, { params: { username, roleId } }),
  updateClub: (data) => request.put('/user/updateClub', data),
  disable: (username) =>
    request.put('/user/disable', JSON.stringify(username), {
      headers: { 'Content-Type': 'application/json' },
    }),
  enable: (username) =>
    request.put('/user/enable', JSON.stringify(username), {
      headers: { 'Content-Type': 'application/json' },
    }),
}

export const notificationApi = {
  list: (params, silent = false) => request.get('/notification/list', { params, silent }),
  read: (id) => request.get('/notification/read', { params: { id } }),
  readAll: (userId) => request.get('/notification/readAll', { params: { userId } }),
}

export const checkinApi = {
  checkin: (ticketNo) => request.put('/checkin/checkin', null, { params: { ticketNo } }),
  count: (activityId) => request.get('/checkin/count', { params: { activityId } }),
}

export const uploadApi = {
  avatar: (file) => {
    const form = new FormData()
    form.append('file', file)
    return request.post('/upload/uploadAvatar', form)
  },
  logo: (file) => {
    const form = new FormData()
    form.append('file', file)
    return request.post('/upload/uploadLogo', form)
  },
  cover: (file) => {
    const form = new FormData()
    form.append('file', file)
    return request.post('/upload/uploadCover', form)
  },
}
