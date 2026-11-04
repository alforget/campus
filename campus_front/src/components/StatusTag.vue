<script setup>
import { computed } from 'vue'
import {
  activityStatusType,
  bookingStatusType,
  registrationStatusType,
  slotStatusType,
} from '@/utils/dict'

const props = defineProps({
  status: { type: String, default: '' },
  kind: { type: String, default: 'activity' },
  size: { type: String, default: 'default' },
})

const type = computed(() => {
  if (props.kind === 'registration') return registrationStatusType(props.status)
  if (props.kind === 'booking') return bookingStatusType(props.status)
  if (props.kind === 'slot') return slotStatusType(props.status)
  return activityStatusType(props.status)
})
</script>

<template>
  <el-tag :type="type" :size="size" effect="light">{{ status || '-' }}</el-tag>
</template>
