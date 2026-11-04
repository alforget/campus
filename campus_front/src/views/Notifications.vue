<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Bell, Check, Refresh } from '@element-plus/icons-vue'
import { notificationApi } from '@/api'
import useUserInfoStore from '@/stores/userInfo'
import { formatDateTime } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'

const userInfoStore = useUserInfoStore()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const tab = ref('unread')
const loadError = ref('')

const readStatus = computed(() => (tab.value === 'unread' ? 0 : null))

async function load() {
  if (!userInfoStore.info?.id) return
  loading.value = true
  loadError.value = ''
  try {
    const page = await notificationApi.list(
      {
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        userId: userInfoStore.info.id,
        readStatus: readStatus.value,
      },
      true,
    )
    list.value = page.items || []
    total.value = page.total || 0
  } catch (error) {
    list.value = []
    total.value = 0
    loadError.value = error?.message || '通知加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function markRead(item) {
  if (item.readStatus === 1) return
  await notificationApi.read(item.id)
  item.readStatus = 1
}

async function readAll() {
  await notificationApi.readAll(userInfoStore.info.id)
  ElMessage.success('已全部标记为已读')
  load()
}

function onTabChange() {
  pageNum.value = 1
  load()
}

function onPageChange(value) {
  pageNum.value = value
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="通知中心" subtitle="报名、候补、审核和预约结果都会在这里通知你">
      <template #actions>
        <el-button :icon="Check" @click="readAll">全部已读</el-button>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </template>
    </PageHeader>

    <el-card shadow="never" class="mt-20">
      <el-tabs v-model="tab" @tab-change="onTabChange">
        <el-tab-pane label="未读" name="unread" />
        <el-tab-pane label="全部" name="all" />
      </el-tabs>

      <el-alert
        v-if="loadError"
        type="warning"
        :closable="false"
        show-icon
        :title="`通知加载失败：${loadError}`"
        description="后端通知接口返回异常，请联系管理员检查 /notification/list。"
      />

      <div v-loading="loading">
        <el-empty v-if="!list.length" description="没有通知" />
        <div v-else class="notice-list">
          <div
            v-for="item in list"
            :key="item.id"
            class="notice-item"
            :class="{ unread: item.readStatus === 0 }"
            @click="markRead(item)"
          >
            <div class="notice-icon"><el-icon><Bell /></el-icon></div>
            <div class="notice-main">
              <div class="notice-head">
                <span class="notice-title">{{ item.title }}</span>
                <span class="muted">{{ formatDateTime(item.createdAt) }}</span>
              </div>
              <div class="notice-content">{{ item.content }}</div>
              <div class="notice-type muted">{{ item.type }}</div>
            </div>
            <el-tag v-if="item.readStatus === 0" type="danger" size="small" effect="plain">未读</el-tag>
          </div>
        </div>
      </div>

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
.notice-list {
  display: flex;
  flex-direction: column;
}
.notice-item {
  display: flex;
  gap: 14px;
  padding: 16px 8px;
  border-bottom: 1px solid #f0f2f5;
  cursor: pointer;
  align-items: flex-start;
}
.notice-item:hover {
  background: #fafcff;
}
.notice-item.unread {
  background: #fffbf0;
}
.notice-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #ecf5ff;
  color: #409eff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 36px;
}
.notice-main {
  flex: 1;
  min-width: 0;
}
.notice-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 12px;
}
.notice-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
.notice-content {
  margin: 6px 0;
  color: #606266;
  line-height: 1.6;
}
.notice-type {
  font-size: 12px;
}
</style>
