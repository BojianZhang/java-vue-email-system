<template>
  <div class="main-layout">
    <!-- 侧边栏 -->
    <div class="sidebar" :class="{ collapsed: isCollapsed }">
      <div class="sidebar-header">
        <div class="logo">
          <Message class="logo-icon" />
          <span v-show="!isCollapsed" class="logo-text">企业邮件</span>
        </div>
        <el-button
          text
          class="collapse-btn"
          @click="toggleCollapse"
        >
          <Fold v-if="!isCollapsed" />
          <Expand v-else />
        </el-button>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapsed"
        :unique-opened="true"
        router
        class="sidebar-menu"
      >
        <template v-for="route in menuRoutes" :key="route.path">
          <el-menu-item
            v-if="!route.meta?.hidden"
            :index="route.path"
            class="menu-item"
          >
            <component :is="route.meta?.icon" class="menu-icon" />
            <span>{{ route.meta?.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </div>

    <!-- 主内容区 -->
    <div class="main-content">
      <!-- 顶部导航 -->
      <div class="header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <!-- 用户菜单 -->
          <el-dropdown trigger="click" @command="handleUserCommand">
            <div class="user-info">
              <el-avatar :src="userStore.avatar" :size="32">
                <User />
              </el-avatar>
              <span class="username">{{ userStore.displayName }}</span>
              <ArrowDown class="dropdown-icon" />
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <User class="menu-icon" />
                  个人资料
                </el-dropdown-item>
                <el-dropdown-item command="security">
                  <Lock class="menu-icon" />
                  安全中心
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <SwitchButton class="menu-icon" />
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <!-- 页面内容 -->
      <div class="content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 侧边栏折叠状态
const isCollapsed = ref(false)

// 当前激活的菜单
const activeMenu = computed(() => route.path)

// 当前页面标题
const currentTitle = computed(() => route.meta?.title || '未知页面')

// 菜单路由（从路由配置中过滤）
const menuRoutes = computed(() => {
  const mainRoute = router.getRoutes().find(r => r.path === '/')
  return mainRoute?.children || []
})

// 切换侧边栏折叠状态
const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

// 处理用户下拉菜单命令
const handleUserCommand = async (command) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'security':
      router.push('/security')
      break
    case 'logout':
      try {
        await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
          type: 'warning'
        })
        await userStore.logout()
      } catch (error) {
        // 用户取消操作
      }
      break
  }
}
</script>

<style lang="scss" scoped>
.main-layout {
  display: flex;
  height: 100vh;
}

.sidebar {
  width: 240px;
  background: #001529;
  transition: width 0.3s ease;
  position: relative;

  &.collapsed {
    width: 64px;
  }

  .sidebar-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 60px;
    padding: 0 16px;
    border-bottom: 1px solid #1f2937;

    .logo {
      display: flex;
      align-items: center;
      color: white;

      .logo-icon {
        font-size: 24px;
        color: #409eff;
      }

      .logo-text {
        margin-left: 12px;
        font-size: 18px;
        font-weight: 600;
      }
    }

    .collapse-btn {
      color: #909399;
      padding: 8px;

      &:hover {
        color: white;
      }
    }
  }

  .sidebar-menu {
    border: none;
    background: transparent;

    :deep(.el-menu-item) {
      color: #b3b3b3;
      margin: 4px 8px;
      border-radius: 6px;

      &:hover {
        background: #1f2937 !important;
        color: white;
      }

      &.is-active {
        background: #409eff !important;
        color: white;
      }

      .menu-icon {
        margin-right: 8px;
      }
    }
  }
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  padding: 0 24px;
  background: white;
  border-bottom: 1px solid #e5e7eb;

  .header-left {
    .el-breadcrumb {
      font-size: 14px;
    }
  }

  .header-right {
    .user-info {
      display: flex;
      align-items: center;
      cursor: pointer;
      padding: 8px 12px;
      border-radius: 6px;
      transition: background-color 0.3s;

      &:hover {
        background: #f5f5f5;
      }

      .username {
        margin: 0 8px;
        font-size: 14px;
      }

      .dropdown-icon {
        font-size: 12px;
        color: #909399;
      }
    }
  }
}

.content {
  flex: 1;
  padding: 24px;
  background: #f5f5f5;
  overflow-y: auto;
}

:deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;

  .menu-icon {
    margin-right: 8px;
  }
}
</style>