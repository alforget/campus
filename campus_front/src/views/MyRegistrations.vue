<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Tickets } from '@element-plus/icons-vue'
import { registrationApi } from '@/api'
import { REGISTRATION_STATUS_OPTIONS } from '@/utils/dict'
import { formatDateTime } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'
import PageHeader from '@/components/PageHeader.vue'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const status = ref('')

const statusOptions = [{ value: '', label: '全部状态' }, ...REGISTRATION_STATUS_OPTIONS]

async function load() {
  loading.value = true
  try {
    const page = await registrationApi.listForStudent({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      status: status.value || null,
    })
    list.value = page.items || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}

function canCancel(row) {
  return ['成功', '候补'].includes(row.status)
}

async function cancel(row) {
  let reason = ''
  try {
    const result = await ElMessageBox.prompt('取消后名额会释放给候补同学，确认取消吗？', '取消报名', {
      confirmButtonText: '确认取消',
      cancelButtonText: '再想想',
      inputPlaceholder: '可填写取消原因（选填）',
      inputValue: '',
    })
    reason = result.value || '个人原因'
  } catch {
    return
  }
  await registrationApi.cancel(row.id, reason)
  ElMessage.success('已取消报名')
  load()
}

async function copyTicket(ticketNo) {
  if (!ticketNo) return
  try {
    await navigator.clipboard.writeText(ticketNo)
    ElMessage.success('票号已复制')
  } catch {
    ElMessage.info(ticketNo)
  }
}

function onPageChange(value) {
  pageNum.value = value
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="我的报名" subtitle="查看报名状态、电子票和签到情况">
      <template #actions>
        <el-select v-model="status" placeholder="报名状态" clearable style="width: 150px" @change="load">
          <el-option
            v-for="item in statusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </template>
    </PageHeader>

    <el-card shadow="never" class="mt-20">
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="活动" min-width="220">
          <template #default="{ row }">
            <div class="activity-title">{{ row.activityTitle || `活动 #${row.activityId}` }}</div>
            <div class="muted small">
              {{ formatDateTime(row.activityStartTime) }} · {{ row.venueText || '地点待定' }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <StatusTag :status="row.status" kind="registration" />
          </template>
        </el-table-column>
        <el-table-column label="票号" min-width="180">
          <template #default="{ row }">
            <el-link v-if="row.ticketNo" type="primary" :underline="false" @click="copyTicket(row.ticketNo)">
              {{ row.ticketNo }}
            </el-link>
            <span v-else class="muted">候补中，暂无票号</span>
          </template>
        </el-table-column>
        <el-table-column label="报名时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canCancel(row)" link type="danger" @click="cancel(row)">
              取消报名
            </el-button>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="还没有报名记录">
            <el-button type="primary" :icon="Tickets" @click="$router.push('/activities')">
              去活动广场
            </el-button>
          </el-empty>
        </template>
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
.activity-title {
  font-weight: 600;
  margin-bottom: 4px;
}
.small {
  font-size: 12px;
}
</style>
