import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

// 导入组件
const Login = () => import('@/views/auth/Login.vue')
const Dashboard = () => import('@/views/Dashboard.vue')
const EmailManagement = () => import('@/views/email/EmailManagement.vue')
const AliasManagement = () => import('@/views/alias/AliasManagement.vue')
const DomainManagement = () => import('@/views/domain/DomainManagement.vue')
const SecurityLog = () => import('@/views/security/SecurityLog.vue')
const UserProfile = () => import('@/views/user/UserProfile.vue')

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: Dashboard,
    meta: { requiresAuth: true }
  },
  {
    path: '/email',
    name: 'EmailManagement',
    component: EmailManagement,
    meta: { 
      requiresAuth: true,
      title: '邮件管理'
    }
  },
  {
    path: '/aliases',
    name: 'AliasManagement', 
    component: AliasManagement,
    meta: { 
      requiresAuth: true,
      title: '别名管理'
    }
  },
  {
    path: '/domains',
    name: 'DomainManagement',
    component: DomainManagement,
    meta: { 
      requiresAuth: true,
      title: '域名管理',
      roles: ['ADMIN']
    }
  },
  {
    path: '/security',
    name: 'SecurityLog',
    component: SecurityLog,
    meta: { 
      requiresAuth: true,
      title: '安全日志',
      roles: ['ADMIN']
    }
  },
  {
    path: '/profile',
    name: 'UserProfile',
    component: UserProfile,
    meta: { 
      requiresAuth: true,
      title: '个人设置'
    }
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  
  // 检查是否需要认证
  if (to.meta.requiresAuth) {
    if (!userStore.isLoggedIn) {
      next('/login')
      return
    }
    
    // 检查角色权限
    if (to.meta.roles && to.meta.roles.length > 0) {
      const hasRole = to.meta.roles.some(role => 
        userStore.userInfo.roles.includes(role)
      )
      
      if (!hasRole) {
        // 没有权限，跳转到首页或显示错误页面
        next('/dashboard')
        return
      }
    }
  }
  
  // 已登录用户访问登录页，重定向到首页
  if (to.name === 'Login' && userStore.isLoggedIn) {
    next('/dashboard')
    return
  }
  
  next()
})

export default router