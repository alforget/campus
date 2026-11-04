<script setup>
import { computed } from 'vue'
import { roleLabel } from '@/utils/dict'

const props = defineProps({
  roleId: { type: [Number, String], default: null },
})

const type = computed(() => {
  const id = Number(props.roleId)
  if (id === 1) return 'danger'
  if (id === 2) return 'warning'
  if (id === 3) return 'success'
  return 'info'
})
</script>

<template>
  <el-tag :type="type" effect="light">{{ roleLabel(roleId) }}</el-tag>
</template>
