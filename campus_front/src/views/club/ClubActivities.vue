<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Refresh, Upload, View } from '@element-plus/icons-vue'
import { activityApi, uploadApi, venueApi } from '@/api'
import useUserInfoStore from '@/stores/userInfo'
import { ACTIVITY_STATUS_OPTIONS } from '@/utils/dict'
import { formatDateTime, imageUrl } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'
import PageHeader from '@/components/PageHeader.vue'

const route = useRoute()
const router = useRouter()
const userInfoStore = useUserInfoStore()

const loading = ref(false)
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const status = ref('')
const isArchive = ref('')
const venues = ref([])
const drawer = ref(false)
const saving = ref(false)
const editing = ref(false)

const emptyForm = () => ({
  id: null,
  title: '',
  summary: '',
  description: '',
  coverUrl: '',
  venueId: null,
  venueText: '',
  startTime: '',
  endTime: '',
  signupStart: '',
  signupEnd: '',
  capacity: 50,
  waitlistEnabled: 1,
  waitlistLimit: 50,
  clubId: userInfoStore.info?.clubId,
})

const form = reactive(emptyForm())

const statusOptions = [{ value: '', label: '全部状态' }, ...ACTIVITY_STATUS_OPTIONS]
const archiveOptions = [
  { value: '', label: '全部' },
  { value: false, label: '未归档' },
  { value: true, label: '已归档' },
]

const canEdit = (row) => ['草稿', '被驳回'].includes(row.status)
const canSubmit = (row) => ['草稿', '被驳回'].includes(row.status)
const canCancel = (row) => ['待审核', '已发布', '报名中'].includes(row.status)

function pad(value) {
  return String(value).padStart(2, '0')
}

function dateTime(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:00`
}

function fillDefaults() {
  const now = new Date()
  const tomorrow = new Date(now.getTime() + 24 * 60 * 60 * 1000)
  tomorrow.setHours(18, 0, 0, 0)
  const end = new Date(tomorrow.getTime() + 2 * 60 * 60 * 1000)
  const signupStart = new Date(now.getTime() + 60 * 60 * 1000)
  signupStart.setMinutes(0, 0, 0)
  const signupEnd = new Date(tomorrow.getTime() - 60 * 60 * 1000)
  form.startTime = dateTime(tomorrow)
  form.endTime = dateTime(end)
  form.signupStart = dateTime(signupStart)
  form.signupEnd = dateTime(signupEnd)
}

async function load() {
  loading.value = true
  try {
    const page = await activityApi.listForClub({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      status: status.value || null,
      isArchive: isArchive.value === '' ? null : isArchive.value,
    })
    list.value = page.items || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}

async function loadVenues() {
  try {
    const data = await venueApi.list()
    venues.value = (data || []).filter((item) => item.status === 1)
  } catch {
    venues.value = []
  }
}

function openCreate() {
  editing.value = false
  Object.assign(form, emptyForm())
  fillDefaults()
  drawer.value = true
}

function openEdit(row) {
  editing.value = true
  Object.assign(form, emptyForm(), {
    id: row.id,
    title: row.title,
    summary: row.summary,
    description: row.description,
    coverUrl: row.coverUrl,
    venueId: row.venueId,
    venueText: row.venueText,
    startTime: row.startTime,
    endTime: row.endTime,
    signupStart: row.signupStart,
    signupEnd: row.signupEnd,
    capacity: row.capacity,
    waitlistEnabled: row.waitlistEnabled,
    waitlistLimit: row.waitlistLimit,
    clubId: row.clubId,
  })
  drawer.value = true
}

function onVenueChange(id) {
  const venue = venues.value.find((item) => item.id === id)
  if (venue) form.venueText = venue.name
}

function validate() {
  if (!form.title || form.title.length < 5) return '标题至少 5 个字'
  if (!form.summary || form.summary.length < 10) return '摘要至少 10 个字'
  if (!form.description || form.description.length < 20) return '详情至少 20 个字'
  if (!form.venueId) return '请选择场馆'
  if (!form.startTime || !form.endTime) return '请选择活动时间'
  if (!form.signupStart || !form.signupEnd) return '请选择报名时间'
  if (!form.capacity || form.capacity < 1) return '名额至少为 1'
  if (!form.waitlistLimit || form.waitlistLimit < 50) return '候补上限至少为 50'
  return ''
}

async function save() {
  const error = validate()
  if (error) {
    ElMessage.warning(error)
    return
  }
  saving.value = true
  try {
    const payload = { ...form, clubId: userInfoStore.info?.clubId }
    if (editing.value) {
      await activityApi.update(payload)
      ElMessage.success('活动已更新')
    } else {
      await activityApi.create(payload)
      ElMessage.success('活动已创建，请提交审核')
    }
    drawer.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function submit(row) {
  try {
    await ElMessageBox.confirm('提交后进入审核状态，确认提交吗？', '提交审核', {
      confirmButtonText: '提交',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  await activityApi.submit(row.id)
  ElMessage.success('已提交审核')
  load()
}

async function cancel(row) {
  let reason = ''
  try {
    const result = await ElMessageBox.prompt('取消活动会通知已报名同学，确认取消吗？', '取消活动', {
      confirmButtonText: '确认取消',
      cancelButtonText: '返回',
      inputPlaceholder: '可填写取消原因',
    })
    reason = result.value || '活动调整'
  } catch {
    return
  }
  await activityApi.cancel(row.id)
  ElMessage.success(`活动已取消${reason ? `：${reason}` : ''}`)
  load()
}

async function remove(row) {
  try {
    await ElMessageBox.confirm('删除后不可恢复，确认删除这个草稿吗？', '删除活动', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  await activityApi.removeFromClub(row.id)
  ElMessage.success('已删除')
  load()
}

async function uploadCover(options) {
  try {
    const url = await uploadApi.cover(options.file)
    form.coverUrl = url
    ElMessage.success('封面上传成功')
  } catch {
    // 拦截器提示
  }
}

function goRegistrations(row) {
  router.push(`/club/activities/${row.id}/registrations`)
}

function onPageChange(value) {
  pageNum.value = value
  load()
}

onMounted(async () => {
  await Promise.all([load(), loadVenues()])
  if (route.query.create === '1') openCreate()
})
</script>

<template>
  <div>
    <PageHeader title="我的活动" subtitle="创建活动、提交审核、查看报名和签到">
      <template #actions>
        <el-select v-model="status" clearable placeholder="活动状态" style="width: 150px" @change="load">
          <el-option
            v-for="item in statusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-select v-model="isArchive" placeholder="归档状态" style="width: 130px" @change="load">
          <el-option
            v-for="item in archiveOptions"
            :key="String(item.value)"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">发布活动</el-button>
      </template>
    </PageHeader>

    <el-card shadow="never" class="mt-20">
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="活动" min-width="230">
          <template #default="{ row }">
            <div class="title">{{ row.title }}</div>
            <div class="muted small">{{ row.summary }}</div>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.startTime) }}
            <div class="muted small">至 {{ formatDateTime(row.endTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="报名时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.signupStart) }}
            <div class="muted small">至 {{ formatDateTime(row.signupEnd) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="名额" width="120">
          <template #default="{ row }">
            <span>{{ row.remaining ?? '-' }} / {{ row.capacity }}</span>
            <div class="muted small">候补 {{ row.waitlistRemaining ?? '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canEdit(row)" link type="primary" :icon="Edit" @click="openEdit(row)">
              编辑
            </el-button>
            <el-button v-if="canSubmit(row)" link type="success" @click="submit(row)">提交</el-button>
            <el-button link type="primary" :icon="View" @click="goRegistrations(row)">名单</el-button>
            <el-button v-if="canCancel(row)" link type="warning" @click="cancel(row)">取消</el-button>
            <el-button v-if="canEdit(row)" link type="danger" :icon="Delete" @click="remove(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="还没有活动，先发布一场吧" /></template>
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

    <el-drawer v-model="drawer" :title="editing ? '编辑活动' : '发布活动'" size="680px">
      <el-form label-width="92px" :model="form">
        <el-form-item label="活动标题" required>
          <el-input v-model="form.title" maxlength="50" show-word-limit placeholder="5—50 个字" />
        </el-form-item>
        <el-form-item label="活动摘要" required>
          <el-input v-model="form.summary" maxlength="200" show-word-limit placeholder="10—200 个字" />
        </el-form-item>
        <el-form-item label="活动详情" required>
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="5"
            maxlength="5000"
            show-word-limit
            placeholder="活动流程、注意事项等，20—5000 字"
          />
        </el-form-item>
        <el-form-item label="活动封面">
          <div class="cover-upload">
            <el-upload
              :show-file-list="false"
              :http-request="uploadCover"
              accept="image/png,image/jpeg"
            >
              <el-button :icon="Upload">上传封面</el-button>
            </el-upload>
            <el-image v-if="form.coverUrl" :src="imageUrl(form.coverUrl)" class="cover-preview" fit="cover" />
          </div>
        </el-form-item>
        <el-form-item label="活动场馆" required>
          <el-select v-model="form.venueId" placeholder="请选择场馆" style="width: 100%" @change="onVenueChange">
            <el-option
              v-for="venue in venues"
              :key="venue.id"
              :label="`${venue.name}（${venue.location || '校内'}）`"
              :value="venue.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="活动时间" required>
          <div class="time-row">
            <el-date-picker
              v-model="form.startTime"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="开始时间"
              style="width: 48%"
            />
            <span class="muted">至</span>
            <el-date-picker
              v-model="form.endTime"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="结束时间"
              style="width: 48%"
            />
          </div>
        </el-form-item>
        <el-form-item label="报名时间" required>
          <div class="time-row">
            <el-date-picker
              v-model="form.signupStart"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="报名开始"
              style="width: 48%"
            />
            <span class="muted">至</span>
            <el-date-picker
              v-model="form.signupEnd"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="报名结束"
              style="width: 48%"
            />
          </div>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="活动名额" required>
              <el-input-number
                v-model="form.capacity"
                :min="1"
                :max="500"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="候补上限">
              <el-input-number
                v-model="form.waitlistLimit"
                :min="50"
                :max="200"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="开启候补">
          <el-select v-model="form.waitlistEnabled" style="width: 100%">
            <el-option label="开启" :value="1" />
            <el-option label="关闭" :value="0" />
          </el-select>
        </el-form-item>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="活动时间需要与已生成的场地时段匹配；如果提交失败，请先在“场地预约”中预约对应时段。"
        />
      </el-form>
      <template #footer>
        <el-button @click="drawer = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">
          {{ editing ? '保存修改' : '创建草稿' }}
        </el-button>
      </template>
    </el-drawer>
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
.cover-upload {
  display: flex;
  align-items: center;
  gap: 14px;
}
.cover-preview {
  width: 120px;
  height: 72px;
  border-radius: 6px;
}
.time-row {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
</style>
