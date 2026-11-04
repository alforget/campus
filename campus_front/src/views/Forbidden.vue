<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="result-page">
    <el-result icon="error" title="403" sub-title="当前角色没有访问该页面的权限">
      <template #extra>
        <el-button type="primary" @click="router.push('/dashboard')">回到首页</el-button>
      </template>
    </el-result>
  </div>
</template>

<style scoped>
.result-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}
</style>
