<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useRoute } from 'vue-router'

const userStore = useUserStore()
const route = useRoute()

// 应用初始化
onMounted(async () => {
  // 检查登录状态
  const token = localStorage.getItem('token')
  if (token && !route.path.includes('/login')) {
    try {
      await userStore.getProfile()
    } catch (error) {
      console.error('获取用户信息失败:', error)
      userStore.logout()
    }
  }
})
</script>

<style lang="scss">
#app {
  height: 100vh;
  overflow: hidden;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen', 'Ubuntu', 'Cantarell', 'Fira Sans', 'Droid Sans', 'Helvetica Neue', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

.el-message-box {
  border-radius: 8px;
}

.el-button {
  border-radius: 6px;
}

.el-card {
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}
</style>