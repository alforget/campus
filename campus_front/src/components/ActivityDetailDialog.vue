<script setup>
import { computed } from 'vue'
import StatusTag from '@/components/StatusTag.vue'
import { formatDateTime, imageUrl } from '@/utils/format'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  activity: { type: Object, default: null },
  registered: { type: Boolean, default: false },
  canRegister: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'register'])

const coverStyle = computed(() =>
  props.activity?.coverUrl
    ? { backgroundImage: `url(${imageUrl(props.activity.coverUrl)})` }
    : {},
)

function close() {
  emit('update:modelValue', false)
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    width="760px"
    :title="activity?.title || '活动详情'"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-loading="loading">
      <div v-if="activity" class="detail-body">
        <div class="cover" :style="coverStyle">
          <div v-if="!activity.coverUrl" class="cover-placeholder">CAMPUS</div>
        </div>
        <div class="meta">
          <StatusTag :status="activity.status" />
          <span class="muted">{{ activity.clubName || '社团活动' }}</span>
          <span class="muted">{{ activity.venueName || activity.venueText || '地点待定' }}</span>
        </div>
        <div class="time">
          {{ formatDateTime(activity.startTime) }} — {{ formatDateTime(activity.endTime) }}
        </div>
        <div class="time muted">
          报名时间：{{ formatDateTime(activity.signupStart) }} — {{ formatDateTime(activity.signupEnd) }}
        </div>
        <div class="quota">
          <el-progress
            :percentage="
              activity.capacity
                ? Math.round(((activity.capacity - (activity.remaining || 0)) / activity.capacity) * 100)
                : 0
            "
            :stroke-width="10"
          />
          <div class="quota-text">
            已报名 {{ (activity.capacity || 0) - (activity.remaining || 0) }} / {{ activity.capacity || 0 }}
            <span class="muted">（剩余 {{ activity.remaining ?? '-' }}）</span>
          </div>
        </div>
        <el-divider />
        <div class="summary">{{ activity.summary }}</div>
        <div class="description">{{ activity.description }}</div>
      </div>
    </div>
    <template #footer>
      <el-button @click="close">关闭</el-button>
      <el-button
        v-if="canRegister"
        type="primary"
        :disabled="registered"
        @click="emit('register')"
      >
        {{ registered ? '已报名' : '立即报名' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.detail-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.cover {
  height: 220px;
  border-radius: 10px;
  background: linear-gradient(135deg, #409eff, #6a5acd);
  background-size: cover;
  background-position: center;
  overflow: hidden;
}
.meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.time {
  color: #606266;
}
.quota-text {
  margin-top: 6px;
  color: #606266;
  font-size: 13px;
}
.summary {
  font-weight: 600;
  color: #303133;
}
.description {
  white-space: pre-wrap;
  line-height: 1.8;
  color: #606266;
}
</style>
