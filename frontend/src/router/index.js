import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import NProgress from 'nprogress'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/LoginPage.vue'),
    meta: {
      title: '登录',
      guest: true
    }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/RegisterPage.vue'),
    meta: {
      title: '注册',
      guest: true
    }
  },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    redirect: '/dashboard',
    meta: {
      requiresAuth: true
    },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/DashboardPage.vue'),
        meta: {
          title: '仪表板',
          icon: 'House'
        }
      },
      {
        path: 'emails',
        name: 'Emails',
        component: () => import('@/views/email/EmailListPage.vue'),
        meta: {
          title: '邮件管理',
          icon: 'Message'
        }
      },
      {
        path: 'emails/compose',
        name: 'ComposeEmail',
        component: () => import('@/views/email/ComposeEmailPage.vue'),
        meta: {
          title: '撰写邮件',
          hidden: true
        }
      },
      {
        path: 'aliases',
        name: 'Aliases',
        component: () => import('@/views/alias/AliasListPage.vue'),
        meta: {
          title: '别名管理',
          icon: 'Avatar'
        }
      },
      {
        path: 'security',
        name: 'Security',
        component: () => import('@/views/security/UserSecurityPage.vue'),
        meta: {
          title: '安全中心',
          icon: 'Lock'
        }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/user/ProfilePage.vue'),
        meta: {
          title: '个人资料',
          icon: 'User'
        }
      }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/layout/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    meta: {
      requiresAuth: true,
      requiresAdmin: true
    },
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/AdminDashboard.vue'),
        meta: {
          title: '管理首页',
          icon: 'DataBoard'
        }
      },
      {
        path: 'users',
        name: 'UserManagement',
        component: () => import('@/views/admin/UserManagement.vue'),
        meta: {
          title: '用户管理',
          icon: 'User'
        }
      },
      {
        path: 'domains',
        name: 'DomainManagement',
        component: () => import('@/views/admin/DomainManagement.vue'),
        meta: {
          title: '域名管理',
          icon: 'Globe'
        }
      },
      {
        path: 'ssl',
        name: 'SslCertificates',
        component: () => import('@/views/ssl/SslCertificateManagement.vue'),
        meta: {
          title: 'SSL证书管理',
          icon: 'Lock'
        }
      },
      {
        path: 'ssl/performance',
        name: 'SslPerformance',
        component: () => import('@/views/ssl/SslPerformanceDashboard.vue'),
        meta: {
          title: 'SSL性能监控',
          icon: 'DataAnalysis'
        }
      },
      {
        path: 'security',
        name: 'SecurityMonitoring',
        component: () => import('@/views/admin/SecurityMonitoring.vue'),
        meta: {
          title: '安全监控',
          icon: 'View'
        }
      },
      {
        path: 'security-config',
        name: 'SecurityConfiguration',
        component: () => import('@/views/admin/SecurityConfiguration.vue'),
        meta: {
          title: '安全配置',
          icon: 'Setting'
        }
      },
      {
        path: 'system',
        name: 'SystemSettings',
        component: () => import('@/views/admin/SystemSettings.vue'),
        meta: {
          title: '系统设置',
          icon: 'Tools'
        }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/NotFound.vue'),
    meta: {
      title: '页面不存在'
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫
router.beforeEach(async (to, from, next) => {
  NProgress.start()
  
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - 企业邮件系统` : '企业邮件系统'
  
  const userStore = useUserStore()
  const token = localStorage.getItem('token')
  
  // 如果是访客页面（登录、注册等）
  if (to.meta.guest) {
    if (token && userStore.isLoggedIn) {
      // 已登录用户访问登录页，重定向到首页
      next(userStore.user.role === 'admin' ? '/admin' : '/')
    } else {
      next()
    }
    return
  }
  
  // 需要认证的页面
  if (to.meta.requiresAuth) {
    if (!token) {
      next('/login')
      return
    }
    
    // 如果没有用户信息，先获取
    if (!userStore.isLoggedIn) {
      try {
        await userStore.getProfile()
      } catch (error) {
        console.error('获取用户信息失败:', error)
        userStore.logout()
        next('/login')
        return
      }
    }
    
    // 检查管理员权限
    if (to.meta.requiresAdmin && userStore.user.role !== 'admin') {
      next('/')
      return
    }
  }
  
  next()
})

router.afterEach(() => {
  NProgress.done()
})

export default router