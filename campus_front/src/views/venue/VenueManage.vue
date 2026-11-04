<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Refresh, Tickets } from '@element-plus/icons-vue'
import { venueApi } from '@/api'
import useUserInfoStore from '@/stores/userInfo'
import { ROLE } from '@/utils/dict'
import { formatDate, formatTime } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'

const userInfoStore = useUserInfoStore()
const canGenerate = computed(() => Number(userInfoStore.info?.roleId) === ROLE.VENUE_ADMIN)

const activeTab = ref('venue')
const loading = ref(false)
const venues = ref([])
const dialogVisible = ref(false)
const editing = ref(false)
const saving = ref(false)
const form = reactive({
  id: null,
  name: '',
  location: '',
  capacity: 50,
  description: '',
  status: 1,
})

const slotVenueId = ref(null)
const slotDate = ref(new Date().toISOString().slice(0, 10))
const slots = ref([])
const slotLoading = ref(false)
const generating = ref(false)

async function loadVenues() {
  loading.value = true
  try {
    venues.value = (await venueApi.list()) || []
    if (!slotVenueId.value && venues.value.length) slotVenueId.value = venues.value[0].id
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = false
  Object.assign(form, {
    id: null,
    name: '',
    location: '',
    capacity: 50,
    description: '',
    status: 1,
  })
  dialogVisible.value = true
}

function openEdit(row) {
  editing.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

async function save() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入场馆名称')
    return
  }
  saving.value = true
  try {
    if (editing.value) {
      await venueApi.update(form)
      ElMessage.success('场馆已更新')
    } else {
      await venueApi.add(form)
      ElMessage.success('场馆已创建')
    }
    dialogVisible.value = false
    loadVenues()
    loadSlots()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm('确认删除该场馆吗？已有预约的场馆请不要删除。', '删除场馆', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  await venueApi.remove(row.id)
  ElMessage.success('已删除')
  loadVenues()
}

async function toggleStatus(row) {
  if (row.status === 1) {
    await venueApi.disable(row.id)
    ElMessage.success('场馆已禁用')
  } else {
    await venueApi.update({ ...row, status: 1 })
    ElMessage.success('场馆已启用')
  }
  loadVenues()
}

async function loadSlots() {
  if (!slotVenueId.value || !slotDate.value) return
  slotLoading.value = true
  try {
    const page = await venueApi.slots({
      pageNum: 1,
      pageSize: 100,
      id: slotVenueId.value,
      slotDate: slotDate.value,
    })
    slots.value = page.items || []
  } catch {
    slots.value = []
  } finally {
    slotLoading.value = false
  }
}

async function generateSlots() {
  generating.value = true
  try {
    await venueApi.assign()
    ElMessage.success('已生成未来 14 天时段')
    loadSlots()
  } finally {
    generating.value = false
  }
}

async function closeSlot(slot) {
  try {
    await ElMessageBox.confirm('关闭后该时段将不可预约，确认关闭吗？', '关闭时段', {
      type: 'warning',
      confirmButtonText: '关闭',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  await venueApi.close(slot.id)
  ElMessage.success('时段已关闭')
  loadSlots()
}

watch([slotVenueId, slotDate], loadSlots)

onMounted(async () => {
  await loadVenues()
  await loadSlots()
})
</script>

<template>
  <div>
    <PageHeader title="场地管理" subtitle="维护场馆信息、生成时段并关闭不可用时段">
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增场馆</el-button>
        <el-button :icon="Refresh" @click="loadVenues">刷新</el-button>
      </template>
    </PageHeader>

    <el-card shadow="never" class="mt-20">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="场馆列表" name="venue">
          <el-table v-loading="loading" :data="venues" stripe>
            <el-table-column label="场馆" min-width="180">
              <template #default="{ row }">
                <div class="name">{{ row.name }}</div>
                <div class="muted small">{{ row.location }}</div>
              </template>
            </el-table-column>
            <el-table-column label="容量" width="90" prop="capacity" />
            <el-table-column label="说明" min-width="220" prop="description" show-overflow-tooltip />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'">
                  {{ row.status === 1 ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
                <el-button link type="warning" @click="toggleStatus(row)">
                  {{ row.status === 1 ? '禁用' : '启用' }}
                </el-button>
                <el-button link type="danger" :icon="Delete" @click="remove(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="时段管理" name="slot">
          <div class="toolbar">
            <el-select v-model="slotVenueId" placeholder="选择场馆" style="width: 220px">
              <el-option
                v-for="venue in venues"
                :key="venue.id"
                :label="venue.name"
                :value="venue.id"
              />
            </el-select>
            <el-date-picker
              v-model="slotDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
              style="width: 160px"
            />
            <el-button :icon="Refresh" @click="loadSlots">查询</el-button>
            <el-button
              v-if="canGenerate"
              type="primary"
              :icon="Tickets"
              :loading="generating"
              @click="generateSlots"
            >
              生成未来 14 天时段
            </el-button>
          </div>
          <el-table v-loading="slotLoading" :data="slots" stripe class="mt-20">
            <el-table-column label="日期" width="130">
              <template #default="{ row }">{{ formatDate(row.slotDate) }}</template>
            </el-table-column>
            <el-table-column label="时间" width="160">
              <template #default="{ row }">
                {{ formatTime(row.startTime) }} - {{ formatTime(row.endTime) }}
              </template>
            </el-table-column>
            <el-table-column label="场馆" min-width="160" prop="venueName" />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <StatusTag :status="row.status" kind="slot" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button
                  v-if="row.status === 'OPEN'"
                  link
                  type="warning"
                  @click="closeSlot(row)"
                >
                  关闭时段
                </el-button>
                <span v-else class="muted">—</span>
              </template>
            </el-table-column>
            <template #empty><el-empty description="该日期暂无时段" /></template>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑场馆' : '新增场馆'" width="520px">
      <el-form label-width="86px" :model="form">
        <el-form-item label="场馆名称" required>
          <el-input v-model="form.name" maxlength="50" />
        </el-form-item>
        <el-form-item label="位置">
          <el-input v-model="form.location" maxlength="100" placeholder="例如：大学生活动中心 201" />
        </el-form-item>
        <el-form-item label="容量">
          <el-input-number v-model="form.capacity" :min="1" :max="1000" style="width: 100%" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.name {
  font-weight: 600;
}
.small {
  font-size: 12px;
}
</style>
